package com.may.simpleecommercesite.beans;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.may.simpleecommercesite.annotations.*;
import com.may.simpleecommercesite.entities.Product;
import com.may.simpleecommercesite.helpers.ErrandBoy;
import com.may.simpleecommercesite.sqlUtils.CompiledStatement;
import com.may.simpleecommercesite.sqlUtils.SqlTypeConverter;
import net.sf.cglib.proxy.*;
import org.objectweb.asm.ClassVisitor;
import javax.enterprise.inject.spi.Producer;
import javax.sql.DataSource;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DbContext {
    private static final Class<? extends Annotation>[] notMappedFieldAnnotations= new Class[]{OneToMany.class};
    final ObjectMapper mapper;
    final SqlTypeConverter typeConverter;
    private final Set<Object> idCache =new HashSet<>();
    private final Map<Map<String,Object>, List<Object>> searchCache=new HashMap<>();
    private final Map<Object, Set<String>> dirtMap=new ConcurrentHashMap<>();
    private final Map<Class<?>, Map<String,String>>  fieldNameMappings=new HashMap<>();
    private DataSource dataSource;
    private Connection connectionn;
    private boolean connectionMode;
    public DbContext(ObjectMapper mapper, DataSource dataSource){
        this.mapper = mapper;
        this.dataSource=dataSource;
        this.typeConverter=new SqlTypeConverter(this.mapper);
    }
    public DbContext(ObjectMapper mapper, Connection connection){
        this.mapper=mapper;
        this.connectionn=connection;
        this.connectionMode=true;
        this.typeConverter=new SqlTypeConverter(this.mapper);
    }
    public <T> T findById(Class<T> clazz, Object... ids) {
        T entity = instantiate(clazz, ids);
        return findAndPopulate(entity);
    }
    public <T> List<T> search(Class<T> clazz ,Map<String,Object> params){
        params= replaceWithColumnName(params, fieldNameMappings.containsKey(clazz)?fieldNameMappings.get(clazz):createColumnNameMapping(clazz));
        if (clazz.equals(Product.class) && this.searchCache.containsKey(params)) return (List<T>) this.searchCache.get(params);
        CompiledStatement statement=new CompiledStatement(CompiledStatement.StatementType.SELECT, clazz.getSimpleName());
        populateSearchStatement(statement, params);
        try {
            Connection connection=getConnection();
            ResultSet results= statement.execute(connection);
            List<T> entities= createWithResults(clazz, results);
            this.searchCache.put(params, (List<Object>) entities);
            entities.replaceAll(this::toProxy);
            close(connection);
            return entities;
        } catch (NoSuchMethodException | SQLException | InvocationTargetException | IllegalAccessException |
                 InstantiationException e) {
            throw new RuntimeException(e);
        }
    }
    private Map<String,String> createColumnNameMapping(Class<?> clazz){
        Map<String,String> mapping=new HashMap<>();
        for (Field field: clazz.getDeclaredFields()){
            String columnName=getColumnName(field);
            if(!field.getName().equals(columnName)) {
                mapping.put(field.getName(), columnName);
            }
        }
        fieldNameMappings.put(clazz, mapping);
        return mapping;
    }
    private static Map<String, Object> replaceWithColumnName(Map<String, Object> params, Map<String, String> mapping){
        Map<String, Object> ret=new HashMap<>();
        for (Map.Entry<String, Object> param: params.entrySet()){
            String columnName=mapping.get(param.getKey());
            ret.put(columnName!=null?columnName:param.getKey(), param.getValue());
        }
        return ret;
    }
    /**
     * queries the database with field values of the object in the parameter and populates the same object with query results.
     * @param entity entity that contains the search parameters.
     * @return returns the populated entity.
     */
    <T> T findAndPopulate(T entity){
        if (this.idCache.contains(entity)) return getFromCache(entity);
        CompiledStatement statement= new CompiledStatement(CompiledStatement.StatementType.SELECT, entity.getClass().getSimpleName());
        try {
            Connection connection= getConnection();
            populateStatement(statement, entity);
            ResultSet rs = statement.execute(connection);
            populateWithResult(entity, rs, false);
            close(connection);
        } catch (SQLException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        entity=toProxy(entity);
        idCache.add(entity);
        return entity;
    }
    private void close(Connection connection) throws SQLException {
        if (!connectionMode) connection.close();
    }
    public <T> T fetchLOb(T entity, String fieldName){
        CompiledStatement statement=new CompiledStatement(CompiledStatement.StatementType.SELECT, entity.getClass().getSimpleName());
        try (Connection connection=dataSource.getConnection()) {
            populateStatement(statement, entity);
            populateWithResult(entity, statement.execute(connection), true, fieldName);
            return entity;
        } catch (SQLException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    public <T> T saveWithNulls(T entity) throws SQLException {
        if(update(entity, true)==0)
            insert(entity);
        idCache.remove(entity);
        idCache.add(entity);
        return  entity;
    }
    public <T> T save(T entity) throws SQLException {
        try{
            entity=insert(entity);
        } catch (SQLException e){
            if (e.getErrorCode()==1062 |e.getErrorCode()==1048) update(entity, false);
            else throw e;
        }
        return  makeProxyAndAddToCache(entity);
    }
    public int remove(Object entity){
        try (CompiledStatement statement=new CompiledStatement(CompiledStatement.StatementType.DELETE, ErrandBoy.getRealClass(entity).getSimpleName());){
            Connection connection=getConnection();
            populateStatement(statement,entity);
            int res= statement.executeUpdate(connection);
            close(connection);
            return res;
        } catch (SQLException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    public <T> T insert(T entity) throws SQLException {
        try (CompiledStatement statement=new CompiledStatement(CompiledStatement.StatementType.INSERT, ErrandBoy.getRealClass(entity).getSimpleName());){
            Connection connection=getConnection();
            populateStatement(statement, entity);
            Field aiField=ErrandBoy.getAnnotatedField(entity.getClass(), AutoGenerated.class);
            if(aiField!=null)
                retrieveGeneratedKey(entity, statement.execute(connection), aiField.getName());
            close(connection);
            return entity;
        } catch (InvocationTargetException | IllegalAccessException | IOException | NoSuchMethodException |
                 InstantiationException e) {
            throw new RuntimeException(e);
        }
    }
    public int update(Object entity, boolean updateNull) throws SQLException {
        try(CompiledStatement statement = new CompiledStatement(CompiledStatement.StatementType.UPDATE, ErrandBoy.getRealClass(entity).getSimpleName(), updateNull);){
            Connection connection=getConnection();
            populateStatement(statement, entity);
            int res= statement.executeUpdate(connection);
            close(connection);
            return res;
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    private Connection getConnection() throws SQLException {
        if (this.dataSource!=null) return dataSource.getConnection();
        else if (this.connectionn!=null) return this.connectionn;
        else throw new RuntimeException("NO CONNECTION SOURCE");
    }
    private <T> T makeProxyAndAddToCache(T entity){
        if (!entity.getClass().getName().contains("$$EnhancerByCGLIB$$")) entity=toProxy(entity);
        idCache.remove(entity);
        idCache.add(entity);
        return entity;
    }
    private <T> T toProxy(T entity){
        Enhancer proxy= new Enhancer();
        proxy.setSuperclass(entity.getClass());
        proxy.setCallback(new EntityMethodInterceptor(this, entity));
        return (T) proxy.create();
    }
    public void addDirtyField(Object entity, String fieldName){
        dirtMap.get(entity).add(fieldName);
    }
    private <T> T getFromCache(T entity){
        for (Object o: idCache){
            if (entity.equals(o)) return (T) o;
        }
        return null;
    }
    private <T> List<T> createWithResults(Class<T> clazz, ResultSet resultSet) throws NoSuchMethodException, SQLException, InvocationTargetException, IllegalAccessException, InstantiationException {
        List<T> entities=new ArrayList<>();
        T entity=clazz.getConstructor().newInstance();
        while (populateWithResult(entity, resultSet, false)){
            entities.add(entity);
            entity=clazz.getConstructor().newInstance();
        }
        return entities;
    }
    private boolean retrieveGeneratedKey(Object entity, ResultSet resultSet, String fieldName) throws SQLException, InvocationTargetException, IllegalAccessException, IOException, NoSuchMethodException, InstantiationException {
        if(!resultSet.next()) return false;
        Class<?> clazz=entity.getClass();
        if (clazz.getSimpleName().contains("$$Enhancer")) clazz=clazz.getSuperclass();
        for (Field field:clazz.getDeclaredFields())
            if (field.getName().equals(fieldName)) {
                invokeSetter(entity, resultSet.getObject(1), field);
            }
        return true;
    }
    private void invokeSetter(Object entity, Object value, Field field) throws IllegalArgumentException, IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        Method setter= findSetter(field, ErrandBoy.getRealClass(entity));
        setter.invoke(entity, typeConverter.convertToJavaType(value, setter.getParameterTypes()[0]));
    }
    private boolean populateWithResult(Object entity, ResultSet resultSet, boolean fetchLOb, String... fieldNames) throws SQLException, InvocationTargetException, IllegalAccessException {
        try {
            boolean populateAllFields=(fieldNames != null ? fieldNames.length : 0) ==0;
            int i=1;
            if (!resultSet.next()) return false;
            for (Field field:ErrandBoy.getRealClass(entity).getDeclaredFields()){
                if (isNotMapped(field) | (isLOb(field) && !fetchLOb) | !populateAllFields && Arrays.stream(fieldNames).noneMatch(fieldName->field.getName()==fieldName)) continue;
                invokeSetter(entity,resultSet.getObject(getColumnName(field)),field);
            }
            return true;
        } catch (IOException | NoSuchMethodException | InstantiationException | IllegalArgumentException e) {
            throw new RuntimeException(e);
        }
    }
    private void populateSearchStatement(CompiledStatement statement, Map<String,Object> params){
        if (statement.getType() != CompiledStatement.StatementType.SELECT) throw new RuntimeException("ONLY QUERIES ARE ALLOWED WITH THIS METHOD");
        String page="0";
        String size="20";
        for (Map.Entry <String,Object> entry:params.entrySet()){
            String key= entry.getKey().replace("High", "").replace("Low", "");
            if (key.equals("page")) page= entry.getValue().toString();
            else if(key.equals("size")) size= entry.getValue().toString();
            else
                statement.where(key,typeConverter.convertToSqlType(entry.getValue(),null));
        }
        statement.page(Integer.parseInt(page),Integer.parseInt(size));
    }
    private void populateStatement(CompiledStatement statement, Object entity) throws InvocationTargetException, IllegalAccessException {
        Class<?> clazz=ErrandBoy.getRealClass(entity);
        for(Field field: clazz.getDeclaredFields()){
            if (isNotMapped(field)) continue;
            String columnName=getColumnName(field);
            Object value=typeConverter.convertToSqlType(findGetter(field, clazz).invoke(entity), null);
            if(field.isAnnotationPresent(Id.class) && statement.getType()!= CompiledStatement.StatementType.INSERT)
                statement.where(columnName, value);
            statement.param(columnName, value);
        }
    }
    private static boolean isLOb(Field field){
        return byte[].class.isAssignableFrom(field.getType()) | char[].class.isAssignableFrom(field.getType());
    }
    private static <T> T instantiate(Class<T> clazz,Object... ids){
        try {
            return clazz.getConstructor( Arrays.stream(ids).map(id->ensurePrimitive(id.getClass())).toArray(Class[]::new)).newInstance(ids);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
    private static Class<?> ensurePrimitive(Class<?> val) {
        Class<?> ret = val;
        if (ret.equals(Integer.class)) ret = int.class;
        else if (ret.equals(Boolean.class)) ret = boolean.class;
        return ret;
    }
    private static boolean isNotMapped(Field field){
        for (Class<? extends Annotation> notMappedFieldAnnotation : notMappedFieldAnnotations) {
            if (field.isAnnotationPresent(notMappedFieldAnnotation)) return true;
        }
        return false;
    }
    private static Method findGetter(Field field, Class<?> clazz){
        String pre;
       if (boolean.class.isAssignableFrom(field.getType())){
           pre="is";
       } else pre="get";
        try {
            return clazz.getDeclaredMethod(pre + ErrandBoy.firstLetterToUpperCase(field.getName()));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
    private static Method findSetter(Field field, Class<?> clazz){
        return Arrays.stream(clazz.getDeclaredMethods())
                .filter(method -> method.getName().matches("^(set)" + ErrandBoy.firstLetterToUpperCase(field.getName())))
                .findFirst().orElse(null);
    }
    private static String getColumnName(Field field) {
        Annotation annotation;
        if((annotation=field.getDeclaredAnnotation(OneToOne.class))!=null) return  ((OneToOne) annotation).joinColumn();
        else if ((annotation=field.getDeclaredAnnotation(OneToMany.class))!=null) return ((OneToMany) annotation).joinColumn();
        else if ((annotation=field.getAnnotation(ManyToOne.class))!=null) return ((ManyToOne) annotation).joinColumn();
        else return field.getName();
    }
}

package com.yusuf.simpleecommercesite.dbContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysql.cj.jdbc.exceptions.SQLError;
import com.yusuf.simpleecommercesite.dbContext.sqlUtils .SqlTypeConverter;
import javax.persistence.*;

import com.yusuf.simpleecommercesite.entities.annotations.AggregateMember;
import com.yusuf.simpleecommercesite.helpers.ErrandBoy;
import com.yusuf.simpleecommercesite.dbContext.sqlUtils.StatementBuilder;
import com.yusuf.simpleecommercesite.network.dtos.SearchResult;
import net.sf.cglib.proxy.*;
import org.apache.commons.dbcp2.BasicDataSource;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import javax.sql.DataSource;
import javax.validation.constraints.NotNull;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static java.lang.Math.pow;

public class DbContext{
    private static final Class<? extends Annotation>[] notMappedFieldAnnotations= new Class[]{OneToMany.class};
    private ObjectMapper mapper;
    private SqlTypeConverter typeConverter;
    private final static int CACHE_MAX_SIZE=500;
    private final Set<Object> idCache =Collections.synchronizedSet(new HashSet<>(2000));
//    private final Map<Map<String,Object>, List<Object>> searchCache=new ConcurrentHashMap<>();
    private final Map<Object, Set<String>> dirtMap=new ConcurrentHashMap<>();
    private final Map<Class<?>, Map<String,String>>  fieldNameMappings=new HashMap<>();
    private DataSource dataSource;
    private Connection connectionn;
    private String connectionString;
    private boolean connectionMode=false;
    private String schema;
    public DbContext(String connectionString, String userName, String password, String schema) throws SQLException {
        this.connectionString=connectionString;
        BasicDataSource ds=new BasicDataSource();
        ds.setUrl(connectionString);
        ds.setPassword(password);
        ds.setDefaultSchema(schema);
        ds.setUsername(userName);
        ds.setMinEvictableIdle(Duration.ofSeconds(5));
        ds.setMaxIdle(10);
        ds.setTestWhileIdle(false);
        this.setSchema(schema);
        ds.setTestOnBorrow(true);
        ds.setValidationQuery("SELECT 1");
        ds.setValidationQueryTimeout(4);
        ds.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
        ds.setPoolPreparedStatements(true);
        ds.setInitialSize(2500);
        try {
        ds.getConnection().close();
        } catch (SQLException e){
            throw new RuntimeException(e);
        }
//        connectionChecker.scheduleAtFixedRate(()->{
//            Thread[] threads = new Thread[Thread.activeCount()];
//            Thread.enumerate(threads);
//            connections.forEach((k,v)->{
//                if(Arrays.stream(threads).noneMatch(thread-> thread.getId()==k)) {
//                    try {
//                        v.close();
//                        connections.remove(k);
//                    } catch (SQLException e) {
//                        throw new RuntimeException(e);
//                    }
//                }
//            });
//        },5, 120, TimeUnit.SECONDS);
        this.dataSource=ds;
    }
    public DbContext(ObjectMapper mapper, String connectionString, String userName, String password, String schema) throws SQLException {
        this(connectionString, userName, password, schema);
        this.mapper = mapper;
        this.typeConverter=new SqlTypeConverter(this.mapper);
    }
    public DbContext(ObjectMapper mapper, DataSource dataSource) throws SQLException {
        this.mapper=mapper;
        this.typeConverter=new SqlTypeConverter(this.mapper);
        this.dataSource=dataSource;
        dataSource.getConnection().close();
    }
    public DbContext(ObjectMapper mapper, Connection connection){
        this.mapper=mapper;
        this.connectionn=connection;
        this.connectionMode=true;
        this.typeConverter=new SqlTypeConverter(this.mapper);
    }
    public void setSchema(String schema){
        this.schema=schema;
    }
    public void  useDatabase(String databaseName, String collation) throws SQLException{
        Statement statement= getConnection().createStatement();
                statement.execute("CREATE DATABASE IF NOT EXISTS "+ databaseName +" CHARACTER SET utf8mb4 "+ (!collation.equals("")?"COLLATE " + collation:"" ));
        statement.close();
        statement=getConnection().createStatement();
        String[] params= connectionString.split("/");
        int len=0,i=0;
        for (String param : params){
            if (i++ < params.length-1) len+=param.length();
        }
        String newString= connectionString.substring(0,len + i -1);
        newString += databaseName;
        ((BasicDataSource)this.dataSource).setUrl(newString);
        statement.execute("USE " + databaseName);
        statement.close();
    }
    public void executeFile(File file) throws SQLException, IOException {
        FileReader reader= new FileReader(file);
        int len = 0xFFFFFFF;
        int offset=0;
        char[] buffer= new char[len];
        int p=1;
        while (reader.read(buffer, offset,len)!= -1 ){
            int newLen =len*(int)pow(2,p++);
            char[] tmp=new char[newLen];
            offset+=len;
            System.arraycopy(buffer,0,tmp,0,offset);
            buffer=tmp;

        }
        String sql= String.valueOf(buffer);
        try(Connection connection=getConnection()){
            connection.createStatement().execute(sql);
        }
    }
    private <T> T[] copy(T[] src,T[] dest, int len) {
        if (len >= 0) System.arraycopy(src, 0, dest, 0, len);
        return dest;
    }
    private Object stripFromProxy(Object proxy){
        try {
        Object target= ErrandBoy.getRealClass(proxy.getClass()).getConstructor().newInstance();
        for (Field field : target.getClass().getDeclaredFields()){
            ErrandBoy.findSetter(field,target.getClass()).invoke(target, ErrandBoy.findGetter(field, target.getClass()).invoke(proxy));
        }
        return target;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
    public <T> T findById(Class<T> clazz, Object... ids) {
        ids= Arrays.stream(ids).map(id->{
            if (isProxy(id)) {
                return stripFromProxy(id);
            } else return id;
        }).toArray();
        T entity = instantiate(clazz, ids);
        try {
            return findAndFill(entity);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public void dropDatabase(String databaseName) throws SQLException{
        getConnection().createStatement().execute("DROP DATABASE IF EXISTS " + databaseName);
    }
    public @NotNull <T> SearchResult<T> search(Class<T> clazz , Map<String,Object> params) {
        if (params!=null &&!params.isEmpty()){
            params= replaceWithColumnNameAndParse(params, fieldNameMappings.containsKey(clazz)?fieldNameMappings.get(clazz):createColumnNameMapping(clazz));
        }
//        if (clazz.equals(Product.class) && this.searchCache.containsKey(params)) return (List<T>) this.searchCache.get(params);

        Connection connection= null;
        PreparedStatement[] statements = null;

        try {
            connection = getConnection();
            statements=buildSearchStatements(ErrandBoy.getRealClass(clazz), params, connection);
            ResultSet results=statements[0].executeQuery();
            List<T> entities= createWithResults(clazz, results);
            entities.replaceAll(this::toProxy);
            ResultSet countResult = statements[1].executeQuery();
            int count =0;
            if (!entities.isEmpty()) {
                countResult.next();
                count = countResult.getInt("count");
            }
//            this.searchCache.put(params, (List<Object>) entities);
//            if(this.searchCache.size()>CACHE_MAX_SIZE) {
//                synchronized (searchCache) {
//                    Map.Entry<Map<String, Object>, List<Object>> e = this.searchCache.entrySet().stream().findFirst().get();
//                    e.getValue().clear();
//                    this.searchCache.remove(e.getKey());
//                }
//            }
            return new SearchResult<>(count, entities);
        } catch (NoSuchMethodException | SQLException | InvocationTargetException | IllegalAccessException |
                 InstantiationException e) {
            assert statements != null;
            for (PreparedStatement statement : statements) {
                System.out.println(statement);
            }
            throw new RuntimeException(e);
        } finally {
            try {
                if (statements!=null) {
                    for (Statement statement : statements) {
                        if (statement != null) statement.close();
                    }
                }
                close(connection);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }
    public <T> T save(T entity, boolean... flags) throws SQLException {
        boolean includeDefaults = flags!=null && flags.length>0&& flags[0];
        if (isProxy(entity))
            update(entity,includeDefaults);
        else {
            try {
                entity = insert(entity);
            } catch (SQLException e) {
                if (Objects.equals(e.getSQLState(), "23505") || Objects.equals(e.getSQLState(), "23502"))
                    update(entity, includeDefaults);
                else {
                    System.err.println("ErrCode: "+ e.getSQLState());
                    throw e;
                }
            }
        }
        flushAggragates(entity);
        return  entity;
    }

    public <T> T merge(T e){
        if (!isProxy(e))
            e=toProxy(e);
        removeFromCache(e);
        try {
            e= findAndFill(e);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        return e;
    }
    public <T> T insert(T entity) throws SQLException {
        Connection connection=null;
        PreparedStatement statement = null;
        try {
            List<Field> aiField=ErrandBoy.getAnnotatedFields(entity.getClass(), GeneratedValue.class);
            aiField= aiField.stream().filter(field -> field.getAnnotation(GeneratedValue.class).strategy()==GenerationType.IDENTITY).collect(Collectors.toList());
            connection= getConnection();
            statement=buildStatement(entity, StatementBuilder.StatementType.INSERT, connection, false);
            statement.executeUpdate();
            if(aiField!=null)
                for (Field field : aiField) {
                    retrieveGeneratedKey(entity, statement.getGeneratedKeys(), field.getName());
                }
            entity=toProxy(entity);
            removeFromCache(entity);
            return entity;
        } catch (SQLException e){
            if (!Objects.equals(e.getSQLState(), "23505"))
                System.err.println("Statement:" +statement.toString());
            throw e;
        }catch (InvocationTargetException | IllegalAccessException | IOException | NoSuchMethodException |
                 InstantiationException  e) {
            e.printStackTrace(System.err);
            throw new RuntimeException(e);
        }finally {
            if(statement!=null)
                statement.close();
            close(connection);
        }
    }
    private void removeFromCache(Object entity) {
        flushAggragates(entity);
        idCache.remove(entity);
    }
    private void flushAggragates(Object entity){
        Class<?> clazz= ErrandBoy.getRealClass(entity.getClass());
        if (clazz.isAnnotationPresent(AggregateMember.class)) {
            Class<?> aggregateOwnerClass=clazz.getAnnotation(AggregateMember.class).in();
            for(Method method: ErrandBoy.getRealClass(entity).getDeclaredMethods() ){
                if(method.getReturnType().isAssignableFrom(aggregateOwnerClass)){
                    Object aggragateOwner = null;
                    try {
                        aggragateOwner = method.invoke(entity);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                    idCache.remove(aggragateOwner);
                }
            }
        }
    }
    public int update(Object entity, boolean updateNull) throws SQLException {
        if (!isProxy(entity))
            entity=toProxy(entity);
        removeFromCache(entity);
        PreparedStatement statement = null;
        Connection connection=null;
        try{
            connection= getConnection();
            statement= buildStatement(entity, StatementBuilder.StatementType.UPDATE, connection, updateNull);
            int res= statement.executeUpdate();
            return res;
        }catch (SQLException e){
            if (!Objects.equals(e.getSQLState(), "23505"))
                System.err.println("Statement:" +statement.toString());
            throw e;
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        } finally {
            if(statement!=null)
                statement.close();
            close(connection);
        }
    }
    <T> T findAndFill(T entity) throws SQLException {
        if (!isProxy(entity))
            entity= toProxy(entity);
        Connection connection= null;
        PreparedStatement statement=null;
        if (this.idCache.contains(entity)) return getFromCache(entity);
        try {
            connection= getConnection();
            statement= buildStatement(entity, StatementBuilder.StatementType.SELECT, connection, false);
            ResultSet rs=statement.executeQuery();
            if(!populateWithResult(entity, rs, false))
                return null;
        } catch (SQLException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace(System.out);
            throw new RuntimeException(e);
        } finally {
            if(statement!=null)
                statement.close();
            close(connection);
        }
        this.idCache.add(entity);
        if(this.idCache.size()>CACHE_MAX_SIZE) {
            synchronized (idCache) {
                removeFromCache(idCache.stream().findFirst().get());
            }
        }
        return entity;
    }


    private static class AnnotationAdder extends ClassVisitor{

        public AnnotationAdder(int api) {
            super(api);
        }

        public AnnotationAdder(int api, ClassVisitor classVisitor) {
            super(api, classVisitor);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            AnnotationVisitor visitor = visitAnnotation("javax/persistence/Entity;",true);
            super.visit(version, access, name, signature, superName, interfaces);
        }
    }
    private <T> T toProxy(T entity){
        Enhancer proxy= new Enhancer();
        proxy.setSuperclass(entity.getClass());
        proxy.setCallback(new EntityMethodInterceptor(this, entity));
        dirtMap.put(entity, new CopyOnWriteArraySet<>());
        entity= (T) proxy.create();
        return entity;
    }
    private boolean isProxy(Object e){
        return e.getClass().getSimpleName().contains("$$Enhancer");
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
    private static Map<String, Object> replaceWithColumnNameAndParse(Map<String, Object> params, Map<String, String> mapping){
        Map<String, Object> ret=new HashMap<>();
        for (Map.Entry<String, Object> param: params.entrySet()){
            String columnName=mapping.get(param.getKey());
            Object val = param.getValue();
            try{
               val= Integer.parseInt(val.toString());
            } catch (NumberFormatException e){}
            if (!(val instanceof Number))try {
                val = Double.parseDouble(val.toString());
            } catch (NumberFormatException e){}
            ret.put(columnName!=null?columnName:param.getKey(), val);
        }
        return ret;
    }
    private void close(Connection connection) throws SQLException {
        if (!connectionMode)
            if(connection!=null)
                connection.close();
    }
    public <T> T fetchLOb(T entity, String fieldName){
        Connection connection=null;
        PreparedStatement statement=null;
         try {
            connection=getConnection();
            statement=buildStatement(entity, StatementBuilder.StatementType.SELECT, connection, false);
            populateWithResult(entity, statement.executeQuery(), true, fieldName);

            return entity;
        } catch (SQLException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }finally {
             try {
                 if(statement!=null)
                     statement.close();
                 close(connection);
             } catch (SQLException e){throw new RuntimeException(e);}
         }
    }
    public <T> T saveWithNulls(T entity) throws SQLException {
        if(update(entity, true)==0)
            insert(entity);
        idCache.remove(entity);
        idCache.add(entity);

        return  entity;
    }

    public int remove(Object entity){
        try {
            Connection connection=getConnection();
            PreparedStatement statement= buildStatement(entity, StatementBuilder.StatementType.DELETE, connection, false);
            int res=statement.executeUpdate();
            statement.close();
            close(connection);
            if(!isProxy(entity)) entity=toProxy(entity);
            removeFromCache(entity);
            return res;
        } catch (SQLException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    private Map<Long, Connection> connections = new ConcurrentHashMap<>();
    private ScheduledExecutorService connectionChecker = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {

        @Override
        public Thread newThread(Runnable r) {
            Thread thread= new Thread(r);
            thread.setDaemon(true);
            return thread;
        }
    });
    private Connection getConnection() throws SQLException {
        if (this.dataSource!=null) {
//            Connection connection = connections.get(Thread.currentThread().getId());
//            if (connection==null ||!connection.isValid(5)){
//                connection=dataSource.getConnection();
//                connections.put(Thread.currentThread().getId(), connection);
//            }
//            return connection;
        return dataSource.getConnection();
        }
        else if (this.connectionn!=null) return this.connectionn;
        else throw new RuntimeException("NO CONNECTION SOURCE");
    }
    public void addDirtyField(Object entity, String fieldName){
        dirtMap.get(entity).add(fieldName);
    }
    private synchronized <T> T getFromCache(T entity){
        for (Object cached : idCache) {
            if (entity.equals(cached)) return (T) cached;
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
        Class<?> clazz = ErrandBoy.getRealClass(entity);
        for (Field field:clazz.getDeclaredFields())
            if (field.getName().equals(fieldName)) {
                invokeSetter(entity, resultSet.getObject(1), field);
            }
        return true;
    }
    private void invokeSetter(Object entity, Object value, Field field) {
        try {
            Method setter= ErrandBoy.findSetter(field, ErrandBoy.getRealClass(entity));
            setter.invoke(entity, typeConverter.convertToJavaType(value, setter.getParameterTypes()[0]));
        } catch (IOException | InvocationTargetException | IllegalAccessException | NoSuchMethodException |
                 InstantiationException e) {
            throw new RuntimeException(e);
        }
    }
    private boolean populateWithResult(Object entity, ResultSet resultSet, boolean fetchLOb, String... fieldNames) {
        boolean populateAllFields=(fieldNames != null ? fieldNames.length : 0) ==0;

        int i=1;
        try {
            if (!resultSet.next()) return false;
            for (Field field : ErrandBoy.getRealClass(entity).getDeclaredFields()) {
                if (isNotMapped(field) || (isLOb(field) && !fetchLOb) || (!populateAllFields && Arrays.stream(fieldNames).noneMatch(fieldName -> field.getName().equals(fieldName))))
                    continue;
                invokeSetter(entity, resultSet.getObject(getColumnName(field).toLowerCase(Locale.ENGLISH)), field);
            }
            return true;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    private static String getTableName(Class<?> clazz, StatementBuilder.StatementType type){
        SecondaryTable view = clazz.getAnnotation(SecondaryTable.class);
        Table table = clazz.getAnnotation(Table.class);
        if (view!=null && type == StatementBuilder.StatementType.SELECT) return view.name();
        else if (table!=null) return table.name();
        else return clazz.getSimpleName();
    }
    public static Field[] getMappedFields(Class<?> entityClass){
        return Arrays.stream(entityClass.getDeclaredFields()).filter(field ->!isNotMapped(field)).toArray(Field[]::new);
    }
    private PreparedStatement[] buildSearchStatements(Class<?> entityClass ,Map<String,Object> params, Connection connection) throws SQLException {
        StatementBuilder builder=new StatementBuilder(StatementBuilder.StatementType.SELECT);
        String tableName = getTableName(entityClass, StatementBuilder.StatementType.SELECT);
        builder.table(schema!=null? schema + "." + tableName : (tableName) );
        int page=0;
        int size=20;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            Object value=typeConverter.convertToSqlType(entry.getValue(), getConnection());
            if (entry.getKey().equals("page")) page = Integer.parseInt(value.toString());
            else if (entry.getKey().equals("size")) size = Integer.parseInt(value.toString());
            else {
                String key;
                if (entry.getKey().endsWith("High"))
                    builder.where(key = entry.getKey().replace("High", ""), value, "<");
                else if (entry.getKey().endsWith("Low"))
                    builder.where(key = entry.getKey().replace("Low", ""), value, ">");
                else if (entry.getKey().endsWith("Desc"))
                    builder.order(key = entry.getKey().replace("Desc", ""), true);
                else if (entry.getKey().endsWith("Asc"))
                    builder.order(key = entry.getKey().replace("Asc", ""), false);
                else builder.where(key = entry.getKey(), value);
            }
        }
        for (Field field: entityClass.getDeclaredFields()) {
            if (!isNotMapped(field)) {
                builder.columns(getColumnName(field));
            }
        }
        builder.page(page,size);
        PreparedStatement[] ret = new PreparedStatement[2];
        ret[0] = builder.build(connection);
        builder.reset("WHERE");
        builder.columns("count(*) as count");
        ret[1]  = builder.build(connection);
        return ret;
    }
    public void clear(){
        synchronized (idCache) {
            this.idCache.clear();
        }
        synchronized (dirtMap) {
            this.dirtMap.forEach((key, value) -> value.clear());
            this.dirtMap.clear();
        }
//        synchronized (searchCache) {
//            this.searchCache.forEach((key, value) -> value.clear());
//            this.searchCache.clear();
//        }
    }
    private static boolean isDefault(Object val){
        return (val == null || val.toString().trim().isEmpty() || ((val instanceof Boolean)) && ((Boolean) val).booleanValue()) || (val instanceof Number && ((Number)val).equals(0));
    }
    private PreparedStatement buildStatement(Object entity, StatementBuilder.StatementType type, Connection connection, boolean defaults) throws InvocationTargetException, IllegalAccessException, SQLException {
        Class<?> clazz=ErrandBoy.getRealClass(entity);
        StatementBuilder builder=new StatementBuilder(type, defaults);
        String tableName = getTableName(clazz,type);
        builder.table(schema!=null? schema + "." + tableName : (tableName));
        for(Field field: clazz.getDeclaredFields()){
            if (isNotMapped(field)) continue;
            String columnName=getColumnName(field);
            Object value=typeConverter.convertToSqlType(ErrandBoy.findGetter(field, clazz).invoke(entity), connection);
            if (StatementBuilder.StatementType.INSERT==builder.getType() && field.isAnnotationPresent(GeneratedValue.class))
                continue;
            if(field.isAnnotationPresent(Id.class) && builder.getType()!= StatementBuilder.StatementType.INSERT)
                builder.where(columnName, value);
            builder.param(columnName, value);
        }
        return builder.build(connection);
    }
    private static boolean isLOb(Field field){
        return byte[].class.isAssignableFrom(field.getType()) | char[].class.isAssignableFrom(field.getType());
    }
    private <T> T instantiate(Class<T> clazz, Object... ids){
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
    private static boolean isReadOnlY(Field field){
        return field.isAnnotationPresent(GeneratedValue.class);
    }

    private static String getColumnName(Field field) {
        Annotation annotation;
        if((annotation=field.getAnnotation(Column.class))!=null && !Objects.equals(((Column) annotation).name(), "")) return ((Column) annotation).name();
        else return field.getName();
    }
}

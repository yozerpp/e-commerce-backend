package com.may.simpleecommercesite.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.may.simpleecommercesite.annotations.Id;
import com.may.simpleecommercesite.annotations.SecondId;
import com.may.simpleecommercesite.beans.DBService;
import com.may.simpleecommercesite.beans.EntityFactory;
import com.may.simpleecommercesite.helpers.ErrandBoy;
import org.apache.commons.lang.StringEscapeUtils;

import javax.sql.DataSource;
import javax.swing.plaf.nimbus.State;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Entity {
    private boolean fetched;
    private static DataSource dataSource;
    private Connection connection;
    boolean isFetched(){
        return fetched;
    }
    public void setFetched(){
        this.fetched=true;
    }

    public Entity fetch() {
        if (this.fetched) {
            return this;
        }
        Map.Entry<Field, Object> primaryKey= getBasePrimaryKey().entrySet().stream().findFirst().get();
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + this.getClass().getSimpleName() + " WHERE " + primaryKey.getKey().getName() + "=?")) {
            PreparedStatement.class.getDeclaredMethod("set" + ErrandBoy.firstLetterToUpperCase(primaryKey.getKey().getType().getSimpleName()),
                    int.class, primaryKey.getKey().getType()).invoke(statement, 1, primaryKey.getValue());
            ResultSet rs = statement.executeQuery();
            if (!rs.next()) {
                return null;
            }
            for (Field field : this.getClass().getDeclaredFields()) {
                if (field.getName().endsWith("Dirty")) {
                    continue;
                }

                if (byte[].class.isAssignableFrom(field.getType())) {
//                    Blob locator = rs.getBlob(field.getName());
//                    if (locator!=null){
//                        byte[] buf = new byte[Math.toIntExact(locator.length())];
//                        InputStream stream = locator.getBinaryStream();
//                        stream.read(buf);
//                        field.set(this, buf);
//                        stream.close();
//                        locator.free();
                } else if (Entity.class.isAssignableFrom(field.getType())) {
                    Class<?> subEntityPkType = getBasePrimaryKeyType((Class<? extends Entity>) field.getType());
                    String subEntityPkTypeName = subEntityPkType.getSimpleName();
                    Object subEntityPk = ResultSet.class.getDeclaredMethod("get" + ErrandBoy.firstLetterToUpperCase(subEntityPkTypeName), String.class).invoke(rs, field.getName()); // requires basePK and associated object field to have the same name
                    Entity subEntity = (Entity) EntityFactory.class.getDeclaredMethod(field.getType().getSimpleName().toLowerCase(), subEntityPkType).invoke(null, subEntityPk);
                    field.set(this, subEntity);
                } else {
                    Class<?> fieldType = field.getType();
                    String fieldTypeName = fieldType.getSimpleName();
                    Object fieldValue = ResultSet.class.getDeclaredMethod("get" + ErrandBoy.firstLetterToUpperCase(fieldTypeName), String.class).invoke(rs, field.getName());
                    field.set(this, fieldValue);
                }
            }
            this.fetched = true;
        }
         catch(SQLException | ArithmeticException | NoSuchMethodException |
               InvocationTargetException | IllegalAccessException e){
            throw new RuntimeException(e);
        }
        return this;
    }
    public Entity commit() {
        Class<? extends Entity> entity=this.getClass();
        String className=entity.getSimpleName();
        // Unbatchable, bad performance but jdbc offers no way to execute different statements in an optimized way
        List<PreparedStatement> statements=new ArrayList<>();
        Savepoint savepoint= null;
        try { savepoint = connection.setSavepoint();} catch (SQLException e) {throw new RuntimeException(e);}
        try (Statement statement=connection.createStatement()) {
            Map.Entry<Field, Object> primaryKey = this.getBasePrimaryKey().entrySet().stream().findFirst().get();
            connection.setAutoCommit(false);
            for (Field dirtyField : entity.getDeclaredFields()) {
                Field field;
                if (dirtyField.getName().endsWith("Dirty") && (boolean) dirtyField.get(this)) {
                    field = entity.getField(dirtyField.getName().replace("Dirty", ""));
                    String fieldTypeName = field.getType().getSimpleName();
                    String methodParamTypeName;
//                    Class<?> methodParamType = null;
                    Object methodParam = null;
                    Statement statement1;
                    if (byte[].class.isAssignableFrom(field.getType())) {
//                        Blob upload = connection.createBlob();
//                        OutputStream stream = upload.setBinaryStream(1);
//                        stream.write((byte[]) field.get(this));
//                        methodParamType = upload.getClass();
//                        methodParam = upload;
//                        stream.close();
                    } else if (Entity.class.isAssignableFrom(field.getType())) {
                        Map.Entry<Field, Object> foreignKey = this.getBasePrimaryKey().entrySet().stream().findFirst().get();
//                        methodParamType = foreignKey.getKey().getType();
                        methodParam = foreignKey.getValue();
                    } else {
//                        methodParamType = field.getType();
                        methodParam = field.get(this);
                    }
//                    methodParamTypeName = methodParamType.getSimpleName().equals("int") ? "Int" : methodParamType.getSimpleName();
//                    String sql="UPDATE " + className + " SET " + field.getName() + "=" + methodParam + " WHERE " + primaryKey.getKey().getName() + "="
                    dirtyField.set(this, false);
                }

            }
//            if (!statements.isEmpty()) {
//                for (PreparedStatement statement: statements){
//                    statement.executeUpdate();
//                }
//                connection.commit();
//            }

        } catch (SQLException | IllegalAccessException |
                 NoSuchFieldException e){
            try {
                if (savepoint!=null) {
                    connection.rollback(savepoint);
                }
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
            throw new RuntimeException(e);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return this;
    }
    // I completely gave up on avoiding injection (I would have to write a 10 cased switch to avoid injection here!)
    public Entity persist() throws SQLException{
        Map<String, Object> params=new HashMap<>();
        Field pkField = null;
         try (Statement statement=connection.createStatement()){
            for (Field field:this.getClass().getDeclaredFields()){
                Object value = null;
                if (field.getName().endsWith("Dirty") | (value=field.get(this))==null) continue;
                if (field.isAnnotationPresent(Id.class)) pkField=field;
                if (byte[].class.isAssignableFrom(field.getType())) {
//                    hasByteArr=true;
//                    this.getClass().getField(field.getName() + "Dirty").set(this, true);
                } else if (Entity.class.isAssignableFrom(field.getType())) {

                    Map.Entry<Field, Object> primaryKey = ((Entity) field.get(this)).getBasePrimaryKey().entrySet().stream().findFirst().get();
                    value = primaryKey.getValue();
                }
                params.put(field.getName(), value);
            }
            try {
                statement.executeUpdate(DBService.createStatementString(this.getClass().getSimpleName(), params, DBService.StatementType.INSERT), new String[]{pkField.getName()});
                pkField.set(this, statement.getGeneratedKeys().getObject(pkField.getName()));
            } catch (SQLException e){
                if(e.getSQLState().equals("S0022"));
                else throw new RuntimeException(e);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return this;
    }
    public void remove(){
        Map<Field, Object> primaryKeys=new HashMap<>();
        StringBuilder sql= new StringBuilder("DELETE FROM ").append(this.getClass().getSimpleName()).append(" WHERE ");
        boolean firstMatch=true;
        try {
            for (Field field : this.getClass().getDeclaredFields()) {
                if (field.getName().endsWith("Dirty")) continue;
                if (field.isAnnotationPresent(SecondId.class) | field.isAnnotationPresent(Id.class)) {
                    if (firstMatch) firstMatch = false;
                    else sql.append(" AND ");
                    if (Entity.class.isAssignableFrom(field.getType())) {
                        primaryKeys.putAll(((Entity) field.get(this)).getBasePrimaryKey());
                    } else {
                        primaryKeys.put(field, field.get(this));
                    }
                    sql.append(field.getName()).append("=?");
                }
            }
        } catch (IllegalAccessException e){
            throw new RuntimeException(e);
        }
        try(PreparedStatement statement=connection.prepareStatement(sql.toString())){
            int i=1;
            for (Map.Entry<Field, Object> primaryKey: primaryKeys.entrySet()){
                statement.getClass().getDeclaredMethod("set" + ErrandBoy.firstLetterToUpperCase(primaryKey.getValue().getClass().getName()), int.class, primaryKey.getValue().getClass())
                        .invoke(statement, i, primaryKey.getValue());
                i++;
            }
            statement.executeUpdate();
            this.connection.close();
        } catch (SQLException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e){
            throw new RuntimeException(e);
        }
    }

    public void setDataSource(DataSource ds) {
        dataSource = ds;
        try {
            this.connection=dataSource.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public void clean() {
        for (Field field : this.getClass().getDeclaredFields()) {
            try {
                if (field.getName().endsWith("Dirty") && (boolean) field.get(this)) {
                    field.set(this, false);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }
    public static void invokeSetters(Object entity, Map<String, Object> fields){
        for (Map.Entry<String, Object> entry : fields.entrySet()){
            if (entry.getValue()!=null){
                Class<?> fieldType= null;
                try {
                    fieldType = entity.getClass().getField(entry.getKey()).getType();
                    RegisteredCustomer.class.getDeclaredMethod("set" + ErrandBoy.firstLetterToUpperCase(entry.getKey()), fieldType)
                            .invoke(entity, fieldType.cast(entry.getValue()));
                } catch (IllegalAccessException | NoSuchFieldException | InvocationTargetException |
                         NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
    @JsonIgnore
    public Map<Field, Object> getBasePrimaryKey()  {
        Map<Field, Object> basePrimaryKey=new HashMap<>();
        try {
                Field pkField=getAnnotatedFields(this.getClass(), Id.class).get(0);
                Object val=pkField.get(this);
                if(val!=null){
                    if (Entity.class.isAssignableFrom(pkField.getType())) {
                        basePrimaryKey.putAll(((Entity) val).getBasePrimaryKey());
                    } else {
                        basePrimaryKey.put(pkField, val);
                    }
                }
        } catch (IllegalAccessException e){
            throw new RuntimeException(e);
        }
        return basePrimaryKey;
    }
    public static Class<?> getBasePrimaryKeyType(Class<? extends Entity> c){
        Class<?> type=getAnnotatedFields(c, Id.class).get(0).getType();
        type=Entity.class.isAssignableFrom(type)?getBasePrimaryKeyType((Class<? extends Entity>) type):type;
        return type;
    }
    public static List<Field> getAnnotatedFields(Class <?> entity, Class<? extends Annotation> annotation) {
        List<Field> pk=new ArrayList<>();
        for (Field field : entity.getDeclaredFields()) {
            if (field.isAnnotationPresent(annotation)) {
                pk.add(field);
            }
        }
        return pk;
    }
}

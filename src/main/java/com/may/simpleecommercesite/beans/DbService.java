package com.may.simpleecommercesite.beans;

import com.may.simpleecommercesite.annotations.Embedded;
import com.may.simpleecommercesite.annotations.Id;
import com.may.simpleecommercesite.annotations.OneToMany;
import com.may.simpleecommercesite.annotations.Relation;
import com.may.simpleecommercesite.entities.Entity;
import com.may.simpleecommercesite.helpers.ErrandBoy;
import com.may.simpleecommercesite.helpers.Json;

import javax.sql.DataSource;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.sql.*;
import java.util.*;

abstract class DbService {
    private static final int MAX_RETRIEVED_KEY_COUNT =3;
    protected static DataSource dataSource;

    public static void setDataSource(DataSource ds) {
        dataSource = ds;
    }

    public static Connection getConnection() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public static <T> List<T> search(Class<T> clazz, Map<String, Object> params, boolean fetchLob, Connection connection) {
        List<T> entities = new ArrayList<>();
        try (PreparedStatement statement = prepareStatement(clazz.getSimpleName(), params, StatementType.QUERY, connection)) {
            Field oneToMany = null;
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                T entity = clazz.getConstructor().newInstance();
                for (Field field : clazz.getDeclaredFields()) {
                    Object val = null;
                    String fieldName = field.getName();
                    String fieldTypeName = field.getType().getSimpleName();
                    Class<?> fieldType = field.getType();
                    if (byte[].class.isAssignableFrom(field.getType())) {
                        if (fetchLob) {
                            Blob locator = rs.getBlob(fieldName);
                            if (locator != null) {
                                InputStream in = locator.getBinaryStream();
                                byte[] buf = new byte[Math.toIntExact(locator.length())];
                                in.read(buf);
                                in.close();
                                locator.free();
                                val = buf;
                            }
                        }
                    } else if (field.isAnnotationPresent(Relation.class)) {
                        Class<?> subEntitypkType = Entity.getBasePrimaryKeyType((Class<? extends Entity>) field.getType());
                        Object subEntityPk = rs.getClass().getDeclaredMethod("get" + ErrandBoy.firstLetterToUpperCase(subEntitypkType.getSimpleName()), String.class).invoke(rs, field.getAnnotation(Relation.class).joinColumn());
                        if (subEntityPk != null)
                            val = EntityFactory.class.getDeclaredMethod(fieldTypeName.toLowerCase(), subEntitypkType).invoke(null, subEntityPk);
                    } else if (fieldType.isEnum()) {
                        val = fieldType.getMethod("valueOf", String.class).invoke(null, (String) rs.getClass().getDeclaredMethod("get" + ErrandBoy.firstLetterToUpperCase(String.class.getSimpleName()), String.class).invoke(rs, fieldName));
                    } else if (field.isAnnotationPresent(OneToMany.class)) {
                        oneToMany = field;
                    } else if (fieldType.isAnnotationPresent(Embedded.class)) {
                        String json = rs.getString(fieldName);
                        val = Json.instantiate(new StringReader(json), fieldType);
                    } else {
                        val = rs.getClass().getDeclaredMethod("get" + ErrandBoy.firstLetterToUpperCase(fieldTypeName), String.class).invoke(rs, fieldName);
                    }
                    if (val != null)
                        entity.getClass().getDeclaredMethod("set" + ErrandBoy.firstLetterToUpperCase(fieldName), field.getType()).invoke(entity, val.getClass().equals(Integer.class) ? ((Integer) val).intValue() : val);
                }
                if (oneToMany != null) {
                    Object joinColumnValue = entity.getClass().getDeclaredMethod("get" + ErrandBoy.firstLetterToUpperCase(Entity.getAnnotatedField(entity.getClass(), Id.class).getName())).invoke(entity);
                    List<Object> val = (List<Object>) search((Class<?>) ((ParameterizedType) oneToMany.getGenericType()).getActualTypeArguments()[0], Map.of(oneToMany.getAnnotation(OneToMany.class).joinColumn(), joinColumnValue), fetchLob, connection);
                    entity.getClass().getDeclaredMethod("set" + ErrandBoy.firstLetterToUpperCase(oneToMany.getName()), oneToMany.getType()).invoke(entity, val);
                }
                entities.add(entity);
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | IllegalArgumentException |
                 IOException | SQLException | InstantiationException e) {
            throw new RuntimeException(e);
        }
        return entities;
    }
    public static int update(String tableName, Map<String,Object> updateColumns, Connection connection){
        try (PreparedStatement statement=prepareStatement(tableName, updateColumns, StatementType.UPDATE, connection)){
           return statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public static Map<String,Object> persist(String tableName, Map<String, Object> insertColumns, Connection connection, String... idColumnNames){
        try(PreparedStatement statement=prepareStatement(tableName, insertColumns, StatementType.INSERT,connection)) {
            statement.executeUpdate();
            ResultSet rs=statement.getGeneratedKeys();
            if (idColumnNames==null) return null;
            Map<String,Object> ids=new HashMap<>(idColumnNames.length);
            if(!rs.next()) throw new RuntimeException("NO KEYS RETURNED ALTHOUGH REQUESTED");
            for (String columnName: idColumnNames){
                ids.put(columnName, rs.getObject(columnName));
            }
            return ids;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public static int remove(String tableName, Map<String, Object> columns, Connection connection){
        try (PreparedStatement statement=prepareStatement(tableName, columns, StatementType.DELETE, connection)){
           return statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    private static boolean isFieldValuePresent(Field field, Object entity){
        return entity.getClass().getMethod("get" + field.getName()).invoke(entity)!=null;
    }
    private static String createStatementTemplate(String table, DbService.StatementType type, Set<String> fieldNames) {
        fieldNames = new LinkedHashSet<>(fieldNames);
        StringBuilder firstParam = new StringBuilder();
        StringBuilder lastParam = new StringBuilder();
        String command = null;
        String separator = type == StatementType.INSERT | type == StatementType.UPDATE ? ", " : " AND ";
        boolean firstRun = true;
        for (String fieldName : fieldNames) {
            boolean fEmpty = firstParam.isEmpty();
            boolean lEmpty = lastParam.isEmpty();
            if (!fEmpty) firstParam.append(separator);
            if (type == StatementType.UPDATE) {
                if (lEmpty) lastParam.append(fieldName).append("=").append("?");
                else {
                    if (fEmpty) firstParam.append("SET ");
                    firstParam.append(fieldName).append("=").append("?");
                }
            } else {
                if (!lEmpty) lastParam.append(separator);
                if (type == StatementType.INSERT) firstParam.append(fieldName);
                else {
                    String comparator;
                    if (fieldName.equals("page") | fieldName.equals("size")) continue;
                    if (fieldName.endsWith("High")) {
                        fieldName = fieldName.replace("High", "");
                        comparator = "<";
                    } else if (fieldName.endsWith("Low")) {
                        fieldName = fieldName.replace("Low", "");
                        comparator = ">";
                    } else comparator = "=";
                    lastParam.append(fieldName).append(comparator);
                }
                lastParam.append("?");
            }
        }
        switch (type) {
            case INSERT:
                firstParam = new StringBuilder("(").append(firstParam).append(")");
                lastParam = new StringBuilder("VALUES(").append(lastParam).append(")");
                command = "INSERT INTO ";
                break;
            case QUERY:
                if (!lastParam.isEmpty()) lastParam = new StringBuilder("WHERE ").append(lastParam);
                lastParam.append(" LIMIT ").append("?").append(", ").append("?");
                command = "SELECT * FROM ";
                break;
            case DELETE:
                if (!lastParam.isEmpty()) lastParam = new StringBuilder("WHERE ").append(lastParam);
                command = "DELETE FROM ";
                break;
            case UPDATE:
                lastParam = new StringBuilder(" WHERE ").append(lastParam);
                command = "UPDATE ";
        }
        String sql = command + table + " " + firstParam + lastParam;
        return sql;
    }

    private static void setStatementParam(PreparedStatement statement, Object param, int index) throws SQLException {
        if (param == null) {
            statement.setNull(index, Types.NULL);
            return;
        }
        Class<?> paramType = param.getClass();
        if (Entity.class.isAssignableFrom(paramType)) {
            param = ((Entity) param).getBasePrimaryKey().getValue();
            paramType = param.getClass();
        } else if (byte[].class.isAssignableFrom(paramType)) {
            Blob locator = statement.getConnection().createBlob();
            try {
                locator.setBinaryStream(1).write((byte[]) param);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            param = locator;
            paramType = param.getClass();
        } else if (paramType.isEnum()) {
            param = param.toString();
            paramType = String.class;
        } else if (paramType.isAnnotationPresent(Embedded.class)) {
            Writer json = new StringWriter();
            Json.serialize(param, json);
            paramType = String.class;
            param = json.toString();
        }
        try {
            statement.getClass().getDeclaredMethod("set" + ErrandBoy.firstLetterToUpperCase(paramType.getSimpleName()), int.class, paramType.equals(Integer.class) ? int.class : paramType).invoke(statement, index, param);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private static PreparedStatement prepareStatement(String table, Map<String, Object> params, DbService.StatementType type, Connection connection) {
        if (params == null) params = new HashMap<>();
        int size = 20;
        int page = 0;
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(createStatementTemplate(table, type, params.keySet()), Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            for (Map.Entry<String, Object> param : params.entrySet()) {
                if (type == StatementType.QUERY && param.getKey().equals("page")) page = (Integer) param.getValue();
                else if (type == StatementType.QUERY && param.getKey().equals("size"))
                    size = (Integer) param.getValue();
                else if (type == StatementType.UPDATE && i++ == 1)
                    setStatementParam(statement, param.getValue(), params.size());
                else setStatementParam(statement, param.getValue(), i++);
            }
            if (type == StatementType.QUERY) {
                statement.setInt(i++, page * size);
                statement.setInt(i, size);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return statement;
    }

    public enum StatementType {
        QUERY,
        INSERT,
        UPDATE,
        DELETE
    }
}

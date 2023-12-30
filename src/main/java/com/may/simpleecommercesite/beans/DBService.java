package com.may.simpleecommercesite.beans;

import com.fasterxml.jackson.core.JsonFactory;
import com.may.simpleecommercesite.annotations.Id;
import com.may.simpleecommercesite.entities.Entity;
import com.may.simpleecommercesite.entities.Product;
import com.may.simpleecommercesite.helpers.ErrandBoy;
import org.apache.commons.lang.StringEscapeUtils;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.codecs.MySQLCodec;
import org.owasp.esapi.configuration.consts.EsapiConfiguration;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class DBService implements Serializable{
    DataSource dataSource;
    private Connection connection;
    SqlLogger logger;
    public DBService(){}
    public DBService(DataSource ds) {
        this.dataSource=ds;
        try {
            connection=dataSource.getConnection(connectparams[1], connectparams[2]);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        this.logger=new SqlLogger();
    }
    public void destroy() {
        try{
        connection.close();
        } catch (SQLException ignored){}
    }
    public Map.Entry<String, Object> newEntity(Class<?> entity){
        Object insertId=null;
        String sql=createStatementString(entity.getSimpleName() ,null, StatementType.INSERT);
        String pkFieldName=Entity.getAnnotatedFields(entity, Id.class).get(0).getName();
        try (Statement statement=connection.createStatement()){
            statement.executeUpdate(sql, new String[]{pkFieldName});
            ResultSet rs= statement.getGeneratedKeys();
            if(rs.next()) {
                insertId = rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.log(SqlLogger.SqlMethodType.INSERT, e);
        }
        return Map.of(pkFieldName, insertId).entrySet().stream().findFirst().get();
    }
    public List<?> byFields(Class<?> clazz, Map<String, Object> params, boolean fetchLob) {
        List<Entity> entities = new ArrayList<>();
        try (Statement statement=connection.createStatement()){
            Object pk = null;
            String pkFieldName=null;
            Class<?> pkType = null;
//            statement.getClass().getDeclaredMethod("set"+ pkTypeName, int.class, pkType).invoke(statement,1, pk.getClass().equals(Integer.class)?((Integer) pk).intValue():pk);
            ResultSet rs=statement.executeQuery(createStatementString(clazz.getSimpleName(), params, StatementType.QUERY));
            while(rs.next()){
                Object entity= EntityFactory.class.getDeclaredMethod(clazz.getSimpleName().toLowerCase()).invoke(null);
                for (Field field: clazz.getDeclaredFields()) {
                    if (field.getName().endsWith("Dirty")) continue;
                    Object val = null;
                    String fieldName=field.getName();
                    String fieldTypeName=field.getType().getSimpleName();
                    if(byte[].class.isAssignableFrom(field.getType())) {
                        if(fetchLob){
                            Blob locator = rs.getBlob(fieldName);
                            if (locator != null) {
                                InputStream in = locator.getBinaryStream();
                                byte[] buf = new byte[Math.toIntExact(locator.length())];
                                in.read(buf);
                                in.close();
                                locator.free();
                                val=buf;
                            }
                        }
                    } else if (Entity.class.isAssignableFrom(field.getType())){
                        Class<?> subEntitypkType= Entity.getBasePrimaryKeyType((Class<? extends Entity>) field.getType());
                        Object subEntityPk= rs.getClass().getDeclaredMethod("get"+ ErrandBoy.firstLetterToUpperCase(subEntitypkType.getSimpleName()), String.class).invoke(rs, fieldName);
                        if (subEntityPk !=null) val =EntityFactory.class.getDeclaredMethod(fieldTypeName.toLowerCase(), subEntitypkType).invoke(null, subEntityPk);
                    } else {
                        val=rs.getClass().getDeclaredMethod("get" + ErrandBoy.firstLetterToUpperCase(fieldTypeName), String.class).invoke(rs, fieldName);
                    }
                    if (val!=null) entity.getClass().getDeclaredMethod("set" + ErrandBoy.firstLetterToUpperCase(fieldName), field.getType()).invoke(entity, val.getClass().equals(Integer.class)?((Integer) val).intValue():val);
                }
                ((Entity) entity).clean();
                entities.add((Entity) entity);
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | IllegalArgumentException |
                 IOException | SQLException e){
            throw new RuntimeException(e);
        }
        return entities;
    }
    public static String createStatementString(String table, Map<String, Object> params, StatementType type){
        StringBuilder firstParam = new StringBuilder();
        StringBuilder lastParam = new StringBuilder();
        int size=20;
        int page=0;
        String command = null;
        String separator = type==StatementType.INSERT | type==StatementType.UPDATE ? ", " : " AND ";
        boolean firstRun=true;
        if (params!=null)
            for (Map.Entry<String, Object> param: params.entrySet()){
                Object value;
                if((value=param.getValue())==null) continue;
                boolean fEmpty=firstParam.isEmpty();
                boolean lEmpty=lastParam.isEmpty();
                if(!fEmpty) firstParam.append(separator);
                if (type==StatementType.UPDATE){
                        if(lEmpty) lastParam.append(param.getKey()).append("=").append(param.getValue().toString());
                        else {
                            if(fEmpty) firstParam.append("SET ");
                            firstParam.append(param.getKey()).append("=").append(param.getValue());
                        }
                    } else {
                        if(!lEmpty) lastParam.append(separator);
                        if (type == StatementType.INSERT) firstParam.append(param.getKey());
                        else {
                            String key = param.getKey();
                            if (key.equals("size")) size = Integer.parseInt((String) value);
                            else if (key.equals("page")) page = Integer.parseInt((String) value);
                            else {
                                lastParam.append(key);
                                if (key.endsWith("High")) lastParam.append("<");
                                else if (key.endsWith("Low")) lastParam.append(">");
                                else lastParam.append("=");
                            }
                        }
                            if (value instanceof String | value instanceof Timestamp) lastParam.append("\"").append(value).append("\"");
                            else lastParam.append(value);
                    }
          }
        switch (type){
            case INSERT:
                firstParam=new StringBuilder("(").append(firstParam).append(")");
                lastParam=new StringBuilder("VALUES(").append(lastParam).append(")");
                command="INSERT INTO ";
                break;
            case QUERY:
                if (!lastParam.isEmpty()) lastParam=new StringBuilder("WHERE ").append(lastParam);
                lastParam.append(" LIMIT ").append(page*size).append(",").append(size);
                command="SELECT * FROM ";
                break;
            case DELETE:
                if (!lastParam.isEmpty()) lastParam=new StringBuilder("WHERE ").append(lastParam);
                command="DELETE FROM ";
                break;
            case UPDATE:
                lastParam=new StringBuilder(" WHERE ").append(lastParam);
                command="UPDATE ";
        }
        String sql=new StringBuilder(command).append(table).append(" ").append(firstParam).append(lastParam).toString();
        // useless. Find alternatives.
        return StringEscapeUtils.escapeSql( sql);
    }
    public enum StatementType{
        INSERT,
        QUERY,
        DELETE,
        UPDATE
    }
    public Connection getConnection() {
        return connection;
    }
    Connection validateAndGetConnection() throws SQLException{
        if(!this.connection.isValid(0)){
            this.connection=this.dataSource.getConnection();
        }
        return this.connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }
    private static final String[] connectparams={"jdbc:mysql://localhost:3306/test", "root", "Yusuf_2002"};
}

package com.yusuf.simpleecommercesite.dbContext.sqlUtils;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class StatementBuilder{
    private static final String exceptionMessage = "METHOD AND INITIALIZED STATEMENT TYPE IS INCOMPATIBLE";
    public boolean includeDefaults;
    private final StatementType type;
    private String command;
    private String tableName;
    private final List<String>  retrievedColumns=new ArrayList<>();
    private final StringBuilder firstHalf=new StringBuilder();
    private boolean firstAppendFirstHalf=true;
    private final StringBuilder lastHalf=new StringBuilder();
    private boolean firstAppendLastHalf=true;
    private final List<Object> firstParams = new ArrayList<>();
    private final List<Object> lastParams = new ArrayList<>();
    private final StringBuilder end = new StringBuilder();
    private final StringBuilder order=new StringBuilder();
    private final StringBuilder groupBy=new StringBuilder();
    private String[] insertColumnBuf=new String[0];
    public static PreparedStatement distinct(String tableName, String columnName, Connection connection) throws SQLException {
       PreparedStatement statement= connection.prepareStatement("SELECT DISTINCT "+ "?" + " FROM " + tableName);
       statement.setString(1, columnName);
       return statement;
    }
    public StatementBuilder(StatementType type) {
        this.type=type;
        createTemplate(type);
    }
    public StatementBuilder(StatementType type, boolean includeDefaults) {
        this(type);
        this.includeDefaults = includeDefaults;
    }
    public StatementBuilder set(String columnName, Object value) {
        if (isNotProcessed(value))
            return this;
        if (!type.equals(StatementType.UPDATE)) throw new RuntimeException(exceptionMessage);
        if(!firstAppendFirstHalf) firstHalf.append(", ");
        else firstAppendFirstHalf=false;
        firstHalf.append(columnName).append("=?");
        firstParams.add(value);
        return this;
    }
    public void reset(String... clausesToKeep){
        Stream<String> clauses=clausesToKeep!=null? Arrays.stream(clausesToKeep): null;
        if ( clauses==null||clauses.noneMatch(clause-> Objects.equals(clause, "SELECT") || Objects.equals(clause, "UPDATE") || Objects.equals(clause, "DELETE") || Objects.equals(clause, "INSERT"))) {
            firstHalf.delete(0, firstHalf.length());
            firstParams.clear();
            firstAppendFirstHalf=true;
            groupBy.delete(0, groupBy.length());
            end.delete(0, end.length());
            order.delete(0, order.length());
        }
        clauses=clausesToKeep!=null? Arrays.stream(clausesToKeep): null;
        if(clauses==null|| clauses.noneMatch(clause-> Objects.equals(clause, "WHERE") || Objects.equals(clause, "VALUES"))){
            lastHalf.delete(0, lastHalf.length());
            lastParams.clear();
            firstAppendLastHalf=true;
        }
        clauses=clausesToKeep!=null? Arrays.stream(clausesToKeep): null;
        if (clauses==null || clauses.noneMatch(clause-> Objects.equals(clause, "GROUP BY") || Objects.equals(clause, "ORDER BY") || Objects.equals(clause, "LIMIT")||  Objects.equals(clause, "OFFSET"))){
            order.delete(0, order.length());
            groupBy.delete(0, groupBy.length());
            end.delete(0, end.length());
        }
    }
    public StatementBuilder where(String columName, Object value, String... operator) {
        if (isNotProcessed(value))
            return this;
        if(!firstAppendLastHalf) lastHalf.append(" AND ");
        else {
            firstAppendLastHalf=false;
            lastHalf.append(" WHERE ");
        }
        String op="=";
        if(operator!=null && operator.length>0) op=operator[0];
        else if(value instanceof String){
            value= "%" + value + "%";
            op=" LIKE ";
        }
        if(value.getClass().isArray() || List.class.isAssignableFrom(value.getClass())) {
            boolean first=true;
            for (Object val : (List<Object>) value) {
                if(first) first=false;
                else lastHalf.append(" OR ");
                lastHalf.append(columName).append(op).append("? ");
                lastParams.add(value);
            }
        } else {
            lastHalf.append(columName).append(op).append("? ");
            lastParams.add(value);
        }
        return this;
    }
    public StatementBuilder groupBy(String columnName){
        if (groupBy.length()==0) groupBy.append(" GROUP BY ");
        else groupBy.append(", ");
        this.groupBy.append(columnName);
        return this;
    }
    public StatementBuilder columns(String... columNames) {
        if (!type.equals(StatementType.SELECT) && !type.equals(StatementType.INSERT))
            throw new RuntimeException(exceptionMessage);
        for (String columName : columNames) {
            if(!firstAppendFirstHalf) firstHalf.append(", ");
            else firstAppendFirstHalf=false;
            firstHalf.append(columName);
        }
        insertColumnBuf=columNames;
        return this;
    }
    public StatementBuilder values(Object... values) {
        if(isNotProcessed(values))
            for (String column:insertColumnBuf)
                firstHalf.delete(firstHalf.indexOf(column), column.length() + 2); // 2 for trailing comma and space
        if (!type.equals(StatementType.INSERT)) throw new RuntimeException(exceptionMessage);
        for (Object value : values) {
            if(!firstAppendLastHalf) lastHalf.append(", ");
            else firstAppendLastHalf=false;
            lastHalf.append("?");
            lastParams.add(value);
        }
        return this;
    }
    //let statement add param according to type
    public StatementBuilder param(String columnName, Object value) {
        if(type!=StatementType.SELECT && isNotProcessed(value)) return this;
        else if (type == StatementType.UPDATE)
            set(columnName, value);
        else if (type == StatementType.SELECT){
            columns(columnName);
            if((!isNotProcessed(value) || includeDefaults) && !lastHalf.toString().contains(columnName))
                where(columnName, value);
        }
        else if (type == StatementType.INSERT)
            columns(columnName).values(value);
        return this;
    }
    public StatementBuilder table(String tableName){this.tableName=tableName; return this;}
    public PreparedStatement build(Connection connection) {
        final String sql;
        if (type==StatementType.INSERT && (lastParams.isEmpty() && firstParams.isEmpty())) {
            firstHalf.delete(0,firstHalf.length());
            lastHalf.delete(0,lastHalf.length());
            end.delete(0,end.length());
            lastHalf.append(" default values");
        }
        if (type == StatementType.SELECT)
            sql = command + (firstHalf.length()==0 ? " * " : firstHalf.toString()) +" FROM " + tableName + lastHalf +groupBy + order + (end.length()==0?" LIMIT 20 OFFSET 0 ":end);
        else sql = command + tableName + firstHalf + lastHalf+  end;
        try {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            for (int j=0; j<2; j++) {
                List<Object> params = j==0?firstParams:lastParams;
                for (Object value : params) {
                    if (value != null && (value.getClass().isEnum() ||String.class.isAssignableFrom( value.getClass()) && ((String) value).contains("{")) )
                        statement.setObject(i++, value.toString(), Types.OTHER);
                    else if (value != null && byte[].class.isAssignableFrom(value.getClass()))
                        statement.setBinaryStream(i++, new ByteArrayInputStream((byte[]) value), ((byte[]) value).length);
                    else statement.setObject(i++, value);
                }
            }
            return statement;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    private boolean isNotProcessed(Object value){
        return (value==null ||  Objects.equals(value, 0) || Objects.equals(value, 0.0)|| Objects.equals(value, BigDecimal.ZERO)|| Objects.equals(value, false)) && !this.includeDefaults;
    }
    public StatementBuilder retrieve(String... columNames){
        this.retrievedColumns.addAll(Arrays.asList(columNames));
        return this;
    }
    public StatementBuilder page(int page, int pageSize) {
        page = page * pageSize;
        end.append(" LIMIT ").append((page+1) * pageSize).append(" OFFSET ").append((page)*pageSize);
        return this;
    }
    public StatementBuilder order(String columnName, boolean descending) {
        if (order.length()==0) order.append(" ORDER BY ");
        else order.append(", ") ;
        order.append(columnName).append(" ").append(descending?"DESC ":"ASC ");
        return this;
    }

//    public ResultSet build(Connection connection) throws SQLException {
//        this.build(connection);
//        ResultSet result = null;
//        if (type == StatementType.SELECT) result = this.innerStatement.executeQuery();
//        else innerStatement.executeUpdate();
//        if (type == StatementType.INSERT) result = this.innerStatement.getGeneratedKeys();
//        return result;
//    }
//    public int executeUpdate(Connection connection) throws SQLException {
//        this.build(connection);
//        return this.innerStatement.executeUpdate();
//    }


    private void createTemplate(StatementType type) {
        switch (type) {
            case INSERT:
                this.command = "INSERT INTO ";
                this.firstHalf.append(" (");
                this.lastHalf.append(") VALUES(");
                this.end.append(" )");
                break;
            case SELECT:
                this.command = "SELECT ";
                break;
            case DELETE:
                this.command = "DELETE FROM ";
                break;
            case UPDATE:
                this.firstHalf.append(" SET ");
                this.command = "UPDATE ";
        }
    }

    public void includeNull(boolean val) {
        this.includeDefaults = val;
    }

//    @Override
//    public void close() throws SQLException {
//        if (innerStatement != null) this.innerStatement.close();
//    }

    public StatementType getType() {
        return this.type;
    }

    public enum StatementType {
        INSERT,
        SELECT,
        DELETE,
        UPDATE
    }
}

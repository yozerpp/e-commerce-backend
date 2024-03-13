package com.may.simpleecommercesite.sqlUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CompiledStatement implements AutoCloseable {
    private static final String exceptionMessage = "METHOD AND INITIALIZED STATEMENT TYPE IS INCOMPATIBLE";
    private PreparedStatement innerStatement = null;
    public boolean includeDefaults;
    private StatementType type;
    private String command;
    private String tableName;
    private List<String>  retrievedColumns=new ArrayList<>();
    private StringBuilder firstHalf=new StringBuilder();
    private boolean firstAppendFirstHalf=true;
    private StringBuilder lastHalf=new StringBuilder();
    private boolean firstAppendLastHalf=true;
    private List<Object> firstParams = new ArrayList<>();
    private List<Object> lastParams = new ArrayList<>();
    private StringBuilder end = new StringBuilder();
    private StringBuilder order=new StringBuilder();
    private List<Object> values = new ArrayList<>();
    private String[] insertColumnBuf=new String[0];
    public CompiledStatement(StatementType type, String tableName) {
        this.type=type;
        this.tableName = tableName;
        createTemplate(type);
    }
    public CompiledStatement(StatementType type, String tableName, boolean includeDefaults) {
        this(type, tableName);
        this.includeDefaults = includeDefaults;
    }

    public CompiledStatement set(String columnName, Object value) {
        if (isNotProcessed(value))
            return this;
        if (!type.equals(StatementType.UPDATE)) throw new RuntimeException(exceptionMessage);
        if(!firstAppendFirstHalf) firstHalf.append(", ");
        else firstAppendFirstHalf=false;
        firstHalf.append(columnName).append("=?");
        firstParams.add(value);
        return this;
    }
    private boolean isNotProcessed(Object value){
        return (value==null | Objects.equals(value, 0) | Objects.equals(value, false)) && !this.includeDefaults;
    }
    public CompiledStatement where(String columName, Object value) {
        if (isNotProcessed(value))
            return this;
        if(!firstAppendLastHalf) lastHalf.append(" AND ");
        else {
            firstAppendLastHalf=false;
            lastHalf.append(" WHERE ");
        }
        lastHalf.append(columName).append("=? ");
        lastParams.add(value);
        return this;
    }
    public CompiledStatement columns(String... columNames) {
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
    public CompiledStatement values(Object... values) {
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
    public CompiledStatement param(String columnName, Object value) {
        if (type == StatementType.UPDATE)
            set(columnName, value);
        else if (type == StatementType.SELECT){
            columns(columnName);
            if(value!=null) where(columnName, value);
        }
        else if (type == StatementType.INSERT)
            columns(columnName).values(value);
        return this;
    }
    public CompiledStatement retrieve(String... columNames){
        this.retrievedColumns.addAll(List.of(columNames));
        return this;
    }
    public CompiledStatement page(int page, int pageSize) {
        page = page * pageSize;
        end.append(" LIMIT ").append(page).append(", ").append(pageSize);
        return this;
    }
    public CompiledStatement order(String columnName) {
        order.append(" ORDER BY ").append(columnName);
        return this;
    }

    public ResultSet execute(Connection connection) throws SQLException {
        this.compile(connection);
        ResultSet result = null;
        if (type == StatementType.SELECT) result = this.innerStatement.executeQuery();
        else innerStatement.executeUpdate();
        if (type == StatementType.INSERT) result = this.innerStatement.getGeneratedKeys();
        return result;
    }
    public int executeUpdate(Connection connection) throws SQLException {
        this.compile(connection);
        return this.innerStatement.executeUpdate();
    }
    private void compile(Connection connection) {
        final String sql;
        if (type == StatementType.SELECT)
            sql = command + (firstHalf.isEmpty() ? " * " : firstHalf.toString()) +" FROM " + tableName + lastHalf + order + (end.isEmpty()?" LIMIT 0,20 ":end);
        else sql = command + tableName + firstHalf + lastHalf + end;
        try {
            this.innerStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            for (Object value : firstParams) innerStatement.setObject(i++, value);
            for (Object value : lastParams) innerStatement.setObject(i++, value);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

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

    @Override
    public void close() throws SQLException {
        if (innerStatement != null) this.innerStatement.close();
    }

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

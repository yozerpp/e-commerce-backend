package com.may.simpleecommercesite.beans;


import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Objects;
import java.util.logging.*;


public class SqlLogger implements Serializable {
    private Logger logger;
    public  SqlLogger(){
        this.logger= Logger.getLogger(SqlLogger.class.getName());
    }

    public void log(SqlMethodType type, SQLException e){
        Level level = java.util.logging.Level.INFO;
        switch (type){
            case UPDATE:
            case DELETE:
            case INSERT:
                level=Level.WARNING;
                break;
            case DDL:
                level=Level.SEVERE;
                break;
        }
            logger.log(Level.INFO, "SQL "+ type + " Exception:");
            for (; e!=null; e=e.getNextException()){
                logger.log(level,   e.getSQLState()+ ":" + e.getMessage() + ":" + e.getCause() + ":" + e.getErrorCode(), e.getStackTrace());
        }
    }

    public Logger getLogger() {
        return logger;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SqlLogger sqlLogger = (SqlLogger) o;
        return Objects.equals(logger, sqlLogger.logger);
    }

    @Override
    public int hashCode() {
        return Objects.hash(logger);
    }

    @Override
    public String toString() {
        return "SqlLogger{}";
    }

    public enum SqlMethodType{
        QUERY,
        INSERT,
        UPDATE,
        DELETE,
        DDL
    }
}

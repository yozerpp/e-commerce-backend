package com.may.simpleecommercesite.beans;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import java.sql.SQLException;
import java.util.Enumeration;

@ApplicationScoped
public class SqlLogger {
    @Resource(lookup = "log/log4j")
    Logger logger;
    SqlLogger(){}

    public void log(SqlMethodType type, SQLException e){
        Level level = Level.INFO;
        switch (type){
            case UPDATE:
            case DELETE:
            case INSERT:
                level=Level.ERROR;
                break;
            case DDL:
                level=Level.FATAL;
                break;
        }
            logger.log(Level.INFO, "Sql "+ type + "Exception:\n");
            for (; e!=null; e=e.getNextException()){
                logger.log(level,   e.getSQLState()+ ":" + e.getMessage() + ":" + e.getCause() + ":" + e.getErrorCode());
        }
    }

    public enum SqlMethodType{
        QUERY,
        INSERT,
        UPDATE,
        DELETE,
        DDL
    }
}

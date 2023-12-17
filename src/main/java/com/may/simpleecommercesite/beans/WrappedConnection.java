package com.may.simpleecommercesite.beans;

import javax.decorator.Decorator;
import javax.enterprise.context.Dependent;
import java.beans.Transient;
import java.io.Serializable;
import java.sql.Connection;

@Dependent
public class WrappedConnection implements Serializable {
    private Connection realConnection;
    public WrappedConnection(Connection c){
        realConnection=c;
    }
    public WrappedConnection(){}
    public Connection getrealConnection(){
        return realConnection;
    }
    public void setRealConnection(Connection realConnection) {
        this.realConnection = realConnection;
    }
}

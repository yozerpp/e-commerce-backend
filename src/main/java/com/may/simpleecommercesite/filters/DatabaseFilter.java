package com.may.simpleecommercesite.filters;

import com.may.simpleecommercesite.apiServlets.AsyncComponent;

import javax.annotation.Resource;
import javax.servlet.AsyncContext;
import javax.servlet.Filter;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;

public class DatabaseFilter extends HttpFilter {
    protected final Object asynclock = new Object();
    protected int asyncCount=0;
    @Resource(name = "java:comp/env/jdbc/pool/test")
    protected DataSource dataSource;
    protected AsyncContext enterAsync(HttpServletRequest req){
        synchronized (asynclock) {
            if( asyncCount++==0) return req.startAsync();
            else return req.getAsyncContext();
        }
    }
    protected void leaveAsync(AsyncContext asyncContext){
        synchronized (asynclock){
            if (--asyncCount==0) asyncContext.complete();
        }
    }
}

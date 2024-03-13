package com.may.simpleecommercesite.beans;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.sql.DataSource;
@WebListener
public class ApplicationInitializer implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ObjectMapper mapper=new ObjectMapper();
        sce.getServletContext().setAttribute("ObjectMapper", mapper);
        try {
            Context ctx=new InitialContext();
            sce.getServletContext().setAttribute("DbContext",new DbContext(mapper, (DataSource)ctx.lookup("java:comp/env/jdbc/pool/test")));
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
;
        sce.getServletContext().setRequestCharacterEncoding("utf-8");
        sce.getServletContext().setResponseCharacterEncoding("utf-8");
    }
}

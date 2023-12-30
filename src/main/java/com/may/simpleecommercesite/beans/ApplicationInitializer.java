package com.may.simpleecommercesite.beans;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.may.simpleecommercesite.beans.EntityFactory;
import com.may.simpleecommercesite.helpers.Json;

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
        try {
            Context ctx=new InitialContext();
            EntityFactory.setDataSource((DataSource)ctx.lookup("java:comp/env/jdbc/pool/test"));

        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
        sce.getServletContext().setRequestCharacterEncoding("utf-8");
        sce.getServletContext().setResponseCharacterEncoding("utf-8");
        Json.setMapper(new ObjectMapper());
        Json.setFactory(new JsonFactory().configure(JsonFactory.Feature.CHARSET_DETECTION, true));
    }
}

package com.may.simpleecommercesite.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.may.simpleecommercesite.entityManager.DbContext;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.sql.DataSource;
@WebListener
public class Initializer implements ServletContextListener {
    public static final int SERIALIZATION_MAX_DEPTH=5;
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ObjectMapper mapper=new ObjectMapper();
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.setDateFormat(new StdDateFormat());
//        mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, true);
//        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE);
        SimpleFilterProvider filter= new SimpleFilterProvider();
        for (int i=1; i<= SERIALIZATION_MAX_DEPTH; i++)
           filter= filter.addFilter("depth_" + i, new JsonDepthFilter(i));
        mapper.setFilterProvider(filter);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
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

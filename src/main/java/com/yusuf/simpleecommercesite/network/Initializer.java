package com.yusuf.simpleecommercesite.network;

import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.yusuf.simpleecommercesite.dbContext.DbContext;
import com.yusuf.simpleecommercesite.entities.metadata.ExchangeRates;
import com.yusuf.simpleecommercesite.helpers.ErrandBoy;
import com.yusuf.simpleecommercesite.helpers.JsonDepthFilter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.sql.DataSource;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@WebListener
public class Initializer implements ServletContextListener {
    public static final int SERIALIZATION_MAX_DEPTH=5;
    private ScheduledExecutorService scheduler;
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.setDateFormat(new StdDateFormat());
//        mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, true);
//        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE);
        SimpleFilterProvider filter = new SimpleFilterProvider();
        for (int i = 1; i <= SERIALIZATION_MAX_DEPTH; i++)
            filter = filter.addFilter("depth_" + i, new JsonDepthFilter(i));
        mapper.setFilterProvider(filter);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        sce.getServletContext().setAttribute("ObjectMapper", mapper);
        DbContext dbContext;
        try {
            Context ctx = new InitialContext();
            dbContext = new DbContext(mapper, (DataSource) ctx.lookup("java:comp/env/jdbc/ecommerce"));
            sce.getServletContext().setAttribute("DbContext", dbContext);
        } catch (NamingException | SQLException e) {
            throw new RuntimeException(e);
        }
        sce.getServletContext().setRequestCharacterEncoding("utf-8");
        sce.getServletContext().setResponseCharacterEncoding("utf-8");
        scheduler.scheduleAtFixedRate(() -> {
            updateExchangeRates(dbContext);
        }, 0, 1, TimeUnit.DAYS);
    }
    private void updateExchangeRates(DbContext dbContext) {
        try {
        dbContext.setSchema("metadata");
        ExchangeRates prevRates= dbContext.findById(ExchangeRates.class, "TRY");
        if (prevRates==null || (prevRates.getLast_updated().getTime() - LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()>=Duration.ofDays(1).toMillis())){
           ExchangeRates newRates = new ExchangeRates("TRY");
            HttpURLConnection http = (HttpURLConnection) new URL("https://www.tcmb.gov.tr/kurlar/today.xml").openConnection();
            http.setRequestMethod("GET");
            http.getResponseCode();
            InputStream in= http.getInputStream();
            Document rateXml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
            NodeList currencies = rateXml.getElementsByTagName("Currency");
            for (Field currency : newRates.getClass().getDeclaredFields()) {
                for (int i=0; i< currencies.getLength() ; i++){
                    Element cur= (Element) currencies.item(i);
                    if (cur.getAttributes().getNamedItem("CurrencyCode").getNodeValue().equals(currency.getName().toUpperCase(Locale.ENGLISH))){
                       ErrandBoy.findSetter(currency, newRates.getClass()).invoke(newRates,Float.valueOf(cur.getElementsByTagName("ForexBuying").item(0).getTextContent()).floatValue());
                    }
                }
            }
            newRates.setLast_updated(Date.from(Instant.now()));
            dbContext.save(newRates);
            System.out.println("Updated the exchange rates");
        }
        } catch (SQLException | IOException | ParserConfigurationException |
                 InvocationTargetException | SAXException | IllegalAccessException e) {
            e.printStackTrace(System.err);
            throw new RuntimeException(e);
        } finally {
            dbContext.setSchema("ecommerce");
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        scheduler.shutdownNow();
        ServletContextListener.super.contextDestroyed(sce);
    }
}

package com.may.initializer;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.may.simpleecommercesite.beans.DbContext;
import com.mysql.cj.jdbc.Driver;

public class DatabaseInitializer {
    static final String patternStr="![^!]+!";
    static final Connection connection;
    static final String pathPrefix="src/main/java/com/may/initializer/";
    static final Properties props=new Properties();

    static {
        try {
            props.load(new FileInputStream(pathPrefix + "./data.properties"));
            DriverManager.registerDriver(new Driver());
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/ecommerce", "root", "123456");
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static void sql() throws IOException, SQLException {
        BufferedReader reader=new BufferedReader(new FileReader(pathPrefix + "./init.sql"));
        Pattern pattern=Pattern.compile(patternStr);
//        Pattern replacePattern=Pattern.compile(replacePatternStr);
        reader.lines().forEach(line->{
            Matcher matcher=pattern.matcher(line);
//            Matcher replaceMatcher=replacePattern.matcher(line);
            if (matcher.find() ){
                List<String> buffer=new ArrayList<>();
                for(boolean more=true; more ;more=matcher.find()){
                    String key;
                    key=matcher.group().replace("!", "");
                    if (key==null) break;
                    buffer.add('\''+props.get(key).toString() +'\'');
                }
                Iterator<String> iter=buffer.iterator();
                String command= matcher.replaceAll(m->iter.next());
                try {
                    connection.createStatement().execute(command);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

            }
            else {
                try {
                    connection.createStatement().execute(line);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        for (Map.Entry<Object,Object> prop: props.entrySet()){
            props.replace(prop.getKey(), incrementString(prop.getValue().toString()));
        }
        props.store(new FileWriter(pathPrefix + "./data.properties"),null);
    }
    public static void dbContext(){
        DbContext dbContext=new DbContext(new ObjectMapper(), connection);

    }
    public static void main (String[] args) throws IOException, SQLException {
        sql();
    }
    public static String incrementString(String str){
        char[] chars=str.toCharArray();
        for (int i=chars.length-1; i>=0;i--){
            if(chars[i]<121){
                chars[i]++;
                break;
            }
        }
        return new String(chars);
    }
}

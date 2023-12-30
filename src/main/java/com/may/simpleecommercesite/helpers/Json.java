package com.may.simpleecommercesite.helpers;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.may.simpleecommercesite.annotations.Id;
import com.may.simpleecommercesite.beans.EntityFactory;
import com.may.simpleecommercesite.entities.Entity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.Timestamp;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Json {
    public static List<? extends Entity> updateFromJson(BufferedReader reader, List<? extends Entity>  entities) throws IllegalAccessException {
        Map<String, Object> json=null;
        json= parseJson(reader);
        for(Entity entity: entities) {
            for (Field field : entity.getClass().getDeclaredFields()) {
                if (json.containsKey(field.getName())) {
                    field.set(entity, json.get(field.getName()));
                }
            }
        }
        return entities;
    }
    public static List<? extends Entity> filterFromJson(BufferedReader reader, List<? extends Entity> entities) throws IllegalAccessException, NoSuchFieldException {
        Map<String,Object> json=null;
        json= parseJson(reader);
        List<Entity> entities1=new ArrayList<>();
        for (Entity entity : entities) {
            boolean match=true;
            for (Map.Entry<String, Object> entry : json.entrySet()) {
                Field field=entity.getClass().getDeclaredField((String)entry.getKey());
                if (field.isAnnotationPresent(Id.class) && !field.get(entity).equals(entry.getValue())){
                    match=false;
                }
            }
            if (match){
                entities1.add(entity);
            }
        }
        return entities1;
    }
    public static List<? extends Entity> updateAndFilterFromJson(BufferedReader reader, List<? extends Entity> entities) throws IllegalAccessException, NoSuchFieldException {
        Map<String, Object> json=null;
        json=parseJson(reader);

        List<Entity> entities1=new ArrayList<>();
        for (Entity entity : entities) {
            boolean match=true;
            for (Map.Entry<String, Object> entry : json.entrySet()) {
                Field field=entity.getClass().getDeclaredField((String)entry.getKey());
                if (field.isAnnotationPresent(Id.class) && !field.get(entity).equals(entry.getValue())){
                    match=false;
                }
            }
            if (match){
                for (Map.Entry<String, Object> entry : json.entrySet()) {
                    Object val=null;
                    Class<?> fieldType=entity.getClass().getDeclaredField(entry.getKey()).getType();
                    if (Entity.class.isAssignableFrom(fieldType)){
                        val=instantiateFromMap(Map.ofEntries(entry), fieldType);
                    } else {
                        val=entry.getValue();
                    }
                    try {
                        entity.getClass().getDeclaredMethod("set" + ErrandBoy.firstLetterToUpperCase(entry.getKey()), val.getClass().equals(Integer.class)?int.class:val.getClass()).invoke(entity, val);
                    } catch (InvocationTargetException | NoSuchMethodException e) {
                        throw new RuntimeException(e);
                    }
                }
                entities1.add(entity);
            }
        }
        return entities1;
    }
    public static Object instantiateFromJson(BufferedReader reader, Class<?> clazz){
        return instantiateFromMap(parseJson(reader),clazz);

    }
    public static Object instantiateFromMap(Map<String, Object> json, Class<?> clazz){
        Object instance=null;
        try {
//            json = json.entrySet().stream().filter(entry->Objects.nonNull(entry.getValue())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            instance=EntityFactory.class.getDeclaredMethod(ErrandBoy.firstLetterToLowerCase(clazz.getSimpleName())).invoke(null);
            for (Map.Entry<String, Object> prop:json.entrySet()){
                Object val=null;
                Class<?> propType=clazz.getDeclaredField(prop.getKey()).getType();
                if (Entity.class.isAssignableFrom(propType)){
                    val=instantiateFromMap(Map.ofEntries(prop), propType);
                } else val=prop.getValue();
                clazz.getDeclaredMethod("set" + ErrandBoy.firstLetterToUpperCase(prop.getKey()), val.getClass().equals(Integer.class)?int.class:val.getClass()).invoke(instance, val);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        return instance;
    }
    public static void serializeObject(Object object, Writer writer) {
        try {
            if (object==null) object=new Object[0];
            mapper.writerFor(object.getClass()).writeValue(writer, object);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static Map<String, Object> parseJson(BufferedReader reader){
        JsonParser parser= null;
        try {
            parser = factory.createParser(reader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Map<String, Object> json = new HashMap<>();
        JsonToken token=null;
        try {
            parser.nextToken();
            while ((token = parser.nextToken())!=JsonToken.END_OBJECT) {
                String key=null;
                Object val=null;
                if(token==JsonToken.FIELD_NAME){
                    key=parser.currentName();
                }
                token=parser.nextToken();
                if(token.isNumeric()) val=parser.getIntValue();
                else if (token.equals(JsonToken.VALUE_EMBEDDED_OBJECT)){
                    val= parser.readValueAs(Class.forName(ErrandBoy.firstLetterToUpperCase( key.endsWith("Id")?key.replace("Id", ""):key)));
                }
                else if(token.equals(JsonToken.VALUE_STRING)){
                    String inter=parser.getText();
                    Matcher dateFinder= Pattern.compile("^(19[6-9]\\d|20[0-2]\\d)-(1[0-2]|0[0-9])-(3[0-1]|[0-2][0-9])$").matcher(inter);
                    if (dateFinder.matches()) val=new Timestamp(Integer.parseInt(dateFinder.group(1))-1900, Integer.parseInt(dateFinder.group(2))-1,Integer.parseInt(dateFinder.group(3)),0,0,0,0);
                    else val=inter=="null"?null:inter;
                }
                if(val!=null) json.put(key, val);
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                parser.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return json;
    }
    private static JsonFactory factory;

    public static void setFactory(JsonFactory factory) {
        Json.factory = factory;
    }

    private static ObjectMapper mapper;
    public static void setMapper(ObjectMapper m){        mapper=m;
    }
}

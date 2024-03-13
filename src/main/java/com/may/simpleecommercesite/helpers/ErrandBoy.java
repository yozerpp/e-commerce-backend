package com.may.simpleecommercesite.helpers;

import com.may.simpleecommercesite.annotations.Cookie;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ErrandBoy {
    private static final Map<Class<?>,Map<Class<? extends Annotation>, Field[]>> fieldCache=new HashMap<>();
    public static String firstLetterToUpperCase(String str){
        char[] chars=str.toCharArray();
        chars[0]=Character.toUpperCase(chars[0]);
        String string= String.copyValueOf(chars);
        string=str.equals("Integer")?str.replace("eger", ""):string;
        return string;
    }
    public static String firstLetterToLowerCase(String str){
        char[] chars=str.toCharArray();
        chars[0]=Character.toLowerCase(chars[0]);
        String string= String.copyValueOf(chars);
        string=str.equals("Integer")?str.replace("eger", ""):string;
        return string;
    }
    public static Field getAnnotatedField(Class<?> clazz, Class<? extends Annotation> annotation){
        return getAnnotatedFields(clazz, annotation)[0];
    }
    public static boolean validateCredentialFormat(String email, String credential){
        return email.matches("^\\w+@\\w+\\.\\w+") &&
                credential.matches("^([a-zA-Z]+\\d+|\\d+[a-zA-Z]+)$") &&
                credential.length()>12;
    }
    public static Class<?> getRealClass(Object entity){
        Class<?> clazz=entity.getClass();
        if (clazz.getSimpleName().contains("$$Enhancer")) clazz=clazz.getSuperclass();
        return clazz;
    }
    public static Class<?> getRealClass(Class<?> clazz){
        if(clazz.getSimpleName().contains("$$Enhancer")) return clazz.getSuperclass();
        else return clazz;
    }
    public static Field[] getAnnotatedFields(Class<?> clazz, Class<? extends Annotation> annotation) {
        clazz=getRealClass(clazz);
        Field[] fields=getFromCache(clazz, annotation);
        if(fields!=null) return fields;
        fields=new Field[4];
        int i=0;
        for (Field field:clazz.getDeclaredFields())
            if (field.isAnnotationPresent(annotation)) fields[i++]=field;
        fieldCache.put(clazz, Map.of(annotation, fields));
        return fields;
    }
    private static Field[] getFromCache(Class<?> clazz, Class<? extends Annotation> annotation){
        Map<Class<? extends Annotation>, Field[]> fieldMap= fieldCache.get(clazz);
        if (fieldMap!=null)
            return fieldMap.get(annotation);
        else return null;
    }
}

package com.yusuf.simpleecommercesite.helpers;

import javax.persistence.Id;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class ErrandBoy {
    private static final Map<Class<?>,Map<Class<? extends Annotation>, List<Field>>> fieldCache=new HashMap<>();
    public static String firstLetterToUpperCase(String str){
        char[] chars=str.toCharArray();
        chars[0]=Character.toUpperCase(chars[0]);
        String string= String.copyValueOf(chars);
        string=str.equals("Integer")?str.replace("eger", ""):string;
        return string;
    }
    public static String toRestLink (Object entity){
        Class<?> entityClass = getRealClass(entity);
         return getAnnotatedFields(entityClass, Id.class).stream().sorted(Comparator.comparing(Field::getName)).map(field -> {
            try {
                return findGetter(field, entityClass).invoke(entity);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }).reduce(new StringBuilder(entityClass.getSimpleName()),(str, id)->((StringBuilder) str).append('/').append(id)).toString().toLowerCase(Locale.ENGLISH);
    }
    public static String firstLetterToLowerCase(String str){
        char[] chars=str.toCharArray();
        chars[0]=Character.toLowerCase(chars[0]);
        String string= String.copyValueOf(chars);
        string=str.equals("Integer")?str.replace("eger", ""):string;
        return string;
    }
    public static Field getAnnotatedField(Class<?> clazz, Class<? extends Annotation> annotation){
        List<Field> fields= getAnnotatedFields(clazz, annotation);
        return  !fields.isEmpty()?fields.get(0):null;
    }
    public static boolean validateCredentialFormat(String email, String credential){
        return  (email==null || email.matches("^\\w+@\\w+\\.\\w+")) && (credential==null||
                (credential.matches(".*\\d.*") && credential.matches(".*[A-Z].*") && credential.matches(".*[a-z].*") &&credential.length()>12)) &&
                (email!=null || credential!=null);
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
    public static List<Field> getAnnotatedFields(Class<?> clazz, Class<? extends Annotation> annotation) {
        clazz=getRealClass(clazz);
        List<Field> fields=getFromCache(clazz, annotation);
        if(fields!=null) return fields;
        fields=new ArrayList<>();
        for (Field field:clazz.getDeclaredFields())
            if (field.isAnnotationPresent(annotation)) fields.add(field);
        Map<Class<? extends Annotation>,List<Field> > map = new HashMap<>();
        map.put(annotation,fields);
        fieldCache.put(clazz, map);
        return fields;
    }
    private static List<Field> getFromCache(Class<?> clazz, Class<? extends Annotation> annotation){
        Map<Class<? extends Annotation>, List<Field>> fieldMap= fieldCache.get(clazz);
        if (fieldMap!=null)
            return fieldMap.get(annotation);
        else return null;
    }

    public static Method findGetter(Field field, Class<?> clazz){
        String pre;
       if (boolean.class.isAssignableFrom(field.getType())){
           pre="is";
       } else pre="get";
        try {
            return clazz.getDeclaredMethod(pre + firstLetterToUpperCase(field.getName()));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static Method findSetter(Field field, Class<?> clazz){
        return Arrays.stream(clazz.getDeclaredMethods())
                .filter(method -> method.getName().matches("^(set)" + firstLetterToUpperCase(field.getName())))
                .findFirst().orElse(null);
    }
}

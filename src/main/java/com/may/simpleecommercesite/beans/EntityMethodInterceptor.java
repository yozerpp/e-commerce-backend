package com.may.simpleecommercesite.beans;

import com.may.simpleecommercesite.annotations.Entity;
import com.may.simpleecommercesite.annotations.Id;
import com.may.simpleecommercesite.helpers.ErrandBoy;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import java.lang.reflect.*;
import java.util.*;

public class EntityMethodInterceptor implements MethodInterceptor {
    DbContext context;
    Object target;
    Map<String,Object> targetId; // fieldName, value
    EntityMethodInterceptor(DbContext context, Object target){
        this.context=context;
        this.target=target;
        this.targetId=getId(target);
    }
    private String getFieldName(String methodName){
        return ErrandBoy.firstLetterToLowerCase(methodName.replaceAll("^(is|get|set)", ""));
    }
    private Map<String,Object> getId(Object target){
        Map<String,Object> ids=new HashMap<>();
        for (Field field: target.getClass().getDeclaredFields()){
            if (field.isAnnotationPresent(Id.class)){
                try {
                    ids.put(field.getName(), target.getClass().getDeclaredMethod("get" + ErrandBoy.firstLetterToUpperCase(field.getName())).invoke(target));
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return ids;
    }
    @Override
    public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
        Object retVal=method.invoke(target, args);
        if(method.getName().startsWith("set")){
            context.addDirtyField(target, getFieldName(method.getName())); // Saves name of the field to the map.
        } else if (method.getName().startsWith("get") | method.getName().startsWith("is")){
            Class<?> fieldType=method.getReturnType();
            if(fieldType.isAnnotationPresent(Entity.class)){
                if (retVal==null) return null;
                if(retVal.getClass().getSimpleName().contains("$$Enhancer")) return retVal;
                retVal=context.findAndPopulate(retVal);
                target.getClass().getDeclaredMethod(method.getName().replaceAll("^(is|get)", "set"), method.getReturnType()).invoke(target, retVal);
            } else if (List.class.isAssignableFrom(fieldType) &&
                    ( (fieldType= (Class<?>)((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0])).isAnnotationPresent(Entity.class)){
                retVal=context.search(fieldType, this.targetId);
            } else if (byte[].class.isAssignableFrom(fieldType) | char[].class.isAssignableFrom(fieldType)){
                retVal=method.invoke(context.fetchLOb(target, getFieldName(method.getName())), args);
            }
        }
        return retVal;
    }
}

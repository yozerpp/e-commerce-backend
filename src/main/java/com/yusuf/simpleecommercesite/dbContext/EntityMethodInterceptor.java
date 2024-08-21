package com.yusuf.simpleecommercesite.dbContext;

import javax.persistence.*;
import com.yusuf.simpleecommercesite.helpers.ErrandBoy;
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
        String fieldName=getFieldName(method.getName());
        if(method.getName().startsWith("set")){
            context.addDirtyField(target, fieldName); // Saves name of the field to the map.
        } else if (method.getName().startsWith("get") || method.getName().startsWith("is")){
            Field field=target.getClass().getDeclaredField(fieldName);
            Class<?> fieldType=method.getReturnType();
            if(field.isAnnotationPresent(OneToOne.class)){
                if (retVal==null) return null;
                if(retVal.getClass().getSimpleName().contains("$$Enhancer")) return retVal;
                retVal=context.findAndFill(retVal);
                target.getClass().getDeclaredMethod(method.getName().replaceAll("^(is|get)", "set"), method.getReturnType()).invoke(target, retVal);
            }
//            else if (List.class.isAssignableFrom(fieldType) &&
//                    ( (fieldType= (Class<?>)((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0])).isAnnotationPresent(Entity.class)){
            else if(field.isAnnotationPresent(OneToMany.class))    {
                String inverseJoinCol=field.getAnnotation(OneToMany.class).mappedBy();
                fieldType= (Class<?>)((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0];
                Map<String, Object> params = new HashMap<>();
                params.put(inverseJoinCol, this.targetId.values().stream().findFirst().get());
                retVal=context.search(fieldType, params ).getData();
                if (retVal!=null) this.target.getClass().getMethod("set"+ErrandBoy.firstLetterToUpperCase(getFieldName(method.getName())), List.class).invoke(this.target, retVal);
            } else if (byte[].class.isAssignableFrom(fieldType) || char[].class.isAssignableFrom(fieldType)){
                retVal=method.invoke(context.fetchLOb(target, getFieldName(method.getName())), args);
            }
        }
        return retVal;
    }
}

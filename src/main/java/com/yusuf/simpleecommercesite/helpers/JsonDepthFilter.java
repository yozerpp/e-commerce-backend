package com.yusuf.simpleecommercesite.helpers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.yusuf.simpleecommercesite.entities.annotations.Entity;
import com.yusuf.simpleecommercesite.entities.annotations.Id;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class JsonDepthFilter extends SimpleBeanPropertyFilter {
    private final int maxDepth;
    public JsonDepthFilter(int depth){
        super();
        maxDepth=depth;
    }
    private int calcDepth(JsonGenerator jgen) {
        JsonStreamContext sc = jgen.getOutputContext();
        int depth = -1;
        while (sc != null) {
            sc = sc.getParent();
            depth++;
        }
        return depth;
    }

    @Override
    public void serializeAsField(Object pojo, JsonGenerator gen, SerializerProvider provider, PropertyWriter writer)
            throws Exception {
        int depth = calcDepth(gen);
        if (depth <= maxDepth) {
            writer.serializeAsField(pojo, gen, provider);
        }
        else {
            if(pojo.getClass().isAnnotationPresent(Entity.class)) {
                Map<String, Object> fields = new HashMap<>();
                ErrandBoy.getAnnotatedFields(pojo.getClass(), Id.class).stream().map(field1 -> field1.getName()).filter(fieldName -> fieldName.equals(writer.getFullName().getSimpleName())).forEach(fieldName -> {
                    try {
                        provider.defaultSerializeField(writer.getFullName().getSimpleName(), pojo.getClass().getDeclaredMethod("get" + ErrandBoy.firstLetterToUpperCase(fieldName)).invoke(pojo), gen);
                    } catch (IOException | NoSuchMethodException | InvocationTargetException |
                             IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
    }
}

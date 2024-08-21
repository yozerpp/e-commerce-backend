package com.yusuf.simpleecommercesite.dbContext.sqlUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yusuf.simpleecommercesite.helpers.ErrandBoy;
import com.yusuf.simpleecommercesite.entities.annotations.Entity;
import javax.persistence.Embeddable;
import javax.persistence.Id;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SqlTypeConverter {
    private ObjectMapper mapper;

    public SqlTypeConverter() {
    }

    public SqlTypeConverter(ObjectMapper mapper) {
        this.mapper = mapper;
    }
    public <T> T convertToJavaType(Object val, Class<T> targetType) throws IOException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        if (val==null) return null;
        else if (targetType.isEnum())
            val = targetType.getMethod("valueOf", String.class).invoke(null, val);
        else if (byte[].class.isAssignableFrom(targetType)) {
            if(val instanceof Blob)
                val = convertBlobToByteArr(val);
        } else if(val instanceof Timestamp) {
            val= new Date(((Timestamp) val).getTime());
        }
        else if (targetType.isAnnotationPresent(Entity.class))
            val = targetType.getConstructor(ensurePrimitive(val.getClass())).newInstance(val);
        else if (Array.class.isAssignableFrom(val.getClass())){
            try {
                val = Arrays.stream(((Object[])((Array)val).getArray())).collect(Collectors.toList());
            } catch (SQLException e) { throw new RuntimeException(e); }
        }
        else if (targetType.isAnnotationPresent(Embeddable.class) | List.class.isAssignableFrom(targetType) | targetType.isArray())
            val = (this.mapper != null ? this.mapper.readerFor(targetType).readValue(new StringReader((String) val)) : null);
        else if(targetType.isPrimitive() && val instanceof Number)
           val = Number.class.getDeclaredMethod(targetType.getSimpleName() + "Value").invoke((Number)val);
        else if(BigDecimal.class.isAssignableFrom(targetType) && val instanceof Number)
            val = BigDecimal.valueOf(((Number)val).doubleValue());
        else if (BigInteger.class.isAssignableFrom(targetType) && val instanceof Number)
            val = BigInteger.valueOf(((Number)val).longValue());
        return (T) val;
    }
    private static byte[] convertBlobToByteArr(Object val) {
        try {
            InputStream in = ((Blob) val).getBinaryStream();
            byte[] buf = new byte[Math.toIntExact(((Blob) val).length())];
            in.read(buf);
            in.close();
            val = buf;
            return buf;
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Class<?> ensurePrimitive(Class<?> val) {
        Class<?> ret = val;
        if (ret.equals(Integer.class)) ret = int.class;
        else if (ret.equals(Boolean.class)) ret = boolean.class;
        return ret;
    }

    public Object convertToSqlType(Object value, Connection connection) {
        if (value==null) return null;
//        else if (value instanceof byte[])
//            return connection != null ? convertByteArrToIS(value) : null;
        else if(value instanceof Date){
            return new Timestamp(((Date) value).getTime());
        } else if (value.getClass().isEnum()){
            return value.toString();
        }
        else if (value.getClass().isAnnotationPresent(Entity.class))
            return convertEntityToPrimitive(value);
        else if (value.getClass().isAnnotationPresent(Embeddable.class))
            return this.mapper != null ? convertObjectToString(value, this.mapper) : null;
        else if(value instanceof List || value instanceof Set ||(value.getClass().isArray() && !value.getClass().getComponentType().isPrimitive())){
            Stream<Object> stream;
            if (value.getClass().isArray())
                stream = Arrays.stream((Object[]) value);
            else stream=((Collection<Object>) value).stream();
            return '{'+ stream.map(Object::toString).collect(Collectors.joining(",")) + '}';
        }
        else return value;
    }

    private Object convertEntityToPrimitive(Object value) {
        try {
            Class<?> clazz=ErrandBoy.getRealClass(value);
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(Id.class)) {
                    return convertToSqlType(value.getClass().getDeclaredMethod("get" + ErrandBoy.firstLetterToUpperCase(field.getName())).invoke(value), null);
                }
            }
            return null;
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private String convertObjectToString(Object value, ObjectMapper mapper) {
        try {
            StringWriter writer = new StringWriter();
            mapper.writerFor(value.getClass()).writeValue(writer, value);
            return writer.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Blob convertByteArrToBlob(Object value, Connection connection) {
        try {
            Blob locator = connection.createBlob();
            OutputStream out = locator.setBinaryStream(1);
            out.write((byte[]) value);
            value = locator;
            out.close();
            return (Blob) value;
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}

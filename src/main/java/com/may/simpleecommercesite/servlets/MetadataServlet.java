package com.may.simpleecommercesite.servlets;

import com.may.simpleecommercesite.annotations.Metadata;
import com.may.simpleecommercesite.helpers.ErrandBoy;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@WebServlet(urlPatterns = "/metadata/*", name = "Metadata",asyncSupported = true)
public class MetadataServlet extends BaseServlet {
    static final String classNamePrefix="com.may.simpleecommercesite.entities.";
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        String[] path = req.getPathInfo().split("/");
        String request = path[1];
        String entity=ErrandBoy.firstLetterToUpperCase(path[2]);
        if (Objects.equals(request, "types")) {
            Map<String, String> ret = new HashMap<>();
            try {
                Class<?> cls = Class.forName( classNamePrefix +entity);
                for (Field field : cls.getDeclaredFields()) {
                    String retS = getTypeNameWithParams(field);
//                    TypeVariable<? extends Class<?>>[] typeparams= type.getTypeParameters();
//
//                    if(typeparams.length!=0) retS.append(">");
                    ret.put(field.getName(), retS);
                }
                this.jsonMapper.writeValue(resp.getWriter(), ret);
            } catch (ClassNotFoundException e) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        }else if(Objects.equals(request, "values")){
            try {
                Class<?> cls = Class.forName(classNamePrefix+ entity);
                if (cls.isAnnotationPresent(Metadata.class)) {
                    List<?> ret = dbContext.search(cls, Map.of());
                    jsonMapper.writeValue(resp.getWriter(), ret);
                } else {
                    resp.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
                }
            } catch(ClassNotFoundException e){
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        }
    }

    private static String getTypeNameWithParams(Field field) {
        Type type= field.getGenericType();
        StringBuilder retS = new StringBuilder();
        String[] typeNames= type.getTypeName().split("<");
        String[] as=typeNames[0].split("\\.");
        retS.append(as[as.length-1]);
        if(typeNames.length>1) {
            retS.append("<");
            String[] ts = typeNames[1].split("\\.");
            String param = ts[ts.length - 1];
            retS.append(param);
        }
//        if(type instanceof ParameterizedType){
//           Type[] typeparams= ((ParameterizedType) type).getActualTypeArguments();
//        boolean first=true;
//        retS.append("<");
//        for(Type typeparam: typeparams){
//            if(first) first=false;
//            else retS.append(",");
//            retS.append(typeparam.getTypeName());
//        }
//        retS.append(">");
//        }
        return retS.toString();
    }
}

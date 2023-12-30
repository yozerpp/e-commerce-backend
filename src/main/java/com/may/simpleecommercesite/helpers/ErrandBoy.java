package com.may.simpleecommercesite.helpers;

import com.may.simpleecommercesite.beans.DBService;
import com.may.simpleecommercesite.entities.Entity;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;

public class ErrandBoy {
    public static String firstLetterToUpperCase(String str){
        char[] chars=str.toCharArray();
        chars[0]=Character.toUpperCase(chars[0]);
        String string= String.copyValueOf(chars);
        string=str.equals("Integer")?str.replace("eger", ""):string;
        return string;
    }
    public static Cookie createGlobalCookie(String name, String value){
        Cookie cookie=new Cookie(name, value);
        cookie.setMaxAge(24*60*60*14);
        cookie.setPath("/");
        return cookie;
    }
    public static String firstLetterToLowerCase(String str){
        char[] chars=str.toCharArray();
        chars[0]=Character.toLowerCase(chars[0]);
        String string= String.copyValueOf(chars);
        string=str.equals("Integer")?str.replace("eger", ""):string;
        return string;
    }
    public static boolean validateCredentialFormat(Map<String, Object> s){
        String password=(String) s.get("credential");
        return ((String) s.get("email")).matches("^\\w+@\\w+.com$") && password.length()>10;
    }
    public static boolean validateCredentialFormat(Map.Entry<String, Object> credential){
        switch (credential.getKey()){
            case "credential":
                return ((String)credential.getValue()).matches("\\d+&&[a-zA-Z]+");
            case "email":
            case "unregEmail":
                return ((String) credential.getValue()).matches("^\\w+@\\w+.com$");
            default: return false;
        }
    }
}

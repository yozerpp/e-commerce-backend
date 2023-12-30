package com.may.simpleecommercesite.filters;

import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.*;

public class ExtendedHttpRequest extends HttpServletRequestWrapper implements HttpServletRequest, ServletRequest {
    List<Cookie> addedCookies;
    public ExtendedHttpRequest(HttpServletRequest request) {
        super(request);
        addedCookies=new ArrayList<>();
        Collections.addAll(addedCookies, this.getCookies());
    }
    public void addCookie(Cookie cookie){
        addedCookies.add(cookie);
    }
    public Optional<Cookie> getCookie(String name) {
        return addedCookies.stream().filter(cookie -> cookie.getName().equals(name)).findFirst();
    }

    public List<Cookie> getAllCookies() {
        return addedCookies;
    }
}

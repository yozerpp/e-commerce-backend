package com.may.simpleecommercesite.filters;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class ExtendedHttpRequest extends HttpServletRequestWrapper {
    private boolean unauthorizedRedirected;
    public ExtendedHttpRequest(HttpServletRequest request) {
        super(request);
    }
    public boolean isUnauthorizedRedirected(){
        return unauthorizedRedirected;
    }
    public void setUnauthorizedRedirected(){
        unauthorizedRedirected=true;
    }
}

package com.may.simpleecommercesite.apiServlets;

import com.may.simpleecommercesite.beans.WrappedConnection;
import com.sun.org.apache.xml.internal.utils.WrappedRuntimeException;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;

public class LoginApiServlet extends BaseApiServlet {


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        super.doGet(req, resp);
    }
}

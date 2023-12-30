package com.may.simpleecommercesite.apiServlets;

import com.may.simpleecommercesite.annotations.Id;
import com.may.simpleecommercesite.beans.DBService;
import com.may.simpleecommercesite.entities.Entity;
import com.may.simpleecommercesite.entities.Sale;
import com.may.simpleecommercesite.helpers.ErrandBoy;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import javax.sql.DataSource;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ApiServlet extends HttpServlet {
    @Resource(name = "java:comp/env/jdbc/pool/test")
    DataSource dataSource;
    int activeRequest;
    boolean destroying;
    @Override
    public void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (destroying) {
            resp.setStatus(503);
            resp.addHeader("Retry-After", "3");
            return;
        }
        startService();
        super.service(req, resp);
        finishService();
    }

    protected synchronized void startService() {
        activeRequest++;
    }

    protected synchronized void finishService() {
        activeRequest--;
    }

    @Override
    public void destroy() {
        destroying = true;
        while (true) {
            if (activeRequest != 0) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {
                }
            } else {
                break;
            }
        }
        super.destroy();
    }
}

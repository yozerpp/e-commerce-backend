package com.may.simpleecommercesite.apiServlets;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class BaseApiServlet extends HttpServlet {
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

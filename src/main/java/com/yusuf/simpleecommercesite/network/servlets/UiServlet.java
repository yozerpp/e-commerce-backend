package com.yusuf.simpleecommercesite.network.servlets;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serial;

@WebServlet( urlPatterns = {"/", "/ecommerce"})
public class UiServlet extends HttpServlet {
    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if(pathInfo == null || pathInfo.isEmpty()){
            getServletContext().getRequestDispatcher("/root/index.html").include(req,resp);
        } else if(pathInfo.equals("/ecommerce")){
            resp.sendRedirect("/ecommerce/api");
        } else if(pathInfo.equals("/ecommerce/api")){
            getServletContext().getRequestDispatcher("swagger/index.html").include(req, resp);
        }
    }
}

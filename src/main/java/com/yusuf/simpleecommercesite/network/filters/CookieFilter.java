package com.yusuf.simpleecommercesite.network.filters;

import com.yusuf.simpleecommercesite.helpers.ErrandBoy;
import com.yusuf.simpleecommercesite.dbContext.DbContext;
import com.yusuf.simpleecommercesite.entities.Cart;
import com.yusuf.simpleecommercesite.entities.Customer;
import javax.persistence.Id;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

@WebFilter(servletNames = {"Cart", "User", "Invoice", "Login", "Product"}, filterName = "Cookie", asyncSupported = true)
public class CookieFilter extends HttpFilter {
    DbContext dbContext;
    protected static final Class<?>[] cookieClasses={Cart.class, Customer.class};
    @Override
    public void init() throws ServletException {
        super.init();
        this.dbContext= (DbContext) getServletContext().getAttribute("DbContext");
    }

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        for (Class<?> cookieClass : cookieClasses) {
            Cookie cookie= saveAndAddCookie(req, res, cookieClass);
            if (cookie!=null)
                req.setAttribute(cookieClass.getSimpleName() + "-Cookie", cookie);
        }
        chain.doFilter(req, res);
    }
    public static Cookie addCookie(HttpServletRequest req, HttpServletResponse res, Object entity) {
        return addCookie(req,res, entity, 0);
    }
    public static Cookie addCookie(HttpServletRequest req, HttpServletResponse res, final Object entity, int maxAge) {
        final Class<?> clazz= ErrandBoy.getRealClass(entity);
        String cookieVal= ErrandBoy.getAnnotatedFields(clazz, Id.class).stream().map(field->{
            try {
                return ErrandBoy.findGetter(field, clazz).invoke(entity).toString();
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }).reduce(new String() , (f, s) -> s+ (f.isEmpty()?"":(',' + f)));
        int lastCookieVersion= Arrays.stream(Optional.ofNullable(req.getCookies()).orElse(new Cookie[0])).filter(cookie-> cookie.getName().contains(clazz.getSimpleName())).map(Cookie::getVersion).max(Integer::compare).orElse(0);
        Cookie cookie= new Cookie(clazz.getSimpleName(), cookieVal);
        cookie.setVersion(lastCookieVersion+1);
        if (maxAge!=0) cookie.setMaxAge(maxAge);
        cookie.setPath("/");
        res.addCookie(cookie);
        return cookie;
    }
    private int getCookieId(String cookieName, String entityName){
        return Integer.parseInt(cookieName.substring(entityName.length()));
    }
    /***
     *
     * @param req
     * @param res
     * @param clazz
     * @return if cookie is added, returns the cookie. If cookie was already present and weren't added, then returns null.
     * @throws ServletException
     * @throws IOException
     */
    protected Cookie saveAndAddCookie(HttpServletRequest req, HttpServletResponse res, Class<?> clazz) throws ServletException, IOException {
        if((req.getCookies() == null || Arrays.stream(req.getCookies()).noneMatch(cookie -> cookie.getName().equals(clazz.getSimpleName()) && !Objects.equals(cookie.getValue(), "")))) {
            try {
                Object entity=dbContext.save(clazz.getConstructor().newInstance());
                return addCookie(req,res,entity, 60*60*24);
            } catch (SQLException | NoSuchMethodException | IllegalAccessException | InvocationTargetException |
                     InstantiationException e) {
                throw new RuntimeException(e);
            }
        } else return null;
    }
}

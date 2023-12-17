package com.may.simpleecommercesite.beans;

import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.json.*;
import javax.swing.*;
import java.io.Serializable;
import java.sql.*;
import java.util.Base64;
import java.util.function.Consumer;

@Dependent
public class DBService implements Serializable {
    @Inject
    private WrappedConnection wcon;
    private Connection con;
    @Inject
    SqlLogger logger;
    public DBService(){}
    @PostConstruct
    void setConnection(){
        con=wcon.getrealConnection();
    }
    JsonObject userByEmailCredential(String email, String password){
        JsonObject user=null;
        try(PreparedStatement statement = con.prepareStatement(userQuery)) {
            statement.setString(1, email);
            statement.setString(2, password);
            ResultSet rs= statement.executeQuery();
            if (rs.next()){
                user= Json.createObjectBuilder().add("cookieId", rs.getInt(1))
                        .add("email", rs.getString(2))
                        .add("credential", rs.getString(3))
                        .add("firstName", rs.getString(4))
                        .add("lastName", rs.getString(5))
                        .add("city", rs.getString(6))
                        .add("district", rs.getString(7))
                        .add("street", rs.getString(8))
                        .add("buildingNo", rs.getString(9))
                        .add("buildingName", rs.getString(10))
                        .add("innerDoorNo", rs.getInt(11))
                        .add("additional", rs.getInt(12)).build();
            }
        } catch (SQLException e) {
            logger.log(SqlLogger.SqlMethodType.QUERY, e);
        }
        return user;
    }
    void editUserByEmailCredential(Consumer<ResultSet> consumer, String email, String password) throws SQLException{
        con.setAutoCommit(false);
        try(PreparedStatement statement = con.prepareStatement(userQuery,
                ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)){
            statement.setString(1, email);
            statement.setString(2, password);
            consumer.accept(statement.executeQuery());
            con.commit();
        } catch (SQLException e){
            con.rollback();
            logger.log(SqlLogger.SqlMethodType.UPDATE,e);
            throw e;
        }finally {
            con.setAutoCommit(true);
        }
    }
    JsonObject cartById(int cartId) {
        JsonObject cart=null;
        JsonArrayBuilder sales=Json.createArrayBuilder();
        int cartTotal = 0;
        try (PreparedStatement table = con.prepareStatement(createTempCart);
            Statement select =con.createStatement();
            Statement drop=con.createStatement()){
            table.setInt(1, cartId);
            table.executeQuery();
            ResultSet rs= select.executeQuery(selectCart);
            while (rs.next()){
                sales.add(Json.createObjectBuilder()
                        .add("productId", rs.getInt("productId"))
                        // TODO Perform image encoding in a different thread?
                        .add("image", Base64.getEncoder().encodeToString(rs.getBytes("image")))
                        .add("title", rs.getString("title"))
                        .add("basePrice", rs.getDouble("basePrice"))
                        .add("baseDiscount", rs.getDouble("baseDiscount"))
                        .add("discountedPrice", rs.getDouble("discountedPrice"))
                        .add("couponCode", rs.getString("couponCode"))
                        .add("couponDiscount", rs.getDouble("couponDiscount"))
                        .add("quantity", rs.getInt("quantity"))
                        .add("saleTotal", rs.getDouble("saleTotal"))
                        .build());
                cartTotal=rs.getInt("cartTotal");
            }
            cart=Json.createObjectBuilder().add("cartTotal", cartTotal)
                    .add("sales", sales).build();
            drop.execute("drop temporary table IF EXISTS customerCart");
        } catch (SQLException e) {
            logger.log(SqlLogger.SqlMethodType.DDL, e);
        }
        return cart;
    }
    void editCartById(Consumer<ResultSet> consumer, int cartId){

        try (PreparedStatement table = con.prepareStatement(createTempCart);
             Statement select =con.createStatement();
             Statement drop=con.createStatement()){
            table.setInt(1, cartId);
            ResultSet rs=select.executeQuery(selectCart);

        } catch (SQLException e){
            try {
                con.rollback();
            } catch (Exception ignored){}
            logger.log(SqlLogger.SqlMethodType.UPDATE, e);
        }
    }
    public Connection getCon() {
        return con;
    }

    public void setCon(Connection con) {
        this.con = con;
    }
    private static final String userQuery="SELECT cookieId, email, credential, firstName, lastName," +
            " city, district, street, buildingNo, coalesce(buildingName,''), innerDoorNo, coalesce(additional, '') " +
            "FROM registeredcustomer r join address a ON r.email=a.email WHERE r.email=? AND credential=?";
    private static final String createTempCart="CREATE temporary table customerCart " +
            "SELECT c.total AS cartTotal ,s.quantity, s.productId, s.couponCode, s.totalPrice AS saleTotal FROM " +
            " cart c join sale s on s.cartId=c.cartId WHERE c.cartId=?";
    private static final String selectCart="SELECT  p.productId, p.image, p.title, p.basePrice, p.baseDiscount, p.discountedPrice, " +
            "c.couponCode, coalesce(c.discount, 0) AS couponDiscount, cc.quantity, cc.saleTotal, cc.cartTotal FROM customerCart cc " +
            "join product p ON cc.productId=p.productId LEFT OUTER JOIN coupon c ON cc.couponCode=c.couponCode";

}

package com.may.simpleecommercesite.helpers;

import com.may.simpleecommercesite.beans.SqlLogger;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.sql.RowSet;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;

public class ResultSetJsonConverter {
    public static JsonObject customer(ResultSet rs){
        JsonObject user=null;
        try {
            if (rs.next()) {
                user = Json.createObjectBuilder().add("cookieId", rs.getInt(1))
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
        }catch (SQLException ignored){}
        return user;
    }
    public static JsonObject cart(ResultSet rs){
        JsonObject cart=null;
        int cartTotal=0;
        JsonArrayBuilder sales=Json.createArrayBuilder();
        try {
            while (rs.next()) {
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
                cartTotal = rs.getInt("cartTotal");
            }
        } catch (SQLException ignored){}
        cart=Json.createObjectBuilder().add("cartTotal", cartTotal)
                .add("sales", sales).build();
        return  cart;
    }

}

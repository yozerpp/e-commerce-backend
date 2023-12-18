package com.may.simpleecommercesite.beans;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;


import javax.sql.DataSource;
import javax.sql.RowSet;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.JoinRowSet;
import javax.sql.rowset.RowSetProvider;

import java.io.Serializable;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class DBService implements Serializable{
    DataSource dataSource;
    private Connection connection;
    SqlLogger logger;
    public DBService(){}
    public DBService(DataSource ds, String username, String password) throws SQLException {
        this.dataSource=ds;
        connection=dataSource.getConnection(username,password);
        this.logger=new SqlLogger();
    }

    public CachedRowSet userByEmailCredential(String email, String password) {
        CachedRowSet user=null;
        try {
            user=newCachedRowset();
            user.setCommand(userQuery);
            user.setString(1, email);
            user.setString(2, password);
            user.execute(connection);
        } catch (SQLException e){
            logger.log(SqlLogger.SqlMethodType.QUERY, e);
        }
        return user;
    }
    public RowSet cartById(int cartId)  {
        CachedRowSet product = null;
        CachedRowSet sale=null;
        CachedRowSet cart=null;
        JoinRowSet jrs=null;
            int cartTotal = 0;
        try (PreparedStatement statement= connection.prepareStatement("SELECT c.cartId, c.total, c.ordered, quantity, totalPrice, p.basePrice, p.baseDiscount, coalesce(co.discount,0), p.productId, p.title, p.discountedPrice FROM cart c join sale s on c.cartId=s.cartId join product p on p.productId=s.productId left outer join coupon co on s.couponCode=co.couponCode WHERE s.cartId=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)){
            cart=newCachedRowset();
            cart.setCommand("SELECT cartId, total, ordered FROM cart WHERE cartId=?");
            cart.setInt(1, 2);
            cart.setMatchColumn(1);
            cart.execute(connection);
            sale=newCachedRowset();
            sale.setCommand("SELECT cartId, productId, quantity, couponCode, totalPrice FROM sale WHERE cartId=?");
            sale.setInt(1, 2);
            sale.setMatchColumn(1);
            sale.execute(connection);
            jrs=newJoinRowset();
            jrs.addRowSet(new CachedRowSet[]{cart, sale}, new int[]{1, 1});
            CachedRowSet crs= jrs.toCachedRowSet();
            crs.next();
            int a= crs.getInt("quantity");
            CachedRowSet rs=newCachedRowset();
            rs.setCommand("SELECT * FROM sale WHERE cartId=2 AND quantity=4");
            rs.execute(connection);
            rs.next();
            connection.setAutoCommit(false);
            crs.updateInt("quantity", 7);
            crs.acceptChanges(connection);
            crs.commit();
            rs.updateInt("quantity", 9);
            //doesnt work
            rs.acceptChanges(connection);
            rs.commit();

            Statement stmt=connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE ,ResultSet.CONCUR_UPDATABLE);
            ResultSet as= stmt.executeQuery("SELECT * FROM sale WHERE cartId=2 AND quantity=3");
            as.next();
            int c= as.getInt("quantity");
            //works
            as.updateInt("quantity", 8);
            as.updateRow();
            connection.commit();
            connection.setAutoCommit(true);
            int b=crs.getInt("quantity");
            logger.log(SqlLogger.SqlMethodType.INSERT, new SQLException("sample"));
        } catch (SQLException e){
            logger.log(SqlLogger.SqlMethodType.QUERY, e);
        }
        return jrs;
    }
    public int insertSale(int cartId, int productId, int quantity, String couponCode){
        int count = 0;
        try (PreparedStatement statement=connection.prepareStatement("INSERT INTO sale (cartId, productId, quantity, couponCode) values(?,?,?,?)")){
            statement.setInt(1, cartId);
            statement.setInt(2, productId);
            statement.setInt(3, quantity);
            statement.setString(4, couponCode);
            count= statement.executeUpdate();
        } catch (SQLException e){
            logger.log(SqlLogger.SqlMethodType.INSERT, e);
        }
        return count;
    }
    public int newCart(){
        int cartId=0;
        try (Statement statement=connection.createStatement();
            Statement id=connection.createStatement()){
            int count =statement.executeUpdate("INSERT INTO cart values ()");
            ResultSet rs= id.executeQuery("SELECT LAST_INSERT_ID() FROM cart");
            rs.next();
            cartId= Integer.parseInt(rs.getString(1));
        } catch (SQLException e) {
            logger.log(SqlLogger.SqlMethodType.INSERT, e);
        }
    return cartId;
    }
    public CachedRowSet newCachedRowset() throws SQLException{
        CachedRowSet rs=RowSetProvider.newFactory().createCachedRowSet();
        rs.setUrl(connectparams[0]);
        rs.setUsername(connectparams[1]);
        rs.setPassword(connectparams[2]);
        return rs;
    }
    public JoinRowSet newJoinRowset() throws SQLException{
        JoinRowSet rs=RowSetProvider.newFactory().createJoinRowSet();
        rs.setUrl(connectparams[0]);
        rs.setUsername(connectparams[1]);
        rs.setPassword(connectparams[2]);
        return rs;
    }
    public Connection getCon() {
        return connection;
    }
    Connection validateAndGetConnection() throws SQLException{
        if(!this.connection.isValid(0)){
            this.connection=this.dataSource.getConnection();
        }
        return this.connection;
    }

    public void setCon(Connection connection) {
        this.connection = connection;
    }
    private static final String userQuery="SELECT cookieId, cartId, r.email, credential, firstName, lastName," +
            " city, district, street, buildingNo, coalesce(buildingName,''), innerDoorNo, coalesce(additional, '') " +
            "FROM registeredcustomer r join address a ON r.email=a.email WHERE r.email=? AND credential=?";
    private static final String createTempCart="CREATE temporary table customerCart " +
            "SELECT c.total AS cartTotal ,s.quantity, s.productId, s.couponCode, s.totalPrice AS saleTotal FROM " +
            " cart c join sale s on s.cartId=c.cartId WHERE c.cartId=?";
    private static final String selectCart="SELECT  p.productId, p.image, p.title, p.basePrice, p.baseDiscount, p.discountedPrice, " +
            "c.couponCode, coalesce(c.discount, 0) AS couponDiscount, cc.quantity, cc.saleTotal, cc.cartTotal FROM customerCart cc " +
            "join product p ON cc.productId=p.productId LEFT OUTER JOIN coupon c ON cc.couponCode=c.couponCode";
    private static final String[] connectparams={"jdbc:mysql://localhost:3306/test", "root", "Yusuf_2002"};
}

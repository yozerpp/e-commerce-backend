import com.fasterxml.jackson.databind.ObjectMapper;
import com.may.simpleecommercesite.beans.DbContext;
import com.may.simpleecommercesite.entities.embedded.Address;
import com.may.simpleecommercesite.entities.*;
import com.may.simpleecommercesite.sqlUtils.CompiledStatement;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.mysql.cj.jdbc.Driver;
import java.sql.*;
import java.util.Map;

public class TestRunner {
    static DbContext dbContext;
    static Connection connection;
    static Driver driver;
    @BeforeAll
    public static void createContext() throws SQLException {
        driver=new Driver();
        DriverManager.registerDriver(driver);
        connection=DriverManager.getConnection("jdbc:mysql://localhost:3306/test", "root" , "Yusuf2002");
        dbContext=new DbContext(new ObjectMapper(), connection);
    }
    @Test
    public void StatementsTest() throws SQLException {
        CompiledStatement statement= new CompiledStatement(CompiledStatement.StatementType.SELECT, Product.class.getSimpleName());
        ResultSet rs= statement.columns("productId").where("productId", 1).execute(connection);
        rs.next();
        Assertions.assertEquals(1, rs.getInt("productId"));
        statement.close();
        rs.close();
        statement=new CompiledStatement(CompiledStatement.StatementType.UPDATE, Product.class.getSimpleName()).set("title", "anan").where("productId", 1);
        Assertions.assertTrue(statement.executeUpdate(connection)>0);
        statement.close();
        rs=new CompiledStatement(CompiledStatement.StatementType.INSERT, Cart.class.getSimpleName()).execute(connection);
        rs.next();
        Assertions.assertTrue(rs.getInt(1)!=0);
//        rs=new CompiledStatement(CompiledStatement.StatementType.INSERT, RegisteredCustomer.class.getSimpleName()).columns("email", "credential", "firstName", "lastName", "dateOfBirth").values("ggfgdfgdfgf@htr.com", "DFDSS2312312", "AASD", "ADSAD", new Timestamp(System.currentTimeMillis())).execute(connection);
//        rs.next();
//        Assertions.assertTrue(rs.getInt(1)!=0 );
//        rs.close();
//        Assertions.assertTrue(new CompiledStatement(CompiledStatement.StatementType.DELETE, RegisteredCustomer.class.getSimpleName()).where("email","a@b.com").executeUpdate(connection)>0);
    }
    @Test
    public void dbContextTests() throws SQLException {
        Assertions.assertTrue(new Product(1).equals(dbContext.findById(Product.class, 1)));
        Assertions.assertTrue(dbContext.save(new Cart()).getCartId()!=0);
        RegisteredCustomer customer=new RegisteredCustomer("a@b.com");
        customer.setFirstName("fgfdg");
        dbContext.save(customer);
//        Assertions.assertTrue(dbContext.save(new RegisteredCustomer("asd", "asd","fdgdfg@b.com", "dfgdfg", new Timestamp(System.currentTimeMillis()))).getCookieId()!=0);
        try {
            dbContext.save(new Sale(new Product(1), new Cart(1)));
        } catch (SQLException e){
            Assertions.assertEquals("45000", e.getSQLState());
        }
        try {
            dbContext.save(new Sale(new Product(13), new Cart(17)));
        } catch (SQLException e){
            Assertions.assertEquals(1644, e.getErrorCode());
        }
        Invoice invoice=new Invoice(12);
        invoice.setPaymentMethod(Invoice.PaymentMethod.AtDoorCash);
        try {
            dbContext.save(invoice);
        } catch (SQLException e){
            Assertions.assertEquals("45000", e.getSQLState());
        }
        invoice=new Invoice();
        invoice.setUnregEmail("ad@asd.com");
        invoice.setCart(new Cart(254));
        invoice.setDateOrdered(new Timestamp(System.currentTimeMillis()));
        invoice.setPaymentMethod(Invoice.PaymentMethod.OnlineCard);
        invoice.setInvoiceStatus(Invoice.Status.Complete);
        invoice.setDeliveryAddress(new Address("a","b", "c", "d", "e"));
//        Assertions.assertTrue(dbContext.save(invoice).getInvoiceId()!=0);
        invoice.setInvoiceStatus(Invoice.Status.Canceled);
//        try {
//            dbContext.save(invoice);
//        }catch (SQLException e){
//            Assertions.assertEquals("45001",e.getSQLState());
//        }
        invoice= new Invoice();
        invoice.setUnregEmail("fdgfdg@sdfsdf");
        invoice.setCart(new Cart(74));
        invoice.setPaymentMethod(Invoice.PaymentMethod.OnlineCard);
        invoice.setDeliveryAddress(new Address("a","b", "c", "d", "e") );
//        dbContext.save(invoice);
        invoice.setPaymentMethod(Invoice.PaymentMethod.AtDoorCash);
//        try{
//            dbContext.save(invoice);
//        } catch (SQLException e){
//            Assertions.assertEquals("45002", e.getSQLState());
//        }
        Assertions.assertFalse(dbContext.search(Product.class, Map.of("title", "ürün")).isEmpty());
    }
    @AfterAll
    public static void closeDriver() throws SQLException {
        connection.close();
        DriverManager.deregisterDriver(driver);
    }
}


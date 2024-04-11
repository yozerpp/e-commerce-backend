//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.may.simpleecommercesite.beans.DbContext;
//import com.may.simpleecommercesite.entities.embedded.Address;
//import com.may.simpleecommercesite.entities.*;
//import com.may.simpleecommercesite.sqlUtils.StatementBuilder;
//import org.apache.commons.dbcp2.BasicDataSource;
//import org.junit.jupiter.api.AfterAll;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.Test;
//import com.mysql.cj.jdbc.Driver;
//import java.sql.*;
//import java.util.Map;
//
//public class TestRunner {
//    static DbContext dbContext;
//    static Connection connection;
//    @BeforeAll
//    public static void createContext() throws SQLException {
//        BasicDataSource dataSource=new BasicDataSource();
//        dataSource.setDriver(new Driver());
//        dataSource.setUrl("jdbc:mysql://localhost:3306/ecommerce");
//        dataSource.setUsername("root");
//        dataSource.setPassword("123456");
//        connection= dataSource.getConnection();
//        dbContext=new DbContext(new ObjectMapper(), dataSource);
//    }
//    @Test
//    public void StatementsTest() throws SQLException {
//        StatementBuilder builder= new StatementBuilder(StatementBuilder.StatementType.SELECT);
//        PreparedStatement statement;
//        builder.table(Product.class.getSimpleName());
//        ResultSet rs= builder.columns("productId").where("productId", 1).build(connection).executeQuery();
//        rs.next();
//        Assertions.assertEquals(1, rs.getInt("productId"));
//        rs.close();
//        builder=new StatementBuilder(StatementBuilder.StatementType.UPDATE).table(Product.class.getSimpleName()).set("title", "anan").where("productId", 1);
//        Assertions.assertTrue(builder.build(connection).executeUpdate()>0);
//        statement=new StatementBuilder(StatementBuilder.StatementType.INSERT).table(Cart.class.getSimpleName()).build(connection);
//        statement.executeUpdate();
//        rs=statement.getGeneratedKeys();
//        rs.next();
//        Assertions.assertTrue(rs.getInt(1)!=0);
////        rs=new CompiledStatement(CompiledStatement.StatementType.INSERT, RegisteredCustomer.class.getSimpleName()).columns("email", "credential", "firstName", "lastName", "dateOfBirth").values("ggfgdfgdfgf@htr.com", "DFDSS2312312", "AASD", "ADSAD", new Timestamp(System.currentTimeMillis())).execute(connection);
////        rs.next();
////        Assertions.assertTrue(rs.getInt(1)!=0 );
////        rs.close();
////        Assertions.assertTrue(new CompiledStatement(CompiledStatement.StatementType.DELETE, RegisteredCustomer.class.getSimpleName()).where("email","a@b.com").executeUpdate(connection)>0);
//    }
//    @Test
//    public void dbContextTests() throws SQLException {
//        Assertions.assertTrue(new Product(1).equals(dbContext.findById(Product.class, 1)));
//        Assertions.assertTrue(dbContext.save(new Cart()).getId()!=0);
//        Customer customer=new Customer("sdfdsa@b.com", "asdasd");
//        try {
//            dbContext.save(customer);
//        } catch(SQLException e){
////            Assertions.assertEquals();
//        }
////        Assertions.assertTrue(dbContext.save(new RegisteredCustomer("asd", "asd","fdgdfg@b.com", "dfgdfg", new Timestamp(System.currentTimeMillis()))).getCookieId()!=0);
//        try {
//            Cart cart= dbContext.save(new Cart());
//            dbContext.save(new Sale(new Product(1), new Cart(1)));
//        } catch (SQLException e){
//            Assertions.assertEquals("45002", e.getSQLState());
//        }
//        try {
//            dbContext.save(new Sale(new Product(1), new Cart(1)));
//        } catch (SQLException e){
//            Assertions.assertEquals(1644, e.getErrorCode());
//        }
//        Invoice invoice=new Invoice(12);
//        invoice.setPaymentMethod(Invoice.PaymentMethod.AtDoorCash);
//        try {
//            dbContext.save(invoice);
//        } catch (SQLException e){
//            Assertions.assertEquals("45002", e.getSQLState());
//        }
//        invoice=new Invoice();        invoice.setCart(new Cart(254));
//        invoice.setCreationDate(new Timestamp(System.currentTimeMillis()));
//        invoice.setPaymentMethod(Invoice.PaymentMethod.OnlineCard);
//        invoice.setStatus(Invoice.Status.Completed);
//        invoice.setDeliveryAddress(new Address("a","b", "c", "d", "e","f"));
////        Assertions.assertTrue(dbContext.save(invoice).getInvoiceId()!=0);
//        invoice.setStatus(Invoice.Status.Canceled);
////        try {
////            dbContext.save(invoice);
////        }catch (SQLException e){
////            Assertions.assertEquals("45001",e.getSQLState());
////        }
//        invoice= new Invoice();
//        invoice.setCart(new Cart(74));
//        invoice.setPaymentMethod(Invoice.PaymentMethod.OnlineCard);
//        invoice.setDeliveryAddress(new Address("a","b", "c", "d", "e","f") );
////        dbContext.save(invoice);
//        invoice.setPaymentMethod(Invoice.PaymentMethod.AtDoorCash);
////        try{
////            dbContext.save(invoice);
////        } catch (SQLException e){
////            Assertions.assertEquals("45002", e.getSQLState());
////        }
//        Assertions.assertFalse(dbContext.search(Product.class, Map.of("title", "ürün")).isEmpty());
//    }
//}
//

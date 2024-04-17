//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.json.JsonMapper;
//import com.yusuf.simpleecommercesite.dbContext.DbContext;
//import com.yusuf.simpleecommercesite.entities.embedded.Address;
//import com.yusuf.simpleecommercesite.entities.*;
//import com.yusuf.simpleecommercesite.dbContext.sqlUtils.StatementBuilder;
//import org.apache.commons.dbcp2.BasicDataSource;
//import org.junit.jupiter.api.AfterAll;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.Test;
//import com.mysql.cj.jdbc.Driver;
//
//import javax.sql.DataSource;
//import java.io.File;
//import java.io.IOException;
//import java.net.URISyntaxException;
//import java.net.URL;
//import java.sql.*;
//import java.util.Map;
//
//public class TestRunner {
//    static DbContext dbContext;
//    static DataSource dataSource;
//    static JsonNode data;
//    static final JsonMapper json= JsonMapper.builder().build();
//    static final URL DATA_FILE=TestRunner.class.getClassLoader().getResource("testData.json");
//    @BeforeAll
//    public static void createContext() throws SQLException, IOException {
//        data=json.readTree(DATA_FILE);
//        BasicDataSource dataSource=new BasicDataSource();
//        dataSource.setDriver(new Driver());
//        dataSource.setUrl("jdbc:mysql://localhost:3306/ecommerce");
//        dataSource.setUsername("root");
//        dataSource.setPassword("123456");
//        TestRunner.dataSource=dataSource;
//        dbContext=new DbContext(new ObjectMapper(), dataSource);
//    }
//    @AfterAll
//    public static void saveTestData() throws URISyntaxException, IOException {
//        json.writer().writeValue(new File(DATA_FILE.toURI()), data);
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

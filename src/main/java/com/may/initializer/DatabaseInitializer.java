package com.may.initializer;

import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.dbcp2.BasicDataSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.may.simpleecommercesite.entityManager.DbContext;
import com.may.simpleecommercesite.entities.*;
import com.may.simpleecommercesite.entities.embedded.*;
import com.mysql.cj.jdbc.Driver;

import javax.imageio.ImageIO;

public class DatabaseInitializer {

    static final String desc= "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Fusce posuere lorem in dui cursus dignissim. Proin feugiat eget tortor sed imperdiet. Morbi sit amet arcu vel mi pellentesque aliquam. Donec nec facilisis ligula. Phasellus lorem nisi, commodo cras.";
    static final Address address=new Address("İzmir", "Konak","Alsancak","Kıbrıs Şehitleri", "27","1");
    static final int NUM_SELLERS =200;
    static final int NUM_BRANDS =100;
    static final int NUM_CATEGORIES =80;
    static final int NUM_IMAGES=300;
    static final int NUM_CUSTOMER=2000;
    static final int NUM_PRODUCTS =4000;
    static final int NUM_COUPONS=NUM_SELLERS*2;
    static final int NUM_CARTS=(NUM_CUSTOMER)*2;
    static final int NUM_INVOICES=NUM_CARTS;
    static final int MAX_SALES_PER_CART=5;
    static final int NUM_RATINGS=NUM_CARTS*MAX_SALES_PER_CART;
    static final int NUM_VOTES_PER_RATING =15;

    static final List<Seller> sellers=new ArrayList<>();
    static final List<Brand> brands=new ArrayList<>();
    static final List<Category> categories=new ArrayList<>();
    static final List<Cart> carts=new ArrayList<>(NUM_CARTS);
    static final List<Product> products=new ArrayList<>(NUM_PRODUCTS);
    static final List<Coupon> coupons=new ArrayList<>(NUM_COUPONS);
    static final List<Customer> customers=new ArrayList<>(NUM_CUSTOMER);
    static final List<Invoice> invoices=new ArrayList<>(NUM_CARTS);
    static final List<byte[]> images=new ArrayList<>(NUM_IMAGES);
    static final Set<Rating> ratings=new HashSet<>(NUM_RATINGS);
    static final Set<RatingVote> ratingvotes=new HashSet<>(NUM_RATINGS* NUM_VOTES_PER_RATING /2);
    static final DbContext dbContext;
    static final Random random=new Random();
    static final HttpClient client=HttpClient.newBuilder().build();
    static final int NUM_THREADS=256;
    static final ExecutorService ex=Executors.newFixedThreadPool(NUM_THREADS);
    static {
        try {
            BasicDataSource dataSource=new BasicDataSource();
            dataSource.setUrl("jdbc:mysql://localhost:3306/ecommerce");
            dataSource.setPassword("123456");
            dataSource.setDefaultSchema("ecommerce");
            dataSource.setUsername("root");
            dataSource.setTestOnBorrow(true);
            dataSource.setValidationQuery("SELECT 1");
            dataSource.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
            dataSource.setDriver(new Driver());
            dataSource.setPoolPreparedStatements(true);
            dataSource.setInitialSize(2500);
            dbContext=new DbContext(new ObjectMapper(), dataSource);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    static void initializeImages() throws URISyntaxException, IOException, InterruptedException {
        fetchImages();
        File imageDir=new File("./images");
        File[] files=imageDir.listFiles();
        CountDownLatch latch =new CountDownLatch(files.length);
        for(File img:files){
            ex.submit(()-> {
                try (FileInputStream file = new FileInputStream(img)) {
                    byte[] data = file.readAllBytes();
                    synchronized (images) {
                        images.add(data);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                latch.countDown();
            });
        }
        synchronized (latch) {
            latch.await();
        }
    }
    static void fetchImages() throws URISyntaxException, IOException, InterruptedException {
        File imageDir=new File("./images");
        imageDir.mkdir();
        if(imageDir.listFiles().length>0) return;
        List<CompletableFuture<Void>>resps=new ArrayList<>();
        for (int i = 0; i < NUM_IMAGES; i++) {
            final int finalI = i;
            HttpResponse<String> resp= client.send(HttpRequest.newBuilder(new URI("https://picsum.photos/200/300")).GET().build()
                    , HttpResponse.BodyHandlers.ofString());
            Optional<String> loc=resp.headers().firstValue("Location");
            if(loc.isEmpty())
                continue;
            CompletableFuture<Void> respp= client.sendAsync(HttpRequest.newBuilder(new URI(loc.get())).GET().header("Accept", "image/jpeg").build(), HttpResponse.BodyHandlers.ofByteArray())
                    .thenAccept(httpResponse -> {
                        byte[] img = httpResponse.body();
                        System.out.println(httpResponse.statusCode() + ", "+ img.length);
                        try (ByteArrayInputStream stream = new ByteArrayInputStream(img)) {
                            BufferedImage image = ImageIO.read(stream);
                            File out = new File(imageDir.getPath() + "/" + finalI + ".jpg");
                            ImageIO.write(image, "jpg", out);
                        } catch (IOException e){
                            throw new RuntimeException(e);
                        }
                    });
            resps.add(respp);
        }
        CompletableFuture<Void> all_resps=CompletableFuture.allOf(resps.toArray(new CompletableFuture[0]));
        Object lock = new Object();
        all_resps.thenRun(()->{
            synchronized (lock) {
                lock.notifyAll();
            }
        });
        synchronized (lock){
            lock.wait();
        }
    }
    public static void initializeSeller() {
        CountDownLatch latch=new CountDownLatch(NUM_SELLERS);
        for(int i = 1; i<= NUM_SELLERS; i++){
            int finalI = i;
            ex.submit(()->{
            Seller seller=new Seller();
            seller.setName("satıcı"+ finalI);
            seller.setAddress(address);
                try {
                    seller= dbContext.save(seller);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                synchronized (sellers) {
                    sellers.add(seller);
                }
                latch.countDown();
            });
        }
        try {
            synchronized (latch) {
                latch.await();
            }
        } catch (InterruptedException e){
        }
    }
    public static void initializeBrand() {
        CountDownLatch latch=new CountDownLatch(NUM_BRANDS);
        for(int i = 1; i<= NUM_BRANDS; i++){
            int finalI = i;
            ex.submit(()-> {
                Brand brand = new Brand();
                brand.setName("marka" + finalI);
                try {
                    brand = dbContext.save(brand);
                } catch (SQLException e) {
                    e.printStackTrace(System.err);
                    throw new RuntimeException(e);
                }
                synchronized (brands) {
                    brands.add(brand);
                }
                latch.countDown();
            });
        }
        try {
            synchronized (latch) {
                latch.await();
            }
        } catch (InterruptedException e){
        }
    }
    static final int maxSubCategories=3;
    public static Category createCategory(Category parent, AtomicInteger count) throws SQLException {
        List<Category> subCategories=new ArrayList<>();
        Category category=new Category();
        category.setParent(parent);
        category.setName("kategori" + count.get());
        category =dbContext.save(category);
        categories.add(category);
        for(int i=0; i<random.nextInt(maxSubCategories) && count.get() >1; i++) {
            count.decrementAndGet();
            subCategories.add(createCategory(category, count));
        }
        return category;
    }
    public static void initializeCategory() throws SQLException {
        for(AtomicInteger i = new AtomicInteger(NUM_CATEGORIES +1); i.get()>1;){
            i.decrementAndGet();
            createCategory(null,i);
        }
    }
    public static void initializeCoupon() {
        CountDownLatch latch=new CountDownLatch(NUM_COUPONS);
        String code="AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
        for(int i=0; i<NUM_COUPONS; i++) {
            final String finalCode=code;
            code=incrementString(code);
            ex.submit(()-> {
                Seller seller = sellers.get(random.nextInt(sellers.size()));
                Coupon coupon = new Coupon();
                coupon.setDiscount(random.nextInt(100));
                coupon.setSeller(seller);
                coupon.setId(finalCode);
                try {
                    coupon = dbContext.save(coupon);
                    synchronized (coupons) {
                        coupons.add(coupon);
                    }
                } catch (SQLException e) {
                    e.printStackTrace(System.err);
                    throw new RuntimeException(e);
                } finally {
                    latch.countDown();
                }
            });
        }
        try {
            synchronized (latch) {
                latch.await();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    static final int productMaxPrice=100000000;
    public static void initializeProduct() {
        CountDownLatch latch=new CountDownLatch(NUM_PRODUCTS);
        for (int i = 0; i < NUM_PRODUCTS; i++) {
            int finalI = i;
            ex.submit(()-> {
                Product product = new Product();
                product.setBrand(brands.get(random.nextInt(brands.size())));
                product.setCategory(categories.get(random.nextInt(categories.size())));
                product.setSeller(sellers.get(random.nextInt(sellers.size())));
                product.setDiscount(random.nextInt(50));
                Date date = new Date();
                date.setDate(random.nextInt(31));
                date.setMonth(random.nextInt(12));
                date.setYear(date.getYear() + random.nextInt( 5)-4);
                product.setDateAdded(new Timestamp(date.getTime()));
                product.setDescription(desc);
                product.setOriginalPrice(BigDecimal.valueOf(random.nextDouble()*productMaxPrice +0.1));
                int numImages = random.nextInt( 3) +1;
                int mainImageIdx = random.nextInt(numImages);
                List<Image> imgs = new ArrayList<>();
                product.setTitle("product" + finalI);
                try {
                    product = dbContext.save(product);
                    synchronized (products) {
                        products.add(product);
                    }
                for (int j = 0; j < numImages; j++) {
                    Image image = new Image(images.get(random.nextInt(images.size())), j == mainImageIdx);
                    image.setProduct(product);
                    dbContext.save(image);
                }
                } catch (SQLException e) {
                    e.printStackTrace(System.err);
                    throw new RuntimeException(e);
                } finally {
                    latch.countDown();
                }
            });
        }
        try {
            synchronized (latch) {
                latch.await();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
   static void initializeCustomer() throws SQLException {
        AtomicInteger regIdxx=new AtomicInteger(0);
        CountDownLatch latch=new CountDownLatch(NUM_CUSTOMER);
        for(int i=0; i<NUM_CUSTOMER; i++){
            ex.submit(()-> {
                Customer customer = new Customer();
                if (random.nextBoolean()) {
                    int regIdx=regIdxx.getAndIncrement();
                    customer.setCart(carts.get(regIdx));
                    customer.setEmail("email" + regIdx + "@" + "hotmail.com");
                    customer.setFirstName("isim" + regIdx);
                    customer.setLastName("soyisim" + regIdx);
                    Date date = new Date();
                    date.setDate(random.nextInt(31));
                    date.setMonth(random.nextInt(12));
                    date.setYear(date.getYear() -random.nextInt(50)-5);
                    customer.setBirthDate(new Timestamp(date.getTime()));
                    customer.setAddress(address);
                    customer.setPassword("password" + regIdx);
                }
                try {
                    customer = dbContext.save(customer);
                    synchronized (customers) {
                        customers.add(customer);
                    }
                } catch (SQLException e) {
                    e.printStackTrace(System.err);
                    throw new RuntimeException(e);
                } finally {
                    latch.countDown();
                }
            });
        }
       try {
           synchronized (latch) {
               latch.await();
           }
       } catch (InterruptedException e) {
           throw new RuntimeException(e);
       }
   }
   static void initializeCart()  {
        CountDownLatch latch=new CountDownLatch(NUM_CARTS);
        for(int i=0; i<NUM_CARTS; i++){
            ex.submit(()-> {
                Cart cart = null;
                try {
                    cart = dbContext.save(new Cart());
                    synchronized (carts) {
                        carts.add(cart);
                    }
                } catch (SQLException e) {
                    e.printStackTrace(System.err);
                    throw new RuntimeException(e);
                }
                finally{
                    latch.countDown();
                }
            });
        }
       try {
           synchronized (latch) {
               latch.await();
           }
       } catch (InterruptedException e) {
           throw new RuntimeException(e);
       }
   }
   static void initializeSale(){
        carts.forEach(cart -> {
            ex.submit(()-> {
                for (int j = 0; j < random.nextInt(MAX_SALES_PER_CART) + 1; j++) {
                    try {
                        dbContext.save(new Sale(products.get(random.nextInt(products.size())), cart));
                    } catch (SQLException e) {
                        e.printStackTrace(System.err);
                        throw new RuntimeException(e);
                    }
                }
            });
        });
   }
   static void initializeInvoice() {
        final Invoice.PaymentMethod[] methods= Invoice.PaymentMethod.values();
        final Invoice.Status[] statuses=Invoice.Status.values();
        CountDownLatch latch=new CountDownLatch(NUM_CARTS);
        for(int i=0; i<NUM_CARTS; i++){
            int finalI = i;
            ex.submit(()-> {
                Invoice invoice = new Invoice();
                invoice.setCart(carts.get(finalI));
                invoice.setPaymentMethod(methods[random.nextInt(methods.length)]);
                invoice.setStatus(statuses[random.nextInt(statuses.length)]);
                invoice.setDeliveryAddress(address);
                Customer customer = customers.get(random.nextInt(customers.size()));
                invoice.setCustomer(customer);
                if (customer.getEmail() == null)
                    invoice.setEmail("unregEmail" + finalI + "@" + "gmail.com");
                else
                    invoice.setEmail(customer.getEmail());
                Date date = new Date();
                date.setDate(random.nextInt(31));
                date.setMonth(random.nextInt(12));
                date.setHours(random.nextInt(24));
                invoice.setCreationDate(new Timestamp(date.getTime()));
                try {
                    invoice = dbContext.save(invoice);
                } catch (SQLException e) {
                    e.printStackTrace(System.err);
                    throw new RuntimeException(e);
                }
                finally {
                    latch.countDown();
                }
                synchronized (invoices) {
                    invoices.add(invoice);
                }

            });
        }
       try {
           synchronized (latch) {
               latch.await();
           }
       } catch (InterruptedException e) {
           throw new RuntimeException(e);
       }
   }
   static void initializeRating() {
        final CountDownLatch latch=new CountDownLatch(NUM_RATINGS);
        for(int i=0; i<NUM_RATINGS; i++){
            int finalI = i;
            ex.submit(()-> {
                Rating rating = new Rating();
                Customer customer=customers.get(random.nextInt(customers.size()));
                Product product=products.get(random.nextInt(products.size()));
                rating.setCustomer(customer);
                rating.setProduct(product);
//                boolean ret=false;
//                synchronized (ratigs) {
//                    if (((HashSet<Rating>)ratings).contains(rating))ret=true;
//                    else ratings.add(rating);
//                }
//                if(ret) {
//                    latch.countDown();
//                    return;
//                }
                rating.setRating(BigDecimal.valueOf(random.nextDouble()*5).setScale(2, RoundingMode.HALF_DOWN));
                rating.setComment(desc);
                Date date = new Date();
                date.setDate(random.nextInt(31));
                date.setMonth(random.nextInt(12));
                date.setHours(random.nextInt(24));
                rating.setDateRated(new Timestamp(date.getTime()));
                if (customer.getEmail() == null) {
                    rating.setFirstName("Kayıtsız İsim" + finalI);
                    rating.setLastName("Kayıtsız Soyisim" + finalI);
                } else {
                    rating.setFirstName(customer.getFirstName());
                    rating.setLastName(customer.getFirstName());
                }
                boolean retry=false;
                do {
                    try {
                        rating = dbContext.insert(rating);
                        synchronized (ratings){
                            ratings.add(rating);
                        }
                        latch.countDown();
                    } catch (SQLException e) {
                        if(e.getMessage().contains("Deadlock")) {
                            retry=true;
                            e.printStackTrace(System.err);
                        } else if(e.getErrorCode()==1062) latch.countDown();
                        else{
                            e.printStackTrace(System.err);
                            latch.countDown();
                            throw new RuntimeException(e);
                        }
                    }
                } while(retry);
            });
        }
       try {
           synchronized (latch) {
               latch.await();
           }
       } catch (InterruptedException e) {
           throw new RuntimeException(e);
       }
   }
   static void initializeVote(){
        CountDownLatch latch=new CountDownLatch(NUM_VOTES_PER_RATING*ratings.size());
        ratings.forEach(rating-> {
            ex.submit(()->{
                for (int j = 0; j < NUM_VOTES_PER_RATING; j++) {
                    RatingVote vote = new RatingVote(rating, customers.get(random.nextInt(customers.size())));
                    vote.setVote(random.nextBoolean() ? RatingVote.VoteType.UP : RatingVote.VoteType.DOWN);
                    boolean retry=false;
                    try {
                        dbContext.insert(vote);
                    } catch (SQLException e) {
                        if(e.getErrorCode()!=1062) {
                            e.printStackTrace(System.err);
                            if (!e.getMessage().contains("Deadlock"))
                                throw new RuntimeException(e);
                        }
                    } finally {
                        latch.countDown();
                    }
                }
            });
        });
       try {
           latch.await();
       }catch (InterruptedException e){
           throw new RuntimeException(e);
       }
   }
   static void p(String s){
        System.out.println("initialized" + " " +s);
   }
    public static void main (String[] args) throws IOException, SQLException, URISyntaxException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, InterruptedException {
        initializeBrand();
        p("brand");
        initializeCategory();
        p("category");
        initializeSeller();
        p("seller");
        initializeCoupon();
        p("coupon");
        initializeImages();
        p("images");
        initializeProduct();
        p("product");
        initializeCart();
        p("cart");
        initializeCustomer();
        p("customer");
        initializeInvoice();
        p("invoice");
        initializeRating();
        p("rating");
        initializeVote();
        p("votes");
        ex.shutdown();
    }
    static String incrementString(String s){
        char[]chars=s.toCharArray();
        for(int i= chars.length-1; i>=0; i--){
            if((chars[i]>=97 && chars[i]<122) || (chars[i]<90&&chars[i]>=65) || (chars[i] < 57 && chars[i] >=48)){
                chars[i]= (char) (chars[i]+1);
                break;
            } else if(chars[i] >=122){
                chars[i]=48;
            }
        }
        return String.valueOf(chars);
    }
}
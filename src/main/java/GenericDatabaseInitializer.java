import com.fasterxml.jackson.databind.ObjectMapper;
import com.yusuf.simpleecommercesite.dbContext.DbContext;
import com.yusuf.simpleecommercesite.entities.Category;
import com.yusuf.simpleecommercesite.entities.Product;
import com.yusuf.simpleecommercesite.entities.annotations.Unique;
import com.yusuf.simpleecommercesite.helpers.ErrandBoy;
import org.postgresql.Driver;
import com.yusuf.simpleecommercesite.entities.annotations.Entity;
import javax.persistence.*;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class GenericDatabaseInitializer{
    private DbContext dbContext;
    private final Class<?>[] entityClasses;
    private final Map<Class<?>, List<Object>> saved = new HashMap<>();
    private  Map< String, Integer> numInstances = null;
    private int baseNumInstances;
    public static void main(String[] args) throws MalformedURLException, SQLException {
        Product product = new Product();
        Map<String, Integer> numInstances = new HashMap<>();
        numInstances.put(null, 100);
        numInstances.put("Category",20);
        numInstances.put("Brand",100);
        numInstances.put("Product", 1000);
        numInstances.put("Customer",100);
        numInstances.put("Cart", numInstances.get("Customer") * 21);
        numInstances.put("Image", numInstances.get("Product") * 3);
        numInstances.put("Invoice", numInstances.get("Customer") * 20);
        numInstances.put("Sale", numInstances.get("Cart")* 10);
        numInstances.put("Rating", numInstances.get("Product")*30);
        numInstances.put("RatingVote", numInstances.get("Rating") * 20);
        GenericDatabaseInitializer initializer= new GenericDatabaseInitializer(args[0],args[1], args[2], args[3], Arrays.stream(args).skip(4).toArray(String[]::new), numInstances);
        initializer.setImageSource("https://picsum.photos/200/300");
        initializer.initialize();
    }
    private GenericDatabaseInitializer(@NotNull String[] packageNames){
        this(packageNames, null);
    }

    private GenericDatabaseInitializer(@NotNull String[] packageNames, ClassLoader classLoader){
        ClassLoader ldr;
        if (classLoader == null){
            ldr = Thread.currentThread().getContextClassLoader();
        }else ldr= classLoader;
        List<String> packageNameList = Arrays.asList(packageNames);
        entityClasses= Arrays.stream(Package.getPackages()).filter(aPackage -> packageNameList.contains(aPackage.getName()))
                .map(aPackage -> {
                    InputStream packageContents= ldr.getResourceAsStream(aPackage.getName().replace('.', '/'));
                    BufferedReader dirReader= new BufferedReader(new InputStreamReader(packageContents));
                    return dirReader.lines().filter(packageContent-> packageContent.endsWith(".class")).map(className-> aPackage.getName() + '.' + className.replace(".class", "")).collect(Collectors.toList());
                }).reduce(new ArrayList<>(), (acc, i)->{acc.addAll(i); return acc;}).stream().map(className-> {
                    try {
                        return ldr.loadClass(className);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }).filter(clazz->clazz.isAnnotationPresent(Entity.class)).toArray(Class<?>[]::new);
        try {
            DriverManager.registerDriver(new Driver());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public GenericDatabaseInitializer(@NotNull String connString, String userName, String password, String schema,@NotNull String[] packageNames, Map<String, Integer> instances){
        this(packageNames);
        this.numInstances=instances;
        baseNumInstances=getNumInstances(instances);
        try {
            this.dbContext = new DbContext(new ObjectMapper(),connString, userName, password, schema);
        } catch (SQLException e) {
            System.err.println("Wrong credentials " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    public GenericDatabaseInitializer(@NotNull DbContext dbContext, @NotNull String[] packageNames, Map<String, Integer> instances)  {
        this(packageNames);
        this.numInstances=instances;
        baseNumInstances= getNumInstances(instances);
        this.dbContext = dbContext;
    }
    private int getNumInstances(Map<String,Integer> instances){
        if (instances == null || !instances.containsKey(null)) return  100;
        else return instances.get(null);
    }
    public void initialize() {
        Arrays.stream(entityClasses).forEach(entityClass -> {
            int count = Optional.ofNullable(numInstances.get(entityClass.getSimpleName())).orElse(numInstances.get(null));
            System.out.println("initializing " + entityClass.getSimpleName());
            for (int i =0; i< count; i++)
                saveAndGet(entityClass, null);
            System.out.println("done.");
        });
        System.out.println("Initialized all.");
    }

    Stack<Class<?>> recursionStack= new Stack<>();
    private <T> T saveAndGet(Class<T> to, Object from){
        if (from==null&&ErrandBoy.getAnnotatedFields(to, Id.class).stream().allMatch(field -> field.isAnnotationPresent(ManyToOne.class))) return getFromCache(to);
        T retVal=null;
         AtomicInteger lastInstanceIdWrapper=lastInstanceIds.get(to);
        if (lastInstanceIdWrapper ==null){
            lastInstanceIdWrapper=new AtomicInteger(0);
            lastInstanceIds.put(to, lastInstanceIdWrapper);
        }
        final Integer lastInstanceId = lastInstanceIdWrapper.incrementAndGet();
        if ((saved.get(to)!=null && saved.get(to).size() >= getMaxInstances(to,  null)) || recursionStack.contains(to))
            return getFromCache(to);
        recursionStack.push(to);
        final T inst;
        try {
            inst = to.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        final List<Field> relatedIdFields = new ArrayList<>();
        final List<Field>  OneToOneFields = new ArrayList<>();
        final Map<Class<?>, Integer> oneToManyFields = new HashMap<>();
        Arrays.stream(to.getDeclaredFields()).forEach(field -> {
            Class<?> fType = field.getType();
            Object val = null;
            if (field.isAnnotationPresent(GeneratedValue.class)) return;
            else if (field.isAnnotationPresent(ManyToOne.class)){
                if (from!=null && from.getClass().equals(field.getType())) val = from;
                else {
                    val = saveAndGet(fType, null);
                }
            } else if(field.isAnnotationPresent(OneToOne.class)){
                OneToOne oneToOne = field.getAnnotation(OneToOne.class);
                if (!oneToOne.optional()) {
                    if (field.isAnnotationPresent(Unique.class)){
                       val = saved.get(fType).get(lastInstanceId-1);
                    }
                    else
                        val = saveAndGet(fType, inst);
                }
                else OneToOneFields.add(field);
            } else if(field.isAnnotationPresent(OneToMany.class)){
                fType= (Class<?>) ((ParameterizedType)field.getGenericType()).getActualTypeArguments()[0];
                if (ErrandBoy.getAnnotatedFields(fType, Id.class).stream().anyMatch(field1 -> field1.getType().equals(to)))
                    oneToManyFields.put(fType, numInstances.get(fType.getSimpleName()) / numInstances.get(to.getSimpleName()));
                return;
//                int numInst = getMaxInstances(to, fType);
//                numInst = numInst==numInstances.get(null)?4:numInst;
//                oneToManyFields.put(fType,numInst );
            } else if(field.isAnnotationPresent(ManyToMany.class)){
                throw new RuntimeException("MANY TO MANY IS NOT IMPLEMENTED");
            } else if (fType.isEnum()){
                val  = generateEnum(fType);
            } else if(List.class.isAssignableFrom(fType) || Set.class.isAssignableFrom(fType)){
                if (List.class.isAssignableFrom(fType))val = new ArrayList<>();
                else if (Set.class.isAssignableFrom(fType)) val = new HashSet<>();
                Category a;
                fType= (Class<?>) ((ParameterizedType)field.getGenericType()).getActualTypeArguments()[0];

                int rnd =  new Random().nextInt(0,(int)Math.pow(2,4));
                for (int i = 0; i < rnd + 1; i++) {
                    Object item = null;
                    if (fType.isEnum()) item= generateEnum(fType);
                    else if(String.class.isAssignableFrom(fType)) item = generateUniqueString(20);
                    else if(Number.class.isAssignableFrom(fType)) {
                        try {
                            item  =Random.class.getMethod("next" + ErrandBoy.firstLetterToUpperCase(fType.getSimpleName())).invoke(new Random());
                        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    try {
                        val.getClass().getMethod("add", Object.class).invoke(val,item);
                    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                        throw new RuntimeException(e);
                    }
                }
            } else if (List.class.isAssignableFrom(fType)){
                fType= (Class<?>) ((ParameterizedType)field.getGenericType()).getActualTypeArguments()[0];
            } else if (field.isAnnotationPresent(Lob.class)){
                if(byte[].class.isAssignableFrom(field.getType()) && this.imageSourceURL!=null){
                    byte[] data;
                    try {
                        data = DataFetcher.fetch(this.imageSourceURL);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    val = data;
                } else if (String.class.isAssignableFrom(field.getType())){
                    val = getLorem(null);
                } else if(char[].class.isAssignableFrom(field.getType())){
                    val = getLorem(null).toCharArray();
                }
            } else if(String.class.isAssignableFrom(fType)){
                if (field.getName().equals("email")){
                    val= generateEmail(lastInstanceId);
                } else {
                    int max=25;
                    Size sizeAnnotation = field.getAnnotation(Size.class);
                    if (sizeAnnotation != null ) max = sizeAnnotation.max()<Integer.MAX_VALUE? sizeAnnotation.max() : max;
                    val = generateUniqueString(max);
                }
            } else if(Number.class.isAssignableFrom(fType)){
                Max maxAnnotation = field.getAnnotation(Max.class);
                int maxVal = (int) (maxAnnotation!=null?maxAnnotation.value():Math.pow(2,12));
                try {
                    if (BigDecimal.class.isAssignableFrom(fType)) val = new BigDecimal(new Random().nextDouble(maxVal));
                    else if (BigInteger.class.isAssignableFrom(fType)) val = BigInteger.valueOf(new Random().nextLong(maxVal));
                    else val= fType.getMethod("valueOf", String.class).invoke(null, String.valueOf(new Random().nextInt(maxVal)+1));
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            } else if (Boolean.class.isAssignableFrom(fType)){
                val = new Random().nextBoolean();
            } else if(Date.class.isAssignableFrom(fType)){
                val = generateDate();
            } else if(fType.isAnnotationPresent(Embeddable.class)){
                val = generateEmbeddable(fType);
            }
            try {
                Method setter= ErrandBoy.findSetter(field, to);
                Class<?> paramType = setter.getParameterTypes()[0];
                setter. invoke(inst, val==null && paramType.isPrimitive()? castToDefaultPrimitive(paramType):val);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        });
        final T proxied;
        try {
             proxied= dbContext.save(inst);
            List<Object> arr= saved.get(to);
            if (arr==null) {
                arr = new ArrayList<>();
                saved.put(to, arr);
            }
            arr.add(proxied);
            if (!OneToOneFields.isEmpty() || !oneToManyFields.isEmpty()) {
                OneToOneFields.forEach(field -> {
                    try {
                        ErrandBoy.findSetter(field, proxied.getClass()).invoke(proxied, saveAndGet(field.getType(), proxied));
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                });
                oneToManyFields.forEach((k,v)->{
//                    System.out.println("generating  "+  k.getSimpleName() + " for " + to.getSimpleName() + " " + v + " times");
                    for (int i=0; i< v ; i++){
                        saveAndGet(k, proxied);
                    }
                });
                dbContext.save(proxied);
            }

            retVal= proxied;
        } catch (SQLException e) {
            if (Objects.equals(e.getSQLState(), "23505") || Objects.equals(e.getSQLState(), "22002")) {
                System.out.println(e.getMessage());
            }
            else throw new RuntimeException(e);
        } finally {
            recursionStack.pop();
        }
        return retVal!=null?retVal:saveAndGet(to,from);
    }
    private static <T> T generateEnum(Class<T> clazz){
        Object[] values = clazz.getEnumConstants();
        return (T) values[new Random().nextInt(values.length)];
    }
    private <T> T generateEmbeddable(Class<T> clazz){
        T inst;
        try {
            inst = clazz.getConstructor().newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        Arrays.stream(clazz.getDeclaredFields()).forEach(field->{
            Object val = null;
            if (String.class.isAssignableFrom(field.getType())){
                val = generateUniqueString(field.getName(), 0);
            }else if (Number.class.isAssignableFrom(field.getType())){
                Random rand = new Random();
                try {
                    val = Random.class.getMethod("next" + ErrandBoy.firstLetterToUpperCase(field.getType().getSimpleName())).invoke(rand);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e);
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            }
            try {
                ErrandBoy.findSetter(field, clazz).invoke(inst, val);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        });
        return inst;
    }
    private static Date generateDate(){
        return Date.from(LocalDate.now(ZoneId.of("UTC+3")).minusDays(new Random().nextInt(30)).minusMonths(new Random().nextInt(12)).minusWeeks(new Random().nextInt(7)).minusDays(new Random().nextInt(4)).minusYears(new Random().nextInt(3)).atStartOfDay().toInstant(ZoneOffset.ofHours(3)));
    }
    private static <T> T castToDefaultPrimitive(Class<T> c) {
        try {
            String upperCased = ErrandBoy.firstLetterToUpperCase(c.getSimpleName());
           Class<?> clazz = Class.forName("java.lang." +  (c==int.class?upperCased+ "eger":upperCased));
           Object o;
           if (Number.class.isAssignableFrom(clazz)) {
                o= clazz.getMethod("valueOf", String.class).invoke(null, "0");
           }else
                o= clazz.getMethod("valueOf", String.class).invoke(null, (String) null);
            return (T) clazz.getMethod(c.getSimpleName() + "Value").invoke(o);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

    }
    public void setImageSource(String url) throws MalformedURLException {
        this.imageSourceURL=url;
    }

    public void setTextSourceURL(String textSourceURL) {
        this.textSourceURL = textSourceURL;
    }

    private String textSourceURL = null;
    private String imageSourceURL=null;
    public static class DataFetcher {
        private static int fetchCount = 0;
        private static final byte[][] fetched = new byte[50][];
        static{
            File imagesDir = new File("./images");
            if (imagesDir.exists()){
                for (File file : imagesDir.listFiles()){
                    try(FileInputStream fis= new FileInputStream(file)){
                        byte[] data = new byte[(int)file.length()];
                        fis.read(data);
                        fetched[fetchCount]=data;
                        fetchCount++;
                    } catch (IOException e){throw new RuntimeException(e);}
                }
            } else imagesDir.mkdir();
        }
        public static byte[] fetch(String url) throws MalformedURLException {
            if (fetchCount >= fetched.length)
                return fetched[new Random().nextInt(fetched.length)];
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestMethod("GET");
                if (connection.getResponseCode() > 300) {
                    throw new IOException("Server returned HTTP response code " + connection.getResponseCode());
                }
                int contentLength = connection.getContentLength();
                byte[] data = new byte[contentLength];
                connection.getInputStream().read(data);
                connection.disconnect();
                fetched[fetchCount]  = data;
                FileOutputStream fos = new FileOutputStream("./images/image" + fetchCount++ + ".jpg");
                fos.write(data);
                fos.close();
                return data;
            }catch(MalformedURLException e){ throw e;} catch (IOException e) { throw new RuntimeException(e);}
        }
    }
    private static String getLorem(Integer maxLength){
        final String lorem = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Cras ut vehicula enim, quis semper nulla. Nulla condimentum hendrerit tempus. Nunc laoreet lacus quis dolor viverra interdum. Praesent iaculis ligula id urna blandit, at vulputate libero rutrum. Morbi gravida vitae eros id malesuada. Phasellus elementum tempus nisi eu ultricies. Maecenas sit amet ligula id lorem interdum aliquam. Pellentesque nibh mi, eleifend a aliquam eget, dictum dignissim neque. Nullam interdum lacus sit amet turpis faucibus dignissim. Etiam lacinia risus non nisi fringilla hendrerit. +" +
                "Vestibulum pharetra mauris quis risus iaculis rhoncus. Nunc facilisis ut purus nec finibus. Quisque finibus laoreet erat congue pretium. Class aptent taciti sociosqu ad litora torquent per conubia nostra, per inceptos himenaeos. Sed eget faucibus est. Praesent vel dictum purus, ac vestibulum nulla. Ut vel magna porttitor, luctus eros quis, pretium quam. Suspendisse semper felis arcu, ac gravida odio malesuada nec. Pellentesque tincidunt risus ac erat posuere, quis dictum dolor pretium. Nunc suscipit mollis elit id eleifend. Sed pretium lacus non ornare pellentesque. Cras pellentesque lacinia quam non euismod. Integer vitae velit a tortor gravida consectetur. +" +
                "Duis est est, facilisis in facilisis ut, commodo non nisl. Ut at pulvinar ante. Etiam id fermentum ex. Integer sodales lacus est, eu tincidunt ipsum venenatis sed. Proin sed justo ac orci convallis porta. Suspendisse vitae tincidunt justo. Sed porttitor neque tortor, in condimentum ipsum lacinia id. +" +
                "Maecenas sed purus in lacus blandit pulvinar id facilisis est. In egestas odio enim. Aenean interdum quam id orci convallis, a maximus erat semper. Aliquam imperdiet feugiat tellus eu sagittis. In vehicula bibendum leo eget consequat. In elementum metus fringilla justo iaculis rhoncus. Fusce quis nulla ac augue varius interdum. Maecenas porttitor condimentum neque vel finibus. Mauris cursus sem quis eros porta, nec elementum ipsum imperdiet. Sed tempor in nunc eu semper. Etiam bibendum, ante ac rutrum sodales, lectus nisl volutpat velit, non tempor nisl nisi et arcu. Phasellus posuere risus at nulla ornare finibus. Mauris eget pellentesque est. Quisque ultrices nibh nisi, eu fermentum ante tristique sit amet. +" +
                "Sed efficitur lacinia auctor. Aliquam ut bibendum sem. Quisque eu euismod sapien. Sed accumsan purus sit amet viverra ornare. Aliquam erat volutpat. Donec vulputate enim quis hendrerit consectetur. Mauris metus massa, feugiat id dolor sit amet, viverra ultrices elit. Mauris ultrices urna eu massa ultrices, quis ultrices neque volutpat. Quisque porttitor elementum leo nec tincidunt. Curabitur leo risus, ultricies vitae lacinia pretium, accumsan dictum libero. Nam a sagittis quam.";
        if (maxLength==null) return lorem.substring(0, new Random().nextInt(lorem.length()));
        else return lorem.substring(0, new Random().nextInt(maxLength));
    }
    private String generateEmail(Integer id){
        String userName = generateUniqueString(20);
        String domain = new String[]{"hotmail", "gmail"}[new Random().nextInt(0,2)];
        return userName + "@" + domain + ".com";
    }
    Map<Class<?>, AtomicInteger> lastInstanceIds = new HashMap<>();
    private String generateUniqueString(@NotNull String base, Integer id){
        return base+id;
    }
    private String generateUniqueString(int length){
        final char[] letters = "ABCDEFGHJKLMOÖPRSŞTUÜVWYZ0123456789".toCharArray();
        StringBuilder sb= new StringBuilder();
        for (int i=0; i<length; i++){
            sb.append(letters[new Random().nextInt(letters.length)]);
        }
        return sb.toString();
    }
    private <T> T getFromCache(Class<T> to){
        List<Object> saveds = saved.get(to);
        if (saveds == null){
            saved.put(to, saveds=new ArrayList<>());
        }
        if (saveds.isEmpty()) return null;
        else return (T) saveds.get((int) (Math.random() * saveds.size()));
    }
    private int getMaxInstances(Class<?> first, Class<?> second){
    boolean b=false;
    Integer ret;
        do{
            b=!b;
            ret = numInstances.get((first!=null?first.getSimpleName()+'.':"") + (second!=null?second.getSimpleName():""));
            if (ret==null) {
                Class<?> tmp=first;
                first=second;
                second=tmp;
            }
        }while (b);
        if (ret==null) ret = numInstances.get(first!=null?first.getSimpleName():second.getSimpleName());
        if (ret==null) ret = numInstances.get(null);
        return ret;
    }
}

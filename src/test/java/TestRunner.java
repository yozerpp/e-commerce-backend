import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonAnyFormatVisitor;
import com.may.simpleecommercesite.beans.DBService;
import org.apache.commons.collections.MapUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.naming.NamingException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestRunner {
    @Test
    public void createStatemntStringTest(){
        Assertions.assertEquals("UPDATE RegisteredCustomer SET firstName=Ahmet, lastName=Özer WHERE email=\"a@b.com\"", DBService.createStatementString("RegisteredCustomer", Map.of("email", "a@b.com", "firstName", "Ahmet", "lastName", "Ozer"), DBService.StatementType.UPDATE));
    }
}


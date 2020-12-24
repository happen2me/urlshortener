package co.yuanchun.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;

import org.junit.Test;

public class AliasGenerationServiceTest {
    private static final String memoryDbLocation = "jdbc:sqlite::memory:";

    @Test
    public void ShouldGenerateCorrectAlias() {
        // setup database
        DatabaseAdaper databdase = null;
        try {
            databdase = new DatabaseAdaper(memoryDbLocation);
        } catch (SQLException e) {
            e.printStackTrace();
            assertTrue("Database connection failed", false);
        }
        AliasGenerationService aliasGenerationService = new AliasGenerationService(databdase);
        String alias1 = aliasGenerationService.insertUrl("https://db.in.tum.de");
        String alias2 = aliasGenerationService.insertUrl("https://db.in.tum.de");
        assertEquals("6fN7m2", alias1);
        assertEquals("7rFVNp", alias2);
    }
}

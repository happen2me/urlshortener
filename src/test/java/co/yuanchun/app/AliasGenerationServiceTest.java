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
            databdase.initializeDb();
        } catch (SQLException e) {
            e.printStackTrace();
            assertTrue("Database connection failed", false);
        }
        AliasGenerationService aliasGenerationService = new AliasGenerationService(databdase);
        AliasRecord record1 = aliasGenerationService.generateAlias("https://db.in.tum.de");
        databdase.insertUrl(record1);
        AliasRecord record2 = aliasGenerationService.generateAlias("https://db.in.tum.de");
        assertEquals("6fN7m2", record1.getAlias());
        assertEquals("7rFVNp", record2.getAlias());
    }
}

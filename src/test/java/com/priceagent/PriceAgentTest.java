package com.priceagent;

import com.priceagent.db.DatabaseInitializer;
import com.priceagent.db.DatabaseManager;
import com.priceagent.tools.ExecuteSqlTool;
import com.priceagent.tools.GetDimensionValuesTool;
import com.priceagent.tools.SearchProductsTool;
import org.junit.jupiter.api.*;

import java.io.File;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PriceAgentTest {

    private static DatabaseManager dbManager;
    private static final String TEST_DB = "test-price-agent.db";

    @BeforeAll
    static void setup() throws SQLException {
        new File(TEST_DB).delete();

        dbManager = new DatabaseManager("jdbc:sqlite:" + TEST_DB);
        DatabaseInitializer initializer = new DatabaseInitializer(dbManager);
        initializer.initialize();
    }

    @AfterAll
    static void teardown() {
        new File(TEST_DB).delete();
    }

    @Test
    @Order(1)
    void testDatabaseSeeded() {
        ExecuteSqlTool sqlTool = new ExecuteSqlTool(dbManager);
        String result = sqlTool.executeSql("SELECT COUNT(*) as total FROM mkt_priceflat");
        assertNotNull(result);
        assertFalse(result.contains("ERROR"));
        assertTrue(result.contains("total"));
        System.out.println("Record count query result:\n" + result);
    }

    @Test
    @Order(2)
    void testExecuteSqlBlocking() {
        ExecuteSqlTool sqlTool = new ExecuteSqlTool(dbManager);

        String result = sqlTool.executeSql("DELETE FROM mkt_priceflat");
        assertTrue(result.contains("ERROR"), "DELETE should be blocked: " + result);

        result = sqlTool.executeSql("DROP TABLE mkt_priceflat");
        assertTrue(result.contains("ERROR"), "DROP should be blocked: " + result);

        result = sqlTool.executeSql("INSERT INTO mkt_priceflat VALUES (1,2,3)");
        assertTrue(result.contains("ERROR"), "INSERT should be blocked: " + result);

        result = sqlTool.executeSql("SELECT createddate FROM mkt_priceflat LIMIT 1");
        assertFalse(result.contains("ERROR"), "SELECT with createddate should work: " + result);
    }

    @Test
    @Order(3)
    void testExecuteSqlSelectWorks() {
        ExecuteSqlTool sqlTool = new ExecuteSqlTool(dbManager);

        String result = sqlTool.executeSql(
            "SELECT DISTINCT emiratenameen FROM mkt_priceflat ORDER BY emiratenameen"
        );
        assertFalse(result.contains("ERROR"));
        assertTrue(result.contains("Abu Dhabi"));
        assertTrue(result.contains("Dubai"));
        assertTrue(result.contains("Sharjah"));
    }

    @Test
    @Order(4)
    void testGetDimensionValues() {
        GetDimensionValuesTool tool = new GetDimensionValuesTool(dbManager);

        String result = tool.getDimensionValues("emirate");
        assertTrue(result.contains("Abu Dhabi"));
        assertTrue(result.contains("7 found"));

        result = tool.getDimensionValues("store");
        assertTrue(result.contains("Carrefour"));
        assertTrue(result.contains("Lulu Hypermarket"));

        result = tool.getDimensionValues("invalid");
        assertTrue(result.contains("ERROR"));
    }

    @Test
    @Order(5)
    void testSearchProducts() {
        SearchProductsTool tool = new SearchProductsTool(dbManager);

        String result = tool.searchProducts("milk");
        assertFalse(result.contains("ERROR"));
        assertTrue(result.contains("Milk"));
        assertTrue(result.contains("productid") || result.contains("ID:"));

        result = tool.searchProducts("xyz_nonexistent_product");
        assertTrue(result.contains("No products found"));
    }

    @Test
    @Order(6)
    void testPriceComparison() {
        ExecuteSqlTool sqlTool = new ExecuteSqlTool(dbManager);

        String result = sqlTool.executeSql(
            "SELECT storenameen, MIN(standardprice) as min_price, MAX(standardprice) as max_price "
            + "FROM mkt_priceflat "
            + "WHERE productid = 1 "
            + "GROUP BY storenameen "
            + "ORDER BY min_price ASC"
        );
        assertFalse(result.contains("ERROR"));
        assertTrue(result.contains("Carrefour") || result.contains("Lulu"));
        System.out.println("Price comparison result:\n" + result);
    }

    @Test
    @Order(7)
    void testDateFiltering() {
        ExecuteSqlTool sqlTool = new ExecuteSqlTool(dbManager);

        String result = sqlTool.executeSql(
            "SELECT DISTINCT createddate FROM mkt_priceflat "
            + "WHERE createddate IS NOT NULL "
            + "ORDER BY createddate"
        );
        assertFalse(result.contains("ERROR"));
        assertTrue(result.contains("2025-01"));
        System.out.println("Dates in DB:\n" + result);
    }
}

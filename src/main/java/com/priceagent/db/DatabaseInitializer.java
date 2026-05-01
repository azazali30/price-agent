package com.priceagent.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Random;

public class DatabaseInitializer {

    private final DatabaseManager dbManager;
    private final Random random = new Random(42);

    private static final String[] EMIRATES = {
        "Abu Dhabi", "Dubai", "Sharjah", "Ajman", "Ras Al Khaimah", "Fujairah", "Umm Al Quwain"
    };

    private static final String[] STORES = {
        "Carrefour", "Lulu Hypermarket", "Spinneys", "Choithrams", "Union Coop"
    };

    private static final String[][] CATEGORIES_AND_SUBS = {
        {"Dairy", "Fresh Milk", "Yogurt", "Cheese", "Butter"},
        {"Bakery", "Bread", "Cakes", "Pastries", "Biscuits"},
        {"Beverages", "Water", "Juice", "Soft Drinks", "Tea & Coffee"},
        {"Meat & Poultry", "Chicken", "Beef", "Lamb", "Processed Meat"},
        {"Fruits & Vegetables", "Fresh Fruits", "Fresh Vegetables", "Frozen Vegetables", "Salads"},
        {"Rice & Grains", "Basmati Rice", "Pasta", "Flour", "Cereals"},
        {"Cooking Oil", "Sunflower Oil", "Olive Oil", "Coconut Oil", "Vegetable Oil"},
        {"Snacks", "Chips", "Nuts", "Chocolate", "Crackers"},
        {"Baby Products", "Baby Food", "Diapers", "Baby Milk", "Baby Care"},
        {"Cleaning", "Detergent", "Dishwash", "Surface Cleaner", "Tissue"}
    };

    private static final String[][] PRODUCTS = {
        // Dairy - Fresh Milk
        {"Al Ain Fresh Milk Full Fat 1L", "Al Ain", "MILK-001", "Liter", "Liter", "1", "1"},
        {"Al Rawabi Fresh Milk 500ml", "Al Rawabi", "MILK-002", "ml", "Liter", "500", "0.5"},
        {"Almarai Full Fat Milk 2L", "Almarai", "MILK-003", "Liter", "Liter", "2", "2"},
        {"Lacnor Fresh Milk Low Fat 1L", "Lacnor", "MILK-004", "Liter", "Liter", "1", "1"},
        // Dairy - Yogurt
        {"Al Ain Natural Yogurt 500g", "Al Ain", "YOG-001", "g", "Kg", "500", "0.5"},
        {"Activia Stirred Yogurt 120g", "Activia", "YOG-002", "g", "Kg", "120", "0.12"},
        {"Almarai Greek Yogurt 200g", "Almarai", "YOG-003", "g", "Kg", "200", "0.2"},
        // Dairy - Cheese
        {"Puck Cream Cheese 500g", "Puck", "CHS-001", "g", "Kg", "500", "0.5"},
        {"President Cheddar Slices 200g", "President", "CHS-002", "g", "Kg", "200", "0.2"},
        {"Kiri Cheese Squares 12pcs", "Kiri", "CHS-003", "Piece", "Piece", "12", "12"},
        // Bakery - Bread
        {"Lusine White Bread 600g", "Lusine", "BRD-001", "g", "Kg", "600", "0.6"},
        {"Wooden Bakery Arabic Bread 6pcs", "Wooden Bakery", "BRD-002", "Piece", "Piece", "6", "6"},
        {"Al Jadeed Samoon Bread 4pcs", "Al Jadeed", "BRD-003", "Piece", "Piece", "4", "4"},
        // Beverages - Water
        {"Al Ain Water 1.5L", "Al Ain", "WTR-001", "Liter", "Liter", "1.5", "1.5"},
        {"Masafi Water 500ml", "Masafi", "WTR-002", "ml", "Liter", "500", "0.5"},
        {"Evian Natural Water 1L", "Evian", "WTR-003", "Liter", "Liter", "1", "1"},
        // Beverages - Juice
        {"Al Rawabi Orange Juice 1L", "Al Rawabi", "JUC-001", "Liter", "Liter", "1", "1"},
        {"Lacnor Mango Juice 1L", "Lacnor", "JUC-002", "Liter", "Liter", "1", "1"},
        {"Rani Apple Juice 240ml", "Rani", "JUC-003", "ml", "Liter", "240", "0.24"},
        // Meat - Chicken
        {"Al Ain Fresh Chicken 1kg", "Al Ain", "CHK-001", "Kg", "Kg", "1", "1"},
        {"Americana Chicken Nuggets 400g", "Americana", "CHK-002", "g", "Kg", "400", "0.4"},
        // Meat - Beef
        {"Australian Beef Mince 500g", "Australian Meat", "BEF-001", "g", "Kg", "500", "0.5"},
        // Fruits - Fresh Fruits
        {"Banana Philippines 1kg", "Generic", "FRT-001", "Kg", "Kg", "1", "1"},
        {"Apple Royal Gala 1kg", "Generic", "FRT-002", "Kg", "Kg", "1", "1"},
        {"Mango Alphonso 1kg", "Generic", "FRT-003", "Kg", "Kg", "1", "1"},
        // Fruits - Fresh Vegetables
        {"Tomato Local 1kg", "Local Farm", "VEG-001", "Kg", "Kg", "1", "1"},
        {"Cucumber Local 1kg", "Local Farm", "VEG-002", "Kg", "Kg", "1", "1"},
        {"Potato 1kg", "Generic", "VEG-003", "Kg", "Kg", "1", "1"},
        // Rice
        {"India Gate Basmati Rice 5kg", "India Gate", "RIC-001", "Kg", "Kg", "5", "5"},
        {"Abu Kass Basmati Rice 2kg", "Abu Kass", "RIC-002", "Kg", "Kg", "2", "2"},
        // Pasta
        {"Barilla Spaghetti 500g", "Barilla", "PAS-001", "g", "Kg", "500", "0.5"},
        // Cooking Oil
        {"Afia Sunflower Oil 1.5L", "Afia", "OIL-001", "Liter", "Liter", "1.5", "1.5"},
        {"Borges Olive Oil 500ml", "Borges", "OIL-002", "ml", "Liter", "500", "0.5"},
        {"KTC Coconut Oil 500ml", "KTC", "OIL-003", "ml", "Liter", "500", "0.5"},
        // Snacks
        {"Lay's Classic Chips 170g", "Lay's", "SNK-001", "g", "Kg", "170", "0.17"},
        {"Oman Chips 100g", "Oman", "SNK-002", "g", "Kg", "100", "0.1"},
        {"Galaxy Chocolate Bar 90g", "Galaxy", "SNK-003", "g", "Kg", "90", "0.09"},
        // Baby
        {"S-26 Gold Baby Milk Stage 1 900g", "S-26", "BBY-001", "g", "Kg", "900", "0.9"},
        {"Pampers Diapers Size 4 44pcs", "Pampers", "BBY-002", "Piece", "Piece", "44", "44"},
        // Cleaning
        {"Tide Detergent Powder 3kg", "Tide", "CLN-001", "Kg", "Kg", "3", "3"},
        {"Fairy Dishwash Liquid 1L", "Fairy", "CLN-002", "Liter", "Liter", "1", "1"},
        {"Fine Tissue 200 Sheets 10pcs", "Fine", "CLN-003", "Piece", "Piece", "10", "10"},
    };

    // Base prices for each product (index matches PRODUCTS array)
    private static final double[] BASE_PRICES = {
        6.50, 3.75, 12.00, 6.25,
        5.50, 3.00, 7.50,
        15.00, 9.50, 8.00,
        5.00, 3.50, 2.75,
        1.50, 1.00, 4.50,
        8.50, 8.00, 2.50,
        22.00, 14.50,
        32.00,
        5.50, 9.00, 15.00,
        3.50, 3.00, 4.50,
        45.00, 28.00,
        7.50,
        18.00, 35.00, 22.00,
        8.50, 5.00, 6.50,
        85.00, 65.00,
        35.00, 12.00, 18.00
    };

    // Category/Subcategory mapping for each product
    private static final int[][] PRODUCT_CATEGORY_MAP = {
        {0, 0}, {0, 0}, {0, 0}, {0, 0},  // Dairy - Fresh Milk
        {0, 1}, {0, 1}, {0, 1},            // Dairy - Yogurt
        {0, 2}, {0, 2}, {0, 2},            // Dairy - Cheese
        {1, 0}, {1, 0}, {1, 0},            // Bakery - Bread
        {2, 0}, {2, 0}, {2, 0},            // Beverages - Water
        {2, 1}, {2, 1}, {2, 1},            // Beverages - Juice
        {3, 0}, {3, 0},                     // Meat - Chicken
        {3, 1},                             // Meat - Beef
        {4, 0}, {4, 0}, {4, 0},            // Fruits
        {4, 1}, {4, 1}, {4, 1},            // Vegetables
        {5, 0}, {5, 0},                     // Rice
        {5, 1},                             // Pasta
        {6, 0}, {6, 1}, {6, 2},            // Cooking Oil
        {7, 0}, {7, 0}, {7, 2},            // Snacks
        {8, 2}, {8, 1},                     // Baby
        {9, 0}, {9, 1}, {9, 3},            // Cleaning
    };

    public DatabaseInitializer(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void initialize() throws SQLException {
        createSchema();
        seedData();
    }

    private void createSchema() throws SQLException {
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS mkt_priceflat (
                    flatid INTEGER PRIMARY KEY AUTOINCREMENT,
                    priceid INTEGER,
                    submissionkey TEXT,
                    submissionid INTEGER,
                    createddate TEXT,
                    productid INTEGER,
                    branchid INTEGER,
                    entityid INTEGER,
                    emirateid INTEGER,
                    categoryid INTEGER,
                    subcategoryid INTEGER,
                    productnameen TEXT,
                    productnamear TEXT,
                    productcode TEXT,
                    brand TEXT,
                    unit TEXT,
                    standardunit TEXT,
                    quantity REAL,
                    conversionfactor REAL,
                    standardquantity REAL,
                    actualprice REAL,
                    standardprice REAL,
                    branchnameen TEXT,
                    storenameen TEXT,
                    emiratenameen TEXT,
                    categorynameen TEXT,
                    subcategorynameen TEXT,
                    branchnamear TEXT,
                    storenamear TEXT,
                    emiratenamear TEXT,
                    categorynamear TEXT,
                    subcategorynamear TEXT
                )
            """);

            // Create indexes
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_pf_productid ON mkt_priceflat(productid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_pf_branchid ON mkt_priceflat(branchid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_pf_entityid ON mkt_priceflat(entityid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_pf_emirateid ON mkt_priceflat(emirateid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_pf_categoryid ON mkt_priceflat(categoryid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_pf_subcategoryid ON mkt_priceflat(subcategoryid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_pf_createddate ON mkt_priceflat(createddate)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_pf_date_category ON mkt_priceflat(createddate, categoryid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_pf_category_standardprice ON mkt_priceflat(categoryid, standardprice)");
        }
    }

    private void seedData() throws SQLException {
        try (Connection conn = dbManager.getConnection()) {

            // Check if data already exists
            try (Statement stmt = conn.createStatement()) {
                var rs = stmt.executeQuery("SELECT COUNT(*) FROM mkt_priceflat");
                rs.next();
                if (rs.getInt(1) > 0) {
                    System.out.println("Database already seeded with " + rs.getInt(1) + " records.");
                    return;
                }
            }

            conn.setAutoCommit(false);

            String insertSql = "INSERT INTO mkt_priceflat (flatid, priceid, submissionkey, submissionid, createddate, " +
                "productid, branchid, entityid, emirateid, categoryid, subcategoryid, " +
                "productnameen, productnamear, productcode, brand, unit, standardunit, " +
                "quantity, conversionfactor, standardquantity, actualprice, standardprice, " +
                "branchnameen, storenameen, emiratenameen, categorynameen, subcategorynameen) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

            try (var pstmt = conn.prepareStatement(insertSql)) {
                int flatId = 1;
                int priceid = 1000;
                int submissionId = 1;
                LocalDate startDate = LocalDate.of(2025, 1, 1);

                for (int productIdx = 0; productIdx < PRODUCTS.length; productIdx++) {
                    String[] product = PRODUCTS[productIdx];
                    int[] catMap = PRODUCT_CATEGORY_MAP[productIdx];
                    String categoryName = CATEGORIES_AND_SUBS[catMap[0]][0];
                    String subcategoryName = CATEGORIES_AND_SUBS[catMap[0]][catMap[1] + 1];
                    double basePrice = BASE_PRICES[productIdx];

                    for (int storeIdx = 0; storeIdx < STORES.length; storeIdx++) {
                        String store = STORES[storeIdx];
                        int entityId = storeIdx + 1;

                        for (int emirateIdx = 0; emirateIdx < EMIRATES.length; emirateIdx++) {
                            String emirate = EMIRATES[emirateIdx];
                            int branchId = (storeIdx * 7) + emirateIdx + 1;
                            String branchName = store + " - " + emirate + " Branch";

                            for (int dateOffset = 0; dateOffset < 3; dateOffset++) {
                                LocalDate date = startDate.plusDays(dateOffset * 15);

                                double storeVariance = 1.0 + (random.nextDouble() * 0.2 - 0.1);
                                double dateInflation = 1.0 + (dateOffset * 0.02);
                                double actualPrice = Math.round(basePrice * storeVariance * dateInflation * 100.0) / 100.0;

                                double quantity = Double.parseDouble(product[5]);
                                double conversionFactor = Double.parseDouble(product[6]) / quantity;
                                double standardQuantity = Double.parseDouble(product[6]);
                                double standardPrice = Math.round((actualPrice / standardQuantity) * 100.0) / 100.0;

                                pstmt.setInt(1, flatId++);
                                pstmt.setInt(2, priceid++);
                                pstmt.setString(3, String.format("SUB-%05d", submissionId));
                                pstmt.setInt(4, submissionId);
                                pstmt.setString(5, date.toString());
                                pstmt.setInt(6, productIdx + 1);
                                pstmt.setInt(7, branchId);
                                pstmt.setInt(8, entityId);
                                pstmt.setInt(9, emirateIdx + 1);
                                pstmt.setInt(10, catMap[0] + 1);
                                pstmt.setInt(11, (catMap[0] * 4) + catMap[1] + 1);
                                pstmt.setString(12, product[0]);
                                pstmt.setString(13, "");
                                pstmt.setString(14, product[2]);
                                pstmt.setString(15, product[1]);
                                pstmt.setString(16, product[3]);
                                pstmt.setString(17, product[4]);
                                pstmt.setDouble(18, quantity);
                                pstmt.setDouble(19, conversionFactor);
                                pstmt.setDouble(20, standardQuantity);
                                pstmt.setDouble(21, actualPrice);
                                pstmt.setDouble(22, standardPrice);
                                pstmt.setString(23, branchName);
                                pstmt.setString(24, store);
                                pstmt.setString(25, emirate);
                                pstmt.setString(26, categoryName);
                                pstmt.setString(27, subcategoryName);
                                pstmt.addBatch();
                                submissionId++;
                            }
                        }
                    }
                }

                pstmt.executeBatch();
                conn.commit();
                System.out.println("Database seeded with " + (flatId - 1) + " records.");
            }
        }
    }
}

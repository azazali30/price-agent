package com.priceagent.tools;

import com.priceagent.db.DatabaseManager;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SearchProductsTool {

    private final DatabaseManager dbManager;

    public SearchProductsTool(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Tool("Search for products by name (case-insensitive). Returns matching products with their " +
          "productid, name, brand, productcode, category, and subcategory. Use this to find products " +
          "before running price comparison queries.")
    public String searchProducts(@P("The search term to look for in product names") String term) {
        if (term == null || term.isBlank()) {
            return "ERROR: Search term cannot be empty.";
        }

        String query = """
            SELECT DISTINCT productid, productnameen, brand, productcode, categorynameen, subcategorynameen
            FROM mkt_priceflat
            WHERE LOWER(productnameen) LIKE LOWER(?)
            ORDER BY categorynameen, productnameen
            LIMIT 30
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, "%" + term.toLowerCase() + "%");
            ResultSet rs = pstmt.executeQuery();

            List<String> results = new ArrayList<>();
            while (rs.next()) {
                results.add(String.format(
                    "ID: %d | %s | Brand: %s | Code: %s | Category: %s > %s",
                    rs.getInt("productid"),
                    rs.getString("productnameen"),
                    rs.getString("brand"),
                    rs.getString("productcode"),
                    rs.getString("categorynameen"),
                    rs.getString("subcategorynameen")
                ));
            }

            if (results.isEmpty()) {
                return "No products found matching '" + term + "'. Try a broader search term.";
            }

            return "Found " + results.size() + " products matching '" + term + "':\n" +
                   String.join("\n", results);

        } catch (SQLException e) {
            return "SQL ERROR: " + e.getMessage();
        }
    }
}

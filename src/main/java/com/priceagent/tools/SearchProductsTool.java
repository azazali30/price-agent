package com.priceagent.tools;

import com.priceagent.db.DatabaseManager;
import com.agentic4j.core.annotation.AgentTool;
import com.agentic4j.core.annotation.Param;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SearchProductsTool {

    private final DatabaseManager dbManager;

    public SearchProductsTool(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @AgentTool("Search for products by name (case-insensitive). Returns matching products with their " +
          "productid, name, brand, productcode, category, and subcategory. Use this to find products " +
          "before running price comparison queries.")
    public String searchProducts(@Param("The search term to look for in product names") String term) {
        if (term == null || term.trim().isEmpty()) {
            return "ERROR: Search term cannot be empty.";
        }

        String query = "SELECT DISTINCT productid, productnameen, brand, productcode, categorynameen, subcategorynameen "
                + "FROM mkt_priceflat "
                + "WHERE LOWER(productnameen) LIKE LOWER(?) "
                + "ORDER BY categorynameen, productnameen "
                + "LIMIT 30";

        try {
            Connection conn = dbManager.getConnection();
            try {
                PreparedStatement pstmt = conn.prepareStatement(query);
                try {
                    pstmt.setString(1, "%" + term.toLowerCase() + "%");
                    ResultSet rs = pstmt.executeQuery();

                    List<String> results = new ArrayList<String>();
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

                    StringBuilder sb = new StringBuilder();
                    sb.append("Found ").append(results.size()).append(" products matching '").append(term).append("':\n");
                    for (int i = 0; i < results.size(); i++) {
                        if (i > 0) sb.append("\n");
                        sb.append(results.get(i));
                    }
                    return sb.toString();
                } finally {
                    pstmt.close();
                }
            } finally {
                conn.close();
            }
        } catch (SQLException e) {
            return "SQL ERROR: " + e.getMessage();
        }
    }
}

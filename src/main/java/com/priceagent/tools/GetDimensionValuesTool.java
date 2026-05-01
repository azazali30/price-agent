package com.priceagent.tools;

import com.priceagent.db.DatabaseManager;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GetDimensionValuesTool {

    private final DatabaseManager dbManager;

    public GetDimensionValuesTool(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Tool("Get all distinct values for a given dimension/column in the price database. " +
          "Valid dimensions: emirate, store, category, subcategory, brand, unit, productcode. " +
          "Use this to discover what values exist before filtering.")
    public String getDimensionValues(@P("The dimension to query: emirate, store, category, subcategory, brand, unit, or productcode") String dimension) {
        String column = switch (dimension.toLowerCase().trim()) {
            case "emirate" -> "emiratenameen";
            case "store" -> "storenameen";
            case "category" -> "categorynameen";
            case "subcategory" -> "subcategorynameen";
            case "brand" -> "brand";
            case "unit" -> "unit";
            case "productcode" -> "productcode";
            default -> null;
        };

        if (column == null) {
            return "ERROR: Unknown dimension '" + dimension + "'. Valid values: emirate, store, category, subcategory, brand, unit, productcode";
        }

        String query = "SELECT DISTINCT " + column + " FROM mkt_priceflat WHERE " + column + " IS NOT NULL ORDER BY " + column;

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            List<String> values = new ArrayList<>();
            while (rs.next()) {
                values.add(rs.getString(1));
            }

            return "Distinct values for '" + dimension + "' (" + values.size() + " found):\n" +
                   String.join(", ", values);

        } catch (SQLException e) {
            return "SQL ERROR: " + e.getMessage();
        }
    }
}

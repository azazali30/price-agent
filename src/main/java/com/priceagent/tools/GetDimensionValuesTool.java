package com.priceagent.tools;

import com.priceagent.db.DatabaseManager;
import com.agentic4j.core.annotation.AgentTool;
import com.agentic4j.core.annotation.Param;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetDimensionValuesTool {

    private static final Map<String, String> DIMENSION_MAP = new HashMap<String, String>();
    static {
        DIMENSION_MAP.put("emirate", "emiratenameen");
        DIMENSION_MAP.put("store", "storenameen");
        DIMENSION_MAP.put("category", "categorynameen");
        DIMENSION_MAP.put("subcategory", "subcategorynameen");
        DIMENSION_MAP.put("brand", "brand");
        DIMENSION_MAP.put("unit", "unit");
        DIMENSION_MAP.put("productcode", "productcode");
    }

    private final DatabaseManager dbManager;

    public GetDimensionValuesTool(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @AgentTool("Get all distinct values for a given dimension/column in the price database. " +
          "Valid dimensions: emirate, store, category, subcategory, brand, unit, productcode. " +
          "Use this to discover what values exist before filtering.")
    public String getDimensionValues(@Param("The dimension to query: emirate, store, category, subcategory, brand, unit, or productcode") String dimension) {
        String column = DIMENSION_MAP.get(dimension.toLowerCase().trim());

        if (column == null) {
            return "ERROR: Unknown dimension '" + dimension + "'. Valid values: emirate, store, category, subcategory, brand, unit, productcode";
        }

        String query = "SELECT DISTINCT " + column + " FROM mkt_priceflat WHERE " + column + " IS NOT NULL ORDER BY " + column;

        try {
            Connection conn = dbManager.getConnection();
            try {
                Statement stmt = conn.createStatement();
                try {
                    ResultSet rs = stmt.executeQuery(query);

                    List<String> values = new ArrayList<String>();
                    while (rs.next()) {
                        values.add(rs.getString(1));
                    }

                    StringBuilder sb = new StringBuilder();
                    sb.append("Distinct values for '").append(dimension).append("' (").append(values.size()).append(" found):\n");
                    for (int i = 0; i < values.size(); i++) {
                        if (i > 0) sb.append(", ");
                        sb.append(values.get(i));
                    }
                    return sb.toString();
                } finally {
                    stmt.close();
                }
            } finally {
                conn.close();
            }
        } catch (SQLException e) {
            return "SQL ERROR: " + e.getMessage();
        }
    }
}

package com.priceagent.tools;

import com.priceagent.db.DatabaseManager;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ExecuteSqlTool {

    private static final Set<String> BLOCKED_KEYWORDS = Set.of(
        "INSERT", "UPDATE", "DELETE", "DROP", "ALTER", "TRUNCATE", "CREATE", "EXEC", "MERGE"
    );

    private static final int MAX_ROWS = 500;

    private final DatabaseManager dbManager;

    public ExecuteSqlTool(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Tool("Execute a read-only SQL SELECT query against the market price database (mkt_priceflat table). " +
          "Returns columns and rows. Only SELECT statements are allowed. " +
          "Use this to discover data, search products, compare prices, and analyze trends.")
    public String executeSql(@P("The SQL SELECT query to execute") String query) {
        // Safety check - use word boundary regex to avoid false positives (e.g., "createddate" matching "CREATE")
        String upperQuery = query.toUpperCase().trim();

        if (!upperQuery.startsWith("SELECT")) {
            return "ERROR: Only SELECT queries are allowed.";
        }

        for (String keyword : BLOCKED_KEYWORDS) {
            if (java.util.regex.Pattern.compile("\\b" + keyword + "\\b").matcher(upperQuery).find()) {
                return "ERROR: " + keyword + " statements are not allowed. Only SELECT queries are permitted.";
            }
        }

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.setQueryTimeout(30);
            ResultSet rs = stmt.executeQuery(query);
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();

            // Build column headers
            List<String> columns = new ArrayList<>();
            for (int i = 1; i <= colCount; i++) {
                columns.add(meta.getColumnLabel(i));
            }

            // Build rows
            List<List<String>> rows = new ArrayList<>();
            int rowCount = 0;
            while (rs.next() && rowCount < MAX_ROWS) {
                List<String> row = new ArrayList<>();
                for (int i = 1; i <= colCount; i++) {
                    String value = rs.getString(i);
                    row.add(value != null ? value : "NULL");
                }
                rows.add(row);
                rowCount++;
            }

            // Format as readable text table
            return formatResult(columns, rows, rowCount, rs.next());

        } catch (SQLException e) {
            return "SQL ERROR: " + e.getMessage();
        }
    }

    private String formatResult(List<String> columns, List<List<String>> rows, int rowCount, boolean hasMore) {
        if (rows.isEmpty()) {
            return "Query returned 0 rows. Columns: " + String.join(", ", columns);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Columns: ").append(String.join(" | ", columns)).append("\n");
        sb.append("Rows returned: ").append(rowCount);
        if (hasMore) {
            sb.append(" (truncated at ").append(MAX_ROWS).append(")");
        }
        sb.append("\n---\n");

        for (List<String> row : rows) {
            sb.append(String.join(" | ", row)).append("\n");
        }

        return sb.toString();
    }
}

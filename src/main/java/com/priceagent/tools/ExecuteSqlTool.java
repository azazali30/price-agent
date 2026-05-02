package com.priceagent.tools;

import com.priceagent.db.DatabaseManager;
import com.agentic4j.core.annotation.AgentTool;
import com.agentic4j.core.annotation.Param;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class ExecuteSqlTool {

    private static final Set<String> BLOCKED_KEYWORDS = new HashSet<String>(Arrays.asList(
        "INSERT", "UPDATE", "DELETE", "DROP", "ALTER", "TRUNCATE", "CREATE", "EXEC", "MERGE"
    ));

    private static final int MAX_ROWS = 500;

    private final DatabaseManager dbManager;

    public ExecuteSqlTool(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @AgentTool("Execute a read-only SQL SELECT query against the market price database (mkt_priceflat table). " +
          "Returns columns and rows. Only SELECT statements are allowed. " +
          "Use this to discover data, search products, compare prices, and analyze trends.")
    public String executeSql(@Param("The SQL SELECT query to execute") String query) {
        String upperQuery = query.toUpperCase().trim();

        if (!upperQuery.startsWith("SELECT")) {
            return "ERROR: Only SELECT queries are allowed.";
        }

        for (String keyword : BLOCKED_KEYWORDS) {
            if (Pattern.compile("\\b" + keyword + "\\b").matcher(upperQuery).find()) {
                return "ERROR: " + keyword + " statements are not allowed. Only SELECT queries are permitted.";
            }
        }

        try {
            Connection conn = dbManager.getConnection();
            try {
                Statement stmt = conn.createStatement();
                try {
                    stmt.setQueryTimeout(30);
                    ResultSet rs = stmt.executeQuery(query);
                    ResultSetMetaData meta = rs.getMetaData();
                    int colCount = meta.getColumnCount();

                    List<String> columns = new ArrayList<String>();
                    for (int i = 1; i <= colCount; i++) {
                        columns.add(meta.getColumnLabel(i));
                    }

                    List<List<String>> rows = new ArrayList<List<String>>();
                    int rowCount = 0;
                    while (rs.next() && rowCount < MAX_ROWS) {
                        List<String> row = new ArrayList<String>();
                        for (int i = 1; i <= colCount; i++) {
                            String value = rs.getString(i);
                            row.add(value != null ? value : "NULL");
                        }
                        rows.add(row);
                        rowCount++;
                    }

                    return formatResult(columns, rows, rowCount, rs.next());
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

    private String formatResult(List<String> columns, List<List<String>> rows, int rowCount, boolean hasMore) {
        if (rows.isEmpty()) {
            return "Query returned 0 rows. Columns: " + join(", ", columns);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Columns: ").append(join(" | ", columns)).append("\n");
        sb.append("Rows returned: ").append(rowCount);
        if (hasMore) {
            sb.append(" (truncated at ").append(MAX_ROWS).append(")");
        }
        sb.append("\n---\n");

        for (List<String> row : rows) {
            sb.append(join(" | ", row)).append("\n");
        }

        return sb.toString();
    }

    private static String join(String delimiter, List<String> parts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) sb.append(delimiter);
            sb.append(parts.get(i));
        }
        return sb.toString();
    }
}

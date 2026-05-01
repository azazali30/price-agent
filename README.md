# Market Price Agent

AI-powered market price monitoring agent for UAE products. Users can browse products, compare prices across stores and emirates, and analyze price trends through a conversational chat interface.

Built with **LangChain4j** (ReAct agent pattern), **Spring Boot**, **SQLite**, and a **React** frontend with SSE streaming.

---

## Prerequisites

- Java 17+
- Maven 3.8+
- Node.js 18+ and npm

## Running Locally

### 1. Backend

```bash
cd /path/to/price-agent

# Run with default config (uses OpenRouter + SQLite file DB)
mvn spring-boot:run

# Or with custom environment variables
OPENAI_API_KEY=your-key OPENAI_BASE_URL=https://openrouter.ai/api/v1 OPENAI_MODEL=openai/gpt-4o-mini mvn spring-boot:run
```

The backend starts on **port 9090**. On first run, it auto-creates and seeds the SQLite database (`price-agent.db`) with 4,410 sample records (42 products x 5 stores x 7 emirates x 3 dates).

### 2. Frontend

```bash
cd frontend
npm install
npm run dev
```

The frontend starts on **port 3000** with a Vite proxy forwarding `/api` requests to `localhost:9090`.

### 3. Open the app

Navigate to [http://localhost:3000](http://localhost:3000) and start chatting.

### Configuration

All configuration is in `src/main/resources/application.properties`:

| Property | Env Variable | Default | Description |
|----------|-------------|---------|-------------|
| `server.port` | — | `9090` | Backend HTTP port |
| `openai.api.key` | `OPENAI_API_KEY` | (hardcoded dev key) | LLM API key |
| `openai.model` | `OPENAI_MODEL` | `openai/gpt-4o-mini` | Model identifier |
| `openai.base.url` | `OPENAI_BASE_URL` | `https://openrouter.ai/api/v1` | OpenAI-compatible API base URL |
| `db.path` | `DB_PATH` | `price-agent.db` | SQLite database file path |

---

## Architecture

### Agent Pattern: ReAct (Reasoning + Acting)

The agent uses LangChain4j's `AiServices` with the ReAct pattern. On each user message, the LLM reasons about what data it needs, invokes tools to fetch that data, observes the results, and continues reasoning until it can provide a final answer.

```
User Message
    │
    ▼
┌──────────────────┐
│   LLM (GPT-4o)   │◄──── System Prompt (schema + rules)
└──────────────────┘
    │         ▲
    │ Tool    │ Tool
    │ Call    │ Result
    ▼         │
┌──────────────────┐
│   Agent Tools     │──── SQLite Database
└──────────────────┘
    │
    ▼
Streamed Response (SSE) → React Frontend
```

### Session Management

Each conversation gets a unique `sessionId`. The agent maintains per-session chat memory (window of last 20 messages) so users can ask follow-up questions with full context.

### Streaming

The backend uses LangChain4j's `TokenStream` API with Spring's `SseEmitter` to deliver tokens in real-time. The frontend consumes the SSE stream and renders markdown incrementally. A non-streaming fallback (`/api/chat`) is used automatically if SSE fails.

---

## Tools

The agent has three tools it can invoke autonomously during a conversation:

### 1. `executeSql`

**Purpose:** Execute arbitrary read-only SQL SELECT queries against the price database.

**When the agent uses it:** For complex queries — price comparisons, aggregations, trend analysis, multi-condition filters, joins across dimensions.

**Safety features:**
- Only `SELECT` statements allowed (enforced by prefix check)
- Blocked keywords (`INSERT`, `UPDATE`, `DELETE`, `DROP`, `ALTER`, `TRUNCATE`, `CREATE`, `EXEC`, `MERGE`) detected via regex word boundaries to avoid false positives on column names like `createddate`
- 30-second query timeout
- Results truncated at 500 rows

**Example agent usage:**
```sql
SELECT storenameen, AVG(standardprice) as avg_price
FROM mkt_priceflat
WHERE productid = 12 AND createddate = '2024-01-15'
GROUP BY storenameen ORDER BY avg_price ASC
```

### 2. `getDimensionValues`

**Purpose:** Retrieve all distinct values for a given dimension column.

**When the agent uses it:** At the start of exploration — to discover what emirates, stores, categories, brands, etc. exist in the data before building filtered queries.

**Supported dimensions:** `emirate`, `store`, `category`, `subcategory`, `brand`, `unit`, `productcode`

**Example agent usage:**
```
getDimensionValues("emirate")
→ "Distinct values for 'emirate' (7 found): Abu Dhabi, Ajman, Dubai, Fujairah, Ras Al Khaimah, Sharjah, Umm Al Quwain"
```

### 3. `searchProducts`

**Purpose:** Case-insensitive product name search. Returns product IDs, names, brands, codes, and categories.

**When the agent uses it:** When the user mentions a product by name — the agent first searches to resolve the exact product ID(s) before running price queries.

**Returns:** Up to 30 matching products with: ID, name, brand, product code, category, and subcategory.

**Example agent usage:**
```
searchProducts("milk")
→ "Found 5 products matching 'milk':
   ID: 1 | Fresh Milk Full Fat 1L | Brand: Al Ain | Code: MILK001 | Category: Dairy > Milk"
```

---

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/chat` | Synchronous chat (request/response) |
| POST | `/api/chat/stream` | Streaming chat via SSE |
| POST | `/api/chat/reset` | Clear session memory |

**Request body** (all endpoints):
```json
{
  "message": "Find the cheapest milk in Dubai",
  "sessionId": "optional-existing-session-id"
}
```

**SSE event types** (`/api/chat/stream`):
- `session` — session ID (sent first)
- `tool` — tool name being executed
- `token` — streamed text token
- `done` — complete final response text
- `error` — error message

---

## Adapting for PostgreSQL or SQL Server

The default setup uses SQLite with a simplified single-table schema and seed data. For production deployments against real databases, swap the system prompt and database connection:

### PostgreSQL (Development/Staging)

Use the PostgreSQL system prompt: [`prompt-postgres-dev.md`](../ap-worktree/vibe-sdk/prompt-postgres-dev.md)

To adapt:
1. Replace `system-prompt.txt` content with the PostgreSQL prompt
2. Add PostgreSQL JDBC dependency to `pom.xml`:
   ```xml
   <dependency>
       <groupId>org.postgresql</groupId>
       <artifactId>postgresql</artifactId>
       <version>42.7.3</version>
   </dependency>
   ```
3. Update `DatabaseManager` to connect via PostgreSQL JDBC URL
4. Remove `DatabaseInitializer` (real data already exists)

### SQL Server (Production)

Use the SQL Server system prompt: [`prompt-sqlserver-prod.md`](../ap-worktree/vibe-sdk/prompt-sqlserver-prod.md)

To adapt:
1. Replace `system-prompt.txt` content with the SQL Server prompt
2. Add SQL Server JDBC dependency to `pom.xml`:
   ```xml
   <dependency>
       <groupId>com.microsoft.sqlserver</groupId>
       <artifactId>mssql-jdbc</artifactId>
       <version>12.6.1.jre11</version>
   </dependency>
   ```
3. Update `DatabaseManager` to connect via SQL Server JDBC URL
4. Remove `DatabaseInitializer` (real data already exists)

Both prompts include the full production schema (with additional columns, relationships, and database-specific SQL syntax guidance).

---

## Project Structure

```
price-agent/
├── pom.xml                              # Maven config (Spring Boot 3.3, LangChain4j 0.36.2)
├── src/main/java/com/priceagent/
│   ├── PriceAgentApplication.java       # Spring Boot entry point
│   ├── config/AppConfig.java            # Bean configuration (DB, Agent)
│   ├── controller/ChatController.java   # REST + SSE endpoints
│   ├── agent/PriceAgent.java            # ReAct agent with session management
│   ├── db/DatabaseManager.java          # JDBC connection provider
│   ├── db/DatabaseInitializer.java      # Schema + seed data (dev only)
│   └── tools/
│       ├── ExecuteSqlTool.java          # SQL query executor
│       ├── GetDimensionValuesTool.java  # Dimension value lookup
│       └── SearchProductsTool.java      # Product name search
├── src/main/resources/
│   ├── application.properties           # Configuration
│   └── system-prompt.txt                # Agent system prompt (SQLite)
├── src/test/java/com/priceagent/
│   └── PriceAgentTest.java             # Unit tests (7 passing)
└── frontend/
    ├── package.json                     # React 19, react-markdown, remark-gfm
    ├── vite.config.js                   # Dev server (port 3000, proxy to 9090)
    ├── src/App.jsx                      # Chat UI with SSE streaming
    ├── src/App.css                      # Styles
    └── index.html                       # Entry HTML
```

---

## Running Tests

```bash
mvn test
```

Runs 7 unit tests covering database initialization, tool functionality, and safety checks.

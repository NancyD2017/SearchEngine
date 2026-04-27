# Search Engine

A fully functional **web search engine** built with Spring Boot — supporting multi-site indexing, full-text search with relevance ranking, and a clean web interface.

---

## Screenshots

<!-- Screenshot: Main dashboard page showing the list of indexed sites with their status (INDEXED / INDEXING / FAILED) and page/lemma counts -->
![image](https://github.com/user-attachments/assets/b3df694a-beac-42ac-b869-1a5a9644f8f9)

<!-- Screenshot: Indexing control panel — show the "Start Indexing" / "Stop Indexing" buttons and the progress indicator while indexing is running -->
![image](https://github.com/user-attachments/assets/855a1c87-aef2-4774-b9ec-64c862e442c2)

<!-- Screenshot: Search results page — show a query entered in the search bar and 3–5 result cards below, each with site name, page title, URL, and a highlighted text snippet -->
![image](https://github.com/user-attachments/assets/93a0d78c-7b2e-4cc3-aa51-4f82fdf2a610)

---

## Features

- **Multi-site indexing** — crawl and index multiple websites defined in config
- **Single-page re-indexing** — update the index for a specific URL on demand
- **Full-text search** — query across all indexed sites or filter by a specific site
- **Relevance ranking** — results sorted by TF-IDF-based relevance score
- **Lemmatization** — Russian-language morphological analysis for accurate indexing
- **REST API** — full programmatic access to all engine functions
- **Web dashboard** — built-in UI for managing indexing and running searches

---

## Tech Stack

| Category | Technology |
|---|---|
| Language | Java 11 |
| Framework | Spring Boot, Spring Data JPA |
| Database | MySQL |
| ORM | Hibernate |
| Frontend | Thymeleaf, JavaScript, CSS |
| Build | Maven |

---

## Getting Started

### Prerequisites
- Java 11+
- MySQL server

### Local Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/NancyD2017/SearchEngine.git
   cd SearchEngine
   ```

2. **Configure the database** — update `application.yaml`:
   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/your_database_name
       username: your_username
       password: your_password
   ```

3. **Configure sites to index** — add target websites in `application.yaml`:
   ```yaml
   indexing-settings:
     sites:
       - url: https://example.com
         name: Example Site
   ```

4. **Build and run**
   ```bash
   mvn clean install
   java -jar target/search-engine-0.0.1-SNAPSHOT.jar
   ```

5. Open the web interface at `http://localhost:8080`

---

## REST API Reference

### Indexing

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/startIndexing` | Start full indexing of all configured sites |
| GET | `/api/stopIndexing` | Stop indexing in progress |
| POST | `/api/indexPage` | Re-index a single page by URL |

### Search

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/search?query=...` | Search across all indexed sites |
| GET | `/api/search?query=...&site=...` | Search within a specific site |

### Statistics

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/statistics` | Get indexing stats (total sites, pages, lemmas) |

#### Example Search Request
```http
GET /api/search?query=spring+boot&site=https://example.com&offset=0&limit=10
```

#### Example Search Response
```json
{
  "result": true,
  "count": 42,
  "data": [
    {
      "site": "https://example.com",
      "siteName": "Example Site",
      "uri": "/blog/spring-boot-intro",
      "title": "Getting Started with Spring Boot",
      "snippet": "...using <b>Spring Boot</b> you can create stand-alone applications...",
      "relevance": 0.93
    }
  ]
}
```

---

## License

Educational project. Built as part of a backend development course at Skillbox.

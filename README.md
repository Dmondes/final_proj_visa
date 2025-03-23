# final_proj_visa

# Fintrend: Reddit Stock Tracker

Fintrend is a web application that tracks trending stocks discussed on popular investing subreddits. It provides users with insights into real-time market sentiment and helps them stay informed about potential investment opportunities.

## Features

*   **Trending Stock Discovery:** Identifies stocks with the most mentions across multiple subreddits (r/wallstreetbets, r/stocks, r/investing, r/StockMarket, r/DeepFuckingValue) within selectable timeframes (24 hours, 7 days).
*   **Real-time Data:**  Scrapes Reddit for new posts and updates ticker counts hourly.  Weekly counts are updated daily.
*   **Stock Details:** Provides key stock information, including current price, change, day's high/low, previous close, and a price range visualization, using the Finnhub API.
*   **Recent Posts:** Displays the 5 most recent Reddit posts related to a selected ticker, allowing users to quickly gauge community sentiment.
*   **User Watchlist:**  Registered users can add and remove stocks from a personalized watchlist for easy tracking (requires registration and login).
*   **Search Functionality:** Users can search for specific stock tickers and view their details.
*   **Responsive Design:**  Built with Angular and Bootstrap for a user-friendly experience across devices.
*   **Clear Navigation:**  Uses Angular Router for easy navigation between different sections (Home, Trending, About, Watchlist, Stock Details).
*   **Pagination** Trending stocks has pagination enabled.

## Technology Stack

*   **Frontend:** Angular (v19), Bootstrap, TypeScript.
*   **Backend:** Spring Boot (Java), REST API.
*   **Database:** MySQL (for user accounts and stock listing), MongoDB (for storing scraped Reddit posts and ticker counts).
*   **APIs:** Reddit API (for scraping posts), Finnhub API (for stock data).
*   **Scheduling:** Spring `@Scheduled` for periodic data updates.
*  **Hash Routing:** Enabled hash routing for Railway deployment.

## Project Structure

The project is divided into two main parts:

*   **`client` (Frontend):**  Contains the Angular application.  Key directories and files include:
    *   `src/app/components`:  Contains Angular components for various parts of the UI (navbar, sidebar, user authentication, etc.).
    *   `src/app/services`:  Provides services for interacting with the backend API (e.g., `StockService`, `UserService`).
    *   `src/app/model`:  Defines data models (e.g., `Stock`, `StockPrice`, `User`, `Post`).
    *   `src/app/app-routing.module.ts`:  Defines the application's routes.
    *   `src/app/app.module.ts`:  The main Angular module.

*   **`server` (Backend):** Contains the Spring Boot application.  Key directories and files include:
    *   `src/main/java/sg/edu/nus/iss/server`:  The main package.
    *   `config`:  Configuration files (e.g., `ScrapConfig` for API keys).
    *   `model`:  Data models (mirrors the frontend models).
    *   `repo`:  Repositories for database interaction (`MongoRepo`, `MysqlRepo`, `ScrapRepo`).
    *   `restcontroller`:  REST controllers (`TicketController`, `UserController`) to handle API requests.
    *   `service`:  Service classes (`ScrapService`, `UserService`) containing business logic.
    *   `ServerApplication.java`:  The main Spring Boot application class, including scheduled tasks.
    *   `resources/application.properties`:  Application configuration (database credentials, API keys, scheduling cron expressions).
     *   `resources/stock.sql`:  SQL script to create the MySQL database and tables.

## Setup and Running

1.  **Prerequisites:**
    *   Java Development Kit (JDK) 17 or later.
    *   Node.js (v18 or later) and npm (Node Package Manager).
    *   Angular CLI (`npm install -g @angular/cli`).
    *   MySQL and MongoDB servers running.
    *   Reddit API credentials (client ID, client secret, username, password)
    *   Finnhub API key.

2.  **Backend (Server):**
    *   Create the MySQL database and tables by running the `src/main/resources/stock.sql` script.
    *   Run the application: `mvn spring-boot:run`

3.  **Frontend (client):**
    *   Navigate to the `client` directory.
    *   Install dependencies: `npm install`

4. **API Endpoints (Backend):**
     *  `/api/trending`: Get trending tickers (with optional `timeframe` parameter: `24h` or `7d`).
     *  `/api/stock/{ticker}`: Get stock details for a given ticker.
     *  `/api/recentposts/{ticker}`: Get the 5 most recent Reddit posts for a ticker.
     * `/api/login`: user login.
     * `/api/register`: create new user.
     *  `/api/user/watchlist/add` and `/api/user/watchlist/remove`: Add/remove tickers from the user's watchlist.
     * `/api/user/{email}`: get user by email, together with user's details.

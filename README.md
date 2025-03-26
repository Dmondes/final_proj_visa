Fintrend tracks trending stocks discussed on Reddit, allowing users to manage personal watchlists and receive automated price alert notifications via web push. Stay ahead of the market buzz!

**➡️ url:** [https://www.fintrend.store/](https://www.fintrend.store/) 🚀

## Key Features ✨

*   **🔥 Trending Stock Discovery:** Identifies stocks with the most mentions across popular investing subreddits (r/wallstreetbets, r/stocks, r/investing, etc.) based on selectable timeframes (24 hours, 7 days).
*   **📊 Real-time Stock Data:** Displays near real-time stock price information (current price, daily change, high/low, previous close) using the Finnhub API.
*   **💬 Reddit Context:** Shows recent Reddit posts related to a specific ticker, providing context for why it might be trending.
*   **⭐ Personalized Watchlist:** Logged-in users can create and manage a custom watchlist of stocks they want to monitor closely.
*   **🔔 Price Alert Notifications:** Users can set custom price targets (above/below) for stocks in their watchlist and receive **Web Push Notifications** via Firebase Cloud Messaging (FCM) when those targets are met (requires browser permission).
*   **🔒 Secure Authentication:** User registration and login handled via Firebase Authentication. Backend API endpoints are protected.
*   **📱 Responsive Design:** Built with Angular and Bootstrap for a seamless experience on desktop and mobile web browsers.
*   **🔍 Ticker Search:** Easily search for specific stock tickers to view their details.
*   **⚙️ Automated Scraping:** Backend service automatically scrapes Reddit hourly and updates trending counts.



## Technology Stack 🛠️

*   **Frontend:**
    *   Angular (~v19)
    *   TypeScript
    *   Bootstrap 5 / ng-bootstrap
    *   Firebase Client SDK (Authentication, Cloud Messaging)
    *   Workbox (for Service Worker - simplified configuration)
*   **Backend:**
    *   Java 23
    *   Spring Boot 3.4
    *   Spring Web (REST API)
    *   Spring Data MongoDB
    *   Spring JDBC (`JdbcTemplate`)
    *   Firebase Admin SDK (Authentication verification, FCM sending)
    *   Maven
*   **Databases:**
    *   **MongoDB:** Stores scraped Reddit post details and aggregated ticker counts.
    *   **MySQL:** Stores user accounts (email, watchlist, price alerts, FCM token) and the initial stock listing.
*   **External APIs:**
    *   Reddit API (OAuth2 for data scraping)
    *   Finnhub API (Real-time stock quotes)
*   **Deployment:**
    *   Docker
    *   Railway (or any platform supporting Docker and environment variables)

## Architecture Overview 🏗️

Fintrend follows a standard client-server architecture:

1.  **Backend (Spring Boot):**
    *   Scheduled tasks scrape Reddit posts using the Reddit API.
    *   Analyzes posts to extract relevant stock tickers.
    *   Stores post data and aggregated ticker counts in MongoDB.
    *   Provides REST API endpoints (`/api/...`).
    *   Fetches real-time stock data from Finnhub API upon request.
    *   Manages user data (registration, login, watchlist, alerts, FCM tokens) in MySQL.
    *   Verifies user authentication using Firebase Admin SDK.
    *   Checks price alerts periodically and sends push notifications via FCM using Firebase Admin SDK.
2.  **Frontend (Angular):**
    *   Provides the user interface.
    *   Interacts with the backend REST API to display trending stocks, stock details, manage watchlists, etc.
    *   Handles user login/registration via the Firebase Client SDK, sending ID tokens to the backend for verification.
    *   Requests notification permission and receives/manages the FCM client token via Firebase Client SDK, sending it to the backend.
    *   Uses a Service Worker (`service-worker.js` + `firebase-messaging-sw.js`) to handle background push message reception and display notifications.

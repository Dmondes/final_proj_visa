importScripts(
  "https://storage.googleapis.com/workbox-cdn/releases/6.4.1/workbox-sw.js"
);

const routing = workbox.routing;
const strategies = workbox.strategies;


workbox.routing.registerRoute(
  /.(?:css|js|jsx|json)$/,
  new workbox.strategies.StaleWhileRevalidate({
    cacheName: "assets",
    plugins: [
      new workbox.expiration.Plugin({
        maxEntries: 1000,
        maxAgeSeconds: 31536000,
      }),
    ],
  })
);

workbox.routing.registerRoute(
  /.(?:png|jpg|jpeg|gif|woff2|ico)$/,
  new workbox.strategies.CacheFirst({
    cacheName: "images",
    plugins: [
      new workbox.expiration.Plugin({
        maxEntries: 1000,
        maxAgeSeconds: 31536000,
      }),
    ],
  })
);


//SPA angular pages
workbox.routing.registerRoute(
  /\/$/,
  new workbox.strategies.StaleWhileRevalidate({
    cacheName: "startPage",
    plugins: [
      new workbox.expiration.Plugin({
        maxEntries: 50,
        maxAgeSeconds: 86400, // 24 hours
      }),
    ],
  })
);

// API requests
workbox.routing.registerRoute(
  /\/api\//,
  new workbox.strategies.NetworkFirst({
    cacheName: "api-cache",
    plugins: [
      new workbox.expiration.Plugin({
        maxEntries: 50,
        maxAgeSeconds: 3600, // 1 hour
      }),
    ],
  })
);

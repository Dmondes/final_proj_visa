// src/service-worker.js

// Import Workbox (using a recent version)
importScripts(
  "https://storage.googleapis.com/workbox-cdn/releases/6.4.1/workbox-sw.js"
);

// Optional: Ensure console logs appear for Workbox
// workbox.setConfig({ debug: true }); // Remove for production

// Check if Workbox loaded
if (workbox) {
  console.log(`Workbox ${workbox.core.version} loaded 🎉`);

  // Immediately activate new service workers
  workbox.core.skipWaiting();
  workbox.core.clientsClaim();

  // --- Strategies ---
  const { registerRoute } = workbox.routing;
  const { CacheFirst, NetworkFirst, NetworkOnly } = workbox.strategies;
  const { ExpirationPlugin } = workbox.expiration;

  // 1. Cache Images, Fonts (CacheFirst is good here)
  registerRoute(
    ({ request }) => request.destination === 'image' || request.destination === 'font',
    new CacheFirst({
      cacheName: 'static-assets-cache',
      plugins: [
        new ExpirationPlugin({
          maxEntries: 150,                  
          maxAgeSeconds: 30 * 24 * 60 * 60, 
          purgeOnQuotaError: true,          
        }),
      ],
    })
  );

  // 2. Handle Application Shell / Navigation (index.html)
  registerRoute(
    ({ request }) => request.mode === 'navigate' || request.destination === 'document',
    new NetworkFirst({
      cacheName: 'app-shell-cache',
      plugins: [
        new ExpirationPlugin({
          maxEntries: 10, 
          maxAgeSeconds: 7 * 24 * 60 * 60, 
        }),
      ],
    })
  );

  // 3. Handle CSS & JS - Use NetworkOnly
  registerRoute(
    ({ request }) => request.destination === 'style' || request.destination === 'script',
    new NetworkOnly()
  );

  // 4. Handle API Calls
  registerRoute(
    ({ url }) => url.pathname.startsWith('/api/'), 
    new NetworkFirst({
      cacheName: 'api-cache',
      networkTimeoutSeconds: 5, 
      plugins: [
        new ExpirationPlugin({
          maxEntries: 60,
          maxAgeSeconds: 1 * 60 * 60,
        }),
      ],
    })
  );

   registerRoute(
     ({ request }) => request.destination === 'manifest',
     new workbox.strategies.StaleWhileRevalidate({
       cacheName: 'manifest-cache',
       plugins: [
         new ExpirationPlugin({
           maxEntries: 5,
           maxAgeSeconds: 7 * 24 * 60 * 60, // 7 days
         }),
       ],
     })
   );


} else {
  console.error("Workbox failed to load");
}

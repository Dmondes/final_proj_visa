// Give the service worker access to Firebase Messaging.
// Note that you can only use Firebase Messaging here. Other Firebase libraries
// are not available in the service worker.
importScripts('https://www.gstatic.com/firebasejs/11.5.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/11.5.0/firebase-messaging-compat.js');

// Initialize the Firebase app in the service worker by passing in
// your app's Firebase config object.
firebase.initializeApp({
  apiKey: "AIzaSyB137VNCXuXn9Y0hM8uA5hOQEEoGnArmwY",
  authDomain: "fintrenduser.firebaseapp.com",
  projectId: "fintrenduser",
  storageBucket: "fintrenduser.firebasestorage.app",
  messagingSenderId: "431029665236",
  appId: "1:431029665236:web:d922b37d16b2a9bda6aa41",
  measurementId: "G-QK72K52FWJ"
});

// Retrieve an instance of Firebase Messaging so that it can handle background
// messages.
const messaging = firebase.messaging();

// Handle background messages
messaging.onBackgroundMessage((payload) => {
  console.log('[firebase-messaging-sw.js] Received background message ', payload);
  
  // Customize notification here
  const notificationTitle = payload.notification.title || 'Fintrend Notification';
  const notificationBody = payload.notification?.body || 'Check your watchlist!';
  const notificationIcon = '/assets/icons/icon-72x72.png';
  const notificationData = payload.data;
  const notificationOptions = {
    body: notificationBody,
    icon: notificationIcon,
    data: notificationData 
  };
  return self.registration.showNotification(notificationTitle, notificationOptions);
});

// Handle notification click
self.addEventListener('notificationclick', event => {
  console.log('[firebase-messaging-sw.js] Notification click Received.');
  event.notification.close();

  const notificationData = event.notification.data;
  console.log('[firebase-messaging-sw.js] Clicked notification data: ', notificationData);

  let urlToOpen = '/'; // Default URL
  if (notificationData && notificationData.ticker) {
    urlToOpen = `/stock/${notificationData.ticker}`; // Navigate to specific stock
  } else if (notificationData && notificationData.url) {
     urlToOpen = notificationData.url; // Or use a specific URL from backend
  }

   console.log('[firebase-messaging-sw.js] Opening window: ', urlToOpen);

  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then((clientList) => {
      // Check if a window/tab matching the target URL already exists
      for (let i = 0; i < clientList.length; i++) {
        const client = clientList[i];
        // Use URL constructor for robust comparison (handles trailing slashes etc.)
        try {
            const clientUrl = new URL(client.url);
            const targetUrl = new URL(urlToOpen, self.location.origin); // Use origin for relative paths

            if (clientUrl.pathname === targetUrl.pathname && 'focus' in client) {
                console.log('[firebase-messaging-sw.js] Found matching client, focusing.');
                return client.focus();
            }
        } catch (e) {
            // Handle potential URL parsing errors if needed
             console.error("Error parsing URL in service worker:", e);
        }
      }
      // If no matching client is found, open a new window/tab
      if (clients.openWindow) {
        console.log('[firebase-messaging-sw.js] No matching client, opening new window.');
        return clients.openWindow(urlToOpen);
      }
    })
  );
});
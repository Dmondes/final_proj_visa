// Give the service worker access to Firebase Messaging.
// Note that you can only use Firebase Messaging here. Other Firebase libraries
// are not available in the service worker.
importScripts('https://www.gstatic.com/firebasejs/11.5.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/11.5.0/firebase-messaging-compat.js');

// pass credentials, shld hide
firebase.initializeApp({
  apiKey: "AIzaSyB137VNCXuXn9Y0hM8uA5hOQEEoGnArmwY",
  authDomain: "fintrenduser.firebaseapp.com",
  projectId: "fintrenduser",
  storageBucket: "fintrenduser.firebasestorage.app",
  messagingSenderId: "431029665236",
  appId: "1:431029665236:web:d922b37d16b2a9bda6aa41",
  measurementId: "G-QK72K52FWJ"
});

// Retrieve an instance of Firebase Messaging for backgrd message
const messaging = firebase.messaging();

// Handle background messages
messaging.onBackgroundMessage((payload) => {
  console.log('[firebase-messaging-sw.js] Received background message ', payload);
  
  // Customize notification
  const notificationTitle = payload.notification.title || 'Fintrend Notification';
  const notificationBody = payload.notification?.body || 'Price target reached, check your watchlist!';
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

  let urlToOpen = '/'; 
  if (notificationData && notificationData.ticker) {
    urlToOpen = `#/stock/${notificationData.ticker}`; // Navigate to specific stock
  } else if (notificationData && notificationData.url) {
     urlToOpen = notificationData.url;
  }

   console.log('[firebase-messaging-sw.js] Opening window: ', urlToOpen);

  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then((clientList) => {
      for (let i = 0; i < clientList.length; i++) {
        const client = clientList[i];
        try {
            const clientUrl = new URL(client.url);
            const targetUrl = new URL(urlToOpen, self.location.origin); // Use origin for relative paths

            if (clientUrl.pathname === targetUrl.pathname && 'focus' in client) {
                console.log('[firebase-messaging-sw.js] Found matching client, focusing.');
                return client.focus();
            }
        } catch (e) {
             console.error("Error parsing URL in service worker:", e);
        }
      }
      if (clients.openWindow) {
        console.log('[firebase-messaging-sw.js] No matching client, opening new window.');
        return clients.openWindow(urlToOpen);
      }
    })
  );
});
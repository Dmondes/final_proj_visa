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
  const notificationOptions = {
    body: payload.notification.body || 'New stock update available!',
    icon: '/assets/icons/icon-72x72.png'
  };

  return self.registration.showNotification(notificationTitle, notificationOptions);
});

// Handle notification click
self.addEventListener('notificationclick', event => {
  event.notification.close();
  
  // This looks to see if the current window is already open and
  // focuses if it is
  event.waitUntil(
    clients.matchAll({
      type: "window"
    })
    .then(function(clientList) {
      // If a window tab matching the targeted URL already exists, focus that;
      for (var i = 0; i < clientList.length; i++) {
        var client = clientList[i];
        if (client.url === '/' && 'focus' in client)
          return client.focus();
      }
      // If no matching client is found, open a new one
      if (clients.openWindow)
        return clients.openWindow('/');
    })
  );
});
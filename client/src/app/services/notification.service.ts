import { Injectable, inject } from '@angular/core';
import { AngularFireMessaging } from '@angular/fire/compat/messaging';
import { BehaviorSubject } from 'rxjs';
import { UserService } from './user.service';
import { UserStore } from '../user.store';
import { firstValueFrom } from 'rxjs';
import { Router } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private messaging = inject(AngularFireMessaging);
  private userService = inject(UserService);
  private userStore = inject(UserStore);
  private router = inject(Router);
  
  currentMessage = new BehaviorSubject<any>(null);
  
  constructor() {
    // Listen for messages when the app is in the foreground
    this.messaging.messages.subscribe((message: any) => {
      console.log('New foreground message received:', message);
      this.currentMessage.next(message);
      
      // Show browser notification
      if (Notification.permission === 'granted') {
        const title = message.notification?.title || 'New Notification';
        const options = {
          body: message.notification?.body || '',
          icon: message.notification?.icon || '/assets/icons/icon-72x72.png',
          data: message.data // Pass along data payload
        };
        const notification = new Notification(title, options);
        
        notification.onclick = (event: any | null) => {
          console.log('Foreground notification clicked. Data:', event.target.data);
          window.focus();
          // Try to navigate based on data payload if available
          const ticker = event.target.data?.ticker;
          if (ticker) {
            this.router.navigate(['/stock', ticker]);
          }
          notification.close();
        };
      }
    });
  }

    //Request permission and get FCM token
  async requestPermission(): Promise<string | null> {
    try {
      // Check if the user is logged in
      const user = await firstValueFrom(this.userStore.currentUser$);
      if (!user) {
        console.log('User not logged in, cannot request notification permission');
        return null;
      }
      
      // Request permission
      const permission = await Notification.requestPermission();
      if (permission !== 'granted') {
        console.log('Notification permission not granted');
        return null;
      }
      
      // Get token
      const token = await firstValueFrom(this.messaging.requestToken);
      console.log('FCM Token:', token);
      
      // Send token to server
      if (user.email && token) {
        await this.userService.updateFCMToken(user.email, token);
      }
      
      return token;
    } catch (error) {
      console.error('Failed to get notification permission:', error);
      return null;
    }
  }
  
   // Delete the FCM token
  async deleteToken(): Promise<boolean> {
    try {
      // First get the current token
      const token = await firstValueFrom(this.messaging.getToken);
      if (token) {
        // Then delete it
        await firstValueFrom(this.messaging.deleteToken(token));
        console.log('Token deleted successfully');
        return true;
      }
      return false;
    } catch (error) {
      console.error('Failed to delete token:', error);
      return false;
    }
  }
  
  //  //Subscribe to price alerts for a specific ticker
  // async subscribeToPriceAlerts(ticker: string): Promise<void> {
  //   try {
  //     const user = await firstValueFrom(this.userStore.currentUser$);
  //     if (!user) {
  //       throw new Error('User not logged in');
  //     }
      
  //     console.log(`Subscribed to price alerts for ${ticker}`);
  //   } catch (error) {
  //     console.error(`Error subscribing to price alerts for ${ticker}:`, error);
  //   }
  // }
  
  //  //Unsubscribe from price alerts for a specific ticker
  // async unsubscribeFromPriceAlerts(ticker: string): Promise<void> {
  //   try {
  //     const user = await firstValueFrom(this.userStore.currentUser$);
  //     if (!user) {
  //       throw new Error('User not logged in');
  //     }
      
  //     console.log(`Unsubscribed from price alerts for ${ticker}`);
  //   } catch (error) {
  //     console.error(`Error unsubscribing from price alerts for ${ticker}:`, error);
  //   }
  // }
}
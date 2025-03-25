import { Injectable, inject } from '@angular/core';
import { AngularFireMessaging } from '@angular/fire/compat/messaging';
import { BehaviorSubject } from 'rxjs';
import { UserService } from './user.service';
import { UserStore } from '../user.store';
import { firstValueFrom } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private messaging = inject(AngularFireMessaging);
  private userService = inject(UserService);
  private userStore = inject(UserStore);
  
  currentMessage = new BehaviorSubject<any>(null);
  
  constructor() {
    // Listen for messages when the app is in the foreground
    this.messaging.messages.subscribe((message) => {
      console.log('New foreground message received:', message);
      this.currentMessage.next(message);
      
      // Show browser notification
      if (Notification.permission === 'granted') {
        const notification = new Notification(message.notification?.title || 'New Notification', {
          body: message.notification?.body || '',
          icon: '/assets/icons/icon-72x72.png'
        });
        
        notification.onclick = () => {
          console.log('Notification clicked');
          window.focus();
          notification.close();
        };
      }
    });
  }
  
  /**
   * Request permission and get FCM token
   */
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
      await this.userService.updateFCMToken(user.email, token);
      
      return token;
    } catch (error) {
      console.error('Failed to get notification permission:', error);
      return null;
    }
  }
  
  /**
   * Delete the FCM token
   */
  async deleteToken(): Promise<boolean> {
    try {
      await this.messaging.deleteToken.toPromise();
      console.log('Token deleted successfully');
      return true;
    } catch (error) {
      console.error('Failed to delete token:', error);
      return false;
    }
  }
  
  /**
   * Subscribe to price alerts for a specific ticker
   */
  async subscribeToPriceAlerts(ticker: string): Promise<void> {
    try {
      const user = await firstValueFrom(this.userStore.currentUser$);
      if (!user) {
        throw new Error('User not logged in');
      }
      
      console.log(`Subscribed to price alerts for ${ticker}`);
    } catch (error) {
      console.error(`Error subscribing to price alerts for ${ticker}:`, error);
    }
  }
  
  /**
   * Unsubscribe from price alerts for a specific ticker
   */
  async unsubscribeFromPriceAlerts(ticker: string): Promise<void> {
    try {
      const user = await firstValueFrom(this.userStore.currentUser$);
      if (!user) {
        throw new Error('User not logged in');
      }
      
      console.log(`Unsubscribed from price alerts for ${ticker}`);
    } catch (error) {
      console.error(`Error unsubscribing from price alerts for ${ticker}:`, error);
    }
  }
}
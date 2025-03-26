import { Component, OnInit, inject } from '@angular/core';
import { UserStore } from '../../user.store';
import { UserService } from '../../services/user.service';
import { NotificationService } from '../../services/notification.service';
import { PriceAlert } from '../../model/user';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-notification-settings',
  standalone: false,
  templateUrl: './notification-settings.component.html',
  styleUrl: './notification-settings.component.css'
})
export class NotificationSettingsComponent implements OnInit {
  private userStore = inject(UserStore);
  private userService = inject(UserService);
  private notificationService = inject(NotificationService);
  
  notificationsEnabled = false;
  priceAlerts: PriceAlert[] = [];
  hasPriceAlerts = false;
  
  ngOnInit(): void {
    this.checkNotificationStatus();
    
    this.loadPriceAlerts();
  }
  
  private async checkNotificationStatus(): Promise<void> {
    this.notificationsEnabled = Notification.permission === 'granted';
  }
  
  async requestNotificationPermission(): Promise<void> {
    const token = await this.notificationService.requestPermission();
    this.notificationsEnabled = !!token;
  }
  
  private async loadPriceAlerts(): Promise<void> {
    const user = await firstValueFrom(this.userStore.currentUser$);
    if (user && user.priceAlerts) {
      this.priceAlerts = Object.values(user.priceAlerts);
      this.hasPriceAlerts = this.priceAlerts.length > 0;
    }
  }
  
  async removePriceAlert(ticker: string): Promise<void> {
    const user = await firstValueFrom(this.userStore.currentUser$);
    if (!user) {
      return;
    }
    
    try {
      await this.userService.removePriceAlert(user.email, ticker);
      // Refresh alerts list
      this.loadPriceAlerts();
    } catch (error) {
      console.error(`Error removing price alert for ${ticker}:`, error);
    }
  }
}
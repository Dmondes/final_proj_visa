import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { UserService } from '../../services/user.service';
import { UserStore } from '../../user.store';
import { firstValueFrom } from 'rxjs';
import { Router } from '@angular/router';
import { StockService } from '../../services/stock.service';
import { NotificationService } from '../../services/notification.service';
import { StockPrice } from '../../model/stockPrice';
import { PriceAlert } from '../../model/user';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-watchlist',
  standalone: false,
  templateUrl: './watchlist.component.html',
  styleUrl: './watchlist.component.css'
})
export class WatchlistComponent implements OnInit, OnDestroy {

  private userService = inject(UserService);
  private userStore = inject(UserStore);
  private router = inject(Router);
  private stockService = inject(StockService);
  private notificationService = inject(NotificationService);
  
  user = this.userStore.currentUser$;
  watchlist = this.userStore.watchlist$;
  
  stockPrices: { [ticker: string]: StockPrice } = {};
  priceAlerts: { [ticker: string]: PriceAlert } = {};
  alertPrices: { [ticker: string]: number } = {};
  alertConditions: { [ticker: string]: 'above' | 'below' } = {};
  
  private priceRefreshInterval: any;

  ngOnInit(): void {
    // Request notification permission
    this.notificationService.requestPermission();
    
    // Load current price alerts
    this.loadPriceAlerts();
    
    // Initial price load
    this.refreshPrices();
    
    // Set up daily refresh
    this.priceRefreshInterval = setInterval(() => {
      this.refreshPrices();
    }, 24 * 60 * 60 * 1000); // 24 hours
  }
  
  ngOnDestroy(): void {
    if (this.priceRefreshInterval) {
      clearInterval(this.priceRefreshInterval);
    }
  }
  
  async loadPriceAlerts(): Promise<void> {
    const user = await firstValueFrom(this.userStore.currentUser$);
    if (user && user.priceAlerts) {
      this.priceAlerts = user.priceAlerts;
      
      // Initialize form values from existing alerts
      Object.keys(this.priceAlerts).forEach(ticker => {
        this.alertPrices[ticker] = this.priceAlerts[ticker].targetPrice;
        this.alertConditions[ticker] = this.priceAlerts[ticker].condition;
      });
    }
  }
  
  async refreshPrices(): Promise<void> {
    const watchlist = await firstValueFrom(this.watchlist);
    if (!watchlist || watchlist.size === 0) {
      return;
    }
    
    for (const ticker of watchlist) {
      try {
        const price = await firstValueFrom(this.stockService.getStockDetails(ticker));
        this.stockPrices[ticker] = price;
        
        // Check for price alerts
        this.checkPriceAlert(ticker, price.c);
      } catch (error) {
        console.error(`Error fetching price for ${ticker}:`, error);
      }
    }
  }
  
  async setPriceAlert(ticker: string, targetPrice: number, condition: 'above' | 'below'): Promise<void> {
    if (!targetPrice) {
      return;
    }
    
    const user = await firstValueFrom(this.userStore.currentUser$);
    if (!user) {
      return;
    }
    
    const alert: PriceAlert = {
      ticker,
      targetPrice,
      condition: condition || 'above',
      createdAt: Date.now()
    };
    
    // Update local state
    this.priceAlerts[ticker] = alert;
    
    // Update server
    try {
      await this.userService.setPriceAlert(user.email, ticker, targetPrice, condition);
    } catch (error) {
      console.error(`Error setting price alert for ${ticker}:`, error);
      delete this.priceAlerts[ticker];
      window.alert(`Error setting price alert for ${ticker}. Please try again.`);
    }
  }
  
  async removePriceAlert(ticker: string): Promise<void> {
    const user = await firstValueFrom(this.userStore.currentUser$);
    if (!user) {
      return;
    }
    
    // Update local state
    const alertCopy = this.priceAlerts[ticker];
    delete this.priceAlerts[ticker];
    
    // Update server
    try {
      await this.userService.removePriceAlert(user.email, ticker);
    } catch (error) {
      console.error(`Error removing price alert for ${ticker}:`, error);
      this.priceAlerts[ticker] = alertCopy;
      window.alert(`Error removing price alert for ${ticker}. Please try again.`);
    }
  }
  
  private checkPriceAlert(ticker: string, currentPrice: number): void {
    const alert = this.priceAlerts[ticker];
    if (!alert) {
      return;
    }
    
    let isTriggered = false;
    
    if (alert.condition === 'above' && currentPrice >= alert.targetPrice) {
      isTriggered = true;
    } else if (alert.condition === 'below' && currentPrice <= alert.targetPrice) {
      isTriggered = true;
    }
    
    if (isTriggered) {
      // Notify user
      const title = `${ticker} Price Alert`;
      const body = `${ticker} is now ${alert.condition === 'above' ? 'above' : 'below'} your target price of $${alert.targetPrice.toFixed(2)} (Current: $${currentPrice.toFixed(2)})`;
      
      // Create a custom notification
      if (Notification.permission === 'granted') {
        const notification = new Notification(title, { body });
        notification.onclick = () => {
          window.focus();
          this.router.navigate(['/stock', ticker]);
          notification.close();
        };
      }
    }
  }
  
  navigateToStock(ticker: string): void {
    this.router.navigate(['/stock', ticker]);
  }

  async removeFromWatchlist(ticker: string) {
    const user = await firstValueFrom(this.userStore.currentUser$);
    if (user) {
      try {
        // Also remove any price alerts for this ticker
        if (this.priceAlerts[ticker]) {
          await this.removePriceAlert(ticker);
        }
        
        await this.userService.removeFromWatchlist(user.email, ticker);
        delete this.stockPrices[ticker];
      } catch (error) {
        console.error(`Error removing ${ticker} from watchlist:`, error);
        window.alert(`Error removing ${ticker} from watchlist. Please try again.`);
      }
    }
  }
}

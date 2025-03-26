import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { UserService } from '../../services/user.service';
import { UserStore } from '../../user.store';
import { firstValueFrom } from 'rxjs';
import { Router } from '@angular/router';
import { StockService } from '../../services/stock.service';
import { StockPrice } from '../../model/stockPrice';
import { PriceAlert } from '../../model/user';


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

  user = this.userStore.currentUser$;
  watchlist = this.userStore.watchlist$;

  stockPrices: { [ticker: string]: StockPrice } = {};
  priceAlerts: { [ticker: string]: PriceAlert } = {};
  alertPrices: { [ticker: string]: number } = {};
  alertConditions: { [ticker: string]: 'above' | 'below' } = {};

  private priceRefreshInterval: any;

  ngOnInit(): void {

    // Load current price alerts
    this.loadPriceAlerts();

    // Initial price load
    this.refreshPrices();

    // Set up daily refresh
    this.priceRefreshInterval = setInterval(() => {
      this.refreshPrices();
    }, 12 * 60 * 60 * 1000); // 12 hours
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
      } catch (error) {
        console.error(`Error fetching price for ${ticker}:`, error);
      }
    }
  }

  async setPriceAlert(ticker: string, targetPrice: number, condition: 'above' | 'below'): Promise<void> {
    if (!targetPrice) {
      console.warn('Target price is required to set an alert.');
      return;
    }

    const user = await firstValueFrom(this.userStore.currentUser$);
    if (!user) {
      console.error('User not found, cannot set price alert.');
      return;
    }

    const alertCondition = condition || 'above';

    const alert: PriceAlert = {
      ticker,
      targetPrice,
      condition: condition || 'above',
      createdAt: Date.now()
    };

    // Update local state, optimistic update
    this.priceAlerts[ticker] = alert;
    this.alertPrices[ticker] = targetPrice;
    this.alertConditions[ticker] = alertCondition;

    // Update server
    try {
      await this.userService.setPriceAlert(user.email, ticker, targetPrice, alertCondition);
      console.log(`Price alert set for ${ticker}`);
      // Add success feedback (e.g., toast)
    } catch (error) {
      console.error(`Error setting price alert for ${ticker}:`, error);
      // Revert optimistic update on failure
      delete this.priceAlerts[ticker];
      delete this.alertPrices[ticker];
      delete this.alertConditions[ticker];
      window.alert(`Error setting price alert for ${ticker}. Please try again.`); // Replace window.alert with better UI
    }
  }

  async removePriceAlert(ticker: string): Promise<void> {
    const user = await firstValueFrom(this.userStore.currentUser$);
    if (!user) {
      console.error('User not found, cannot remove price alert.');
      return;
    }

    // Update local state
    const alertCopy = this.priceAlerts[ticker];
    delete this.priceAlerts[ticker];
    delete this.alertPrices[ticker]; 
    delete this.alertConditions[ticker];

    // Update server
    try {
      await this.userService.removePriceAlert(user.email, ticker);
      console.log(`Price alert removed for ${ticker}`);
    } catch (error) {
      console.error(`Error removing price alert for ${ticker}:`, error);
      // Revert optimistic update on failure
      if (alertCopy) { //use copy instead
         this.priceAlerts[ticker] = alertCopy;
         this.alertPrices[ticker] = alertCopy.targetPrice;
         this.alertConditions[ticker] = alertCopy.condition;
      }
      window.alert(`Error removing price alert for ${ticker}. Please try again.`);
    }
  }

  navigateToStock(ticker: string): void {
    this.router.navigate(['/stock', ticker]);
  }

  async removeFromWatchlist(ticker: string) {
    const user = await firstValueFrom(this.userStore.currentUser$);
    if (user) {
      try {
        if (this.priceAlerts[ticker]) {
          await this.removePriceAlert(ticker);
        }

        await this.userService.removeFromWatchlist(user.email, ticker);
        delete this.stockPrices[ticker];
        console.log(`${ticker} removed from watchlist.`);
      } catch (error) {
        console.error(`Error removing ${ticker} from watchlist:`, error);
        window.alert(`Error removing ${ticker} from watchlist. Please try again.`);
      }
    }
  }
}

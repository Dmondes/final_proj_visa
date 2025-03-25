import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription, firstValueFrom } from 'rxjs';
import { StockService } from '../../services/stock.service';
import { UserService } from '../../services/user.service';
import { UserStore } from '../../user.store';

import { Title } from '@angular/platform-browser';
import { StockPrice } from '../../model/stockPrice';

@Component({
  selector: 'app-stock',
  standalone: false,
  templateUrl: './stock.component.html',
  styleUrl: './stock.component.css'
})
export class StockComponent implements OnInit, OnDestroy {
  ticker: string = '';
  stock: StockPrice | null = null;
  isLoading: boolean = true;
  error: string = '';
  private subscription = new Subscription();

  private route = inject(ActivatedRoute);
  private stockService = inject(StockService);
  private userService = inject(UserService);
  private userStore = inject(UserStore);
  private router = inject(Router);
  private title = inject(Title);
  
  loginStatus = this.userStore.isLoggedIn$;


  ngOnInit(): void {
    this.subscription.add(
      this.route.params.subscribe(params => {
        this.ticker = params['ticker'];
        this.loadStockDetails(this.ticker);
        this.title.setTitle(`${this.ticker} - Fintrend`);
      })
    );
  }

  loadStockDetails(ticker: string): void {
    this.isLoading = true;
    this.subscription.add(
      this.stockService.getStockDetails(ticker).subscribe({
        next: (data) => {
          this.stock = data;
          this.isLoading = false;
        },
        error: (err) => {
          console.error('Error fetching stock details:', err);
          this.error = 'Unable to fetch stock data. Please try again later.';
          this.isLoading = false;
        }
      })
    );
  }

  getPriceChangeClass(): string {
    if (!this.stock) return '';
    return this.stock.d > 0 ? 'text-success' : 'text-danger';
  }

  getChangeIcon(): string {
    if (!this.stock) return '';
    return this.stock.d > 0 ? 'ti ti-trending-up' : 'ti ti-trending-down';
  }

  async addToWatchlist(ticker: string) {
    try {
      const user = await firstValueFrom(this.userService.currentUser);

      if (user) {
        await this.userService.addToWatchlist(user.email, ticker);
        alert(`${ticker} added to watchlist!`); // Success message
      } else {
        this.router.navigate(['/login']); // Redirect to login if not logged in
      }
    } catch (error) {
      console.error(`Error adding ${ticker} to watchlist:`, error);
      alert(`Error adding ${ticker} to watchlist. Please try again.`);
    }
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }
}
import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs';
import { StockService } from '../../services/stock.service';

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
  private title = inject(Title);


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

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }
}
import { Component, inject, OnInit } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { Router } from '@angular/router';
import { StockService } from '../../services/stock.service';
import { FormControl } from '@angular/forms';
import { UserService } from '../../services/user.service';
import { UserStore } from '../../user.store';

@Component({
  selector: 'app-trending',
  standalone: false,
  templateUrl: './trending.component.html',
  styleUrl: './trending.component.css'
})
export class TrendingComponent implements OnInit {

  trendingStocks: any[] | undefined;
  timeFrameControl = new FormControl('24h');
  lastUpdate = new Date();
  timeFrame = '24h';
  recentPosts: any[] | undefined;
  selectedTicker!: string;

  // Page properties
  currentPage = 1;
  itemsPerPage = 10;
  totalPages = 0;

  private stockService = inject(StockService);
  private userService = inject(UserService);
  private userStore = inject(UserStore);
  private router = inject(Router);

  loginStatus = this.userStore.isLoggedIn$;

  ngOnInit(): void {
    this.loadDataByTimeframe(this.timeFrame);
    this.timeFrameControl.valueChanges.subscribe(value => {
      if (value) this.loadDataByTimeframe(value);
    })
  }

  loadDataByTimeframe(timeframe: string) {
    this.stockService.getTrendingStocks(timeframe).subscribe(data => { //pass timeframe 
      
      this.trendingStocks = Object.entries(data).sort((a: any, b: any) => b[1] - a[1]);
      this.lastUpdate = new Date();
      this.recentPosts = [];
      this.selectedTicker = "";

      // Calculate total pages
      this.totalPages = Math.ceil((this.trendingStocks?.length || 0) / this.itemsPerPage);
      this.currentPage = 1; // Reset to first page when loading new data
    });
  }

  showRecentPosts(ticker: string) {
    this.selectedTicker = ticker;
    // Pass the selected timeframe
    this.stockService.getRecentPosts(ticker, this.timeFrameControl.value!).subscribe(posts => {
      this.recentPosts = posts;
    });
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

  // Get current page items
  get paginatedStocks(): any[] {
    if (!this.trendingStocks) return [];

    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    return this.trendingStocks.slice(startIndex, endIndex);
  }
}
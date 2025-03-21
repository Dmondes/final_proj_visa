import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { User } from '../../model/user';
import { firstValueFrom, Subscription } from 'rxjs';
import { Router } from '@angular/router';
import { StockService } from '../../services/stock.service';
import { FormControl } from '@angular/forms';
import { UserService } from '../../services/user.service';

@Component({
  selector: 'app-trending',
  standalone: false,
  templateUrl: './trending.component.html',
  styleUrl: './trending.component.css'
})
export class TrendingComponent implements OnInit, OnDestroy {

  // Initialize trendingStocks as undefined.  Crucial!
  trendingStocks: any[] | undefined;
  timeFrameControl = new FormControl('24hr');
  lastUpdate = new Date();
  timeFrame = '24hr';
  sub !: Subscription;
  recentPosts: any[] | undefined;
  selectedTicker!: string;
  loginStatus!: boolean;

  // Pagination properties
  currentPage = 1;
  itemsPerPage = 10;
  totalPages = 1;

  private stockService = inject(StockService);
  private userService = inject(UserService);
  private router = inject(Router);

  ngOnInit(): void {
    this.loadDataByTimeframe(this.timeFrame);
    this.sub = this.userService.loginStatus$.subscribe(status => {
      this.loginStatus = status;
    });
    this.timeFrameControl.valueChanges.subscribe(value => {
      if (value) this.loadDataByTimeframe(value);
    })
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  loadDataByTimeframe(timeframe: string) {
    this.stockService.getTrendingStocks().subscribe(data => {
      // Correctly sort and assign to trendingStocks.  It's now an array.
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
    this.stockService.getRecentPosts(ticker).subscribe(posts => {
      this.recentPosts = posts;
    });
  }

  async addToWatchlist(ticker: string) {
    // Use firstValueFrom with currentUser$ to get the current user as a Promise.
    try {
      const user = await firstValueFrom(this.userService.currentUser$);

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

  // Navigation methods
  goToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
    }
  }

  prevPage(): void {
    if (this.currentPage > 1) {
      this.currentPage--;
    }
  }

  // Helper method to generate page numbers for pagination
  getPageNumbers(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i + 1);
  }
}
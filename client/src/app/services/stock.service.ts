import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs/internal/Observable';
import { StockPrice } from '../model/stockPrice';

@Injectable({
  providedIn: 'root'
})
export class StockService {
  private apiUrl = '/api';

  private http = inject(HttpClient);

  getStockDetails(ticker: string): Observable<StockPrice> {
    return this.http.get<StockPrice>(`${this.apiUrl}/stock/${ticker}`);
  }

  getRecentPosts(ticker: string, timeframe: string = '24h'): Observable<any[]> { // Add timeframe, default to 24h
    return this.http.get<any[]>(`${this.apiUrl}/recentposts/${ticker}?timeframe=${timeframe}`);
  }

  getTrendingStocks(timeframe: string = '24h'): Observable<{ [key: string]: number }> { //add timeframe parameter.
    return this.http.get<{ [key: string]: number }>(`${this.apiUrl}/trending?timeframe=${timeframe}`);
  }
}


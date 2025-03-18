import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs/internal/Observable';
import { Stock } from '../model/stock';
import { StockPrice } from '../model/stockPrice';

@Injectable({
  providedIn: 'root'
})
export class StockService {
  private apiUrl = 'http://localhost:8080/api';

  private http = inject(HttpClient);

  getTrendingStocks() {
    return this.http.get<{ [key: string]: number }>(`${this.apiUrl}/trending`);
  }

  getStockDetails(ticker: string): Observable<StockPrice> {
    return this.http.get<StockPrice>(`${this.apiUrl}/stock/${ticker}`);
  }

  getRecentPosts(ticker: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/recentposts/${ticker}`);
  }
}


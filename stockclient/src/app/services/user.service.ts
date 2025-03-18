import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { BehaviorSubject, firstValueFrom } from 'rxjs';
import { User } from '../model/user';


@Injectable({
  providedIn: 'root'
})
export class UserService {
  private userUrl = 'http://localhost:8080/api';
  private http = inject(HttpClient);

  private loggedIn = new BehaviorSubject<boolean>(false);
  private userDetails = new BehaviorSubject<User | null>(null);

  public loginStatus$ = this.loggedIn.asObservable();
  public currentUser$ = this.userDetails.asObservable();

  async login(email: string, password: string): Promise<void> {
    try {
      // Use firstValueFrom instead of toPromise()
      await firstValueFrom(this.http.post(`${this.userUrl}/login`, { email, password }, { responseType: 'text' }));

      // If login is successful, fetch user details
      const user = await this.getUserByEmail(email);
      this.loggedIn.next(true);
      this.userDetails.next(user);
    } catch (error) {
      console.error("Login error:", error);
      this.loggedIn.next(false);
      this.userDetails.next(null);
      throw error; //Re-throw error
    }
  }

  logout() {
    this.loggedIn.next(false);
    this.userDetails.next(null);
  }

  async register(email: string, password: string): Promise<void> {
    await firstValueFrom(this.http.post(`${this.userUrl}/register`, null, {
      params: { email, password },
      responseType: 'text'
    }));
  }

  async getUserByEmail(email: string): Promise<User> {
    return await firstValueFrom(this.http.get<User>(`${this.userUrl}/user/${email}`)) as User;
  }

  async addToWatchlist(email: string, ticker: string): Promise<void> {
    const currentUser = this.userDetails.value;
    if (!currentUser) {
      throw new Error('User not logged in.');
    }

    const updatedWatchlist = new Set(currentUser.watchlist);
    updatedWatchlist.add(ticker);
    //Use userDetails
    this.userDetails.next({ ...currentUser, watchlist: updatedWatchlist });

    try {
      await firstValueFrom(this.http.post(`${this.userUrl}/user/watchlist/add`, null, {
        params: { email, ticker },
        responseType: 'text'
      }));
    } catch (error) {
        this.userDetails.next(currentUser); // Revert
        console.error("Error adding to watchlist:", error);
        throw error; //Re-throw error
    }
  }

  async removeFromWatchlist(email: string, ticker: string): Promise<void> {
    const currentUser = this.userDetails.value;
    if (!currentUser) {
      throw new Error('User not logged in.');
    }

    const updatedWatchlist = new Set(currentUser.watchlist);
    updatedWatchlist.delete(ticker);
    //Use userDetails
    this.userDetails.next({ ...currentUser, watchlist: updatedWatchlist });

    try {
      await firstValueFrom(this.http.post(`${this.userUrl}/user/watchlist/remove`, null, {
        params: { email, ticker },
        responseType: 'text'
      }));
    } catch (error) {
        this.userDetails.next(currentUser); //Revert
        console.error("Error removing from watchlist:", error);
        throw error;//Re-throw error
    }
  }
}
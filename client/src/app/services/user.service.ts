import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { BehaviorSubject, firstValueFrom } from 'rxjs';
import { User, UserState } from '../model/user';
import { UserStore } from '../user.store';


@Injectable({providedIn: 'root'})
export class UserService {
  private userUrl = '/api';
  private http = inject(HttpClient);
  private userStore = inject(UserStore);
  
  readonly currentUser = this.userStore.currentUser$;
  readonly loginStatus = this.userStore.isLoggedIn$;

  async login(email: string, password: string): Promise<void> {
    try {
      this.userStore.setLoading(true);
      this.userStore.setError(null);
      await firstValueFrom(this.http.post(`${this.userUrl}/login`, { email, password }, { responseType: 'text' }));

      // If login is successful, fetch user details
      const user = await this.getUserByEmail(email);
      this.userStore.setCurrentUser(user);
      this.userStore.setLoggedIn(true);
    } catch (error: any) {
      this.userStore.setError(error.message || 'Login failed');
      this.userStore.setLoggedIn(false);
      throw error;
    } finally {
      this.userStore.setLoading(false);
    }
  }

  logout() {
    this.userStore.setCurrentUser(null);
    this.userStore.setLoggedIn(false);
  }

  async register(email: string, password: string): Promise<void> {
    try {
      this.userStore.setLoading(true);
      this.userStore.setError(null);
      
      await firstValueFrom(this.http.post(`${this.userUrl}/register`, null, {
        params: { email, password },
        responseType: 'text'
      }));
    } catch (error: any) {
      this.userStore.setError(error.message || 'Registration failed');
      throw error;
    } finally {
      this.userStore.setLoading(false);
    }
  }


  async getUserByEmail(email: string): Promise<User> {
    return await firstValueFrom(this.http.get<User>(`${this.userUrl}/user/${email}`)) as User;
  }

  async addToWatchlist(email: string, ticker: string): Promise<void> {
    // Get current user from the store
    const currentUser = await firstValueFrom(this.userStore.currentUser$);
    if (!currentUser) {
      throw new Error('User not logged in.');
    }

    const updatedWatchlist = new Set(currentUser.watchlist);
    updatedWatchlist.add(ticker);
    
    this.userStore.setCurrentUser({ ...currentUser, watchlist: updatedWatchlist });

    try {
      await firstValueFrom(this.http.post(`${this.userUrl}/user/watchlist/add`, null, {
        params: { email, ticker },
        responseType: 'text'
      }));
    } catch (error) {
      this.userStore.setCurrentUser(currentUser);
      console.error("Error adding to watchlist:", error);
      throw error;
    }
  }

  async removeFromWatchlist(email: string, ticker: string): Promise<void> {
    // Get current user from the store
    const currentUser = await firstValueFrom(this.userStore.currentUser$);
    if (!currentUser) {
      throw new Error('User not logged in.');
    }
    // Create a new watchlist minus
    const updatedWatchlist = new Set(currentUser.watchlist);
    updatedWatchlist.delete(ticker);
    
    this.userStore.setCurrentUser({ ...currentUser, watchlist: updatedWatchlist });

    try {
      await firstValueFrom(this.http.post(`${this.userUrl}/user/watchlist/remove`, null, {
        params: { email, ticker },
        responseType: 'text'
      }));
    } catch (error) {
      this.userStore.setCurrentUser(currentUser);
      console.error("Error removing from watchlist:", error);
      throw error;
    }
  }
}
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { BehaviorSubject, firstValueFrom, from, Observable } from 'rxjs';
import { PriceAlert, User, UserState } from '../model/user';
import { UserStore } from '../user.store';
import { AngularFireAuth } from '@angular/fire/compat/auth';
import { tap, switchMap, catchError } from 'rxjs/operators';

@Injectable({providedIn: 'root'})
export class UserService {
  private userUrl = '/api';
  private http = inject(HttpClient);
  private userStore = inject(UserStore);
  private auth = inject(AngularFireAuth);
  
  readonly currentUser = this.userStore.currentUser$;
  readonly loginStatus = this.userStore.isLoggedIn$;

  constructor() {
    // Listen to Firebase auth state changes
    this.auth.authState.subscribe(async (firebaseUser) => {
      if (firebaseUser) {
        try {
          // Get Firebase token
          const token = await firebaseUser.getIdToken();
          
          // Get user data from backend
          const user = await this.getUserByEmail(firebaseUser.email || '');
          this.userStore.setCurrentUser(user);
          this.userStore.setLoggedIn(true);
        } catch (error) {
          console.error('Error getting user data:', error);
          this.userStore.setLoggedIn(false);
        }
      } else {
        this.userStore.setCurrentUser(null);
        this.userStore.setLoggedIn(false);
      }
    });
  }

  async login(email: string, password: string): Promise<void> {
    try {
      this.userStore.setLoading(true);
      this.userStore.setError(null);
      
      // Firebase authentication
      await this.auth.signInWithEmailAndPassword(email, password);
    
      const user = await this.getUserByEmail(email);
      this.userStore.setCurrentUser(user);
      this.userStore.setLoggedIn(true);
    } catch (error: any) {
      let errorMessage = 'Login failed';
      if (error.code) {
        switch (error.code) {
          case 'auth/invalid-email':
            errorMessage = 'Invalid email format';
            break;
          case 'auth/user-disabled':
            errorMessage = 'This account has been disabled';
            break;
          case 'auth/user-not-found':
            errorMessage = 'User not found';
            break;
          case 'auth/wrong-password':
            errorMessage = 'Incorrect password';
            break;
        }
      }
      this.userStore.setError(errorMessage);
      this.userStore.setLoggedIn(false);
      throw error;
    } finally {
      this.userStore.setLoading(false);
    }
  }

  async logout(): Promise<void> {
    try {
      await this.auth.signOut();
      this.userStore.setCurrentUser(null);
      this.userStore.setLoggedIn(false);
    } catch (error) {
      console.error('Error signing out:', error);
    }
  }

  async register(email: string, password: string): Promise<void> {
    try {
      this.userStore.setLoading(true);
      this.userStore.setError(null);
      
      // Firebase authentication
      const userCredential = await this.auth.createUserWithEmailAndPassword(email, password);
      
      // Get Firebase token
      const token = await userCredential.user?.getIdToken();
      console.log("token: " + token);
      const headers = new HttpHeaders().set('Authorization', `Bearer ${token}`);
      
      await firstValueFrom(this.http.post(`${this.userUrl}/register`, null, {
        params: { email, password },
        headers: headers,
        responseType: 'text'
      }));
    } catch (error: any) {
      let errorMessage = 'Registration failed';
      if (error.code) {
        switch (error.code) {
          case 'auth/email-already-in-use':
            errorMessage = 'Email already in use';
            break;
          case 'auth/invalid-email':
            errorMessage = 'Invalid email format';
            break;
          case 'auth/weak-password':
            errorMessage = 'Password is too weak';
            break;
        }
      }
      this.userStore.setError(errorMessage);
      throw error;
    } finally {
      this.userStore.setLoading(false);
    }
  }

  getCurrentFirebaseUser(): Observable<firebase.default.User | null> {
    return this.auth.authState;
  }

  private async getAuthHeaders(): Promise<HttpHeaders> {
    const user = await firstValueFrom(this.auth.authState);
    if (!user) {
      throw new Error('User not authenticated');
    }
    
    const token = await user.getIdToken();
    return new HttpHeaders().set('Authorization', `Bearer ${token}`);
  }

  async getUserByEmail(email: string): Promise<User> {
    const headers = await this.getAuthHeaders();
    return await firstValueFrom(
      this.http.get<User>(`${this.userUrl}/user/${email}`, { headers })
    ) as User;
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
      const headers = await this.getAuthHeaders();
      await firstValueFrom(this.http.post(`${this.userUrl}/user/watchlist/add`, null, {
        params: { email, ticker },
        headers,
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
      const headers = await this.getAuthHeaders();
      await firstValueFrom(this.http.post(`${this.userUrl}/user/watchlist/remove`, null, {
        params: { email, ticker },
        headers,
        responseType: 'text'
      }));
    } catch (error) {
      this.userStore.setCurrentUser(currentUser);
      console.error("Error removing from watchlist:", error);
      throw error;
    }
  }

  async setPriceAlert(email: string, ticker: string, targetPrice: number, condition: 'above' | 'below'): Promise<void> {
    // Get current user from the store
    const currentUser = await firstValueFrom(this.userStore.currentUser$);
    if (!currentUser) {
      throw new Error('User not logged in.');
    }

    // Create the alert
    const alert: PriceAlert = {
      ticker,
      targetPrice,
      condition: condition || 'above',
      createdAt: Date.now()
    };

    // Update local store
    const updatedAlerts = { ...(currentUser.priceAlerts || {}) };
    updatedAlerts[ticker] = alert;
    
    this.userStore.setCurrentUser({ 
      ...currentUser, 
      priceAlerts: updatedAlerts 
    });

    try {
      const headers = await this.getAuthHeaders();
      await firstValueFrom(this.http.post(`${this.userUrl}/user/price-alert/set`, {
        ticker,
        targetPrice,
        condition
      }, {
        params: { email },
        headers,
        responseType: 'text'
      }));
    } catch (error) {
      // Rollback on error
      this.userStore.setCurrentUser(currentUser);
      console.error("Error setting price alert:", error);
      throw error;
    }
  }

  async removePriceAlert(email: string, ticker: string): Promise<void> {
    // Get current user from the store
    const currentUser = await firstValueFrom(this.userStore.currentUser$);
    if (!currentUser) {
      throw new Error('User not logged in.');
    }

    // Remove from local state
    if (!currentUser.priceAlerts || !currentUser.priceAlerts[ticker]) {
      return; // Nothing to remove
    }

    const updatedAlerts = { ...currentUser.priceAlerts };
    delete updatedAlerts[ticker];
    
    this.userStore.setCurrentUser({ 
      ...currentUser, 
      priceAlerts: updatedAlerts 
    });

    try {
      const headers = await this.getAuthHeaders();
      await firstValueFrom(this.http.post(`${this.userUrl}/user/price-alert/remove`, null, {
        params: { email, ticker },
        headers,
        responseType: 'text'
      }));
    } catch (error) {
      // Rollback on error
      this.userStore.setCurrentUser(currentUser);
      console.error("Error removing price alert:", error);
      throw error;
    }
  }

  async updateFCMToken(email: string, token: string): Promise<void> {
    try {
      const headers = await this.getAuthHeaders();
      await firstValueFrom(this.http.post(`${this.userUrl}/user/fcm-token`, { token }, {
        params: { email },
        headers,
        responseType: 'text'
      }));
      console.log('FCM token saved on server');
    } catch (error) {
      console.error('Error saving FCM token on server:', error);
      throw error;
    }
  }
}
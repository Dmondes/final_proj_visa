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
      
      // Get Firebase token
      const currentUser = await firstValueFrom(this.auth.authState);
      if (!currentUser) {
        throw new Error('Login failed: Unable to get Firebase user');
      }
      
      console.log("Firebase login successful for: " + email);
      
      try {
        const user = await this.getUserByEmail(email);
        this.userStore.setCurrentUser(user);
        this.userStore.setLoggedIn(true);
        console.log("Backend user data retrieved successfully");
      } catch (backendError: any) {
        console.error("Error getting user data from backend:", backendError);
        
        // If the user doesn't exist in the backend but does in Firebase,
        // we should create the user in the backend
        if (backendError.status === 404) {
          console.log("User exists in Firebase but not in backend, attempting to register");
          
          try {
            // Get Firebase token
            const token = await currentUser.getIdToken();
            const headers = new HttpHeaders().set('Authorization', `Bearer ${token}`);
            
            // Register the user in the backend
            await firstValueFrom(this.http.post(`${this.userUrl}/register`, 
              { email, password },
              {
                headers: headers,
                responseType: 'text'
              }
            ));
            
            // Try to get user data again
            const user = await this.getUserByEmail(email);
            this.userStore.setCurrentUser(user);
            this.userStore.setLoggedIn(true);
          } catch (registerError) {
            console.error("Error registering user in backend:", registerError);
            throw new Error('Login successful in Firebase but failed to create user in backend');
          }
        } else {
          throw backendError;
        }
      }
    } catch (error: any) {
      console.error("Login error:", error);
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
      } else if (error.message) {
        errorMessage = error.message;
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
      
      // Send email and password in request body instead of query parameters
      await firstValueFrom(this.http.post(`${this.userUrl}/register`, 
        { email, password }, // Email and password in request body
        {
          headers: headers,
          responseType: 'text'
        }
      ));
      
      console.log("Registration successful");

    } catch (error: any) {
      console.error("Registration error:", error);
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
    try {
      const user = await firstValueFrom(this.auth.authState);
      if (!user) {
        console.error('No authenticated user found');
        throw new Error('User not authenticated');
      }
      
      try {
        const token = await user.getIdToken(true); // Force token refresh
        return new HttpHeaders().set('Authorization', `Bearer ${token}`);
      } catch (tokenError) {
        console.error('Error getting authentication token:', tokenError);
        throw new Error('Failed to get authentication token');
      }
    } catch (error) {
      console.error('Authentication error:', error);
      throw error;
    }
  }

  async getUserByEmail(email: string): Promise<User> {
    try {
      console.log(`Fetching user data for: ${email}`);
      const headers = await this.getAuthHeaders();
      
      return await firstValueFrom(
        this.http.get<User>(`${this.userUrl}/user/${email}`, { headers })
          .pipe(
            tap(user => {
              console.log(`Successfully retrieved data for user: ${email}`);
            }),
            catchError(error => {
              console.error(`Error getting user data for ${email}:`, error);
              
              if (error.status === 500) {
                console.error('Server error (500). This could be due to database connection issues or server configuration.');
              } else if (error.status === 401) {
                console.error('Authentication error (401). Token may be invalid or expired.');
                // Force logout on authentication error
                this.userStore.setLoggedIn(false);
                this.userStore.setCurrentUser(null);
              } else if (error.status === 404) {
                console.error(`User not found in backend database: ${email}`);
              }
              
              throw error;
            })
          )
      ) as User;
    } catch (error) {
      console.error('Failed to get user data:', error);
      throw error;
    }
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
import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { UserService } from '../../services/user.service';
import { UserStore } from '../../user.store';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-watchlist',
  standalone: false,
  templateUrl: './watchlist.component.html',
  styleUrl: './watchlist.component.css'
})
export class WatchlistComponent implements OnInit {

  private userService = inject(UserService);
  private userStore = inject(UserStore);
  
  user = this.userStore.currentUser$;
  watchlist = this.userStore.watchlist$;

  ngOnInit(): void {
  }

  async removeFromWatchlist(ticker: string) {
    const user = await firstValueFrom(this.userStore.currentUser$);
    if (user) {
      try {
        await this.userService.removeFromWatchlist(user.email, ticker);
        // No need to update manually - the userService already updates the state
      } catch (error) {
        console.error(`Error removing ${ticker} from watchlist:`, error);
        alert(`Error removing ${ticker} from watchlist. Please try again.`);
      }
    }
  }
}

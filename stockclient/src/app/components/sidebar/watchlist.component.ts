import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { UserService } from '../../services/user.service';
import { Subscription } from 'rxjs';
import { User } from '../../model/user';

@Component({
  selector: 'app-watchlist',
  standalone: false,
  templateUrl: './watchlist.component.html',
  styleUrl: './watchlist.component.css'
})
export class WatchlistComponent implements OnInit, OnDestroy {


  user: User | null = null;
  sub!: Subscription;

  private userService = inject(UserService);

  ngOnInit(): void {
    this.sub = this.userService.currentUser$.subscribe(user => {
      this.user = user;
    });
  }

  ngOnDestroy(): void {
      this.sub?.unsubscribe();
  }

  async removeFromWatchlist(ticker: string) {
    if (this.user) {
        try{
            await this.userService.removeFromWatchlist(this.user.email, ticker);
            alert(`${ticker} removed from watchlist!`);
        }catch(error){
            console.error(`Error removing ${ticker} from watchlist:`, error);
            alert(`Error removing ${ticker} from watchlist. Please try again.`);
        }
    }
  }
}

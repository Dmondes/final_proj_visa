import { Component, inject, OnDestroy, OnInit, ViewEncapsulation } from '@angular/core';
import { StockService } from '../../services/stock.service';
import { Subscription } from 'rxjs';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { UserService } from '../../services/user.service';

@Component({
  selector: 'app-navbar',
  standalone: false,
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.css',
  encapsulation: ViewEncapsulation.None

})
export class NavbarComponent implements OnInit, OnDestroy {

  ticker !: string;
  loginStatus !: boolean;
  sub!: Subscription;
  searchForm !: FormGroup;

  private stockService = inject(StockService);
  private userService = inject(UserService);
  private router = inject(Router);
  private fb = inject(FormBuilder);

  ngOnInit(): void {
    this.searchForm = this.fb.group({
      ticker: ['', [Validators.required, Validators.minLength(1)]]
    });
    this.sub = this.userService.loginStatus$.subscribe(status => {
      this.loginStatus = status;
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  search() {
    if (this.searchForm.valid) {
      this.ticker = this.searchForm.get('ticker')?.value;
      this.router.navigate(['/stock', this.ticker.toUpperCase()]);
      this.ticker = '';
      this.searchForm.reset();
    }
  }

  logout() {
    this.userService.logout();
    this.router.navigate(['/home']);
  }


}

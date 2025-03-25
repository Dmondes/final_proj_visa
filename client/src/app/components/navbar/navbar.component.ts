import { Component, inject, OnDestroy, OnInit, ViewEncapsulation } from '@angular/core';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { UserService } from '../../services/user.service';
import { UserStore } from '../../user.store';
import { Subject, takeUntil } from 'rxjs';

@Component({
  selector: 'app-navbar',
  standalone: false,
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.css',
  encapsulation: ViewEncapsulation.None
})
export class NavbarComponent implements OnInit, OnDestroy {

  ticker!: string;
  searchForm!: FormGroup;
  private destroy$ = new Subject<void>();

  private userService = inject(UserService);
  private userStore = inject(UserStore);
  private router = inject(Router);
  private fb = inject(FormBuilder);

  loginStatus = this.userStore.isLoggedIn$;
  userEmail = '';

  ngOnInit(): void {
    this.searchForm = this.fb.group({
      ticker: ['', [Validators.required, Validators.minLength(1)]]
    });

    // Get current user email for display
    this.userStore.currentUser$
      .pipe(takeUntil(this.destroy$))
      .subscribe(user => {
        this.userEmail = user?.email || '';
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  search() {
    if (this.searchForm.valid) {
      this.ticker = this.searchForm.get('ticker')?.value;
      this.router.navigate(['/stock', this.ticker.toUpperCase()]);
      this.ticker = '';
      this.searchForm.reset();
    }
  }

  async logout() {
    try {
      await this.userService.logout();
      this.router.navigate(['/home']);
    } catch (error) {
      console.error('Logout error:', error);
    }
  }
}
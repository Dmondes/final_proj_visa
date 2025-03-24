import { Component, inject, OnDestroy, OnInit, ViewEncapsulation } from '@angular/core';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { UserService } from '../../services/user.service';
import { UserStore } from '../../user.store';

@Component({
  selector: 'app-navbar',
  standalone: false,
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.css',
  encapsulation: ViewEncapsulation.None

})
export class NavbarComponent implements OnInit {

  ticker !: string;
  searchForm !: FormGroup;


  private userService = inject(UserService);
  private userStore = inject(UserStore);
  private router = inject(Router);
  private fb = inject(FormBuilder);

  loginStatus = this.userStore.isLoggedIn$;

  ngOnInit(): void {
    this.searchForm = this.fb.group({
      ticker: ['', [Validators.required, Validators.minLength(1)]]
    });

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
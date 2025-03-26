import { Component, inject, OnInit } from '@angular/core';
import { FormGroup, FormControl, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { UserService } from '../../services/user.service';
import { UserStore } from '../../user.store';

@Component({
  selector: 'app-login',
  standalone: false,
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent implements OnInit {

  userForm!: FormGroup;
  error!: string;
  isLoading = false;

  private userService = inject(UserService);
  private userStore = inject(UserStore);
  private router = inject(Router);

  ngOnInit(): void {
    this.userForm = new FormGroup({
      email: new FormControl('', [Validators.required, Validators.email]),
      password: new FormControl('', [Validators.required, Validators.minLength(6)])
    });

    this.userStore.error$.subscribe(error => {
      this.error = error || '';
    });

    this.userStore.isLoading$.subscribe(isLoading => {
      this.isLoading = isLoading;
    });

    this.userStore.isLoggedIn$.subscribe(isLoggedIn => {
      if (isLoggedIn) {
        this.router.navigate(['/watchlist']);
      }
    });
  }

  async processForm() {
    if (this.userForm.invalid) return;
    const { email, password } = this.userForm.value;
    try {
      await this.userService.login(email, password);
      this.router.navigate(['/watchlist']);
    } catch (err: any) {
      // Error is handled by the store subscription
    }
  }
}
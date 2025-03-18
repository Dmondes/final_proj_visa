import { Component, inject, OnInit } from '@angular/core';
import { FormGroup, FormControl, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { UserService } from '../../services/user.service';

@Component({
  selector: 'app-login',
  standalone: false,
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent implements OnInit {

  userForm!: FormGroup;
  error !: string;

  private userService = inject(UserService);
  private router = inject(Router);

  ngOnInit(): void {
    this.userForm = new FormGroup({
      email: new FormControl('', [Validators.required, Validators.email]),
      password: new FormControl('', [Validators.required, Validators.minLength(6)])
    });
  }

  async processForm() {
    if (this.userForm.invalid) return;
    const { email, password } = this.userForm.value;
    try {
      await this.userService.login(email, password);
      this.router.navigate(['/watchlist']);
    } catch (err: any) {
      this.error = err.message || err.error || err.statusText || 'Login failed';
    }
  }
}


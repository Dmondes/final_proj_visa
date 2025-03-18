import { Component, inject, OnInit } from '@angular/core';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { UserService } from '../../services/user.service';

@Component({
  selector: 'app-register',
  standalone: false,
  templateUrl: './register.component.html',
  styleUrl: './register.component.css'
})
export class RegisterComponent implements OnInit {
 
  registerForm!: FormGroup;
  error !: string;
  success !: string;

  private fb = inject(FormBuilder);
  private userService = inject(UserService);
  private router = inject(Router);

  ngOnInit(): void {
    this.registerForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', Validators.required]
    }, { validator: this.passwordMatchValidator });
  }

  passwordMatchValidator(form: FormGroup) {
    return form.get('password')?.value === form.get('confirmPassword')?.value 
      ? null : { mismatch: true };
  }

  async processRegistration() {
    if (this.registerForm.invalid) return;

    const { email, password } = this.registerForm.value;
    try{
        await this.userService.register(email, password);
        this.success = 'Registration successful!';
        this.router.navigate(['/login']);
    }catch(err: any){
        this.error = err.error || 'Registration failed';
    }
  }
}

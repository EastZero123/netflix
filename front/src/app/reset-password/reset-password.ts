import {Component, OnInit} from '@angular/core';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';
import {ActivatedRoute, Router} from '@angular/router';
import {AuthService} from '../shared/services/auth-service';
import {NotificationService} from '../shared/services/notification-service';
import {submit} from '@angular/forms/signals';

@Component({
  selector: 'app-reset-password',
  standalone: false,
  templateUrl: './reset-password.html',
  styleUrl: './reset-password.css',
})
export class ResetPassword implements OnInit {
  resetPasswordForm! : FormGroup;
  loading = false;
  tokenValid = false;
  token = '';
  hidePassword = true;
  hideConfirmPassword = true;

  constructor(
    private fb : FormBuilder,
    private route : ActivatedRoute,
    private router : Router,
    private authService: AuthService,
    private notification : NotificationService
  ) {
    this.resetPasswordForm = fb.group({
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required, this.authService.passwordMatchValidator('password')]],
    })
  }

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');
    if(token) {
      this.token = token;
      this.tokenValid = true;
    }
    else {
      this.tokenValid = false;
    }
  }

  onSubmit() {
    this.loading = true;
    const newPassword = this.resetPasswordForm.value.password;

    this.authService.resetPassword(this.token, newPassword).subscribe({
      next:(response:any) => {
        this.loading = false;
        this.notification.success(response.message || 'Password reset successfully');
        this.router.navigate(['/login']);
      },
      error:(err) => {
        this.loading = false;
        const errorMsg = err.error?.error || 'failed to reset password';

        if(errorMsg.toLowerCase().includes('expired') || errorMsg.toLowerCase().includes('invalid')) {
          this.tokenValid = false;
        } else {
          this.notification.error(errorMsg);
        }
      }
    })
  }

  protected readonly submit = submit;
}

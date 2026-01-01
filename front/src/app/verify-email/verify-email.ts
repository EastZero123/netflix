import {ChangeDetectorRef, Component, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {AuthService} from '../shared/services/auth-service';

@Component({
  selector: 'app-verify-email',
  standalone: false,
  templateUrl: './verify-email.html',
  styleUrl: './verify-email.css',
})
export class VerifyEmail implements OnInit {
  loading = true;
  success = false;
  message = '';

  constructor(
    private route: ActivatedRoute,
    private authService: AuthService,
    private cdr : ChangeDetectorRef
  ) {
  }

  ngOnInit():void {
    const token = this.route.snapshot.queryParamMap.get('token');

    if(!token) {
      this.loading = false;
      this.success = false;
      this.message = 'Invalid verification link';
      return;
    }

    this.authService.verifyEmail(token).subscribe({
      next: (response: any) => {
        this.loading = false;
        this.success = true;
        this.message = response.message || 'Email verified successfully';

        this.cdr.detectChanges();
      },
      error: (err) => {
        this.loading = false;
        this.success = false;
        this.message = err.error?.error || 'Verification failed';

        this.cdr.detectChanges();
      }
    })
  }
}

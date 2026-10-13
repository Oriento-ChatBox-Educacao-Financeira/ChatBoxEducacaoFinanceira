import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';

/**
 * P\u00e1gina exibida ap\u00f3s cadastro bem-sucedido. Como o backend Java n\u00e3o
 * exige confirma\u00e7\u00e3o por e-mail (diferente do Supabase Auth), s\u00f3
 * confirmamos a cria\u00e7\u00e3o e direcionamos o usu\u00e1rio para o login.
 */
@Component({
  selector: 'app-register-success',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './register-success.html',
  styleUrls: ['./register-success.css'],
})
export class RegisterSuccess implements OnInit {
  email = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.email = this.route.snapshot.queryParamMap.get('email') ?? '';
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }
}

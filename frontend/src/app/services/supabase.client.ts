import { Injectable } from '@angular/core';
import { createClient, SupabaseClient } from '@supabase/supabase-js';
import { environment } from '../../environments/environment';

interface RuntimeSupabaseConfig {
  url?: string;
  anonKey?: string;
}

declare global {
  interface Window {
    __ORIENTO_SUPABASE_CONFIG__?: RuntimeSupabaseConfig;
  }
}

@Injectable({ providedIn: 'root' })
export class SupabaseClientService {
  readonly client: SupabaseClient;

  constructor() {
    // Prioriza configuração injetada em runtime (ex.: <script> em index.html
    // populado pelo deploy a partir de env vars) e cai no bundle só quando
    // ausente. Isso evita acoplar o build a um projeto Supabase específico.
    const runtime = typeof window !== 'undefined' ? window.__ORIENTO_SUPABASE_CONFIG__ : undefined;
    const url = runtime?.url ?? environment.supabase.url;
    const anonKey = runtime?.anonKey ?? environment.supabase.anonKey;

    this.client = createClient(url, anonKey, {
      auth: {
        autoRefreshToken: true,
        persistSession: true,
        detectSessionInUrl: true,
      },
    });
  }
}

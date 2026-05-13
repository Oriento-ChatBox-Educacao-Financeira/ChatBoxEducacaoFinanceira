// Fallback de configuracao. Em producao, prefira injetar URL/anonKey via
// `window.__ORIENTO_SUPABASE_CONFIG__` no index.html (substituido pelo pipeline
// de deploy a partir de env vars). Os valores abaixo so sao usados se o
// placeholder do index nao for resolvido.
export const environment = {
  production: true,
  apiUrl: 'https://api.oriento.ai/api',
  authUrl: 'https://api.oriento.ai/api/auth',
  geminiUrl: 'https://api.oriento.ai/api/oriento/ask',
  supabase: {
    url: 'https://gchjfmvajatdbefjycjh.supabase.co',
    anonKey:
      'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImdjaGpmbXZhamF0ZGJlZmp5Y2poIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Nzg1OTYwNDYsImV4cCI6MjA5NDE3MjA0Nn0.IrZuPm_RKtdYOK9kbpW8h_1XettGKTiFzbP3MbJ8E34',
  },
};

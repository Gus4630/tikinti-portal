export default defineNuxtConfig({
  modules: ['@nuxt/ui', '@pinia/nuxt', '@nuxt/eslint'],

  devtools: { enabled: true },

  css: ['~/assets/css/main.css'],

  compatibilityDate: '2025-01-15',

  future: { compatibilityVersion: 4 },

  router: {
    middleware: ['auth'],
  },

  runtimeConfig: {
    public: {
      apiBase: 'http://localhost:8080',
    },
  },

  app: {
    head: {
      title: 'Tikinti Portal',
      htmlAttrs: { lang: 'az' },
      link: [
        { rel: 'preconnect', href: 'https://fonts.googleapis.com' },
        { rel: 'preconnect', href: 'https://fonts.gstatic.com', crossorigin: '' },
        {
          rel: 'stylesheet',
          href: 'https://fonts.googleapis.com/css2?family=Inter:ital,opsz,wght@0,14..32,400;0,14..32,500;0,14..32,600;0,14..32,700;1,14..32,400&family=Geist+Mono:wght@400;500;600&display=swap',
        },
      ],
    },
  },

  eslint: {
    config: {
      stylistic: { commaDangle: 'never', braceStyle: '1tbs' },
    },
  },
})

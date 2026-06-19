interface AuthUser {
  username: string
  fullName: string | null
  email: string
  role: string
}

interface AuthTokens {
  accessToken: string
  refreshToken: string
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(null)
  const refreshToken = ref<string | null>(null)
  const user = ref<AuthUser | null>(null)

  const isAuthenticated = computed(() => !!token.value)

  const initials = computed(() => {
    if (!user.value) return '??'
    const name = user.value.fullName || user.value.username
    return name.split(' ').map((w: string) => w[0]).join('').toUpperCase().slice(0, 2)
  })

  const displayName = computed(() => {
    if (!user.value) return ''
    return user.value.fullName || user.value.username
  })

  function setTokens(tokens: AuthTokens) {
    token.value = tokens.accessToken
    refreshToken.value = tokens.refreshToken
    if (import.meta.client) {
      localStorage.setItem('tk_access', tokens.accessToken)
      localStorage.setItem('tk_refresh', tokens.refreshToken)
    }
  }

  function setUser(u: AuthUser) {
    user.value = u
  }

  function logout() {
    token.value = null
    refreshToken.value = null
    user.value = null
    if (import.meta.client) {
      localStorage.removeItem('tk_access')
      localStorage.removeItem('tk_refresh')
    }
  }

  function hydrate() {
    if (import.meta.client) {
      const stored = localStorage.getItem('tk_access')
      const storedRefresh = localStorage.getItem('tk_refresh')
      if (stored) token.value = stored
      if (storedRefresh) refreshToken.value = storedRefresh
    }
  }

  return { token, refreshToken, user, isAuthenticated, initials, displayName, setTokens, setUser, logout, hydrate }
})

<script setup lang="ts">
definePageMeta({ layout: false })

const config = useRuntimeConfig()
const auth = useAuthStore()
const router = useRouter()

const form = reactive({ username: '', password: '', remember: true })
const loading = ref(false)
const error = ref('')

onMounted(() => {
  if (auth.isAuthenticated) router.replace('/dashboard')
})

async function login() {
  error.value = ''
  loading.value = true
  try {
    const res = await $fetch<{ accessToken: string; refreshToken: string }>('/api/v1/auth/login', {
      baseURL: config.public.apiBase,
      method: 'POST',
      body: { username: form.username, password: form.password, deviceLabel: 'web' },
    })
    auth.setTokens({ accessToken: res.accessToken, refreshToken: res.refreshToken })
    await router.replace('/dashboard')
  } catch (e: unknown) {
    error.value = 'İstifadəçi adı və ya şifrə yanlışdır.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div style="display:flex;height:100vh;width:100%">
    <!-- Left panel -->
    <div style="flex:0 0 46%;background:#0D1440;color:#fff;padding:56px;display:flex;flex-direction:column;justify-content:space-between;position:relative;overflow:hidden">
      <div style="position:absolute;right:-80px;top:-80px;width:320px;height:320px;border-radius:9999px;background:rgba(61,90,241,0.18)" />
      <div style="position:absolute;right:60px;bottom:-120px;width:260px;height:260px;border-radius:9999px;background:rgba(245,158,11,0.10)" />

      <div style="display:flex;align-items:center;gap:11px;position:relative">
        <div style="width:34px;height:34px;border-radius:8px;background:#F59E0B;display:flex;align-items:center;justify-content:center;color:#0D1440;font-weight:700;font-size:16px">T</div>
        <span style="font-size:19px;font-weight:700;letter-spacing:-0.01em">Tikinti</span>
      </div>

      <div style="position:relative">
        <h1 style="font-size:38px;line-height:1.15;font-weight:700;letter-spacing:-0.02em;margin:0 0 18px">
          Tikinti xərclərini bir mərkəzdən idarə edin.
        </h1>
        <p style="font-size:16px;line-height:1.6;color:#B7C0E8;margin:0;max-width:420px">
          Qaimə-fakturaların OCR ilə emalı, büdcə nəzarəti və təchizatçı hesablaşmaları — bütün layihələriniz üçün vahid portal.
        </p>
        <div style="display:flex;gap:30px;margin-top:38px">
          <div>
            <div style="font-family:var(--font-mono);font-size:26px;font-weight:600;color:#F59E0B">₼8.4M</div>
            <div style="font-size:13px;color:#9098C9;margin-top:2px">idarə olunan xərc</div>
          </div>
          <div>
            <div style="font-family:var(--font-mono);font-size:26px;font-weight:600">6</div>
            <div style="font-size:13px;color:#9098C9;margin-top:2px">aktiv layihə</div>
          </div>
          <div>
            <div style="font-family:var(--font-mono);font-size:26px;font-weight:600">7</div>
            <div style="font-size:13px;color:#9098C9;margin-top:2px">təchizatçı</div>
          </div>
        </div>
      </div>

      <div style="font-size:13px;color:#6B73A6">© 2026 Tikinti · Bütün hüquqlar qorunur</div>
    </div>

    <!-- Right panel -->
    <div style="flex:1;display:flex;align-items:center;justify-content:center;padding:40px;background:#fff">
      <div style="width:100%;max-width:380px">
        <h2 style="font-size:24px;font-weight:700;letter-spacing:-0.01em;margin:0 0 6px">Portala daxil olun</h2>
        <p style="font-size:14px;color:#6B7280;margin:0 0 30px">Hesabınızla davam edin</p>

        <div v-if="error" style="background:#FEF2F2;border:1px solid #FCA5A5;border-radius:8px;padding:11px 14px;font-size:13.5px;color:#DC2626;margin-bottom:18px">
          {{ error }}
        </div>

        <form @submit.prevent="login">
          <label style="display:block;font-size:13px;font-weight:500;color:#374151;margin-bottom:7px">İstifadəçi adı</label>
          <div style="position:relative;margin-bottom:18px">
            <UIcon name="i-lucide-user" style="position:absolute;left:13px;top:50%;transform:translateY(-50%);color:#9CA3AF;width:16px;height:16px" />
            <input
              v-model="form.username"
              type="text"
              autocomplete="username"
              style="width:100%;height:44px;border:1px solid #D1D5DB;border-radius:8px;padding:0 14px 0 40px;font-size:15px;color:#0A0A0A;outline:none;font-family:inherit;transition:border-color .15s,box-shadow .15s"
            >
          </div>

          <label style="display:block;font-size:13px;font-weight:500;color:#374151;margin-bottom:7px">Şifrə</label>
          <div style="position:relative;margin-bottom:14px">
            <UIcon name="i-lucide-lock" style="position:absolute;left:13px;top:50%;transform:translateY(-50%);color:#9CA3AF;width:16px;height:16px" />
            <input
              v-model="form.password"
              type="password"
              autocomplete="current-password"
              style="width:100%;height:44px;border:1px solid #D1D5DB;border-radius:8px;padding:0 14px 0 40px;font-size:15px;color:#0A0A0A;outline:none;font-family:inherit;transition:border-color .15s,box-shadow .15s"
            >
          </div>

          <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:26px">
            <label style="display:flex;align-items:center;gap:8px;font-size:13px;color:#374151;cursor:pointer">
              <input v-model="form.remember" type="checkbox" style="width:15px;height:15px;accent-color:#3D5AF1">
              Məni xatırla
            </label>
            <a style="font-size:13px;color:#3D5AF1;font-weight:500;cursor:pointer">Şifrəni unutmusunuz?</a>
          </div>

          <button
            type="submit"
            :disabled="loading"
            style="width:100%;height:46px;background:#3D5AF1;color:#fff;border:none;border-radius:8px;font-size:15px;font-weight:600;cursor:pointer;font-family:inherit;transition:background .15s;display:flex;align-items:center;justify-content:center;gap:8px"
          >
            <UIcon v-if="loading" name="i-lucide-loader-2" style="width:18px;height:18px;animation:spin 1s linear infinite" />
            {{ loading ? 'Gözləyin...' : 'Daxil ol' }}
          </button>
        </form>

        <p style="text-align:center;font-size:13px;color:#9CA3AF;margin:24px 0 0">
          Hesabınız yoxdur? <a style="color:#3D5AF1;font-weight:500;cursor:pointer">Administratorla əlaqə</a>
        </p>
      </div>
    </div>
  </div>
</template>

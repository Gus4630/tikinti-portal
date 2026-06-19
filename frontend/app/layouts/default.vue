<script setup lang="ts">
const auth = useAuthStore()
const route = useRoute()

const navItems = [
  { label: 'İdarə paneli', icon: 'i-lucide-layout-dashboard', to: '/dashboard' },
  { label: 'Binalar', icon: 'i-lucide-building-2', to: '/buildings' },
  { label: 'Xərclər', icon: 'i-lucide-file-text', to: '/expenses' },
  { label: 'Təchizatçılar', icon: 'i-lucide-truck', to: '/suppliers' },
  { label: 'Qruplar', icon: 'i-lucide-users', to: '/groups' },
  { label: 'Hesabatlar', icon: 'i-lucide-bar-chart-2', to: '/reports' },
]

const bottomNav = [
  { label: 'Ayarlar', icon: 'i-lucide-settings', to: '/settings' },
]

function isActive(to: string) {
  return route.path === to || route.path.startsWith(to + '/')
}
</script>

<template>
  <div style="display:flex;height:100vh;width:100%;overflow:hidden">
    <!-- Sidebar -->
    <aside style="flex:none;width:var(--sidebar-width);background:#0D1440;display:flex;flex-direction:column;overflow:hidden">
      <!-- Logo -->
      <div style="height:var(--header-height);display:flex;align-items:center;gap:11px;padding:0 20px;flex:none;border-bottom:1px solid rgba(255,255,255,0.08)">
        <div style="width:32px;height:32px;border-radius:8px;background:#F59E0B;display:flex;align-items:center;justify-content:center;color:#0D1440;font-weight:700;font-size:15px;flex:none">T</div>
        <span style="font-size:18px;font-weight:700;color:#fff;letter-spacing:-0.01em">Tikinti</span>
      </div>

      <!-- Nav -->
      <nav style="flex:1;overflow-y:auto;padding:12px 0">
        <NuxtLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="nav-item"
          :class="{ active: isActive(item.to) }"
        >
          <UIcon :name="item.icon" style="width:18px;height:18px;flex:none" />
          <span>{{ item.label }}</span>
        </NuxtLink>
      </nav>

      <!-- Bottom -->
      <div style="border-top:1px solid rgba(255,255,255,0.08)">
        <NuxtLink
          v-for="item in bottomNav"
          :key="item.to"
          :to="item.to"
          class="nav-item"
          :class="{ active: isActive(item.to) }"
        >
          <UIcon :name="item.icon" style="width:18px;height:18px;flex:none" />
          <span>{{ item.label }}</span>
        </NuxtLink>

        <!-- User -->
        <div style="display:flex;align-items:center;gap:11px;padding:14px 18px;margin:4px 0">
          <div style="width:36px;height:36px;border-radius:9999px;background:#F59E0B;color:#0D1440;display:flex;align-items:center;justify-content:center;font-weight:700;font-size:13px;flex:none">
            {{ auth.initials }}
          </div>
          <div style="flex:1;min-width:0">
            <div style="font-size:13px;font-weight:600;color:#fff;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">{{ auth.displayName }}</div>
            <div style="font-size:11.5px;color:#9098C9">{{ auth.user?.role }}</div>
          </div>
          <button
            style="color:#9098C9;background:none;border:none;cursor:pointer;padding:4px;flex:none"
            title="Çıxış"
            @click="auth.logout(); navigateTo('/login')"
          >
            <UIcon name="i-lucide-log-out" style="width:16px;height:16px" />
          </button>
        </div>
      </div>
    </aside>

    <!-- Main area -->
    <div style="flex:1;display:flex;flex-direction:column;min-width:0;height:100vh">
      <!-- Header -->
      <header style="height:var(--header-height);flex:none;background:#fff;border-bottom:1px solid #E5E7EB;display:flex;align-items:center;justify-content:space-between;padding:0 28px;gap:16px">
        <div style="position:relative;width:300px;max-width:38vw">
          <UIcon name="i-lucide-search" style="position:absolute;left:12px;top:50%;transform:translateY(-50%);color:#9CA3AF;width:16px;height:16px" />
          <input
            placeholder="Qaimə, təchizatçı, layihə axtar..."
            style="width:100%;height:38px;background:#F3F4F6;border:1px solid transparent;border-radius:8px;padding:0 14px 0 38px;font-size:13.5px;outline:none;font-family:inherit"
          >
        </div>
        <div style="display:flex;align-items:center;gap:16px">
          <div style="position:relative;color:#6B7280;cursor:pointer">
            <UIcon name="i-lucide-bell" style="width:20px;height:20px" />
            <span style="position:absolute;top:-3px;right:-3px;width:8px;height:8px;border-radius:9999px;background:#F59E0B;border:2px solid #fff" />
          </div>
          <div style="width:1px;height:24px;background:#E5E7EB" />
          <div style="display:flex;align-items:center;gap:9px">
            <div style="width:32px;height:32px;border-radius:9999px;background:#141F5E;color:#fff;display:flex;align-items:center;justify-content:center;font-weight:700;font-size:12px">
              {{ auth.initials }}
            </div>
            <span style="font-size:13.5px;font-weight:500;color:#374151">{{ auth.displayName }}</span>
          </div>
        </div>
      </header>

      <!-- Page content -->
      <main style="flex:1;overflow-y:auto;padding:28px 32px 56px">
        <slot />
      </main>
    </div>
  </div>
</template>

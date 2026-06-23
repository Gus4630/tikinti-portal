<script setup lang="ts">
const auth = useAuthStore()
const context = useContextStore()
const route = useRoute()
const tutorial = useTutorial()

interface NavItem { label: string; icon: string; to: string; tutorial?: string }

const navItems: NavItem[] = [
  { label: 'İdarə paneli', icon: 'i-lucide-layout-dashboard', to: '/dashboard' },
  { label: 'Binalar',       icon: 'i-lucide-building-2',       to: '/buildings',  tutorial: 'nav-buildings' },
  { label: 'Xərclər',      icon: 'i-lucide-file-text',        to: '/expenses',   tutorial: 'nav-expenses' },
  { label: 'Kateqoriyalar', icon: 'i-lucide-tag',             to: '/categories', tutorial: 'nav-categories' },
  { label: 'Təchizatçılar', icon: 'i-lucide-truck',           to: '/suppliers' },
  { label: 'Qruplar',      icon: 'i-lucide-users',            to: '/groups',     tutorial: 'nav-groups' },
  { label: 'Hesabatlar',   icon: 'i-lucide-bar-chart-2',      to: '/reports',    tutorial: 'nav-reports' },
]
const bottomNav: NavItem[] = [
  { label: 'Ayarlar', icon: 'i-lucide-settings', to: '/settings', tutorial: 'nav-settings' },
]

// Mobile bottom tab bar — 4 primary tabs
const mobileNavTabs: NavItem[] = [
  { label: 'İdarə paneli', icon: 'i-lucide-layout-dashboard', to: '/dashboard' },
  { label: 'Xərclər',     icon: 'i-lucide-file-text',        to: '/expenses',  tutorial: 'mobile-nav-expenses' },
  { label: 'Binalar',     icon: 'i-lucide-building-2',       to: '/buildings', tutorial: 'mobile-nav-buildings' },
  { label: 'Qruplar',     icon: 'i-lucide-users',            to: '/groups' },
]

// "More" drawer overflow items
const moreItems = [
  { label: 'Kateqoriyalar', icon: 'i-lucide-tag',          to: '/categories' },
  { label: 'Təchizatçılar', icon: 'i-lucide-truck',        to: '/suppliers' },
  { label: 'Hesabatlar',   icon: 'i-lucide-bar-chart-2',   to: '/reports' },
  { label: 'Ayarlar',      icon: 'i-lucide-settings',      to: '/settings' },
]

function isActive(to: string) {
  return route.path === to || route.path.startsWith(to + '/')
}

const moreActive = computed(() =>
  moreItems.some(item => isActive(item.to)),
)

// Sidebar expand/collapse state
const pinned = ref(true)
const hovered = ref(false)
const expanded = computed(() => pinned.value || hovered.value)

onMounted(() => {
  const saved = localStorage.getItem('sidebar_pinned')
  if (saved !== null) pinned.value = saved === 'true'
  tutorial.init()
})

watch(pinned, v => localStorage.setItem('sidebar_pinned', String(v)))

const sidebarWidth = computed(() => expanded.value ? '220px' : '62px')

// Mobile "more" drawer
const moreOpen = ref(false)
const profileOpen = ref(false)

function goMore(to: string) {
  moreOpen.value = false
  navigateTo(to)
}

function openProfile() {
  if (typeof window !== 'undefined' && window.innerWidth <= 767) {
    profileOpen.value = true
  }
}

function handleLogout() {
  moreOpen.value = false
  profileOpen.value = false
  auth.logout()
  navigateTo('/login')
}
</script>

<template>
  <div style="display:flex;height:100vh;width:100%;overflow:hidden">
    <!-- ── Sidebar (desktop only) ──────────────────────────────── -->
    <aside
      data-tutorial="sidebar"
      class="layout-sidebar"
      :style="{
        flex: 'none',
        width: sidebarWidth,
        background: '#0D1440',
        display: 'flex',
        flexDirection: 'column',
        overflow: 'hidden',
        transition: 'width 0.22s cubic-bezier(0.4,0,0.2,1)',
        zIndex: 30,
      }"
      @mouseenter="hovered = true"
      @mouseleave="hovered = false"
    >
      <!-- Logo + pin -->
      <div style="height:var(--header-height);display:flex;align-items:center;padding:0 13px;flex:none;border-bottom:1px solid rgba(255,255,255,0.08);gap:10px;overflow:hidden">
        <div style="width:32px;height:32px;border-radius:8px;background:#F59E0B;display:flex;align-items:center;justify-content:center;color:#0D1440;font-weight:700;font-size:13px;flex:none;letter-spacing:-0.03em">SG</div>
        <span
          class="nav-label"
          :style="{ opacity: expanded ? '1' : '0', fontSize: '18px', fontWeight: '700', color: '#fff', letterSpacing: '-0.01em', flex: '1', minWidth: 0 }"
        >SagaGroup</span>
        <button
          v-if="expanded"
          :title="pinned ? 'Sıxılmış görünüş' : 'Sabitlə'"
          :style="{
            background: 'none', border: 'none', cursor: 'pointer', padding: '4px',
            color: pinned ? '#F59E0B' : '#9098C9', flexShrink: '0',
            display: 'flex', alignItems: 'center',
          }"
          @click="pinned = !pinned"
        >
          <UIcon :name="pinned ? 'i-lucide-pin' : 'i-lucide-pin-off'" style="width:15px;height:15px" />
        </button>
      </div>

      <!-- Main nav -->
      <nav style="flex:1;overflow-y:auto;padding:12px 0">
        <NuxtLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="nav-item"
          :class="{ active: isActive(item.to) }"
          :data-tutorial="item.tutorial"
        >
          <UIcon :name="item.icon" style="width:18px;height:18px;flex:none" />
          <span class="nav-label" :style="{ opacity: expanded ? '1' : '0' }">{{ item.label }}</span>
        </NuxtLink>
      </nav>

      <!-- Bottom nav -->
      <div style="border-top:1px solid rgba(255,255,255,0.08)">
        <NuxtLink
          v-for="item in bottomNav"
          :key="item.to"
          :to="item.to"
          class="nav-item"
          :class="{ active: isActive(item.to) }"
          :data-tutorial="item.tutorial"
        >
          <UIcon :name="item.icon" style="width:18px;height:18px;flex:none" />
          <span class="nav-label" :style="{ opacity: expanded ? '1' : '0' }">{{ item.label }}</span>
        </NuxtLink>

        <!-- User row -->
        <div style="display:flex;align-items:center;gap:10px;padding:14px 13px;overflow:hidden">
          <div style="width:34px;height:34px;border-radius:9999px;background:#F59E0B;color:#0D1440;display:flex;align-items:center;justify-content:center;font-weight:700;font-size:12px;flex:none">
            {{ auth.initials }}
          </div>
          <div
            class="nav-label"
            :style="{ opacity: expanded ? '1' : '0', flex: '1', minWidth: 0, overflow: 'hidden' }"
          >
            <div style="font-size:13px;font-weight:600;color:#fff;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">{{ auth.displayName }}</div>
            <div style="font-size:11.5px;color:#9098C9;white-space:nowrap">{{ auth.user?.role }}</div>
          </div>
          <button
            v-if="expanded"
            style="color:#9098C9;background:none;border:none;cursor:pointer;padding:4px;flex:none;display:flex;align-items:center"
            title="Çıxış"
            @click="auth.logout(); navigateTo('/login')"
          >
            <UIcon name="i-lucide-log-out" style="width:15px;height:15px" />
          </button>
        </div>
      </div>
    </aside>

    <!-- ── Main area ───────────────────────────────────────────── -->
    <div style="flex:1;display:flex;flex-direction:column;min-width:0;height:100vh;overflow:hidden">
      <!-- Header -->
      <header
        class="layout-header"
        style="height:var(--header-height);flex:none;background:#fff;border-bottom:1px solid #E5E7EB;display:flex;align-items:center;justify-content:space-between;padding:0 28px;gap:16px"
      >
        <!-- Mobile: logo mark -->
        <div class="header-mobile-logo">
          <div style="width:30px;height:30px;border-radius:7px;background:#F59E0B;display:flex;align-items:center;justify-content:center;color:#0D1440;font-weight:700;font-size:12px;letter-spacing:-0.03em">SG</div>
          <span style="font-size:16px;font-weight:700;color:#0D1440;letter-spacing:-0.01em">SagaGroup</span>
        </div>

        <!-- Desktop: search + context switcher -->
        <div class="header-desktop-left">
          <div style="position:relative;width:260px;max-width:30vw;flex:none">
            <UIcon name="i-lucide-search" style="position:absolute;left:12px;top:50%;transform:translateY(-50%);color:#9CA3AF;width:16px;height:16px" />
            <input
              placeholder="Qaimə, təchizatçı, layihə axtar..."
              style="width:100%;height:38px;background:#F3F4F6;border:1px solid transparent;border-radius:8px;padding:0 14px 0 38px;font-size:13.5px;outline:none;font-family:inherit"
            >
          </div>
          <div style="width:1px;height:24px;background:#E5E7EB;flex:none" />
          <ContextSwitcher />
        </div>

        <!-- Right side: notifications + user -->
        <div style="display:flex;align-items:center;gap:14px;margin-left:auto">
          <InvitationsDropdown />
          <div style="width:1px;height:24px;background:#E5E7EB" />
          <div style="display:flex;align-items:center;gap:9px;cursor:pointer" role="button" @click="openProfile">
            <div style="width:32px;height:32px;border-radius:9999px;background:#141F5E;color:#fff;display:flex;align-items:center;justify-content:center;font-weight:700;font-size:12px">
              {{ auth.initials }}
            </div>
            <span class="md-hide" style="font-size:13.5px;font-weight:500;color:#374151">{{ auth.displayName }}</span>
          </div>
        </div>
      </header>

      <!-- Page content -->
      <main class="layout-main" style="flex:1;overflow-y:auto;padding:28px 32px 56px">
        <slot />
      </main>
    </div>

    <!-- ── Mobile bottom nav ───────────────────────────────────── -->
    <nav class="mobile-nav" data-tutorial="mobile-nav">
      <NuxtLink
        v-for="tab in mobileNavTabs"
        :key="tab.to"
        :to="tab.to"
        class="mobile-nav-tab"
        :class="{ active: isActive(tab.to) }"
        :data-tutorial="tab.tutorial"
      >
        <UIcon :name="tab.icon" style="width:22px;height:22px" />
        {{ tab.label }}
      </NuxtLink>
      <button
        data-tutorial="mobile-more"
        class="mobile-nav-tab"
        :class="{ active: moreActive || moreOpen }"
        @click="moreOpen = true"
      >
        <UIcon name="i-lucide-grid-3x3" style="width:22px;height:22px" />
        Daha çox
      </button>
    </nav>

    <!-- ── Mobile "More" drawer ────────────────────────────────── -->
    <Teleport to="body">
      <div v-if="moreOpen" class="drawer-overlay" @click="moreOpen = false">
        <div class="drawer-sheet" @click.stop>
          <div class="drawer-handle" />

          <div class="drawer-section-title">Digər bölmələr</div>

          <button
            v-for="item in moreItems"
            :key="item.to"
            class="drawer-link"
            :class="{ active: isActive(item.to) }"
            @click="goMore(item.to)"
          >
            <UIcon :name="item.icon" style="width:20px;height:20px;flex:none" />
            {{ item.label }}
          </button>
        </div>
      </div>
    </Teleport>

    <!-- ── Tutorial overlay ──────────────────────────────────── -->
    <AppTutorial />

    <!-- ── Mobile Profile sheet ───────────────────────────────── -->
    <Teleport to="body">
      <div v-if="profileOpen" class="drawer-overlay" @click="profileOpen = false">
        <div class="drawer-sheet" @click.stop>
          <div class="drawer-handle" />

          <!-- User info -->
          <div class="drawer-user-row">
            <div style="width:42px;height:42px;border-radius:9999px;background:#F59E0B;color:#0D1440;display:flex;align-items:center;justify-content:center;font-weight:700;font-size:14px;flex:none">
              {{ auth.initials }}
            </div>
            <div style="flex:1;min-width:0">
              <div style="font-size:15px;font-weight:600;color:#fff;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">{{ auth.displayName }}</div>
              <div style="font-size:12px;color:#5A6BA0">{{ auth.user?.role }}</div>
            </div>
          </div>

          <div class="drawer-sep" />
          <div class="drawer-section-title">Qrup &amp; Layihə</div>

          <!-- Group select -->
          <div v-if="context.groups.length > 0" style="padding:0 10px 10px">
            <label style="display:block;font-size:11px;font-weight:600;color:#5A6BA0;text-transform:uppercase;letter-spacing:0.05em;margin-bottom:6px">Qrup</label>
            <select
              :value="context.activeGroupId ?? ''"
              style="width:100%;height:42px;border:1px solid rgba(255,255,255,0.14);border-radius:8px;padding:0 12px;font-size:14px;background:rgba(255,255,255,0.08);color:#fff;font-family:inherit;outline:none;appearance:auto"
              @change="context.setGroup(($event.target as HTMLSelectElement).value)"
            >
              <option v-for="g in context.groups" :key="g.id" :value="g.id" style="background:#0D1440;color:#fff">{{ g.name }}</option>
            </select>
          </div>

          <!-- Building select -->
          <div v-if="context.buildings.length > 0" style="padding:0 10px 10px">
            <label style="display:block;font-size:11px;font-weight:600;color:#5A6BA0;text-transform:uppercase;letter-spacing:0.05em;margin-bottom:6px">Layihə</label>
            <select
              :value="context.activeBuildingId ?? ''"
              style="width:100%;height:42px;border:1px solid rgba(255,255,255,0.14);border-radius:8px;padding:0 12px;font-size:14px;background:rgba(255,255,255,0.08);color:#fff;font-family:inherit;outline:none;appearance:auto"
              @change="context.setBuilding(($event.target as HTMLSelectElement).value)"
            >
              <option v-for="b in context.buildings" :key="b.id" :value="b.id" style="background:#0D1440;color:#fff">{{ b.name }}</option>
            </select>
          </div>

          <div class="drawer-sep" />

          <button class="drawer-link" style="color:#EF4444" @click="handleLogout">
            <UIcon name="i-lucide-log-out" style="width:20px;height:20px;flex:none;color:#EF4444" />
            Çıxış
          </button>
        </div>
      </div>
    </Teleport>
  </div>
</template>

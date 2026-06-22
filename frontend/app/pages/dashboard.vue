<script setup lang="ts">
definePageMeta({ layout: 'default' })

const config = useRuntimeConfig()
const auth = useAuthStore()
const context = useContextStore()

const STATUS_META: Record<string, { label: string; color: string; bg: string }> = {
  UPLOADED:       { label: 'Yükləndi',    color: '#525252', bg: '#F3F4F6' },
  OCR_PROCESSING: { label: 'Emal olunur', color: '#3B82F6', bg: '#EFF6FF' },
  PENDING_REVIEW: { label: 'Yoxlanılır',  color: '#D97706', bg: '#FFFBEB' },
  APPROVED:       { label: 'Təsdiqləndi', color: '#16A34A', bg: '#F0FDF4' },
  DISPUTED:       { label: 'Mübahisəli',  color: '#DC2626', bg: '#FEF2F2' },
  REJECTED:       { label: 'Rədd edildi', color: '#991B1B', bg: '#FEE2E2' },
}

function fmt(n: number) {
  return '₼' + n.toLocaleString('az-AZ', { minimumFractionDigits: 0 })
}

function shortId(id: unknown) {
  return 'QF-' + String(id).replace(/-/g, '').slice(0, 8).toUpperCase()
}

const headers = computed(() => ({
  Authorization: `Bearer ${auth.token}`,
}))

const baseURL = config.public.apiBase

// Current month range
const now = new Date()
const monthStart = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-01`
const monthEnd = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate()).padStart(2, '0')}`

const { data: recentExpenses } = await useAsyncData('recent-expenses', () =>
  $fetch<{ content: unknown[] }>('/api/v1/expenses/search', {
    baseURL,
    method: 'POST',
    headers: headers.value,
    body: { page: 0, perPage: 6, sortBy: 'createdAt', sortDir: 'DESC', buildingId: context.activeBuildingId || undefined },
  }).catch(() => ({ content: [] })),
  { watch: [() => context.activeBuildingId] },
)

const { data: monthlyExpenses } = await useAsyncData('monthly-expenses', () =>
  $fetch<{ content: unknown[] }>('/api/v1/expenses/search', {
    baseURL,
    method: 'POST',
    headers: headers.value,
    body: { page: 0, perPage: 100, dateFrom: monthStart, dateTo: monthEnd, buildingId: context.activeBuildingId || undefined },
  }).catch(() => ({ content: [] })),
  { watch: [() => context.activeBuildingId] },
)

const { data: buildings } = await useAsyncData('buildings-dashboard', () =>
  $fetch<{ content: unknown[] }>('/api/v1/buildings/search', {
    baseURL,
    method: 'POST',
    headers: headers.value,
    body: { page: 0, perPage: 10, groupId: context.activeGroupId || undefined },
  }).catch(() => ({ content: [] })),
  { watch: [() => context.activeGroupId] },
)

const expenses = computed(() => (recentExpenses.value?.content ?? []) as Record<string, unknown>[])
const buildingList = computed(() => (buildings.value?.content ?? []) as Record<string, unknown>[])

const totalSpend = computed(() =>
  buildingList.value.reduce((s, b) => s + (Number(b.totalSpent) || 0), 0),
)

const monthlyTotal = computed(() => {
  const list = (monthlyExpenses.value?.content ?? []) as Record<string, unknown>[]
  return list.reduce((s, e) => s + (Number(e.amountBaseCurrency) || 0), 0)
})

const pendingCount = computed(() =>
  expenses.value.filter((e) => e.status === 'PENDING_REVIEW').length,
)
const pendingAmount = computed(() =>
  expenses.value.filter((e) => e.status === 'PENDING_REVIEW').reduce((s, e) => s + (Number(e.amountBaseCurrency) || 0), 0),
)

const disputedCount = computed(() =>
  expenses.value.filter((e) => e.status === 'DISPUTED').length,
)
const disputedAmount = computed(() =>
  expenses.value.filter((e) => e.status === 'DISPUTED').reduce((s, e) => s + (Number(e.amountBaseCurrency) || 0), 0),
)

const chartMax = computed(() => {
  const vals = buildingList.value.flatMap(b => [Number(b.budgetLimit) || 0, Number(b.totalSpent) || 0])
  return Math.max(...vals, 1)
})

function utilPct(b: Record<string, unknown>) {
  if (!b.budgetLimit || !b.totalSpent) return 0
  return Math.min(100, (Number(b.totalSpent) / Number(b.budgetLimit)) * 100)
}
function barColor(b: Record<string, unknown>) {
  const pct = utilPct(b)
  if (pct >= 100) return '#DC2626'
  if (pct >= 80) return '#F59E0B'
  return '#3D5AF1'
}
</script>

<template>
  <div class="animate-page">
    <!-- Page header -->
    <div style="margin-bottom:24px">
      <div style="font-size:12px;font-weight:500;color:#9CA3AF;letter-spacing:0.04em;text-transform:uppercase;margin-bottom:6px">Xoş gəldiniz</div>
      <h1 style="font-size:26px;font-weight:700;letter-spacing:-0.02em;margin:0 0 4px">İdarə paneli</h1>
      <p style="font-size:14px;color:#6B7280;margin:0">Layihələrinizin maliyyə icmalı</p>
    </div>

    <!-- Stat cards — 4 columns desktop, 2×2 mobile -->
    <div class="rg-4">
      <div class="tk-card stat-card" style="padding:20px">
        <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:16px">
          <span style="font-size:13.5px;font-weight:500;color:#6B7280">Ümumi xərc</span>
          <div class="stat-icon" style="width:36px;height:36px;border-radius:8px;background:#E0E9FF;color:#3D5AF1;display:flex;align-items:center;justify-content:center">
            <UIcon name="i-lucide-trending-up" style="width:18px;height:18px" />
          </div>
        </div>
        <div class="stat-num" style="font-family:var(--font-mono);font-size:28px;font-weight:600;letter-spacing:-0.02em;line-height:1.1;margin-bottom:6px">{{ fmt(totalSpend) }}</div>
        <div style="font-size:12px;color:#6B7280">Bütün layihələr üzrə</div>
      </div>

      <div class="tk-card stat-card" style="padding:20px">
        <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:16px">
          <span style="font-size:13.5px;font-weight:500;color:#6B7280">Bu ay</span>
          <div class="stat-icon" style="width:36px;height:36px;border-radius:8px;background:#FEF3C7;color:#D97706;display:flex;align-items:center;justify-content:center">
            <UIcon name="i-lucide-calendar" style="width:18px;height:18px" />
          </div>
        </div>
        <div class="stat-num" style="font-family:var(--font-mono);font-size:28px;font-weight:600;letter-spacing:-0.02em;line-height:1.1;margin-bottom:6px">
          {{ monthlyTotal > 0 ? fmt(monthlyTotal) : '—' }}
        </div>
        <div style="font-size:12px;color:#6B7280">Cari ay xərcləri</div>
      </div>

      <div class="tk-card stat-card" style="padding:20px">
        <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:16px">
          <span style="font-size:13.5px;font-weight:500;color:#6B7280">Yoxlanılır</span>
          <div class="stat-icon" style="width:36px;height:36px;border-radius:8px;background:#FFFBEB;color:#D97706;display:flex;align-items:center;justify-content:center">
            <UIcon name="i-lucide-clock" style="width:18px;height:18px" />
          </div>
        </div>
        <div class="stat-num" style="font-family:var(--font-mono);font-size:28px;font-weight:600;letter-spacing:-0.02em;line-height:1.1;margin-bottom:6px">{{ pendingCount }}</div>
        <div style="font-size:12px;color:#D97706">{{ pendingAmount > 0 ? fmt(pendingAmount) + ' gözləyir' : 'Təsdiq gözləyir' }}</div>
      </div>

      <div class="tk-card stat-card" style="padding:20px">
        <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:16px">
          <span style="font-size:13.5px;font-weight:500;color:#6B7280">Mübahisəli</span>
          <div class="stat-icon" style="width:36px;height:36px;border-radius:8px;background:#FEF2F2;color:#DC2626;display:flex;align-items:center;justify-content:center">
            <UIcon name="i-lucide-alert-triangle" style="width:18px;height:18px" />
          </div>
        </div>
        <div class="stat-num" style="font-family:var(--font-mono);font-size:28px;font-weight:600;letter-spacing:-0.02em;line-height:1.1;margin-bottom:6px">{{ disputedCount }}</div>
        <div style="font-size:12px;color:#DC2626">{{ disputedAmount > 0 ? fmt(disputedAmount) + ' mübahisədə' : 'Nəzərdən keçirin' }}</div>
      </div>
    </div>

    <!-- Charts row: 2-col desktop, stacked mobile -->
    <div class="rg-2w">
      <!-- Budget vs Actual bar chart -->
      <div class="tk-card" style="padding:22px">
        <div style="display:flex;align-items:flex-start;justify-content:space-between;margin-bottom:18px;flex-wrap:wrap;gap:10px">
          <div>
            <h3 style="font-size:16px;font-weight:600;margin:0 0 4px">Büdcə vs Faktiki</h3>
            <p style="font-size:13px;color:#6B7280;margin:0">Layihə üzrə müqayisə</p>
          </div>
          <div style="display:flex;align-items:center;gap:12px;flex-shrink:0">
            <div style="display:flex;align-items:center;gap:5px"><span style="width:11px;height:11px;background:#C7D2FE;border-radius:2px;display:inline-block"/><span style="font-size:12px;color:#6B7280">Büdcə</span></div>
            <div style="display:flex;align-items:center;gap:5px"><span style="width:11px;height:11px;background:#3D5AF1;border-radius:2px;display:inline-block"/><span style="font-size:12px;color:#6B7280">Faktiki</span></div>
          </div>
        </div>
        <div v-if="buildingList.length" style="overflow-x:auto;-webkit-overflow-scrolling:touch">
          <div :style="`min-width:${Math.max(buildingList.slice(0,6).length * 70, 280)}px`">
            <div style="display:flex;align-items:flex-end;justify-content:space-around;height:160px;gap:6px;padding:0 4px">
              <div v-for="b in buildingList.slice(0,6)" :key="String(b.id)" style="display:flex;flex-direction:column;align-items:center;gap:8px;flex:1">
                <div style="display:flex;align-items:flex-end;gap:4px;height:135px">
                  <div :style="`width:16px;background:#C7D2FE;border-radius:3px 3px 0 0;height:${b.budgetLimit ? Math.round((Number(b.budgetLimit)/chartMax)*135) : 0}px;min-height:${b.budgetLimit ? 3 : 0}px`" />
                  <div :style="`width:16px;border-radius:3px 3px 0 0;background:${Number(b.totalSpent)>Number(b.budgetLimit) ? '#F59E0B' : '#3D5AF1'};height:${Math.round((Number(b.totalSpent)||0)/chartMax*135)}px;min-height:3px`" />
                </div>
                <span style="font-size:10px;color:#6B7280;font-weight:500;text-align:center;max-width:50px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">{{ b.name }}</span>
              </div>
            </div>
          </div>
        </div>
        <div v-else style="padding:32px;text-align:center;color:#9CA3AF;font-size:13.5px">Hələ ki layihə yoxdur</div>
      </div>

      <!-- Budget utilization -->
      <div class="tk-card" style="padding:22px">
        <h3 style="font-size:16px;font-weight:600;margin:0 0 4px">Büdcə istifadəsi</h3>
        <p style="font-size:13px;color:#6B7280;margin:0 0 18px">Layihə üzrə faiz</p>
        <div v-if="buildingList.length" style="display:flex;flex-direction:column;gap:14px">
          <div v-for="b in buildingList.slice(0,6)" :key="String(b.id)">
            <div style="display:flex;justify-content:space-between;font-size:13px;margin-bottom:6px">
              <span style="color:#374151;font-weight:500;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;max-width:160px">{{ b.name }}</span>
              <span :style="`font-family:var(--font-mono);font-weight:600;color:${barColor(b)}`">{{ utilPct(b).toFixed(0) }}%</span>
            </div>
            <div class="progress-track">
              <div class="progress-bar" :style="{ width: b.budgetLimit ? utilPct(b) + '%' : '0%', background: barColor(b) }" />
            </div>
          </div>
        </div>
        <div v-else style="padding:32px;text-align:center;color:#9CA3AF;font-size:13.5px">Hələ ki layihə yoxdur</div>
      </div>
    </div>

    <!-- Recent expenses -->
    <div class="tk-card" style="overflow:hidden">
      <div style="display:flex;align-items:center;justify-content:space-between;padding:18px 22px;border-bottom:1px solid #F3F4F6">
        <h3 style="font-size:16px;font-weight:600;margin:0">Son qaimələr</h3>
        <NuxtLink to="/expenses" style="font-size:13px;color:#3D5AF1;font-weight:500;text-decoration:none">Hamısına bax →</NuxtLink>
      </div>

      <!-- Desktop table -->
      <div class="md-hide" style="overflow-x:auto">
        <table class="data-table" style="min-width:540px">
          <thead>
            <tr>
              <th>Qaimə №</th>
              <th>Təchizatçı</th>
              <th>Layihə</th>
              <th>Status</th>
              <th>Tarix</th>
              <th class="text-right">Məbləğ</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="e in expenses"
              :key="String(e.id)"
              style="cursor:pointer"
              @click="navigateTo('/expenses/' + e.id)"
            >
              <td style="font-family:var(--font-mono);font-size:12.5px;color:#141F5E;font-weight:600">{{ shortId(e.id) }}</td>
              <td style="font-weight:500">{{ e.supplierName || '—' }}</td>
              <td style="color:#6B7280">{{ e.buildingName || '—' }}</td>
              <td>
                <span :class="'badge badge-' + e.status" style="font-size:11.5px">
                  {{ STATUS_META[String(e.status)]?.label ?? e.status }}
                </span>
              </td>
              <td style="font-family:var(--font-mono);color:#6B7280;font-size:13px">{{ e.expenseDate }}</td>
              <td style="font-family:var(--font-mono);font-weight:500;text-align:right">{{ fmt(Number(e.amountBaseCurrency) || 0) }}</td>
            </tr>
            <tr v-if="!expenses.length">
              <td colspan="6" style="text-align:center;color:#9CA3AF;padding:32px">Xərc tapılmadı</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Mobile card list -->
      <div class="md-show m-list">
        <div
          v-for="e in expenses"
          :key="String(e.id)"
          class="m-row"
          @click="navigateTo('/expenses/' + e.id)"
        >
          <div class="m-row-left">
            <div class="m-row-title">{{ e.supplierName || '—' }}</div>
            <div class="m-row-sub">{{ e.buildingName || '—' }} · {{ e.expenseDate }}</div>
          </div>
          <div class="m-row-right">
            <div class="m-row-amount">{{ fmt(Number(e.amountBaseCurrency) || 0) }}</div>
            <span :class="'badge badge-' + e.status" style="font-size:10.5px">
              {{ STATUS_META[String(e.status)]?.label ?? e.status }}
            </span>
          </div>
        </div>
        <div v-if="!expenses.length" style="padding:32px;text-align:center;color:#9CA3AF;font-size:14px">
          Xərc tapılmadı
        </div>
      </div>
    </div>
  </div>
</template>

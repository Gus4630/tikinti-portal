<script setup lang="ts">
definePageMeta({ layout: 'default' })

const config = useRuntimeConfig()
const auth = useAuthStore()

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

const headers = computed(() => ({
  Authorization: `Bearer ${auth.token}`,
}))

const baseURL = config.public.apiBase

const { data: recentExpenses } = await useAsyncData('recent-expenses', () =>
  $fetch<{ content: unknown[] }>('/api/v1/expenses/search', {
    baseURL,
    method: 'POST',
    headers: headers.value,
    body: { page: 0, perPage: 6, sortBy: 'createdAt', sortDir: 'DESC' },
  }).catch(() => ({ content: [] })),
)

const { data: buildings } = await useAsyncData('buildings-dashboard', () =>
  $fetch<{ content: unknown[] }>('/api/v1/buildings/search', {
    baseURL,
    method: 'POST',
    headers: headers.value,
    body: { page: 0, perPage: 10 },
  }).catch(() => ({ content: [] })),
)

const expenses = computed(() => (recentExpenses.value?.content ?? []) as Record<string, unknown>[])
const buildingList = computed(() => (buildings.value?.content ?? []) as Record<string, unknown>[])

const totalSpend = computed(() =>
  buildingList.value.reduce((s, b) => s + (Number(b.totalSpent) || 0), 0),
)

const pendingCount = computed(() =>
  expenses.value.filter((e) => e.status === 'PENDING_REVIEW').length,
)

const disputedCount = computed(() =>
  expenses.value.filter((e) => e.status === 'DISPUTED').length,
)
</script>

<template>
  <div class="animate-page">
    <!-- Page header -->
    <div style="margin-bottom:24px">
      <div style="font-size:12px;font-weight:500;color:#9CA3AF;letter-spacing:0.04em;text-transform:uppercase;margin-bottom:6px">Xoş gəldiniz</div>
      <h1 style="font-size:26px;font-weight:700;letter-spacing:-0.02em;margin:0 0 4px">İdarə paneli</h1>
      <p style="font-size:14px;color:#6B7280;margin:0">Layihələrinizin maliyyə icmalı</p>
    </div>

    <!-- Stat cards -->
    <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:18px;margin-bottom:22px">
      <div class="tk-card" style="padding:20px">
        <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:16px">
          <span style="font-size:13.5px;font-weight:500;color:#6B7280">Ümumi xərc</span>
          <div style="width:36px;height:36px;border-radius:8px;background:#E0E9FF;color:#3D5AF1;display:flex;align-items:center;justify-content:center">
            <UIcon name="i-lucide-trending-up" style="width:18px;height:18px" />
          </div>
        </div>
        <div style="font-family:var(--font-mono);font-size:28px;font-weight:600;letter-spacing:-0.02em;line-height:1.1;margin-bottom:6px">{{ fmt(totalSpend) }}</div>
        <div style="font-size:12px;color:#6B7280">Bütün layihələr üzrə</div>
      </div>

      <div class="tk-card" style="padding:20px">
        <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:16px">
          <span style="font-size:13.5px;font-weight:500;color:#6B7280">Bu ay</span>
          <div style="width:36px;height:36px;border-radius:8px;background:#FFFBEB;color:#D97706;display:flex;align-items:center;justify-content:center">
            <UIcon name="i-lucide-calendar" style="width:18px;height:18px" />
          </div>
        </div>
        <div style="font-family:var(--font-mono);font-size:28px;font-weight:600;letter-spacing:-0.02em;line-height:1.1;margin-bottom:6px">—</div>
        <div style="font-size:12px;color:#6B7280">Cari ay</div>
      </div>

      <div class="tk-card" style="padding:20px">
        <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:16px">
          <span style="font-size:13.5px;font-weight:500;color:#6B7280">Yoxlanılır</span>
          <div style="width:36px;height:36px;border-radius:8px;background:#FFFBEB;color:#D97706;display:flex;align-items:center;justify-content:center">
            <UIcon name="i-lucide-clock" style="width:18px;height:18px" />
          </div>
        </div>
        <div style="font-family:var(--font-mono);font-size:28px;font-weight:600;letter-spacing:-0.02em;line-height:1.1;margin-bottom:6px">{{ pendingCount }}</div>
        <div style="font-size:12px;color:#D97706">Təsdiq gözləyir</div>
      </div>

      <div class="tk-card" style="padding:20px">
        <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:16px">
          <span style="font-size:13.5px;font-weight:500;color:#6B7280">Mübahisəli</span>
          <div style="width:36px;height:36px;border-radius:8px;background:#FEF2F2;color:#DC2626;display:flex;align-items:center;justify-content:center">
            <UIcon name="i-lucide-alert-triangle" style="width:18px;height:18px" />
          </div>
        </div>
        <div style="font-family:var(--font-mono);font-size:28px;font-weight:600;letter-spacing:-0.02em;line-height:1.1;margin-bottom:6px">{{ disputedCount }}</div>
        <div style="font-size:12px;color:#DC2626">Nəzərdən keçirin</div>
      </div>
    </div>

    <!-- Charts row -->
    <div style="display:grid;grid-template-columns:1fr;gap:18px;margin-bottom:22px">
      <!-- Budget utilization per building -->
      <div class="tk-card" style="padding:22px">
        <h3 style="font-size:16px;font-weight:600;margin:0 0 4px">Büdcə istifadəsi</h3>
        <p style="font-size:13px;color:#6B7280;margin:0 0 18px">Layihə üzrə faktiki xərc</p>
        <div v-if="buildingList.length" style="display:flex;flex-direction:column;gap:14px">
          <div v-for="b in buildingList.slice(0,6)" :key="String(b.id)">
            <div style="display:flex;justify-content:space-between;font-size:13px;margin-bottom:6px">
              <span style="color:#374151;font-weight:500;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;max-width:200px">{{ b.name }}</span>
              <span style="font-family:var(--font-mono);font-weight:600;color:#3D5AF1">{{ fmt(Number(b.totalSpent) || 0) }}</span>
            </div>
            <div class="progress-track">
              <div
                class="progress-bar"
                :style="{
                  width: b.budgetLimit ? Math.min(100, (Number(b.totalSpent) / Number(b.budgetLimit)) * 100) + '%' : '0%',
                  background: b.budgetLimit && Number(b.totalSpent) > Number(b.budgetLimit) ? '#DC2626' : '#3D5AF1',
                }"
              />
            </div>
          </div>
        </div>
        <div v-else style="padding:32px;text-align:center;color:#9CA3AF;font-size:13.5px">
          Hələ ki layihə yoxdur
        </div>
      </div>
    </div>

    <!-- Recent expenses table -->
    <div class="tk-card" style="overflow:hidden">
      <div style="display:flex;align-items:center;justify-content:space-between;padding:18px 22px;border-bottom:1px solid #F3F4F6">
        <h3 style="font-size:16px;font-weight:600;margin:0">Son qaimələr</h3>
        <NuxtLink to="/expenses" style="font-size:13px;color:#3D5AF1;font-weight:500;text-decoration:none">Hamısına bax →</NuxtLink>
      </div>
      <table class="data-table">
        <thead>
          <tr>
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
            @click="navigateTo('/expenses/' + e.id)"
          >
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
            <td colspan="5" style="text-align:center;color:#9CA3AF;padding:32px">Xərc tapılmadı</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

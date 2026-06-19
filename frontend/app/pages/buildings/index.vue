<script setup lang="ts">
definePageMeta({ layout: 'default' })

const config = useRuntimeConfig()
const auth = useAuthStore()
const headers = computed(() => ({ Authorization: `Bearer ${auth.token}` }))

const { data, refresh } = await useAsyncData('buildings', () =>
  $fetch<{ content: unknown[] }>('/api/v1/buildings/search', {
    baseURL: config.public.apiBase,
    method: 'POST',
    headers: headers.value,
    body: { page: 0, perPage: 50 },
  }).catch(() => ({ content: [] })),
)

const { data: groupsData } = await useAsyncData('buildings-groups', () =>
  $fetch<Record<string, unknown>[]>('/api/v1/groups', {
    baseURL: config.public.apiBase,
    headers: headers.value,
  }).catch(() => []),
)

const buildings = computed(() => (data.value?.content ?? []) as Record<string, unknown>[])
const groups = computed(() => (groupsData.value ?? []) as Record<string, unknown>[])

const modalOpen = ref(false)
const editBuilding = ref<Record<string, unknown> | null>(null)

function openCreate() { editBuilding.value = null; modalOpen.value = true }
function openEdit(b: Record<string, unknown>) { editBuilding.value = b; modalOpen.value = true }
function onSaved() { modalOpen.value = false; refresh() }

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

function fmt(n: number) {
  return '₼' + n.toLocaleString('az-AZ')
}
</script>

<template>
  <div class="animate-page">
    <div style="display:flex;align-items:flex-end;justify-content:space-between;gap:24px;margin-bottom:24px">
      <div>
        <div style="font-size:12px;font-weight:500;color:#9CA3AF;letter-spacing:0.04em;text-transform:uppercase;margin-bottom:6px">Aktivlər</div>
        <h1 style="font-size:26px;font-weight:700;letter-spacing:-0.02em;margin:0 0 4px">Binalar</h1>
        <p style="font-size:14px;color:#6B7280;margin:0">{{ buildings.length }} layihə</p>
      </div>
      <button
        style="display:inline-flex;align-items:center;gap:8px;height:40px;padding:0 18px;background:#3D5AF1;color:#fff;border:none;border-radius:8px;font-size:14px;font-weight:600;cursor:pointer"
        @click="openCreate"
      >
        <UIcon name="i-lucide-plus" style="width:16px;height:16px" />
        Yeni layihə
      </button>
    </div>

    <div style="display:grid;grid-template-columns:repeat(3,1fr);gap:18px">
      <div
        v-for="b in buildings"
        :key="String(b.id)"
        class="tk-card"
        style="padding:22px;cursor:pointer"
        @click="openEdit(b)"
      >
        <div style="display:flex;align-items:flex-start;justify-content:space-between;margin-bottom:18px">
          <h3 style="font-size:16px;font-weight:600;margin:0;max-width:74%;text-wrap:balance">{{ b.name }}</h3>
          <span style="font-size:11.5px;font-weight:600;padding:3px 9px;border-radius:9999px;color:#16A34A;background:#F0FDF4;white-space:nowrap">Aktiv</span>
        </div>

        <div style="display:flex;justify-content:space-between;margin-bottom:6px">
          <span style="font-size:13px;color:#6B7280">Faktiki xərc</span>
          <span style="font-family:var(--font-mono);font-size:14px;font-weight:600">{{ fmt(Number(b.totalSpent) || 0) }}</span>
        </div>
        <div v-if="b.budgetLimit" style="display:flex;justify-content:space-between;margin-bottom:14px">
          <span style="font-size:13px;color:#6B7280">Büdcə</span>
          <span style="font-family:var(--font-mono);font-size:13.5px;color:#6B7280">{{ fmt(Number(b.budgetLimit)) }}</span>
        </div>

        <div v-if="b.budgetLimit" class="progress-track" style="margin-bottom:8px">
          <div class="progress-bar" :style="{ width: utilPct(b) + '%', background: barColor(b) }" />
        </div>
        <div v-if="b.budgetLimit" style="display:flex;justify-content:space-between;font-size:12.5px">
          <span style="color:#9CA3AF">İstifadə</span>
          <span :style="`font-family:var(--font-mono);font-weight:600;color:${barColor(b)}`">{{ utilPct(b).toFixed(0) }}%</span>
        </div>

        <div v-if="b.address" style="margin-top:14px;padding-top:14px;border-top:1px solid #F3F4F6;font-size:12.5px;color:#9CA3AF">
          <UIcon name="i-lucide-map-pin" style="width:12px;height:12px;vertical-align:middle" />
          {{ b.address }}
        </div>
        <div v-if="b.groupName" style="margin-top:8px;font-size:12px;color:#6B7280">
          <UIcon name="i-lucide-users" style="width:12px;height:12px;vertical-align:middle" />
          {{ b.groupName }}
        </div>
      </div>

      <div v-if="!buildings.length" style="grid-column:1/-1;text-align:center;padding:60px;color:#9CA3AF;font-size:14px">
        Hələ ki bina yoxdur
      </div>
    </div>

    <!-- Building modal -->
    <BuildingModal
      v-if="modalOpen"
      :building="editBuilding"
      :groups="groups"
      @close="modalOpen = false"
      @saved="onSaved"
    />
  </div>
</template>

<script setup lang="ts">
definePageMeta({ layout: 'default' })

const config = useRuntimeConfig()
const auth = useAuthStore()
const router = useRouter()

const STATUS_META: Record<string, { label: string }> = {
  UPLOADED:       { label: 'Yükləndi' },
  OCR_PROCESSING: { label: 'Emal olunur' },
  PENDING_REVIEW: { label: 'Yoxlanılır' },
  APPROVED:       { label: 'Təsdiqləndi' },
  DISPUTED:       { label: 'Mübahisəli' },
  REJECTED:       { label: 'Rədd edildi' },
}

const statusOptions = [
  { value: '', label: 'Bütün statuslar' },
  ...Object.entries(STATUS_META).map(([v, m]) => ({ value: v, label: m.label })),
]

const filters = reactive({ buildingId: '', status: '', page: 0, perPage: 20 })
const uploadOpen = ref(false)

const headers = computed(() => ({ Authorization: `Bearer ${auth.token}` }))

const { data, refresh, pending } = await useAsyncData('expenses', () =>
  $fetch<{ content: unknown[]; totalElements: number }>('/api/v1/expenses/search', {
    baseURL: config.public.apiBase,
    method: 'POST',
    headers: headers.value,
    body: {
      page: filters.page,
      perPage: filters.perPage,
      buildingId: filters.buildingId || undefined,
      status: filters.status || undefined,
      sortBy: 'createdAt',
      sortDir: 'DESC',
    },
  }).catch(() => ({ content: [], totalElements: 0 })),
  { watch: [filters] },
)

const { data: buildings } = await useAsyncData('buildings-for-filter', () =>
  $fetch<{ content: unknown[] }>('/api/v1/buildings/search', {
    baseURL: config.public.apiBase,
    method: 'POST',
    headers: headers.value,
    body: { page: 0, perPage: 100 },
  }).catch(() => ({ content: [] })),
)

const expenses = computed(() => (data.value?.content ?? []) as Record<string, unknown>[])
const buildingList = computed(() => (buildings.value?.content ?? []) as Record<string, unknown>[])

function fmt(n: number) {
  return '₼' + n.toLocaleString('az-AZ')
}
</script>

<template>
  <div class="animate-page">
    <!-- Page header -->
    <div style="display:flex;align-items:flex-end;justify-content:space-between;gap:24px;margin-bottom:24px">
      <div>
        <div style="font-size:12px;font-weight:500;color:#9CA3AF;letter-spacing:0.04em;text-transform:uppercase;margin-bottom:6px">Maliyyə</div>
        <h1 style="font-size:26px;font-weight:700;letter-spacing:-0.02em;margin:0 0 4px">Xərclər</h1>
        <p style="font-size:14px;color:#6B7280;margin:0">{{ data?.totalElements ?? 0 }} qeyd tapıldı</p>
      </div>
      <button
        style="display:inline-flex;align-items:center;gap:8px;height:40px;padding:0 18px;background:#3D5AF1;color:#fff;border:none;border-radius:8px;font-size:14px;font-weight:600;cursor:pointer;white-space:nowrap"
        @click="uploadOpen = true"
      >
        <UIcon name="i-lucide-upload" style="width:16px;height:16px" />
        Qaimə yüklə
      </button>
    </div>

    <!-- Filters -->
    <div class="tk-card" style="padding:16px 18px;display:flex;gap:14px;flex-wrap:wrap;align-items:flex-end;margin-bottom:18px">
      <div style="flex:1;min-width:160px">
        <label style="display:block;font-size:12px;font-weight:500;color:#6B7280;margin-bottom:6px">Layihə</label>
        <select v-model="filters.buildingId" style="width:100%;height:38px;border:1px solid #D1D5DB;border-radius:7px;padding:0 12px;font-size:13.5px;background:#fff;color:#0A0A0A;cursor:pointer;font-family:inherit">
          <option value="">Bütün layihələr</option>
          <option v-for="b in buildingList" :key="String(b.id)" :value="b.id">{{ b.name }}</option>
        </select>
      </div>
      <div style="flex:1;min-width:150px">
        <label style="display:block;font-size:12px;font-weight:500;color:#6B7280;margin-bottom:6px">Status</label>
        <select v-model="filters.status" style="width:100%;height:38px;border:1px solid #D1D5DB;border-radius:7px;padding:0 12px;font-size:13.5px;background:#fff;color:#0A0A0A;cursor:pointer;font-family:inherit">
          <option v-for="opt in statusOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
        </select>
      </div>
    </div>

    <!-- Table -->
    <div class="tk-card" style="overflow:hidden">
      <div v-if="pending" style="padding:40px;text-align:center;color:#9CA3AF">
        <UIcon name="i-lucide-loader-2" style="width:24px;height:24px;animation:spin 1s linear infinite" />
      </div>
      <table v-else class="data-table">
        <thead>
          <tr>
            <th>Təchizatçı</th>
            <th>Layihə</th>
            <th>Kateqoriya</th>
            <th>Tarix</th>
            <th>Status</th>
            <th class="text-right">Məbləğ</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="e in expenses"
            :key="String(e.id)"
            @click="router.push('/expenses/' + e.id)"
          >
            <td style="font-weight:500">{{ e.supplierName || '—' }}</td>
            <td style="color:#6B7280">{{ e.buildingName || '—' }}</td>
            <td style="color:#6B7280;max-width:200px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">{{ e.categoryItemName || '—' }}</td>
            <td style="font-family:var(--font-mono);color:#6B7280;font-size:13px">{{ e.expenseDate ?? '—' }}</td>
            <td>
              <span :class="'badge badge-' + e.status">
                {{ STATUS_META[String(e.status)]?.label ?? e.status }}
              </span>
            </td>
            <td style="font-family:var(--font-mono);font-weight:500;text-align:right">{{ fmt(Number(e.amountBaseCurrency) || 0) }}</td>
          </tr>
          <tr v-if="!expenses.length">
            <td colspan="6" style="text-align:center;color:#9CA3AF;padding:40px">Xərc tapılmadı</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Upload modal -->
    <ExpenseUploadModal v-if="uploadOpen" :buildings="buildingList" @close="uploadOpen = false" @uploaded="refresh()" />
  </div>
</template>

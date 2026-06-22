<script setup lang="ts">
definePageMeta({ layout: 'default' })

const config = useRuntimeConfig()
const auth = useAuthStore()
const context = useContextStore()
const router = useRouter()

const STATUS_META: Record<string, { label: string; color: string }> = {
  UPLOADED:       { label: 'Yükləndi',     color: '#6B7280' },
  OCR_PROCESSING: { label: 'Emal olunur',  color: '#D97706' },
  PENDING_REVIEW: { label: 'Yoxlanılır',   color: '#2563EB' },
  APPROVED:       { label: 'Təsdiqləndi',  color: '#16A34A' },
  PAID:           { label: 'Ödənilib',     color: '#0891B2' },
  DISPUTED:       { label: 'Mübahisəli',   color: '#DC2626' },
  REJECTED:       { label: 'Rədd edildi',  color: '#DC2626' },
}

const statusOptions = [
  { value: '', label: 'Bütün statuslar' },
  ...Object.entries(STATUS_META).map(([v, m]) => ({ value: v, label: m.label })),
]

interface CategoryFilter { id: string; itemName: string; level1: string; level2: string; level3: string }

const filters = reactive({
  buildingId: context.activeBuildingId ?? '',
  status: '',
  categoryId: '',
  dateFrom: '',
  dateTo: '',
  page: 0,
  perPage: 20,
})
const selectedCategoryFilter = ref<CategoryFilter | null>(null)
const catFilterOpen = ref(false)
const uploadOpen = ref(false)
const manualOpen = ref(false)
const filtersOpen = ref(false)

watch(() => context.activeBuildingId, (id) => {
  filters.buildingId = id ?? ''
  filters.page = 0
})

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
      categoryId: filters.categoryId || undefined,
      dateFrom: filters.dateFrom || undefined,
      dateTo: filters.dateTo || undefined,
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

function onCategoryFilterSelected(cat: CategoryFilter) {
  selectedCategoryFilter.value = cat
  filters.categoryId = String(cat.id)
  catFilterOpen.value = false
}

function clearCategoryFilter() {
  selectedCategoryFilter.value = null
  filters.categoryId = ''
}

const activeFilterCount = computed(() => {
  let n = 0
  if (filters.buildingId) n++
  if (filters.status) n++
  if (filters.categoryId) n++
  if (filters.dateFrom) n++
  if (filters.dateTo) n++
  return n
})

function clearAllFilters() {
  filters.status = ''
  filters.categoryId = ''
  filters.dateFrom = ''
  filters.dateTo = ''
  selectedCategoryFilter.value = null
}

function fmt(n: number) {
  return '₼' + n.toLocaleString('az-AZ')
}

function shortId(id: unknown) {
  return 'QF-' + String(id).replace(/-/g, '').slice(0, 8).toUpperCase()
}
</script>

<template>
  <div class="animate-page">
    <!-- Page header -->
    <div class="page-hdr">
      <div>
        <div style="font-size:12px;font-weight:500;color:#9CA3AF;letter-spacing:0.04em;text-transform:uppercase;margin-bottom:6px">Maliyyə</div>
        <h1 style="font-size:26px;font-weight:700;letter-spacing:-0.02em;margin:0 0 4px">Xərclər</h1>
        <p style="font-size:14px;color:#6B7280;margin:0">{{ data?.totalElements ?? 0 }} qeyd tapıldı</p>
      </div>
      <div class="page-hdr-actions" style="display:flex;gap:10px">
        <button
          style="display:inline-flex;align-items:center;gap:8px;height:44px;padding:0 18px;background:#fff;color:#374151;border:1px solid #D1D5DB;border-radius:8px;font-size:14px;font-weight:600;cursor:pointer;white-space:nowrap;font-family:inherit"
          @click="manualOpen = true"
        >
          <UIcon name="i-lucide-plus" style="width:16px;height:16px" />
          <span class="md-hide">Yeni xərc</span>
          <span class="md-show">Yeni</span>
        </button>
        <button
          style="display:inline-flex;align-items:center;gap:8px;height:44px;padding:0 18px;background:#3D5AF1;color:#fff;border:none;border-radius:8px;font-size:14px;font-weight:600;cursor:pointer;white-space:nowrap;font-family:inherit"
          @click="uploadOpen = true"
        >
          <UIcon name="i-lucide-upload" style="width:16px;height:16px" />
          <span class="md-hide">Qaimə yüklə</span>
          <span class="md-show">Yüklə</span>
        </button>
      </div>
    </div>

    <!-- Filter toggle (mobile) -->
    <button
      class="filter-toggle-btn"
      style="display:none;align-items:center;gap:8px;height:44px;padding:0 16px;background:#fff;border:1px solid #D1D5DB;border-radius:8px;font-size:13.5px;font-weight:600;color:#374151;cursor:pointer;font-family:inherit;margin-bottom:12px;width:100%;justify-content:space-between;box-sizing:border-box"
      @click="filtersOpen = !filtersOpen"
    >
      <div style="display:flex;align-items:center;gap:8px">
        <UIcon name="i-lucide-sliders-horizontal" style="width:15px;height:15px" />
        Filterlər
        <span v-if="activeFilterCount > 0" style="background:#3D5AF1;color:#fff;font-size:11px;font-weight:700;padding:2px 7px;border-radius:9999px">{{ activeFilterCount }}</span>
      </div>
      <UIcon :name="filtersOpen ? 'i-lucide-chevron-up' : 'i-lucide-chevron-down'" style="width:15px;height:15px;color:#9CA3AF" />
    </button>

    <!-- Filters panel -->
    <div class="tk-card filter-bar" :class="{ open: filtersOpen }" style="margin-bottom:18px;border-radius:10px;padding:14px 16px">

      <!-- Building -->
      <div>
        <label style="display:block;font-size:12px;font-weight:600;color:#6B7280;margin-bottom:6px">Layihə</label>
        <select v-model="filters.buildingId" style="width:100%;height:40px;border:1px solid #E5E7EB;border-radius:7px;padding:0 12px;background:#fff;color:#0A0A0A;cursor:pointer;font-family:inherit;outline:none;font-size:13.5px">
          <option value="">Hamısı</option>
          <option v-for="b in buildingList" :key="String(b.id)" :value="b.id">{{ b.name }}</option>
        </select>
      </div>

      <!-- Status -->
      <div>
        <label style="display:block;font-size:12px;font-weight:600;color:#6B7280;margin-bottom:6px">Status</label>
        <select v-model="filters.status" style="width:100%;height:40px;border:1px solid #E5E7EB;border-radius:7px;padding:0 12px;background:#fff;color:#0A0A0A;cursor:pointer;font-family:inherit;outline:none;font-size:13.5px">
          <option v-for="opt in statusOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
        </select>
      </div>

      <!-- Category: compact picker button -->
      <div>
        <label style="display:block;font-size:12px;font-weight:600;color:#6B7280;margin-bottom:6px">Kateqoriya</label>
        <!-- Selected -->
        <div
          v-if="selectedCategoryFilter"
          style="display:flex;align-items:center;gap:8px;height:40px;padding:0 10px 0 12px;border:1px solid #C7D2FE;border-radius:7px;background:#EEF2FF;cursor:pointer;font-size:13.5px"
          @click="catFilterOpen = true"
        >
          <span style="flex:1;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;font-size:13px;font-weight:500;color:#0A0A0A">{{ selectedCategoryFilter.itemName }}</span>
          <button style="flex:none;width:20px;height:20px;border:none;background:none;cursor:pointer;display:flex;align-items:center;justify-content:center;padding:0" @click.stop="clearCategoryFilter">
            <UIcon name="i-lucide-x" style="width:13px;height:13px;color:#3D5AF1" />
          </button>
        </div>
        <!-- Empty -->
        <button
          v-else
          style="width:100%;height:40px;border:1px solid #E5E7EB;border-radius:7px;background:#fff;display:flex;align-items:center;justify-content:space-between;padding:0 10px 0 12px;cursor:pointer;font-family:inherit;box-sizing:border-box"
          @click="catFilterOpen = true"
        >
          <span style="font-size:13.5px;color:#9CA3AF">Bütün kateqoriyalar</span>
          <UIcon name="i-lucide-chevron-down" style="width:14px;height:14px;color:#D1D5DB" />
        </button>
      </div>

      <!-- Date from -->
      <div>
        <label style="display:block;font-size:12px;font-weight:600;color:#6B7280;margin-bottom:6px">Başlanğıc</label>
        <input v-model="filters.dateFrom" type="date" style="width:100%;height:40px;border:1px solid #E5E7EB;border-radius:7px;padding:0 10px;background:#fff;color:#0A0A0A;font-family:inherit;outline:none;font-size:13px;box-sizing:border-box">
      </div>

      <!-- Date to -->
      <div>
        <label style="display:block;font-size:12px;font-weight:600;color:#6B7280;margin-bottom:6px">Son tarix</label>
        <input v-model="filters.dateTo" type="date" style="width:100%;height:40px;border:1px solid #E5E7EB;border-radius:7px;padding:0 10px;background:#fff;color:#0A0A0A;font-family:inherit;outline:none;font-size:13px;box-sizing:border-box">
      </div>

      <!-- Clear all -->
      <div v-if="activeFilterCount > 0" class="filter-clear" style="align-self:end">
        <button
          style="width:100%;height:40px;padding:0 14px;border:1px solid #E5E7EB;border-radius:7px;background:#fff;color:#6B7280;font-size:13px;font-weight:500;cursor:pointer;font-family:inherit;display:inline-flex;align-items:center;justify-content:center;gap:6px;box-sizing:border-box"
          @click="clearAllFilters"
        >
          <UIcon name="i-lucide-x" style="width:13px;height:13px" />
          Təmizlə
        </button>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="pending" style="padding:40px;text-align:center;color:#9CA3AF">
      <UIcon name="i-lucide-loader-2" style="width:24px;height:24px;animation:spin 1s linear infinite" />
    </div>

    <template v-else>
      <!-- Desktop table -->
      <div class="tk-card md-hide" style="overflow-x:auto">
        <table class="data-table" style="min-width:600px">
          <thead>
            <tr>
              <th>Qaimə №</th>
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
              style="cursor:pointer"
              @click="router.push('/expenses/' + e.id)"
            >
              <td style="font-family:var(--font-mono);font-size:12.5px;color:#141F5E;font-weight:600">{{ shortId(e.id) }}</td>
              <td style="font-weight:500">{{ e.supplierName || '—' }}</td>
              <td style="color:#6B7280">{{ e.buildingName || '—' }}</td>
              <td style="color:#6B7280;max-width:160px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">{{ e.categoryItemName || '—' }}</td>
              <td style="font-family:var(--font-mono);color:#6B7280;font-size:13px">{{ e.expenseDate ?? '—' }}</td>
              <td><span :class="'badge badge-' + e.status">{{ STATUS_META[String(e.status)]?.label ?? e.status }}</span></td>
              <td style="font-family:var(--font-mono);font-weight:500;text-align:right">{{ fmt(Number(e.amountBaseCurrency) || 0) }}</td>
            </tr>
            <tr v-if="!expenses.length">
              <td colspan="7" style="text-align:center;color:#9CA3AF;padding:40px">Xərc tapılmadı</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Mobile card list -->
      <div class="tk-card md-show" style="overflow:hidden">
        <div class="m-list">
          <div
            v-for="e in expenses"
            :key="String(e.id)"
            class="m-row"
            @click="router.push('/expenses/' + e.id)"
          >
            <div class="m-row-left">
              <div class="m-row-title">{{ e.supplierName || '—' }}</div>
              <div class="m-row-sub">{{ e.buildingName || '—' }}</div>
              <div class="m-row-sub" style="margin-top:1px">{{ e.categoryItemName || '—' }}</div>
            </div>
            <div class="m-row-right">
              <div class="m-row-amount">{{ fmt(Number(e.amountBaseCurrency) || 0) }}</div>
              <div class="m-row-date">{{ e.expenseDate ?? '—' }}</div>
              <span :class="'badge badge-' + e.status" style="font-size:10px;margin-top:2px">
                {{ STATUS_META[String(e.status)]?.label ?? e.status }}
              </span>
            </div>
          </div>
          <div v-if="!expenses.length" style="padding:40px;text-align:center;color:#9CA3AF;font-size:14px">
            Xərc tapılmadı
          </div>
        </div>
      </div>
    </template>

    <!-- Upload modal -->
    <ExpenseUploadModal v-if="uploadOpen" :buildings="buildingList" @close="uploadOpen = false" @uploaded="refresh()" />
    <!-- Manual expense modal -->
    <ManualExpenseModal v-if="manualOpen" :buildings="buildingList" @close="manualOpen = false" @saved="refresh()" />

    <!-- Category filter bottom sheet -->
    <Teleport to="body">
      <Transition name="sheet">
        <div v-if="catFilterOpen" class="s-overlay">
          <div class="s-backdrop" @click="catFilterOpen = false" />
          <div class="s-panel s-panel--tall">
            <div class="s-handle"><div class="s-handle-bar" /></div>
            <div class="s-head">
              <h3 class="s-title">Kateqoriya seçin</h3>
              <button class="s-close-btn" @click="catFilterOpen = false">
                <UIcon name="i-lucide-x" style="width:15px;height:15px;color:#6B7280" />
              </button>
            </div>
            <div class="s-body">
              <CategoryPicker v-model="filters.categoryId" @selected="onCategoryFilterSelected" />
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
/* Category picker bottom sheet (same pattern as ManualExpenseModal) */
.s-overlay {
  position: fixed; inset: 0; z-index: 200;
  display: flex; flex-direction: column; justify-content: flex-end; align-items: stretch;
}
.s-backdrop { position: absolute; inset: 0; background: rgba(0,0,0,0.5); }
.s-panel {
  position: relative; background: #fff;
  border-radius: 16px 16px 0 0;
  box-shadow: 0 -4px 30px rgba(0,0,0,0.12);
}
.s-panel--tall { max-height: 88vh; display: flex; flex-direction: column; }
.s-handle { display: flex; justify-content: center; padding: 10px 0 2px; flex: none; }
.s-handle-bar { width: 36px; height: 4px; border-radius: 2px; background: #E5E7EB; }
.s-head { display: flex; align-items: center; justify-content: space-between; padding: 8px 20px 14px; flex: none; }
.s-title { font-size: 16px; font-weight: 700; margin: 0; }
.s-close-btn { width: 30px; height: 30px; border-radius: 8px; border: 1px solid #E5E7EB; background: #fff; cursor: pointer; display: flex; align-items: center; justify-content: center; }
.s-body { flex: 1; overflow-y: auto; padding: 0 20px 32px; }

@media (min-width: 640px) {
  .s-overlay { justify-content: center; align-items: center; padding: 16px; }
  .s-panel { border-radius: 12px; width: 100%; max-width: 540px; box-shadow: 0 20px 60px rgba(0,0,0,0.2); }
  .s-panel--tall { max-height: 80vh; }
  .s-handle { display: none; }
  .s-head { padding: 18px 24px 14px; }
  .s-body { padding: 0 24px 24px; }
}

.sheet-enter-active, .sheet-leave-active { transition: none; }
.sheet-enter-active .s-backdrop, .sheet-leave-active .s-backdrop { transition: opacity 0.22s ease; }
.sheet-enter-from .s-backdrop, .sheet-leave-to .s-backdrop { opacity: 0; }
.sheet-enter-active .s-panel, .sheet-leave-active .s-panel { transition: transform 0.22s cubic-bezier(0.32,0.72,0,1), opacity 0.18s ease; }
.sheet-enter-from .s-panel, .sheet-leave-to .s-panel { transform: translateY(100%); opacity: 1; }
@media (min-width: 640px) {
  .sheet-enter-from .s-panel, .sheet-leave-to .s-panel { transform: scale(0.95) translateY(0); opacity: 0; }
  .sheet-enter-active .s-panel, .sheet-leave-active .s-panel { transition: transform 0.18s cubic-bezier(0.34,1.2,0.64,1), opacity 0.15s ease; }
}
</style>

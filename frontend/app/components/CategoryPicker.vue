<script setup lang="ts">
interface Category {
  id: string
  itemCode: string
  level1: string
  level2: string
  level3: string
  itemName: string
}

const props = defineProps<{ modelValue: string }>()
const emit = defineEmits<{
  'update:modelValue': [value: string]
  'selected': [cat: Category]
}>()

const config = useRuntimeConfig()
const auth = useAuthStore()
const headers = computed(() => ({ Authorization: `Bearer ${auth.token}` }))

// ── All categories for hierarchy dropdowns (GET returns full list) ──────────
const allCategories = ref<Category[]>([])
onMounted(async () => {
  try {
    allCategories.value = await $fetch<Category[]>('/api/v1/categories', {
      baseURL: config.public.apiBase,
      headers: headers.value,
    })
  } catch { /* fail silently — filters just won't populate */ }
})

// ── Filter state ────────────────────────────────────────────────────────────
const showFilters = ref(false)
const filterLevel1 = ref('')
const filterLevel2 = ref('')
const filterLevel3 = ref('')
const keyword = ref('')

const activeFilterCount = computed(() =>
  [filterLevel1.value, filterLevel2.value, filterLevel3.value].filter(Boolean).length,
)

// Cascading distinct values from allCategories
const level1Options = computed(() =>
  [...new Set(allCategories.value.map(c => c.level1).filter(Boolean))].sort(),
)

const level2Options = computed(() => {
  const src = filterLevel1.value
    ? allCategories.value.filter(c => c.level1 === filterLevel1.value)
    : allCategories.value
  return [...new Set(src.map(c => c.level2).filter(Boolean))].sort()
})

const level3Options = computed(() => {
  let src = allCategories.value
  if (filterLevel1.value) src = src.filter(c => c.level1 === filterLevel1.value)
  if (filterLevel2.value) src = src.filter(c => c.level2 === filterLevel2.value)
  return [...new Set(src.map(c => c.level3).filter(Boolean))].sort()
})

// ── Search detection: if keyword looks like a code (e.g. MEP.05), use itemCode ──
const isCodeSearch = computed(() => {
  const s = keyword.value.trim()
  return s.length > 0 && /^[A-Z0-9]+(\.[0-9]+)*$/i.test(s) && /[0-9.]/.test(s)
})

// ── Paginated results with infinite scroll ──────────────────────────────────
const results = ref<Category[]>([])
const currentPage = ref(0)
const hasMore = ref(true)
const loading = ref(false)

function buildBody(page: number): Record<string, unknown> {
  const body: Record<string, unknown> = {
    page,
    perPage: filterLevel3.value ? 50 : 20, // load more per page when level3 filtering is client-side
    isActive: true,
  }
  if (filterLevel1.value) body.level1s = [filterLevel1.value]
  if (filterLevel2.value) body.level2s = [filterLevel2.value]
  const kw = keyword.value.trim()
  if (kw) {
    if (isCodeSearch.value) body.itemCode = kw
    else body.itemName = kw
  }
  return body
}

function applyLevel3(items: Category[]): Category[] {
  return filterLevel3.value ? items.filter(c => c.level3 === filterLevel3.value) : items
}

async function doSearch() {
  loading.value = true
  currentPage.value = 0
  results.value = []
  try {
    const res = await $fetch<{ content: Category[]; last: boolean }>('/api/v1/categories/search', {
      baseURL: config.public.apiBase,
      method: 'POST',
      headers: headers.value,
      body: buildBody(0),
    })
    results.value = applyLevel3(res.content)
    hasMore.value = !res.last

    // When level3 filtering creates sparse results, auto-fill by loading more pages
    while (hasMore.value && results.value.length < 6) {
      const nextPage = currentPage.value + 1
      const more = await $fetch<{ content: Category[]; last: boolean }>('/api/v1/categories/search', {
        baseURL: config.public.apiBase,
        method: 'POST',
        headers: headers.value,
        body: buildBody(nextPage),
      })
      currentPage.value = nextPage
      results.value = [...results.value, ...applyLevel3(more.content)]
      hasMore.value = !more.last
    }
  } catch {
    results.value = []
    hasMore.value = false
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  if (loading.value || !hasMore.value) return
  loading.value = true
  const nextPage = currentPage.value + 1
  try {
    const res = await $fetch<{ content: Category[]; last: boolean }>('/api/v1/categories/search', {
      baseURL: config.public.apiBase,
      method: 'POST',
      headers: headers.value,
      body: buildBody(nextPage),
    })
    currentPage.value = nextPage
    results.value = [...results.value, ...applyLevel3(res.content)]
    hasMore.value = !res.last
  } finally {
    loading.value = false
  }
}

// ── Debounce keyword → re-search ────────────────────────────────────────────
let debounceTimer: ReturnType<typeof setTimeout> | null = null
watch(keyword, () => {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(doSearch, 320)
})

onMounted(doSearch)

// ── Cascade handlers ─────────────────────────────────────────────────────────
function onLevel1Change() { filterLevel2.value = ''; filterLevel3.value = ''; doSearch() }
function onLevel2Change() { filterLevel3.value = ''; doSearch() }

function clearFilters() {
  filterLevel1.value = ''
  filterLevel2.value = ''
  filterLevel3.value = ''
  doSearch()
}

// ── IntersectionObserver — sentinel at bottom of scrollable list ─────────────
const scrollContainerRef = ref<HTMLElement | null>(null)
const sentinelRef = ref<HTMLElement | null>(null)
let observer: IntersectionObserver | null = null

watch([scrollContainerRef, sentinelRef], ([container, el]) => {
  observer?.disconnect()
  if (!container || !el) return
  observer = new IntersectionObserver(
    (entries) => { if (entries[0]?.isIntersecting) loadMore() },
    { root: container, rootMargin: '80px' },
  )
  observer.observe(el)
})

onUnmounted(() => observer?.disconnect())

// ── Selected category (looked up from allCategories OR results) ──────────────
const selectedCategory = computed(() => {
  if (!props.modelValue) return null
  return (
    allCategories.value.find(c => String(c.id) === props.modelValue) ??
    results.value.find(c => String(c.id) === props.modelValue) ??
    null
  )
})

function select(cat: Category) { emit('update:modelValue', String(cat.id)); emit('selected', cat) }
function clearSelection() { emit('update:modelValue', '') }
</script>

<template>
  <div style="display:flex;flex-direction:column;gap:10px">

    <!-- Selected chip ─────────────────────────────────────────────────────── -->
    <div
      v-if="selectedCategory"
      style="display:flex;align-items:center;gap:10px;padding:10px 12px;background:#EEF2FF;border:1.5px solid #C7D2FE;border-radius:8px"
    >
      <UIcon name="i-lucide-check-circle-2" style="width:16px;height:16px;color:#3D5AF1;flex:none" />
      <div style="flex:1;min-width:0">
        <div style="font-size:10.5px;font-weight:600;color:#6B7280;text-transform:uppercase;letter-spacing:0.05em;margin-bottom:1px">Seçilmiş</div>
        <div style="display:flex;align-items:baseline;gap:7px;flex-wrap:wrap">
          <span style="font-family:var(--font-mono);font-size:12px;font-weight:700;color:#3D5AF1;white-space:nowrap">{{ selectedCategory.itemCode }}</span>
          <span style="font-size:13px;font-weight:500;color:#0A0A0A;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">{{ selectedCategory.itemName }}</span>
        </div>
        <div style="font-size:11px;color:#9CA3AF;margin-top:1px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">
          {{ [selectedCategory.level1, selectedCategory.level2, selectedCategory.level3].filter(Boolean).join(' › ') }}
        </div>
      </div>
      <button
        style="flex:none;width:24px;height:24px;border-radius:6px;border:none;background:rgba(61,90,241,0.1);cursor:pointer;display:flex;align-items:center;justify-content:center;color:#3D5AF1"
        title="Seçimi ləğv et"
        @click="clearSelection"
      >
        <UIcon name="i-lucide-x" style="width:13px;height:13px" />
      </button>
    </div>

    <!-- Search row ──────────────────────────────────────────────────────────── -->
    <div style="display:flex;gap:8px">
      <div style="flex:1;position:relative">
        <UIcon name="i-lucide-search" style="position:absolute;left:10px;top:50%;transform:translateY(-50%);width:15px;height:15px;color:#9CA3AF;pointer-events:none" />
        <input
          v-model="keyword"
          type="text"
          placeholder="Ad və ya kod ilə axtar..."
          style="width:100%;height:38px;padding:0 48px 0 32px;border:1px solid #D1D5DB;border-radius:8px;font-size:13.5px;color:#0A0A0A;font-family:inherit;outline:none;box-sizing:border-box;transition:border-color .15s"
        >
        <span
          v-if="keyword && isCodeSearch"
          style="position:absolute;right:8px;top:50%;transform:translateY(-50%);font-size:10px;font-weight:700;letter-spacing:0.04em;color:#3D5AF1;background:#EEF2FF;border:1px solid #C7D2FE;padding:1px 6px;border-radius:4px;white-space:nowrap"
        >KOD</span>
        <span
          v-else-if="keyword && !isCodeSearch"
          style="position:absolute;right:8px;top:50%;transform:translateY(-50%);font-size:10px;font-weight:700;letter-spacing:0.04em;color:#059669;background:#ECFDF5;border:1px solid #6EE7B7;padding:1px 6px;border-radius:4px;white-space:nowrap"
        >AD</span>
      </div>

      <!-- Filter toggle -->
      <button
        :style="`display:inline-flex;align-items:center;gap:5px;height:38px;padding:0 12px;border-radius:8px;font-size:13px;font-weight:600;cursor:pointer;font-family:inherit;white-space:nowrap;transition:all .15s;${showFilters ? 'background:#EEF2FF;color:#3D5AF1;border:1.5px solid #C7D2FE' : 'background:#fff;color:#374151;border:1px solid #D1D5DB'}`"
        @click="showFilters = !showFilters"
      >
        <UIcon name="i-lucide-sliders-horizontal" style="width:14px;height:14px" />
        Süzgəc
        <span
          v-if="activeFilterCount"
          style="background:#3D5AF1;color:#fff;font-size:10px;font-weight:700;border-radius:9999px;padding:0 5px;min-width:16px;height:16px;display:inline-flex;align-items:center;justify-content:center;line-height:1"
        >{{ activeFilterCount }}</span>
      </button>
    </div>

    <!-- Filter panel ─────────────────────────────────────────────────────────── -->
    <div
      v-if="showFilters"
      style="padding:12px 14px;background:#F9FAFB;border:1px solid #E5E7EB;border-radius:8px;display:flex;flex-direction:column;gap:10px"
    >
      <div class="cat-filter-grid" style="display:grid;grid-template-columns:1fr 1fr 1fr;gap:8px">
        <!-- Level 1 -->
        <div>
          <label style="display:block;font-size:10.5px;font-weight:700;color:#6B7280;text-transform:uppercase;letter-spacing:0.06em;margin-bottom:4px">Səviyyə 1</label>
          <select
            v-model="filterLevel1"
            style="width:100%;height:32px;padding:0 8px;border:1px solid #D1D5DB;border-radius:6px;font-size:12.5px;background:#fff;color:#0A0A0A;font-family:inherit;outline:none"
            @change="onLevel1Change"
          >
            <option value="">— Hamısı —</option>
            <option v-for="v in level1Options" :key="v" :value="v">{{ v }}</option>
          </select>
        </div>

        <!-- Level 2 -->
        <div>
          <label style="display:block;font-size:10.5px;font-weight:700;color:#6B7280;text-transform:uppercase;letter-spacing:0.06em;margin-bottom:4px">Səviyyə 2</label>
          <select
            v-model="filterLevel2"
            :disabled="level2Options.length === 0"
            style="width:100%;height:32px;padding:0 8px;border:1px solid #D1D5DB;border-radius:6px;font-size:12.5px;background:#fff;color:#0A0A0A;font-family:inherit;outline:none;disabled:opacity:0.5"
            @change="onLevel2Change"
          >
            <option value="">— Hamısı —</option>
            <option v-for="v in level2Options" :key="v" :value="v">{{ v }}</option>
          </select>
        </div>

        <!-- Level 3 (client-side) -->
        <div>
          <div style="display:flex;align-items:center;gap:4px;margin-bottom:4px">
            <label style="font-size:10.5px;font-weight:700;color:#6B7280;text-transform:uppercase;letter-spacing:0.06em">Səviyyə 3</label>
            <span style="font-size:9.5px;color:#9CA3AF;background:#F3F4F6;border-radius:3px;padding:0 4px">client</span>
          </div>
          <select
            v-model="filterLevel3"
            :disabled="level3Options.length === 0"
            style="width:100%;height:32px;padding:0 8px;border:1px solid #D1D5DB;border-radius:6px;font-size:12.5px;background:#fff;color:#0A0A0A;font-family:inherit;outline:none"
            @change="doSearch"
          >
            <option value="">— Hamısı —</option>
            <option v-for="v in level3Options" :key="v" :value="v">{{ v }}</option>
          </select>
        </div>
      </div>

      <div v-if="activeFilterCount" style="display:flex;justify-content:flex-end">
        <button
          style="font-size:12px;color:#DC2626;background:none;border:none;cursor:pointer;padding:0;font-family:inherit;font-weight:500"
          @click="clearFilters"
        >
          Süzgəcləri sıfırla
        </button>
      </div>
    </div>

    <!-- Results list ──────────────────────────────────────────────────────────── -->
    <div
      ref="scrollContainerRef"
      style="border:1px solid #E5E7EB;border-radius:8px;overflow-y:auto;max-height:264px;position:relative"
    >
      <!-- Initial loading -->
      <div v-if="loading && results.length === 0" style="display:flex;align-items:center;justify-content:center;padding:30px;gap:8px;color:#9CA3AF">
        <UIcon name="i-lucide-loader-2" style="width:16px;height:16px;animation:spin 1s linear infinite" />
        <span style="font-size:13px">Axtarılır...</span>
      </div>

      <!-- Empty -->
      <div v-else-if="!loading && results.length === 0" style="padding:30px;text-align:center">
        <UIcon name="i-lucide-search-x" style="width:24px;height:24px;color:#D1D5DB;display:block;margin:0 auto 8px" />
        <p style="font-size:13px;color:#9CA3AF;margin:0">Nəticə tapılmadı</p>
        <p v-if="keyword || activeFilterCount" style="font-size:12px;color:#D1D5DB;margin:4px 0 0">Axtarış kriteriyalarını dəyişdirin</p>
      </div>

      <!-- Items -->
      <template v-else>
        <div
          v-for="(cat, idx) in results"
          :key="cat.id"
          :style="`display:flex;align-items:center;gap:10px;padding:9px 14px;cursor:pointer;border-bottom:1px solid #F3F4F6;transition:background .1s;${props.modelValue === String(cat.id) ? 'background:#EEF2FF' : ''}`"
          @mouseenter="(e) => { if (props.modelValue !== String(cat.id)) (e.currentTarget as HTMLElement).style.background = '#F9FAFB' }"
          @mouseleave="(e) => { if (props.modelValue !== String(cat.id)) (e.currentTarget as HTMLElement).style.background = '' }"
          @click="select(cat)"
        >
          <UIcon
            :name="props.modelValue === String(cat.id) ? 'i-lucide-check-circle-2' : 'i-lucide-circle'"
            :style="`width:15px;height:15px;flex:none;${props.modelValue === String(cat.id) ? 'color:#3D5AF1' : 'color:#D1D5DB'}`"
          />
          <div style="flex:1;min-width:0">
            <div style="display:flex;align-items:baseline;gap:8px">
              <span class="cat-code" style="font-family:var(--font-mono);font-size:11.5px;font-weight:600;color:#3D5AF1;flex:none">{{ cat.itemCode }}</span>
              <span style="font-size:13px;font-weight:500;color:#0A0A0A;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">{{ cat.itemName }}</span>
            </div>
            <div style="font-size:11px;color:#9CA3AF;margin-top:1px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">
              {{ [cat.level1, cat.level2, cat.level3].filter(Boolean).join(' › ') }}
            </div>
          </div>
        </div>

        <!-- Sentinel for infinite scroll -->
        <div ref="sentinelRef" style="height:1px" />

        <!-- Loading more -->
        <div v-if="loading" style="display:flex;align-items:center;justify-content:center;padding:10px;gap:6px;color:#9CA3AF">
          <UIcon name="i-lucide-loader-2" style="width:12px;height:12px;animation:spin 1s linear infinite" />
          <span style="font-size:12px">Yüklənir...</span>
        </div>

        <!-- End marker -->
        <div v-else-if="!hasMore" style="text-align:center;padding:8px;font-size:11px;color:#D1D5DB">
          {{ results.length }} kateqoriya
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
@media (max-width: 639px) {
  .cat-code { display: none; }
}
</style>

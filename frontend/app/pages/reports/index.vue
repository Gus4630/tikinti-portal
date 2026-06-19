<script setup lang="ts">
definePageMeta({ layout: 'default' })

const config = useRuntimeConfig()
const auth = useAuthStore()
const headers = computed(() => ({ Authorization: `Bearer ${auth.token}` }))

// Building selector
const { data: buildingsData } = await useAsyncData('report-buildings', () =>
  $fetch<{ content: Record<string, unknown>[] }>('/api/v1/buildings/search', {
    baseURL: config.public.apiBase,
    method: 'POST',
    headers: headers.value,
    body: { page: 0, perPage: 100, sortBy: 'name', sortDir: 'ASC' },
  }).catch(() => ({ content: [] })),
)
const buildings = computed(() => buildingsData.value?.content ?? [])
const selectedBuildingId = ref<string>('')

const activeTab = ref('budget')
const tabs = [
  { id: 'budget',   label: 'Büdcə vs Faktiki' },
  { id: 'cats',     label: 'Kateqoriyalar' },
  { id: 'ledger',   label: 'Təchizatçı dəftəri' },
  { id: 'trend',    label: 'Aylıq trend' },
  { id: 'm2',       label: 'm²-ə düşən xərc' },
]

// Budget tab: per-building summary using buildings search (all or filtered)
const budgetRows = computed(() => {
  const list = (buildingsData.value?.content ?? []) as Record<string, unknown>[]
  return list.map(b => {
    const budget = Number(b.budgetLimit) || 0
    const actual = Number(b.totalSpent) || 0
    const variance = budget - actual
    const util = budget > 0 ? Math.round((actual / budget) * 100) : 0
    return { name: b.name, budget, actual, variance, util }
  })
})

const chartMax = computed(() => {
  const vals = budgetRows.value.flatMap(r => [r.budget, r.actual])
  return Math.max(...vals, 1)
})

// Per-building reports (fetched when building is selected)
const catRows = ref<Record<string, unknown>[]>([])
const ledgerRows = ref<Record<string, unknown>[]>([])
const trendData = ref<Record<string, unknown>[]>([])
const m2Data = ref<Record<string, unknown> | null>(null)
const reportLoading = ref(false)

async function loadBuildingReports(id: string) {
  if (!id) {
    catRows.value = []
    ledgerRows.value = []
    trendData.value = []
    m2Data.value = null
    return
  }
  reportLoading.value = true
  try {
    const h = headers.value
    const base = config.public.apiBase
    const [cats, ledger, trend, m2] = await Promise.allSettled([
      $fetch<Record<string, unknown>[]>(`/api/v1/reports/budget-vs-actual?buildingId=${id}`, { baseURL: base, headers: h }),
      $fetch<Record<string, unknown>[]>(`/api/v1/reports/supplier-ledger?buildingId=${id}`, { baseURL: base, headers: h }),
      $fetch<Record<string, unknown>[]>(`/api/v1/reports/monthly-trend?buildingId=${id}`, { baseURL: base, headers: h }),
      $fetch<Record<string, unknown>>(`/api/v1/reports/cost-per-m2?buildingId=${id}`, { baseURL: base, headers: h }),
    ])
    catRows.value = cats.status === 'fulfilled' ? (cats.value as Record<string, unknown>[]).filter(r => Number(r.budgetAmount) > 0 || Number(r.actualSpend) > 0) : []
    ledgerRows.value = ledger.status === 'fulfilled' ? ledger.value as Record<string, unknown>[] : []
    trendData.value = trend.status === 'fulfilled' ? trend.value as Record<string, unknown>[] : []
    m2Data.value = m2.status === 'fulfilled' ? m2.value as Record<string, unknown> : null
  } finally {
    reportLoading.value = false
  }
}

watch(selectedBuildingId, (id) => loadBuildingReports(id))

// Category donut helpers
const COLORS = ['#3D5AF1','#F59E0B','#10B981','#EF4444','#8B5CF6','#06B6D4','#EC4899','#F97316']
const catTotal = computed(() => catRows.value.reduce((s, r) => s + Number(r.actualSpend), 0))
const catLegend = computed(() => {
  return catRows.value
    .filter(r => Number(r.actualSpend) > 0)
    .sort((a, b) => Number(b.actualSpend) - Number(a.actualSpend))
    .slice(0, 8)
    .map((r, i) => ({
      name: String(r.itemName ?? r.level2 ?? r.level1 ?? '—'),
      val: Number(r.actualSpend),
      pct: catTotal.value > 0 ? ((Number(r.actualSpend) / catTotal.value) * 100).toFixed(1) + '%' : '0%',
      barW: catTotal.value > 0 ? ((Number(r.actualSpend) / catTotal.value) * 100).toFixed(1) + '%' : '0%',
      color: COLORS[i % COLORS.length],
    }))
})

// Donut SVG path generator
const donutSegments = computed(() => {
  const r = 80, cx = 92, cy = 92
  let offset = 0
  return catLegend.value.map(item => {
    const pct = catTotal.value > 0 ? item.val / catTotal.value : 0
    const angle = pct * 2 * Math.PI
    const x1 = cx + r * Math.sin(offset)
    const y1 = cy - r * Math.cos(offset)
    offset += angle
    const x2 = cx + r * Math.sin(offset)
    const y2 = cy - r * Math.cos(offset)
    const large = angle > Math.PI ? 1 : 0
    return { d: `M${cx},${cy} L${x1},${y1} A${r},${r} 0 ${large},1 ${x2},${y2} Z`, color: item.color }
  })
})

// Trend bar chart
const trendMax = computed(() => Math.max(...trendData.value.map(r => Number(r.total)), 1))
const trendBars = computed(() => trendData.value.map(r => ({
  month: String(r.month).slice(5), // "YYYY-MM" → "MM"
  h: Math.round((Number(r.total) / trendMax.value) * 170),
  val: Number(r.total),
})))

// Export
const exportYear = ref(new Date().getFullYear())
const exportMonth = ref(new Date().getMonth() + 1)
async function doExport() {
  if (!selectedBuildingId.value) return
  const url = `${config.public.apiBase}/api/v1/reports/export/monthly?buildingId=${selectedBuildingId.value}&year=${exportYear.value}&month=${exportMonth.value}`
  const res = await fetch(url, { headers: headers.value })
  const blob = await res.blob()
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = `expenses-${exportYear.value}-${String(exportMonth.value).padStart(2, '0')}.xlsx`
  a.click()
}

function fmt(n: number) {
  return '₼' + n.toLocaleString('az-AZ', { minimumFractionDigits: 2 })
}
function varianceColor(v: number) {
  return v >= 0 ? '#16A34A' : '#DC2626'
}
</script>

<template>
  <div class="animate-page">
    <div style="display:flex;align-items:flex-end;justify-content:space-between;gap:24px;margin-bottom:24px">
      <div>
        <div style="font-size:12px;font-weight:500;color:#9CA3AF;letter-spacing:0.04em;text-transform:uppercase;margin-bottom:6px">Analitika</div>
        <h1 style="font-size:26px;font-weight:700;letter-spacing:-0.02em;margin:0 0 4px">Hesabatlar</h1>
        <p style="font-size:14px;color:#6B7280;margin:0">Büdcə, dəftər və trend analitikası</p>
      </div>

      <!-- Building selector -->
      <div style="display:flex;align-items:center;gap:10px;flex-shrink:0">
        <select
          v-model="selectedBuildingId"
          style="height:38px;border:1px solid #D1D5DB;border-radius:8px;padding:0 32px 0 12px;font-size:14px;background:#fff;color:#0A0A0A;font-family:inherit;min-width:200px"
        >
          <option value="">Bütün layihələr</option>
          <option v-for="b in buildings" :key="String(b.id)" :value="b.id">{{ b.name }}</option>
        </select>
        <button
          v-if="selectedBuildingId"
          style="display:inline-flex;align-items:center;gap:7px;height:38px;padding:0 16px;background:#fff;color:#374151;border:1px solid #D1D5DB;border-radius:8px;font-size:14px;font-weight:500;cursor:pointer;font-family:inherit"
          @click="doExport"
        >
          <UIcon name="i-lucide-download" style="width:15px;height:15px" />
          Excel
        </button>
      </div>
    </div>

    <!-- Tabs -->
    <div style="display:flex;border-bottom:1px solid #E5E7EB;margin-bottom:22px">
      <button
        v-for="tab in tabs"
        :key="tab.id"
        :style="{
          padding: '10px 18px',
          fontSize: '14px',
          fontWeight: '600',
          border: 'none',
          background: 'none',
          cursor: 'pointer',
          fontFamily: 'inherit',
          color: activeTab === tab.id ? '#141F5E' : '#6B7280',
          borderBottom: activeTab === tab.id ? '2px solid #3D5AF1' : '2px solid transparent',
          marginBottom: '-1px',
        }"
        @click="activeTab = tab.id"
      >{{ tab.label }}</button>
    </div>

    <div v-if="reportLoading" style="text-align:center;padding:40px;color:#9CA3AF">
      <UIcon name="i-lucide-loader-2" style="width:24px;height:24px;animation:spin 1s linear infinite" />
    </div>

    <!-- BUDGET TAB -->
    <div v-else-if="activeTab === 'budget'">
      <!-- Bar chart -->
      <div v-if="budgetRows.length" class="tk-card" style="padding:22px;margin-bottom:18px">
        <h3 style="font-size:15px;font-weight:600;margin:0 0 20px">Büdcə vs Faktiki — layihələr üzrə</h3>
        <div style="display:flex;align-items:flex-end;justify-content:space-around;height:220px;gap:8px;padding:0 8px">
          <div v-for="r in budgetRows" :key="String(r.name)" style="display:flex;flex-direction:column;align-items:center;gap:9px;flex:1">
            <div style="display:flex;align-items:flex-end;gap:5px;height:190px">
              <div :style="`width:20px;background:#C7D2FE;border-radius:3px 3px 0 0;height:${Math.round(r.budget / chartMax * 190)}px`" />
              <div :style="`width:20px;border-radius:3px 3px 0 0;background:${r.actual > r.budget ? '#F59E0B' : '#3D5AF1'};height:${Math.round(r.actual / chartMax * 190)}px`" />
            </div>
            <span style="font-size:11px;color:#6B7280;font-weight:500;text-align:center;max-width:60px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">{{ r.name }}</span>
          </div>
        </div>
        <div style="display:flex;align-items:center;gap:20px;margin-top:14px;justify-content:center">
          <div style="display:flex;align-items:center;gap:6px"><span style="width:12px;height:12px;background:#C7D2FE;border-radius:2px;display:inline-block"/><span style="font-size:12px;color:#6B7280">Büdcə</span></div>
          <div style="display:flex;align-items:center;gap:6px"><span style="width:12px;height:12px;background:#3D5AF1;border-radius:2px;display:inline-block"/><span style="font-size:12px;color:#6B7280">Faktiki</span></div>
        </div>
      </div>

      <!-- Table -->
      <div class="tk-card" style="overflow:hidden">
        <table class="data-table">
          <thead>
            <tr>
              <th>Layihə</th>
              <th class="text-right">Büdcə</th>
              <th class="text-right">Faktiki</th>
              <th class="text-right">Fərq</th>
              <th class="text-right">İstifadə</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in budgetRows" :key="String(r.name)">
              <td style="font-weight:500">{{ r.name }}</td>
              <td style="font-family:var(--font-mono);text-align:right;color:#6B7280">{{ r.budget ? fmt(r.budget) : '—' }}</td>
              <td style="font-family:var(--font-mono);text-align:right">{{ fmt(r.actual) }}</td>
              <td :style="`font-family:var(--font-mono);text-align:right;font-weight:600;color:${varianceColor(r.variance)}`">
                {{ r.budget ? (r.variance >= 0 ? '+' : '') + fmt(r.variance) : '—' }}
              </td>
              <td style="font-family:var(--font-mono);text-align:right">{{ r.budget ? r.util + '%' : '—' }}</td>
            </tr>
            <tr v-if="!budgetRows.length">
              <td colspan="5" style="text-align:center;color:#9CA3AF;padding:40px">Məlumat yoxdur</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- CATEGORIES TAB -->
    <div v-else-if="activeTab === 'cats'">
      <div v-if="!selectedBuildingId" style="text-align:center;padding:60px;color:#9CA3AF;font-size:14px">
        Kateqoriya bölgüsü üçün yuxarıdan layihə seçin
      </div>
      <div v-else-if="catRows.length === 0" style="text-align:center;padding:60px;color:#9CA3AF;font-size:14px">
        Bu layihə üçün kateqoriya məlumatı yoxdur
      </div>
      <div v-else class="tk-card" style="padding:26px;display:grid;grid-template-columns:280px 1fr;gap:32px;align-items:center">
        <!-- Donut -->
        <div style="display:flex;align-items:center;justify-content:center">
          <div style="position:relative;width:184px;height:184px">
            <svg width="184" height="184">
              <g v-for="(seg, i) in donutSegments" :key="i">
                <path :d="seg.d" :fill="seg.color" />
              </g>
              <circle cx="92" cy="92" r="52" fill="#fff" />
            </svg>
            <div style="position:absolute;inset:0;display:flex;flex-direction:column;align-items:center;justify-content:center">
              <span style="font-family:var(--font-mono);font-size:16px;font-weight:600;color:#141F5E">{{ fmt(catTotal) }}</span>
              <span style="font-size:11px;color:#9CA3AF;margin-top:2px">{{ catLegend.length }} kateqoriya</span>
            </div>
          </div>
        </div>

        <!-- Legend -->
        <div style="display:flex;flex-direction:column;gap:14px">
          <div v-for="item in catLegend" :key="item.name">
            <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:6px">
              <span style="display:inline-flex;align-items:center;gap:9px;font-size:13.5px;color:#374151">
                <span :style="`width:10px;height:10px;border-radius:9999px;background:${item.color};flex:none`" />
                {{ item.name }}
              </span>
              <span style="display:inline-flex;align-items:center;gap:10px">
                <span style="font-family:var(--font-mono);font-size:13.5px;font-weight:600">{{ fmt(item.val) }}</span>
                <span style="font-family:var(--font-mono);font-size:12px;color:#9CA3AF;width:40px;text-align:right">{{ item.pct }}</span>
              </span>
            </div>
            <div style="height:6px;background:#F3F4F6;border-radius:9999px;overflow:hidden">
              <div :style="`height:100%;border-radius:9999px;background:${item.color};width:${item.barW}`" />
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- LEDGER TAB -->
    <div v-else-if="activeTab === 'ledger'">
      <div v-if="!selectedBuildingId" style="text-align:center;padding:60px;color:#9CA3AF;font-size:14px">
        Təchizatçı dəftəri üçün yuxarıdan layihə seçin
      </div>
      <div v-else class="tk-card" style="overflow:hidden">
        <table class="data-table">
          <thead>
            <tr>
              <th>Təchizatçı</th>
              <th class="text-right">Faktura edilib</th>
              <th class="text-right">Ödənilib</th>
              <th class="text-right">Saxlanılan</th>
              <th class="text-right">Açıq qalıq</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in (ledgerRows as Record<string,unknown>[])" :key="String(r.supplierId)">
              <td style="font-weight:500">{{ r.supplierName }}</td>
              <td style="font-family:var(--font-mono);text-align:right">{{ fmt(Number(r.totalInvoiced) || 0) }}</td>
              <td style="font-family:var(--font-mono);text-align:right;color:#16A34A">{{ fmt(Number(r.totalAdvancedPaid) || 0) }}</td>
              <td style="font-family:var(--font-mono);text-align:right;color:#6B7280">{{ fmt(Number(r.retainageHeldAmount) || 0) }}</td>
              <td style="font-family:var(--font-mono);text-align:right;font-weight:600;color:#D97706">{{ fmt(Number(r.outstandingBalance) || 0) }}</td>
            </tr>
            <tr v-if="!ledgerRows.length">
              <td colspan="5" style="text-align:center;color:#9CA3AF;padding:40px">Məlumat yoxdur</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- TREND TAB -->
    <div v-else-if="activeTab === 'trend'">
      <div v-if="!selectedBuildingId" style="text-align:center;padding:60px;color:#9CA3AF;font-size:14px">
        Aylıq trend üçün yuxarıdan layihə seçin
      </div>
      <div v-else-if="!trendBars.length" style="text-align:center;padding:60px;color:#9CA3AF;font-size:14px">
        Bu layihə üçün trend məlumatı yoxdur
      </div>
      <div v-else class="tk-card" style="padding:22px">
        <h3 style="font-size:15px;font-weight:600;margin:0 0 22px">Aylıq xərc trendi</h3>
        <div style="display:flex;align-items:flex-end;justify-content:space-around;height:200px;gap:14px;padding:0 8px">
          <div
            v-for="b in trendBars"
            :key="b.month"
            style="display:flex;flex-direction:column;align-items:center;gap:10px;flex:1"
          >
            <span style="font-size:11.5px;color:#6B7280;font-family:var(--font-mono)">{{ fmt(b.val).replace('₼','') }}</span>
            <div :style="`width:100%;max-width:44px;background:#3D5AF1;border-radius:4px 4px 0 0;height:${b.h}px;min-height:4px`" />
            <span style="font-size:12px;font-weight:500;color:#6B7280">{{ b.month }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- M² TAB -->
    <div v-else-if="activeTab === 'm2'">
      <div v-if="!selectedBuildingId" style="text-align:center;padding:60px;color:#9CA3AF;font-size:14px">
        m²-ə düşən xərc üçün yuxarıdan layihə seçin
      </div>
      <div v-else-if="!m2Data" style="text-align:center;padding:60px;color:#9CA3AF;font-size:14px">
        Məlumat yoxdur
      </div>
      <div v-else>
        <div style="display:grid;grid-template-columns:repeat(3,1fr);gap:16px;margin-bottom:18px">
          <div class="tk-card" style="padding:22px">
            <div style="font-size:12px;font-weight:500;color:#9CA3AF;text-transform:uppercase;letter-spacing:0.04em;margin-bottom:8px">Ümumi xərc</div>
            <div style="font-family:var(--font-mono);font-size:24px;font-weight:700;color:#141F5E">{{ fmt(Number(m2Data.totalSpend) || 0) }}</div>
          </div>
          <div class="tk-card" style="padding:22px">
            <div style="font-size:12px;font-weight:500;color:#9CA3AF;text-transform:uppercase;letter-spacing:0.04em;margin-bottom:8px">Sahə</div>
            <div style="font-family:var(--font-mono);font-size:24px;font-weight:700;color:#141F5E">{{ m2Data.floorAreaM2 ? Number(m2Data.floorAreaM2).toLocaleString('az-AZ') + ' m²' : '—' }}</div>
          </div>
          <div class="tk-card" style="padding:22px">
            <div style="font-size:12px;font-weight:500;color:#9CA3AF;text-transform:uppercase;letter-spacing:0.04em;margin-bottom:8px">m²-ə düşən xərc</div>
            <div style="font-family:var(--font-mono);font-size:24px;font-weight:700;color:#3D5AF1">{{ m2Data.costPerM2 ? fmt(Number(m2Data.costPerM2)) + '/m²' : '—' }}</div>
          </div>
        </div>

        <!-- Export section -->
        <div class="tk-card" style="padding:20px;display:flex;align-items:center;gap:14px;flex-wrap:wrap">
          <div style="font-size:14px;font-weight:500;color:#374151;flex:none">Aylıq Excel:</div>
          <select
            v-model="exportYear"
            style="height:36px;border:1px solid #D1D5DB;border-radius:7px;padding:0 10px;font-size:13.5px;background:#fff;font-family:inherit"
          >
            <option v-for="y in [2024,2025,2026,2027]" :key="y" :value="y">{{ y }}</option>
          </select>
          <select
            v-model="exportMonth"
            style="height:36px;border:1px solid #D1D5DB;border-radius:7px;padding:0 10px;font-size:13.5px;background:#fff;font-family:inherit"
          >
            <option v-for="m in 12" :key="m" :value="m">{{ String(m).padStart(2,'0') }}</option>
          </select>
          <button
            style="display:inline-flex;align-items:center;gap:7px;height:36px;padding:0 16px;background:#3D5AF1;color:#fff;border:none;border-radius:8px;font-size:13.5px;font-weight:600;cursor:pointer;font-family:inherit"
            @click="doExport"
          >
            <UIcon name="i-lucide-download" style="width:15px;height:15px" />
            Yüklə
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

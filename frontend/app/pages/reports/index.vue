<script setup lang="ts">
definePageMeta({ layout: 'default' })

const config = useRuntimeConfig()
const auth = useAuthStore()
const context = useContextStore()
const headers = computed(() => ({ Authorization: `Bearer ${auth.token}` }))

// ── Buildings dropdown ────────────────────────────────────────────
const { data: buildingsData } = await useAsyncData('report-buildings', () =>
  $fetch<{ content: Record<string, unknown>[] }>('/api/v1/buildings/search', {
    baseURL: config.public.apiBase,
    method: 'POST',
    headers: headers.value,
    body: { page: 0, perPage: 100, sortBy: 'name', sortDir: 'ASC', groupId: context.activeGroupId || undefined },
  }).catch(() => ({ content: [] })),
  { watch: [() => context.activeGroupId] },
)
const buildings = computed(() => buildingsData.value?.content ?? [])
const selectedBuildingId = ref<string>(context.activeBuildingId ?? '')

watch(() => context.activeBuildingId, (id) => { selectedBuildingId.value = id ?? '' })

// ── Tabs ──────────────────────────────────────────────────────────
const activeTab = ref('budget')
const tabs = [
  { id: 'budget',  label: 'Büdcə vs Faktiki' },
  { id: 'cats',    label: 'Kateqoriyalar' },
  { id: 'ledger',  label: 'Təchizatçı dəftəri' },
  { id: 'trend',   label: 'Aylıq trend' },
  { id: 'm2',      label: 'm²-ə düşən xərc' },
]

// ── Per-building report data ──────────────────────────────────────
interface BudgetRow {
  itemCode: string; level1: string; level2: string; level3: string
  itemName: string; budgetLimit: number; actualSpend: number; variance: number; usedPercent: number
}
interface LedgerRow {
  supplierId: string; name: string
  totalInvoiced: number; totalPaid: number
  retainageHeldAmount: number; balanceOwed: number
}
interface TrendRow { month: string; total: number }
interface M2Data { totalSpend: number; floorAreaM2: number | null; costPerM2: number | null }

const catRows    = ref<BudgetRow[]>([])
const ledgerRows = ref<LedgerRow[]>([])
const trendData  = ref<TrendRow[]>([])
const m2Data     = ref<M2Data | null>(null)
const reportLoading = ref(false)

async function loadBuildingReports(id: string) {
  if (!id) { catRows.value = []; ledgerRows.value = []; trendData.value = []; m2Data.value = null; return }
  reportLoading.value = true
  try {
    const h = headers.value
    const base = config.public.apiBase
    const [bva, ledger, trend, m2] = await Promise.allSettled([
      $fetch<BudgetRow[]>(`/api/v1/reports/budget-vs-actual?buildingId=${id}`, { baseURL: base, headers: h }),
      $fetch<LedgerRow[]>(`/api/v1/reports/supplier-ledger?buildingId=${id}`, { baseURL: base, headers: h }),
      $fetch<TrendRow[]>(`/api/v1/reports/monthly-trend?buildingId=${id}`, { baseURL: base, headers: h }),
      $fetch<M2Data>(`/api/v1/reports/cost-per-m2?buildingId=${id}`, { baseURL: base, headers: h }),
    ])
    catRows.value   = bva.status    === 'fulfilled' ? bva.value    : []
    ledgerRows.value = ledger.status === 'fulfilled' ? ledger.value : []
    trendData.value  = trend.status  === 'fulfilled'
      ? (trend.value as unknown as Array<{ month: string; total: unknown }>).map(r => ({ month: r.month, total: Number(r.total) }))
      : []
    m2Data.value     = m2.status     === 'fulfilled' ? m2.value    : null
  } finally {
    reportLoading.value = false
  }
}

watch(selectedBuildingId, (id) => loadBuildingReports(id), { immediate: true })

// ── Budget vs Actual helpers ──────────────────────────────────────
const budgetGroups = computed(() => {
  const map = new Map<string, BudgetRow[]>()
  for (const row of catRows.value) {
    const key = row.level1 || 'Digər'
    if (!map.has(key)) map.set(key, [])
    map.get(key)!.push(row)
  }
  return [...map.entries()].map(([group, rows]) => ({
    group,
    budget:  rows.reduce((s, r) => s + r.budgetLimit,  0),
    actual:  rows.reduce((s, r) => s + r.actualSpend,  0),
    rows,
  }))
})

const budgetTotals = computed(() => ({
  budget: budgetGroups.value.reduce((s, g) => s + g.budget, 0),
  actual: budgetGroups.value.reduce((s, g) => s + g.actual, 0),
}))

// ── Category donut helpers ────────────────────────────────────────
const COLORS = ['#3D5AF1','#F59E0B','#16A34A','#8B5CF6','#EC4899','#0EA5E9','#14B8A6','#DC2626']

const topCats = computed(() =>
  [...catRows.value]
    .filter(r => r.actualSpend > 0)
    .sort((a, b) => b.actualSpend - a.actualSpend)
    .slice(0, 8)
    .map((r, i) => ({ name: r.itemName || r.level2 || r.level1 || '—', val: r.actualSpend, color: COLORS[i % COLORS.length] }))
)
const catTotal = computed(() => topCats.value.reduce((s, c) => s + c.val, 0))

const donutSegments = computed(() => {
  const r = 80, cx = 92, cy = 92
  let offset = 0
  return topCats.value.map(item => {
    const pct = catTotal.value > 0 ? item.val / catTotal.value : 0
    const angle = pct * 2 * Math.PI
    const x1 = cx + r * Math.sin(offset), y1 = cy - r * Math.cos(offset)
    offset += angle
    const x2 = cx + r * Math.sin(offset), y2 = cy - r * Math.cos(offset)
    return { d: `M${cx},${cy} L${x1},${y1} A${r},${r} 0 ${angle > Math.PI ? 1 : 0},1 ${x2},${y2} Z`, color: item.color }
  })
})

// ── Monthly trend helpers ─────────────────────────────────────────
const AZ_MONTHS: Record<string, string> = {
  '01': 'Yan', '02': 'Fev', '03': 'Mar', '04': 'Apr',
  '05': 'May', '06': 'İyn', '07': 'İyl', '08': 'Avq',
  '09': 'Sen', '10': 'Okt', '11': 'Noy', '12': 'Dek',
}

function fmtMonth(ym: string) {
  const parts = ym.split('-')
  return `${AZ_MONTHS[parts[1]!] ?? parts[1]} ${parts[0]}`
}

const trendMax    = computed(() => Math.max(...trendData.value.map(r => r.total), 1))
const trendTotal  = computed(() => trendData.value.reduce((s, r) => s + r.total, 0))
const trendAvg    = computed(() => trendData.value.length ? trendTotal.value / trendData.value.length : 0)

const trendCumulative = computed(() => {
  let acc = 0
  return trendData.value.map(r => { acc += r.total; return acc })
})

// ── Export ────────────────────────────────────────────────────────
const exportYear  = ref(new Date().getFullYear())
const exportMonth = ref(new Date().getMonth() + 1)
async function doExport() {
  if (!selectedBuildingId.value) return
  const url = `${config.public.apiBase}/api/v1/reports/export/monthly?buildingId=${selectedBuildingId.value}&year=${exportYear.value}&month=${exportMonth.value}`
  const res  = await fetch(url, { headers: headers.value })
  const blob = await res.blob()
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = `expenses-${exportYear.value}-${String(exportMonth.value).padStart(2, '0')}.xlsx`
  a.click()
}

// ── Formatters ────────────────────────────────────────────────────
function fmt(n: number) {
  return '₼' + n.toLocaleString('az-AZ', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function fmtCompact(n: number) {
  if (n >= 1_000_000) return '₼' + (n / 1_000_000).toFixed(1) + 'M'
  if (n >= 1_000)     return '₼' + (n / 1_000).toFixed(0) + 'k'
  return '₼' + n.toFixed(0)
}
function varianceColor(v: number) { return v >= 0 ? '#16A34A' : '#DC2626' }
function usedColor(pct: number) {
  if (pct >= 100) return '#DC2626'
  if (pct >= 85)  return '#D97706'
  return '#3D5AF1'
}
</script>

<template>
  <div class="animate-page">
    <!-- Page header -->
    <div class="page-hdr" style="flex-wrap:wrap;gap:14px">
      <div>
        <div style="font-size:12px;font-weight:500;color:#9CA3AF;letter-spacing:0.04em;text-transform:uppercase;margin-bottom:6px">Analitika</div>
        <h1 style="font-size:26px;font-weight:700;letter-spacing:-0.02em;margin:0 0 4px">Hesabatlar</h1>
        <p style="font-size:14px;color:#6B7280;margin:0">Büdcə, dəftər və trend analitikası</p>
      </div>
      <div style="display:flex;align-items:center;gap:10px;flex-wrap:wrap">
        <select
          v-model="selectedBuildingId"
          style="height:42px;border:1px solid #D1D5DB;border-radius:8px;padding:0 12px;font-size:14px;background:#fff;color:#0A0A0A;font-family:inherit;min-width:180px;outline:none"
        >
          <option value="">Layihə seçin</option>
          <option v-for="b in buildings" :key="String(b.id)" :value="b.id">{{ b.name }}</option>
        </select>
        <button
          v-if="selectedBuildingId"
          style="display:inline-flex;align-items:center;gap:7px;height:42px;padding:0 16px;background:#fff;color:#374151;border:1px solid #D1D5DB;border-radius:8px;font-size:14px;font-weight:500;cursor:pointer;font-family:inherit"
          @click="doExport"
        >
          <UIcon name="i-lucide-download" style="width:15px;height:15px" />
          Excel
        </button>
      </div>
    </div>

    <!-- Tabs — horizontally scrollable on mobile -->
    <div class="tabs-scroll">
      <button
        v-for="tab in tabs"
        :key="tab.id"
        :style="{
          padding: '10px 16px', fontSize: '14px', fontWeight: '600',
          border: 'none', background: 'none', cursor: 'pointer', fontFamily: 'inherit',
          color: activeTab === tab.id ? '#141F5E' : '#6B7280',
          borderBottom: activeTab === tab.id ? '2px solid #3D5AF1' : '2px solid transparent',
          marginBottom: '-1px', whiteSpace: 'nowrap',
        }"
        @click="activeTab = tab.id"
      >{{ tab.label }}</button>
    </div>

    <!-- No building selected -->
    <div v-if="!selectedBuildingId && activeTab !== 'budget'" style="text-align:center;padding:60px;color:#9CA3AF;font-size:14px">
      Hesabat üçün yuxarıdan layihə seçin
    </div>

    <div v-else-if="reportLoading" style="text-align:center;padding:60px;color:#9CA3AF">
      <UIcon name="i-lucide-loader-2" style="width:24px;height:24px;animation:spin 1s linear infinite" />
    </div>

    <!-- ══ BUDGET VS ACTUAL ══════════════════════════════════════ -->
    <div v-else-if="activeTab === 'budget'">
      <div v-if="!selectedBuildingId" style="text-align:center;padding:60px;color:#9CA3AF;font-size:14px">
        Büdcə analizi üçün yuxarıdan layihə seçin
      </div>
      <div v-else-if="catRows.length === 0" style="text-align:center;padding:60px;color:#9CA3AF;font-size:14px">
        Bu layihə üçün büdcə məlumatı yoxdur
      </div>
      <template v-else>
        <div class="rg-3" style="margin-bottom:20px">
          <div class="tk-card" style="padding:18px 22px">
            <div style="font-size:11.5px;font-weight:600;color:#9CA3AF;text-transform:uppercase;letter-spacing:0.04em;margin-bottom:8px">Ümumi büdcə</div>
            <div style="font-family:var(--font-mono);font-size:22px;font-weight:700;color:#141F5E">{{ fmt(budgetTotals.budget) }}</div>
          </div>
          <div class="tk-card" style="padding:18px 22px">
            <div style="font-size:11.5px;font-weight:600;color:#9CA3AF;text-transform:uppercase;letter-spacing:0.04em;margin-bottom:8px">Faktiki xərc</div>
            <div style="font-family:var(--font-mono);font-size:22px;font-weight:700;color:#141F5E">{{ fmt(budgetTotals.actual) }}</div>
          </div>
          <div class="tk-card" style="padding:18px 22px">
            <div style="font-size:11.5px;font-weight:600;color:#9CA3AF;text-transform:uppercase;letter-spacing:0.04em;margin-bottom:8px">Fərq</div>
            <div
              style="font-family:var(--font-mono);font-size:22px;font-weight:700"
              :style="{ color: varianceColor(budgetTotals.budget - budgetTotals.actual) }"
            >
              {{ budgetTotals.budget > 0 ? ((budgetTotals.budget - budgetTotals.actual) >= 0 ? '+' : '') + fmt(budgetTotals.budget - budgetTotals.actual) : '—' }}
            </div>
          </div>
        </div>

        <div style="display:flex;flex-direction:column;gap:12px">
          <div v-for="grp in budgetGroups" :key="grp.group" class="tk-card" style="overflow:hidden">
            <div style="display:flex;align-items:center;justify-content:space-between;padding:14px 20px;background:#F9FAFB;border-bottom:1px solid #F3F4F6;flex-wrap:wrap;gap:8px">
              <span style="font-size:14px;font-weight:700;color:#111827">{{ grp.group }}</span>
              <div style="display:flex;align-items:center;gap:16px;flex-wrap:wrap">
                <span style="font-size:12.5px;color:#6B7280">Büdcə: <strong style="color:#374151;font-family:var(--font-mono)">{{ grp.budget ? fmt(grp.budget) : '—' }}</strong></span>
                <span style="font-size:12.5px;color:#6B7280">Faktiki: <strong style="color:#374151;font-family:var(--font-mono)">{{ fmt(grp.actual) }}</strong></span>
                <span
                  v-if="grp.budget > 0"
                  style="font-size:12px;font-weight:700;font-family:var(--font-mono);min-width:44px;text-align:right"
                  :style="{ color: usedColor((grp.actual / grp.budget) * 100) }"
                >{{ ((grp.actual / grp.budget) * 100).toFixed(0) }}%</span>
              </div>
            </div>
            <div v-if="grp.budget > 0" style="height:4px;background:#F3F4F6">
              <div :style="{ height:'100%', width:Math.min((grp.actual/grp.budget)*100,100)+'%', background:usedColor((grp.actual/grp.budget)*100), transition:'width 0.4s ease' }" />
            </div>
            <div style="overflow-x:auto;-webkit-overflow-scrolling:touch">
              <table class="data-table" style="min-width:480px">
                <thead>
                  <tr>
                    <th style="padding-left:24px">Maddə</th>
                    <th>Kod</th>
                    <th class="text-right">Büdcə</th>
                    <th class="text-right">Faktiki</th>
                    <th class="text-right">Fərq</th>
                    <th class="text-right" style="width:100px">İstifadə</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="row in grp.rows" :key="row.itemCode">
                    <td style="padding-left:24px;font-weight:500">{{ row.itemName || row.level3 || row.level2 }}</td>
                    <td style="font-family:var(--font-mono);font-size:12px;color:#9CA3AF">{{ row.itemCode }}</td>
                    <td style="font-family:var(--font-mono);text-align:right;color:#6B7280">{{ row.budgetLimit ? fmt(row.budgetLimit) : '—' }}</td>
                    <td style="font-family:var(--font-mono);text-align:right">{{ fmt(row.actualSpend) }}</td>
                    <td :style="`font-family:var(--font-mono);text-align:right;font-weight:600;color:${varianceColor(row.variance)}`">
                      {{ row.budgetLimit ? ((row.variance >= 0 ? '+' : '') + fmt(row.variance)) : '—' }}
                    </td>
                    <td style="text-align:right;padding-right:16px">
                      <div v-if="row.budgetLimit > 0" style="display:flex;align-items:center;gap:8px;justify-content:flex-end">
                        <div style="width:60px;height:5px;background:#F3F4F6;border-radius:9999px;overflow:hidden">
                          <div :style="{ height:'100%', width:Math.min(row.usedPercent,100)+'%', background:usedColor(row.usedPercent), borderRadius:'9999px' }" />
                        </div>
                        <span style="font-family:var(--font-mono);font-size:12px;min-width:32px;text-align:right" :style="{ color:usedColor(row.usedPercent) }">
                          {{ row.usedPercent.toFixed(0) }}%
                        </span>
                      </div>
                      <span v-else style="color:#D1D5DB;font-size:12px">—</span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </template>
    </div>

    <!-- ══ CATEGORIES DONUT ═══════════════════════════════════════ -->
    <div v-else-if="activeTab === 'cats'">
      <div v-if="topCats.length === 0" style="text-align:center;padding:60px;color:#9CA3AF;font-size:14px">
        Bu layihə üçün xərc məlumatı yoxdur
      </div>
      <div v-else class="tk-card" style="padding:28px">
        <div class="report-cat-grid" style="display:grid;grid-template-columns:240px 1fr;gap:36px;align-items:center">
          <!-- Donut -->
          <div class="report-cat-donut" style="display:flex;align-items:center;justify-content:center">
            <div style="position:relative;width:184px;height:184px">
              <svg width="184" height="184">
                <path v-for="(seg, i) in donutSegments" :key="i" :d="seg.d" :fill="seg.color" />
                <circle cx="92" cy="92" r="52" fill="#fff" />
              </svg>
              <div style="position:absolute;inset:0;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:2px">
                <span style="font-family:var(--font-mono);font-size:15px;font-weight:700;color:#141F5E">{{ fmtCompact(catTotal) }}</span>
                <span style="font-size:11px;color:#9CA3AF">{{ topCats.length }} kateqoriya</span>
              </div>
            </div>
          </div>
          <!-- Legend -->
          <div style="display:flex;flex-direction:column;gap:12px">
            <div v-for="item in topCats" :key="item.name">
              <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:5px">
                <span style="display:inline-flex;align-items:center;gap:8px;font-size:13px;color:#374151">
                  <span :style="`width:9px;height:9px;border-radius:9999px;background:${item.color};flex:none`" />
                  {{ item.name }}
                </span>
                <span style="display:inline-flex;align-items:center;gap:10px">
                  <span style="font-family:var(--font-mono);font-size:13px;font-weight:600">{{ fmt(item.val) }}</span>
                  <span style="font-family:var(--font-mono);font-size:11.5px;color:#9CA3AF;width:42px;text-align:right">
                    {{ catTotal > 0 ? ((item.val / catTotal) * 100).toFixed(1) + '%' : '0%' }}
                  </span>
                </span>
              </div>
              <div style="height:5px;background:#F3F4F6;border-radius:9999px;overflow:hidden">
                <div :style="`height:100%;border-radius:9999px;background:${item.color};width:${catTotal > 0 ? ((item.val / catTotal) * 100).toFixed(1) : 0}%`" />
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ══ SUPPLIER LEDGER ════════════════════════════════════════ -->
    <div v-else-if="activeTab === 'ledger'">
      <div v-if="ledgerRows.length === 0" style="text-align:center;padding:60px;color:#9CA3AF;font-size:14px">
        Bu layihə üçün təchizatçı fəaliyyəti yoxdur
      </div>
      <div v-else class="tk-card" style="overflow-x:auto;-webkit-overflow-scrolling:touch">
        <table class="data-table" style="min-width:480px">
          <thead>
            <tr>
              <th>Təchizatçı</th>
              <th class="text-right">Faktura məbləği</th>
              <th class="text-right">Ödənilib</th>
              <th class="text-right">Saxlanılan</th>
              <th class="text-right">Qalıq borc</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in ledgerRows" :key="r.supplierId">
              <td style="font-weight:500">{{ r.name }}</td>
              <td style="font-family:var(--font-mono);text-align:right;font-weight:600">{{ fmt(r.totalInvoiced) }}</td>
              <td style="font-family:var(--font-mono);text-align:right;color:#16A34A">{{ fmt(r.totalPaid) }}</td>
              <td style="font-family:var(--font-mono);text-align:right;color:#6B7280">{{ fmt(r.retainageHeldAmount) }}</td>
              <td style="font-family:var(--font-mono);text-align:right;font-weight:700" :style="{ color: r.balanceOwed > 0 ? '#D97706' : '#16A34A' }">{{ fmt(r.balanceOwed) }}</td>
            </tr>
            <tr style="background:#F9FAFB;font-weight:700">
              <td style="font-size:12.5px;color:#6B7280;text-transform:uppercase;letter-spacing:0.03em">Cəmi</td>
              <td style="font-family:var(--font-mono);text-align:right">{{ fmt(ledgerRows.reduce((s, r) => s + r.totalInvoiced, 0)) }}</td>
              <td style="font-family:var(--font-mono);text-align:right;color:#16A34A">{{ fmt(ledgerRows.reduce((s, r) => s + r.totalPaid, 0)) }}</td>
              <td style="font-family:var(--font-mono);text-align:right;color:#6B7280">{{ fmt(ledgerRows.reduce((s, r) => s + r.retainageHeldAmount, 0)) }}</td>
              <td style="font-family:var(--font-mono);text-align:right;color:#D97706">{{ fmt(ledgerRows.reduce((s, r) => s + r.balanceOwed, 0)) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- ══ MONTHLY TREND ══════════════════════════════════════════ -->
    <div v-else-if="activeTab === 'trend'">
      <div v-if="trendData.length === 0" style="text-align:center;padding:60px;color:#9CA3AF;font-size:14px">
        Bu layihə üçün trend məlumatı yoxdur
      </div>
      <template v-else>
        <div class="rg-3" style="margin-bottom:20px">
          <div class="tk-card" style="padding:18px 22px">
            <div style="font-size:11.5px;font-weight:600;color:#9CA3AF;text-transform:uppercase;letter-spacing:0.04em;margin-bottom:8px">Ümumi xərc</div>
            <div style="font-family:var(--font-mono);font-size:22px;font-weight:700;color:#141F5E">{{ fmt(trendTotal) }}</div>
            <div style="font-size:12px;color:#9CA3AF;margin-top:4px">{{ trendData.length }} ay üzrə</div>
          </div>
          <div class="tk-card" style="padding:18px 22px">
            <div style="font-size:11.5px;font-weight:600;color:#9CA3AF;text-transform:uppercase;letter-spacing:0.04em;margin-bottom:8px">Aylıq ortalama</div>
            <div style="font-family:var(--font-mono);font-size:22px;font-weight:700;color:#141F5E">{{ fmt(trendAvg) }}</div>
          </div>
          <div class="tk-card" style="padding:18px 22px">
            <div style="font-size:11.5px;font-weight:600;color:#9CA3AF;text-transform:uppercase;letter-spacing:0.04em;margin-bottom:8px">Ən yüksək ay</div>
            <div style="font-family:var(--font-mono);font-size:22px;font-weight:700;color:#141F5E">
              {{ fmt(Math.max(...trendData.map(r => r.total))) }}
            </div>
            <div style="font-size:12px;color:#9CA3AF;margin-top:4px">
              {{ fmtMonth(trendData.reduce((a, b) => a.total > b.total ? a : b).month) }}
            </div>
          </div>
        </div>

        <div class="tk-card" style="padding:24px;margin-bottom:16px">
          <div style="display:flex;align-items:baseline;justify-content:space-between;margin-bottom:20px;flex-wrap:wrap;gap:10px">
            <h3 style="font-size:15px;font-weight:600;margin:0">Aylıq xərc trendi</h3>
            <div style="display:flex;align-items:center;gap:16px">
              <div style="display:flex;align-items:center;gap:6px">
                <div style="width:10px;height:10px;background:#3D5AF1;border-radius:2px" />
                <span style="font-size:12px;color:#6B7280">Aylıq xərc</span>
              </div>
              <div style="display:flex;align-items:center;gap:6px">
                <div style="width:16px;height:2px;background:#F59E0B" />
                <span style="font-size:12px;color:#6B7280">Ortalama</span>
              </div>
            </div>
          </div>

          <div style="overflow-x:auto;-webkit-overflow-scrolling:touch">
            <div :style="`min-width:${Math.max(trendData.length * 72, 320)}px`">
              <div style="position:relative;height:200px;margin-bottom:8px">
                <div :style="{ position:'absolute', left:0, right:0, bottom:(trendAvg / trendMax * 200)+'px', borderTop:'1.5px dashed #F59E0B', zIndex:2 }">
                  <span style="position:absolute;right:0;top:-16px;font-size:10.5px;font-weight:600;color:#D97706;background:#fff;padding:0 4px">
                    ort. {{ fmtCompact(trendAvg) }}
                  </span>
                </div>
                <div style="display:flex;align-items:flex-end;height:100%;gap:8px;padding:0 4px">
                  <div
                    v-for="r in trendData"
                    :key="r.month"
                    style="flex:1;display:flex;flex-direction:column;align-items:center;gap:4px;height:100%;justify-content:flex-end"
                  >
                    <span style="font-size:10px;font-weight:600;color:#6B7280;white-space:nowrap">{{ fmtCompact(r.total) }}</span>
                    <div
                      :style="{
                        width:'100%', maxWidth:'48px',
                        background: r.total >= trendAvg ? '#3D5AF1' : '#93ACFF',
                        borderRadius:'4px 4px 0 0',
                        height: Math.max((r.total / trendMax) * 180, r.total > 0 ? 4 : 0) + 'px',
                        transition:'height 0.3s ease',
                      }"
                    />
                  </div>
                </div>
              </div>
              <div style="display:flex;gap:8px;padding:0 4px">
                <div v-for="r in trendData" :key="r.month" style="flex:1;text-align:center">
                  <div style="font-size:11px;font-weight:600;color:#374151;line-height:1.3">{{ AZ_MONTHS[r.month.split('-')[1] ?? ''] ?? r.month }}</div>
                  <div style="font-size:10px;color:#9CA3AF">{{ r.month.split('-')[0] }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="tk-card" style="overflow-x:auto;-webkit-overflow-scrolling:touch">
          <table class="data-table" style="min-width:400px">
            <thead>
              <tr>
                <th>Ay</th>
                <th class="text-right">Xərc</th>
                <th class="text-right">Kumulyativ</th>
                <th style="width:160px">Ortalamaya nisbət</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(r, i) in trendData" :key="r.month">
                <td style="font-weight:500">{{ fmtMonth(r.month) }}</td>
                <td style="font-family:var(--font-mono);text-align:right;font-weight:600">{{ fmt(r.total) }}</td>
                <td style="font-family:var(--font-mono);text-align:right;color:#6B7280">{{ fmt(trendCumulative[i] ?? 0) }}</td>
                <td style="padding-right:16px">
                  <div style="display:flex;align-items:center;gap:8px">
                    <div style="flex:1;height:5px;background:#F3F4F6;border-radius:9999px;overflow:hidden">
                      <div :style="{ height:'100%', width:trendMax>0?Math.min((r.total/trendMax)*100,100)+'%':'0%', background:r.total>=trendAvg?'#3D5AF1':'#93ACFF', borderRadius:'9999px' }" />
                    </div>
                    <span style="font-family:var(--font-mono);font-size:11.5px;min-width:40px;text-align:right" :style="{ color:r.total>=trendAvg?'#3D5AF1':'#9CA3AF' }">
                      {{ trendAvg > 0 ? ((r.total / trendAvg) * 100).toFixed(0) + '%' : '—' }}
                    </span>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </template>
    </div>

    <!-- ══ m² TAB ════════════════════════════════════════════════ -->
    <div v-else-if="activeTab === 'm2'">
      <div v-if="!m2Data" style="text-align:center;padding:60px;color:#9CA3AF;font-size:14px">Məlumat yoxdur</div>
      <div v-else>
        <div class="rg-3" style="margin-bottom:18px">
          <div class="tk-card" style="padding:22px">
            <div style="font-size:11.5px;font-weight:600;color:#9CA3AF;text-transform:uppercase;letter-spacing:0.04em;margin-bottom:8px">Ümumi xərc</div>
            <div style="font-family:var(--font-mono);font-size:24px;font-weight:700;color:#141F5E">{{ fmt(m2Data.totalSpend) }}</div>
          </div>
          <div class="tk-card" style="padding:22px">
            <div style="font-size:11.5px;font-weight:600;color:#9CA3AF;text-transform:uppercase;letter-spacing:0.04em;margin-bottom:8px">Sahə</div>
            <div style="font-family:var(--font-mono);font-size:24px;font-weight:700;color:#141F5E">
              {{ m2Data.floorAreaM2 ? Number(m2Data.floorAreaM2).toLocaleString('az-AZ') + ' m²' : '—' }}
            </div>
          </div>
          <div class="tk-card" style="padding:22px">
            <div style="font-size:11.5px;font-weight:600;color:#9CA3AF;text-transform:uppercase;letter-spacing:0.04em;margin-bottom:8px">m²-ə düşən xərc</div>
            <div style="font-family:var(--font-mono);font-size:24px;font-weight:700;color:#3D5AF1">
              {{ m2Data.costPerM2 ? fmt(Number(m2Data.costPerM2)) + '/m²' : '—' }}
            </div>
          </div>
        </div>
        <div class="tk-card" style="padding:20px;display:flex;align-items:center;gap:14px;flex-wrap:wrap">
          <div style="font-size:14px;font-weight:500;color:#374151;flex:none">Aylıq Excel:</div>
          <select v-model="exportYear" style="height:40px;border:1px solid #D1D5DB;border-radius:7px;padding:0 10px;background:#fff;font-family:inherit;outline:none">
            <option v-for="y in [2024,2025,2026,2027]" :key="y" :value="y">{{ y }}</option>
          </select>
          <select v-model="exportMonth" style="height:40px;border:1px solid #D1D5DB;border-radius:7px;padding:0 10px;background:#fff;font-family:inherit;outline:none">
            <option v-for="m in 12" :key="m" :value="m">{{ String(m).padStart(2,'0') }}</option>
          </select>
          <button
            style="display:inline-flex;align-items:center;gap:7px;height:40px;padding:0 16px;background:#3D5AF1;color:#fff;border:none;border-radius:8px;font-size:13.5px;font-weight:600;cursor:pointer;font-family:inherit"
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

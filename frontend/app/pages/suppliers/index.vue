<script setup lang="ts">
definePageMeta({ layout: 'default' })

const config = useRuntimeConfig()
const auth = useAuthStore()
const headers = computed(() => ({ Authorization: `Bearer ${auth.token}` }))

const { data, refresh } = await useAsyncData('suppliers', () =>
  $fetch<{ content: unknown[] }>('/api/v1/suppliers/search', {
    baseURL: config.public.apiBase,
    method: 'POST',
    headers: headers.value,
    body: { page: 0, perPage: 100, sortBy: 'name', sortDir: 'ASC' },
  }).catch(() => ({ content: [] })),
)

const suppliers = computed(() => (data.value?.content ?? []) as Record<string, unknown>[])

const modalOpen = ref(false)
const editSupplier = ref<Record<string, unknown> | null>(null)

function openCreate() { editSupplier.value = null; modalOpen.value = true }
function openEdit(s: Record<string, unknown>) { editSupplier.value = s; modalOpen.value = true }
function onSaved() { modalOpen.value = false; refresh() }

function fmt(n: number) {
  return '₼' + n.toLocaleString('az-AZ', { minimumFractionDigits: 2 })
}

function typeLabel(t: string) {
  return t === 'COMPANY' ? 'Şirkət' : t === 'INDIVIDUAL' ? 'Fərdi' : t
}
</script>

<template>
  <div class="animate-page">
    <div style="display:flex;align-items:flex-end;justify-content:space-between;gap:24px;margin-bottom:24px">
      <div>
        <div style="font-size:12px;font-weight:500;color:#9CA3AF;letter-spacing:0.04em;text-transform:uppercase;margin-bottom:6px">Satınalma</div>
        <h1 style="font-size:26px;font-weight:700;letter-spacing:-0.02em;margin:0 0 4px">Təchizatçılar</h1>
        <p style="font-size:14px;color:#6B7280;margin:0">{{ suppliers.length }} təchizatçı</p>
      </div>
      <button
        style="display:inline-flex;align-items:center;gap:8px;height:40px;padding:0 18px;background:#3D5AF1;color:#fff;border:none;border-radius:8px;font-size:14px;font-weight:600;cursor:pointer"
        @click="openCreate"
      >
        <UIcon name="i-lucide-plus" style="width:16px;height:16px" />
        Yeni təchizatçı
      </button>
    </div>

    <div class="tk-card" style="overflow:hidden">
      <table class="data-table">
        <thead>
          <tr>
            <th>Təchizatçı</th>
            <th>Növ</th>
            <th>VÖEN</th>
            <th class="text-right">Avans ödənilmiş</th>
            <th class="text-right">Retainage %</th>
            <th class="text-right">Saxlanılan məbləğ</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="s in suppliers"
            :key="String(s.id)"
            style="cursor:pointer"
            @click="openEdit(s)"
          >
            <td style="font-weight:500">{{ s.name }}</td>
            <td style="color:#6B7280">{{ typeLabel(String(s.supplierType)) }}</td>
            <td style="font-family:var(--font-mono);color:#6B7280;font-size:13px">{{ s.taxId ?? '—' }}</td>
            <td style="font-family:var(--font-mono);text-align:right">{{ fmt(Number(s.totalAdvancedPaid) || 0) }}</td>
            <td style="font-family:var(--font-mono);text-align:right;color:#6B7280">{{ s.retainagePercentage ?? 0 }}%</td>
            <td style="font-family:var(--font-mono);text-align:right;font-weight:600;color:#D97706">{{ fmt(Number(s.retainageHeldAmount) || 0) }}</td>
          </tr>
          <tr v-if="!suppliers.length">
            <td colspan="6" style="text-align:center;color:#9CA3AF;padding:40px">Təchizatçı tapılmadı</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Supplier modal -->
    <SupplierModal
      v-if="modalOpen"
      :supplier="editSupplier"
      @close="modalOpen = false"
      @saved="onSaved"
    />
  </div>
</template>

<script setup lang="ts">
const props = defineProps<{
  supplier?: Record<string, unknown> | null
}>()

const emit = defineEmits<{
  close: []
  saved: []
}>()

const config = useRuntimeConfig()
const auth = useAuthStore()

const form = reactive({
  name: String(props.supplier?.name ?? ''),
  supplierType: String(props.supplier?.supplierType ?? 'COMPANY'),
  taxId: String(props.supplier?.taxId ?? ''),
  phoneNumber: String(props.supplier?.phoneNumber ?? ''),
  totalAdvancedPaid: props.supplier?.totalAdvancedPaid ? String(props.supplier.totalAdvancedPaid) : '',
  retainagePercentage: props.supplier?.retainagePercentage ? String(props.supplier.retainagePercentage) : '',
  retainageHeldAmount: props.supplier?.retainageHeldAmount ? String(props.supplier.retainageHeldAmount) : '',
})

const loading = ref(false)
const error = ref('')

const isEdit = computed(() => !!props.supplier?.id)
const title = computed(() => isEdit.value ? 'Təchizatçını redaktə et' : 'Yeni təchizatçı')

async function save() {
  if (!form.name.trim()) { error.value = 'Ad tələb olunur'; return }
  if (!form.supplierType) { error.value = 'Növ tələb olunur'; return }
  error.value = ''
  loading.value = true
  try {
    const body: Record<string, unknown> = {
      name: form.name.trim(),
      supplierType: form.supplierType,
      taxId: form.taxId.trim() || undefined,
      phoneNumber: form.phoneNumber.trim() || undefined,
      totalAdvancedPaid: form.totalAdvancedPaid ? Number(form.totalAdvancedPaid) : undefined,
      retainagePercentage: form.retainagePercentage ? Number(form.retainagePercentage) : undefined,
      retainageHeldAmount: form.retainageHeldAmount ? Number(form.retainageHeldAmount) : undefined,
    }
    const headers = { Authorization: `Bearer ${auth.token}` }
    if (isEdit.value) {
      await $fetch(`/api/v1/suppliers/${props.supplier!.id}`, {
        baseURL: config.public.apiBase, method: 'PUT', headers, body,
      })
    } else {
      await $fetch('/api/v1/suppliers', {
        baseURL: config.public.apiBase, method: 'POST', headers, body,
      })
    }
    emit('saved')
  } catch {
    error.value = 'Xəta baş verdi. Yenidən cəhd edin.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div style="position:fixed;inset:0;z-index:50;display:flex;align-items:center;justify-content:center;padding:20px">
    <div style="position:absolute;inset:0;background:rgba(0,0,0,0.35)" @click="emit('close')" />
    <div style="position:relative;background:#fff;border-radius:12px;width:100%;max-width:480px;box-shadow:0 20px 60px rgba(0,0,0,0.18);overflow:hidden">
      <!-- Header -->
      <div style="display:flex;align-items:center;justify-content:space-between;padding:20px 24px;border-bottom:1px solid #F3F4F6">
        <h2 style="font-size:17px;font-weight:700;margin:0;letter-spacing:-0.01em">{{ title }}</h2>
        <button style="background:none;border:none;cursor:pointer;padding:4px;color:#6B7280" @click="emit('close')">
          <UIcon name="i-lucide-x" style="width:20px;height:20px" />
        </button>
      </div>

      <!-- Body -->
      <div style="padding:24px;display:flex;flex-direction:column;gap:16px">
        <div v-if="error" style="background:#FEF2F2;border:1px solid #FCA5A5;border-radius:8px;padding:10px 14px;font-size:13.5px;color:#DC2626">
          {{ error }}
        </div>

        <!-- Name -->
        <div>
          <label style="display:block;font-size:13px;font-weight:500;color:#374151;margin-bottom:6px">Ad <span style="color:#EF4444">*</span></label>
          <input
            v-model="form.name"
            type="text"
            placeholder="məs. Azərİnşaat MMC"
            style="width:100%;height:40px;border:1px solid #D1D5DB;border-radius:8px;padding:0 12px;font-size:14px;color:#0A0A0A;font-family:inherit;outline:none;box-sizing:border-box"
          >
        </div>

        <!-- Type -->
        <div>
          <label style="display:block;font-size:13px;font-weight:500;color:#374151;margin-bottom:6px">Növ <span style="color:#EF4444">*</span></label>
          <select
            v-model="form.supplierType"
            style="width:100%;height:40px;border:1px solid #D1D5DB;border-radius:8px;padding:0 12px;font-size:14px;background:#fff;color:#0A0A0A;font-family:inherit;outline:none;box-sizing:border-box"
          >
            <option value="COMPANY">Şirkət</option>
            <option value="INDIVIDUAL">Fərdi</option>
          </select>
        </div>

        <!-- Tax ID + Phone -->
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:14px">
          <div>
            <label style="display:block;font-size:13px;font-weight:500;color:#374151;margin-bottom:6px">VÖEN</label>
            <input
              v-model="form.taxId"
              type="text"
              placeholder="1234567890"
              style="width:100%;height:40px;border:1px solid #D1D5DB;border-radius:8px;padding:0 12px;font-size:14px;color:#0A0A0A;font-family:inherit;outline:none;box-sizing:border-box"
            >
          </div>
          <div>
            <label style="display:block;font-size:13px;font-weight:500;color:#374151;margin-bottom:6px">Telefon</label>
            <input
              v-model="form.phoneNumber"
              type="text"
              placeholder="+994 50 000 00 00"
              style="width:100%;height:40px;border:1px solid #D1D5DB;border-radius:8px;padding:0 12px;font-size:14px;color:#0A0A0A;font-family:inherit;outline:none;box-sizing:border-box"
            >
          </div>
        </div>

        <!-- Financial fields -->
        <div style="display:grid;grid-template-columns:1fr 1fr 1fr;gap:14px">
          <div>
            <label style="display:block;font-size:13px;font-weight:500;color:#374151;margin-bottom:6px">Avans (₼)</label>
            <input
              v-model="form.totalAdvancedPaid"
              type="number"
              min="0"
              placeholder="0.00"
              style="width:100%;height:40px;border:1px solid #D1D5DB;border-radius:8px;padding:0 12px;font-size:14px;color:#0A0A0A;font-family:inherit;outline:none;box-sizing:border-box"
            >
          </div>
          <div>
            <label style="display:block;font-size:13px;font-weight:500;color:#374151;margin-bottom:6px">Ret. %</label>
            <input
              v-model="form.retainagePercentage"
              type="number"
              min="0"
              max="100"
              placeholder="0"
              style="width:100%;height:40px;border:1px solid #D1D5DB;border-radius:8px;padding:0 12px;font-size:14px;color:#0A0A0A;font-family:inherit;outline:none;box-sizing:border-box"
            >
          </div>
          <div>
            <label style="display:block;font-size:13px;font-weight:500;color:#374151;margin-bottom:6px">Saxlanılan (₼)</label>
            <input
              v-model="form.retainageHeldAmount"
              type="number"
              min="0"
              placeholder="0.00"
              style="width:100%;height:40px;border:1px solid #D1D5DB;border-radius:8px;padding:0 12px;font-size:14px;color:#0A0A0A;font-family:inherit;outline:none;box-sizing:border-box"
            >
          </div>
        </div>
      </div>

      <!-- Footer -->
      <div style="display:flex;justify-content:flex-end;gap:10px;padding:16px 24px;border-top:1px solid #F3F4F6;background:#FAFBFC">
        <button
          style="height:38px;padding:0 18px;background:#fff;color:#374151;border:1px solid #D1D5DB;border-radius:8px;font-size:14px;font-weight:500;cursor:pointer;font-family:inherit"
          @click="emit('close')"
        >
          Ləğv et
        </button>
        <button
          :disabled="loading"
          style="height:38px;padding:0 20px;background:#3D5AF1;color:#fff;border:none;border-radius:8px;font-size:14px;font-weight:600;cursor:pointer;font-family:inherit;display:inline-flex;align-items:center;gap:8px"
          @click="save"
        >
          <UIcon v-if="loading" name="i-lucide-loader-2" style="width:15px;height:15px;animation:spin 1s linear infinite" />
          {{ loading ? 'Saxlanılır...' : 'Saxla' }}
        </button>
      </div>
    </div>
  </div>
</template>

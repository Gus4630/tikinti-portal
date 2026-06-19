<script setup lang="ts">
const props = defineProps<{
  building?: Record<string, unknown> | null
  groups: Record<string, unknown>[]
}>()

const emit = defineEmits<{
  close: []
  saved: []
}>()

const config = useRuntimeConfig()
const auth = useAuthStore()

const form = reactive({
  name: String(props.building?.name ?? ''),
  address: String(props.building?.address ?? ''),
  description: String(props.building?.description ?? ''),
  floorAreaM2: props.building?.floorAreaM2 ? String(props.building.floorAreaM2) : '',
  budgetLimit: props.building?.budgetLimit ? String(props.building.budgetLimit) : '',
  groupId: props.building?.groupId ? String(props.building.groupId) : '',
})

const loading = ref(false)
const error = ref('')

const isEdit = computed(() => !!props.building?.id)
const title = computed(() => isEdit.value ? 'Binanı redaktə et' : 'Yeni bina')

async function save() {
  if (!form.name.trim()) { error.value = 'Ad tələb olunur'; return }
  error.value = ''
  loading.value = true
  try {
    const body: Record<string, unknown> = {
      name: form.name.trim(),
      address: form.address.trim() || undefined,
      description: form.description.trim() || undefined,
      floorAreaM2: form.floorAreaM2 ? Number(form.floorAreaM2) : undefined,
      budgetLimit: form.budgetLimit ? Number(form.budgetLimit) : undefined,
      groupId: form.groupId || undefined,
    }
    const headers = { Authorization: `Bearer ${auth.token}` }
    if (isEdit.value) {
      await $fetch(`/api/v1/buildings/${props.building!.id}`, {
        baseURL: config.public.apiBase, method: 'PUT', headers, body,
      })
    } else {
      await $fetch('/api/v1/buildings', {
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
    <div style="position:relative;background:#fff;border-radius:12px;width:100%;max-width:500px;box-shadow:0 20px 60px rgba(0,0,0,0.18);overflow:hidden">
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
            placeholder="məs. Yasamal Kompleks"
            style="width:100%;height:40px;border:1px solid #D1D5DB;border-radius:8px;padding:0 12px;font-size:14px;color:#0A0A0A;font-family:inherit;outline:none;box-sizing:border-box"
          >
        </div>

        <!-- Address -->
        <div>
          <label style="display:block;font-size:13px;font-weight:500;color:#374151;margin-bottom:6px">Ünvan</label>
          <input
            v-model="form.address"
            type="text"
            placeholder="məs. Bakı, Yasamal r-nu"
            style="width:100%;height:40px;border:1px solid #D1D5DB;border-radius:8px;padding:0 12px;font-size:14px;color:#0A0A0A;font-family:inherit;outline:none;box-sizing:border-box"
          >
        </div>

        <!-- Description -->
        <div>
          <label style="display:block;font-size:13px;font-weight:500;color:#374151;margin-bottom:6px">Açıqlama</label>
          <textarea
            v-model="form.description"
            rows="2"
            placeholder="Qısa açıqlama..."
            style="width:100%;border:1px solid #D1D5DB;border-radius:8px;padding:10px 12px;font-size:14px;color:#0A0A0A;font-family:inherit;outline:none;resize:vertical;box-sizing:border-box"
          />
        </div>

        <!-- Floor area + Budget limit side by side -->
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:14px">
          <div>
            <label style="display:block;font-size:13px;font-weight:500;color:#374151;margin-bottom:6px">Sahə (m²)</label>
            <input
              v-model="form.floorAreaM2"
              type="number"
              min="0"
              placeholder="0"
              style="width:100%;height:40px;border:1px solid #D1D5DB;border-radius:8px;padding:0 12px;font-size:14px;color:#0A0A0A;font-family:inherit;outline:none;box-sizing:border-box"
            >
          </div>
          <div>
            <label style="display:block;font-size:13px;font-weight:500;color:#374151;margin-bottom:6px">Büdcə (₼)</label>
            <input
              v-model="form.budgetLimit"
              type="number"
              min="0"
              placeholder="0.00"
              style="width:100%;height:40px;border:1px solid #D1D5DB;border-radius:8px;padding:0 12px;font-size:14px;color:#0A0A0A;font-family:inherit;outline:none;box-sizing:border-box"
            >
          </div>
        </div>

        <!-- Group -->
        <div v-if="groups.length">
          <label style="display:block;font-size:13px;font-weight:500;color:#374151;margin-bottom:6px">Qrup</label>
          <select
            v-model="form.groupId"
            style="width:100%;height:40px;border:1px solid #D1D5DB;border-radius:8px;padding:0 12px;font-size:14px;background:#fff;color:#0A0A0A;font-family:inherit;outline:none;box-sizing:border-box"
          >
            <option value="">— Seçin —</option>
            <option v-for="g in groups" :key="String(g.id)" :value="g.id">{{ g.name }}</option>
          </select>
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

interface ContextGroup {
  id: string
  name: string
}

interface ContextBuilding {
  id: string
  name: string
}

export const useContextStore = defineStore('context', () => {
  const config = useRuntimeConfig()
  const auth = useAuthStore()

  const activeGroupId = ref<string | null>(null)
  const activeBuildingId = ref<string | null>(null)
  const groups = ref<ContextGroup[]>([])
  const buildings = ref<ContextBuilding[]>([])
  const groupsLoading = ref(false)
  const buildingsLoading = ref(false)

  const headers = computed(() => ({ Authorization: `Bearer ${auth.token}` }))

  async function loadBuildings() {
    if (!activeGroupId.value || !auth.token) return
    buildingsLoading.value = true
    try {
      const res = await $fetch<{ content: ContextBuilding[] }>('/api/v1/buildings/search', {
        baseURL: config.public.apiBase,
        method: 'POST',
        headers: headers.value,
        body: { page: 0, perPage: 100, groupId: activeGroupId.value },
      })
      buildings.value = res.content ?? []

      const savedBuilding = localStorage.getItem('tk_building')
      if (savedBuilding && buildings.value.some(b => b.id === savedBuilding)) {
        activeBuildingId.value = savedBuilding
      } else if (buildings.value.length > 0) {
        activeBuildingId.value = buildings.value[0]?.id ?? null
        if (activeBuildingId.value) localStorage.setItem('tk_building', activeBuildingId.value)
      } else {
        activeBuildingId.value = null
        localStorage.removeItem('tk_building')
      }
    } catch {
      buildings.value = []
      activeBuildingId.value = null
    } finally {
      buildingsLoading.value = false
    }
  }

  async function loadGroups() {
    if (!auth.token) return
    groupsLoading.value = true
    try {
      const all = await $fetch<ContextGroup[]>('/api/v1/groups', {
        baseURL: config.public.apiBase,
        headers: headers.value,
      })

      groups.value = all

      const savedGroup = localStorage.getItem('tk_group')
      if (savedGroup && groups.value.some(g => g.id === savedGroup)) {
        activeGroupId.value = savedGroup
      } else if (groups.value.length > 0) {
        activeGroupId.value = groups.value[0]?.id ?? null
        if (activeGroupId.value) localStorage.setItem('tk_group', activeGroupId.value)
      }

      if (activeGroupId.value) {
        await loadBuildings()
      }
    } catch {
      groups.value = []
    } finally {
      groupsLoading.value = false
    }
  }

  async function setGroup(id: string) {
    activeGroupId.value = id
    activeBuildingId.value = null
    buildings.value = []
    localStorage.setItem('tk_group', id)
    localStorage.removeItem('tk_building')
    await loadBuildings()
  }

  function setBuilding(id: string) {
    activeBuildingId.value = id
    localStorage.setItem('tk_building', id)
  }

  async function hydrate() {
    const savedGroup = localStorage.getItem('tk_group')
    const savedBuilding = localStorage.getItem('tk_building')
    if (savedGroup) activeGroupId.value = savedGroup
    if (savedBuilding) activeBuildingId.value = savedBuilding
    await loadGroups()
  }

  return {
    activeGroupId,
    activeBuildingId,
    groups,
    buildings,
    groupsLoading,
    buildingsLoading,
    hydrate,
    loadGroups,
    loadBuildings,
    setGroup,
    setBuilding,
  }
})

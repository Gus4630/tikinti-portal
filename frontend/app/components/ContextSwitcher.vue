<script setup lang="ts">
const context = useContextStore()
</script>

<template>
  <div style="display:flex;align-items:center;gap:6px">
    <UIcon
      v-if="context.groupsLoading || context.buildingsLoading"
      name="i-lucide-loader-2"
      style="width:14px;height:14px;color:#9CA3AF;animation:spin 1s linear infinite;flex:none"
    />

    <template v-if="context.groups.length > 0">
      <div style="display:flex;align-items:center;gap:5px">
        <UIcon name="i-lucide-layers" style="width:13px;height:13px;color:#9CA3AF;flex:none" />
        <select
          :value="context.activeGroupId ?? ''"
          style="height:30px;border:1px solid #E5E7EB;border-radius:7px;padding:0 28px 0 8px;font-size:12.5px;background:#F9FAFB;color:#374151;font-family:inherit;cursor:pointer;outline:none;max-width:130px;appearance:auto"
          @change="context.setGroup(($event.target as HTMLSelectElement).value)"
        >
          <option v-for="g in context.groups" :key="g.id" :value="g.id">{{ g.name }}</option>
        </select>
      </div>

      <span v-if="context.buildings.length > 0" style="color:#D1D5DB;font-size:14px;line-height:1">/</span>

      <div v-if="context.buildings.length > 0" style="display:flex;align-items:center;gap:5px">
        <UIcon name="i-lucide-building-2" style="width:13px;height:13px;color:#9CA3AF;flex:none" />
        <select
          :value="context.activeBuildingId ?? ''"
          style="height:30px;border:1px solid #E5E7EB;border-radius:7px;padding:0 28px 0 8px;font-size:12.5px;background:#F9FAFB;color:#374151;font-family:inherit;cursor:pointer;outline:none;max-width:150px;appearance:auto"
          @change="context.setBuilding(($event.target as HTMLSelectElement).value)"
        >
          <option v-for="b in context.buildings" :key="b.id" :value="b.id">{{ b.name }}</option>
        </select>
      </div>
    </template>
  </div>
</template>

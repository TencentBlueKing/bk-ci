<template>
    <div class="bk-button-group">
        <bk-button
            v-for="item in publicVarModes"
            size="small"
            :key="item.id"
            :class="item.cls"
            :disabled="isModeSwitching"
            :loading="isModeSwitching"
            @click="updateMode(item.id)"
        >
            {{ item.label }}
        </bk-button>
    </div>
</template>

<script setup>
    import { computed } from 'vue'
    import UseInstance from '@/hook/useInstance'
    const { proxy } = UseInstance()
    import { UI_MODE } from '@/utils/pipelineConst'
    const isModeSwitching = computed(() => proxy.$store.state?.publicVar?.isModeSwitching)
    const publicVarMode = computed(() => proxy.$store.state?.publicVar?.publicVarMode)
    const modeList = computed(() => proxy.$store.state?.publicVar?.modeList)
    const publicVarModes = computed(() => {
        // code模式暂不上线
        return modeList.value.filter(i => i === UI_MODE).map((mode) => ({
            label: proxy.$t(`details.${mode}`),
            disabled: true,
            id: mode,
            cls: publicVarMode.value === mode ? 'is-selected' : ''
        }))
    })
        
    async function updateMode (mode) {
        if (isModeSwitching.value) {
            return
        }
        await proxy.$store.dispatch('publicVar/updatePublicVarMode', mode)
        proxy.$emit('change', mode)
    }
</script>
<style lang="scss" scoped>
.bk-button-group {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
}
</style>

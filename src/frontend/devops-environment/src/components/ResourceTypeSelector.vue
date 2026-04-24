<template>
    <div class="resource-selector">
        <div class="resource-type">
            <span>{{ displayName }}</span>
        </div>
    </div>
</template>

<script setup>
    import { ref, computed, defineProps } from 'vue'
    import {
        SERVICE_RESOURCE_TYPE
    } from '@/store/constant'
    import UseInstance from '@/hooks/useInstance'
    const { proxy } = UseInstance()
    const activeValue = ref(SERVICE_RESOURCE_TYPE.PIPELINE)
    const displayName = computed(() => {
        const nameMap = {
            [SERVICE_RESOURCE_TYPE.PIPELINE]: proxy.$t('pipeline'),
            [SERVICE_RESOURCE_TYPE.FEFLOW]: proxy.$t('feflow')
        }
        return nameMap[activeValue.value]
    })
</script>

<style lang="scss" scoped>
    .resource-selector {
        width: 210px;
        height: 40px;
        background-color: #fff;
        border: 1px solid #DCDEE5;
        box-shadow: 0 0 8px 0 #DCDEE5;
        border-radius: 3px;
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 0 10px;
        cursor: pointer;
    }
</style>

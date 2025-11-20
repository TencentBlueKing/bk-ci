<template>
    <bk-resize-layout
        ref="resizeLayoutRef"
        class="template-group-section"
        collapsible
        :min="280"
        :max="400"
        :initial-divide="initialDivide"
        @collapse-change="handleCollapseChange"
        @after-resize="afterResize"
    >
        <TemplateGroupAside slot="aside" />
        <router-view slot="main" />
    </bk-resize-layout>
</template>

<script setup>
    import {
        TEMPLATE_ASIDE_PANEL_TOGGLE,
        TEMPLATE_ASIDE_WIDTH_CACHE
    } from '@/store/modules/templates/constants'
    import { onMounted, ref } from 'vue'
    import TemplateGroupAside from './TemplateGroupAside'
    const resizeLayoutRef = ref(null)
    const initialDivide = ref(Number(localStorage.getItem(TEMPLATE_ASIDE_WIDTH_CACHE)) || 280)
    function handleCollapseChange (val) {
        localStorage.setItem(TEMPLATE_ASIDE_PANEL_TOGGLE, JSON.stringify(val))
    }
    function afterResize (width) {
        localStorage.setItem(TEMPLATE_ASIDE_WIDTH_CACHE, JSON.stringify(width))
    }

    onMounted(() => {
        if (localStorage.getItem(TEMPLATE_ASIDE_PANEL_TOGGLE) === 'true') {
            console.log(resizeLayoutRef.value, 'resizeLayoutRef.value')
            resizeLayoutRef.value.setCollapse(true)
        }
    })
</script>
<style lang="scss">
    .template-group-section {
        flex: 1;
        display: flex;
        overflow: hidden;
        border-top: 0;
    }
</style>

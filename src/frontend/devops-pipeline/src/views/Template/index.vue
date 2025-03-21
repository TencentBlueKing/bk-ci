<template>
    <bk-resize-layout
        ref="resizeLayout"
        class="template-group-section"
        collapsible
        :min="280"
        :max="400"
        :initial-divide="initialDivide"
        @collapse-change="handleCollapseChange"
        @after-resize="afterResize"
    >
        <TemplateGroupAside slot="aside" />
        <templateList slot="main" />
    </bk-resize-layout>
</template>

<script setup>
    import { onMounted, ref } from 'vue'
    import TemplateGroupAside from './TemplateGroupAside'
    import templateList from './List/'
    import {
        PIPELINE_ASIDE_PANEL_TOGGLE,
        PIPELINE_GROUP_ASIDE_WIDTH_CACHE
    } from '@/store/constants'

    const initialDivide = ref(Number(localStorage.getItem(PIPELINE_GROUP_ASIDE_WIDTH_CACHE)) || 280)
    function handleCollapseChange (val) {
        localStorage.setItem(PIPELINE_ASIDE_PANEL_TOGGLE, JSON.stringify(val))
    }

    function afterResize (width) {
        localStorage.setItem(PIPELINE_GROUP_ASIDE_WIDTH_CACHE, JSON.stringify(width))
    }
    
    onMounted(() => {
        if (localStorage.getItem(PIPELINE_ASIDE_PANEL_TOGGLE) === 'true') {
            this.$refs.resizeLayout.setCollapse(true)
        }
    })
</script>
<style lang="scss">
    .template-group-section {
        flex: 1;
        display: flex;
        overflow: hidden;
    }
</style>

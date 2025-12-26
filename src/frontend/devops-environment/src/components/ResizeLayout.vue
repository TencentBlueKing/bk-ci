<template>
    <bk-resize-layout
        ref="resizeLayout"
        :initial-divide="initialDivide"
        collapsible
        :border="false"
        :min="240"
        :max="360"
        ext-cls="environment-resize-layout"
        @collapse-change="handleCollapseChange"
        @after-resize="afterResize"
    >
        <template #aside>
            <slot name="aside" />
        </template>

        <template #main>
            <div :style="{ width: `${mainWidth}px`, height: 'calc(100% - 48px)' }">
                <slot
                    name="content"
                    :main-width="mainWidth"
                />
            </div>
        </template>
    </bk-resize-layout>
</template>

<script setup>
    import { defineProps, toRef } from 'vue'
    import { useResizeLayout } from '@/hooks/useResizeLayout'
    
    const props = defineProps({
        containerWidth: {
            type: Number,
            default: 1920
        }
    })

    const {
        resizeLayout,
        initialDivide,
        mainWidth,
        handleCollapseChange,
        afterResize,
        setCollapse
    } = useResizeLayout({
        defaultWidth: 240,
        containerWidth: toRef(props, 'containerWidth'),
        autoInit: true
    })
    
    defineExpose({
        setCollapse
    })
</script>

<style lang="scss" scoped>
    .environment-resize-layout {
        height: 100%;
        overflow: hidden;
    }
</style>

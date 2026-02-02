<template>
    <span
        style="font-size:0"
        v-bind="$attrs"
        v-on="listeners"
    >
        <svg
            :width="size"
            :height="size"
            style="fill: currentColor; stroke: currentColor"
        >
            <title v-if="title">{{ title }}</title>
            <use v-bind="{ 'xlink:href': svgHref }"></use>
        </svg>
    </span>
</template>

<script setup>
    import { computed } from 'vue'
    import { useListeners } from './hooks/useListeners'

    const props = defineProps({
        name: String,
        size: {
            type: [String, Number],
            default: 18
        },
        title: {
            type: String
        }
    })

    // 使用统一的useListeners Hook处理事件监听器兼容性
    const listeners = useListeners()

    const svgHref = computed(() => {
        const defaultId = '#bk-pipeline-order'
        if (typeof props.name !== 'string') {
            return defaultId
        }
        const id = `bk-pipeline-${props.name.toLowerCase()}`
        return document.getElementById(id) ? `#${id}` : defaultId
    })
</script>

<template>
    <div
        class="metrics-grid"
        :style="{ gridTemplateColumns: `repeat(${columns}, 1fr)` }"
    >
        <slot></slot>
    </div>
</template>

<script>
    import { defineComponent } from 'vue'

    export default defineComponent({
        name: 'MetricsGrid',
        props: {
            columns: {
                type: Number,
                default: 3
            }
        }
    })
</script>

<style lang="scss" scoped>
.metrics-grid {
    display: grid;
    gap: 16px;
}
</style>

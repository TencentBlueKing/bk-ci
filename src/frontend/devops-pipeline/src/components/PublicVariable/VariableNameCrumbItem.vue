<template>
    <span class="variable-name-crumb">
        <span>{{ variableName }}</span>
    </span>
</template>

<script>
    import { defineComponent } from 'vue'

    export default defineComponent({
        props: {
            variableName: {
                type: String,
                default: '--'
            }
        }
    })
</script>

<style lang="scss" scoped>
    .variable-name-crumb {
        display: flex;
        align-items: center;
        grid-gap: 8px;
    }
</style>

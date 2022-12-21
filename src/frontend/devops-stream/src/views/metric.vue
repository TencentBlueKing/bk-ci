<template>
    <iframe :src="metricUrl" class="iframe"></iframe>
</template>

<script>
    import {
        defineComponent,
        computed
    } from '@vue/composition-api'
    import store from '@/store'

    export default defineComponent({
        setup () {
            const metricUrl = computed(() => {
                const projectId = store.state.projectId
                return `/metrics/${projectId}`
            })
            return {
                metricUrl
            }
        }
    })
</script>

<style lang="postcss" scoped>
    .iframe {
        position: absolute;
        border: none;
    }
</style>

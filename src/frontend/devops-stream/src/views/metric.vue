<template>
    <iframe :src="metricUrl"></iframe>
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
                return `//${DEVOPS_HOST}/console/metrics/${projectId}`
            })
            return {
                metricUrl
            }
        }
    })
</script>

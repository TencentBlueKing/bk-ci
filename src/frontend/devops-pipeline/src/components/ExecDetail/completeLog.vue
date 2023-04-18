<template>
    <detail-container @close="$emit('close')"
        :title="execDetail.pipelineName"
        :status="execDetail.status"
        current-tab="log"
    >
        <template v-slot:content>
            <plugin-log :build-id="execDetail.id" :execute-count="executeCount" />
        </template>
    </detail-container>
</template>

<script>
    import { mapState } from 'vuex'
    import detailContainer from './detailContainer'
    import pluginLog from './log/pluginLog'

    export default {
        components: {
            detailContainer,
            pluginLog
        },
        props: {
            executeCount: {
                type: Number,
                default: 1
            }
        },

        computed: {
            ...mapState('atom', [
                'execDetail'
            ])
        }
    }
</script>

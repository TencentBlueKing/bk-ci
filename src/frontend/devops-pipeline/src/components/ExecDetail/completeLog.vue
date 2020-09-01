<template>
    <detail-container @close="$emit('close')"
        :title="execDetail.pipelineName"
        :status="execDetail.status"
        current-tab="log"
    >
        <template v-slot:content>
            <plugin-log :build-id="execDetail.id" :execute-count="1" />
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

        computed: {
            ...mapState('atom', [
                'execDetail'
            ])
        }
    }
</script>

<template>
    <detail-container @close="$emit('close')"
        :title="logData.name"
        :status="logData.status"
    >
        <plugin-log :job-index="jobIndex"
            :stage-index="stageIndex"
            :plugin-index="pluginIndex"
            :matrix-index="matrixIndex"
            :plugin="logData"
            ref="log"
        />
    </detail-container>
</template>

<script>
    import detailContainer from './detailContainer'
    import pluginLog from './log/pluginLog'

    export default {
        components: {
            detailContainer,
            pluginLog
        },

        props: {
            logData: Object,
            jobIndex: Number,
            pluginIndex: Number,
            stageIndex: Number,
            matrixIndex: Number
        },

        data () {
            return {
                buildId: this.$route.params.buildId
            }
        }
    }
</script>

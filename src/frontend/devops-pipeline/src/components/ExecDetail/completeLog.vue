<template>
    <detail-container
        @close="$emit('close')"
        :title="execDetail.pipelineName"
        :status="execDetail.status"
        current-tab="log"
    >
        <template v-slot:content>
            <plugin-log
                :build-id="execDetail.id"
                :exec-detail="execDetail"
                :execute-count="executeCount"
                :progress-task-id="runningTask.id"
                :progress-task-status="runningTask.status"
                :progress-task-execute-count="runningTask.executeCount || executeCount"
            />
        </template>
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
            executeCount: {
                type: Number,
                default: 1
            },
            execDetail: {
                type: Object,
                required: true
            }
        },
        computed: {
            runningTask () {
                const stages = this.execDetail?.model?.stages ?? []
                for (const stage of stages) {
                    const containers = stage.containers ?? []
                    for (const container of containers) {
                        const groupContainers = container.groupContainers?.length ? container.groupContainers : [container]
                        for (const groupContainer of groupContainers) {
                            const task = groupContainer.elements?.find(element => element.status === 'RUNNING')
                            if (task) return task
                        }
                    }
                }
                return {}
            }
        }
    }
</script>

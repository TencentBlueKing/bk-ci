import { bus } from '@/utils/bus'
import progressBar from '@/components/devops/progressBar'
import triggers from '@/components/pipeline/triggers'
import extMenu from '@/components/pipelineList/extMenu'
export default {
    components: {
        progressBar,
        triggers,
        extMenu
    },
    methods: {
        getHistoryURL (pipelineId) {
            return `${WEB_URL_PIRFIX}/pipeline/${this.$route.params.projectId}/${pipelineId}/history`
        },
        emitEventHandler (eventName, pipelineId) {
            bus.$emit(eventName, pipelineId)
        },
        goHistory (e, pipelineId) {
            const withAltKey = e.metaKey || e.altKey
            if (withAltKey) {
                window.open(this.getHistoryURL(pipelineId), '_blank')
            } else {
                this.emitEventHandler('title-click', pipelineId)
            }
        },
        triggersExec ({ pipelineId, ...params }) {
            bus.$emit('triggers-exec', params, pipelineId)
        },
        applyPermission ({ pipelineName, pipelineId }) {
            bus.$emit(
                'set-permission',
                this.$permissionResourceMap.pipeline,
                this.$permissionActionMap.view,
                [{
                    id: pipelineId,
                    name: pipelineName
                }],
                this.$route.params.projectId
            )
        }
    }

}

<template>
    <detail-container @close="$emit('close')" :title="stage.name" :status="stage.status">
        <template v-slot:content>
            <stage-review-control
                :stage="stage"
                :stage-index="editingElementPos.stageIndex"
                :disabled="true"
            />
        </template>
    </detail-container>
</template>

<script>
    import { mapState, mapGetters } from 'vuex'
    import detailContainer from './detailContainer'
    import StageReviewControl from '@/components/StagePropertyPanel/StageReviewControl'
    export default {
        components: {
            detailContainer,
            StageReviewControl
        },

        computed: {
            ...mapState('atom', ['editingElementPos']),
            ...mapGetters('atom', {
                execDetail: 'getExecDetail'
            }),

            stage () {
                const { editingElementPos, execDetail } = this
                if (editingElementPos) {
                    const model = execDetail.model || {}
                    const stages = model.stages || []
                    const stage = stages[editingElementPos.stageIndex]
                    return stage
                }
                return null
            }
        }
    }
</script>

<style lang="scss" scoped>
.pipeline-stage-review-control {
  padding: 10px 50px;
}
</style>

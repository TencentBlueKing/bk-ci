<template>
    <detail-container @close="$emit('close')"
        :title="stage.name"
        :status="stage.status"
        :current-tab="currentTab"
    >
        <span class="head-tab" slot="tab">
            <!-- <span @click="currentTab = 'log'" :class="{ active: currentTab === 'log' }">{{ $t('execDetail.log') }}</span> -->
            <span @click="currentTab = 'setting'" :class="{ active: currentTab === 'setting' }">{{ $t('execDetail.setting') }}</span>
        </span>
        <template v-slot:content>
            <stage-log
                v-if="currentTab === 'log'"
                :id="stage.id"
                :build-id="execDetail.id"
                :stage="stage"
                :execute-count="stage.executeCount"
            />
            <stage-content
                v-else
                :stage="stage"
                :stage-index="editingElementPos.stageIndex"
                :editable="false"
            />
        </template>
    </detail-container>
</template>

<script>
    import detailContainer from './detailContainer'
    import StageContent from '@/components/StagePropertyPanel/StageContent.vue'
    import StageLog from './log/stageLog.vue'

    export default {
        components: {
            detailContainer,
            StageContent,
            StageLog
        },
        props: {
            execDetail: {
                type: Object,
                required: true
            },
            editingElementPos: {
                type: Object,
                required: true
            }
        },
        data () {
            return {
                currentTab: 'setting'
            }
        },

        computed: {

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
    ::v-deep .stage-property-panel {
        padding: 10px 50px;
    }
</style>

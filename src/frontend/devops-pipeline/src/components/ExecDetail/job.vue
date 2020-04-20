<template>
    <detail-container @closeLog="$emit('closeLog')"
        :title="currentJob.name"
        :status="currentJob.status"
        :current-tab="currentTab"
    >
        <span class="head-tab" slot="tab">
            <span @click="currentTab = 'log'" :class="{ active: currentTab === 'log' }">日志</span><span @click="currentTab = 'setting'" :class="{ active: currentTab === 'setting' }">配置</span>
        </span>
        <span slot="tool"
            v-if="currentTab === 'setting'"
            class="tool-debug"
            @click="startDebug"
        >{{ $t('editPage.docker.debugConsole') }}</span>
        <template v-slot:content>
            <job-log v-show="currentTab === 'log'"
                :plugin-list="pluginList"
                :build-id="execDetail.id"
                ref="jobLog"
            />
            <container-content v-show="currentTab === 'setting'"
                :container-index="editingElementPos.containerIndex"
                :stage-index="editingElementPos.stageIndex"
                :stages="execDetail.model.stages"
                :editable="false"
            />
        </template>
    </detail-container>
</template>

<script>
    import { mapState } from 'vuex'
    import jobLog from './log/jobLog'
    import detailContainer from './detailContainer'
    import ContainerContent from '@/components/ContainerPropertyPanel/ContainerContent'

    export default {
        components: {
            detailContainer,
            jobLog,
            ContainerContent
        },

        data () {
            return {
                showTime: false,
                searchStr: '',
                currentTab: 'log'
            }
        },

        computed: {
            ...mapState('atom', [
                'execDetail',
                'editingElementPos'
            ]),

            downLoadJobLink () {
                const editingElementPos = this.editingElementPos
                const fileName = encodeURI(encodeURI(`${editingElementPos.stageIndex + 1}-${editingElementPos.containerIndex + 1}-${this.currentJob.name}`))
                const jobId = this.currentJob.containerId
                // to add job exec
                const currentExe = 1
                return `${AJAX_URL_PIRFIX}/log/api/user/logs/${this.$route.params.projectId}/${this.$route.params.pipelineId}/${this.execDetail.id}/download?jobId=${jobId}&executeCount=${currentExe}&fileName=${fileName}`
            },

            currentJob () {
                const { editingElementPos, execDetail } = this
                const model = execDetail.model || {}
                const stages = model.stages || []
                const currentStage = stages[editingElementPos.stageIndex] || []
                return currentStage.containers[editingElementPos.containerIndex]
            },

            pluginList () {
                const startUp = { name: 'Set up job', status: this.currentJob.startVMStatus, id: `startVM-${this.currentJob.id}` }
                return [startUp, ...this.currentJob.elements]
            }
        }
    }
</script>

<style lang="scss" scoped>
    /deep/ .container-property-panel {
        padding: 10px 50px;
        overflow: auto;
    }
    .tool-debug {
        cursor: pointer;
        font-size: 14px;
        margin-right: 5px;
        color: #3c96ff;
    }
</style>

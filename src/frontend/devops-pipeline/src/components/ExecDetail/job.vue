<template>
    <log-container @closeLog="$emit('closeLog')"
        @showSearchLog="showSearchLog"
        :title="currentJob.name"
        :status="currentJob.status"
        :show-time.sync="showTime"
        :search-str.sync="searchStr"
        :down-load-link="downLoadJobLink"
        :current-tab="currentTab"
    >
        <span class="head-tab" slot="tab">
            <span @click="currentTab = 'log'" :class="{ active: currentTab === 'log' }">日志</span><span @click="currentTab = 'setting'" :class="{ active: currentTab === 'setting' }">配置</span>
        </span>
        <template v-slot:content>
            <job-log v-show="currentTab === 'log'"
                :show-time="showTime"
                :plugin-list="pluginList"
                :build-id="execDetail.id"
                :search-str="searchStr"
                ref="jobLog"
            />
            <container-content v-show="currentTab === 'setting'"
                :container-index="editingElementPos.containerIndex"
                :stage-index="editingElementPos.stageIndex"
                :stages="execDetail.model.stages"
                :editable="false"
            />
        </template>
    </log-container>
</template>

<script>
    import { mapState } from 'vuex'
    import jobLog from './jobLog'
    import logContainer from './logContainer'
    import ContainerContent from '@/components/ContainerPropertyPanel/ContainerContent'

    export default {
        components: {
            logContainer,
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
        },

        methods: {
            showSearchLog (res) {
                this.$refs.jobLog.showSearchLog(res)
            }
        }
    }
</script>

<style lang="scss" scoped>
    /deep/ .container-property-panel {
        padding: 0 50px;
        overflow: auto;
    }
</style>

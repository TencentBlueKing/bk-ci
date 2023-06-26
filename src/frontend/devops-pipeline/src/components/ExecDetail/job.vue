<template>
    <detail-container @close="$emit('close')"
        :title="currentJob.name"
        :status="currentJob.status"
        :current-tab="currentTab"
    >
        <span class="head-tab" slot="tab">
            <span @click="currentTab = 'log'" :class="{ active: currentTab === 'log' }">{{ $t('execDetail.log') }}</span>
            <span @click="currentTab = 'setting'" :class="{ active: currentTab === 'setting' }">{{ $t('execDetail.setting') }}</span>
        </span>
        <span slot="tool"
            v-if="currentTab === 'setting' && showDebugDockerBtn"
            class="head-tool"
            @click="handleDebug"
        >{{ $t('editPage.docker.debugConsole') }}</span>
        <template v-slot:content>
            <error-summary v-if="activeErorr && currentTab === 'log'" :error="activeErorr"></error-summary>
            <template v-if="currentTab === 'log'">
                <plugin-log :id="currentJob.containerHashId"
                    :key="currentJob.containerHashId"
                    :build-id="execDetail.id"
                    :exec-detail="execDetail"
                    :current-tab="currentTab"
                    :execute-count="currentJob.executeCount"
                    type="containerLog"
                    ref="jobLog"
                    v-if="currentJob.matrixGroupFlag"
                />
                <job-log
                    v-else
                    :key="currentJob.id"
                    :plugin-list="pluginList"
                    :build-id="execDetail.id"
                    :down-load-link="downLoadJobLink"
                    :execute-count="executeCount"
                    ref="jobLog"
                />
            </template>
            <container-content v-if="currentTab === 'setting'"
                :container-index="editingElementPos.containerIndex"
                :container-group-index="editingElementPos.containerGroupIndex"
                :stage-index="editingElementPos.stageIndex"
                :stages="execDetail.model.stages"
                :editable="false"
                ref="container"
            />
        </template>
    </detail-container>
</template>

<script>
    import { mapGetters } from 'vuex'
    import jobLog from './log/jobLog'
    import pluginLog from './log/pluginLog'
    import detailContainer from './detailContainer'
    import ErrorSummary from '@/components/ExecDetail/ErrorSummary'
    import ContainerContent from '@/components/ContainerPropertyPanel/ContainerContent'

    export default {
        components: {
            detailContainer,
            jobLog,
            pluginLog,
            ContainerContent,
            ErrorSummary
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
                showTime: false,
                searchStr: '',
                currentTab: 'log'
            }
        },

        computed: {
            ...mapGetters('atom', [
                'checkShowDebugDockerBtn'
            ]),
            downLoadJobLink () {
                const editingElementPos = this.editingElementPos
                const fileName = encodeURI(encodeURI(`${editingElementPos.stageIndex + 1}-${editingElementPos.containerIndex + 1}-${this.currentJob.name}`))
                const jobId = this.currentJob.containerId
                return `${API_URL_PREFIX}/log/api/user/logs/${this.$route.params.projectId}/${this.$route.params.pipelineId}/${this.execDetail.id}/download?jobId=${jobId}&fileName=${fileName}`
            },

            currentJob () {
                const { editingElementPos, execDetail } = this
                const model = execDetail.model || {}
                const stages = model.stages || []
                const currentStage = stages[editingElementPos.stageIndex] || []

                try {
                    if (editingElementPos.containerGroupIndex === undefined) {
                        return currentStage.containers[editingElementPos.containerIndex]
                    } else {
                        return currentStage.containers[editingElementPos.containerIndex].groupContainers[editingElementPos.containerGroupIndex]
                    }
                } catch (_) {
                    return {}
                }
            },

            pluginList () {
                const startUp = { name: 'Set up job', status: this.currentJob.startVMStatus, id: `startVM-${this.currentJob.id}`, executeCount: this.currentJob.executeCount || 1 }
                return [startUp, ...this.currentJob.elements]
            },

            showDebugDockerBtn () {
                return this.checkShowDebugDockerBtn(this.currentJob, this.$route.name, this.execDetail)
            },

            executeCount () {
                const executeCountList = this.pluginList.map((plugin) => plugin.executeCount || 1)
                return Math.max(...executeCountList)
            },
            activeErorr () {
                return null
                // try {
                //     return this.execDetail.errorInfoList.find(error => error.containerId === this.currentJob.id && !error.taskId)
                // } catch (error) {
                //     return null
                // }
            }
        },
        methods: {
            handleDebug () {
                this.$refs.container?.startDebug?.()
            }
        }
    }
</script>

<style lang="scss" scoped>
    ::v-deep .container-property-panel {
        padding: 10px 50px;
        overflow: auto;
        .bk-form-item.is-required .bk-label, .bk-form-inline-item.is-required .bk-label {
            margin-right: 10px;
        }
    }
</style>

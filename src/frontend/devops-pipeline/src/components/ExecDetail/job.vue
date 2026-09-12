<template>
    <div>
        <detail-container
            @close="$emit('close')"
            :title="currentJob.name || ''"
            :position="panelPosition"
            :status="currentJob.status"
            :current-tab="currentTab"
            :ignore-close="configOpen"
        >
            <span
                class="head-tab"
                slot="tab"
            >
                <span
                    @click="currentTab = 'log'"
                    :class="{ active: currentTab === 'log' }"
                >{{ $t('execDetail.log') }}</span>
                <span
                    @click="currentTab = 'setting'"
                    :class="{ active: currentTab === 'setting' }"
                >{{ $t('execDetail.setting') }}</span>
            </span>
            <span
                slot="tool"
                v-if="currentTab === 'setting' && showDebugDockerBtn"
                class="head-tool"
                @click="handleDebug"
            >{{ $t('editPage.docker.debugConsole') }}</span>
            <template v-slot:content>
                <error-summary
                    v-if="activeErorr && currentTab === 'log' && useLegacyLog"
                    :error="activeErorr"
                ></error-summary>
                <template v-if="currentTab === 'log'">
                    <plugin-log
                        :id="currentJob.containerHashId"
                        :key="currentJob.containerHashId"
                        :build-id="execDetail.id"
                        :exec-detail="execDetail"
                        :current-tab="currentTab"
                        :execute-count="currentJob.executeCount"
                        type="containerLog"
                        ref="jobLog"
                        v-if="currentJob.matrixGroupFlag"
                    />
                    <job-log-panel
                        v-else-if="!useLegacyLog"
                        :key="'v2-' + currentJob.id"
                        :build-id="execDetail.id"
                        :exec-detail="execDetail"
                        :job="currentJob"
                        :plugins="pluginList"
                        ref="jobLog"
                        @fallback="useLegacyLog = true"
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
                <job-config-view
                    v-if="currentTab === 'setting'"
                    :rows="jobConfigRows"
                    @view-config="configOpen = true"
                />
            </template>
        </detail-container>
        <bk-sideslider
            :is-show.sync="configOpen"
            :width="640"
            :quick-close="true"
            :z-index="2100"
            :title="$t('logPanel.jobConfigTitle')"
            class="job-config-slider"
        >
            <container-content
                v-if="configOpen"
                slot="content"
                :container-index="editingElementPos.containerIndex"
                :container-group-index="editingElementPos.containerGroupIndex"
                :stage-index="editingElementPos.stageIndex"
                :stages="execDetail.model.stages"
                :editable="false"
                :pipeline="pipeline"
                ref="container"
            />
        </bk-sideslider>
    </div>
</template>

<script>
    import { mapGetters } from 'vuex'
    import jobLog from './log/jobLog'
    import pluginLog from './log/pluginLog'
    import JobLogPanel from './log-panel/JobLogPanel'
    import JobConfigView from './log-panel/JobConfigView'
    import { positionCode, buildJobConfigRows } from './log-panel/logPanelAdapter'
    import detailContainer from './detailContainer'
    import ErrorSummary from '@/components/ExecDetail/ErrorSummary'
    import ContainerContent from '@/components/ContainerPropertyPanel/ContainerContent'

    export default {
        components: {
            detailContainer,
            jobLog,
            pluginLog,
            JobLogPanel,
            JobConfigView,
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
            },
            pipeline: {
                type: Object
            }
        },
        data () {
            return {
                showTime: false,
                searchStr: '',
                currentTab: 'log',
                useLegacyLog: false,
                configOpen: false
            }
        },

        computed: {
            ...mapGetters('atom', [
                'checkShowDebugDockerBtn'
            ]),
            panelPosition () {
                return positionCode(this.editingElementPos, 2)
            },
            downLoadJobLink () {
                const editingElementPos = this.editingElementPos
                const fileName = encodeURI(encodeURI(`${editingElementPos.stageIndex + 1}-${editingElementPos.containerIndex + 1}-${this.currentJob.name}`))
                const jobId = this.currentJob.containerHashId
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
            jobConfigRows () {
                return buildJobConfigRows(this.currentJob)
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
            }
        },
        methods: {
            handleDebug () {
                this.configOpen = true
                this.$nextTick(() => this.$refs.container?.startDebug?.())
            }
        }
    }
</script>

<style lang="scss" scoped>
    ::v-deep .container-property-panel {
        padding: 10px 24px 24px;
        overflow: auto;
        .bk-form-item.is-required .bk-label, .bk-form-inline-item.is-required .bk-label {
            margin-right: 10px;
        }
    }
</style>
<style lang="scss">
    .job-config-slider {
        .bk-sideslider-content {
            overflow: auto;
            background: #fff;
        }
    }
</style>

<template>
    <div>
        <bk-pipeline
            class="detail-stages"
            :editable="false"
            :is-exec-detail="true"
            :match-rules="[]"
            :pipeline="pipeline"
            @click="handlePipelineClick"
            @stage-check="handleStageCheck"
            @stage-retry="handleRetry"
        ></bk-pipeline>
        <stage-review-panel></stage-review-panel>
        <bk-dialog v-model="showRetryStageDialog"
            render-directive="if"
            ext-cls="stage-retry-dialog"
            ok-text="Confirm"
            cancel-text="Cancle"
            :width="400"
            :auto-close="false"
            :loading="isRetrying"
            @confirm="confirmRetry"
        >
            <bk-radio-group v-model="failedContainer">
                <bk-radio :value="false">{{$t('pipeline.allJobs')}}</bk-radio>
                <bk-radio :value="true">{{$t('pipeline.allFailedJobs')}}</bk-radio>
            </bk-radio-group>
        </bk-dialog>
        <template v-if="editingElementPos != null">
            <plugin-log
                v-if="editingElementPos.logData"
                v-bind="editingElementPos"
                @close="closeLog"
            />
            <job-log
                v-else-if="Number.isInteger(editingElementPos.jobIndex)"
                v-bind="editingElementPos"
                :job="getJob(editingElementPos)"
                :stages="pipeline.stages"
                @close="closeLog"
            />
        </template>
    </div>
</template>

<script>
    import { mapState, mapActions } from 'vuex'
    import { pipelines } from '@/http'
    import stageReviewPanel from '@/components/StageReviewPanel'
    import pluginLog from '@/components/exec-detail/single-log.vue'
    import jobLog from '@/components/exec-detail/job'

    export default {
        components: {
            stageReviewPanel,
            pluginLog,
            jobLog
        },

        props: {
            pipeline: {
                type: Object,
                required: true
            }
        },

        data () {
            return {
                showRetryStageDialog: false,
                failedContainer: false,
                isRetrying: false,
                taskId: null,
                editingElementPos: null,
                firstIn: true
            }
        },

        computed: {
            ...mapState(['projectId', 'permission', 'curPipeline'])
        },

        watch: {
            pipeline (val) {
                if (val.stages?.length > 0 && this.firstIn) {
                    this.firstIn = false
                    this.autoOpenReview()
                    this.autoOpenLog()
                }
            }
        },

        methods: {
            ...mapActions([
                'toggleStageReviewPanel',
                'togglePropertyPanel'
            ]),

            getJob ({ stageIndex, jobIndex, matrixIndex }) {
                try {
                    if (Number.isInteger(matrixIndex)) {
                        return this.pipeline.stages[stageIndex].containers[jobIndex].groupContainers[matrixIndex]
                    } else {
                        return this.pipeline.stages[stageIndex].containers[jobIndex]
                    }
                } catch (error) {
                    console.trace()
                    return null
                }
            },

            handlePipelineClick (args) {
                const { elementIndex, containerGroupIndex, containerIndex, stageIndex } = args
                this.editingElementPos = {
                    pluginIndex: elementIndex,
                    matrixIndex: containerGroupIndex,
                    jobIndex: containerIndex,
                    stageIndex: stageIndex
                }
                const job = this.getJob(this.editingElementPos)
                if (Number.isInteger(elementIndex)) {
                    this.editingElementPos.logData = job.elements[elementIndex]
                } else if (!Reflect.has(args, 'containerGroupIndex')) {
                    this.editingElementPos.logData = job
                } else if (Number.isInteger(containerIndex)) {
                    this.editingElementPos.job = job
                }
            },

            closeLog () {
                this.editingElementPos = null
            },

            autoOpenReview () {
                const query = this.$route.query || {}
                const checkIn = query.checkIn
                const checkOut = query.checkOut
                this.pipeline.stages.every(stage => {
                    if (stage.id === checkIn) {
                        return this.handleStageCheck('checkIn')
                    } else if (stage.id === checkOut) {
                        return this.handleStageCheck('checkOut')
                    }
                    return true
                })
            },

            autoOpenLog () {
                if (this.$route.query.stageIndex) {
                    const { stageIndex, elementIndex, containerGroupIndex, containerIndex } = this.$route.query
                    const params = {
                        // stream 流水线隐藏了第一个stage,所以减去1
                        stageIndex: Number(stageIndex) - 1,
                        elementIndex: Number(elementIndex),
                        containerIndex: Number(containerIndex)
                    }
                    if (containerGroupIndex) params.containerGroupIndex = Number(containerGroupIndex)
                    this.handlePipelineClick(params)
                }
            },

            handleStageCheck ({ type, stageIndex }) {
                const stage = this.pipeline.stages[stageIndex]
                this.toggleStageReviewPanel({
                    isShow: true,
                    type,
                    stage
                })
            },

            showStageCheck (stageControl = {}) {
                const hasReviewFlow = stageControl.manualTrigger
                const hasReviewQuality = stageControl.ruleIds && stageControl.ruleIds.length > 0
                return !this.isFinallyStage && (hasReviewFlow || hasReviewQuality)
            },

            handleRetry ({ taskId, skip = false }) {
                if (!this.curPipeline.enabled || !this.permission) return
                this.taskId = taskId
                this.showRetryStageDialog = true
            },

            confirmRetry (stageId) {
                this.isRetrying = true
                const routeParams = this.$route.params || {}
                const query = {
                    taskId: this.taskId,
                    failedContainer: this.failedContainer
                }
                pipelines.rebuildPipeline(this.projectId, routeParams.pipelineId, routeParams.buildId, query).then(() => {
                    this.showRetryStageDialog = false
                    this.$bkMessage({ theme: 'success', message: 'Re-run successful' })
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isRetrying = false
                    this.taskId = null
                })
            }
        }
    }
</script>

<style lang="postcss" scoped>
    .stages-home {
        display: flex;
        align-items: flex-start;
    }
</style>

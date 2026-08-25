<template>
    <div class="stage-steps">
        <template v-for="step in steps">
            <span
                v-if="isProgressStep(step)"
                v-bk-tooltips="progressTooltipConfig"
                :data-stageId="step.stageId"
                :class="`stage-step ${step.statusCls}`"
                :key="`${step.stageId}-progress`"
                @mouseenter="handleProgressHover(step)"
                @click.stop="handleStepClick(step)"
            >
                <logo
                    :class="`step-icon ${step.statusCls} ${getRunningCls(step.statusCls)}`"
                    :name="step.icon"
                    size="16"
                />
            </span>
            <span
                v-else-if="step.tooltip"
                v-bk-tooltips="step.tooltip"
                :data-stageId="step.stageId"
                :class="`stage-step ${step.statusCls}`"
                :key="`${step.stageId}-tooltip`"
                @click.stop="handleStepClick(step)"
            >
                <logo
                    :class="`step-icon ${step.statusCls} ${getRunningCls(step.statusCls)}`"
                    :name="step.icon"
                    size="16"
                />
            </span>
            <span
                v-else
                :data-stageId="step.stageId"
                :class="`stage-step ${step.statusCls}`"
                :key="`${step.stageId}-normal`"
                @click.stop="handleStepClick(step)"
            >
                <logo
                    :class="`step-icon ${step.statusCls} ${getRunningCls(step.statusCls)}`"
                    :name="step.icon"
                    size="16"
                />
            </span>
        </template>
        <div
            v-if="hasProgressLoader"
            class="stage-progress-tooltip-source"
        >
            <div
                :id="progressTooltipId"
                class="stage-progress-tooltip"
            >
                <i
                    v-if="currentProgress.loading"
                    class="devops-icon icon-circle-2-1 spin-icon"
                ></i>
                <p
                    v-else-if="currentProgress.error"
                    class="stage-progress-line"
                >
                    --
                </p>
                <template v-else>
                    <p class="stage-progress-line">{{ $t('completeness') }}{{ formatPercent(currentProgress.rate) }}</p>
                    <template v-if="currentProgress.tasks.length">
                        <p class="stage-progress-line">{{ $t('runningSteps') }}</p>
                        <p
                            v-for="(task, index) in currentProgress.tasks"
                            :key="index"
                            class="stage-progress-step"
                        >
                            [{{ task.taskExecutionOrder }}] {{ task.taskName }}: {{ formatPercent(task.taskProgressRete) }}
                        </p>
                    </template>
                    <template v-if="currentProgress.hasDetail">
                        <p class="stage-progress-divider"></p>
                        <p class="stage-progress-view">
                            {{ $t('progressDetail.clickToView') }}
                            <span
                                class="stage-progress-view-link"
                                @click="handleViewDetail"
                            >{{ $t('progressDetail.viewDetail') }}</span>
                        </p>
                    </template>
                </template>
            </div>
        </div>
    </div>
</template>

<script>
    import Logo from '@/components/Logo'
    export default {
        name: 'stage-steps',
        components: {
            Logo
        },
        props: {
            steps: {
                type: Array,
                default: () => []
            },
            buildId: {
                type: String,
                required: true
            },
            progressLoader: {
                type: Function,
                default: null
            }
        },
        data () {
            return {
                progressTooltipId: `stage-progress-tooltip-${this._uid}`,
                currentStep: null,
                currentProgress: {
                    loading: true,
                    error: false,
                    rate: 0,
                    tasks: [],
                    hasDetail: false
                }
            }
        },
        computed: {
            hasProgressLoader () {
                return typeof this.progressLoader === 'function'
            },
            progressTooltipConfig () {
                return {
                    allowHTML: true,
                    theme: 'dark',
                    placement: 'top',
                    interactive: true,
                    delay: [100, 0],
                    content: `#${this.progressTooltipId}`
                }
            }
        },
        methods: {
            handleStepClick (step) {
                this.$emit('show-progress-detail', step)
            },
            getRunningCls (statusCls) {
                return statusCls === 'RUNNING' ? ' spin-icon' : ''
            },
            isProgressStep (step) {
                return this.hasProgressLoader && step.statusCls === 'RUNNING'
            },
            formatPercent (value) {
                const percent = (Number(value) || 0) * 100
                return `${Math.round(percent * 100) / 100}%`
            },
            taskHasDetail (task) {
                const detail = task && task.progressDetail
                return !!(detail && (detail.progress || detail.subtasks?.items?.length || detail.timeline?.items?.length))
            },
            async handleProgressHover (step) {
                if (!this.isProgressStep(step)) return
                this.currentStep = step
                this.currentProgress = {
                    loading: true,
                    error: false,
                    rate: 0,
                    tasks: [],
                    hasDetail: false
                }
                try {
                    const data = await this.progressLoader(this.buildId, step.stageId)
                    if (this.currentStep !== step) return
                    const tasks = Array.isArray(data?.taskProgressList) ? data.taskProgressList : []
                    this.currentProgress = {
                        loading: false,
                        error: false,
                        rate: data?.stageProgressRete || 0,
                        tasks,
                        hasDetail: tasks.some(task => this.taskHasDetail(task))
                    }
                } catch (e) {
                    if (this.currentStep !== step) return
                    this.currentProgress = {
                        loading: false,
                        error: true,
                        rate: 0,
                        tasks: [],
                        hasDetail: false
                    }
                }
            },
            handleViewDetail () {
                if (this.currentStep) {
                    this.handleStepClick(this.currentStep)
                }
            }
        }
    }
</script>

<style lang="scss">
    @import "../../scss/conf";
    .stage-step {
        position: relative;
        margin-right: 8px;
        &:before {
            position: absolute;
            top: 5px;
            right: -7px;
            content: '';
            width: 6px;
            height: 1px;
            background-color: #A9ABB9;
        }
        &:last-child::before {
            display: none;
        }

        &.SUCCEED {
            &:before {
                background-color: $successColor;
            }
            .step-icon {
                fill: $successColor !important;
            }
        }
        &.FAILED  {
            &:before {
                background-color: $dangerColor;
            }
            .step-icon {
                fill: $dangerColor !important;
            }
        }
        &.RUNNING {
            &:before {
                background-color: $primaryColor;
            }
            .step-icon {
                fill: $primaryColor !important;
            }
        }
        &.SKIP {
            &:before {
                background-color: $borderLightColor;
            }
            .step-icon {
                fill: $borderLightColor !important;
            }
        }
        &.PAUSE  {
            &:before {
                background-color: $iconPrimaryColor;
            }
            .step-icon {
                fill: $iconPrimaryColor !important;
            }
        }

        .step-icon {
            display: inline-block;
            fill: $borderLightColor;
        }
    }

    .stage-progress-tooltip-source {
        display: none;
    }
    .stage-progress-tooltip {
        min-width: 160px;
        font-size: 12px;
        line-height: 20px;
        .stage-progress-line {
            margin: 0;
        }
        .stage-progress-step {
            margin: 0;
            padding-left: 12px;
        }
        .stage-progress-divider {
            height: 0;
            margin: 6px 0;
            border-top: 1px solid rgba(255, 255, 255, .15);
        }
        .stage-progress-view {
            margin: 0;
        }
        .stage-progress-view-link {
            color: #3a84ff;
            cursor: pointer;
        }
    }

</style>

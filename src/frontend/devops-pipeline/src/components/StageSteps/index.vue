<template>
    <div class="stage-steps">
        <template v-for="step in steps">
            <span v-if="step.tooltip" v-bk-tooltips="step.tooltip" :class="`stage-step ${step.statusCls}`" :key="step.icon">
                <logo :class="`step-icon ${step.statusCls} ${getRunningCls(step.statusCls)}`" :name="step.icon" size="16" />
            </span>
            <span v-else v-bk-tooltips="progressTooltips" @click.stop="getProgress(step)" :class="`stage-step ${step.statusCls}`" :key="step.icon">
                <logo :class="`step-icon ${step.statusCls} ${getRunningCls(step.statusCls)}`" :name="step.icon" size="16" />
            </span>
        </template>
    </div>
</template>

<script>
    import Logo from '@/components/Logo'
    import { PROCESS_API_URL_PREFIX } from '@/store/constants'
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
            }
        },
        data () {
            return {
                progressTooltips: {
                    html: '<i class="devops-icon icon-circle-2-1 spin-icon"/>',
                    placement: 'bottom',
                    trigger: 'click'
                }
            }
        },
        methods: {
            getRunningCls (statusCls) {
                return statusCls === 'RUNNING' ? ' spin-icon' : ''
            },
            async getProgress (step) {
                try {
                    console.log(step)

                    const { projectId, pipelineId } = this.$route.params
                    const { data } = await this.$ajax.get(`${PROCESS_API_URL_PREFIX}/user/builds/${projectId}/${pipelineId}/getStageProgressRate?buildId=${this.buildId}&stageId=${step.stageId}`)
                    Object.assign(this.progressTooltips, {
                        html: `
                            <p>${this.$t('completeness')}${Math.min(1, data.stageProgressRete) * 100}%</p>
                            <p>${this.$t('runningSteps')}</p>
                            ${data.taskProgressList.map(item =>
                                `<p style="text-indent: 12px">${[
                                    `[${item.taskOrder}]`,
                                    item.taskName,
                                    `${Math.min(1, item.taskProgressRete) * 100}%`
                                ].join('')}</p>`
                            ).join('')
                        }`
                    })
                } catch (error) {
                    this.$showTips({
                        theme: 'error',
                        message: error.message
                    })
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
                fill: $successColor;
            }
        }
        &.FAILED  {
            &:before {
                background-color: $dangerColor;
            }
            .step-icon {
                fill: $dangerColor;
            }
        }
        &.RUNNING {
            &:before {
                background-color: $primaryColor;
            }
            .step-icon {
                fill: $primaryColor;
            }
        }
        &.SKIP {
            &:before {
                background-color: $borderLightColor;
            }
            .step-icon {
                fill: $borderLightColor;
            }
        }
        &.PAUSE  {
            &:before {
                background-color: $iconPrimaryColor;
            }
            .step-icon {
                fill: $iconPrimaryColor;
            }
        }

        .step-icon {
            display: inline-block;
            fill: $borderLightColor;
        }
    }
</style>

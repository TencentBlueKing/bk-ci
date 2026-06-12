<template>
    <div class="stage-steps">
        <template v-for="step in steps">
            <span
                v-if="step.tooltip"
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
            }
        },
        methods: {
            handleStepClick (step) {
                this.$emit('show-progress-detail', step)
            },
            getRunningCls (statusCls) {
                return statusCls === 'RUNNING' ? ' spin-icon' : ''
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

</style>

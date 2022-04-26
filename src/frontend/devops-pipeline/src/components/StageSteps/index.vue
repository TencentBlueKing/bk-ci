<template>
    <div class="stage-steps">
        <template v-for="step in steps">
            <span v-if="step.tooltip" v-bk-tooltips="step.tooltip" :class="`stage-step ${step.statusCls}`" :key="step.icon">
                <logo :class="`step-icon ${step.statusCls} ${getRunningCls(step.statusCls)}`" :name="step.icon" size="16" />
            </span>
            <span v-else :class="`stage-step ${step.statusCls}`" :key="step.icon">
                <logo :class="`step-icon ${step.statusCls} ${getRunningCls(step.statusCls)}`" :name="step.icon" size="16" />
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
            }
        },
        methods: {
          getRunningCls (statusCls) {
            return statusCls === 'RUNNING' ? ' spin-icon' : '';
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

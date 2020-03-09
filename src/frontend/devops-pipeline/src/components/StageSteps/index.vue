<template>
    <div class="stage-steps">
        <template v-for="step in steps">
            <span v-if="step.tooltip" v-bk-tooltips="step.tooltip" class="stage-step" :key="step.icon">
                <logo :class="`step-icon ${step.statusCls}`" :name="step.icon" size="16" />
            </span>
            <span v-else class="stage-step" :key="step.icon">
                <logo :class="`step-icon ${step.statusCls}`" :name="step.icon" size="16" />
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
            background-color: #333;
        }
        &:last-child::before {
            display: none;
        }

        > .step-icon {
            display: inline-block;
            &.SUCCEED {
                fill: $successColor;
            }
            &.FAILED  {
                fill: $dangerColor;
            }
            // &.RUNNING {
            //     fill: $primaryColor;
            // }
            &.SKIP {
                fill: $borderLightColor;
            }
            &.PAUSE  {
                fill: $iconPrimaryColor;
            }
        }
    }
</style>

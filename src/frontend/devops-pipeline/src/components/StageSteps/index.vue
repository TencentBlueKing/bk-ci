<template>
    <div class="stage-steps">
        <template v-for="step in steps">
            <span v-if="step.tooltip" v-bk-tooltips="step.tooltip" class="stage-step" :key="step.icon">
                <i :class="`bk-icon icon-${step.icon} ${step.status}`" />
            </span>
            <span v-else class="stage-step" :key="step.icon">
                <i :class="`bk-icon icon-${step.icon} ${step.status}`" />
            </span>
        </template>
    </div>
</template>

<script>
    export default {
        name: 'stage-steps',
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
            top: 6px;
            right: -7px;
            content: '';
            width: 6px;
            height: 1px;
            background-color: #333;
        }
        &:last-child::before {
            display: none;
        }

        > i {
            display: inline-block;
            font-size: 16px;
            &.SUCCEED {
            color: $successColor;
            }
            &.FAILED  {
                color: $dangerColor;
            }
            // &.RUNNING {
            //     color: $primaryColor;
            // }
            &.PAUSE  {
                color: $iconPrimaryColor;
            }
        }
    }
</style>

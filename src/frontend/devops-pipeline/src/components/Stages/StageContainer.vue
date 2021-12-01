<template>
    <div
        ref="stageContainer"
        :class="{ 'devops-stage-container': true, 'first-stage-container': stageIndex === 0, 'readonly': !editable || containerDisabled, 'editing': isEditPage }"
    >
        <template v-if="containerIndex > 0">
            <cruve-line :straight="true" :width="60" :style="`margin-top: -${cruveHeight}px`" :height="cruveHeight" class="connect-line left" />
            <cruve-line :straight="true" :width="60" :style="`margin-top: -${cruveHeight}px`" :height="cruveHeight" :direction="false" class="connect-line right" />
        </template>
        <template v-else>
            <cruveLine v-if="stageIndex !== 0" class="first-connect-line connect-line left" :width="60" :height="60"></cruveLine>
            <cruve-line class="first-connect-line connect-line right" :width="60" :direction="false" :height="60"></cruve-line>
        </template>

        <component
            :is="getJobComp(container)"
            :key="container.containerId"
            :stage-index="stageIndex"
            :pre-container="preContainer"
            :container-index="containerIndex"
            :stage-length="stageLength"
            :editable="editable"
            :is-preview="isPreview"
            :can-skip-element="canSkipElement"
            :stage-disabled="stageDisabled"
            :container-length="containerLength"
            :container="container">
        </component>
    </div>
</template>

<script>
    import { getOuterHeight } from '@/utils/util'
    import Job from './Job'
    import MatrixGroup from './MatrixGroup'
    import CruveLine from '@/components/Stages/CruveLine'

    export default {
        components: {
            Job,
            MatrixGroup,
            CruveLine
        },
        props: {
            preContainer: Object,
            container: Object,
            stageIndex: Number,
            containerIndex: Number,
            stageLength: Number,
            containerLength: Number,
            stageDisabled: Boolean,
            editable: {
                type: Boolean,
                default: true
            },
            isPreview: {
                type: Boolean,
                default: false
            },
            canSkipElement: {
                type: Boolean,
                default: false
            }
        },
        data () {
            return {
                cruveHeight: 0
            }
        },
        computed: {
            isEditPage () {
                return this.$route.name === 'pipelinesEdit' || this.$route.name === 'templateEdit'
            },
            isDetailPage () {
                return this.$route.name === 'pipelinesDetail'
            },
            containerCls () {
                if (this.container.jobControlOption && this.container.jobControlOption.enable === false) {
                    return 'DISABLED'
                }
                
                return this.container && this.container.status ? this.container.status : ''
            },
            containerDisabled () {
                return !!(this.container.jobControlOption && this.container.jobControlOption.enable === false) || this.stageDisabled
            }
        },
        mounted () {
            this.updateCruveConnectHeight()
            if (this.containerDisabled) {
                this.container.runContainer = false
            }
        },
        updated () {
            this.updateCruveConnectHeight()
        },
        methods: {
            updateCruveConnectHeight () {
                if (this.$refs.stageContainer && this.$refs.stageContainer.previousSibling) {
                    this.cruveHeight = getOuterHeight(this.$refs.stageContainer.previousSibling)
                }
            },
            getJobComp (container) {
                return Job
                // if (!this.isDetailPage && container.matrixGroupFlag) {
                //     return MatrixGroup
                // } else {
                //     return Job
                // }
            }
        }
    }
</script>

<style lang="scss">
    @import "./Stage";
    .devops-stage-container {
        text-align: left;
        margin: 16px 20px 0 20px;
        position: relative;

        // 实心圆点
        &:after {
            content: '';
            width: $smalldotR;
            height: $smalldotR;
            position: absolute;
            right: -$smalldotR / 2;
            top: $itemHeight / 2 - ($smalldotR / 2 - 1);
            background: $primaryColor;
            border-radius: 50%;
        }
        // 三角箭头
        &:before {
            font-family: 'bk-icons-linear' !important;
            content: "\e94d";
            position: absolute;
            font-size: 13px;
            color: $primaryColor;
            left: -10px;
            top: $itemHeight / 2 - 13px / 2 + 1;
            z-index: 2;
        }

        &.first-stage-container {
            &:before {
                display: none;
            }
        }
        .container-title {
            display: flex;
            height: $itemHeight;
            background: #33333f;
            color: white;
            font-size: 14px;
            align-items: center;
            position: relative;
            margin: 0 0 16px 0;
            width: 240px;
            z-index: 3;
            > .container-name {
                @include ellipsis();
                flex: 1;
                padding: 0 12px;
                span:hover {
                    color: $primaryColor;
                }
            }

            .atom-canskip-checkbox {
                margin-right: 6px;
                &.is-disabled .bk-checkbox {
                    background-color: transparent;
                    border-color: #979BA4;
                }

            }
            input[type=checkbox] {
                border-radius: 3px;
            }
            .debug-btn {
                position: absolute;
                height: 100%;
                right: 0;
            }
            .copyJob {
                display: none;
                margin-right: 10px;
                fill: #c4c6cd;
                cursor: pointer;
                &:hover {
                    fill: $primaryColor;
                }
            }
            .close {
                @include add-plus-icon(#2E2E3A, #2E2E3A, #c4c6cd, 16px, true);
                @include add-plus-icon-hover($dangerColor, $dangerColor, white);
                border: none;
                display: none;
                margin-right: 10px;
                transform: rotate(45deg);
                cursor: pointer;
                &:before, &:after {
                    left: 7px;
                    top: 4px;
                }
            }

            &:hover {
                .copyJob, .close {
                    display: block;
                }
                .hover-hide {
                    display: none;
                }
            }

        }

        .connect-line {
            position: absolute;
            top: $itemHeight / 2 - 4;
            stroke: $primaryColor;
            stroke-width: 1;
            fill: none;
            z-index: 0;

             &.left {
                left: -$svgWidth + 4;

            }
            &.right {
                right: -$StageMargin - $addIconLeft - $addBtnSize - 2;
            }

            &.first-connect-line {
                height: 76px;
                width: $svgWidth;
                top: -$stageEntryHeight / 2 - 2 - 16px;
                &.left {
                    left: -$svgWidth - $addBtnSize / 2 + 4;
                }
                &.right {
                    left: auto;
                    right: -$addIconLeftMargin - $containerMargin - $addBtnSize / 2;

                }
            }
        }
    }
</style>

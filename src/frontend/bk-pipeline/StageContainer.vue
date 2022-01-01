<template>
    <div
        ref="stageContainer"
        :class="{
            'devops-stage-container': true,
            'first-stage-container': stageIndex === 0,
            'readonly': !editable || containerDisabled,
            'editing': editable
        }"
    >
        <template v-if="containerIndex === 0">
            <cruveLine v-if="stageIndex !== 0" class="first-connect-line connect-line left" :width="58" :height="60"></cruveLine>
            <cruve-line class="first-connect-line connect-line right" style="margin-left: 2px" :width="58" :direction="false" :height="60"></cruve-line>
        </template>
        <template v-if="containerIndex !== containerLength - 1">
            <cruve-line :straight="true" :width="58" :height="cruveHeight" class="connect-line left" />
            <cruve-line :straight="true" :width="58" :height="cruveHeight" :direction="false" class="connect-line right" />
        </template>
        <Component
            :is="jobComponentName"
            v-bind="jobComponentProps"
        />
    </div>
</template>

<script>
    import MatrixGroup from './MatrixGroup'
    import Job from './Job'
    import CruveLine from './CruveLine'
    import {
        getOuterHeight
    } from './util'

    export default {
        components: {
            CruveLine,
            MatrixGroup,
            Job
        },
        props: {
            stage: {
                type: Object,
                requiured: true
            },
            container: {
                type: Object,
                requiured: true
            },
            stageIndex: Number,
            containerIndex: Number,
            stageLength: Number,
            containerLength: Number,
            stageDisabled: Boolean,
            editable: {
                type: Boolean,
                default: true
            },
            isExecDetail: {
                type: Boolean,
                default: false
            },
            isPreview: {
                type: Boolean,
                default: false
            },
            canSkipElement: {
                type: Boolean,
                default: false
            },
            handleChange: {
                type: Function,
                required: true
            },
            userName: {
                type: String,
                default: 'unknow'
            },
            matchRules: {
                type: Array,
                default: []
            }
        },
        data () {
            return {
                cruveHeight: 0
            }
        },
        computed: {
            containerDisabled () {
                return !!(this.container.jobControlOption && this.container.jobControlOption.enable === false) || this.stageDisabled
            },
            isMatrix () {
                return this.isExecDetail && this.container.matrixGroupFlag && this.container.groupContainers
            },
            jobComponentName () {
                return this.isMatrix ? MatrixGroup : Job
            },
            jobComponentProps () {
                return {
                    ...(this.isMatrix
                        ? {
                            matrix: this.container
                        }
                        : {
                            container: this.container
                        }),
                    stageIndex: this.stageIndex,
                    containerDisabled: this.containerDisabled,
                    containerIndex: this.containerIndex,
                    containerLength: this.containerLength,
                    stageDisabled: this.stageDisabled,
                    editable: this.editable,
                    isPreview: this.isPreview,
                    handleChange: this.handleChange,
                    userName: this.userName,
                    matchRules: this.matchRules,
                    canSkipElement: this.canSkipElement,
                    updateCruveConnectHeight: this.updateCruveConnectHeight
                }
            }
        },
        watch: {
            'container.elements.length': function (newVal, oldVal) {
                if (newVal !== oldVal) {
                    this.updateCruveConnectHeight()
                }
            }
        },
        mounted () {
            this.updateCruveConnectHeight()
            if (this.containerDisabled) {
                this.handleChange(this.container, { runContainer: false })
            }
        },
        updated () {
            this.updateCruveConnectHeight()
        },
        methods: {
            updateCruveConnectHeight () {
                this.$nextTick(() => {
                    if (this.$refs.stageContainer) {
                        this.cruveHeight = getOuterHeight(this.$refs.stageContainer)
                    }
                })
            }
        }
    }
</script>

<style lang="scss">
    @use "sass:math";
    @import "./index";
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
            right: math.div(-$smalldotR, 2);
            top: math.div($itemHeight, 2) - (math.div($smalldotR, 2) - 1);
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
            top: math.div($itemHeight, 2) - math.div(13px, 2) + 1;
            z-index: 2;
        }

        &.first-stage-container {
            &:before {
                display: none;
            }
        }

        .connect-line {
            position: absolute;
            top: math.div($itemHeight, 2) - 4;
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
                top: math.div(-$stageEntryHeight, 2) - 2 - 16px;
                &.left {
                    left: -$svgWidth - math.div($addBtnSize, 2) + 4;
                }
                &.right {
                    left: auto;
                    right: -$addIconLeftMargin - $containerMargin - math.div($addBtnSize, 2);

                }
            }
        }
    }
</style>

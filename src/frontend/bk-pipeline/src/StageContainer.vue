<template>
    <div
        ref="stageContainer"
        :class="{
            'devops-stage-container': true,
            'first-stage-container': stageIndex === 0,
            'last-stage-container': stageIndex === stageLength - 1,
            'readonly': !reactiveData.editable || containerDisabled,
            'editing': reactiveData.editable
        }"
    >
        <Logo v-if="stageIndex !== 0" size="12" name="right-shape" class="container-connect-triangle" />
        <template v-if="containerIndex === 0">
            <cruve-line v-if="stageIndex !== 0" class="first-connect-line connect-line left" :width="58" :height="60" />
            <cruve-line v-if="showLastCruveLine" class="first-connect-line connect-line right" style="margin-left: 2px" :width="58" :direction="false" :height="60" />
        </template>
        <template v-if="containerIndex !== containerLength - 1">
            <cruve-line v-if="stageIndex !== 0" :straight="true" :width="58" :height="cruveHeight" class="connect-line left" />
            <cruve-line v-if="showLastCruveLine" :straight="true" :width="58" :height="cruveHeight" :direction="false" class="connect-line right" />
        </template>
        <Component
            :is="jobComponentName"
            v-bind="jobComponentProps"
            v-on="$listeners"
            ref="jobBox"
        />
    </div>
</template>

<script>
    import MatrixGroup from './MatrixGroup'
    import Job from './Job'
    import Logo from './Logo'
    import CruveLine from './CruveLine'
    import {
        getOuterHeight
    } from './util'

    export default {
        components: {
            CruveLine,
            MatrixGroup,
            Logo,
            Job
        },
        props: {
            stage: {
                type: Object,
                required: true
            },
            container: {
                type: Object,
                required: true
            },
            stageIndex: Number,
            containerIndex: Number,
            stageLength: Number,
            containerLength: Number,
            stageDisabled: Boolean,
            isFinallyStage: {
                type: Boolean,
                default: false
            },
            handleChange: {
                type: Function,
                required: true
            }
        },
        inject: [
            'reactiveData'
        ],
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
                return this.reactiveData.isExecDetail && this.container.matrixGroupFlag && this.container.groupContainers
            },
            showLastCruveLine () {
                return (this.stageIndex !== this.stageLength - 1 || this.reactiveData.editable) && !this.isFinallyStage
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
                    updateCruveConnectHeight: this.updateCruveConnectHeight,
                    disabled: this.containerDisabled,
                    ...this.$props
                }
            }
        },
        mounted () {
            this.resizeObserver = new ResizeObserver((entries) => {
                this.updateCruveConnectHeight()
            })
            this.resizeObserver.observe(this.$el)
        },

        beforeDestroy () {
            this.resizeObserver?.unobserve?.(this.$el)
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
    @import "./conf";
    .devops-stage-container {
        text-align: left;
        margin: 16px 20px 24px 20px;
        position: relative;

        // 实心圆点
        &:not(.last-stage-container):after {
            content: '';
            width: $smalldotR;
            height: $smalldotR;
            position: absolute;
            right: math.div(-$smalldotR, 2);
            top: math.div($itemHeight, 2) - (math.div($smalldotR, 2) - 1);
            &:not(.readonly) {
                background: $primaryColor;
            }
            border-radius: 50%;
        }
        
        // 三角箭头
        .container-connect-triangle {
            position: absolute;
            color: $primaryColor;
            left: -9px;
            top: math.div($itemHeight, 2) - math.div(13px, 2) + 1;
            z-index: 2;
        }

        .connect-line {
            position: absolute;
            top: 1px;
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

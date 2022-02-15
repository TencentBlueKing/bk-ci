<template>
    <div
        ref="stageContainer"
        :class="{ 'devops-stage-container': true, 'first-stage-container': stageIndex === 0, 'readonly': !editable || containerDisabled, 'editing': isEditPage }"
    >
        <template v-if="containerIndex > 0">
            <cruve-line :straight="true" :width="60" :style="`margin-top: -${cruveHeight + 2}px`" :height="cruveHeight" class="connect-line left" />
            <cruve-line :straight="true" :width="46" :style="`margin-top: -${cruveHeight + 2}px`" :height="cruveHeight" :direction="false" class="connect-line right" />
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
    import { bus } from '@/utils/bus'

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
            containerDisabled () {
                return !!(this.container.jobControlOption && this.container.jobControlOption.enable === false) || this.stageDisabled
            }
        },
        mounted () {
            this.updateCruveConnectHeight()
            bus.$on('update-container-line', ({ stageIndex, containerIndex }) => {
                if (stageIndex === this.stageIndex && containerIndex === this.containerIndex) {
                    this.updateCruveConnectHeight()
                }
            })
        },
        updated () {
            this.updateCruveConnectHeight()
        },
        methods: {
            updateCruveConnectHeight () {
                if (this.$refs.stageContainer && this.$refs.stageContainer.previousSibling) {
                    this.$nextTick(() => {
                        this.cruveHeight = getOuterHeight(this.$refs.stageContainer.previousSibling)
                    })
                }
            },
            getJobComp (container) {
                if (this.isDetailPage && container.matrixGroupFlag && container.groupContainers) {
                    return MatrixGroup
                } else {
                    return Job
                }
            }
        }
    }
</script>

<style lang="scss">
    @use "sass:math";
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
            width: $svgWidth - 4;

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
</style>

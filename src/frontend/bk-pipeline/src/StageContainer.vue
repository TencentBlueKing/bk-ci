<template>
    <div
        ref="stageContainer"
        :class="{
            'devops-stage-container': true,
            'last-stage-container': stageIndex === stageLength - 1,
            'readonly': !reactiveData.editable || containerDisabled,
            'editing': reactiveData.editable
        }"
    >
        <Logo
            v-if="showLeftCruveLine"
            size="12"
            name="right-shape"
            class="container-connect-triangle"
        />
        <template v-if="containerIndex === 0">
            <cruve-line
                v-if="showLeftCruveLine"
                class="first-connect-line connect-line left"
                :width="58"
                :height="60"
            />
            <cruve-line
                v-if="showLastCruveLine"
                class="first-connect-line connect-line right"
                style="margin-left: 2px"
                :width="58"
                :direction="false"
                :height="60"
            />
        </template>
        <template v-if="containerIndex !== containerLength - 1">
            <cruve-line
                v-if="showLeftCruveLine"
                :straight="true"
                :width="58"
                :height="cruveHeight"
                class="connect-line left"
            />
            <cruve-line
                v-if="showLastCruveLine"
                :straight="true"
                :width="58"
                :height="cruveHeight"
                :direction="false"
                class="connect-line right"
            />
        </template>
        <Component
            :is="jobComponentName"
            v-bind="jobComponentProps"
            v-on="listeners"
            ref="jobBox"
        />
    </div>
</template>

<script setup>
    import { ref, computed, onMounted, onBeforeUnmount, nextTick, inject } from 'vue'
    import { useListeners } from './hooks/useListeners'
    import CruveLine from './CruveLine'
    import Job from './Job'
    import Logo from './Logo'
    import MatrixGroup from './MatrixGroup'
    import { getOuterHeight } from './util'

    const props = defineProps({
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
        isTriggerStage: {
            type: Boolean,
            default: false
        },
        isFinallyStage: {
            type: Boolean,
            default: false
        },
        handleChange: {
            type: Function,
            required: true
        }
    })

    const reactiveData = inject('reactiveData')
    const stageContainer = ref(null)
    const jobBox = ref(null)
    const cruveHeight = ref(0)
    let resizeObserver = null

    // 使用统一的useListeners Hook处理事件监听器兼容性
    const listeners = useListeners()

    const containerDisabled = computed(() => {
        return !!(props.container.jobControlOption && props.container.jobControlOption.enable === false) || props.stageDisabled
    })

    const isMatrix = computed(() => {
        return reactiveData.isExecDetail && props.container.matrixGroupFlag && props.container.groupContainers
    })

    const showLastCruveLine = computed(() => {
        return (props.stageIndex !== props.stageLength - 1 || reactiveData.editable) && !props.isFinallyStage
    })

    const showLeftCruveLine = computed(() => {
        return props.stageIndex > 0
    })

    const jobComponentName = computed(() => {
        return isMatrix.value ? MatrixGroup : Job
    })

    const jobComponentProps = computed(() => {
        return {
            ...(isMatrix.value
                ? {
                    matrix: props.container
                }
                : {
                    container: props.container
                }),
            updateCruveConnectHeight: updateCruveConnectHeight,
            disabled: containerDisabled.value,
            stage: props.stage,
            stageIndex: props.stageIndex,
            containerIndex: props.containerIndex,
            stageLength: props.stageLength,
            containerLength: props.containerLength,
            stageDisabled: props.stageDisabled,
            isTriggerStage: props.isTriggerStage,
            isFinallyStage: props.isFinallyStage,
            handleChange: props.handleChange
        }
    })

    const updateCruveConnectHeight = () => {
        nextTick(() => {
            if (stageContainer.value) {
                cruveHeight.value = getOuterHeight(stageContainer.value)
            }
        })
    }

    onMounted(() => {
        resizeObserver = new ResizeObserver(() => {
            updateCruveConnectHeight()
        })
        if (stageContainer.value) {
            resizeObserver.observe(stageContainer.value)
        }
    })

    onBeforeUnmount(() => {
        if (resizeObserver && stageContainer.value) {
            resizeObserver.unobserve(stageContainer.value)
        }
    })

    defineExpose({
        jobBox
    })
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

<template>
    <div
        ref="stageContainer"
        :class="{ &quot;soda-stage-container&quot;: true, &quot;first-container&quot;: stageIndex === 0, &quot;readonly&quot;: !editable || containerDisabled }"
    >
        <template v-if="!isOnlyOneContainer && containerLength - 1 !== containerIndex">
            <span class="connect-line left" :class="{ &quot;cruve&quot;: containerIndex === 0 }"></span>
            <span class="connect-line right" :class="{ &quot;cruve&quot;: containerIndex === 0 }"></span>
        </template>
        <show-tooltip placement="bottom" v-bind="containerTooltipConfig">
            <h3 :class="{ &quot;container-title&quot;: true, &quot;first-ctitle&quot;: containerIndex === 0, [container.status]: container.status }" @click="showContainerPanel">
                <status-icon type="container" :editable="editable" :job-option="container.jobControlOption" :status="container.status">
                    {{ containerSerialNum }}
                </status-icon>
                <p class="container-name" :title="container.name">
                    <span>{{ container.status === 'PREPARE_ENV' ? '准备构建环境中' : container.name }}</span>
                </p>
                <container-type :container="container" v-if="!showCheckedToatal"></container-type>

                <bk-checkbox v-if="showCheckedToatal" class="atom-canskip-checkbox" v-model="container.runContainer" :disabled="containerDisabled" @click.stop></bk-checkbox>
                <bk-button v-if="showDebugBtn" class="debug-btn" theme="warning" @click.stop="debugDocker">登录调试</bk-button>
            </h3>
        </show-tooltip>
        <atom-list :container="container" :editable="editable" :is-preview="isPreview" :stage-index="stageIndex" :container-index="containerIndex" :container-status="container.status">
        </atom-list>
    </div>
</template>

<script>
    import { mapActions, mapGetters, mapState } from 'vuex'
    import { getOuterHeight } from '@/utils/util'
    import ContainerType from './ContainerType'
    import AtomList from './AtomList'
    import showTooltip from '@/components/common/showTooltip'
    import StatusIcon from './StatusIcon'

    export default {
        components: {
            StatusIcon,
            ContainerType,
            AtomList,
            showTooltip
        },
        props: {
            container: Object,
            stageIndex: Number,
            containerIndex: Number,
            stageLength: Number,
            containerLength: Number,
            editable: {
                type: Boolean,
                default: true
            },
            isPreview: {
                type: Boolean,
                default: false
            }
        },
        data () {
            return {
                showContainerName: false,
                showAtomName: false
            }
        },
        computed: {
            ...mapState('atom', [
                'execDetail'
            ]),
            ...mapGetters('atom', [
                'isTriggerContainer',
                'isDockerBuildResource',
                'getAllContainers'
            ]),
            showCheckedToatal () {
                const { isTriggerContainer, container, $route } = this
                return $route.path.indexOf('preview') > 0 && !isTriggerContainer(container)
            },
            containerSerialNum () {
                return `${this.stageIndex + 1}-${this.containerIndex + 1}`
            },
            isOnlyOneContainer () {
                return this.containerLength === 1
            },
            projectId () {
                return this.$route.params.projectId
            },
            containerDisabled () {
                return !!(this.container.jobControlOption && this.container.jobControlOption.enable === false)
            },
            containerTooltipConfig () {
                let name, content
                switch (true) {
                    case this.isTriggerContainer(this.container):
                        name = 'build_trigger'
                        content = '点击构建触发可配置推荐版本号、流水线变量'
                        break
                }
                return !this.isPreview && name ? {
                    name,
                    content,
                    key: name
                } : {}
            }
        },
        watch: {
            'container.elements.length': function (newVal, oldVal) {
                if (newVal !== oldVal) {
                    this.$forceUpdate()
                }
            },
            'container.runContainer' (newVal) {
                const { container, updateContainer } = this
                const { elements } = container
                if (this.containerDisabled) return
                elements.filter(item => (item.additionalOptions === undefined || item.additionalOptions.enable)).map(item => {
                    item.canElementSkip = newVal
                    return false
                })
                updateContainer({
                    container,
                    newParam: {
                        elements
                    }
                })
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
            ...mapActions('atom', [
                'togglePropertyPanel',
                'addAtom',
                'deleteAtom',
                'updateContainer'
            ]),
            updateCruveConnectHeight () {
                if (!this.$refs.stageContainer) {
                    return
                }
                const height = `${getOuterHeight(this.$refs.stageContainer) - 12}px`
                Array.from(this.$refs.stageContainer.querySelectorAll('.connect-line')).map(el => {
                    el.style.height = height
                })
            },
            showContainerPanel () {
                const { stageIndex, containerIndex } = this
                this.togglePropertyPanel({
                    isShow: true,
                    editingElementPos: {
                        stageIndex,
                        containerIndex
                    }
                })
            }
        }
    }
</script>

<style lang="scss">
    @import "./Stage";
    .soda-stage-container {
        text-align: left;
        margin: 0 0 26px $StageMargin;
        position: relative;

        // 实心圆点
        &:after {
            content: '';
            width: $dotR;
            height: $dotR;
            position: absolute;
            right: -$dotR / 2;
            top: $itemHeight / 2 - ($dotR / 2 - 1);
            background: $primaryColor;
            border-radius: 50%;
        }
        // 三角箭头
        &:before {
            content: '';
            border: $angleSize solid transparent;
            border-left-color: $primaryColor;
            height: 0;
            width: 0;
            position: absolute;
            left: -$angleSize;
            top: $itemHeight / 2 - $angleSize + 1;
            z-index: 2;
        }

        &.first-container {
            margin-left: 0;
            &:before,
            .container-title:before {
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
            cursor: pointer;
            width: 240px;
            z-index: 3;
            > .container-name {
                flex: 1;
                padding: 0 15px;
                @include ellipsis();
                &:hover {
                    color: $primaryColor;
                }
            }
            .atom-canskip-checkbox {
                margin-right: 6px
            }
            input[type=checkbox] {
                border-radius: 3px
            }
            .debug-btn {
                position: absolute;
                height: 100%;
                right: 0;
            }

            // 实线
            &:before,
            &:after {
                content: '';
                position: absolute;
                border-top: 2px $lineStyle $primaryColor;
                width: $StagePadding;
                top: $itemHeight / 2;
                left: -$StagePadding;
            }
            &:after {
                right: -$StagePadding;
                left: auto;
            }

            &:not(.first-ctitle) {
                &:before,
                &:after {
                    width: $shortLine;
                    left: -$shortLine;
                }

                &:after {
                    left: auto;
                    right: -$shortLine;
                }
            }
        }
        .connect-line {
            position: absolute;
            top: $itemHeight / 2 + 1 + $lineRadius;
            @include cruve-connect ($lineRadius, $lineStyle, $primaryColor, false);
            &.left {
                left: -$StageMargin / 2 + (2 * $lineRadius);
            }
            &.right {
                @include cruve-connect ($lineRadius, $lineStyle, $primaryColor, true);
                right: -$StageMargin / 2  + (2 * $lineRadius);
            }
            &:not(.cruve) {
                &:before {
                    top: -$itemHeight / 2 + $lineRadius + 1;
                    transform: rotate(0);
                    border-radius: 0;
                }
            }
        }
    }

    .readonly {
        .connect-line {
            &.left,
            &.right {
                border-color: $lineColor;
                &:before {
                    border-right-color: $lineColor;
                }
                &:after {
                    border-bottom-color: $lineColor;
                }
            }
        }
        &:after {
            background: $lineColor;
        }
        // 三角箭头
        &:before {
            border-left-color: $lineColor;
        }
        .container-title {
            background-color: $fontWeightColor;

            &.RUNNING {
                background-color: $loadingColor;
            }
            &.PREPARE_ENV {
                background-color: $loadingColor;
            }
            &.CANCELED, &.REVIEWING, &.REVIEW_ABORT {
                background-color: $cancelColor;
            }
            &.FAILED, &.HEARTBEAT_TIMEOUT {
                background-color: $dangerColor;
            }
            &.SUCCEED {
                background-color: $successColor;
            }
            &:before,
            &:after {
                border-top-color: $lineColor;
            }
            > .container-name:hover {
                color: white;
            }
        }
    }
</style>

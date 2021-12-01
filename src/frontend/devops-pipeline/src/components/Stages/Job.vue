<template>
    <section>
        <h3 :class="{ 'container-title': true, 'first-ctitle': containerIndex === 0, [containerCls]: true }" @click.stop="showContainerPanel">
            <status-icon type="container" :editable="editable" :container-disabled="containerDisabled" :status="container.status" :depend-on-value="dependOnValue">
                {{ containerSerialNum }}
            </status-icon>
            <p class="container-name">
                <span :class="{ 'skip-name': containerDisabled || container.status === 'SKIP' }" :title="container.name">{{ container.status === 'PREPARE_ENV' ? $t('editPage.prepareEnv') : container.name }}</span>
            </p>
            <container-type :class="showCopyJob ? 'hover-hide' : ''" :container="container" v-if="!showCheckedToatal"></container-type>
            <span :title="$t('editPage.copyJob')" v-if="showCopyJob && !container.isError" class="devops-icon copyJob" @click.stop="copyContainer">
                <Logo name="copy" size="18"></Logo>
            </span>
            <i v-if="showCopyJob" @click.stop="deleteJob" class="add-plus-icon close" />
            <span @click.stop v-if="showCheckedToatal && canSkipElement">
                <bk-checkbox class="atom-canskip-checkbox" v-model="container.runContainer" :disabled="containerDisabled"></bk-checkbox>
            </span>
        </h3>
        <atom-list
            :container="container"
            :editable="editable"
            :is-preview="isPreview"
            :can-skip-element="canSkipElement"
            :stage-index="stageIndex"
            :container-index="containerIndex"
            :container-status="container.status"
            :container-disabled="containerDisabled"
        >
        </atom-list>
    </section>
</template>

<script>
    import { mapActions, mapGetters, mapState } from 'vuex'
    import { hashID, randomString } from '@/utils/util'
    import ContainerType from './ContainerType'
    import AtomList from './AtomList'
    import StatusIcon from './StatusIcon'
    import Logo from '@/components/Logo'

    export default {
        components: {
            StatusIcon,
            ContainerType,
            AtomList,
            Logo
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
                showContainerName: false,
                showAtomName: false,
                cruveHeight: 0
            }
        },
        computed: {
            ...mapState('atom', [
                'execDetail',
                'pipeline',
                'pipelineLimit'
            ]),
            ...mapGetters('atom', [
                'isTriggerContainer',

                'getAllContainers'
            ]),
            isEditPage () {
                return this.$route.name === 'pipelinesEdit' || this.$route.name === 'templateEdit'
            },
            containerCls () {
                if (this.container.jobControlOption && this.container.jobControlOption.enable === false) {
                    return 'DISABLED'
                }
                
                return this.container && this.container.status ? this.container.status : ''
            },
            showCheckedToatal () {
                const { isTriggerContainer, container, $route } = this
                return $route.path.indexOf('preview') > 0 && !isTriggerContainer(container)
            },
            showCopyJob () {
                const { isTriggerContainer, container, $route } = this
                return $route.path.indexOf('edit') > 0 && !isTriggerContainer(container) && this.editable
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
                return !!(this.container.jobControlOption && this.container.jobControlOption.enable === false) || this.stageDisabled
            },
            dependOnValue () {
                if (this.container.status !== 'DEPENDENT_WAITING') return ''
                let val = ''
                if (this.container.jobControlOption && this.container.jobControlOption.dependOnType) {
                    if (this.container.jobControlOption.dependOnType === 'ID') {
                        val = this.container.jobControlOption.dependOnId || []
                    } else {
                        val = this.container.jobControlOption.dependOnName || ''
                    }
                }
                return `${this.$t('storeMap.dependOn')} 【${val}】`
            }
        },
        watch: {
            'preContainer.elements.length': function (newVal, oldVal) {
                if (newVal !== oldVal) {
                    this.$forceUpdate()
                }
            },
            'container.runContainer' (newVal) {
                const { container, updateContainer } = this
                const { elements } = container
                if (this.containerDisabled && newVal) return
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
            if (this.containerDisabled) {
                this.container.runContainer = false
            }
        },
        methods: {
            ...mapActions('atom', [
                'togglePropertyPanel',
                'addAtom',
                'deleteAtom',
                'updateContainer',
                'setPipelineEditing',
                'deleteContainer',
                'deleteStage'
            ]),
            deleteJob () {
                const { containerIndex, stageIndex } = this
                const containers = this.pipeline.stages[stageIndex].containers || []

                if (containers.length === 1) {
                    this.deleteStage({
                        stageIndex
                    })
                } else {
                    this.deleteContainer({
                        stageIndex,
                        containerIndex
                    })
                }
            },
            showContainerPanel () {
                const { stageIndex, containerIndex } = this
                console.log(stageIndex, containerIndex, 553)
                this.togglePropertyPanel({
                    isShow: true,
                    editingElementPos: {
                        stageIndex,
                        containerIndex
                    }
                })
            },
            copyContainer () {
                if (this.containerLength >= this.pipelineLimit.jobLimit) {
                    this.$showTips({
                        theme: 'error',
                        message: this.$t('storeMap.jobLimit') + this.pipelineLimit.jobLimit
                    })
                    return
                }
                try {
                    const copyContainer = JSON.parse(JSON.stringify(this.container))
                    const container = {
                        ...copyContainer,
                        containerId: `c-${hashID(32)}`,
                        jobId: `job_${randomString(3)}`,
                        elements: copyContainer.elements.map(element => ({
                            ...element,
                            id: `e-${hashID(32)}`
                        })),
                        jobControlOption: copyContainer.jobControlOption ? {
                            ...copyContainer.jobControlOption,
                            dependOnType: 'ID',
                            dependOnId: []
                        } : undefined
                    }
                    this.pipeline.stages[this.stageIndex].containers.splice(this.containerIndex + 1, 0, JSON.parse(JSON.stringify(container)))
                    this.setPipelineEditing(true)
                } catch (e) {
                    console.error(e)
                    this.$showTips({
                        theme: 'error',
                        message: this.$t('editPage.copyJobFail')
                    })
                }
            }
        }
    }
</script>

<style lang="scss">
    @import "./Stage";
    .devops-stage-container {
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

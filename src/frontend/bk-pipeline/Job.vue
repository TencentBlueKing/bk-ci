<template>
    <div>
        <h3 :class="jobTitleCls"
            @click.stop="showContainerPanel"
        >
            <status-icon type="container" :editable="editable" :container-disabled="disabled" :status="container.status" :depend-on-value="dependOnValue">
                {{ containerSerialNum }}
            </status-icon>
            <p class="container-name">
                <span
                    :class="displayNameCls"
                    :title="displayName"
                >
                    {{ displayName }}
                </span>
            </p>
            <container-type :class="showCopyJob ? 'hover-hide' : ''" :container="container" v-if="!canSkipElement"></container-type>
            <span :title="$t('editPage.copyJob')" v-if="showCopyJob && !container.isError" class="devops-icon copyJob" @click.stop="copyContainer">
                <Logo name="copy" size="18"></Logo>
            </span>
            <i v-if="showCopyJob" @click.stop="deleteJob" class="add-plus-icon close" />
            <span @click.stop v-if="canSkipElement">
                <bk-checkbox class="atom-canskip-checkbox" v-model="container.runContainer" :disabled="disabled"></bk-checkbox>
            </span>
        </h3>
        <atom-list
            :stage="stage"
            :container="container"
            :editable="editable"
            :is-preview="isPreview"
            :can-skip-element="canSkipElement"
            :stage-index="stageIndex"
            :handle-change="handleChange"
            :user-name="userName"
            :container-index="containerIndex"
            :container-status="container.status"
            :match-rules="matchRules"
            :container-disabled="disabled"
        >
        </atom-list>
    </div>
</template>

<script>
    import {
        hashID,
        randomString,
        eventBus,
        isTriggerContainer,
        getDependOnDesc,
        isObject
    } from './util'
    import {
        DELETE_EVENT_NAME,
        COPY_EVENT_NAME,
        CLICK_EVENT_NAME,
        STATUS_MAP
    } from './constants'
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
            container: {
                type: Object,
                requiured: true
            },
            stageIndex: Number,
            containerIndex: Number,
            containerGroupIndex: Number,
            containerLength: Number,
            stageDisabled: Boolean,
            disabled: Boolean,
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
        emits: [
            DELETE_EVENT_NAME,
            COPY_EVENT_NAME,
            CLICK_EVENT_NAME
        ],
        data () {
            return {
                showContainerName: false,
                showAtomName: false,
                cruveHeight: 0
            }
        },
        computed: {
            jobTitleCls () {
                const status = this.container && this.container.status ? this.container.status : ''
                return {
                    'container-title': true,
                    'first-ctitle': this.containerIndex === 0,
                    DISABLED: this.disabled,
                    [status]: true
                }
            },
            displayNameCls () {
                return {
                    'skip-name': this.disabled || this.container.status === STATUS_MAP.SKIP
                }
            },
            displayName () {
                try {
                    const { matrixContext, status, name } = this.container
                    const suffix = isObject(matrixContext) ? Object.values(matrixContext).join(', ') : ''
                    const isPrepare = (status === STATUS_MAP.PREPARE_ENV && this.containerGroupIndex === undefined)
                    return isPrepare ? this.$t('editPage.prepareEnv') : `${name}${suffix ? `(${suffix})` : ''}`
                } catch (error) {
                    return 'unknow'
                }
            },
            showCopyJob () {
                return !isTriggerContainer(this.container) && this.editable
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
            
            dependOnValue () {
                const val = getDependOnDesc(this.container)
                return `${this.$t('storeMap.dependOn')} 【${val}】`
            }
        },
        watch: {
            'container.runContainer' (newVal) {
                const { elements } = this.container
                if (this.disabled && newVal) return
                elements.filter(item => (item.additionalOptions === undefined || item.additionalOptions.enable))
                    .forEach(item => {
                        item.canElementSkip = newVal
                    })
                this.handleChange(this.container, { elements })
            }
        },
        mounted () {
            if (this.disabled) {
                this.handleChange(this.container, { runContainer: false })
            }
        },
        methods: {
            deleteJob () {
                const { containerIndex, stageIndex, containerLength } = this

                this.$emit(DELETE_EVENT_NAME, {
                    stageIndex,
                    containerIndex: containerLength === 1 ? undefined : containerIndex
                })
            },
            showContainerPanel () {
                eventBus.$emit(CLICK_EVENT_NAME, {
                    stageIndex: this.stageIndex,
                    containerIndex: this.containerIndex,
                    containerGroupIndex: this.containerGroupIndex
                })
            },
            copyContainer () {
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
                        jobControlOption: copyContainer.jobControlOption
                            ? {
                                ...copyContainer.jobControlOption,
                                dependOnType: 'ID',
                                dependOnId: []
                            }
                            : undefined
                    }
                    this.$emit(COPY_EVENT_NAME, {
                        containerIndex: this.containerIndex,
                        container
                    })
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
        .container-title {
            display: flex;
            height: $itemHeight;
            background: #33333f;
            color: white;
            font-size: 14px;
            align-items: center;
            position: relative;
            margin: 0 0 16px 0;
            z-index: 3;
            > .container-name {
                @include ellipsis();
                flex: 1;
                padding: 0 6px;
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

    }
</style>

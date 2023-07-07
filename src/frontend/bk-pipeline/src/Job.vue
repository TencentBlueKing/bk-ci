<template>
    <div
        :class="{
            'un-exec-this-time': reactiveData.isExecDetail && isUnExecThisTime
        }"
        :id="container.id"
    >
        <h3 :class="jobTitleCls" @click.stop="showContainerPanel">
            <status-icon
                type="container"
                :editable="reactiveData.editable"
                :container-disabled="disabled"
                :status="containerStatus"
                :depend-on-value="dependOnValue"
            >
                {{ containerSerialNum }}
            </status-icon>
            <p class="container-name">
                <span :class="displayNameCls" :title="displayName">
                    {{ displayName }}
                </span>
            </p>
            <container-type
                :class="containerTypeCls"
                :container="container"
                v-if="!reactiveData.canSkipElement"
            ></container-type>
            <Logo
                v-if="showCopyJob && !container.isError"
                :title="t('copyJob')"
                class="copyJob"
                @click.stop="handleCopyContainer"
                name="clipboard"
                size="16"
            />
            <i v-if="showCopyJob" @click.stop="deleteJob" class="add-plus-icon close" />
            <span @click.stop v-if="reactiveData.canSkipElement">
                <bk-checkbox
                    class="atom-canskip-checkbox"
                    v-model="container.runContainer"
                    :disabled="disabled"
                ></bk-checkbox>
            </span>
            <Logo
                v-if="
                    (reactiveData.editable || reactiveData.isPreview) && container.matrixGroupFlag
                "
                name="matrix"
                size="16"
                class="matrix-flag-icon"
            >
            </Logo>
            <Logo
                v-if="showMatrixFold"
                name="angle-circle-down"
                size="18"
                @click.stop="toggleShowAtom()"
                :class="matrixFoldLogoCls"
            >
            </Logo>
            <bk-button
                v-if="showDebugBtn"
                class="debug-btn"
                theme="warning"
                @click.stop="debugDocker"
            >
                {{ t("debugConsole") }}
            </bk-button>
            <Logo
                v-if="container.locateActive"
                name="location-right"
                class="container-locate-icon"
                size="18"
            />
        </h3>
        <atom-list
            v-if="showAtomList || !showMatrixFold"
            ref="atomList"
            :stage="stage"
            :container="container"
            :stage-index="stageIndex"
            :handle-change="handleChange"
            :container-index="containerIndex"
            :container-group-index="containerGroupIndex"
            :container-status="containerStatus"
            :container-disabled="disabled"
        >
        </atom-list>
    </div>
</template>

<script>
    import { localeMixins } from './locale'
    import {
        eventBus,
        getDependOnDesc,
        hashID,
        isObject,
        isTriggerContainer,
        randomString
    } from './util'

    import AtomList from './AtomList'
    import ContainerType from './ContainerType'
    import Logo from './Logo'
    import StatusIcon from './StatusIcon'
    import {
        CLICK_EVENT_NAME,
        COPY_EVENT_NAME,
        DEBUG_CONTAINER,
        DELETE_EVENT_NAME,
        DOCKER_BUILD_TYPE,
        PUBLIC_BCS_BUILD_TYPE,
        PUBLIC_DEVCLOUD_BUILD_TYPE,
        STATUS_MAP
    } from './constants'

    export default {
        components: {
            StatusIcon,
            ContainerType,
            AtomList,
            Logo
        },
        mixins: [localeMixins],
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
            containerGroupIndex: Number,
            containerLength: Number,
            stageDisabled: Boolean,
            disabled: Boolean,
            handleChange: {
                type: Function,
                required: true
            },
            stageLength: Number,
            updateCruveConnectHeight: Function
        },
        inject: ['reactiveData'],
        emits: [DELETE_EVENT_NAME, COPY_EVENT_NAME],
        data () {
            return {
                showContainerName: false,
                showAtomName: false,
                showAtomList: false,
                cruveHeight: 0
            }
        },
        computed: {
            containerStatus () {
                return this.container && this.container.status ? this.container.status : ''
            },
            containerTypeCls () {
                return { 'hover-hide': this.showCopyJob }
            },
            jobTitleCls () {
                return {
                    'container-title': true,
                    'first-ctitle': this.containerIndex === 0,
                    DISABLED: this.disabled,
                    [this.containerStatus]: true
                }
            },
            displayNameCls () {
                return {
                    'skip-name': this.disabled || this.containerStatus === STATUS_MAP.SKIP
                }
            },
            matrixFoldLogoCls () {
                return {
                    'fold-atom-icon': true,
                    open: this.showAtomList,
                    [this.containerStatus || 'readonly']: true
                }
            },
            displayName () {
                try {
                    const { matrixContext, status, name } = this.container
                    const suffix = isObject(matrixContext)
                        ? Object.values(matrixContext).join(', ')
                        : ''
                    const isPrepare
                        = status === STATUS_MAP.PREPARE_ENV && this.containerGroupIndex === undefined
                    return isPrepare ? this.t('prepareEnv') : `${name}${suffix ? `(${suffix})` : ''}`
                } catch (error) {
                    return 'unknow'
                }
            },
            showCopyJob () {
                return !isTriggerContainer(this.container) && this.reactiveData.editable
            },
            containerSerialNum () {
                if (this.reactiveData.isExecDetail) {
                    let jobSerialNum = this.container.id - this.stage.containers[0].id + 1
                    if (this.container.matrixGroupFlag) {
                        jobSerialNum = parseInt(this.container.id, 10) % 1000
                    }
                    return `${this.stage.id.replace('stage-', '')}-${jobSerialNum}`
                }
                return `${this.stageIndex + 1}-${this.containerIndex + 1}`
            },
            isOnlyOneContainer () {
                return this.containerLength === 1
            },
            projectId () {
                return this.$route.params.projectId
            },

            dependOnValue () {
                if (isTriggerContainer(this.container)) return ''
                const val = getDependOnDesc(this.container)
                return `${this.t('dependOn')} 【${val}】`
            },
            showMatrixFold () {
                return this.reactiveData.isExecDetail && this.containerGroupIndex !== undefined
            },
            buildResourceType () {
                try {
                    return this.container.dispatchType.buildType
                } catch (e) {
                    return DOCKER_BUILD_TYPE
                }
            },
            showDebugBtn () {
                const {
                    reactiveData,
                    container: { baseOS, status }
                } = this
                const isshowDebugType = [
                    DOCKER_BUILD_TYPE,
                    PUBLIC_DEVCLOUD_BUILD_TYPE,
                    PUBLIC_BCS_BUILD_TYPE
                ].includes(this.buildResourceType)
                return (
                    baseOS === 'LINUX'
                    && isshowDebugType
                    && reactiveData.isExecDetail
                    && reactiveData.isLatestBuild
                    && status === STATUS_MAP.FAILED
                )
            },
            isUnExecThisTime () {
                return this.container?.executeCount < this.reactiveData.currentExecCount
            }
        },
        watch: {
            'container.runContainer' (newVal) {
                const { elements } = this.container
                if (this.disabled && newVal) return
                elements
                    .filter(
                        (item) => item.additionalOptions === undefined || item.additionalOptions.enable
                    )
                    .forEach((item) => {
                        item.canElementSkip = newVal
                    })
                this.handleChange(this.container, { elements })
            },
            'container.locateActive' (val) {
                if (val) {
                    const ele = document.getElementById(this.container.id)
                    ele?.scrollIntoView?.({
                        block: 'center',
                        inline: 'center',
                        behavior: 'smooth'
                    })
                }
            }
        },
        methods: {
            toggleShowAtom (show) {
                this.showAtomList = show ?? !this.showAtomList
                this.updateCruveConnectHeight()
            },
            deleteJob () {
                const { containerIndex, stageIndex, isOnlyOneContainer } = this

                this.$emit(DELETE_EVENT_NAME, {
                    stageIndex,
                    containerIndex: isOnlyOneContainer ? undefined : containerIndex
                })
            },
            showContainerPanel () {
                eventBus.$emit(CLICK_EVENT_NAME, {
                    stageIndex: this.stageIndex,
                    containerIndex: this.containerIndex,
                    containerGroupIndex: this.containerGroupIndex,
                    container: this.container
                })
            },

            handleCopyContainer () {
                try {
                    const copyContainer = JSON.parse(JSON.stringify(this.container))
                    const { containerHashId, containerId, ...resetContainerProps } = copyContainer
                    const container = {
                        ...resetContainerProps,
                        containerId: `c-${hashID()}`,
                        jobId: `job_${randomString(3)}`,
                        elements: copyContainer.elements.map((element) => ({
                            ...element,
                            id: `e-${hashID()}`
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
                        message: this.t('copyJobFail')
                    })
                }
            },
            debugDocker () {
                eventBus.$emit(DEBUG_CONTAINER, {
                    stageIndex: this.stageIndex,
                    containerIndex: this.containerIndex,
                    containerGroupIndex: this.containerGroupIndex,
                    container: this.container
                })
            }
        }
    }
</script>

<style lang="scss">
@use "sass:math";
@import "./conf";
.devops-stage-container {
  .container-title {
    display: flex;
    height: $itemHeight;
    background: #33333f;
    cursor: pointer;
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
        border-color: #979ba4;
      }
    }
    input[type="checkbox"] {
      border-radius: 3px;
    }
    .matrix-flag-icon {
      position: absolute;
      top: 0px;
      font-size: 16px;
    }
    .fold-atom-icon {
      position: absolute;
      background: white;
      border-radius: 50%;
      bottom: -10px;
      left: 44%;
      transition: all 0.3s ease;
      &.open {
        transform: rotate(-180deg);
      }
      &.readonly {
        color: $fontWeightColor;
      }
    }
    .copyJob {
      display: none;
      margin-right: 10px;
      color: $fontLighterColor;
      cursor: pointer;
      &:hover {
        color: $primaryColor;
      }
    }
    .close {
      @include add-plus-icon(#2e2e3a, #2e2e3a, #c4c6cd, 16px, true);
      @include add-plus-icon-hover($dangerColor, $dangerColor, white);
      border: none;
      display: none;
      margin-right: 10px;
      transform: rotate(45deg);
      cursor: pointer;
      &:before,
      &:after {
        left: 7px;
        top: 4px;
      }
    }
    .debug-btn {
      position: absolute;
      height: 100%;
      right: 0;
    }

    .container-locate-icon {
        position: absolute;
        left: -30px;
        top: 13px;
        color: $primaryColor;
    }

    &:hover {
      .copyJob,
      .close {
        display: block;
      }
      .hover-hide {
        display: none;
      }
    }
  }
}
</style>

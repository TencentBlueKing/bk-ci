<template>
    <div :class="[{ 'pipeline-drag': editable && !isTriggerStage, 'readonly': !editable || stageDisabled }, 'pipeline-stage']" ref="stageRef">
        <span :class="{ 'stage-review-logo': true, 'pointer': true }" v-bk-tooltips.top="reviewTooltip(stage.checkIn)" @click.stop="startNextStage('checkIn')">
            <logo v-if="!isTriggerStage && !isFinallyStage" :name="reviewStatausIcon(stage.checkIn)" size="28" />
        </span>
        <bk-button :class="['pipeline-stage-entry', [stageStatusCls], { 'editable-stage-entry': editable, 'stage-disabled': stageDisabled }]" @click.stop="showStagePanel">
            <span :title="stageTitle" :class="{ 'stage-entry-name': true, 'skip-name': stageDisabled || stage.status === 'SKIP', 'show-right-icon': (showCheckedToatal || canStageRetry) }">
                <logo v-if="stage.status === 'SKIP'" v-bk-tooltips="$t('skipStageDesc')" class="skip-icon redo-arrow" name="redo-arrow" size="16"></logo>
                <i v-else-if="stageStatusIcon" :class="`stage-status-icon bk-icon icon-${stageStatusIcon}`"></i>
                {{ stageTitle }}
            </span>
            <i v-if="isStageError" class="bk-icon icon-exclamation-triangle-shape stage-entry-error-icon" />
            <span @click.stop v-if="showCheckedToatal" class="check-total-stage">
                <bk-checkbox class="atom-canskip-checkbox" v-model="stage.runStage" :disabled="stageDisabled"></bk-checkbox>
            </span>
            <span v-if="canStageRetry" @click.stop="() => showRetryStageDialog = true" class="stage-single-retry">
                {{ $t('retry') }}
            </span>
            <span class="stage-entry-btns" v-if="!stage.isError && showCopyStage">
                <span :title="$t('editPage.copyStage')" class="bk-icon copy-stage" @click.stop="copyStage">
                    <Logo name="copy" size="16"></Logo>
                </span>
                <i @click.stop="deleteStageHandler" class="add-plus-icon close" />
            </span>
        </bk-button>
        <draggable v-model="computedContainer" v-bind="dragOptions" :move="checkMove" tag="ul">
            <stage-container v-for="(container, index) in computedContainer"
                :key="container.containerId"
                :stage-index="stageIndex"
                :pre-container="containers[index - 1]"
                :container-index="index"
                :stage-length="stageLength"
                :editable="editable"
                :is-preview="isPreview"
                :can-skip-element="canSkipElement"
                :stage-disabled="stageDisabled"
                :container-length="computedContainer.length"
                :container="container">
            </stage-container>
        </draggable>
        <span class="stage-connector">
            <i class="devops-icon icon-right-shape connector-angle"></i>
        </span>
        <template v-if="editable">
            <span v-if="!isFirstStage" class="add-menu" @click.stop="toggleAddMenu(!isAddMenuShow)">
                <i :class="{ [iconCls]: true, 'active': isAddMenuShow }" />
                <template v-if="isAddMenuShow">
                    <cruve-line class="add-connector connect-line left" :width="60" :height="cruveHeight"></cruve-line>
                    <span class="insert-stage direction">
                        <div class="click-item" @click.stop="appendStage(false)">
                            <span>
                                {{ $t('editPage.insertStage') }}
                            </span>
                        </div>
                        <div :class="{ 'disabled-item': hasFinallyStage || stageLength === 1, 'click-item': true }" @click.stop="appendStage(true)">
                            <span>
                                {{ $t('editPage.insertFinallyStage') }}
                            </span>
                        </div>
                    </span>
                    <div @click.stop="showStageSelectPopup(true)" class="insert-tip parallel-add" :style="`top: ${cruveHeight}px`">
                        <i class="tip-icon" />
                        <span>
                            {{ $t('editPage.append') }}
                        </span>
                    </div>
                </template>
            </span>
            <span v-if="isLastStage && !isFinallyStage && editable" @click.stop="toggleLastMenu(!lastAddMenuShow)" class="append-stage pointer">
                <i class="add-plus-icon" />
                <template v-if="lastAddMenuShow">
                    <span class="insert-stage direction">
                        <div class="click-item" @click.stop="appendStage(false, true)">
                            <span>
                                {{ $t('editPage.insertStage') }}
                            </span>
                        </div>
                        <div :class="{ 'click-item': true, 'disabled-item': hasFinallyStage || stageLength === 1 }" @click.stop="appendStage(true)">
                            <span>
                                {{ $t('editPage.insertFinallyStage') }}
                            </span>
                        </div>
                    </span>
                </template>
            </span>
        </template>
        <bk-dialog
            v-model="showRetryStageDialog"
            render-directive="if"
            ext-cls="stage-retry-dialog"
            :width="400"
            :auto-close="false"
            @confirm="confirmRetry"
        >
            <bk-radio-group v-model="failedContainer">
                <bk-radio :value="false">{{ $t('editPage.retryAllJobs') }}</bk-radio>
                <bk-radio :value="true">{{ $t('editPage.retryFailJobs') }}</bk-radio>
            </bk-radio-group>
        </bk-dialog>
    </div>
</template>

<script>

    import Vue from 'vue'
    import { mapActions, mapState, mapGetters } from 'vuex'
    import StageContainer from './StageContainer'
    import { getOuterHeight, hashID, randomString } from '@/utils/util'
    import Logo from '@/components/Logo'
    import CruveLine from '@/components/Stages/CruveLine'

    export default {
        components: {
            StageContainer,
            CruveLine,
            Logo
        },
        props: {
            containers: {
                type: Array,
                default: []
            },
            stage: Object,
            stageIndex: Number,
            stageLength: Number,
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
            }
        },
        data () {
            return {
                isAddMenuShow: false,
                lastAddMenuShow: false,
                cruveHeight: 0,
                failedContainer: false,
                showRetryStageDialog: false
            }
        },
        computed: {
            ...mapState('atom', [
                'insertStageIndex',
                'pipeline',
                'pipelineLimit',
                'showStageReviewPanel'
            ]),
            ...mapGetters('atom', [
                'isTriggerContainer',
                'hasFinallyStage'
            ]),
            isStageError () {
                try {
                    return this.stage.isError
                } catch (e) {
                    console.warn(e)
                    return false
                }
            },
            canStageRetry () {
                return this.stage.canRetry === true
            },
            showCopyStage () {
                return !this.isTriggerStage && !this.isFinallyStage && this.editable
            },
            isFirstStage () {
                return this.stageIndex === 0
            },
            isLastStage () {
                return this.stageIndex === this.stageLength - 1
            },
            isTriggerStage () {
                return this.checkIsTriggerStage(this.stage)
            },
            isFinallyStage () {
                return this.stage.finally === true
            },
            stageTitle () {
                return this.stage ? this.stage.name : 'stage'
            },
            showCheckedToatal () {
                const { isTriggerStage, $route } = this
                return $route.path.indexOf('preview') > 0 && !isTriggerStage && this.canSkipElement
            },
            stageDisabled () {
                return !!(this.stage.stageControlOption && this.stage.stageControlOption.enable === false)
            },
            stageReviewType () {
                return this.showStageReviewPanel.type
            },
            computedContainer: {
                get () {
                    return this.containers
                },
                set (containers) {
                    let data = []
                    containers.forEach(container => {
                        if (container.containers) data = [...data, ...container.containers] // 拖动的是stage
                        else data.push(container)
                    })
                    this.setPipelineContainer({ oldContainers: this.containers, containers: data })
                    this.setPipelineEditing(true)
                }
            },
            dragOptions () {
                return {
                    group: this.stage.finally ? 'finally-stage-job' : 'pipeline-job',
                    ghostClass: 'sortable-ghost-atom',
                    chosenClass: 'sortable-chosen-atom',
                    animation: 130,
                    disabled: !this.editable
                }
            },
            iconCls () {
                switch (true) {
                    case !this.isAddMenuShow:
                        return 'add-plus-icon'
                    case this.isAddMenuShow:
                        return 'minus-icon'
                    default:
                        return 'add-plus-icon'
                }
            },
            stageStatusCls () {
                return this.stage && this.stage.status ? this.stage.status : ''
            },
            stageStatusIcon () {
                switch (this.stageStatusCls) {
                    case 'SUCCEED':
                        return 'check-circle'
                    case 'FAILED':
                        return 'close-circle'
                    case 'SKIP':
                        return 'redo-arrow'
                    case 'RUNNING':
                        return 'circle-2-1 spin-icon'
                    default:
                        return ''
                }
            }
        },
        watch: {
            'stage.runStage' (newVal) {
                const { stage, updateStage } = this
                const { containers } = stage
                if (this.stageDisabled || !this.showCheckedToatal) return
                containers.filter(container => (container.jobControlOption === undefined || container.jobControlOption.enable)).map(container => {
                    container.runContainer = newVal
                    return false
                })
                updateStage({
                    stage,
                    newParam: {
                        containers
                    }
                })
            }
        },
        mounted () {
            this.updateHeight()
            if (this.showCheckedToatal) {
                Vue.set(this.stage, 'runStage', !this.stageDisabled)
            }
            document.addEventListener('click', this.hideAddStage)
        },
        beforeDestroyed () {
            window.removeEventListener('click', this.hideAddStage)
        },
        updated () {
            this.updateHeight()
        },
        methods: {
            ...mapActions('pipelines', ['requestRetryPipeline']),
            ...mapActions('atom', [
                'setInertStageIndex',
                'setInsertStageIsFinally',
                'togglePropertyPanel',
                'toggleStageSelectPopup',
                'setPipelineContainer',
                'setPipelineEditing',
                'updateStage',
                'deleteStage',
                'toggleStageReviewPanel'
            ]),

            reviewTooltip (stageControl = {}) {
                const reviewGroups = stageControl.reviewGroups || []
                const curReviewGroup = reviewGroups.find((review) => (review.status === undefined)) || {}
                const canTriggerStage = (curReviewGroup.reviewers || []).includes(this.$userInfo ? this.$userInfo.username : '')
                const isStagePause = stageControl.status !== 'REVIEWING'
                return {
                    content: canTriggerStage ? this.$t('editPage.toCheck') : this.$t('editPage.noAuthToCheck'),
                    disabled: isStagePause
                }
            },

            reviewStatausIcon (stageControl = {}) {
                try {
                    if (stageControl.isReviewError) return 'review-error'
                    switch (true) {
                        case stageControl.status === 'REVIEWING':
                            return 'reviewing'
                        case stageControl.status === 'QUEUE':
                            return 'review-waiting'
                        case stageControl.status === 'REVIEW_PROCESSED':
                            return 'reviewed'
                        case stageControl.status === 'REVIEW_ABORT':
                            return 'review-abort'
                        case stageControl.status === 'QUALITY_CHECK_FAIL':
                            return 'quality_check_fail'
                        case this.stageStatusCls === 'SKIP':
                        case !this.stageStatusCls && this.isExecDetail:
                            return stageControl.manualTrigger ? 'review-waiting' : 'review-auto-gray'
                        case !!this.stageStatusCls:
                            return 'review-auto-pass'
                        default:
                            return stageControl.manualTrigger ? 'review-enable' : 'review-auto'
                    }
                } catch (e) {
                    console.warn('get review icon error: ', e)
                    return 'review-auto'
                }
            },

            confirmRetry () {
                this.showRetryStageDialog = false
                this.singleRetry(this.stage.id)
            },
            async singleRetry (stageId) {
                let message, theme
                try {
                    // 请求执行构建
                    const res = await this.requestRetryPipeline({
                        projectId: this.$route.params.projectId,
                        pipelineId: this.$route.params.pipelineId,
                        buildId: this.$route.params.buildNo,
                        taskId: stageId,
                        failedContainer: this.failedContainer
                    })
                    if (res.id) {
                        message = this.$t('subpage.retrySuc')
                        theme = 'success'
                    } else {
                        message = this.$t('subpage.retryFail')
                        theme = 'error'
                    }
                } catch (err) {
                    this.handleError(err, [{
                        actionId: this.$permissionActionMap.execute,
                        resourceId: this.$permissionResourceMap.pipeline,
                        instanceId: [{
                            id: this.$route.params.pipelineId,
                            name: this.$route.params.pipelineId
                        }],
                        projectId: this.$route.params.projectId
                    }])
                } finally {
                    message && this.$showTips({
                        message,
                        theme
                    })
                }
            },
            checkIsTriggerStage (stage) {
                try {
                    return this.isTriggerContainer(stage.containers[0])
                } catch (e) {
                    return false
                }
            },

            showStagePanel () {
                const { stageIndex } = this
                this.togglePropertyPanel({
                    isShow: true,
                    editingElementPos: {
                        stageIndex
                    }
                })
            },

            checkMove (event) {
                const dragContext = event.draggedContext || {}
                const element = dragContext.element || {}
                const isTrigger = element['@type'] === 'trigger'

                const relatedContext = event.relatedContext || {}
                const relatedelement = relatedContext.element || {}
                const isRelatedTrigger = relatedelement['@type'] === 'trigger'
                const isTriggerStage = this.checkIsTriggerStage(relatedelement)
                const isFinallyStage = relatedelement.finally === true

                return !isTrigger && !isRelatedTrigger && !isTriggerStage && !isFinallyStage
            },

            appendStage (isFinally = false, fromLast = false) {
                const { stageIndex, stageLength, setInertStageIndex, setInsertStageIsFinally, hasFinallyStage, showStageSelectPopup } = this
                if (isFinally && (hasFinallyStage || stageLength === 1)) return
                setInertStageIndex({
                    insertStageIndex: isFinally ? this.stageLength : (fromLast ? stageIndex + 1 : stageIndex)
                })
                setInsertStageIsFinally({
                    insertStageIsFinally: isFinally
                })
                showStageSelectPopup(false)
                this.toggleAddMenu(false)
                this.toggleLastMenu(false)
            },
            showStageSelectPopup (isParallel) {
                let limitMsg = ''
                if (!isParallel && this.stageLength >= this.pipelineLimit.stageLimit) {
                    limitMsg = this.$t('storeMap.stageLimit') + this.pipelineLimit.stageLimit
                } else if (isParallel && this.stage.containers.length >= this.pipelineLimit.jobLimit) {
                    limitMsg = this.$t('storeMap.jobLimit') + this.pipelineLimit.jobLimit
                }
                if (limitMsg) {
                    this.$showTips({
                        theme: 'error',
                        message: limitMsg
                    })
                    return
                }

                this.toggleStageSelectPopup({
                    isStagePopupShow: true,
                    isAddParallelContainer: isParallel
                })
            },

            toggleAddMenu (isAddMenuShow) {
                if (!this.editable) return
                const { stageIndex, setInertStageIndex } = this
                this.isAddMenuShow = typeof isAddMenuShow === 'boolean' ? isAddMenuShow : false
                if (this.isAddMenuShow) {
                    setInertStageIndex({
                        insertStageIndex: stageIndex
                    })
                }
            },

            toggleLastMenu (isLastMenuShow) {
                if (!this.editable) return
                const { stageIndex, setInertStageIndex } = this
                this.lastAddMenuShow = typeof isLastMenuShow === 'boolean' ? isLastMenuShow : false
                if (this.lastAddMenuShow) {
                    setInertStageIndex({
                        insertStageIndex: stageIndex
                    })
                }
            },

            hideAddStage () {
                this.lastAddMenuShow = false
                this.isAddMenuShow = false
            },

            startNextStage (type) {
                this.toggleStageReviewPanel({
                    showStageReviewPanel: {
                        isShow: true,
                        type
                    },
                    editingElementPos: {
                        stageIndex: this.stageIndex
                    }
                })
            },
            updateHeight () {
                const parentEle = this.$refs.stageRef
                const height = getOuterHeight(parentEle)
                this.cruveHeight = height
            },
            deleteStageHandler () {
                const { stageIndex } = this

                this.deleteStage({
                    stageIndex
                })
            },
            copyStage () {
                if (this.stageLength >= this.pipelineLimit.stageLimit) {
                    this.$showTips({
                        theme: 'error',
                        message: this.$t('storeMap.stageLimit') + this.pipelineLimit.stageLimit
                    })
                    return
                }
                try {
                    const copyStage = JSON.parse(JSON.stringify(this.stage))
                    const stage = {
                        ...copyStage,
                        id: `s-${hashID(32)}`,
                        containers: copyStage.containers.map(container => ({
                            ...container,
                            jobId: `job_${randomString(3)}`,
                            containerId: `c-${hashID(32)}`,
                            elements: container.elements.map(element => ({
                                ...element,
                                id: `e-${hashID(32)}`
                            })),
                            jobControlOption: container.jobControlOption
                                ? {
                                    ...container.jobControlOption,
                                    dependOnType: 'ID',
                                    dependOnId: []
                                }
                                : undefined
                        }))

                    }

                    this.pipeline.stages.splice(this.stageIndex + 1, 0, JSON.parse(JSON.stringify(stage)))
                    this.setPipelineEditing(true)
                } catch (e) {
                    console.error(e)
                    this.$showTips({
                        theme: 'error',
                        message: this.$t('editPage.copyStageFail')
                    })
                }
            }
        }
    }
</script>

<style lang='scss'>
    @use "sass:math";
    @import 'Stage';
    $addIconTop: math.div($stageEntryHeight, 2) - math.div($addBtnSize, 2);

    .pipeline-drag {
        cursor: url('../../images/grab.cur'), default;
    }

    .pipeline-stage {
        position: relative;
        width: 280px;
        border-radius: 2px;
        padding: 0 0 24px 0;
        background: $stageBGColor;
        margin: 0 $StageMargin 0 0;

        .stage-review-logo {
            position: absolute;
            left: math.div(-$reviewIconSize, 2);
            top: math.div(($stageEntryHeight - $reviewIconSize), 2);
            z-index: 3;
        }

        .pipeline-stage-entry {
            display: block;
            width: 100%;
            height: 50px;
            line-height: 50px;
            background-color: #EFF5FF;
            border-color: #D4E8FF;
            color: $primaryColor;
            z-index: 2;

            .stage-entry-name {
                @include ellipsis();
                width: 90%;
                &.show-right-icon {
                    width: 70%;
                }
            }

            &:not(.editable-stage-entry),
            &.stage-disabled {
                background-color: #F3F3F3;
                border-color: #D0D8EA;
                color: black;

                .skip-icon {
                    vertical-align: middle;
                }

                &.SKIP {
                    color: $borderLightColor;
                    fill: $borderLightColor;
                }

                &.RUNNING {
                    background-color: #EFF5FF;
                    border-color: #D4E8FF;
                    color: $primaryColor;
                }
                &.REVIEWING {
                    background-color: #F3F3F3;
                    border-color: #D0D8EA;
                    color: black;
                }

                &.FAILED {
                    border-color: #FFD4D4;
                    background-color: #FFF9F9;
                    color: black;
                    .stage-status-icon {
                        color: #FF5656;
                    }
                }
                &.SUCCEED {
                    background-color: #F3FFF6;
                    border-color: #BBEFC9;
                    color: black;
                    .stage-status-icon {
                        color: #34DA7B;
                    }

                }
            }

            &.editable-stage-entry:hover {
                color: black;
                border-color: #1A6DF3;
                background-color: #D1E2FD;
                .stage-entry-btns {
                    display: flex;
                }
                .stage-entry-error-icon {
                    display: none;
                }
                .stage-entry-name {
                    width: 70%;
                }
            }

            .stage-single-retry {
                cursor: pointer;
                position: absolute;
                right: 6%;
                color: $primaryColor;
            }

            .stage-entry-error-icon,
            .check-total-stage {
                position: absolute;
                right: 27px;
                &.stage-entry-error-icon {
                    top: 16px;
                    right: 8px;
                    color: $dangerColor;
                }
            }

            .stage-entry-btns {
                position: absolute;
                right: 0;
                top: 16px;
                display: none;
                .copy-stage {
                    margin-right: 8px;
                    fill: white;
                }
                .close {
                    @include add-plus-icon(#2E2E3A, #2E2E3A, white, 16px, true);
                    @include add-plus-icon-hover($dangerColor, $dangerColor, white);
                    border: none;
                    margin-right: 10px;
                    transform: rotate(45deg);
                    cursor: pointer;
                    &:before, &:after {
                        left: 7px;
                        top: 4px;
                    }
                }
            }
        }

        .add-connector {
            stroke-dasharray: 4,4;
            top: 7px;
            left: math.div($addBtnSize, 2);
        }

        .append-stage {
            position: absolute;
            top: $addIconTop;
            right: $appendIconRight;
            z-index: 3;
            .add-plus-icon {
                box-shadow: 0px 2px 4px 0px rgba(60, 150, 255, 0.2);
            }
            .line-add {
                top: -46px;
                left: -16px;
            }
        }

        .add-menu {
            position: absolute;
            top: $addIconTop;
            left: $addIconLeft;
            cursor: pointer;
            z-index: 3;
            .add-plus-icon {
                box-shadow: 0px 2px 4px 0px rgba(60, 150, 255, 0.2);
            }
            .minus-icon {
                z-index: 4;
            }
            .line-add {
                top: -46px;
                left: -16px;
            }
            .parallel-add {
                left: 50px;
            }
        }

        &:last-child {
            .stage-connector {
                width: math.div($StageMargin, 2);
                right: math.div(-$StageMargin, 2);
                .connector-angle {
                    display: none;
                }
            }
        }
        .stage-connector {
            position: absolute;
            width: $StageMargin - math.div($reviewIconSize, 2);
            height: $stageConnectorSize;
            right: - $StageMargin + math.div($reviewIconSize, 2);
            top: math.div($stageEntryHeight, 2) - 1;
            color: $primaryColor;
            background-color: $primaryColor;

            &:before {
                content: '';
                width: $dotR;
                height: $dotR;
                position: absolute;
                left: math.div(-$dotR, 2);
                top: - (math.div($dotR, 2) - 1);
                background-color: $primaryColor;
                border-radius: 50%;
            }
            .connector-angle {
                position: absolute;
                right: -$angleSize;
                top: -$angleSize;
            }
        }

        .insert-stage {
            position: absolute;
            display: block;
            width: 160px;
            background-color: #ffffff;
            border: 1px solid #dcdee5;
            .click-item {
                padding: 0 15px;
                font-size: 12px;
                line-height: 32px;

                &:hover, :hover {
                    color: #3c96ff;
                    background-color: #eaf3ff;
                }
            }
            .disabled-item {
                cursor: not-allowed;
                color: #c4cdd6;
                &:hover, :hover {
                    color: #c4cdd6;
                    background-color: #ffffff;
                }
            }
        }
    }
    .stage-retry-dialog {
        .bk-form-radio {
            display: block;
            margin-top: 15px;
            .bk-radio-text {
                font-size: 14px;
            }
        }
    }
</style>

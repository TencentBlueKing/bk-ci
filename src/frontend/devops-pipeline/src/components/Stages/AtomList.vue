<template>
    <section>
        <draggable class="container-atom-list" :class="{ 'trigger-container': isTriggerContainer(container), 'readonly': !editable }" :data-baseos="container.baseOS || container.classType" v-model="atomList" v-bind="dragOptions" :move="checkMove">
            <li v-for="(atom, index) in atomList" :key="atom.id" :class="{ 'atom-item': true,
                                                                           [atom.status]: atom.status,
                                                                           'quality-item': (atom['@type'] === 'qualityGateOutTask') || (atom['@type'] === 'qualityGateInTask'),
                                                                           'last-quality-item': (atom['@type'] === 'qualityGateOutTask' && index === atomList.length - 1),
                                                                           'arrival-atom': atom.status,
                                                                           'qualitt-next-atom': handlePreviousAtomCheck(atomList, index)
            }"
                @click.stop="showPropertyPanel(index)"
            >
                <section class="atom-item atom-section normal-atom" :class="{ [atom.status]: atom.status,
                                                                              'is-error': atom.isError,
                                                                              'quality-atom': atom['@type'] === 'qualityGateOutTask',
                                                                              'is-intercept': atom.isQualityCheck,
                                                                              'template-compare-atom': atom.templateModify }"
                    v-if="atom['@type'] !== 'qualityGateInTask' && atom['@type'] !== 'qualityGateOutTask'">
                    <status-icon v-if="atom.status && atom.status !== 'SKIP'" type="element" :status="atom.status" />
                    <status-icon v-else-if="isWaiting && atom.status !== 'SKIP'" type="element" status="WAITING" />
                    <img v-else-if="atomMap[atom.atomCode] && atomMap[atom.atomCode].icon" :src="atomMap[atom.atomCode].icon" :class="{ 'atom-icon': true, 'skip-icon': useSkipStyle(atom) }" />
                    <logo v-else :class="{ 'atom-icon': true, 'skip-icon': useSkipStyle(atom) }" :name="getAtomIcon(atom.atomCode)" size="18" />
                    <p class="atom-name">
                        <span :title="atom.name" :class="{ 'skip-name': useSkipStyle(atom) }">{{ atom.atomCode ? atom.name : $t('editPage.pendingAtom') }}</span>
                    </p>
                    <bk-popover placement="top" v-if="atom.isReviewing">
                        <span @click.stop="checkAtom(atom)" :class="isCurrentUser(atom.computedReviewers) ? 'atom-reviewing-tips' : 'atom-review-diasbled-tips'">{{ $t('editPage.toCheck') }}</span>
                        <template slot="content">
                            <p>{{ $t('editPage.checkUser') }}{{ atom.computedReviewers.join(';') }}</p>
                        </template>
                    </bk-popover>
                    <bk-popover placement="top" v-if="atom.status === 'REVIEW_ABORT'">
                        <span class="atom-review-diasbled-tips">{{ $t('editPage.aborted') }}</span>
                        <template slot="content">
                            <p>{{ $t('editPage.abortTips') }}{{ $t('editPage.checkUser') }}{{ execDetail.cancelUserId }}</p>
                        </template>
                    </bk-popover>
                    <a href="javascript: void(0);" class="atom-single-retry" v-if="atom.status !== 'SKIP' && atom.canRetry" @click.stop="singleRetry(atom.id)">{{ $t('retry') }}</a>
                    <bk-popover placement="top" v-else-if="atom.status !== 'SKIP'" :disabled="!atom.elapsed">
                        <span :class="atom.status === 'SUCCEED' ? 'atom-success-timer' : (atom.status === 'REVIEW_ABORT' ? 'atom-warning-timer' : 'atom-fail-timer')">
                            <span v-if="atom.elapsed && atom.elapsed >= 36e5">&gt;</span>{{ atom.elapsed ? atom.elapsed > 36e5 ? '1h' : localTime(atom.elapsed) : '' }}
                        </span>
                        <template slot="content">
                            <p>{{ atom.elapsed ? localTime(atom.elapsed) : '' }}</p>
                        </template>
                    </bk-popover>
                    <span class="devops-icon copy" v-if="editable && stageIndex !== 0 && !atom.isError" :title="$t('editPage.copyAtom')" @click.stop="copyAtom(index)">
                        <Logo name="copy" size="18"></Logo>
                    </span>
                    <i v-if="editable" @click.stop="editAtom(index, false)" class="add-plus-icon close" />
                    <i v-if="editable && atom.isError" class="devops-icon icon-exclamation-triangle-shape" />
                    <span @click.stop="" v-if="isPreview && canSkipElement && container['@type'].indexOf('trigger') < 0">
                        <bk-checkbox class="atom-canskip-checkbox" v-model="atom.canElementSkip" :disabled="useSkipStyle(atom)" />
                    </span>
                </section>

                <section class="atom-section quality-atom"
                    :class="{ 'is-review': atom.isReviewing,
                              'is-success': (atom.status === 'SUCCEED' || atom.status === 'REVIEW_PROCESSED'),
                              'is-fail': (atom.status === 'QUALITY_CHECK_FAIL' || atom.status === 'REVIEW_ABORT') }"
                    v-if="atom['@type'] === 'qualityGateInTask' || atom['@type'] === 'qualityGateOutTask'">
                    <span class="atom-title">{{ $t('details.quality.quality') }}</span>
                    <span class="handler-list" :class="{ 'disabled-review': atom.isReviewing && !isCurrentUser(atom.computedReviewers) }"
                        v-if="atom.isReviewing && !reviewLoading">
                        <span class="revire-btn continue-excude" @click.stop="reviewExcute(atom, 'PROCESS', atom.computedReviewers)">{{ $t('resume') }}</span>
                        <span class="review-btn stop-excude" @click.stop="reviewExcute(atom, 'ABORT', atom.computedReviewers)">{{ $t('terminate') }}</span>
                    </span>
                    <i class="devops-icon icon-circle-2-1 executing-job" v-if="atom.isReviewing && reviewLoading"></i>
                </section>
            </li>
            <span v-if="editable" :class="{ 'add-atom-entry': true, 'block-add-entry': atomList.length === 0 }" @click="editAtom(atomList.length - 1, true)">
                <i class="add-plus-icon" />
                <span v-if="atomList.length === 0">{{ $t('editPage.addAtom') }}</span>
            </span>
        </draggable>
        <check-atom-dialog :is-show-check-dialog="isShowCheckDialog" :atom="currentAtom" :toggle-check="toggleCheckDialog"></check-atom-dialog>
    </section>
</template>

<script>
    import StatusIcon from './StatusIcon'
    import { mapActions, mapGetters, mapState } from 'vuex'
    import { coverTimer, hashID } from '@/utils/util'
    import draggable from 'vuedraggable'
    import Logo from '@/components/Logo'
    import CheckAtomDialog from './CheckAtomDialog'
    export default {
        name: 'atom-list',
        components: {
            StatusIcon,
            draggable,
            Logo,
            CheckAtomDialog
        },
        props: {
            container: Object,
            stageIndex: Number,
            containerIndex: Number,
            containerStatus: String,
            containerDisabled: Boolean,
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
                reviewLoading: false,
                isShowCheckDialog: false,
                currentAtom: {}
            }
        },
        computed: {
            ...mapState('soda', [
                'ruleList',
                'templateRuleList'
            ]),
            ...mapState('atom', [
                'execDetail',
                'atomMap',
                'pipeline'
            ]),
            ...mapGetters('atom', [
                'isTriggerContainer',
                'getElements',
                'getStage'
            ]),
            isWaiting () {
                return this.containerStatus === 'PREPARE_ENV'
            },
            routerParams () {
                return this.$route.params
            },
            isInstanceEditable () {
                return !this.editable && this.pipeline && this.pipeline.instanceFromTemplate
            },
            curMatchRules () {
                return this.$route.path.indexOf('template') > 0 ? this.templateRuleList : this.isInstanceEditable ? this.templateRuleList.concat(this.ruleList) : this.ruleList
            },
            atomList: {
                get () {
                    const atoms = this.getElements(this.container)
                    atoms.forEach(atom => {
                        if (this.curMatchRules.some(rule => rule.taskId === atom.atomCode
                            && (rule.ruleList.every(val => !val.gatewayId)
                            || rule.ruleList.some(val => atom.name.indexOf(val.gatewayId) > -1)))) {
                            atom.isQualityCheck = true
                        } else {
                            atom.isQualityCheck = false
                        }
                        atom.isReviewing = atom.status === 'REVIEWING'
                        if (atom.isReviewing) {
                            const atomReviewer = this.getReviewUser(atom)
                            atom.computedReviewers = atomReviewer
                        }
                    })
                    return atoms
                },
                set (elements) {
                    const { container, updateContainer } = this
                    updateContainer({
                        container,
                        newParam: {
                            elements
                        }
                    })
                }
            },
            dragOptions () {
                return {
                    group: 'pipeline-atom',
                    ghostClass: 'sortable-ghost-atom',
                    chosenClass: 'sortable-chosen-atom',
                    animation: 130,
                    disabled: !this.editable
                }
            }
        },
        methods: {
            ...mapActions('soda', [
                'reviewExcuteAtom',
                'requestAuditUserList'
            ]),
            ...mapActions('atom', [
                'updateContainer',
                'togglePropertyPanel',
                'addAtom',
                'deleteAtom',
                'setPipelineEditing'
            ]),
            toggleCheckDialog (isShow = false) {
                this.isShowCheckDialog = isShow
                if (!isShow) {
                    this.currentAtom = {}
                }
            },
            checkAtom (atom) {
                if (!this.isCurrentUser(atom.computedReviewers)) return
                this.currentAtom = atom
                this.toggleCheckDialog(true)
            },
            checkMove (event) {
                const dragContext = event.draggedContext || {}
                const element = dragContext.element || {}
                const atomCode = element.atomCode || ''
                const atom = this.atomMap[atomCode] || {}
                const os = atom.os || []
                const isTriggerAtom = atom.category === 'TRIGGER'

                const to = event.to || {}
                const dataSet = to.dataset || {}
                const baseOS = dataSet.baseos || ''

                const isJobTypeOk = os.includes(baseOS) || (os.length <= 0 && (!baseOS || baseOS === 'normal'))
                return !!atomCode && ((isTriggerAtom && baseOS === 'trigger') || (!isTriggerAtom && isJobTypeOk) || (!isTriggerAtom && baseOS !== 'trigger' && os.length <= 0 && atom.buildLessRunFlag))
            },

            handlePreviousAtomCheck (atomList, index) {
                if (index && (atomList[index - 1]['@type'] === 'qualityGateInTask' || atomList[index - 1]['@type'] === 'qualityGateOutTask')) {
                    return true
                } else {
                    return false
                }
            },
            getAtomIcon (atomCode) {
                if (!atomCode) {
                    return 'placeholder'
                }
                return document.getElementById(atomCode) ? atomCode : 'order'
            },
            localTime (time) {
                return coverTimer(time)
            },
            isCurrentUser (users = []) {
                return this.$userInfo && users.includes(this.$userInfo.username)
            },
            getReviewUser (atom) {
                const list = atom.reviewUsers || (atom.data && atom.data.input && atom.data.input.reviewers)
                const reviewUsers = list.map(user => user.split(';').map(val => val.trim())).reduce((prev, curr) => {
                    return prev.concat(curr)
                })
                return reviewUsers
            },
            showPropertyPanel (elementIndex) {
                const { stageIndex, containerIndex } = this
                this.togglePropertyPanel({
                    isShow: true,
                    editingElementPos: {
                        stageIndex,
                        containerIndex,
                        elementIndex
                    }
                })
            },
            editAtom (atomIndex, isAdd) {
                const { stageIndex, containerIndex, container, addAtom, deleteAtom } = this
                const editAction = isAdd ? addAtom : deleteAtom
                editAction({
                    container,
                    atomIndex,
                    stageIndex,
                    containerIndex
                })
            },
            copyAtom (atomIndex) {
                try {
                    const { id, ...element } = this.container.elements[atomIndex]
                    this.container.elements.splice(atomIndex + 1, 0, JSON.parse(JSON.stringify({
                        ...element,
                        id: `e-${hashID(32)}`
                    })))
                    this.setPipelineEditing(true)
                } catch (e) {
                    console.error(e)
                    this.$showTips({
                        theme: 'error',
                        message: this.$t('editPage.copyAtomFail')
                    })
                }
            },
            async reviewExcute (atom, action, reviewer) {
                if (this.isCurrentUser(reviewer)) {
                    this.reviewLoading = true
                    try {
                        const data = {
                            projectId: this.routerParams.projectId,
                            pipelineId: this.routerParams.pipelineId,
                            buildId: this.routerParams.buildNo,
                            elementId: atom.id,
                            action
                        }
                        const res = await this.reviewExcuteAtom(data)
                        if (res === true) {
                            this.$showTips({
                                message: this.$t('editPage.operateSuc'),
                                theme: 'success'
                            })
                        }
                    } catch (err) {
                        this.$showTips({
                            message: err.message || err,
                            theme: 'error'
                        })
                    } finally {
                        setTimeout(() => {
                            this.reviewLoading = false
                        }, 1000)
                    }
                }
            },
            singleRetry (taskId) {
                if (typeof taskId === 'string') {
                    this.retryPipeline(taskId)
                }
            },
            /**
             * 重试流水线
             */
            async retryPipeline (taskId) {
                let message, theme
                try {
                    // 请求执行构建
                    const res = await this.$store.dispatch('pipelines/requestRetryPipeline', {
                        projectId: this.routerParams.projectId,
                        pipelineId: this.routerParams.pipelineId,
                        buildId: this.routerParams.buildNo,
                        taskId: taskId
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
                            id: this.routerParams.pipelineId,
                            name: this.routerParams.pipelineId
                        }],
                        projectId: this.routerParams.projectId
                    }])
                } finally {
                    message && this.$showTips({
                        message,
                        theme
                    })
                }
            },
            useSkipStyle (atom) {
                return (atom && (atom.status === 'SKIP' || (atom.additionalOptions && atom.additionalOptions.enable === false))) || this.containerDisabled
            }
        }
    }
</script>

<style lang="scss">
    @import "./Stage";
    .container-atom-list {
        position: relative;
        z-index: 3;

        &.trigger-container .atom-item {
            &:before,
            &:after {
                display: none;
            }
        }
        .atom-item {
            position: relative;
            display: flex;
            flex-direction: row;
            align-items: center;
            width: 240px;
            height: $itemHeight;
            margin: 0 0 11px 0;
            background-color: white;
            border-radius: 2px;
            font-size: 14px;
            transition: all .4s ease-in-out;
            z-index: 2;
            .atom-icon {
                text-align: center;
                margin: 0 14.5px;
                font-size: 18px;
                width: 18px;
                fill: currentColor;
            }
            .atom-icon.skip-icon {
                color: #c4cdd6;
            }

            &.is-error {
                border-color: $dangerColor;
                color: $dangerColor;
                &:hover {
                   .icon-exclamation-triangle-shape {
                        display: none;
                    }
                }
                .icon-exclamation-triangle-shape {
                    margin: 0 12px;
                }
            }

            &:hover{
                border-color: $primaryColor;
                .atom-icon.skip-icon {
                    color: #c4cdd6;
                }
                .atom-icon {
                    color: $primaryColor;
                }
                .add-plus-icon.close, .copy {
                    cursor: pointer;
                    color: #c3cdd7;
                    display: block;
                }
            }
            &:first-child {
                &:before {
                    top: -16px;
                }
            }
            &:before {
                content: '';
                position: absolute;
                height: 14px;
                width: 2px;
                background: $fontLigtherColor;
                top: -12px;
                left: 21.5px;
                z-index: 1;
            }

            &:after {
                content: '';
                position: absolute;
                height: 4px;
                width: 4px;
                border: 2px solid $fontLigtherColor;
                border-radius: 50%;
                background: white;
                top: -5px;
                left: 18.5px;
                z-index: 2;
            }

            .add-plus-icon.close {
                @include add-plus-icon(#fff, #fff, #c4c6cd, 16px, true);
                @include add-plus-icon-hover($dangerColor, $dangerColor, white);
                display: none;
                margin-right: 10px;
                border: none;
                transform: rotate(45deg);
                &:before, &:after {
                    left: 7px;
                    top: 4px;
                }
            }

            .copy {
                display: none;
                margin-right: 10px;
                fill: #c4c6cd;
                &:hover {
                    fill: $primaryColor;
                }
            }

            > .atom-name {
                flex: 1;
                color: $fontWeightColor;
                @include ellipsis();
                max-width: 188px;
                span:hover {
                    color: $primaryColor;
                }
            }

            .atom-review-diasbled-tips {
                cursor: default;
                color: #c3cdd7;
                margin: 0 8px 0 2px;
            }
            .atom-reviewing-tips {
                margin: 0 8px 0 2px;
                color: $primaryColor;
            }
            .atom-success-timer {
                margin: 0 8px 0 2px;
                color: $successColor;
            }
            .atom-fail-timer {
                margin: 0 8px 0 2px;
                color: $failColor;
            }
            .atom-warning-timer {
                margin: 0 8px 0 2px;
                color: $warningColor;
            }
            .atom-single-retry {
                margin: 0 8px 0 2px;
                color: $primaryColor;
            }
            .atom-canskip-checkbox {
                margin-right: 6px;
            }
        }

        .quality-item {
            height: 24px;
            line-height: 20px;
            text-align: center;
            background: transparent;
            font-size: 12px;
            &:before {
                height: 40px;
                z-index: 8;
            }
            &:after {
                display: none;
            }
        }

        .atom-section {
            margin: 0;
            width: 100%;
            height: 100%;
            border: 1px solid $fontLigtherColor;
            &:before,
            &:after {
                display: none;
            }
        }

        .is-intercept {
            border-color: $warningColor;
            &:hover {
                border-color: $warningColor;
            }
        }

        .last-quality-item {
            &:before {
                height: 22px;
            }
        }

        .quality-atom {
            margin-left: 84px;
            width: 70px;
            border-radius: 12px;
            z-index: 9;
            .atom-title {
                font-weight: bold;
                &:before,
                &:after {
                    content: '';
                    position: absolute;
                    left: 0;
                    top: 10px;
                    height: 1px;
                    width: 84px;
                    border-top: 2px dashed $fontLigtherColor;
                }
                &:before {
                    left: 21.5px;
                    width: 62px;
                }
                &:after {
                    left: 154px;
                    width: 85px;
                }
            }
            &.is-success {
                border-color: $successColor;
                .atom-title {
                    color: $successColor;
                    &:before,
                    &:after {
                        border-color: $successColor;
                    }
                }
            }
            &.is-review {
                border-color: $warningColor;
                .atom-title {
                    color: $warningColor;
                    &:before {
                        border-color: $warningColor;
                    }
                    &:after {
                        display: none;
                    }
                }
            }
            &.is-fail {
                border-color: $dangerColor;
                .atom-title {
                    color: $dangerColor;
                    &:before,
                    &:after {
                        border-top: 2px solid $dangerColor;
                    }
                }
            }
            .handler-list {
                position: absolute;
                right: 10px;
                span {
                    color: $primaryColor;
                    font-size: 12px;
                    &:first-child {
                        margin-right: 5px;
                    }
                }
            }
            .executing-job {
                position: absolute;
                top: 6px;
                right: 42px;
                &:before {
                    display: inline-block;
                    animation: rotating infinite .6s ease-in-out;
                }
            }
            .disabled-review span {
                color: #c4cdd6;
                cursor: default;
            }
        }

        .add-atom-entry {
            position: absolute;
            bottom: -10px;
            left: 111px;
            background-color: white;
            cursor: pointer;
            z-index: 3;
            .add-plus-icon {
                @include add-plus-icon($fontLigtherColor, $fontLigtherColor, white, 18px, true);
                @include add-plus-icon-hover($primaryColor, $primaryColor, white);
            }
            &.block-add-entry {
                @extend .atom-item;
                position: static;
                border-style: dashed;
                color: $borderWeightColor;
                border-color: $borderWeightColor;
                border-width: 1px;
                .add-plus-icon {
                    margin: 12px 13px;
                }
                &:before,
                &:after {
                    display: none;
                }
            }

            &:hover {
                border-color: $primaryColor;
                color: $primaryColor;
            }
        }

        &.readonly {
            .atom-item {
                cursor: pointer;
                .atom-name:hover {
                    span {
                        color: #63656E;
                    }
                    .skip-name {
                        text-decoration: line-through;
                        color: #c4cdd6;
                    }
                }
                &:hover {
                    border-color: $fontLigtherColor;
                    .atom-icon {
                        color: #63656E;
                    }
                    .skip-icon {
                        color: #c4cdd6;
                    }
                }
                &.CANCELED, &.REVIEWING {
                    border-color: $warningColor;
                    // &:before {
                    //     background: $warningColor;
                    // }

                    // &:after {
                    //     border: 2px solid $warningColor;
                    //     background: white;
                    // }
                    .atom-icon {
                        color: $warningColor;
                    }
                }
                &.FAILED, &.QUALITY_CHECK_FAIL, &.HEARTBEAT_TIMEOUT, &.QUEUE_TIMEOUT, .EXEC_TIMEOUT {
                    border-color: $dangerColor;
                    // &:before {
                    //     background: $dangerColor;
                    // }

                    // &:after {
                    //     border: 2px solid $dangerColor;
                    //     background: white;
                    // }
                    .atom-icon {
                        color: $dangerColor;
                    }
                }
                &.SUCCEED, &.REVIEW_PROCESSED {
                    border-color: $successColor;
                    // &:before {
                    //     background: $successColor;
                    // }

                    // &:after {
                    //     border: 2px solid $successColor;
                    //     background: white;
                    // }
                    .atom-icon {
                        color: $successColor;
                    }
                }
            }
            .arrival-atom {
                &:before {
                    background: $successColor;
                }
                &:after {
                    border: 2px solid $successColor;
                    background: white;
                }
            }
            .qualitt-next-atom {
                &:before {
                    height: 24px;
                    top: -23px;
                }
            }
            .template-compare-atom {
                // border-color: $warningColor;
                border-color: #ff6e00;
                &:hover {
                   border-color: $warningColor;
                }
            }
            .is-fail {
                &:hover {
                   border-color: $dangerColor;
                }
            }
            .is-review,
            .is-intercept {
                &:hover {
                   border-color: $warningColor;
                }
            }
            .is-success,
            .is-process {
                &:hover {
                   border-color: $successColor;
                }
                &:before {
                    background: $successColor;
                }
            }
        }
    }

    .sortable-ghost-atom {
        opacity: 0.5;
    }
    .sortable-chosen-atom {
        transform: scale(1.0);
    }
</style>

<template>
    <li
        :key="atom.id"
        :class="{
            'atom-item': true,
            [atomStatusCls]: true,
            'arrival-atom': atom.status,
            'quality-item': isQualityGateAtom,
            'last-quality-item': isLastQualityAtom,
            'quality-prev-atom': isPrevAtomQuality
        }"
        @click.stop="handleAtomClick(index)"
    >
        <section
            v-if="isQualityGateAtom"
            :class="{
                'atom-section quality-atom': true,
                'is-review': atom.isReviewing,
                [qualityStatus]: !!qualityStatus
            }"
        >
            <span class="atom-title">{{ $t('details.quality.quality') }}</span>
            <span
                v-if="atom.isReviewing && !isBusy"
                :class="{
                    'handler-list': true,
                    'disabled-review': atom.isReviewing && !hasReviewPerm
                }"
            >
                <span class="revire-btn continue-excude" @click.stop="qualityApprove('PROCESS')">{{ $t('resume') }}</span>
                <span class="review-btn stop-excude" @click.stop="qualityApprove('ABORT')">{{ $t('terminate') }}</span>
            </span>
            <i class="devops-icon icon-circle-2-1 executing-job" v-if="atom.isReviewing && isBusy"></i>
        </section>
        <section
            v-else
            :class="{
                'atom-item atom-section normal-atom': true,
                'is-error': atom.isError,
                'is-intercept': isQualityCheckAtom,
                'template-compare-atom': atom.templateModify
            }"
        >
            <status-icon
                v-if="!isSkip && !!atomStatus"
                type="element"
                :status="atomStatus"
                :is-hook="isHookAtom"
            />
            
            <img
                v-else-if="atom.atomIcon"
                :src="atom.atomIcon"
                :class="logoCls"
            />
            <logo v-else
                :class="logoCls"
                :name="svgAtomIcon"
                size="18"
            />
            <p class="atom-name">
                <span
                    :title="atom.name"
                    :class="{ 'skip-name': isSkip }"
                >{{ atom.atomCode ? atom.name : $t('editPage.pendingAtom') }}</span>
            </p>
            <bk-popover v-if="atom.isReviewing" placement="top">
                <span
                    @click.stop="reviewAtom"
                    class="atom-reviewing-tips atom-operate-area"
                    :disabled="hasReviewPerm"
                >{{ $t('editPage.toCheck') }}</span>
                <template slot="content">
                    <p>{{ $t('editPage.checkUser') }}{{ atom.computedReviewers.join(';') }}</p>
                </template>
            </bk-popover>
            <bk-popover v-else-if="atom.status === 'REVIEW_ABORT'" placement="top">
                <span class="atom-review-diasbled-tips">{{ $t('editPage.aborted') }}</span>
                <template slot="content">
                    <p>{{ $t('editPage.abortTips') }}{{ $t('editPage.checkUser') }}{{ execDetail.cancelUserId }}</p>
                </template>
            </bk-popover>
            <template v-else-if="atom.status === 'PAUSE'">
                <bk-popover placement="top" disabled="!Array.isArray(atom.pauseReviewers)">
                    <span
                        :class="[
                            { 'disabled': isBusy || !hasExecPerm },
                            'pause-button'
                        ]"
                        @click.stop="atomExecute(true)"
                    >
                        {{ $t('resume') }}
                    </span>
                    <template slot="content">
                        <p>{{ $t('editPage.checkUser') }}{{ pauseReviewerStr }}</p>
                    </template>
                </bk-popover>
                <span @click.stop="atomExecute(false)" class="pause-button">
                    <i v-if="isBusy" class="devops-icon icon-circle-2-1 executing-job" />
                    <span v-else>{{ $t('pause') }}</span>
                </span>
            </template>
            <span v-else class="atom-operate-area">
                <i v-if="isBusy" class="devops-icon icon-circle-2-1 executing-job" />
                <template v-else>
                    <span
                        v-if="atom.canRetry"
                        @click.stop="skipOrRetry(false)"
                    >
                        {{ $t('retry') }}
                    </span>
                    <span
                        v-if="atom.canSkip"
                        @click.stop="skipOrRetry(true)"
                    >
                        {{ $t('details.statusMap.SKIP') }}
                    </span>
                    <bk-popover
                        v-else-if="!isSkip && !isWaiting && atom.elapsed"
                        placement="top"
                        :disabled="!atom.elapsed"
                    >
                        <span class="atom-execute-time">
                            <span v-if="isElapsedGt1h">&gt;</span>
                            {{ isElapsedGt1h ? '1h' : formatTime }}
                        </span>
                        <template slot="content">
                            <p>{{ formatTime }}</p>
                        </template>
                    </bk-popover>
                </template>
            </span>
            <span
                v-if="editable && stageIndex !== 0 && !atom.isError"
                class="devops-icon copy"
                :title="$t('editPage.copyAtom')"
                @click.stop="copyAtom"
            >
                <Logo name="copy" size="18"></Logo>
            </span>
            <template v-if="editable">
                <i @click.stop="deleteAtom(false)" class="add-plus-icon close" />
                <i v-if="atom.isError" class="devops-icon icon-exclamation-triangle-shape" />
            </template>
            <span v-if="canSkipElement" @click.stop="">
                <bk-checkbox class="atom-canskip-checkbox" v-model="atom.canElementSkip" :disabled="isSkip" />
            </span>
        </section>
    </li>
</template>

<script>
    import { bkPopover, bkCheckbox } from 'bk-magic-vue'
    import StatusIcon from './StatusIcon'
    import Logo from '@/components/Logo'
    import {
        eventBus,
        hashID,
        convertMStoString
    } from './util'
    import {
        CLICK_EVENT_NAME,
        COPY_EVENT_NAME,
        DELETE_EVENT_NAME,
        QUALITY_IN_ATOM_CODE,
        QUALITY_OUT_ATOM_CODE,
        ATOM_CONTINUE_EVENT_NAME,
        ATOM_EXEC_EVENT_NAME,
        ATOM_QUALITY_CHECK_EVENT_NAME,
        ATOM_REVIEW_EVENT_NAME
    } from './constants'

    export default {
        name: 'atom',
        components: {
            StatusIcon,
            Logo,
            bkPopover,
            bkCheckbox
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
            atom: {
                type: Object,
                requiured: true
            },
            stageIndex: {
                type: Number,
                requiured: true
            },
            containerIndex: {
                type: Number,
                requiured: true
            },
            atomIndex: {
                type: Number,
                requiured: true
            },
            isWaiting: Boolean,
            editable: Boolean,
            containerDisabled: Boolean,
            isLastAtom: Boolean,
            prevAtom: {
                type: Object
            },
            userName: {
                type: String,
                default: 'unknow'
            },
            canSkipElement: Boolean,
            matchRules: {
                type: Array,
                default: []
            }
        },
        data () {
            return {
                isBusy: false
            }
        },
        computed: {
            isHookAtom () {
                try {
                    return this.atom.additionalOptions.elementPostInfo
                } catch (error) {
                    return false
                }
            },
            isSkip () {
                try {
                    return this.atom.status === 'SKIP' || !this.atom.additionalOptions.enable || this.containerDisabled
                } catch (error) {
                    return false
                }
            },
            qualityStatus () {
                switch (true) {
                    case ['SUCCEED', 'REVIEW_PROCESSED'].includes(this.atom.status):
                        return 'is-success'
                    case ['QUALITY_CHECK_FAIL', 'REVIEW_ABORT'].includes(this.atom.status):
                        return 'is-fail'
                }
                return ''
            },
            isQualityGateAtom () {
                return this.isQualityGate(this.atom)
            },
            isLastQualityAtom () {
                return this.atom.atomCode === QUALITY_OUT_ATOM_CODE && this.isLastAtom
            },
            isPrevAtomQuality () {
                return this.prevAtom !== null && this.isQualityGate(this.prevAtom)
            },
            atomStatus () {
                try {
                    if (this.atom.status) {
                        return this.atom.status
                    }
                    return this.isWaiting ? 'WAITING' : ''
                } catch (error) {
                    return ''
                }
            },
            atomStatusCls () {
                try {
                    if (this.atom.additionalOptions && this.atom.additionalOptions.enable === false) {
                        return 'DISABLED'
                    }
                    return this.atom.status ? this.atom.status : ''
                } catch (error) {
                    console.error('get atom cls error', error)
                    return ''
                }
            },
            logoCls () {
                return {
                    'atom-icon': true,
                    'skip-icon': this.isSkip
                }
            },
            svgAtomIcon () {
                if (this.isHookAtom) {
                    return 'icon-build-hooks'
                }
                const { atomCode } = this.atom
                if (!atomCode) {
                    return 'placeholder'
                }
                return document.getElementById(atomCode) ? atomCode : 'order'
            },
            hasReviewPerm () {
                return this.atom.computedReviewers.includes(this.userName)
            },
            hasExecPerm () {
                const hasPauseReviewer = Array.isArray(this.atom.pauseReviewers)
                if (!hasPauseReviewer || (hasPauseReviewer && this.atom.pauseReviewers.length === 0)) return true
                return this.atom.pauseReviewers.includes(this.userName)
            },
            pauseReviewerStr () {
                return Array.isArray(this.atom.pauseReviewers) && this.atom.pauseReviewers.join(';')
            },
            isElapsedGt1h () {
                return this.atom.elapsed && this.atom.elapsed >= 36e5
            },
            formatTime () {
                try {
                    return convertMStoString(this.atom.elapsed)
                } catch (error) {
                    return '--'
                }
            },
            isQualityCheckAtom () {
                return Array.isArray(this.matchRules)
                    && this.matchRules.some(rule => rule.taskId === this.atom.atomCode
                        && rule.ruleList.every(val => this.atom.name.indexOf(val.gatewayId) < 0 || !val.gatewayId)
                    )
            }
        },
        methods: {
            reviewAtom () {
                eventBus.$emit(ATOM_REVIEW_EVENT_NAME, this.atom)
            },
            isQualityGate (atom) {
                try {
                    return [QUALITY_IN_ATOM_CODE, QUALITY_OUT_ATOM_CODE].includes(atom.atomCode)
                } catch (error) {
                    return false
                }
            },
            handleAtomClick () {
                eventBus.$emit(CLICK_EVENT_NAME, {
                    stageIndex: this.stageIndex,
                    containerIndex: this.containerIndex,
                    containerGroupIndex: this.containerGroupIndex,
                    elementIndex: this.atomIndex
                })
            },
            copyAtom () {
                const { id, ...restAttr } = this.atom
                this.$emit(COPY_EVENT_NAME, {
                    elementIndex: this.atomIndex,
                    element: JSON.parse(JSON.stringify({
                        ...restAttr,
                        id: `e-${hashID(32)}`
                    }))
                })
            },
            deleteAtom () {
                this.$emit(DELETE_EVENT_NAME, {
                    elementIndex: this.atomIndex
                })
            },
            async atomExecute (isContinue = false) {
                if (this.isBusy || !this.hasExecPerm) return
        
                this.isBusy = true
                const { stageIndex, containerIndex, containerGroupIndex, atomIndex } = this
                        
                await this.asyncEvent(ATOM_EXEC_EVENT_NAME, {
                    stageIndex,
                    containerIndex,
                    containerGroupIndex,
                    isContinue,
                    showPanelType: 'PAUSE',
                    elementIndex: atomIndex,
                    stageId: this.stage.id,
                    containerId: this.container.id,
                    taskId: this.atom.id,
                    atom: this.atom
                })
                setTimeout(() => {
                    this.isBusy = false
                }, 1000)
            },
            
            async qualityApprove (action) {
                if (this.hasReviewPerm) {
                    try {
                        this.isBusy = true
                        const data = {
                            elementId: this.atom.id,
                            action
                        }
                        await this.asyncEvent(ATOM_QUALITY_CHECK_EVENT_NAME, data)
                    } catch (error) {
                        console.error(error)
                    } finally {
                        setTimeout(() => {
                            this.isBusy = false
                        }, 1000)
                    }
                }
            },

            asyncEvent (...args) {
                return new Promise((resolve, reject) => {
                    eventBus.$emit(...args, resolve, reject)
                })
            },
            
            async skipOrRetry (skip = false) {
                if (this.isBusy) return
                try {
                    this.isBusy = true
                        
                    await this.asyncEvent(ATOM_CONTINUE_EVENT_NAME, {
                        taskId: this.atom.id,
                        skip
                    })
                } catch (error) {
                    console.error(error)
                } finally {
                    setTimeout(() => {
                        this.isBusy = false
                    }, 1000)
                }
            }
            
        }
    }
</script>

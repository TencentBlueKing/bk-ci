<template>
    <div class="pipeline-stage-review-control bk-form bk-form-vertical">
        <form-field :label="$t('stageReview.stageInConditions')" class="stage-rule">
            <bk-radio-group class="stage-review-radio-group" v-model="manualTrigger">
                <bk-radio :disabled="disabled" :value="false">{{ $t('disableStageReviewRadioLabel') }}</bk-radio>
                <bk-radio :disabled="disabled" :value="true" style="marginLeft:82px">{{ $t('enableStageReviewRadioLabel') }}</bk-radio>
            </bk-radio-group>
        </form-field>
        <template v-if="manualTrigger">
            <bk-divider class="stage-divider"></bk-divider>

            <form-field required :label="$t('stageReview.approvalFlow')" :is-error="!hasTriggerMember" :error-msg="$t('editPage.stageManualTriggerUserNoEmptyTips')">
                <edit-review-flow
                    :review-groups="reviewGroups"
                    :disabled="disabled"
                    @change="handleUpdateStageControl"
                ></edit-review-flow>
            </form-field>

            <form-field :disabled="disabled" :label="$t('stageReviewInputDesc')" class="mt14">
                <vuex-textarea :placeholder="$t('stageReviewInputDescTip')" name="reviewDesc" clearable :disabled="disabled" :handle-change="handleUpdateStageControl" :value="reviewDesc"></vuex-textarea>
            </form-field>

            <form-field :disabled="disabled" :label="$t('stageReviewInputNotice')" class="mt14">
                <atom-checkbox-list :list="notifyTypeList" :disabled="disabled" name="notifyType" :handle-change="handleUpdateNotifyType" :value="notifyType"></atom-checkbox-list>
            </form-field>

            <form-field v-show="showNotifyGroup" :disabled="disabled" required :label="$t('weChatGroupID')" class="mt14" :is-error="!validWeChatGroupID" :error-msg="$t('stageWeChatGroupIDError')">
                <vuex-input name="notifyGroup" :placeholder="$t('notifyGroupDesc')" required :handle-change="handleUpdateNotifyGroup" :value="notifyGroup"></vuex-input>
            </form-field>

            <form-field :disabled="disabled" class="mt14">
                <atom-checkbox name="markdownContent" :text="$t('markdownContentLabel')" :handle-change="handleUpdateStageControl" :value="markdownContent"></atom-checkbox>
            </form-field>

            <form-field :required="true" :disabled="disabled" :label="$t('stageTimeoutLabel')" class="mt14" :is-error="!validTimeout" :desc="$t('stageTimeoutDesc')" :error-msg="$t('stageTimeoutError')">
                <bk-input type="number" :disabled="disabled" v-model="timeout" :min="1" :max="720">
                    <template slot="append">
                        <div class="group-text">{{ $t('timeMap.hours') }}</div>
                    </template>
                </bk-input>
            </form-field>

            <form-field :disabled="disabled" :label="$t('stageReviewParams')" class="mt14">
                <edit-params
                    :disabled="disabled"
                    :review-params="reviewParams"
                    @change="handleUpdateStageControl"
                ></edit-params>
            </form-field>
        </template>
    </div>
</template>

<script>
    import Vue from 'vue'
    import { mapActions } from 'vuex'
    import EditParams from './components/params/edit'
    import EditReviewFlow from './components/reviewFlow/edit'
    import VuexInput from '@/components/atomFormField/VuexInput'
    import AtomCheckbox from '@/components/atomFormField/AtomCheckbox'
    import FormField from '@/components/AtomPropertyPanel/FormField'
    import VuexTextarea from '@/components/atomFormField/VuexTextarea'
    import AtomCheckboxList from '@/components/atomFormField/AtomCheckboxList'

    export default {
        name: 'stage-review-control',
        components: {
            FormField,
            VuexInput,
            VuexTextarea,
            AtomCheckbox,
            AtomCheckboxList,
            EditParams,
            EditReviewFlow
        },
        props: {
            stage: {
                type: Object,
                default: () => ({})
            },
            disabled: {
                type: Boolean,
                default: false
            },
            stageControl: {
                type: Object,
                default: () => ({})
            },
            stageReviewType: {
                type: String
            }
        },
        computed: {
            manualTrigger: {
                get () {
                    return !!this.stageControl.manualTrigger
                },
                set (manualTrigger) {
                    this.handleUpdateStageControl('manualTrigger', manualTrigger)
                }
            },
            timeout: {
                get () {
                    return this.stageControl.timeout
                },
                set (timeout) {
                    this.handleUpdateStageControl('timeout', timeout)
                }
            },
            reviewGroups () {
                return this.stageControl && Array.isArray(this.stageControl.reviewGroups) ? this.stageControl.reviewGroups : []
            },
            hasTriggerMember () {
                try {
                    return this.manualTrigger && this.reviewGroups.length > 0
                } catch (e) {
                    return false
                }
            },
            validTimeout () {
                return /\d+/.test(this.timeout) && parseInt(this.timeout) > 0 && parseInt(this.timeout) <= 1440
            },
            validWeChatGroupID () {
                return this.notifyGroup.length
            },
            reviewDesc () {
                return this.stageControl && this.stageControl.reviewDesc
            },
            reviewParams () {
                return this.stageControl && Array.isArray(this.stageControl.reviewParams) ? this.stageControl.reviewParams : []
            },
            notifyType () {
                return (this.stageControl && this.stageControl.notifyType) || []
            },
            notifyGroup () {
                return (this.stageControl && this.stageControl.notifyGroup) || []
            },
            markdownContent () {
                return this.stageControl && this.stageControl.markdownContent
            },
            notifyTypeList () {
                return [
                    { id: 'RTX', name: '企业微信', disabled: false, desc: '通过企业微信通知' },
                    { id: 'WEWORK_GROUP', name: '企业微信群消息', disabled: false, desc: '通过企业微信群消息通知' }
                ]
            },
            showNotifyGroup () {
                return this.notifyType.includes('WEWORK_GROUP')
            }
        },
        watch: {
            manualTrigger (val) {
                if (!val) {
                    this.stageControl.notifyGroup = []
                    this.stageControl.notifyType = []
                    this.stageControl.reviewGroups = []
                }
                this.handleUpdateStageControl('isReviewError', !this.validateStageControl())
            },
            hasTriggerMember (hasTriggerMember) {
                this.handleUpdateStageControl('isReviewError', !this.validateStageControl())
            },
            validTimeout (valid) {
                this.handleUpdateStageControl('isReviewError', !this.validateStageControl())
            },
            validWeChatGroupID () {
                this.handleUpdateStageControl('isReviewError', !this.validateStageControl())
            }
        },
        mounted () {
            if (!Object.prototype.hasOwnProperty.call(this.stageControl, 'notifyType')) {
                this.stageControl.notifyType = ['RTX']
            }
            if (!Object.prototype.hasOwnProperty.call(this.stageControl, 'markdownContent')) {
                this.stageControl.markdownContent = true
            }
            if (!this.disabled) {
                this.initStageReview()
                this.handleUpdateStageControl('isReviewError', !this.validateStageControl())
            }
        },
        methods: {
            ...mapActions('atom', [
                'setPipelineEditing',
                'toggleStageReviewPanel',
                'updateStage'
            ]),
            handleStageChange (name, value) {
                if (!Object.prototype.hasOwnProperty.call(this.stage, name)) {
                    Vue.set(this.stage, name, value)
                }
                this.updateStage({
                    stage: this.stage,
                    newParam: {
                        [name]: value
                    }
                })
            },
            handleUpdateStageControl (name, value) {
                this.setPipelineEditing(true)
                this.handleStageChange(this.stageReviewType, {
                    ...(this.stageControl || {}),
                    [name]: value
                })
            },
            initStageReview () {
                if (this.stageControl === undefined || JSON.stringify(this.stageControl) === '{}') {
                    this.handleStageChange(this.stageReviewType, {
                        manualTrigger: false,
                        reviewGroups: [],
                        timeout: 24
                    })
                }
            },
            validateStageControl () {
                return !this.manualTrigger || (this.validTimeout && this.hasTriggerMember && this.validWeChatGroupID)
            },
            handleUpdateNotifyType (name, value) {
                if (!value.includes('WEWORK_GROUP')) {
                    delete this.stageControl.notifyGroup
                }
                this.handleUpdateStageControl(name, value)
            },
            handleUpdateNotifyGroup (name, value) {
                const curVal = value ? value.split(',') : []
                this.handleUpdateStageControl(name, curVal)
            }
        }
    }
</script>

<style lang="scss" scoped>
    .stage-review-radio-group {
        .bk-form-radio {
            margin-right: 16px;
        }
    }
    .stage-rule {
        ::v-deep .bk-form-content {
            min-height: auto;
            line-height: 20px;
        }
    }
    .stage-divider {
        margin: 24px 0 2px !important;
    }
    .mt14 {
        margin-top: 14px !important;
    }
</style>

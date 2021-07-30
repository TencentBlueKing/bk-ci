<template>
    <div class="pipeline-stage-review-control bk-form bk-form-vertical">
        <form-field label="准入规则">
            <bk-radio-group class="stage-review-radio-group" v-model="manualTrigger">
                <bk-radio :disabled="disabled" :value="false">{{ $t('disableStageReviewRadioLabel') }}</bk-radio>
                <bk-radio :disabled="disabled" :value="true" style="marginLeft:82px">{{ $t('enableStageReviewRadioLabel') }}</bk-radio>
            </bk-radio-group>
        </form-field>
        <template v-if="manualTrigger">
            <bk-divider></bk-divider>

            <form-field required label="审核流" :is-error="!hasTriggerMember" :error-msg="$t('editPage.stageManualTriggerUserNoEmptyTips')">
                <edit-review-flow
                    :review-groups="reviewGroups"
                    :disabled="disabled"
                    @change="handleUpdateStageControl"
                ></edit-review-flow>
            </form-field>

            <form-field :disabled="disabled" :label="$t('stageReviewInputDesc')">
                <vuex-textarea :placeholder="$t('stageReviewInputDescTip')" name="reviewDesc" clearable :disabled="disabled" :handle-change="handleUpdateStageControl" :value="reviewDesc"></vuex-textarea>
            </form-field>

            <form-field :required="true" :disabled="disabled" :label="$t('stageTimeoutLabel')" :is-error="!validTimeout" :desc="$t('stageTimeoutDesc')" :error-msg="$t('stageTimeoutError')">
                <bk-input type="number" :disabled="disabled" v-model="timeout" :min="1" :max="720">
                    <template slot="append">
                        <div class="group-text">{{ $t('timeMap.hours') }}</div>
                    </template>
                </bk-input>
            </form-field>

            <form-field :disabled="disabled" :label="$t('stageReviewParams')">
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
    import FormField from '@/components/AtomPropertyPanel/FormField'
    import VuexTextarea from '@/components/atomFormField/VuexTextarea'
    import EditParams from './components/params/edit'
    import EditReviewFlow from './components/reviewFlow/edit'

    export default {
        name: 'stage-review-control',
        components: {
            FormField,
            VuexTextarea,
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
            reviewDesc () {
                return this.stageControl && this.stageControl.reviewDesc
            },
            reviewParams () {
                return this.stageControl && Array.isArray(this.stageControl.reviewParams) ? this.stageControl.reviewParams : []
            }
        },
        watch: {
            manualTrigger (val) {
                !val && this.handleUpdateStageControl('reviewGroups', [])
                this.handleUpdateStageControl('isReviewError', !this.validateStageControl())
            },
            hasTriggerMember (hasTriggerMember) {
                this.handleUpdateStageControl('isReviewError', !this.validateStageControl())
            },
            validTimeout (valid) {
                this.handleUpdateStageControl('isReviewError', !this.validateStageControl())
            }
        },
        mounted () {
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
                if (!this.stage.hasOwnProperty(name)) {
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
                this.handleStageChange('stageControlOption', {
                    ...(this.stageControl || {}),
                    [name]: value
                })
            },
            initStageReview () {
                if (this.stageControl === undefined || JSON.stringify(this.stageControl) === '{}') {
                    this.handleStageChange('stageControlOption', {
                        enable: true,
                        runCondition: 'AFTER_LAST_FINISHED',
                        customVariables: [{ key: 'param1', value: '' }],
                        manualTrigger: false,
                        reviewGroups: [],
                        timeout: 24
                    })
                }
            },
            validateStageControl () {
                return !this.manualTrigger || (this.validTimeout && this.hasTriggerMember)
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
</style>

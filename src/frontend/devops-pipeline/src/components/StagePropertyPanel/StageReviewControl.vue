<template>
    <div class="pipeline-stage-review-control bk-form bk-form-vertical">
        <form-field>
            <bk-checkbox :disabled="disabled" v-model="manualTrigger">
                {{ $t('enableStageReview') }}
            </bk-checkbox>
            <i v-bk-tooltips="$t('stageReviewDesc')" class="bk-icon icon-info-circle" />
        </form-field>
        <template v-if="manualTrigger">
            <form-field :required="true" :disabled="disabled" :label="$t('stageUserTriggers')" :is-error="!hasTriggerMember" :desc="$t('stageTriggerDesc')" :error-msg="$t('editPage.stageManualTriggerUserNoEmptyTips')">
                <bk-input :clearable="true" :disabled="disabled" v-model="triggerUsers"></bk-input>
            </form-field>

            <form-field :required="true" :disabled="disabled" :label="$t('stageTimeoutLabel')" :is-error="!validTimeout" :desc="$t('stageTimeoutDesc')" :error-msg="$t('stageTimeoutError')">
                <bk-input type="number" :disabled="disabled" v-model="timeout" :min="1" :max="1440">
                    <template slot="append">
                        <div class="group-text">{{ $t('timeMap.hours') }}</div>
                    </template>
                </bk-input>
            </form-field>
        </template>
    </div>
</template>

<script>
    import Vue from 'vue'
    import { mapActions } from 'vuex'
    import FormField from '@/components/AtomPropertyPanel/FormField'
    export default {
        name: 'stage-review-control',
        components: {
            FormField
        },
        props: {
            stage: {
                type: Object,
                default: () => ({})
            },
            disabled: {
                type: Boolean,
                default: false
            }
        },
        computed: {
            stageControl () {
                if (this.stage && this.stage.stageControlOption) {
                    return this.stage.stageControlOption
                }
                return {}
            },
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
            triggerUsers: {
                get () {
                    return this.stageControl && Array.isArray(this.stageControl.triggerUsers) ? this.stageControl.triggerUsers.join(',') : ''
                },
                set (triggerUsers) {
                    this.handleUpdateStageControl('triggerUsers', triggerUsers.split(','))
                }

            },
            hasTriggerMember () {
                try {
                    return this.manualTrigger && this.triggerUsers.length > 0
                } catch (e) {
                    return false
                }
            },
            validTimeout () {
                return /\d+/.test(this.timeout) && parseInt(this.timeout) > 0 && parseInt(this.timeout) <= 1440
            }
        },
        watch: {
            manualTrigger (val) {
                !val && this.handleUpdateStageControl('triggerUsers', [])
                this.handleUpdateStageControl('isError', !this.validateStageControl())
            },
            hasTriggerMember (hasTriggerMember) {
                this.handleUpdateStageControl('isError', !this.validateStageControl())
            },
            validTimeout (valid) {
                this.handleUpdateStageControl('isError', !this.validateStageControl())
            }
        },
        mounted () {
            if (!this.disabled) {
                this.initStageReview()
                this.handleUpdateStageControl('isError', !this.validateStageControl())
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
                        manualTrigger: false,
                        triggerUsers: [],
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

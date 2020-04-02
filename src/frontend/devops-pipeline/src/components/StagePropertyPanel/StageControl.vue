<template>
    <accordion show-checkbox show-content>
        <header class="var-header" slot="header">
            <span>{{ $t('editPage.stageOption') }}</span>
            <i class="bk-icon icon-angle-down" style="display:block"></i>
        </header>
        <div slot="content" class="bk-form bk-form-vertical">
            <bk-form :label-width="200" form-type="vertical">
                <bk-form-item>
                    <bk-checkbox :disabled="disabled" v-model="stageEnable">
                        {{ $t('enableStage') }}
                    </bk-checkbox>
                </bk-form-item>
                <bk-form-item>
                    <bk-checkbox :disabled="disabled" v-model="stageFastKill">
                        {{ $t('stageFastKill') }}
                    </bk-checkbox>
                    <i v-bk-tooltips="$t('stageFastKillDesc')" class="bk-icon icon-info-circle" />
                </bk-form-item>
                <bk-form-item>
                    <bk-checkbox :disabled="disabled" v-model="manualTrigger">
                        {{ $t('enableStageReview') }}
                    </bk-checkbox>
                    <i v-bk-tooltips="$t('stageReviewDesc')" class="bk-icon icon-info-circle" />
                </bk-form-item>
                <template v-if="manualTrigger">

                    <form-field :required="true" :label="$t('stageUserTriggers')" :is-error="!hasTriggerMember" :desc="$t('stageTriggerDesc')" :error-msg="$t('editPage.stageManualTriggerUserNoEmptyTips')">
                        <bk-input :clearable="true" :disabled="disabled" v-model="triggerUsers"></bk-input>
                    </form-field>

                    <form-field :required="true" :label="$t('stageTimeoutLabel')" :is-error="!validTimeout" :desc="$t('stageTimeoutDesc')" :error-msg="$t('stageTimeoutError')">
                        <bk-input type="number" :disabled="disabled" v-model="timeout" :min="1" :max="720">
                            <template slot="append">
                                <div class="group-text">{{ $t('timeMap.hours') }}</div>
                            </template>
                        </bk-input>
                    </form-field>
                </template>
                <bk-form-item :label="$t('stageOptionLabel')">
                    <bk-select :disabled="disabled" v-model="stageCondition" searchable>
                        <bk-option v-for="option in conditionConf"
                            :key="option.id"
                            :id="option.id"
                            :name="option.name">
                        </bk-option>
                    </bk-select>
                </bk-form-item>
                <bk-form-item v-if="showVariable">
                    <key-value-normal :disabled="disabled" :value="variables" :allow-null="false" name="customVariables" :handle-change="handleUpdateStageControl"></key-value-normal>
                </bk-form-item>
            </bk-form>
        </div>
    </accordion>
</template>

<script>
    import { mapActions } from 'vuex'
    import Accordion from '@/components/atomFormField/Accordion'
    import KeyValueNormal from '@/components/atomFormField/KeyValueNormal'
    import FormField from '@/components/AtomPropertyPanel/FormField'

    export default {
        name: 'stage-control',
        components: {
            Accordion,
            KeyValueNormal,
            FormField
        },
        props: {
            stageControl: {
                type: Object,
                default: () => ({})
            },
            disabled: {
                type: Boolean,
                default: false
            },
            handleStageChange: {
                type: Function,
                required: true
            }
        },
        computed: {
            stageEnable: {
                get () {
                    return this.stageControl.enable
                },
                set (enable) {
                    this.handleUpdateStageControl('enable', enable)
                }
            },
            stageFastKill: {
                get () {
                    return this.stageControl.fastKill
                },
                set (fastKill) {
                    this.handleStageChange('fastKill', fastKill)
                }
            },
            manualTrigger: {
                get () {
                    return this.stageControl.manualTrigger
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
            stageCondition: {
                get () {
                    return this.stageControl.runCondition
                },
                set (runCondition) {
                    this.handleUpdateStageControl('runCondition', runCondition)
                }
            },
            variables () {
                return this.stageControl && Array.isArray(this.stageControl.customVariables) ? this.stageControl.customVariables : []
            },
            triggerUsers: {
                get () {
                    return this.stageControl && Array.isArray(this.stageControl.triggerUsers) ? this.stageControl.triggerUsers.join(',') : ''
                },
                set (triggerUsers) {
                    this.handleUpdateStageControl('triggerUsers', triggerUsers.split(','))
                }

            },
            conditionConf () {
                return [
                    {
                        id: 'AFTER_LAST_FINISHED',
                        name: this.$t('storeMap.afterPreStageSuccess')
                    },
                    {
                        id: 'CUSTOM_VARIABLE_MATCH',
                        name: this.$t('storeMap.varMatch')
                    },
                    {
                        id: 'CUSTOM_VARIABLE_MATCH_NOT_RUN',
                        name: this.$t('storeMap.varNotMatch')
                    }
                ]
            },
            showVariable () {
                return ['CUSTOM_VARIABLE_MATCH', 'CUSTOM_VARIABLE_MATCH_NOT_RUN'].indexOf(this.stageCondition) > -1
            },
            hasTriggerMember () {
                try {
                    return this.manualTrigger && this.triggerUsers.length > 0
                } catch (e) {
                    return false
                }
            },
            validTimeout () {
                return /\d+/.test(this.timeout) && parseInt(this.timeout) > 0 && parseInt(this.timeout) <= 720
            }
        },
        watch: {
            showVariable (val) {
                !val && this.handleUpdateStageControl('customVariables', [{ key: 'param1', value: '' }])
            },
            manualTrigger (val) {
                !val && this.handleUpdateStageControl('triggerUsers', [])
                this.handleStageChange('isError', !this.validateStageControl())
            },
            hasTriggerMember (hasTriggerMember) {
                this.handleStageChange('isError', !this.validateStageControl())
            },
            validTimeout (valid) {
                this.handleStageChange('isError', !this.validateStageControl())
            }
        },
        created () {
            if (!this.disabled) {
                this.initStageControl()
            }
        },
        methods: {
            ...mapActions('atom', [
                'setPipelineEditing'
            ]),
            handleUpdateStageControl (name, value) {
                this.setPipelineEditing(true)
                this.handleStageChange('stageControlOption', {
                    ...(this.stageControl || {}),
                    [name]: value
                })
            },
            initStageControl () {
                if (this.stageControl === undefined || JSON.stringify(this.stageControl) === '{}') {
                    this.handleStageChange('stageControlOption', {
                        enable: true,
                        runCondition: 'AFTER_LAST_FINISHED',
                        manualTrigger: false,
                        triggerUsers: [],
                        timeout: 24,

                        customVariables: [{ key: 'param1', value: '' }]
                    })
                    this.handleStageChange('fastKill', false)
                }
            },
            validateStageControl () {
                return !this.manualTrigger || (this.validTimeout && this.hasTriggerMember)
            }
        }
    }
</script>

<style lang="scss">
    .stage-trigger-member-input.is-error,
    .stage-timeout-input.is-error {
        color: #ff5656;
    }
</style>

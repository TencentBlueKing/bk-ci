<template>
    <accordion show-checkbox show-content>
        <header class="var-header" slot="header">
            <span>{{ $t('editPage.stageOption') }}</span>
            <i class="bk-icon icon-angle-down" style="display:block"></i>
        </header>
        <div slot="content" class="bk-form bk-form-vertical">
            <bk-form :label-width="200" form-type="vertical">
                <bk-form-item>
                    <bk-checkbox v-model="stageEnable">
                        {{ $t('enableStage') }}
                    </bk-checkbox>
                </bk-form-item>
                <bk-form-item>
                    <bk-checkbox v-model="stageFastKill">
                        {{ $t('stageFastKill') }}
                    </bk-checkbox>
                </bk-form-item>
                <bk-form-item :label="$t('stageOptionLabel')">
                    <bk-select v-model="stageCondition" searchable>
                        <bk-option v-for="option in conditionConf"
                            :key="option.id"
                            :id="option.id"
                            :name="option.name">
                        </bk-option>
                    </bk-select>
                </bk-form-item>
            </bk-form>
        </div>
    </accordion>
</template>

<script>
    import { mapActions } from 'vuex'
    import Accordion from '@/components/atomFormField/Accordion'

    export default {
        name: 'stage-control',
        components: {
            Accordion
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
                    this.handleUpdateStageControl('fastKill', fastKill)
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
            conditionConf () {
                return [
                    {
                        id: 'MANUAL_STAGE_RUNNING',
                        name: this.$t('storeMap.manualRunStage')
                    },
                    {
                        id: 'STAGE_RUNNING',
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
                this.handleStageChange('stageControl', {
                    ...(this.stageControl || {}),
                    [name]: value
                })
            },
            initStageControl () {
                if (this.stageControl === undefined || JSON.stringify(this.stageControl) === '{}') {
                    this.handleStageChange('stageControl', {
                        enable: true,
                        fastKill: false,
                        runCondition: 'STAGE_RUNNING',
                        customVariables: [{ key: 'param1', value: '' }]
                    })
                }
            },
            setKeyValueValidate (addErrors, removeErrors) {
                this.$emit('setKeyValueValidate', addErrors, removeErrors)
            }
        }
    }
</script>

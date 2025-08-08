<template>
    <div class="bk-button-group">
        <bk-button
            v-for="item in pipelineModes"
            size="small"
            :key="item.id"
            :class="item.cls"
            :disabled="isSwitching"
            :loading="isSwitching"
            @click="updateMode(item.id)"
        >
            {{ item.label }}
        </bk-button>
        <bk-dialog
            render-directive="if"
            header-position="left"
            v-model="leaveConfirmVisisble"
            :width="500"
            :title="$t('tips')"
            :z-index="2020"
        >
            {{ $t('saveBeforeSwitch') }}
            <template slot="footer">
                <bk-button
                    theme="primary"
                    :loading="isSaving"
                    @click="handleConfirm(false)"
                >
                    {{ $t('saveDraft&Switch')
                    }}
                </bk-button>
                <bk-button
                    @click="handleConfirm(true)"
                    :loading="isSaving"
                >
                    {{ $t('dropDraft') }}
                </bk-button>
                <bk-button
                    @click="handleClose"
                    :loading="isSaving"
                >
                    {{ $t('cancel') }}
                </bk-button>
            </template>
        </bk-dialog>
    </div>
</template>

<script>
    import { UPDATE_PIPELINE_INFO } from '@/store/modules/atom/constants'
    import { CODE_MODE } from '@/utils/pipelineConst'
    import { mapActions, mapGetters, mapState } from 'vuex'
    export default {
        emit: ['change'],
        props: {
            isYamlSupport: {
                type: [Boolean, null],
                default: null
            },
            draft: Boolean,
            yamlInvalidMsg: String,
            readOnly: {
                type: Boolean,
                default: false
            },
            save: {
                type: Function,
                default: () => { }
            }
        },
        data () {
            return {
                isSwitching: false,
                leaveConfirmVisisble: false,
                isSaving: false,
                newMode: ''
            }
        },
        computed: {
            ...mapState(['pipelineMode', 'modeList']),
            ...mapState('atom', [
                'pipeline',
                'pipelineWithoutTrigger',
                'pipelineSetting',
                'pipelineYaml'
            ]),
            ...mapGetters({
                isEditing: 'atom/isEditing'
            }),
            pipelineModes () {
                return this.modeList.map((mode) => ({
                    label: this.$t(`details.${mode}`),
                    disabled: true,
                    id: mode,
                    cls: this.pipelineMode === mode ? 'is-selected' : ''
                }))
            }
        },

        methods: {
            ...mapActions({
                updatePipelineMode: 'updatePipelineMode',
                canSwitchToYaml: 'atom/canSwitchToYaml',
                setPipelineYaml: 'atom/setPipelineYaml'
            }),
            async handleConfirm (isDrop = false) {
                let result = true
                if (!isDrop) {
                    this.isSaving = true
                    result = await this.save()
                    this.isSaving = false
                }
                if (result) {
                    this.updatePipelineMode(this.newMode)
                    this.$emit('change', this.newMode)
                }
                this.handleClose()
            },
            handleClose () {
                this.leaveConfirmVisisble = false
                this.newMode = ''
            },
            async detectYamlSupport () {
                try {
                    // TODO: 模板不支持YAML
                    if (this.pipeline?.instanceFromTemplate) {
                        return {
                            yamlSupported: false,
                            yamlInvalidMsg: this.$t('templateYamlNotSupport')
                        }
                    }
                    if (typeof this.isYamlSupport === 'boolean') {
                        return {
                            yamlSupported: this.isYamlSupport,
                            yamlInvalidMsg: this.yamlInvalidMsg
                        }
                    }
                    const pipeline = Object.assign({}, this.pipeline, {
                        stages: [
                            this.pipeline.stages[0],
                            ...(this.pipelineWithoutTrigger?.stages ?? [])
                        ]
                    })
                    const { yamlSupported, yamlInvalidMsg, newYaml } = await this.canSwitchToYaml({
                        projectId: this.$route.params.projectId,
                        pipelineId: this.$route.params.pipelineId,
                        modelAndSetting: {
                            model: pipeline,
                            setting: this.pipelineSetting
                        }
                    })
                    return {
                        yamlSupported,
                        yamlInvalidMsg,
                        newYaml
                    }
                } catch (error) {
                    console.log(error)
                    return {
                        yamlSupported: true,
                        yamlInvalidMsg: ''
                    }
                }
            },
            async updateMode (mode) {
                if (this.isSwitching) {
                    return
                }
                if (mode === CODE_MODE) {
                    this.isSwitching = true
                    const { yamlSupported, yamlInvalidMsg, newYaml } = await this.detectYamlSupport()
                    if (!yamlSupported && yamlInvalidMsg) {
                        this.$bkInfo({
                            type: this.pipeline?.instanceFromTemplate ? 'warning' : 'error',
                            width: 500,
                            zIndex: 2020,
                            title: this.$t(this.pipeline?.instanceFromTemplate ? 'unSupportCodeMode' : 'invalidCodeMode'),
                            subHeader: this.$createElement(
                                'pre',
                                {
                                    style: {
                                        padding: '16px',
                                        background: '#f5f5f5',
                                        textAlign: 'left',
                                        lineHeight: '24px',
                                        whiteSpace: 'pre-wrap',
                                        wordBreak: 'break-all'
                                    }
                                },
                                yamlInvalidMsg
                            ),
                            showFooter: false
                        })
                        this.$store.commit(`atom/${UPDATE_PIPELINE_INFO}`, {
                            yamlSupported: true,
                            yamlInvalidMsg: ''
                        })
                        this.isSwitching = false
                        return
                    } else if (this.draft) {
                        this.setPipelineYaml(newYaml)
                    }
                    this.isSwitching = false
                }
                if (!this.draft && this.isEditing && !this.readOnly) {
                    this.leaveConfirmVisisble = true
                    this.newMode = mode
                    return
                }
                this.updatePipelineMode(mode)
                this.$emit('change', mode)
            }
        }
    }
</script>
<style lang="scss" scoped>
.bk-button-group {
    display: grid;
    grid-template-columns: repeat(2, 1fr);

}
</style>

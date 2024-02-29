<template>
    <div class="bk-button-group">
        <bk-button
            v-for="item in pipelineModes"
            size="small"
            :key="item.id"
            :class="item.cls"
            :disabled="isSwitching"
            @click="updateMode(item.id)"
        >
            {{ item.label }}
        </bk-button>
    </div>
</template>

<script>
    import { mapState, mapActions, mapGetters } from 'vuex'
    import { CODE_MODE } from '@/utils/pipelineConst'
    import { UPDATE_PIPELINE_INFO } from '@/store/modules/atom/constants'
    export default {
        emit: ['change'],
        props: {
            isYamlSupport: {
                type: [Boolean, null],
                default: null
            },
            yamlInvalidMsg: String,
            readOnly: {
                type: Boolean,
                default: false
            },
            save: {
                type: Function,
                default: () => {}
            }
        },
        data () {
            return {
                isSwitching: false
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
            },
            canSwitch () {
                return !(!this.readOnly && this.isEditing)
            }
        },

        methods: {
            ...mapActions({
                updatePipelineMode: 'updatePipelineMode',
                canSwitchToYaml: 'atom/canSwitchToYaml'
            }),
            async detectYamlSupport () {
                try {
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
                    const { yamlSupported, yamlInvalidMsg } = await this.canSwitchToYaml({
                        projectId: this.$route.params.projectId,
                        pipelineId: this.$route.params.pipelineId,
                        modelAndSetting: {
                            model: pipeline,
                            setting: this.pipelineSetting
                        }
                    })
                    return {
                        yamlSupported,
                        yamlInvalidMsg
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
                if (this.isEditing) {
                    this.$bkInfo({
                        title: this.$t('tips'),
                        subTitle: this.$t('saveBeforeSwitch'),
                        okText: this.$t('saveDraft&Switch'),
                        confirmFn: async () => {
                            await this.save()
                            this.updatePipelineMode(mode)
                            this.$emit('change', mode)
                        }
                    })
                    return
                }
                if (mode === CODE_MODE) {
                    this.isSwitching = true
                    const { yamlSupported, yamlInvalidMsg } = await this.detectYamlSupport()
                    if (!yamlSupported && yamlInvalidMsg) {
                        this.$bkInfo({
                            type: 'error',
                            width: 500,
                            title: this.$t('invalidCodeMode'),
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
                    }
                    this.isSwitching = false
                }
                this.updatePipelineMode(mode)
                this.$emit('change', mode)
            }
        }
    }
</script>

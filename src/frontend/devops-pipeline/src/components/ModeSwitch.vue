<template>
    <div class="bk-button-group">
        <bk-button
            v-for="item in pipelineModes"
            size="small"
            :key="item.id"
            :class="item.cls"
            @click="updateMode(item.id)"
        >
            {{ item.label }}
        </bk-button>
    </div>
</template>

<script>
    import { mapState, mapActions } from 'vuex'
    import { CODE_MODE } from '@/utils/pipelineConst'
    export default {
        props: {
            isYamlSupport: {
                type: Boolean,
                default: true
            },
            yamlInvalidMsg: {
                type: String,
                default: ''
            }
        },
        emit: ['change'],
        computed: {
            ...mapState([
                'pipelineMode',
                'modeList'
            ]),
            pipelineModes () {
                return this.modeList.map(mode => ({
                    label: this.$t(`details.${mode}`),
                    disabled: true,
                    id: mode,
                    cls: this.pipelineMode === mode ? 'is-selected' : ''
                }))
            }
        },
        methods: {
            ...mapActions([
                'updatePipelineMode'
            ]),
            updateMode (mode) {
                if (mode === CODE_MODE && !this.isYamlSupport && this.yamlInvalidMsg) {
                    this.$bkInfo({
                        type: 'error',
                        width: 500,
                        title: this.$t('invalidCodeMode'),
                        subHeader: this.$createElement('pre', {
                            style: {
                                padding: '16px',
                                background: '#f5f5f5',
                                textAlign: 'left',
                                lineHeight: '24px',
                                whiteSpace: 'pre-wrap',
                                wordBreak: 'break-all'
                            }
                        }, this.yamlInvalidMsg),
                        showFooter: false
                    })
                    return
                }
                this.updatePipelineMode(mode)
                this.$emit('change', mode)
            }
        }
    }
</script>

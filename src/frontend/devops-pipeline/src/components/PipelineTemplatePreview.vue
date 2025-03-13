<template>
    <bk-dialog
        v-model="value"
        :draggable="false"
        width="90%"
        height="90%"
        :auto-close="false"
        :show-footer="false"
        header-position="left"
        :title="title"
        ext-cls="pipeline-template-preivew"
        @cancel="handleCancel"
    >
        <mode-switch
            style="width: 150px;"
            :is-yaml-support="isYamlSupport"
            :yaml-invalid-msg="yamlInvalidMsg"
            read-only
        />
        <YamlEditor
            v-if="isCodeMode"
            style="margin-top: 20px"
            :value="templateYaml"
            read-only
            :highlight-ranges="highlightMarkList"
        />
        <bk-tab
            v-else
            :active.sync="activePanel"
            type="unborder-card"
        >
            <bk-tab-panel
                v-for="panel in panels"
                :key="panel.name"
                :label="panel.label"
                :name="panel.name"
            >
                <component
                    v-if="value"
                    style="pointer-events: none"
                    v-bind="panel.props"
                    :is="panel.component"
                />
            </bk-tab-panel>
        </bk-tab>
    </bk-dialog>
</template>

<script>
    import ModeSwitch from '@/components/ModeSwitch'
    import Pipeline from '@/components/Pipeline'
    import { BaseSettingTab, NotifyTab, TriggerTab } from '@/components/PipelineEditTabs/'
    import YamlEditor from '@/components/YamlEditor'
    import { UI_MODE } from '@/utils/pipelineConst'
    import { mapActions, mapGetters } from 'vuex'
    export default {
        components: {
            ModeSwitch,
            Pipeline,
            TriggerTab,
            NotifyTab,
            BaseSettingTab,
            YamlEditor
        },
        props: {
            value: Boolean,
            previewSettingType: {
                type: String,
                default: ''
            },
            templatePipeline: {
                type: Object
            }
        },
        data () {
            return {
                isLoading: false,
                activePanel: 'pipelineModel',
                templateYaml: '',
                highlightMarkList: [],
                isYamlSupport: true,
                yamlInvalidMsg: '',
                pipelineSetting: null
            }
        },
        computed: {
            ...mapGetters({
                isCodeMode: 'isCodeMode'
            }),
            title () {
                return this.$t('templatePreivewHeader', [
                    this.templatePipeline?.name ?? '',
                    this.previewSettingType
                ])
            },
            highlightType () {
                const conf = {
                    useSubscriptionSettings: 'NOTIFY',
                    useLabelSettings: 'LABEL',
                    useConcurrencyGroup: 'CONCURRENCY'
                }
                return conf[this.previewSettingType] ?? 'PIPELINE_MODEL'
            },
            panels () {
                return [
                    {
                        name: 'pipelineModel',
                        label: this.$t('pipelineModel'),
                        component: 'Pipeline',
                        props: {
                            pipeline: {
                                ...this.templatePipeline,
                                stages: this.templatePipeline?.stages?.slice(1) ?? []
                            },
                            editable: false,
                            showHeader: false
                        }
                    },
                    {
                        name: 'triggerConf',
                        label: this.$t('triggerName'),
                        component: 'triggerTab',
                        props: {
                            editable: false,
                            pipeline: this.templatePipeline
                        }
                    },
                    {
                        name: 'notification',
                        label: this.$t('settings.notify'),
                        component: 'NotifyTab',
                        props: {
                            editable: false,
                            failSubscriptionList: this.pipelineSetting?.failSubscriptionList ?? [],
                            successSubscriptionList: this.pipelineSetting?.successSubscriptionList ?? []
                        }
                    },
                    {
                        name: 'baseConfig',
                        label: this.$t('editPage.baseSetting'),
                        component: 'BaseSettingTab',
                        props: {
                            editable: false,
                            pipelineSetting: this.pipelineSetting
                        }
                    }
                ]
            }
        },
        watch: {
            value (val) {
                if (val) {
                    if (this.previewSettingType) {
                        const tabMap = {
                            useSubscriptionSettings: 'notification'
                        }
                        this.activePanel = tabMap[this.previewSettingType] ?? 'baseConfig'
                    }
                    this.init()
                } else {
                    this.highlightMarkList = []
                    this.templateYaml = ''
                    this.activePanel = 'pipelineModel'
                }
            }
        },
        methods: {
            ...mapActions({
                updatePipelineMode: 'updatePipelineMode',
                requestTemplatePreview: 'pipelines/requestTemplatePreview'
            }),
            async init () {
                try {
                    this.isLoading = true
                    const res = await this.requestTemplatePreview({
                        projectId: this.$route.params.projectId,
                        templateId: this.templatePipeline.templateId,
                        highlightType: this.highlightType
                    })
                    if (!res.yamlSupported && this.isCodeMode) {
                        this.updatePipelineMode(UI_MODE)
                    } else {
                        this.templateYaml = res.templateYaml
                        this.highlightMarkList = res.highlightMarkList ?? []
                    }
                    this.isYamlSupport = res.yamlSupported
                    this.yamlInvalidMsg = res.yamlInvalidMsg
                    this.pipelineSetting = res.setting
                } catch (error) {
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message
                    })
                } finally {
                    this.isLoading = false
                }
            },
            handleCancel () {
                this.$emit('input', false)
            }
        }
    }
</script>

<style lang="scss">
.pipeline-template-preivew {
  height: 100%;
  .bk-dialog {
    top: 2vh;
    height: 96vh;
    .bk-dialog-content {
      height: 100%;
      display: flex;
      flex-direction: column;
      overflow: hidden;
      .bk-dialog-body {
        flex: 1;
        overflow: auto;
        display: flex;
        flex-direction: column;
        .bk-tab {
          flex: 1;
          display: flex;
          flex-direction: column;
          .bk-tab-header {
            flex-shrink: 0;
          }
          .bk-tab-section {
            flex: 1;
            overflow: hidden;
            .bk-tab-content {
              height: 100%;
            }
          }
        }
      }
    }
  }
}
</style>

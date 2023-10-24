<template>
    <bk-dialog
        v-model="isShow"
        v-bk-loading="{ isLoading }"
        width="90%"
        height="90%"
        :auto-close="false"
        :show-footer="false"
        header-position="left"
        render-directive="if"
        :title="title"
        ext-cls="pipeline-template-preivew"
        @cancel="handleCancel"
    >
        <template v-if="templatePipeline && isShow">
            <mode-switch />
            <YamlEditor
                v-if="isCodeMode"
                style="margin-top: 20px;"
                :value="templateYaml"
                read-only
                :highlight-ranges="highlightMarkList"
            />
            <bk-tab
                v-else
                v-model="activePanel"
                type="unborder-card"
            >
                <bk-tab-panel
                    v-for="panel in panels"
                    :key="panel.name"
                    :label="panel.label"
                    :name="panel.name"
                    render-directive="if"
                >
                    <component
                        style="pointer-events: none"
                        v-bind="panel.props"
                        :is="panel.component"
                    />
                </bk-tab-panel>
            </bk-tab>
        </template>
    </bk-dialog>
</template>

<script>
    import { mapGetters, mapActions } from 'vuex'
    import ModeSwitch from '@/components/ModeSwitch'
    import YamlEditor from '@/components/YamlEditor'
    import Pipeline from '@/components/Pipeline'
    import { BaseSettingTab, NotifyTab, TriggerTab } from '@/components/PipelineEditTabs/'
    export default {
        components: {
            ModeSwitch,
            // eslint-disable-next-line vue/no-unused-components
            Pipeline,
            // eslint-disable-next-line vue/no-unused-components
            TriggerTab,
            // eslint-disable-next-line vue/no-unused-components
            NotifyTab,
            // eslint-disable-next-line vue/no-unused-components
            BaseSettingTab,
            YamlEditor

        },
        props: {
            isShow: Boolean,
            previewSettingType: {
                type: String,
                default: ''
            },
            templatePipeline: {
                type: Object,
                required: true
            }
        },
        data () {
            return {
                isLoading: false,
                activePanel: 'pipelineModel',
                templateYaml: '',
                highlightMarkList: [],
                pipelineSetting: null
            }
        },
        computed: {
            ...mapGetters(['isCodeMode']),
            title () {
                return this.$t('templatePreivewHeader', [this.templatePipeline?.name ?? '', this.previewSettingType])
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
                        label: this.$t('流水线编排'),
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
                        label: this.$t('触发器'),
                        component: 'triggerTab',
                        props: {
                            editable: false,
                            clickable: false,
                            pipeline: this.templatePipeline
                        }
                    },
                    {
                        name: 'notification',
                        label: this.$t('通知'),
                        component: 'NotifyTab',
                        props: {
                            editable: false,
                            failSubscriptionList: this.pipelineSetting?.failSubscriptionList ?? null,
                            successSubscriptionList: this.pipelineSetting?.successSubscriptionList ?? null
                        }
                    },
                    {
                        name: 'baseConfig',
                        label: this.$t('基础设置'),
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
            isShow (val) {
                if (val) {
                    this.init()
                } else {
                    this.highlightMarkList = []
                    this.templateYaml = ''
                    this.activePanel = 'pipelineModel'
                }
            }
        },
        created () {
            console.log('templatePipeline', this.templatePipeline)
        },
        methods: {
            ...mapActions('pipelines', [
                'requestTemplatePreview'
            ]),
            async init () {
                try {
                    this.isLoading = true
                    const res = await this.requestTemplatePreview({
                        projectId: this.templatePipeline.projectId,
                        templateId: this.templatePipeline.templateId,
                        highlightType: this.highlightType
                    })
                    this.templateYaml = res.templateYaml
                    this.highlightMarkList = res.highlightMarkList ?? []
                    this.pipelineSetting = res.setting
                    console.log('res', res)
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
                this.$emit('update:isShow', false)
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

<template>
    <bk-dialog
        v-model="isShow"
        width="90%"
        height="90%"
        :auto-close="false"
        :show-footer="false"
        header-position="left"
        render-directive="if"
        :title="$t('预览模板 - 模板名称')"
        ext-cls="pipeline-template-preivew"
        @cancel="handleCancel"
    >
        <template v-if="templatePipeline && isShow">
            <mode-switch v-model="pipelineMode" />
            <bk-tab
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
                    <Ace v-if="pipelineMode === 'codeMode'" height="100%" width="100%" />
                    <component
                        v-else
                        v-bind="panel.props"
                        :is="panel.component"
                    />
                </bk-tab-panel>
            </bk-tab>
        </template>
    </bk-dialog>
</template>

<script>
    import ModeSwitch from '@/components/ModeSwitch'
    import Ace from '@/components/common/ace-editor'
    import Pipeline from '@/components/Pipeline'
    import TriggerConfig from '@/components/PipelineDetailTabs/TriggerConfig'
    import NotificationConfig from '@/components/PipelineDetailTabs/NotificationConfig'
    import BaseConfig from '@/components/PipelineDetailTabs/BaseConfig'
    export default {
        components: {
            ModeSwitch,
            // eslint-disable-next-line vue/no-unused-components
            Pipeline,
            // eslint-disable-next-line vue/no-unused-components
            TriggerConfig,
            // eslint-disable-next-line vue/no-unused-components
            NotificationConfig,
            // eslint-disable-next-line vue/no-unused-components
            BaseConfig,
            Ace

        },
        props: {
            isShow: Boolean,
            templatePipeline: {
                type: Object,
                required: true
            }
        },
        data () {
            return {
                pipelineMode: 'uiMode',
                activePanel: 'pipelineModel'
            }
        },
        computed: {
            panels () {
                return [
                    {
                        name: 'pipelineModel',
                        label: this.$t('流水线编排'),
                        component: 'Pipeline',
                        props: {
                            pipeline: this.templatePipeline,
                            editable: false,
                            showHeader: false
                        }
                    },
                    {
                        name: 'triggerConf',
                        label: this.$t('触发器'),
                        component: 'TriggerConfig',
                        props: {}
                    },
                    {
                        name: 'notification',
                        label: this.$t('通知'),
                        component: 'NotificationConfig',
                        props: {}
                    },
                    {
                        name: 'baseConfig',
                        label: this.$t('基础设置'),
                        component: 'BaseConfig',
                        props: {}
                    }
                ]
            }
        },
        methods: {
            handleCancel () {
                this.$emit('update:isShow', false)
            },
            handleTabChange () {
                debugger
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

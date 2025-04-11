<template>
    <section
        v-bkloading="{ isLoading }"
        class="bkdevops-pipeline-edit-wrapper"
    >
        <empty-tips
            v-if="hasNoPermission"
            :show-lock="true"
            :title="noPermissionTipsConfig.title"
            :desc="noPermissionTipsConfig.desc"
            :btns="noPermissionTipsConfig.btns"
        >
        </empty-tips>
        <YamlPipelineEditor
            v-else-if="isCodeMode"
            :editable="!isTemplatePipeline"
        />
        <template v-else>
            <show-variable
                v-if="currentTab === 'pipeline' && pipeline"
                :editable="!isTemplatePipeline"
                :pipeline="pipeline"
            />
            <header
                class="choose-type-switcher"
                :class="{ 'when-show-variable': currentTab === 'pipeline' && showVariable }"
            >
                <span
                    v-for="panel in panels"
                    :key="panel.name"
                    :class="[
                        'choose-type-switcher-tab',
                        {
                            active: currentTab === panel.name
                        }
                    ]"
                    @click="switchTab(panel)"
                >
                    {{ panel.label }}
                </span>
            </header>

            <div
                class="edit-content-area"
                :class="{ 'when-show-variable': currentTab === 'pipeline' && showVariable }"
            >
                <component
                    :is="curPanel.component"
                    v-bind="curPanel.bindData"
                    v-on="curPanel.listeners"
                ></component>
                <template v-if="!isLoading && pipeline && currentTab === 'pipeline'">
                    <!-- <mini-map :stages="pipeline.stages" scroll-class=".bk-tab-section .bk-tab-content"></mini-map> -->
                </template>
            </div>
        </template>
    </section>
</template>

<script>
    import MiniMap from '@/components/MiniMap'
    import {
        AuthorityTab,
        BaseSettingTab,
        NotifyTab,
        PipelineEditTab,
        ShowVariable,
        TriggerTab
    } from '@/components/PipelineEditTabs/'
    import emptyTips from '@/components/devops/emptyTips'
    import pipelineOperateMixin from '@/mixins/pipeline-operate-mixin'
    import { RESOURCE_ACTION, handlePipelineNoPermission } from '@/utils/permission'
    import { pipelineTabIdMap } from '@/utils/pipelineConst'
    import { navConfirm } from '@/utils/util'
    import { mapActions, mapGetters, mapState } from 'vuex'
    import YamlPipelineEditor from './YamlPipelineEditor'
    import { TEMPLATE_MODE } from '@/store/modules/templates/constants'

    export default {
        components: {
            emptyTips,
            PipelineEditTab,
            BaseSettingTab,
            TriggerTab,
            NotifyTab,
            ShowVariable,
            MiniMap,
            YamlPipelineEditor,
            AuthorityTab
        },
        mixins: [pipelineOperateMixin],
        data () {
            return {
                isLoading: false,
                hasNoPermission: false,
                leaving: false,
                confirmMsg: this.$t('editPage.confirmMsg'),
                noPermissionTipsConfig: {
                    title: this.$t('noPermission'),
                    desc: this.$t('history.noEditPermissionTips'),
                    btns: [
                        {
                            theme: 'primary',
                            size: 'normal',
                            handler: this.changeProject,
                            text: this.$t('changeProject')
                        },
                        {
                            theme: 'success',
                            size: 'normal',
                            handler: () => {
                                handlePipelineNoPermission({
                                    projectId: this.$route.params.projectId,
                                    resourceCode: this.pipelineId,
                                    action: RESOURCE_ACTION.EDIT
                                })
                            },
                            text: this.$t('applyPermission')
                        }
                    ]
                }
            }
        },
        computed: {
            ...mapState(['fetchError']),
            ...mapState('atom', [
                'pipeline',
                'pipelineInfo',
                'pipelineWithoutTrigger',
                'pipelineYaml',
                'pipelineSetting',
                'editfromImport',
                'showVariable'
            ]),
            ...mapGetters({
                isCodeMode: 'isCodeMode',
                getPipelineSubscriptions: 'atom/getPipelineSubscriptions',
                isTemplate: 'atom/isTemplate'
            }),
            pipelineVersion () {
                return this.pipelineInfo?.version
            },
            projectId () {
                return this.$route.params.projectId
            },
            pipelineId () {
                return this.pipelineInfo?.pipelineId
            },
            currentTab () {
                return this.$route.query.tab || 'pipeline'
            },
            curPanel () {
                return this.panels.find((panel) => panel.name === this.currentTab)
            },
            isTemplatePipeline () {
                return (this.pipelineInfo?.mode === TEMPLATE_MODE.CONSTRAINT && this.isTemplate) ?? false
            },
            panels () {
                return [
                    {
                        name: pipelineTabIdMap.pipeline,
                        label: this.$t('pipeline'),
                        component: 'PipelineEditTab',
                        bindData: {
                            editable: !this.isTemplatePipeline,
                            pipeline: this.pipelineWithoutTrigger,
                            isLoading: !this.pipelineWithoutTrigger
                        }
                    },
                    {
                        name: pipelineTabIdMap.trigger,
                        label: this.$t('settings.trigger'),
                        component: 'TriggerTab',
                        bindData: {
                            editable: !this.isTemplatePipeline,
                            pipeline: this.pipeline
                        }
                    },
                    {
                        name: pipelineTabIdMap.notice,
                        label: this.$t('settings.notify'),
                        component: 'NotifyTab',
                        bindData: {
                            failSubscriptionList: this.getPipelineSubscriptions('fail'),
                            successSubscriptionList: this.getPipelineSubscriptions('success'),
                            updateSubscription: (name, value) => {
                                this.setPipelineEditing(true)
                                this.updatePipelineSetting({
                                    setting: this.pipelineSetting,
                                    param: {
                                        [name]: value
                                    }
                                })
                            }
                        }
                    },
                    {
                        name: pipelineTabIdMap.setting,
                        label: this.$t('editPage.baseSetting'),
                        component: 'BaseSettingTab',
                        bindData: {
                            pipelineSetting: this.pipelineSetting,
                            updatePipelineSetting: (...args) => {
                                this.setPipelineEditing(true)
                                this.updatePipelineSetting(...args)
                            }
                        }
                    }
                ]
            }
        },
        watch: {
            pipelineId (pipelineId, oldId) {
                this.$nextTick(() => {
                    this.init()
                })
            },
            pipeline (val) {
                this.getInterceptAtom()
                if (val && val.instanceFromTemplate) this.requestMatchTemplateRules(val.templateId)
            },
            'pipelineInfo.permissions.canEdit': {
                handler (val) {
                    if (typeof val === 'boolean') {
                        this.hasNoPermission = !val
                        if (!val) {
                            this.removeLeaveListener()
                        }
                    }
                },
                immediate: true
            },
            fetchError (error) {
                if (error.code === 403) {
                    this.hasNoPermission = true
                    this.removeLeaveListener()
                }
            },
            isCodeMode: function (val) {
                this.togglePropertyPanel({
                    isShow: false
                })
                this.setPipelineEditing(false)
                if (!this.editfromImport) {
                    this.init()
                }
            }
        },
        mounted () {
            if (!this.editfromImport) {
                this.init()
            }
            this.getQualityAtom()
            this.addLeaveListenr()
        },
        beforeDestroy () {
            this.removeLeaveListener()
            this.setPipelineEditing(false)
            this.setSaveStatus(false)
            this.setEditFrom(false)
            this.setAtomEditing(false)
            this.togglePropertyPanel({
                isShow: false
            })
            this.errors.clear()
        },
        beforeRouteUpdate (to, from, next) {
            if (from.name !== to.name) {
                this.leaveConfirm(to, from, next)
            } else {
                next()
            }
        },
        beforeRouteLeave (to, from, next) {
            this.leaveConfirm(to, from, next)
        },
        methods: {
            ...mapActions('atom', [
                'requestPipeline',
                'togglePropertyPanel',
                'setPipelineEditing',
                'setSaveStatus',
                'setEditFrom',
                'updatePipelineSetting',
                'setAtomEditing'
            ]),
            ...mapActions('common', [
                'requestQualityAtom',
                'requestInterceptAtom',
                'requestMatchTemplateRuleList'
            ]),
            async init () {
                if (this.pipelineVersion) {
                    this.isLoading = true
                    await this.requestPipeline({
                        ...this.$route.params,
                        version: this.pipelineVersion
                    })
                    this.isLoading = false
                }
            },
            switchTab (tab) {
                this.$router.replace({
                    query: {
                        tab: tab.name
                    }
                })
            },
            leaveConfirm (to, from, next) {
                if (!this.leaving) {
                    if (this.isEditing) {
                        this.leaving = true
                        navConfirm({
                            title: this.$t('leaveConfirmTitle'),
                            content: this.$t('leaveConfirmTips'),
                            width: 600
                        })
                            .then((result) => {
                                next(result)
                                this.leaving = false
                            })
                            .catch(() => {
                                next(false)
                                this.leaving = false
                            })
                    } else {
                        next(true)
                    }
                } else {
                    next(true)
                }
            },
            addLeaveListenr () {
                window.addEventListener('beforeunload', this.leaveSure)
            },
            removeLeaveListener () {
                window.removeEventListener('beforeunload', this.leaveSure)
            },
            leaveSure (e) {
                e.returnValue = this.confirmMsg
                return this.confirmMsg
            },
            getQualityAtom () {
                this.requestQualityAtom({
                    projectId: this.projectId
                })
            },
            getInterceptAtom () {
                if (this.projectId && this.pipelineId) {
                    this.requestInterceptAtom({
                        projectId: this.projectId,
                        pipelineId: this.pipelineId
                    })
                }
            },
            requestMatchTemplateRules (templateId) {
                this.requestMatchTemplateRuleList({
                    projectId: this.projectId,
                    templateId
                })
            }
        }
    }
</script>

<style lang="scss">
@import "@/scss/conf";

.new-ui-form {

    .ui-inner-label,
    .bk-label {
        font-size: 12px;
        line-height: 20px;
        min-height: 20px;
        margin-bottom: 4px;
    }

    .ui-inner-label {
        display: flex;
        align-items: center;
    }

    .bk-form-tip,
    .bk-form-help {
        margin-bottom: 0;
        line-height: 18px;
    }

    i.icon-question-circle-shape {
        color: #979ba5;
        font-size: 14px;
    }
}

.new-ui-form.bk-form-vertical .bk-form-item+.bk-form-item {
    margin-top: 24px;
}

.bkdevops-pipeline-edit-wrapper {
    .choose-type-switcher {
        background: #f0f1f5;
        height: 42px;
        flex-shrink: 0;
        display: flex;
        align-items: center;
        margin: 24px 24px 0;

        &.when-show-variable {
            margin-right: 496px;
        }

        .choose-type-switcher-tab {
            padding: 0 18px;
            height: 42px;
            font-size: 14px;
            display: flex;
            align-items: center;
            position: relative;
            cursor: pointer;

            &:hover {
                color: $primaryColor;
            }

            &:not(:first-child)::before {
                position: absolute;
                content: "";
                width: 1px;
                height: 16px;
                background-color: #c4c6cc;
                top: 13px;
                left: 0;
            }

            &:last-child::after {
                position: absolute;
                content: "";
                width: 1px;
                height: 16px;
                background-color: #c4c6cc;
                top: 13px;
                right: 0;
            }

            &.active {
                background-color: white;
                border-radius: 4px 4px 0 0;

                &::before,
                &::after {
                    background-color: white;
                }
            }

            &.active+ ::before {
                display: none;
            }
        }
    }

    .edit-content-area {
        position: relative;
        background: white;
        height: calc(100vh - 114px);
        margin: 0 24px;
        padding: 24px 24px 40px;
        overflow-y: auto;
        flex: 1;
        box-shadow: 0 2px 2px 0 #00000026;

        &.when-show-variable {
            margin-right: 496px;
        }
    }

    .choose-type-container {
        margin: 26px 24px 20px;
    }

    .bk-tab-section {
        padding: 0 25px 20px;
    }

    .bkdevops-pipeline-edit-tab {
        flex: 1;
        display: flex;
        flex-direction: column;
        width: 100%;
        overflow: hidden;

        .bk-tab-content {
            overflow: auto;
        }
    }
}
</style>

<template>
    <section v-bkloading="{ isLoading }" class="bkdevops-pipeline-edit-wrapper">
        <empty-tips
            v-if="hasNoPermission"
            :show-lock="true"
            :title="noPermissionTipsConfig.title"
            :desc="noPermissionTipsConfig.desc"
            :btns="noPermissionTipsConfig.btns">
        </empty-tips>
        <YamlPipelineEditor v-else-if="isCodeMode" />
        <template v-else>
            <header class="choose-type-switcher">
                <span
                    v-for="panel in panels"
                    :key="panel.name"
                    :class="['choose-type-switcher-tab', {
                        active: currentTab === panel.name
                    }]"
                    @click="switchTab(panel)"
                >
                    {{ panel.label }}
                </span>
            </header>

            <div :class="['edit-content-area']">
                <component
                    :is="curPanel.component"
                    v-bind="curPanel.bindData"
                    v-on="curPanel.listeners"
                ></component>
                <template v-if="!isLoading && pipeline && currentTab === 'pipeline'">
                    <show-variable :pipeline="pipeline" />
                    <!-- <mini-map :stages="pipeline.stages" scroll-class=".bk-tab-section .bk-tab-content"></mini-map> -->
                </template>
            </div>
        </template>
    </section>
</template>

<script>
    import MiniMap from '@/components/MiniMap'
    import { navConfirm } from '@/utils/util'
    import { PipelineEditTab, BaseSettingTab, TriggerTab, NotifyTab, ShowVariable } from '@/components/PipelineEditTabs/'
    import pipelineOperateMixin from '@/mixins/pipeline-operate-mixin'
    import { mapActions, mapState, mapGetters } from 'vuex'
    import YamlPipelineEditor from './YamlPipelineEditor'
    import emptyTips from '@/components/devops/emptyTips'

    export default {
        components: {
            emptyTips,
            PipelineEditTab,
            BaseSettingTab,
            TriggerTab,
            NotifyTab,
            ShowVariable,
            MiniMap,
            YamlPipelineEditor
        },
        mixins: [pipelineOperateMixin],
        data () {
            return {
                isLoading: false,
                hasNoPermission: false,
                leaving: false,
                confirmMsg: this.$t('editPage.confirmMsg'),
                confirmTitle: this.$t('editPage.confirmTitle'),
                noPermissionTipsConfig: {
                    title: this.$t('noPermission'),
                    desc: this.$t('history.noPermissionTips'),
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
                                this.toApplyPermission(this.$permissionActionMap.edit, {
                                    id: this.pipelineId,
                                    name: this.pipelineId
                                })
                            },
                            text: this.$t('applyPermission')
                        }
                    ]
                }
            }
        },
        computed: {
            ...mapState([
                'fetchError'
            ]),
            ...mapState('atom', [
                'pipeline',
                'pipelineWithoutTrigger',
                'pipelineYaml',
                'pipelineSetting',
                'editfromImport'
            ]),
            ...mapGetters({
                isCodeMode: 'isCodeMode'
            }),
            projectId () {
                return this.$route.params.projectId
            },
            pipelineId () {
                return this.$route.params.pipelineId
            },
            currentTab () {
                return this.$route.params.tab || 'pipeline'
            },
            curPanel () {
                return this.panels.find(panel => panel.name === this.currentTab)
            },
            isDraftEdit () {
                return this.$route.name === 'pipelineImportEdit'
            },
            panels () {
                return [{
                            name: 'pipeline',
                            label: this.$t('pipeline'),
                            component: 'PipelineEditTab',
                            bindData: {
                                isEditing: this.isEditing,
                                pipeline: this.pipelineWithoutTrigger,
                                isLoading: !this.pipeline
                            }
                        },
                        {
                            name: 'trigger',
                            label: this.$t('settings.trigger'),
                            component: 'triggerTab',
                            bindData: {
                                isEditing: this.isEditing,
                                pipeline: this.pipeline,
                                isLoading: !this.pipeline
                            }
                        },
                        {
                            name: 'notify',
                            label: this.$t('settings.notify'),
                            component: 'NotifyTab',
                            bindData: {
                                failSubscriptionList: this.pipelineSetting?.failSubscriptionList ?? null,
                                successSubscriptionList: this.pipelineSetting?.successSubscriptionList ?? null,
                                updateSubscription: (name, value) => {
                                    this.setPipelineEditing(true)
                                    console.log(name, value)
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
                            name: 'baseSetting',
                            label: this.$t('editPage.baseSetting'),
                            component: 'BaseSettingTab',
                            bindData: {
                                pipelineSetting: this.pipelineSetting,
                                updatePipelineSetting: (...args) => {
                                    this.setPipelineEditing(true)
                                    this.updatePipelineSetting(...args)
                                }
                            }
                        }]
            }
        },
        watch: {
            pipelineId (pipelineId, oldId) {
                this.$nextTick(() => {
                    this.init()
                })
            },
            pipelineVersion (v) {
                this.$nextTick(() => {
                    this.init()
                })
            },
            pipeline (val) {
                this.getInterceptAtom()
                if (val && val.instanceFromTemplate) this.requestMatchTemplateRules(val.templateId)
            },
            fetchError (error) {
                if (error.code === 403) {
                    this.hasNoPermission = true
                    this.removeLeaveListenr()
                }
            },
            isCodeMode (val) {
                const pipeline = Object.assign({}, this.pipeline, {
                    stages: [
                        this.pipeline.stages[0],
                        ...(this.pipelineWithoutTrigger?.stages ?? [])
                    ]
                })
                this.transfertModelToYaml({
                    projectId: this.$route.params.projectId,
                    pipelineId: this.$route.params.pipelineId,
                    actionType: val ? 'FULL_MODEL2YAML' : 'FULL_YAML2MODEL',
                    modelAndSetting: {
                        model: pipeline,
                        setting: this.pipelineSetting
                    },
                    oldYaml: this.pipelineYaml
                })
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
            this.removeLeaveListenr()
            this.setPipelineEditing(false)
            this.setSaveStatus(false)
            this.setEditFrom(false)
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
                'transfertModelToYaml'
            ]),
            ...mapActions('common', [
                'requestQualityAtom',
                'requestInterceptAtom',
                'requestMatchTemplateRuleList'
            ]),
            async init () {
                if (!this.isDraftEdit && this.pipelineVersion) {
                    this.isLoading = true
                    await this.requestPipeline({
                        ...this.$route.params,
                        version: this.pipelineVersion
                    })
                    this.isLoading = false
                }
            },
            switchTab (tab) {
                this.$router.push({
                    params: {
                        tab: tab.name
                    }
                })
            },
            leaveConfirm (to, from, next) {
                if (!this.leaving) {
                    if (this.isEditing) {
                        this.leaving = true
                        navConfirm({ content: this.confirmMsg, type: 'warning' })
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
                }
            },
            addLeaveListenr () {
                window.addEventListener('beforeunload', this.leaveSure)
            },
            removeLeaveListenr () {
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
        .ui-inner-label, .bk-label {
            font-size: 12px;
            line-height: 20px;
            min-height: 20px;
            margin-bottom: 4px;
        }
        .ui-inner-label {
            display: flex;
            align-items: center;
        }
        .bk-form-tip, .bk-form-help {
            margin-bottom: 0;
            line-height: 18px;
        }
        i.icon-question-circle-shape {
            color: #979BA5;
            font-size: 14px;
        }
    }
    .new-ui-form.bk-form-vertical .bk-form-item+.bk-form-item {
        margin-top: 24px;
    }
    .bkdevops-pipeline-edit-wrapper {
        /* display: flex; */
        .choose-type-switcher {
            background: #f0f1f5;
            height: 42px;
            flex-shrink: 0;
            display: flex;
            align-items: center;
            margin: 24px 24px 0;
            /* margin: 0 24px;
            position: sticky;
            top: 56px;
            z-index: 8; */

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
                    content: '';
                    width: 1px;
                    height: 16px;
                    background-color: #C4C6CC;
                    top: 13px;
                    left: 0;
                }

                &:last-child::after {
                    position: absolute;
                    content: '';
                    width: 1px;
                    height: 16px;
                    background-color: #C4C6CC;
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
                &.active + ::before {
                    display: none;
                }
            }
        }

        .edit-content-area {
            position: relative;
            background: white;
            /* height: calc(100vh - 192px); */
            height: calc(100vh - 140px);
            margin: 0 24px;
            padding: 24px 24px 40px;
            overflow-y: auto;
            flex: 1;
            box-shadow: 0 2px 2px 0 #00000026;
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

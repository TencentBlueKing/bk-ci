<template>
    <section v-bkloading="{ isLoading }" class="bkdevops-pipeline-edit-wrapper">
        <empty-tips
            v-if="hasNoPermission"
            :show-lock="true"
            :title="noPermissionTipsConfig.title"
            :desc="noPermissionTipsConfig.desc"
            :btns="noPermissionTipsConfig.btns">
        </empty-tips>
        <template v-else>
            <bk-tab :active="currentTab" @tab-change="switchTab" class="bkdevops-pipeline-tab-card bkdevops-pipeline-edit-tab" type="unborder-card">
                <bk-tab-panel
                    v-for="panel in panels"
                    v-bind="{ name: panel.name, label: panel.label }"
                    render-directive="if"
                    :key="panel.name"
                >
                    <component :is="panel.component" v-bind="panel.bindData" @hideColumnPopup="toggleColumnsSelectPopup(false)"></component>
                </bk-tab-panel>
            </bk-tab>
            <mini-map :stages="pipeline.stages" scroll-class=".bk-tab-section .bk-tab-content" v-if="!isLoading && pipeline && currentTab === 'pipeline'"></mini-map>
        </template>
    </section>
</template>

<script>
    import { mapActions, mapState } from 'vuex'
    import emptyTips from '@/components/devops/emptyTips'
    import MiniMap from '@/components/MiniMap'
    import { navConfirm } from '@/utils/util'
    import { PipelineEditTab, BaseSettingTab, NotifyTab } from '@/components/PipelineEditTabs/'
    import pipelineOperateMixin from '@/mixins/pipeline-operate-mixin'

    export default {
        components: {
            emptyTips,
            PipelineEditTab,
            BaseSettingTab,
            NotifyTab,
            MiniMap
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
                'editfromImport'
            ]),
            projectId () {
                return this.$route.params.projectId
            },
            pipelineId () {
                return this.$route.params.pipelineId
            },
            currentTab () {
                return this.$route.params.tab || 'pipeline'
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
                                pipeline: this.pipeline,
                                isLoading: !this.pipeline
                            }
                        },
                        {
                            name: 'notify',
                            label: this.$t('settings.notify'),
                            component: 'NotifyTab',
                            bindData: {
                                failSubscription: this.pipelineSetting ? this.pipelineSetting.failSubscription : null,
                                successSubscription: this.pipelineSetting ? this.pipelineSetting.successSubscription : null,
                                updateSubscription: (container, name, value) => {
                                    this.setPipelineEditing(true)
                                    this.updatePipelineSetting({
                                        container,
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
            '$route.params.pipelineId': function (pipelineId, oldId) {
                this.init()
            },
            pipeline (val) {
                this.isLoading = false
                this.requestInterceptAtom()
                if (val && val.instanceFromTemplate) this.requestMatchTemplateRules(val.templateId)
            },
            fetchError (error) {
                if (error.code === 403) {
                    this.hasNoPermission = true
                    this.removeLeaveListenr()
                }
            }
        },
        mounted () {
            if (!this.editfromImport) {
                this.init()
                this.requestQualityAtom()
            }
            this.addLeaveListenr()
        },
        beforeDestroy () {
            this.setPipeline(null)
            this.resetPipelineSetting()
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
                'setPipeline',
                'setPipelineEditing',
                'setSaveStatus',
                'setEditFrom'
            ]),
            ...mapActions('pipelines', [
                'requestPipelineSetting',
                'updatePipelineSetting',
                'resetPipelineSetting'
            ]),
            ...mapActions('common', [
                'requestQualityAtom',
                'requestInterceptAtom'
            ]),
            init () {
                if (!this.isDraftEdit) {
                    this.isLoading = true
                    this.requestPipeline(this.$route.params)
                    this.requestPipelineSetting(this.$route.params)
                }
            },
            switchTab (tab) {
                this.$router.push({
                    params: {
                        tab
                    }
                })
            },
            leaveConfirm (to, from, next) {
                if (!this.leaving) {
                    if (this.isEditing) {
                        this.leaving = true
                        navConfirm({ content: this.confirmMsg, type: 'warning' })
                            .then(() => {
                                next(true)
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
            requestQualityAtom () {
                this.$store.dispatch('common/requestQualityAtom', {
                    projectId: this.projectId
                })
            },
            requestInterceptAtom () {
                if (this.projectId && this.pipelineId) {
                    this.$store.dispatch('common/requestInterceptAtom', {
                        projectId: this.projectId,
                        pipelineId: this.pipelineId
                    })
                }
            },
            requestMatchTemplateRules (templateId) {
                this.$store.dispatch('common/requestMatchTemplateRuleList', {
                    projectId: this.projectId,
                    templateId
                })
            }
        }
    }
</script>

<style lang="scss">
    .bkdevops-pipeline-edit-wrapper {
        display: flex;
        .bk-tab-header {
            padding: 7px 25px 0;
            box-sizing: content-box;
        }
        .bk-tab-section {
            padding: 0 25px 20px;
        }
        .scroll-container {
            margin-top: -20px;
            margin-left: -30px;
            width: fit-content;
            overflow: visible;
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

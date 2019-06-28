<template>
    <section v-bkloading="{ isLoading }" class="bkdevops-pipeline-edit-wrapper">
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
        <empty-tips
            v-if="hasNoPermission"
            :title="noPermissionTipsConfig.title"
            :desc="noPermissionTipsConfig.desc"
            :btns="noPermissionTipsConfig.btns">
        </empty-tips>
    </section>
</template>

<script>
    import { mapActions, mapState } from 'vuex'
    import emptyTips from '@/components/devops/emptyTips'
    import { navConfirm } from '@/utils/util'
    import { CONFIRM_MSG, CONFIRM_TITLE } from '@/utils/pipelineConst'
    import { PipelineEditTab, BaseSettingTab } from '@/components/PipelineEditTabs/'
    import pipelineOperateMixin from '@/mixins/pipeline-operate-mixin'

    export default {
        components: {
            emptyTips,
            PipelineEditTab,
            BaseSettingTab
        },
        mixins: [pipelineOperateMixin],
        data () {
            return {
                isLoading: true,
                hasNoPermission: false,
                noPermissionTipsConfig: {
                    title: '没有权限',
                    desc: '你没有查看该流水线的权限，请切换项目或申请相应权限',
                    btns: [
                        {
                            theme: 'primary',
                            size: 'normal',
                            handler: this.changeProject,
                            text: '切换项目'
                        },
                        {
                            theme: 'success',
                            size: 'normal',
                            handler: () => {
                                this.goToApplyPerm('role_viewer')
                            },
                            text: '申请权限'
                        }
                    ]
                }
            }
        },
        computed: {
            ...mapState([
                'fetchError'
            ]),
            currentTab () {
                return this.$route.params.tab || 'pipeline'
            },
            panels () {
                return [{
                            name: 'pipeline',
                            label: '流水线',
                            component: 'PipelineEditTab',
                            bindData: {
                                isEditing: this.isEditing,
                                pipeline: this.pipeline,
                                isLoading: !this.pipeline
                            }
                        },
                        {
                            name: 'baseSetting',
                            label: '基础设置',
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
            pipeline () {
                this.isLoading = false
            },
            fetchError (error) {
                if (error.code === 403) {
                    this.hasNoPermission = true
                    this.removeLeaveListenr()
                }
            }
        },
        mounted () {
            this.init()
            this.addLeaveListenr()
        },
        beforeDestroy () {
            this.setPipeline()
            this.removeLeaveListenr()
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
                'setPipelineEditing'
            ]),
            ...mapActions('pipelines', [
                'requestPipelineSetting',
                'updatePipelineSetting'
            ]),
            init () {
                this.requestPipeline(this.$route.params)
                this.requestPipelineSetting(this.$route.params)
            },
            switchTab (tab) {
                this.$router.push({
                    params: {
                        tab
                    }
                })
            },
            leaveConfirm (to, from, next) {
                if (this.isEditing) {
                    navConfirm({ content: CONFIRM_MSG, title: CONFIRM_TITLE })
                        .then(() => next())
                        .catch(() => next(false))
                } else {
                    next(true)
                }
            },
            addLeaveListenr () {
                window.addEventListener('beforeunload', this.leaveSure)
            },
            removeLeaveListenr () {
                window.removeEventListener('beforeunload', this.leaveSure)
            },
            leaveSure (e) {
                e.returnValue = CONFIRM_MSG
                return CONFIRM_MSG
            }
        }
    }
</script>

<style lang="scss">
    .bkdevops-pipeline-edit-wrapper {
        padding: 7px 25px 20px 25px;
        display: flex;
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
            .bk-tab-section {
                overflow: auto;
            }
        }
    }
</style>

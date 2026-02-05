<template>
    <section
        class="template-edit"
        v-bkloading="{ isLoading }"
    >
        <template>
            <header class="template-edit-header">
                <TemplateBreadCrumb
                    :template-name="pipeline?.name"
                    :is-loading="!pipeline"
                >
                    <span class="template-edit-header-tag">
                        <bk-tag>
                            <span
                                v-bk-overflow-tips
                                class="edit-header-draft-tag"
                            >
                                {{ currentVersionName }}
                            </span>
                        </bk-tag>
                    </span>
                </TemplateBreadCrumb>
                <mode-switch
                    :save="saveTemplateDraft"
                />
                <aside class="template-edit-right-aside">
                    <DraftManager
                        ref="draftManager"
                        v-model="isConflictDraft"
                        :laster-draft-info="lasterDraftInfo"
                        :release-version="releaseVersion"
                        :project-id="projectId"
                        :unique-id="templateId"
                        :is-template="true"
                        @rollback="handleRollback"
                        @continue-save-draft="continueSaveDraft"
                        @go-pipeline-model="goPipelineModel"
                    />
                    <bk-button
                        theme="primary"
                        @click="saveTemplateDraft"
                        :disabled="saveStatus || !isEditing"
                        outline
                    >
                        {{ $t('saveDraft') }}
                    </bk-button>
                    <PipelineEditMoreAction
                        is-template
                        :project-id="projectId"
                        :unique-id="templateId"
                    />
                    <ReleaseButton
                        :can-release="canRelease && !isEditing"
                        :project-id="projectId"
                        :id="templateId"
                    />
                </aside>
            </header>
            <Edit
                v-if="pipeline"
                class="template-edit-wrapper"
            />
        </template>
    </section>
</template>

<script>
    import ModeSwitch from '@/components/ModeSwitch'
    import PipelineEditMoreAction from '@/components/PipelineEditMoreAction'
    import ReleaseButton from '@/components/PipelineHeader/ReleaseButton.vue'
    import TemplateBreadCrumb from '@/components/Template/TemplateBreadCrumb'
    import {
        TEMPLATE_RESOURCE_ACTION
    } from '@/utils/permission'
    import {
        convertMStoStringByRule,
        convertTime,
        showPipelineCheckMsg
    } from '@/utils/util'
    import Edit from '@/views/subpages/edit'
    import { mapActions, mapGetters, mapState } from 'vuex'
    import DraftManager from '@/components/DraftManager'

    export default {
        components: {
            TemplateBreadCrumb,
            ReleaseButton,
            ModeSwitch,
            Edit,
            DraftManager,
            PipelineEditMoreAction
        },
        data () {
            return {
                isLoading: true,
                confirmMsg: this.$t('editPage.confirmMsg'),
                lasterDraftInfo: null,
                isConflictDraft: false,
            }
        },
        computed: {
            ...mapGetters('atom', [
                'checkPipelineInvalid',
                'isEditing',
                'getDraftBaseVersionName'
            ]),
            ...mapGetters({
                isCodeMode: 'isCodeMode'
            }),
            ...mapState('atom', [
                'saveStatus',
                'pipeline',
                'pipelineInfo',
                'pipelineWithoutTrigger',
                'pipelineSetting',
                'pipelineYaml'
            ]),
            ...mapState([
                'fetchError',
                'pipelineMode'
            ]),

            canRelease () {
                return (this.pipelineInfo?.canRelease ?? false) && !this.saveStatus
            },
            projectId () {
                return this.$route.params.projectId
            },
            templateId () {
                return this.$route.params.templateId
            },
            releaseVersion () {
                return this.pipelineInfo?.releaseVersion ?? ''
            },
            currentVersionId () {
                return this.$route.params?.version ?? this.pipelineInfo?.version
            },
            TEMPLATE_RESOURCE_ACTION () {
                return TEMPLATE_RESOURCE_ACTION
            },
            versionName () {
                return this.pipelineInfo?.versionName ?? '--'
            },
            currentVersionName () {
                if (this.pipelineInfo?.canDebug) {
                    return this.$t('editPage.draftVersion', [this.getDraftBaseVersionName])
                }
                return this.versionName
            }
        },
        watch: {
            fetchError (error) {
                if (error.code === 403) {
                    this.isLoading = false
                    this.removeLeaveListenr()
                }
            },
            pipeline: {
                deep: true,
                handler (newVal, oldVal) {
                    this.isLoading = false
                }
            }
        },
        created () {
            this.requestTemplateByVersion()
        },
        mounted () {
            this.requestQualityAtom()
            this.requestMatchTemplateRules()
        },
        beforeDestroy () {
            this.setPipeline(null)
            this.setPipelineEditing(false)
            this.setAtomEditing(false)
            this.removeLeaveListenr()
            this.errors.clear()
        },
        methods: {
            // TODO: 优化
            ...mapActions('atom', [
                'setPipeline',
                'saveDraftTemplate',
                'setPipelineEditing',
                'setAtomEditing',
                'requestTemplateSummary',
                'requestPipeline',
                'transfer',
                'fetchTemplateByVersion',
                'updateContainer'
            ]),
            ...mapActions({
                rollbackTemplateVersion: 'templates/rollbackTemplateVersion',
                getTemplateDraftStatus: 'common/getTemplateDraftStatus'
            }),
            requestTemplateByVersion (version = this.currentVersionId) {
                try {
                    this.requestPipeline({
                        projectId: this.projectId,
                        templateId: this.templateId,
                        version
                    })
                } catch (err) {
                    this.$showTips({
                        theme: 'error',
                        message: err.message || err
                    })
                }
            },
            // 构建 model 对象
            buildModel () {
                return Object.assign({}, this.pipeline, {
                    name: this.pipelineSetting.pipelineName,
                    stages: [
                        this.pipeline.stages[0],
                        ...this.pipelineWithoutTrigger.stages
                    ]
                })
            },
            async handleRollback (item) {
                this.$bkInfo({
                    maskClose: false,
                    confirmLoading: true,
                    title: this.$t('confirmRollbackToThisHistory'),
                    subTitle: this.$t('historyRollback', [item.updater, convertTime(item.updateTime)]),
                    confirmFn: async () => {
                        try {
                            const res = await this.rollbackTemplateVersion({
                                projectId: this.projectId,
                                templateId: this.templateId,
                                version: item.version,
                                draftVersion: item.draftVersion
                            })
                            
                            if (res?.version) {
                                // 重新获取模板摘要信息
                                await this.requestTemplateSummary(this.$route.params)
                                // 刷新草稿列表（通过子组件）
                                if (this.$refs.draftManager) {
                                    await this.$refs.draftManager.refresh()
                                }
                                // 获取回滚后的模板完整数据并更新到 store
                                await this.requestPipeline({
                                    projectId: this.projectId,
                                    templateId: this.templateId,
                                    source: 'EDIT',
                                    version: res.version
                                })
                                
                                // 跳转到编辑页面
                                this.$router.replace({
                                    name: 'templateEdit',
                                    params: {
                                        ...this.$route.params,
                                        version: res.version
                                    },
                                })
                                return true
                            }
                        } catch (error) {
                            this.$bkMessage({
                                theme: 'error',
                                message: error.message ?? error
                            })
                            return false
                        }
                    }
                })
            },
            goPipelineModel () {
                this.isConflictDraft = false
                this.$router.push({
                    name: 'TemplateOverview',
                    params: {
                        ...this.$route.params,
                        version: this.pipelineInfo?.releaseVersion,
                        type: 'pipeline'
                    },
                    query: this.$route.query
                })
            },
            // 处理保存草稿时的错误
            handleSaveDraftError (e) {
                if (e.code === 2101244) {
                    showPipelineCheckMsg(this.$bkMessage, e.message, this.$createElement)
                } else {
                    this.$showTips({
                        message: e.message || e,
                        theme: 'error'
                    })
                }
            },
            // 执行草稿保存的核心逻辑
            async executeSaveDraft () {
                const pipeline = this.buildModel()
                const { inValid, message } = this.checkPipelineInvalid(pipeline.stages, this.pipelineSetting)
             
                if (inValid) {
                    this.$showTips({
                        theme: 'error',
                        message
                    })
                    return
                }

                // 清除流水线参数渲染过程中添加的key
                this.formatParams(pipeline)
                let result
                try {
                    this.saveStatus = true

                    const { data } = await this.saveDraftTemplate({
                        projectId: this.projectId,
                        templateId: this.templateId,
                        storageType: this.pipelineMode,
                        ...(this.isCodeMode
                            ? {
                                yaml: this.pipelineYaml
                            }
                            : {
                                model: pipeline,
                                templateSetting: this.pipelineSetting
                            }),
                        baseVersion: this.pipelineInfo?.baseVersion,
                        baseDraftVersion: this.pipelineInfo?.draftVersion,
                        type: this.pipelineInfo?.type
                    })
                    if (data) {
                        this.$showTips({
                            message: `${this.pipeline.name}${' '}${this.$t('updateSuc')}`,
                            theme: 'success'
                        })
                        this.setPipelineEditing(false)

                        await this.requestTemplateSummary(this.$route.params)

                        this.$router.replace({
                            ...this.$route,
                            params: {
                                ...this.$route.params,
                                version: data.version
                            }
                        })
                        // 刷新草稿列表（通过子组件）
                        if (this.$refs.draftManager) {
                            await this.$refs.draftManager.refresh()
                        }
                        result = true
                    } else {
                        this.$showTips({
                            message: `${this.pipeline.name} ${this.$t('updateFail')}`,
                            theme: 'error'
                        })
                    }
                } catch (err) {
                    this.handleSaveDraftError(err)

                    result = false
                } finally {
                    this.saveStatus = false
                }
                return result
            },

            async continueSaveDraft () {
                try {
                    return await this.executeSaveDraft()
                } catch (e) {
                    this.handleSaveDraftError(e)
                    return false
                } finally {
                    this.saveStatus = false
                    this.isConflictDraft = false
                }
            },

            async saveTemplateDraft () {
                const draftStatus = await this.getTemplateDraftStatus({
                    projectId: this.projectId,
                    templateId: this.templateId,
                    ...(this.pipelineInfo?.draftVersion ? {
                        version: this.pipelineInfo?.version,
                        baseDraftVersion: this.pipelineInfo?.draftVersion,
                    } : {}),
                    actionType: 'SAVE'
                })
                this.lasterDraftInfo = draftStatus
                if (this.lasterDraftInfo.status === 'NORMAL') {
                    return await this.executeSaveDraft()
                } else if (this.lasterDraftInfo.status === 'CONFLICT' || this.lasterDraftInfo.status === 'PUBLISHED') {
                    this.isConflictDraft = true
                    return false
                }
            },

            requestQualityAtom () {
                this.$store.dispatch('common/requestQualityAtom', {
                    projectId: this.projectId
                })
            },
            requestMatchTemplateRules () {
                this.$store.dispatch('common/requestMatchTemplateRuleList', {
                    projectId: this.projectId,
                    templateId: this.templateId
                })
            },
            exit () {
                this.$router.push({
                    name: 'pipelinesTemplate'
                })
            },
            formatParams (pipeline) {
                const params = pipeline && pipeline.stages[0].containers[0].params
                const paramList = params && this.getParams(params)
                this.updateContainer({
                    container: pipeline.stages[0].containers[0],
                    newParam: {
                        params: paramList
                    }
                })
            },
            getParams (params) {
                const result = params.map(param => {
                    const { paramIdKey, ...temp } = param
                    return temp
                })
                return result
            },

            localConvertMStoString (num) {
                return convertMStoStringByRule(new Date().getTime() - num)
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
            }
        }
    }
</script>

<style lang="scss">
    @import './../../scss/conf';
    @import '@/scss/mixins/ellipsis';

    .template-edit {
        .template-edit-header {
            width: 100%;
            height: 48px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            align-self: stretch;
            background-color: white;
            box-shadow: 0 2px 5px 0 #333c4808;
            border-bottom: 1px solid $borderLightColor;
            padding: 0 0 0 24px;
            .template-edit-header-tag {
                display: flex;
                align-items: center;
                grid-gap: 8px;
                line-height: 1;
                .bk-tag {
                    margin: 0;
                    max-width: 222px;
                    .edit-header-draft-tag {
                        @include ellipsis();
                        width: 100%;
                    }
                }
            }
            .template-edit-right-aside {
                height: 100%;
                display: flex;
                justify-self: flex-end;
                align-items: center;
                grid-gap: 10px;
            }
        }
        .template-edit-wrapper {
            overflow: hidden;
            height: calc(100% - 48px);
            background-color: #F5F7FA;
        }
    }

</style>

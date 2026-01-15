<template>
    <div class="pipeline-edit-header">
        <pipeline-bread-crumb
            :is-loading="!isPipelineNameReady"
        >
            <span class="pipeline-edit-header-tag">
                <bk-tag>
                    <span
                        v-bk-overflow-tips
                        class="edit-header-draft-tag"
                    >
                        {{ currentVersionName }}
                    </span>
                </bk-tag>
            </span>
        </pipeline-bread-crumb>
        <mode-switch
            :save="saveDraft"
        />
        <aside class="pipeline-edit-right-aside">
            <DraftManager
                v-model="isConflictDraft"
                :draft-list="draftList"
                :laster-draft-info="lasterDraftInfo"
                :current-edit-yaml="currentEditYaml"
                :draft-yaml="draftYaml"
                @diff="handleDiff"
                @rollback="handleRollback"
                @continue-save-draft="continueSaveDraft"
                @go-pipeline-model="goPipelineModel"
            />
            <bk-button
                :disabled="saveStatus"
                :loading="saveStatus"
                @click="goBack"
            >
                {{ $t("cancel") }}
            </bk-button>

            <bk-button
                :disabled="saveStatus || !isEditing"
                :loading="saveStatus"
                outline
                theme="primary"
                @click="saveDraft"
                v-perm="{
                    hasPermission: canEdit,
                    disablePermissionApi: true,
                    permissionData: {
                        projectId,
                        resourceType: RESOURCE_TYPE.PIPELINE,
                        resourceCode: pipelineId,
                        action: RESOURCE_ACTION.EDIT
                    }
                }"
            >
                <span
                    v-bk-tooltips="{
                        content: $t('noChange'),
                        arrow: true,
                        disabled: !(saveStatus || !isEditing)
                    }"
                >{{ $t("saveDraft") }}</span>
            </bk-button>
            <bk-button
                :disabled="!canDebug"
                :loading="executeStatus"
                v-perm="{
                    hasPermission: canExecute,
                    disablePermissionApi: true,
                    permissionData: {
                        projectId,
                        resourceType: RESOURCE_TYPE.PIPELINE,
                        resourceCode: pipelineId,
                        action: RESOURCE_ACTION.EXECUTE
                    }
                }"
                @click="exec(true)"
            >
                {{ $t("debug") }}
            </bk-button>
            <PipelineEditMoreAction
                :can-debug="canDebug"
                :project-id="projectId"
                :unique-id="pipelineId"
            />

            <!-- <more-actions /> -->
            <release-button
                :can-release="canRelease && !isEditing"
                :project-id="projectId"
                :id="pipelineId"
            />
        </aside>
    </div>
</template>

<script>
    import ModeSwitch from '@/components/ModeSwitch'
    import PipelineEditMoreAction from '@/components/PipelineEditMoreAction.vue'
    import {
        RESOURCE_ACTION,
        RESOURCE_TYPE
    } from '@/utils/permission'
    import { UI_MODE } from '@/utils/pipelineConst'
    import { showPipelineCheckMsg, convertTime } from '@/utils/util'
    import { mapActions, mapGetters, mapState } from 'vuex'
    import PipelineBreadCrumb from './PipelineBreadCrumb.vue'
    import ReleaseButton from './ReleaseButton'
    import DraftManager from '@/components/DraftManager'

    export default {
        components: {
            PipelineBreadCrumb,
            ReleaseButton,
            ModeSwitch,
            DraftManager,
            PipelineEditMoreAction
        },
        props: {
            isSwitchPipeline: Boolean
        },
        data () {
            return {
                draftList: [],
                lasterDraftInfo: null,
                isConflictDraft: false,
                currentEditYaml: '', // 当前编辑的 YAML 内容
                draftYaml: '' // 选中的草稿的 YAML 内容
            }
        },
        computed: {
            ...mapState([
                'pipelineMode'
            ]),
            ...mapState('atom', [
                'pipeline',
                'saveStatus',
                'pipelineWithoutTrigger',
                'pipelineSetting',
                'pipelineYaml',
                'pipelineInfo'
            ]),
            ...mapState('pipelines', ['executeStatus', 'isManage']),
            ...mapGetters({
                isCurPipelineLocked: 'atom/isCurPipelineLocked',
                isEditing: 'atom/isEditing',
                checkPipelineInvalid: 'atom/checkPipelineInvalid',
                draftBaseVersionName: 'atom/getDraftBaseVersionName'
            }),
            projectId () {
                return this.$route.params.projectId
            },
            pipelineId () {
                return this.$route.params.pipelineId
            },
            canEdit () {
                return this.pipelineInfo?.permissions?.canEdit ?? true
            },
            canExecute () {
                return this.pipelineInfo?.permissions?.canExecute ?? true
            },
            canDebug () {
                return (this.pipelineInfo?.canDebug ?? false) && !this.saveStatus && !this.isCurPipelineLocked
            },
            RESOURCE_ACTION () {
                return RESOURCE_ACTION
            },
            RESOURCE_TYPE () {
                return RESOURCE_TYPE
            },
            btnDisabled () {
                return this.saveStatus || this.executeStatus
            },
            canRelease () {
                return (this.pipelineInfo?.canRelease ?? false) && !this.saveStatus
            },
            isTemplatePipeline () {
                return this.pipelineInfo?.instanceFromTemplate ?? false
            },
            versionName () {
                return this.pipelineInfo?.versionName ?? '--'
            },
            currentVersionName () {
                if (this.pipelineInfo?.canDebug) {
                    return this.$t('editPage.draftVersion', [this.draftBaseVersionName])
                }
                return this.versionName
            },
            currentVersion () {
                return this.pipelineInfo?.version ?? ''
            },
            isPipelineNameReady () {
                return this.pipelineSetting?.pipelineId === this.$route.params.pipelineId
            }
        },
        watch: {
            isTemplatePipeline: {
                handler (val) {
                    if (val) {
                        this.updatePipelineMode(UI_MODE)
                    }
                },
                immediate: true
            }
        },
        mounted () {
            this.getDraftList()
        },
        methods: {
            ...mapActions({
                getDraftVersion: 'common/getDraftVersion',
                getDraftStatus: 'common/getDraftStatus',
                updatePipelineMode: 'updatePipelineMode'
            }),
            ...mapActions('atom', [
                'setPipelineEditing',
                'saveDraftPipeline',
                'setSaveStatus',
                'requestPipelineSummary',
                'transfer',
                'fetchPipelineByVersion',
                'updateContainer'
            ]),
            async getDraftList () {
                try {
                    const res = await this.getDraftVersion({
                        projectId: this.projectId,
                        pipelineId: this.pipelineId,
                        version: this.pipelineInfo?.version
                    })
                    this.draftList = res
                    this.lasterDraftInfo = this.draftList?.[0]
                } catch (error) {
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message ?? error
                    })
                }
            },
            // 构建 modelAndSetting 对象
            buildModelAndSetting () {
                const pipeline = Object.assign({}, this.pipeline, {
                    stages: [
                        this.pipeline.stages[0],
                        ...this.pipelineWithoutTrigger.stages
                    ]
                })
                
                return {
                    model: {
                        ...pipeline,
                        name: this.pipelineSetting.pipelineName,
                        desc: this.pipelineSetting.desc
                    },
                    setting: Object.assign(this.pipelineSetting, {
                        failSubscription: undefined,
                        successSubscription: undefined
                    })
                }
            },
            async handleDiff (draftVersion) {
                try {
                    const modelAndSetting = this.buildModelAndSetting()
                    const [newDraftInfo, draftInfo] = await Promise.all([
                        // 获取当前编辑的yaml数据
                        this.transfer({
                            projectId: this.projectId,
                            pipelineId: this.pipelineId,
                            actionType: 'FULL_MODEL2YAML',
                            modelAndSetting
                        }),
                        // 获取选中草稿的yaml数据
                        this.fetchPipelineByVersion({
                            projectId: this.projectId,
                            pipelineId: this.pipelineId,
                            version: this.pipelineInfo?.version,
                            draftVersion
                        })
                    ])
                    
                    this.currentEditYaml = newDraftInfo.newYaml
                    this.draftYaml = draftInfo?.yamlPreview?.yaml
                } catch (error) {
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message ?? error
                    })
                }
            },
            handleRollback (item) {
                this.$bkInfo({
                    maskClose: false,
                    title: this.$t('confirmRollbackToThisHistory'),
                    subTitle: this.$t('historyRollback', [item.updater, item.updateTime]),
                    confirmFn: () => {
                        // TODO 调用回滚接口
                    }
                })
            },
            goPipelineModel () {
                this.$router.push({
                    name: 'pipelinesHistory',
                    params: {
                        ...this.$route.params,
                        version: this.pipelineInfo?.releaseVersion,
                        type: 'pipeline'
                    },
                    query: this.$route.query
                })
            },
            async exec (debug) {
                if (debug && this.isEditing) {
                    const result = await this.saveDraft()
                    if (!result) {
                        return
                    }
                }
                this.$router.push({
                    name: 'executePreview',
                    query: {
                        ...(debug ? { debug: '' } : {})
                    },
                    params: {
                        ...this.$route.params,
                        version: this.pipelineInfo?.[debug ? 'version' : 'releaseVersion']
                    }
                })
            },
            formatParams (pipeline) {
                const params = pipeline.stages[0].containers[0].params
                const paramList
                    = params
                        && params.map((param) => {
                            const { paramIdKey, ...temp } = param
                            return temp
                        })
                this.updateContainer({
                    container: this.pipeline.stages[0].containers[0],
                    newParam: {
                        params: paramList
                    }
                })
            },
            // 执行草稿保存的核心逻辑
            async executeSaveDraft () {
                this.setSaveStatus(true)
                const pipeline = Object.assign({}, this.pipeline, {
                    stages: [
                        this.pipeline.stages[0],
                        ...this.pipelineWithoutTrigger.stages
                    ]
                })
                const { projectId, pipelineId, pipelineSetting, checkPipelineInvalid, pipelineYaml } = this
                const { inValid, message } = checkPipelineInvalid(pipeline.stages, pipelineSetting)
                if (inValid) {
                    throw new Error(message)
                }
                const modelAndSetting = this.buildModelAndSetting()
                // 清除流水线参数渲染过程中添加的key
                this.formatParams(pipeline)

                // 请求执行构建
                const { version } = await this.saveDraftPipeline({
                    projectId,
                    pipelineId,
                    baseVersion: this.pipelineInfo?.baseVersion,
                    baseDraftVersion: this.pipelineInfo?.draftVersion,
                    storageType: this.pipelineMode,
                    modelAndSetting,
                    yaml: pipelineYaml
                })
                this.setPipelineEditing(false)

                await this.requestPipelineSummary(this.$route.params)
                this.$router.replace({
                    params: {
                        ...this.$route.params,
                        version
                    }
                })
                await this.getDraftList()
                this.$bkMessage({
                    theme: 'success',
                    message: this.$t('editPage.saveDraftSuccess', [pipelineSetting.pipelineName]),
                    limit: 1
                })
                return true
            },

            async continueSaveDraft () {
                try {
                    return await this.executeSaveDraft()
                } catch (e) {
                    this.handleSaveDraftError(e)
                    return false
                } finally {
                    this.setSaveStatus(false)
                }
            },

            async saveDraft () {
                try {
                    const draftStatus = await this.getDraftStatus({
                        projectId: this.projectId,
                        pipelineId: this.uniqueId,
                        actionType: 'SAVE'
                    })
                    this.lasterDraftInfo = draftStatus
                    if (this.lasterDraftInfo.status === 'NORMAL') {
                        return await this.executeSaveDraft()
                    } else if (this.lasterDraftInfo.status === 'CONFLICT' || this.lasterDraftInfo.status === 'PUBLISHED') {
                        this.isConflictDraft = true
                        return false
                    }
                } catch (e) {
                    this.handleSaveDraftError(e)
                    return false
                } finally {
                    this.setSaveStatus(false)
                }
            },

            // 处理保存草稿时的错误
            handleSaveDraftError (e) {
                const { projectId, pipelineId } = this.$route.params

                if (e.code === 2101244) {
                    showPipelineCheckMsg(this.$bkMessage, e.message, this.$createElement)
                } else {
                    this.handleError(e, {
                        projectId,
                        resourceCode: pipelineId,
                        action: RESOURCE_ACTION.EDIT
                    })
                }
            },
            goBack () {
                this.$router.back()
            }
        }
    }
</script>

<style lang="scss">
@import '@/scss/conf';
@import '@/scss/mixins/ellipsis';
.pipeline-edit-header {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  padding: 0 0 0 14px;
  align-self: stretch;
  .pipeline-edit-header-tag {
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
  .pipeline-edit-right-aside {
    display: grid;
    grid-gap: 10px;
    grid-auto-flow: column;
    height: 100%;
    align-items: center;
    justify-content: center;
  }
}
.pipeline-save-error-list-box {
    display: flex;
    flex-direction: column;
    grid-gap: 10px;
    .pipeline-save-error-list-item {

        > p {
            margin-bottom: 12px;
        }
        .pipeline-save-error-list {
            > li {
                line-height: 26px;
                a {
                    color: $primaryColor;
                    margin-left: 10px;
                    text-align: right;
                }
            }
        }
    }
}
</style>

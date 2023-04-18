<template>
    <div class="pipeline-edit-header">
        <pipeline-bread-crumb>
            <version-sideslider />
        </pipeline-bread-crumb>
        <aside class="pipeline-edit-right-aside">
            <bk-button
                :disabled="saveBtnDisabled"
                :loading="saveStatus"
                theme="primary"
                @click="save"
            >
                {{ $t("save") }}
            </bk-button>
            <bk-button
                theme="primary"
                :disabled="btnDisabled || !canManualStartup"
                :loading="executeStatus"
                :title="canManualStartup ? '' : this.$t('newlist.cannotManual')"
                @click="saveAndExec"
            >
                {{ isSaveAndRun ? $t("subpage.saveAndExec") : $t("exec") }}
            </bk-button>
            <more-actions />
        </aside>
    </div>
</template>

<script>
    import { mapState, mapGetters, mapActions, mapMutations } from 'vuex'
    import PipelineBreadCrumb from './PipelineBreadCrumb.vue'
    import VersionSideslider from '@/components/VersionSideslider'
    import MoreActions from './MoreActions.vue'
    import { PROCESS_API_URL_PREFIX } from '@/store/constants'
    import { HttpError } from '@/utils/util'
    export default {
        components: {
            PipelineBreadCrumb,
            VersionSideslider,
            MoreActions
        },
        computed: {
            ...mapState('atom', ['pipeline', 'executeStatus', 'saveStatus']),
            ...mapState('pipelines', ['pipelineSetting']),
            ...mapGetters({
                curPipeline: 'pipelines/getCurPipeline',
                isEditing: 'atom/isEditing',
                checkPipelineInvalid: 'atom/checkPipelineInvalid'
            }),
            btnDisabled () {
                return this.saveStatus || this.executeStatus
            },
            saveBtnDisabled () {
                return (
                    this.saveStatus
                    || this.executeStatus
                    || Object.keys(this.pipelineSetting).length === 0
                )
            },
            isSaveAndRun () {
                return this.isEditing && !this.saveBtnDisabled
            },
            canManualStartup () {
                return this.curPipeline ? this.curPipeline.canManualStartup : false
            },
            pipelineStatus () {
                return this.canManualStartup ? 'ready' : 'disable'
            },
            isTemplatePipeline () {
                return this.curPipeline && this.curPipeline.instanceFromTemplate
            }
        },
        methods: {
            ...mapActions('atom', [
                'setPipelineEditing',
                'setExecuteStatus',
                'setSaveStatus',
                'updateContainer'
            ]),
            ...mapMutations('pipelines', ['updateCurPipelineByKeyValue']),
            async saveAndExec () {
                if (this.isSaveAndRun) {
                    await this.save()
                }
                this.$router.push({
                    name: 'pipelinesPreview'
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
            wechatGroupCompletion (setting) {
                try {
                    let successWechatGroup = setting.successSubscription.wechatGroup
                    let failWechatGroup = setting.failSubscription.wechatGroup
                    if (successWechatGroup && !/\;$/.test(successWechatGroup)) {
                        successWechatGroup = `${successWechatGroup};`
                    }
                    if (failWechatGroup && !/\;$/.test(failWechatGroup)) {
                        failWechatGroup = `${failWechatGroup};`
                    }
                    return {
                        ...setting,
                        successSubscription: {
                            ...setting.successSubscription,
                            wechatGroup: successWechatGroup
                        },
                        failSubscription: {
                            ...setting.failSubscription,
                            wechatGroup: failWechatGroup
                        }
                    }
                } catch (e) {
                    console.warn(e)
                    return setting
                }
            },

            async savePipelineAndSetting () {
                const { pipelineSetting, checkPipelineInvalid, pipeline } = this
                const { inValid, message } = checkPipelineInvalid(pipeline.stages, pipelineSetting)
                const { projectId, pipelineId } = this.$route.params
                if (inValid) {
                    throw new Error(message)
                }
                // 清除流水线参数渲染过程中添加的key
                this.formatParams(pipeline)
                const finalSetting = this.wechatGroupCompletion({
                    ...pipelineSetting,
                    projectId: projectId
                })
                const body = {
                    model: {
                        ...pipeline,
                        name: finalSetting.pipelineName,
                        desc: finalSetting.desc
                    },
                    setting: finalSetting
                }
                if (!pipelineId) {
                    return this.importPipelineAndSetting(body)
                }

                // 请求执行构建
                return this.$ajax.post(
                    `${PROCESS_API_URL_PREFIX}/user/pipelines/${projectId}/${pipelineId}/saveAll`,
                    body
                )
            },
            getPipelineSetting () {
                const { pipelineSetting } = this
                const { projectId } = this.$route.params
                return this.wechatGroupCompletion({
                    ...pipelineSetting,
                    projectId
                })
            },
            saveSetting () {
                const pipelineSetting = this.getPipelineSetting()
                const { projectId, pipelineId } = this.$route.params
                return this.$ajax.post(
                    `/${PROCESS_API_URL_PREFIX}/user/pipelines/${projectId}/${pipelineId}/saveSetting`,
                    pipelineSetting
                )
            },
            async save () {
                const { pipelineId, projectId } = this.$route.params
                try {
                    this.setSaveStatus(true)
                    const saveAction = this.isTemplatePipeline
                        ? this.saveSetting
                        : this.savePipelineAndSetting
                    const responses = await saveAction()

                    if (responses.code === 403) {
                        throw new HttpError(403, responses.message)
                    }
                    this.setPipelineEditing(false)
                    this.$showTips({
                        message: this.$t('saveSuc'),
                        theme: 'success'
                    })

                    if (
                        !this.isTemplatePipeline
                        && this.pipeline.latestVersion
                        && !isNaN(this.pipeline.latestVersion)
                    ) {
                        ++this.pipeline.latestVersion
                        this.updateCurPipelineByKeyValue({
                            key: 'pipelineVersion',
                            value: this.pipeline.latestVersion
                        })
                    }

                    if (
                        this.pipelineSetting
                        && this.pipelineSetting.pipelineName !== this.curPipeline.pipelineName
                    ) {
                        this.updateCurPipelineByKeyValue({
                            key: 'pipelineName',
                            value: this.pipelineSetting.pipelineName
                        })
                    }

                    return {
                        code: 0,
                        data: responses
                    }
                } catch (e) {
                    this.handleError(e, [
                        {
                            actionId: this.$permissionActionMap.edit,
                            resourceId: this.$permissionResourceMap.pipeline,
                            instanceId: [
                                {
                                    id: pipelineId,
                                    name: this.pipeline.name
                                }
                            ],
                            projectId
                        }
                    ])
                    return {
                        code: e.code,
                        message: e.message
                    }
                } finally {
                    this.setSaveStatus(false)
                }
            }
        }
    }
</script>

<style lang="scss">
.pipeline-edit-header {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px 0 14px;
  .pipeline-edit-right-aside {
    display: grid;
    grid-gap: 10px;
    grid-auto-flow: column;
  }
}
</style>

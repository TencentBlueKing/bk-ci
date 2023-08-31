<template>
    <div class="pipeline-edit-header">
        <pipeline-bread-crumb>
            <bk-tag>{{ currentVersionName }}</bk-tag>
        </pipeline-bread-crumb>
        <mode-switch />
        <aside class="pipeline-edit-right-aside">
            <bk-button
                :disabled="saveBtnDisabled"
                :loading="saveStatus"
                outline
                theme="primary"
                @click="save"
            >
                {{ $t("saveDraft") }}
            </bk-button>
            <bk-button
                :disabled="!canDebug"
                :loading="executeStatus"
                @click="debug"
            >
                <span class="debug-pipeline-draft-btn">
                    {{ $t("debug") }}
                    <e>|</e>
                    <i @click.stop="goDraftDebugRecord" class="devops-icon icon-txt" />
                </span>
            </bk-button>
            <bk-button
                theme="primary"
                :disabled="btnDisabled || !canManualStartup || isCurPipelineLocked"
                :loading="executeStatus"
                :title="canManualStartup ? '' : this.$t('newlist.cannotManual')"
                @click="saveAndExec"
            >
                {{ isSaveAndRun ? $t("subpage.saveAndExec") : $t("exec") }}
            </bk-button>
            <!-- <more-actions /> -->
            <span class="publish-pipeline-btn">
                <i class="devops-icon icon-check-small" />
                {{ $t('release') }}
            </span>
        </aside>
    </div>
</template>

<script>
    import { mapState, mapGetters, mapActions, mapMutations } from 'vuex'
    import PipelineBreadCrumb from './PipelineBreadCrumb.vue'
    // import MoreActions from './MoreActions.vue'
    import { PROCESS_API_URL_PREFIX } from '@/store/constants'
    import ModeSwitch from '@/components/ModeSwitch'
    import { HttpError } from '@/utils/util'
    export default {
        components: {
            PipelineBreadCrumb,
            // MoreActions,
            ModeSwitch
        },
        computed: {

            ...mapState('atom', ['pipeline', 'saveStatus', 'pipelineSetting']),
            ...mapState('pipelines', ['pipelineInfo', 'executeStatus']),
            ...mapGetters({
                isCurPipelineLocked: 'pipelines/isCurPipelineLocked',
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
                    || !this.pipelineSetting
                    || Object.keys(this.pipelineSetting).length === 0
                )
            },
            canDebug () {
                return true
            },
            isSaveAndRun () {
                return this.isEditing && !this.saveBtnDisabled
            },
            canManualStartup () {
                return this.pipelineInfo?.canManualStartup ?? false
            },
            isTemplatePipeline () {
                return this.pipelineInfo?.instanceFromTemplate ?? false
            },
            currentVersionName () {
                return this.pipelineInfo?.versionName ?? '--'
            }
        },
        methods: {
            ...mapActions('atom', [
                'setPipelineEditing',
                'setSaveStatus',
                'updateContainer'
            ]),
            ...mapMutations('pipelines', [
                'updatePipelineInfo'
            ]),
            async saveAndExec () {
                if (this.isSaveAndRun) {
                    await this.save()
                }
                this.$router.push({
                    name: 'executePreview',
                    params: {
                        ...this.$route.params,
                        version: this.pipelineInfo?.version
                    }
                })
            },
            debug () {
                this.$router.push({
                    name: 'executePreview',
                    query: {
                        debug: ''
                    },
                    params: {
                        ...this.$route.params,
                        version: this.pipelineInfo?.version
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
                        this.pipelineSetting
                        && this.pipelineSetting.pipelineName !== this.pipelineInfo.pipelineName
                    ) {
                        this.updatePipelineInfo({
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
            },
            goDraftDebugRecord () {
                this.$router.push({
                    name: 'draftDebugRecord'
                })
            }
        }
    }
</script>

<style lang="scss">
@import '@/scss/conf';
.pipeline-edit-header {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  padding: 0 0 0 14px;
  align-self: stretch;
  .debug-pipeline-draft-btn {
    display: flex;
    align-items: center;
    grid-gap: 8px;
    > e {
        color: #DCDEE5;
    }
    > i:hover {
        color: $primaryColor;
    }
  }
  .pipeline-edit-right-aside {
    display: grid;
    grid-gap: 10px;
    grid-auto-flow: column;
    height: 100%;
    align-items: center;
    .publish-pipeline-btn {
        height: 100%;
        display: flex;
        align-items: center;
        color: white;
        background: $primaryColor;
        font-size: 14px;
        padding: 0 20px;
        .icon-check-small {
            font-size: 18px;
        }
        &.disabled {
            background: #DCDEE5;
            cursor: not-allowed;
        }
    }
  }
}
</style>

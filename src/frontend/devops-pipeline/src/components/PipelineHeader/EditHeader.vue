<template>
    <div class="pipeline-edit-header">
        <pipeline-bread-crumb>
            <bk-tag>{{ currentVersionName }}</bk-tag>
        </pipeline-bread-crumb>
        <mode-switch />
        <aside class="pipeline-edit-right-aside">
            <bk-button
                :disabled="saveStatus"
                :loading="saveStatus"
                outline
                theme="primary"
                @click="saveDraft"
            >
                {{ $t("saveDraft") }}
            </bk-button>
            <bk-button
                :disabled="!canDebug"
                :loading="executeStatus"
                @click="exec(true)"
            >
                <span class="debug-pipeline-draft-btn">
                    {{ $t("debug") }}
                    <e>|</e>
                    <i @click.stop="goDraftDebugRecord" class="devops-icon icon-txt" />
                </span>
            </bk-button>
            <bk-button
                theme="primary"
                :disabled="btnDisabled"
                :loading="executeStatus"
                :title="canManualStartup ? '' : this.$t('newlist.cannotManual')"
                @click="exec(false)"
            >
                {{ $t("exec") }}
            </bk-button>
            <!-- <more-actions /> -->
            <span :class="['publish-pipeline-btn', {
                'publish-diabled': saveStatus
            }]" @click="showReleaseSlider">
                <i class="devops-icon icon-check-small" />
                {{ $t('release') }}
            </span>
        </aside>
        <ReleasePipelineSideSlider :version="currentVersion" v-model="isReleaseSliderShow" />
    </div>
</template>

<script>
    import { mapState, mapGetters, mapActions, mapMutations } from 'vuex'
    import PipelineBreadCrumb from './PipelineBreadCrumb.vue'
    // import MoreActions from './MoreActions.vue'
    import { PROCESS_API_URL_PREFIX } from '@/store/constants'
    import ReleasePipelineSideSlider from './ReleasePipelineSideSlider'
    import ModeSwitch from '@/components/ModeSwitch'
    export default {
        components: {
            PipelineBreadCrumb,
            // MoreActions,
            ModeSwitch,
            ReleasePipelineSideSlider
        },
        data () {
            return {
                isReleaseSliderShow: false
            }
        },
        computed: {
            ...mapState('atom', [
                'pipeline',
                'saveStatus',
                'pipelineWithoutTrigger',
                'pipelineSetting',
                'pipelineYaml'
            ]),
            ...mapState('pipelines', ['pipelineInfo', 'executeStatus', 'isManage']),
            ...mapGetters({
                isCurPipelineLocked: 'pipelines/isCurPipelineLocked',
                isEditing: 'atom/isEditing',
                checkPipelineInvalid: 'atom/checkPipelineInvalid'
            }),

            btnDisabled () {
                return this.canDebug || this.executeStatus || !this.canManualStartup || this.isCurPipelineLocked
            },
            canDebug () {
                // TODO: 临时方案，后续需要后端支持
                return (this.pipeline?.canDebug ?? false) && !this.saveStatus
            },
            canManualStartup () {
                return this.pipelineInfo?.canManualStartup ?? false
            },
            isTemplatePipeline () {
                return this.pipelineInfo?.instanceFromTemplate ?? false
            },
            currentVersionName () {
                if (this.canDebug) {
                    return this.$t('editPage.draftVersion', [this.pipeline?.baseVersionName ?? '--'])
                }
                return this.pipelineInfo?.versionName ?? '--'
            },
            pipelineName () {
                return this.pipelineInfo?.name ?? '--'
            },
            currentVersion () {
                return this.pipelineInfo?.version ?? ''
            }
        },
        methods: {
            ...mapActions('atom', [
                'setPipelineEditing',
                'saveDraftPipeline',
                'setSaveStatus',
                'updateContainer'
            ]),
            ...mapMutations('pipelines', [
                'updatePipelineInfo'
            ]),
            async exec (debug) {
                this.$router.push({
                    name: 'executePreview',
                    query: {
                        ...(debug ? { debug: '' } : {})
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

            async saveDraft () {
                try {
                    this.setSaveStatus(true)
                    const pipeline = Object.assign({}, this.pipeline, {
                        stages: [
                            this.pipeline.stages[0],
                            ...this.pipelineWithoutTrigger.stages
                        ]
                    })
                    const { pipelineSetting, checkPipelineInvalid, pipelineYaml } = this
                    const { inValid, message } = checkPipelineInvalid(pipeline.stages, pipelineSetting)
                    const { projectId, pipelineId } = this.$route.params
                    if (inValid) {
                        throw new Error(message)
                    }
                    // 清除流水线参数渲染过程中添加的key
                    this.formatParams(pipeline)
                    // if (!pipelineId) {
                    //     return this.importPipelineAndSetting(body)
                    // }

                    // 请求执行构建
                    await this.saveDraftPipeline({
                        projectId,
                        pipelineId,
                        baseVersion: pipeline.baseVersion,
                        modelAndSetting: {
                            model: {
                                ...pipeline,
                                name: pipelineSetting.pipelineName,
                                desc: pipelineSetting.desc
                            },
                            setting: pipelineSetting
                        },
                        yaml: pipelineYaml
                    })
                    this.setPipelineEditing(false)
                    this.updatePipelineInfo({
                        key: 'canDebug',
                        value: true
                    })
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('editPage.saveDraftSuccess', [pipelineSetting.pipelineName])
                    })
                } catch (e) {
                    this.handleError(e, [
                        {
                            actionId: this.$permissionActionMap.edit,
                            resourceId: this.$permissionResourceMap.pipeline,
                            instanceId: [
                                {
                                    id: this.pipeline.pipelineId,
                                    name: this.pipeline.name
                                }
                            ],
                            projectId: this.$route.params.projectId
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

            saveSetting () {
                const pipelineSetting = this.getPipelineSetting()
                const { projectId, pipelineId } = this.$route.params
                return this.$ajax.post(
                    `/${PROCESS_API_URL_PREFIX}/user/pipelines/${projectId}/${pipelineId}/saveSetting`,
                    pipelineSetting
                )
            },
            showReleaseSlider () {
                this.isReleaseSliderShow = true
            },
            goDraftDebugRecord () {
                this.$router.push({
                    name: 'draftDebugRecord',
                    params: {
                        version: this.pipelineInfo?.version
                    }
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
        cursor: pointer;
        &.publish-diabled {
            background: #DCDEE5;
            cursor: not-allowed;
        }
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

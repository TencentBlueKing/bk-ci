<template>
    <div class="pipeline-edit-header">
        <pipeline-bread-crumb :pipeline-name="pipelineSetting?.pipelineName">
            <bk-tag>{{ currentVersionName }}</bk-tag>
        </pipeline-bread-crumb>
        <mode-switch :save="saveDraft" />
        <aside class="pipeline-edit-right-aside">
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
                        resourceType: 'pipeline',
                        resourceCode: pipelineId,
                        action: RESOURCE_ACTION.EDIT
                    }
                }"
            >
                {{ $t("saveDraft") }}
            </bk-button>
            <bk-button
                :disabled="!canDebug"
                :loading="executeStatus"
                v-perm="{
                    hasPermission: canExecute,
                    disablePermissionApi: true,
                    permissionData: {
                        projectId,
                        resourceType: 'pipeline',
                        resourceCode: pipelineId,
                        action: RESOURCE_ACTION.EXECUTE
                    }
                }"
                @click="exec(true)"
            >
                <span class="debug-pipeline-draft-btn">
                    {{ $t("debug") }}
                    <b>|</b>
                    <i
                        :class="['devops-icon icon-txt', {
                            'icon-txt-disabled': !canDebug
                        }]"
                        @click.stop="goDraftDebugRecord"
                    />
                </span>
            </bk-button>
            <!-- <more-actions /> -->
            <release-button
                :can-release="canRelease && !isEditing"
                :project-id="projectId"
                :pipeline-id="pipelineId"
            />
        </aside>
    </div>
</template>

<script>
    import { mapActions, mapGetters, mapState } from 'vuex'
    import PipelineBreadCrumb from './PipelineBreadCrumb.vue'
    import ReleaseButton from './ReleaseButton'
    import ModeSwitch from '@/components/ModeSwitch'
    import { UPDATE_PIPELINE_INFO } from '@/store/modules/atom/constants'
    import { PROCESS_API_URL_PREFIX } from '@/store/constants'
    import {
        RESOURCE_ACTION
    } from '@/utils/permission'

    export default {
        components: {
            PipelineBreadCrumb,
            ReleaseButton,
            ModeSwitch
        },
        data () {
            return {
                isReleaseSliderShow: false
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
            }
        },
        methods: {
            ...mapActions('atom', [
                'setPipelineEditing',
                'saveDraftPipeline',
                'setSaveStatus',
                'updateContainer'
            ]),
            async exec (debug) {
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

            async saveDraft () {
                try {
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
                    // 清除流水线参数渲染过程中添加的key
                    this.formatParams(pipeline)

                    // 请求执行构建
                    const { data: { version, versionName } } = await this.saveDraftPipeline({
                        projectId,
                        pipelineId,
                        baseVersion: this.pipelineInfo.baseVersion,
                        storageType: this.pipelineMode,
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

                    this.$store.commit(`atom/${UPDATE_PIPELINE_INFO}`, {
                        canDebug: true,
                        canRelease: true,
                        baseVersion: this.pipelineInfo?.baseVersion ?? this.pipelineInfo?.releaseVersion,
                        baseVersionName: this.pipelineInfo?.baseVersionName ?? this.pipelineInfo?.releaseVersionName,
                        version,
                        versionName
                    })

                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('editPage.saveDraftSuccess', [pipelineSetting.pipelineName]),
                        limit: 1
                    })
                    return true
                } catch (e) {
                    const { projectId, pipelineId } = this.$route.params

                    this.handleError(e, {
                        projectId,
                        resourceCode: pipelineId,
                        action: RESOURCE_ACTION.EDIT
                    }, 10000)
                    return false
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
            goDraftDebugRecord () {
                if (this.canDebug) {
                    this.$router.push({
                        name: 'draftDebugRecord',
                        params: {
                            version: this.pipelineInfo?.version
                        }
                    })
                }
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
    .icon-txt-disabled {
        cursor: not-allowed;
    }
    > i:not(.icon-txt-disabled):hover {
        color: $primaryColor;
    }
  }
  .pipeline-edit-right-aside {
    display: grid;
    grid-gap: 10px;
    grid-auto-flow: column;
    height: 100%;
    align-items: center;
  }
}

  </style>

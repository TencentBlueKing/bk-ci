<template>
    <div
        v-if="pipelineName"
        class="pipeline-preview-header"
    >
        <div class="pipeline-preview-left-aside">
            <pipeline-bread-crumb
                :is-loading="!pipelineName"
                :pipeline-name="pipelineName"
            >
                <span class="build-num-switcher-wrapper">
                    {{ title }}
                </span>
            </pipeline-bread-crumb>
            <pac-branch-selector
                ref="pacBranchSelectorRef"
                @branch-change="handleBranchChange"
            />
        </div>
        <aside class="pipeline-preview-right-aside">
            <bk-button
                theme="primary"
                :disabled="executeStatus || hasPacError || branchLoading"
                :loading="executeStatus || branchLoading"
                v-if="isDebugPipeline"
                v-perm="{
                    hasPermission: canExecute,
                    disablePermissionApi: true,
                    permissionData: execPermData
                }"
                @click="handleClick"
            >
                {{ $t("debug") }}
            </bk-button>

            <bk-button
                :disabled="executeStatus"
                @click="goBack"
            >
                {{ $t("cancel") }}
            </bk-button>
            <span v-bk-tooltips="execTips">
                <bk-button
                    theme="primary"
                    :disabled="executeStatus || versionNotMatch || hasPacError || branchLoading"
                    :loading="executeStatus || branchLoading"
                    v-if="!isDebugPipeline"
                    v-perm="{
                        hasPermission: canExecute,
                        disablePermissionApi: true,
                        permissionData: execPermData
                    }"
                    @click="handleClick"
                >
                    {{ $t("exec") }}
                </bk-button>
            </span>
        </aside>
    </div>
    <i
        v-else
        class="devops-icon icon-circle-2-1 spin-icon"
        style="margin-left: 20px;"
    />
</template>

<script>
    import { UPDATE_PREVIEW_PIPELINE_NAME, PAC_BRANCH_CHANGE, UPDATE_PAC_ERROR_STATUS, PAC_BRANCH_LOADING, bus } from '@/utils/bus'
    import {
        RESOURCE_ACTION,
        RESOURCE_TYPE
    } from '@/utils/permission'
    import { mapActions, mapGetters, mapState } from 'vuex'
    import PipelineBreadCrumb from './PipelineBreadCrumb'
    import PacBranchSelector from './PacBranchSelector'
    export default {
        components: {
            PipelineBreadCrumb,
            PacBranchSelector
        },
        data () {
            return {
                paramsValid: true,
                previewPipelineName: '',
                hasPacError: false,
                branchLoading: false
            }
        },
        computed: {
            ...mapState('pipelines', ['executeStatus']),
            ...mapGetters({
                isEditing: 'atom/isEditing',
                isBranchVersion: 'atom/isBranchVersion',
                isReleaseVersion: 'atom/isReleaseVersion',
                canManualStartup: 'pipelines/canManualStartup'
            }),
            ...mapState('atom', [
                'pipelineInfo'
            ]),
            pipelineName () {
                return this.previewPipelineName || this.pipelineInfo?.pipelineName || ''
            },
            RESOURCE_ACTION () {
                return RESOURCE_ACTION
            },
            execPermData () {
                return {
                    projectId: this.projectId,
                    resourceType: RESOURCE_TYPE.PIPELINE,
                    resourceCode: this.spipelineId,
                    action: RESOURCE_ACTION.EXECUTE
                }
            },
            title () {
                return this.$t(`details.${this.isDebugPipeline ? 'debug' : 'exec'}Preview`)
            },
            isDebugPipeline () {
                return Object.prototype.hasOwnProperty.call(this.$route.query, 'debug')
            },
            projectId () {
                return this.$route.params.projectId
            },
            pipelineId () {
                return this.$route.params.pipelineId
            },
            canEdit () {
                return this.pipelineInfo?.permissions.canEdit ?? true
            },
            canExecute () {
                return this.pipelineInfo?.permissions.canExecute ?? true
            },
            versionNotMatch () {
                try {
                    return !this.isDebugPipeline && !this.isBranchVersion && !this.isReleaseVersion
                } catch (error) {
                    return false
                }
            },
            execTips () {
                return {
                    content: this.$t('versionNotMatch'),
                    disabled: !this.versionNotMatch
                }
            }
        },
        watch: {
            pipelineId (pipelineId) {
                this.$router.push({
                    name: 'pipelinesHistory',
                    params: {
                        projectId: this.projectId,
                        pipelineId,
                        type: 'history'
                    }
                })
            }
        },
        mounted () {
            bus.$on(UPDATE_PREVIEW_PIPELINE_NAME, this.updatePipelineName)
            bus.$on(UPDATE_PAC_ERROR_STATUS, this.updatePacErrorStatus)
            bus.$on(PAC_BRANCH_LOADING, this.updateBranchLoading)
        },
        beforeDestroy () {
            bus.$off(UPDATE_PREVIEW_PIPELINE_NAME, this.updatePipelineName)
            bus.$off(UPDATE_PAC_ERROR_STATUS, this.updatePacErrorStatus)
            bus.$off(PAC_BRANCH_LOADING, this.updateBranchLoading)
            this.selectPipelineVersion(null)
        },
        methods: {
            ...mapActions('atom', ['selectPipelineVersion']),
            updatePipelineName (name) {
                this.previewPipelineName = name
            },
            updatePacErrorStatus (hasError) {
                this.hasPacError = hasError
            },
            updateBranchLoading (isLoading) {
                this.branchLoading = isLoading
            },
            handleClick () {
                bus.$emit('start-execute')
            },
            goBack () {
                this.$router.back()
            },
            handleBranchChange (branchName, branchInfo) {
                bus.$emit(PAC_BRANCH_CHANGE, branchName, branchInfo)
            }
        }
    }
</script>

<style lang="scss">
.pipeline-preview-header {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px 0 14px;
  .pipeline-preview-left-aside {
    display: flex;
    align-items: center;
    flex: 1;
    overflow: hidden;
  }
  .build-num-switcher-wrapper {
    display: grid;
    grid-auto-flow: column;
    grid-gap: 6px;
  }
  .pipeline-execute-step {
    width: 300px;
    flex-shrink: 0;
  }
  .pipeline-preview-right-aside {
    display: grid;
    grid-gap: 10px;
    grid-auto-flow: column;
    flex-shrink: 0;
  }
}
</style>

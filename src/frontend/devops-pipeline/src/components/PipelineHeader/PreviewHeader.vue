<template>
    <div
        v-if="pipelineName"
        class="pipeline-preview-header"
    >
        <pipeline-bread-crumb
            :is-loading="!pipelineName"
            :pipeline-name="pipelineName"
        >
            <span class="build-num-switcher-wrapper">
                {{ title }}
            </span>
        </pipeline-bread-crumb>
        <aside class="pipeline-preview-right-aside">
            <bk-button
                theme="primary"
                :disabled="executeStatus"
                :loading="executeStatus"
                v-if="isDebugPipeline"
                v-perm="{
                    hasPermission: canExecute,
                    disablePermissionApi: true,
                    permissionData: {
                        projectId: projectId,
                        resourceType: 'pipeline',
                        resourceCode: pipelineId,
                        action: RESOURCE_ACTION.EXECUTE
                    }
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
                    :disabled="executeStatus || versionNotMatch"
                    :loading="executeStatus"
                    v-if="!isDebugPipeline"
                    v-perm="{
                        hasPermission: canExecute,
                        disablePermissionApi: true,
                        permissionData: {
                            projectId: projectId,
                            resourceType: 'pipeline',
                            resourceCode: pipelineId,
                            action: RESOURCE_ACTION.EXECUTE
                        }
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
    import { UPDATE_PREVIEW_PIPELINE_NAME, bus } from '@/utils/bus'
    import {
        RESOURCE_ACTION
    } from '@/utils/permission'
    import { mapActions, mapGetters, mapState } from 'vuex'
    import PipelineBreadCrumb from './PipelineBreadCrumb'
    export default {
        components: {
            PipelineBreadCrumb
        },
        data () {
            return {
                paramsValid: true,
                pipelineName: ''
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
            RESOURCE_ACTION () {
                return RESOURCE_ACTION
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
        },
        beforeDestroy () {
            bus.$off(UPDATE_PREVIEW_PIPELINE_NAME, this.updatePipelineName)
            this.selectPipelineVersion(null)
        },
        methods: {
            ...mapActions('atom', ['selectPipelineVersion']),
            updatePipelineName (name) {
                this.pipelineName = name
            },
            handleClick () {
                bus.$emit('start-execute')
            },
            goBack () {
                this.$router.back()
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

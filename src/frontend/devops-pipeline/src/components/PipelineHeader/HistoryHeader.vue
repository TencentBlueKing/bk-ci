<template>
    <div class="pipeline-history-header">
        <div class="pipeline-history-left-aside">
            <pipeline-bread-crumb :is-loading="switchingVersion" />
            <pac-tag class="pipeline-pac-indicator" v-if="pacEnabled" :info="yamlInfo" />
            <VersionSelector
                :value="activePipelineVersion?.version"
                ref="versionSelectorInstance"
                @change="handleVersionChange"
                @showAllVersion="showVersionSideSlider"
            />
            <bk-button
                v-if="!isReleaseVersion"
                text
                size="small"
                theme="primary"
                @click="switchToReleaseVersion"
            >
                <i class="devops-icon icon-shift"></i>
                {{ $t("switchToReleaseVersion") }}
            </bk-button>
            <badge
                v-if="isReleaseVersion"
                class="pipeline-exec-badge"
                :project-id="projectId"
                :pipeline-id="pipelineId"
            />
        </div>
        <aside class="pipeline-history-right-aside">
            <VersionDiffEntry
                v-if="!isReleaseVersion"
                :text="false"
                outline
                :version="activePipelineVersion?.version"
                :latest-version="releaseVersion"
            >
                {{ $t("diff") }}
            </VersionDiffEntry>
            <RollbackEntry
                v-if="showRollback"
                :text="false"
                :has-permission="canEdit"
                :version="activePipelineVersion?.version"
                :pipeline-id="pipelineId"
                :project-id="projectId"
                :version-name="activePipelineVersion?.versionName"
                :draft-base-version-name="draftBaseVersionName"
                :is-active-draft="activePipelineVersion?.isDraft"
            />
            <template v-else>
                <bk-button
                    theme="primary"
                    outline
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
                    @click="goEdit"
                >
                    {{ $t("edit") }}
                </bk-button>
                <span v-bk-tooltips="tooltip">
                    <bk-button
                        :disabled="!executable"
                        theme="primary"
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
                        @click="goExecPreview"
                    >
                        {{ $t(isActiveDraftVersion ? 'debug' : 'exec') }}

                    </bk-button>
                </span>
            </template>
            <more-actions v-if="isReleaseVersion" />
        </aside>
        <VersionHistorySideSlider
            :show-version-sideslider="showVersionSideslider"
            @close="closeVersionSideSlider"
        />
    </div>
</template>

<script>
    import { mapGetters, mapState, mapActions } from 'vuex'

    import {
        RESOURCE_ACTION
    } from '@/utils/permission'
    import MoreActions from './MoreActions.vue'
    import PipelineBreadCrumb from './PipelineBreadCrumb.vue'
    import PacTag from '@/components/PacTag.vue'
    import Badge from '@/components/Badge.vue'
    import VersionSelector from '@/components/PipelineDetailTabs/VersionSelector'
    import VersionHistorySideSlider from '@/components/PipelineDetailTabs/VersionHistorySideSlider'
    import VersionDiffEntry from '@/components/PipelineDetailTabs/VersionDiffEntry'
    import RollbackEntry from '@/components/PipelineDetailTabs/RollbackEntry'

    export default {
        components: {
            PipelineBreadCrumb,
            PacTag,
            Badge,
            MoreActions,
            VersionSelector,
            VersionHistorySideSlider,
            VersionDiffEntry,
            RollbackEntry
        },
        props: {
            updatePipeline: Function
        },
        data () {
            return {
                showVersionSideslider: false
            }
        },
        computed: {
            ...mapState('atom', [
                'pipeline',
                'pipelineInfo',
                'activePipelineVersion',
                'switchingVersion'
            ]),
            ...mapGetters({
                isCurPipelineLocked: 'atom/isCurPipelineLocked',
                isReleasePipeline: 'atom/isReleasePipeline',
                isReleaseVersion: 'atom/isReleaseVersion',
                isActiveDraftVersion: 'atom/isActiveDraftVersion',
                isOutdatedVersion: 'atom/isOutdatedVersion',
                draftBaseVersionName: 'atom/getDraftBaseVersionName',
                pacEnabled: 'atom/pacEnabled'
            }),
            showRollback () {
                return !this.isActiveDraftVersion && !this.isReleaseVersion
            },
            releaseVersion () {
                return this.pipelineInfo?.releaseVersion
            },
            releaseVersionName () {
                return this.pipelineInfo?.releaseVersionName
            },
            projectId () {
                return this.$route.params.projectId
            },
            pipelineId () {
                return this.pipelineInfo?.pipelineId
            },
            yamlInfo () {
                return this.pipelineInfo?.yamlInfo
            },
            canEdit () {
                return this.pipelineInfo?.permissions?.canEdit ?? true
            },
            canExecute () {
                return this.pipelineInfo?.permissions?.canExecute ?? true
            },
            executable () {
                return (!this.isCurPipelineLocked && this.canManualStartup && this.isReleasePipeline) || this.isActiveDraftVersion
            },
            canManualStartup () {
                return this.pipelineInfo?.canManualStartup ?? true
            },
            tooltip () {
                return this.executable
                    ? {
                        disabled: true
                    }
                    : {
                        content: this.$t(!this.isReleasePipeline ? 'draftPipelineExecTips' : this.isCurPipelineLocked ? 'pipelineLockTips' : 'pipelineManualDisable'),
                        delay: [300, 0]
                    }
            },
            RESOURCE_ACTION () {
                return RESOURCE_ACTION
            },
            filters () {
                return [this.pipelineId, this.activePipelineVersion?.version].join('\\')
            }
        },
        watch: {
            releaseVersion (version) {
                console.log('watch, releaseVersion', version)
                this.selectPipelineVersion({
                    version
                })
            },
            filters (filters) {
                console.log('watch', filters)
                this.$nextTick(() => {
                    this.init()
                })
            }
        },
        mounted () {
            if (this.releaseVersion) {
                this.selectPipelineVersion({
                    version: this.releaseVersion
                })
            }
        },
        methods: {
            ...mapActions('atom', [
                'selectPipelineVersion',
                'requestPipeline',
                'setSwitchingPipelineVersion',
                'setShowVariable'
            ]),
            goEdit () {
                this.$router.push({
                    name: 'pipelinesEdit'
                })
            },
            showVersionSideSlider () {
                this.setShowVariable(false)
                this.$refs?.versionSelectorInstance?.close?.()
                this.showVersionSideslider = true
            },
            closeVersionSideSlider () {
                this.showVersionSideslider = false
            },
            async init () {
                try {
                    const version = this.activePipelineVersion?.version
                    console.log('watch,init', this.activePipelineVersion?.version)
                    if (version) {
                        this.setSwitchingPipelineVersion(true)
                        await this.requestPipeline({
                            projectId: this.projectId,
                            pipelineId: this.pipelineId,
                            version
                        })
                    }
                } catch (error) {
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message
                    })
                } finally {
                    this.setSwitchingPipelineVersion(false)
                }
            },
            goExecPreview () {
                this.$router.push({
                    name: 'executePreview',
                    query: {
                        ...(this.isActiveDraftVersion ? { debug: '' } : {})
                    },
                    params: {
                        ...this.$route.params,
                        version: this.activePipelineVersion?.version
                    }
                })
            },
            switchToReleaseVersion () {
                this.selectPipelineVersion({
                    version: this.releaseVersion
                })
            },
            handleVersionChange (versionId, version) {
                this.selectPipelineVersion(version)
                if (['history', 'triggerEvent'].includes(this.$route.params.type) && !this.isReleaseVersion) {
                    this.$nextTick(() => {
                        this.$router.push({
                            name: 'pipelinesHistory',
                            params: {
                                ...this.$route.params,
                                type: 'pipeline'
                            }
                        })
                    })
                }
            }
        }
    }
</script>

<style lang="scss">
.pipeline-history-header {
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 24px 0 14px;
    .pipeline-history-left-aside {
        display: grid;
        grid-auto-flow: column;
        align-items: center;
        .pipeline-pac-indicator{
            margin-right: 16px;
        }
        .pipeline-exec-badge  {
            margin-left: 4px;
        }
    }

    .pipeline-history-right-aside {
        flex-shrink: 0;
        display: grid;
        align-items: center;
        grid-gap: 10px;
        grid-auto-flow: column;
    }
}
</style>

<template>
    <div class="pipeline-history-header">
        <component
            :is="breadCrumb"
            v-bind="breadCrumbProps"
        >
            <bk-popover :delay="[666, 0]">
                <VersionSelector
                    :value="currentVersion"
                    ref="versionSelectorInstance"
                    @change="handleVersionChange"
                    @showAllVersion="showVersionSideSlider"
                    :include-draft="false"
                    refresh-list-on-expand
                    :is-template="isTemplate"
                    :unique-id="uniqueId"
                />
                <div slot="content">
                    <p>{{ $t('versionRuleP') }}</p>
                    <p>{{ $t('versionRuleT') }}</p>
                    <p>{{ $t('versionRuleA') }}</p>
                </div>
            </bk-popover>
            <bk-button
                v-if="!isReleaseVersion && isReleasePipeline"
                text
                size="small"
                theme="primary"
                @click="switchToReleaseVersion"
            >
                <i class="devops-icon icon-shift"></i>
                {{ $t("switchToReleaseVersion") }}
            </bk-button>
            <badge
                v-if="!isTemplate && isReleaseVersion"
                class="pipeline-exec-badge"
                :project-id="projectId"
                :pipeline-id="uniqueId"
            />
        </component>

        <aside
            v-show="!(isSwitchPipeline || switchingVersion)"
            class="pipeline-history-right-aside"
        >
            <VersionDiffEntry
                v-if="!isTemplatePipeline && !editAndExecutable && !archiveFlag"
                :text="false"
                outline
                :version="currentVersion"
                :latest-version="releaseVersion"
            >
                {{ $t("diff") }}
            </VersionDiffEntry>
            <RollbackEntry
                v-if="showRollback && (isReleasePipeline || onlyBranchPipeline) && !archiveFlag"
                :text="false"
                :has-permission="canEdit"
                :version="currentVersion"
                :draft-version="pipelineInfo?.version"
                :pipeline-id="uniqueId"
                :project-id="projectId"
                :theme="isTemplate ? 'default' : 'primary'"
                :version-name="activePipelineVersion?.versionName"
                :draft-base-version-name="draftBaseVersionName"
                :is-active-draft="activePipelineVersion?.isDraft"
                :is-active-branch-version="isActiveBranchVersion"
                :draft-creator="activePipelineVersion?.creator"
                :draft-create-time="activePipelineVersion?.createTime"
                :rollback-id="uniqueId"
                :draft-status="draftStatus"
                :draft-save-info="draftSaveInfo"
                :is-rollback-btn="isRollback"
            >
                {{ operateName }}
            </RollbackEntry>
            <bk-button
                v-else-if="onlyBranchPipeline && activePipelineVersion?.version === releaseVersion && !archiveFlag"
                theme="primary"
                outline
                v-perm="{
                    hasPermission: canEdit,
                    disablePermissionApi: true,
                    permissionData: {
                        projectId,
                        resourceType,
                        resourceCode: uniqueId,
                        action: resourceEditAction
                    }
                }"
                @click="goEdit"
            >
                {{ isTemplate ? $t('template.editTemplate') : $t('edit') }}
            </bk-button>

            <instance-release-btn
                v-if="isTemplate && canInstantiate"
                :perm-data="{
                    disablePermissionApi: false,
                    permissionData: {
                        projectId,
                        resourceType: RESOURCE_TYPE.PROJECT,
                        resourceCode: projectId,
                        action: RESOURCE_ACTION.CREATE
                    }
                }"
                @click="handleToInstanceEntry"
            />
            <template v-else-if="!isTemplate">
                <template v-if="editAndExecutable">
                    <span
                        v-if="!archiveFlag"
                        v-bk-tooltips="tooltip"
                    >
                        <bk-button
                            :disabled="!executable"
                            theme="primary"
                            v-perm="{
                                hasPermission: canExecute,
                                disablePermissionApi: true,
                                permissionData: {
                                    projectId,
                                    resourceType,
                                    resourceCode: uniqueId,
                                    action: RESOURCE_ACTION.EXECUTE
                                }
                            }"
                            @click="goExecPreview"
                        >
                            {{ $t(isActiveDraftVersion ? 'debug' : 'exec') }}
                        </bk-button>
                    </span>
                    <more-actions />
                </template>
            </template>
        </aside>
        <VersionHistorySideSlider
            :show-version-sideslider="showVersionSideslider"
            @close="closeVersionSideSlider"
        />
    </div>
</template>

<script>
    import Badge from '@/components/Badge.vue'
    import RollbackEntry from '@/components/PipelineDetailTabs/RollbackEntry'
    import VersionDiffEntry from '@/components/PipelineDetailTabs/VersionDiffEntry'
    import VersionHistorySideSlider from '@/components/PipelineDetailTabs/VersionHistorySideSlider'
    import VersionSelector from '@/components/PipelineDetailTabs/VersionSelector'
    import InstanceReleaseBtn from '@/components/Template/InstanceReleaseBtn.vue'
    import TemplateBreadCrumb from '@/components/Template/TemplateBreadCrumb.vue'
    import {
        RESOURCE_ACTION,
        RESOURCE_TYPE,
        TEMPLATE_RESOURCE_ACTION,
    } from '@/utils/permission'
    import { pipelineTabIdMap } from '@/utils/pipelineConst'
    import dayjs from 'dayjs'
    import { mapActions, mapGetters, mapState } from 'vuex'
    import MoreActions from './MoreActions.vue'
    import PipelineBreadCrumb from './PipelineBreadCrumb.vue'

    export default {
        components: {
            PipelineBreadCrumb,
            TemplateBreadCrumb,
            Badge,
            MoreActions,
            VersionSelector,
            VersionHistorySideSlider,
            VersionDiffEntry,
            RollbackEntry,
            InstanceReleaseBtn
        },
        props: {
            isSwitchPipeline: Boolean
        },
        data () {
            return {
                RESOURCE_TYPE,
                RESOURCE_ACTION,
                showVersionSideslider: false,
                draftStatus: 'NORMAL',
                draftSaveInfo: null,
                isPipelineIdChanged: false
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
                pipelineHistoryViewable: 'atom/pipelineHistoryViewable',
                onlyBranchPipeline: 'atom/onlyBranchPipeline',
                isTemplate: 'atom/isTemplate'
            }),
            
            breadCrumb () {
                return this.isTemplate ? 'template-bread-crumb' : 'pipeline-bread-crumb'
            },
            breadCrumbProps () {
                return this.isTemplate
                    ? {
                        templateName: this.pipeline?.name,
                        isLoading: !this.pipeline
                    }
                    : {
                        isLoading: this.isSwitchPipeline || this.switchingVersion
                    }
            },
            editAndExecutable () {
                return this.isReleaseVersion || this.activePipelineVersion?.isBranchVersion
            },
            isActiveBranchVersion () {
                return this.activePipelineVersion?.isBranchVersion ?? false
            },
            showRollback () {
                return this.editAndExecutable || !this.pipelineInfo?.baseVersion || this.activePipelineVersion?.baseVersion !== this.pipelineInfo?.baseVersion
            },
            currentVersion () {
                return this.$route.params.version ? parseInt(this.$route.params.version) : this.releaseVersion
            },
            isTemplatePipeline () {
                return this.pipelineInfo?.instanceFromTemplate ?? false
            },
            releaseVersion () {
                return this.pipelineInfo?.releaseVersion
            },
            projectId () {
                return this.$route.params.projectId
            },
            uniqueId () {
                return this.$route.params?.[this.isTemplate ? 'templateId' : 'pipelineId']
            },
            resourceType () {
                return this.isTemplate ? RESOURCE_TYPE.TEMPLATE : RESOURCE_TYPE.PIPELINE
            },
            resourceEditAction () {
                return this.isTemplate ? TEMPLATE_RESOURCE_ACTION.EDIT : RESOURCE_ACTION.EDIT
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
                return (!this.isCurPipelineLocked && this.canManualStartup && this.editAndExecutable) || this.isActiveDraftVersion
            },
            canManualStartup () {
                return this.pipelineInfo?.canManualStartup ?? true
            },
            // 判断当前操作是否为回滚操作
            isRollback () {
                // 编辑模式：非回滚
                if (this.editAndExecutable) return false
                // 编辑当前草稿：非回滚
                if (this.pipelineInfo?.baseVersion && this.activePipelineVersion?.version === this.pipelineInfo?.baseVersion) return false
                // 其他情况：回滚
                return true
            },
            operateName () {
                switch (true) {
                    case this.editAndExecutable:
                        return this.isTemplate ? this.$t('template.editTemplate') : this.$t('edit')
                    case this.pipelineInfo?.baseVersion && this.activePipelineVersion?.version === this.pipelineInfo?.baseVersion:
                        return this.$t('editCurDraft')
                    default:
                        return this.$t('rollback')
                }
            },
            tooltip () {
                return this.executable
                    ? {
                        disabled: true
                    }
                    : {
                        content: this.$t(this.isCurPipelineLocked ? 'pipelineLockTips' : !(this.isReleasePipeline || this.onlyBranchPipeline) ? 'draftPipelineExecTips' : 'pipelineManualDisable'),
                        delay: [300, 0]
                    }
            },
            editRouteName () {
                return this.isTemplate ? 'templateEdit' : 'pipelinesEdit'
            },
            canInstantiate () {
                return this.releaseVersion === this.currentVersion || this.isBranchVersion
            },
            archiveFlag () {
                return this.$route.query.archiveFlag
            }
        },
        watch: {
            currentVersion () {
                this.$nextTick(this.init)
            }
        },
        created () {
            if (!this.pipelineHistoryViewable) {
                this.$router.replace({
                    name: this.editRouteName
                })
            }
            if (this.releaseVersion !== this.currentVersion && !this.currentVersion) {
                this.handleVersionChange(this.releaseVersion)
            } else {
                this.init()
            }
            // 获取草稿状态
            this.getPipelineDraftStatus()
        },
        methods: {
            ...mapActions('atom', [
                'selectPipelineVersion',
                'requestPipeline',
                'setSwitchingPipelineVersion',
                'setShowVariable'
            ]),
            ...mapActions('common', [
                'getDraftStatus',
                'getTemplateDraftStatus',
            ]),
            async getPipelineDraftStatus () {
                try {
                    const request = this.isTemplate ? this.getTemplateDraftStatus : this.getDraftStatus
                    const dynamicKey = this.isTemplate ? 'templateId' : 'pipelineId'
                    const params = {
                        projectId: this.projectId,
                        actionType: 'EDIT',
                        [dynamicKey]: this.uniqueId,
                    }
                    const res = await request(params)
                    this.draftStatus = res.status
                    this.draftSaveInfo = {
                        creator: res.draft?.creator,
                        createTime: dayjs(res.draft?.createTime).format('YYYY-MM-DD HH:mm:ss'),
                        draftVersionName: res.draft?.baseVersionName,
                        draftVersion: res.draft?.version,
                        releaseVersionName: res.release?.versionName || this.pipelineInfo?.releaseVersionName,
                        releaseVersion: res.release?.version || this.releaseVersion,
                    }
                } catch (error) {
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message
                    })
                }
            },
            goEdit () {
                this.$router.push({
                    name: this.editRouteName,
                    query: {
                        tab: pipelineTabIdMap[this.$route.params.type] ?? 'pipeline'
                    }
                })
            },
            handleToInstanceEntry () {
                this.$router.push({
                    name: 'instanceEntry',
                    params: {
                        ...this.$route.params,
                        version: this.releaseVersion,
                        type: 'create'
                    }
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
                    if (this.currentVersion) {
                        this.setSwitchingPipelineVersion(true)
                        const urlParams = {
                            projectId: this.projectId,
                            [this.isTemplate ? 'templateId' : 'pipelineId']: this.uniqueId,
                            version: this.currentVersion,
                            archiveFlag: this.archiveFlag
                        }
                        await this.requestPipeline(urlParams)
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
                        version: this.currentVersion
                    }
                })
            },
            switchToReleaseVersion () {
                this.handleVersionChange(this.releaseVersion)
            },

            handleVersionChange (versionId, version) {
                let routeType = this.$route.params.type || this.isTemplate ? 'instanceList' : 'history'
                const noRecordVersionTab = this.isTemplate ? ['instanceList'] : ['history', 'triggerEvent']

                if (version) {
                    this.selectPipelineVersion(version)
                    if (this.releaseVersion) {
                        const noRecordVersion = noRecordVersionTab.includes(this.$route.params.type) && !(versionId === this.releaseVersion || version.isBranchVersion)
                        routeType = noRecordVersion ? pipelineTabIdMap.pipeline : this.$route.params.type
                    }
                }

                this.$router.replace({
                    query: this.$route.query,
                    params: {
                        ...this.$route.params,
                        version: versionId,
                        type: routeType
                    }
                })
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

    .pipeline-history-right-aside {
        flex-shrink: 0;
        display: grid;
        align-items: center;
        grid-gap: 10px;
        grid-auto-flow: column;
    }
}
</style>

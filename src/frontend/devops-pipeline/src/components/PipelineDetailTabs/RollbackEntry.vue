<template>
    <span>
        <bk-button
            :text="text"
            :outline="outline"
            :theme="theme"
            :disabled="loading"
            :loading="loading"
            v-perm="{
                hasPermission,
                disablePermissionApi: typeof hasPermission === 'boolean',
                permissionData: {
                    projectId: projectId,
                    resourceType,
                    resourceCode: rollbackId,
                    action: resourceAction
                }
            }"
            @click.stop="handleClick"
        >
            <slot>
                {{ operateName }}
            </slot>
        </bk-button>
        <DraftConfirmDialog
            v-model="isShowConfirmDialog"
            :has-draft-pipeline="hasDraftPipeline"
            :draft-status="draftStatus"
            :draft-save-info="draftSaveInfo"
            :draft-hint-title="draftHintTitle"
            :active-branch-version-info="activeBranchVersionInfo"
            :version-name="versionName"
            :is-active-branch-version="isActiveBranchVersion"
            :is-rollback="isRollback"
            :is-template-pipeline="isTemplatePipeline"
            :draft-version="draftVersion"
            :click-action-type="clickActionType"
            @confirm="rollback"
            @edit-draft="goEdit"
            @cancel="close"
        />
    </span>
</template>

<script>
    import {
        RESOURCE_ACTION,
        RESOURCE_TYPE,
        TEMPLATE_RESOURCE_ACTION
    } from '@/utils/permission'
    import { pipelineTabIdMap, DRAFT_STATUS } from '@/utils/pipelineConst'
    import dayjs from 'dayjs'
    import { mapActions, mapGetters, mapState } from 'vuex'
    import DraftConfirmDialog from '@/components/PipelineHeader/DraftConfirmDialog'
    import useDraftStatus from '@/hook/useDraftStatus'

    export default {
        components: {
            DraftConfirmDialog
        },
        props: {
            outline: Boolean,
            hasPermission: {
                type: Boolean
            },
            text: {
                type: Boolean,
                default: true
            },
            theme: {
                type: String,
                default: 'primary'
            },
            version: {
                type: Number,
                required: true
            },
            versionName: {
                type: String,
                required: true
            },
            draftBaseVersionName: {
                type: String
            },
            draftVersion: {
                type: Number
            },
            projectId: {
                type: String,
                required: true
            },
            rollbackId: {
                type: String,
                required: true
            },
            draftCreator: {
                type: String,
                default: ''
            },
            draftCreateTime: {
                type: String,
                default: ''
            },
            clickActionType: {
                type: String,
                default: ''
            },
            isVersionList: Boolean,
            isActiveDraft: Boolean,
            isActiveBranchVersion: Boolean
        },
        setup () {
            const { fetchLatestDraftStatus } = useDraftStatus()
            return {
                fetchLatestDraftStatus
            }
        },
        data () {
            return {
                loading: false,
                isShowConfirmDialog: false,
                draftStatus: DRAFT_STATUS.NORMAL,
                draftSaveInfo: null,
                RESOURCE_ACTION
            }
        },
        computed: {
            ...mapState('atom', [
                'pipelineInfo'
            ]),
            ...mapGetters({
                hasDraftPipeline: 'atom/hasDraftPipeline',
                isTemplate: 'atom/isTemplate'
            }),
            resourceType () {
                return this.isTemplate ? RESOURCE_TYPE.TEMPLATE : RESOURCE_TYPE.PIPELINE
            },
            resourceAction () {
                return this.isTemplate
                    ? TEMPLATE_RESOURCE_ACTION.EDIT
                    : RESOURCE_ACTION.EDIT
            },
            isRollback () {
                const { baseVersion, releaseVersion } = (this.pipelineInfo ?? {})
                const isReleaseVersion = this.version === releaseVersion
                return !(this.isActiveDraft || baseVersion === this.version || this.isActiveBranchVersion || (isReleaseVersion && !this.hasDraftPipeline))
            },
            operateName () {
                return this.isRollback
                    ? this.$t('rollback')
                    : this.$t('edit')
            },
            draftHintTitle () {
                switch (true) {
                    case this.hasDraftPipeline && this.isActiveBranchVersion:
                        return this.$t('template.templateCoverWarning')
                    case this.hasDraftPipeline:
                        return this.$t('hasDraft')
                    default:
                        return this.$t(this.isActiveBranchVersion ? 'createBranchDraftTips' : 'createDraftTips', [this.versionName])
                }
            },
            activeBranchVersionInfo () {
                const key = this.draftBaseVersionName === this.versionName ? 'templateOutDateCoverWarningDesc' : 'templateCoverWarningDesc'
                return this.$t(`template.${key}`, [this.draftCreator, this.formatDraftCreateTime, this.draftBaseVersionName])
            },
            isTemplatePipeline () {
                return this.pipelineInfo?.instanceFromTemplate ?? false
            },
            formatDraftCreateTime () {
                return dayjs(this.draftCreateTime).format('YYYY-MM-DD HH:mm:ss')
            }
        },
        created () {
            this.DRAFT_STATUS = DRAFT_STATUS
        },
        methods: {
            ...mapActions({
                requestPipelineSummary: 'atom/requestPipelineSummary',
                requestTemplateSummary: 'atom/requestTemplateSummary',
                rollbackPipelineVersion: 'pipelines/rollbackPipelineVersion',
                rollbackTemplateVersion: 'templates/rollbackTemplateVersion',
                checkTemplatePipelineRollback: 'templates/checkTemplatePipelineRollback'
            }),
            async handleClick () {
                // 回滚操作
                if (this.isRollback) {
                    const result = await this.fetchLatestDraftStatus({
                        projectId: this.projectId,
                        id: this.rollbackId,
                        actionType: 'EDIT',
                        isTemplate: this.isTemplate,
                        pipelineInfo: this.pipelineInfo
                    })
                    
                    this.draftStatus = result.status
                    this.draftSaveInfo = result.draftSaveInfo
                    
                    this.showDraftConfirmDialog()
                    return
                }

                // 编辑操作
                await this.handleEdit()
            },

            // 处理编辑操作
            async handleEdit () {
                // 分支版本且不是当前基准版本，直接确认
                if (this.isActiveBranchVersion && this.version !== this.pipelineInfo?.baseVersion) {
                    this.showDraftConfirmDialog()
                    return
                }

                try {
                    const result = await this.fetchLatestDraftStatus({
                        projectId: this.projectId,
                        id: this.rollbackId,
                        actionType: 'EDIT',
                        isTemplate: this.isTemplate,
                        pipelineInfo: this.pipelineInfo
                    })
                    
                    this.draftStatus = result.status
                    this.draftSaveInfo = result.draftSaveInfo
                    // 无草稿冲突，直接编辑
                    if (this.draftStatus === DRAFT_STATUS.NORMAL) {
                        this.goEdit(this.draftVersion ?? this.version)
                        return
                    }
                    // 有草稿冲突，弹窗确认
                    this.showDraftConfirmDialog()
                } catch (error) {
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message || error
                    })
                }
            },

            // 检查约束模式流水线是否允许回滚
            async checkRollbackPermission () {
                const res = await this.checkTemplatePipelineRollback({
                    ...this.$route.params,
                    version: this.version
                })
                if (!res.data) {
                    this.$showTips({
                        theme: 'error',
                        message: this.$t('template.templatePipelineRollbackNotAllowedTips')
                    })
                    return false
                }
                return true
            },
            showDraftConfirmDialog () {
                this.isShowConfirmDialog = true
            },
            close () {
                this.isShowConfirmDialog = false
            },
            async rollback () {
                let rollbackVersion = ''

                if (this.isRollback) {
                    rollbackVersion = this.version
                    if (this.clickActionType) {
                        rollbackVersion = this.clickActionType === 'rollback' ? this.version : this.draftSaveInfo.releaseVersion
                    }
                } else {
                    rollbackVersion = this.draftSaveInfo.releaseVersion || this.version
                }
                try {
                    this.loading = true

                    let res

                    if (this.isTemplate) {
                        res = await this.rollbackTemplateVersion({
                            ...this.$route.params,
                            version: rollbackVersion
                        })
                        await this.requestTemplateSummary(this.$route.params)
                    } else {
                        res = await this.rollbackPipelineVersion({
                            ...this.$route.params,
                            version: rollbackVersion
                        })
                        await this.requestPipelineSummary(this.$route.params)
                    }

                    if (res.version) {
                        this.goEdit(res.version, true)
                    }
                } catch (error) {
                    this.handleError(error, {
                        projectId: this.projectId,
                        resourceType: this.resourceType,
                        resourceCode: this.rollbackId,
                        action: this.resourceAction
                    })
                } finally {
                    this.loading = false
                }
            },
            goEdit (version, rollback = false) {
                const routerName = this.isTemplate ? 'templateEdit' : 'pipelinesEdit'
                const params = {
                    ...this.$route.params,
                    version,
                }
                const query = {
                    ...(rollback || this.isRollback
                        ? {
                            type: 'rollback',
                            versionName: this.versionName,
                        } : {}
                    ),
                    ...(!this.isTemplate
                        ? {
                            tab: pipelineTabIdMap[this.$route.params.type] ?? 'pipeline'
                        } : {}
                    )
                }
                this.$router.push({
                    name: routerName,
                    params,
                    query
                })
            }
        }
    }
</script>

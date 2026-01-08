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
        <bk-dialog
            v-model="isShowConfirmDialog"
            :width="480"
            footer-position="center"
            theme="primary"
        >
            <header
                class="draft-hint-title"
                slot="header"
            >
                <i class="devops-icon icon-exclamation"></i>
                {{ draftHintTitle }}
            </header>
            <div
                v-if="hasDraftPipeline"
                class="draft-hint-content"
            >
                <p
                    v-if="isActiveBranchVersion"
                    class="is-active-branch-version"
                >
                    {{ activeBranchVersionInfo }}
                </p>
                <div v-else>
                    <div v-if="isVersionList || isRollbackBtn">{{ $t('dropDraftTips', [versionName]) }}</div>
                    <div v-else>
                        <p class="draft-info">
                            <i18n
                                path="existingDraft"
                                class="existing-draft"
                            >
                                <span>{{ draftSaveInfo?.creator }}</span>
                                <span>{{ draftSaveInfo?.createTime }}</span>
                            </i18n>
                            <VersionDiffEntry
                                style="cursor: pointer;"
                                :text="true"
                                :latest-version="draftSaveInfo?.draftVersion"
                                :version="draftSaveInfo?.releaseVersion"
                            >
                                <Logo
                                    name="diff"
                                    size="14"
                                />
                            </VersionDiffEntry>
                        </p>
                        <div
                            v-if="draftStatus === 'OUTDATED'"
                            class="is-active-branch-version"
                        >
                            <i18n path="draftBaselineIsEarlierThanCurrentVersionNotice">
                                <span>{{ draftSaveInfo?.draftVersionName }}</span>
                                <span class="earlier">{{ $t('Earlier') }}</span>
                                <span>{{ draftSaveInfo?.releaseVersionName }}</span>
                            </i18n>
                            <p>{{ $t('draftNoticeTip1') }}</p>
                            <p>{{ $t('draftNoticeTip2') }}</p>
                        </div>
                        <div
                            v-if="draftStatus === 'EXISTS'"
                            class="is-active-branch-version"
                        >{{ $t('regenerateDraftOrEditExisting') }}</div>
                    </div>
                </div>
            </div>
            <footer slot="footer">
                <bk-button
                    theme="primary"
                    @click="rollback"
                >
                    {{ $t(isActiveBranchVersion ? 'resume' : (isVersionList || isRollbackBtn) ? 'newVersion' : 'newDraft') }}
                </bk-button>
                <bk-button
                    v-if="hasDraftPipeline && !isActiveBranchVersion"
                    @click="goEdit(draftVersion)"
                >
                    {{ $t('editDraft') }}
                </bk-button>
                <bk-button @click="close">
                    {{ $t(isActiveBranchVersion ? 'cancel' : 'thinkthink') }}
                </bk-button>
            </footer>
        </bk-dialog>
    </span>
</template>

<script>
    import {
        RESOURCE_ACTION,
        RESOURCE_TYPE,
        TEMPLATE_RESOURCE_ACTION
    } from '@/utils/permission'
    import { pipelineTabIdMap } from '@/utils/pipelineConst'
    import Logo from '@/components/Logo'
    import VersionDiffEntry from '@/components/PipelineDetailTabs/VersionDiffEntry'
    import dayjs from 'dayjs'
    import { mapActions, mapGetters, mapState } from 'vuex'

    export default {
        components: {
            VersionDiffEntry,
            Logo,
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
            isVersionList: Boolean,
            // 草稿状态
            draftStatus: {
                type: String,
                default: 'NORMAL'
            },
            draftSaveInfo: {
                type: Object,
                default: () => ({})
            },
            // 是否为回滚操作
            isRollbackBtn: {
                type: Boolean,
                default: undefined
            },
            isActiveDraft: Boolean,
            isActiveBranchVersion: Boolean
        },
        data () {
            return {
                loading: false,
                isShowConfirmDialog: false,
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
                        // 版本列表和回滚按钮保持原有逻辑，编辑操作需检测是否有草稿冲突
                        return (this.isVersionList || this.isRollbackBtn) ? this.$t('hasDraftTips', [this.draftBaseVersionName]) : this.$t('hasDraft')
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
                    await this.handleRollback()
                    return
                }

                // 编辑操作
                await this.handleEdit()
            },

            // 处理回滚操作
            async handleRollback () {
                // 约束模式流水线：需要检查是否允许回滚
                if (this.isTemplatePipeline) {
                    const isAllowed = await this.checkRollbackPermission()
                    if (!isAllowed) return
                }
                this.showDraftConfirmDialog()
            },

            // 处理编辑操作
            async handleEdit () {
                // 版本列表页：分支版本且不是当前基准版本，需要确认
                if (this.isVersionList) {
                    const needConfirm = this.isActiveBranchVersion && this.version !== this.pipelineInfo?.baseVersion
                    if (needConfirm) {
                        this.showDraftConfirmDialog()
                    } else {
                        this.goEdit(this.draftVersion ?? this.version)
                    }
                    return
                }

                // 详情页：无草稿冲突，直接编辑
                if (this.draftStatus === 'NORMAL') {
                    this.goEdit(this.draftVersion ?? this.version)
                    return
                }

                // 详情页：有草稿冲突，弹窗确认
                this.showDraftConfirmDialog()
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
                try {
                    this.loading = true

                    let res

                    if (this.isTemplate) {
                        res = await this.rollbackTemplateVersion({
                            ...this.$route.params,
                            version: this.version
                        })
                        await this.requestTemplateSummary(this.$route.params)
                    } else {
                        res = await this.rollbackPipelineVersion({
                            ...this.$route.params,
                            version: this.version
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

<style lang="scss">
    .draft-hint-title {
        color: #313238;
        font-size: 20px;
        display: flex;
        flex-direction: column;
        grid-gap: 24px;
        align-items: center;
        > i {
            border-radius: 50%;
            background-color: #ffe8c3;
            color: #ff9c01;
            border-radius: 50%;
            font-size: 24px;
            height: 42px;
            line-height: 42px;
            width: 42px;
        }
    }
    .draft-hint-content {
        text-align: center;
    }
    .is-active-branch-version {
        background: #F5F6FA;
        padding: 16px 12px;
        margin: 0 8px;
        border-radius: 2px;
        text-align: left;
    }
    .draft-info {
        display: flex;
        align-items: center;
        text-align: left;
        margin: 16px;
        margin-left: 12px;
    }
    .earlier {
        color: #fe6159;
    }

    .existing-draft {
        color: #b4b4b7;
        margin-right: 10px;
        span {
            color: #313239;
        }
    }
</style>

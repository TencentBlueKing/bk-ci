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
                :class="['draft-hint-content', { 'is-active-branch-version': isActiveBranchVersion }]"
            >
                {{ draftWarningInfo }}
            </div>
            <footer slot="footer">
                <bk-button
                    theme="primary"
                    @click="rollback"
                >
                    {{ $t(isActiveBranchVersion ? 'resume' : 'newVersion') }}
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
    import dayjs from 'dayjs'
    import { mapActions, mapGetters, mapState } from 'vuex'

    export default {
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
                        return this.$t('hasDraftTips', [this.draftBaseVersionName])
                    default:
                        return this.$t(this.isActiveBranchVersion ? 'createBranchDraftTips' : 'createDraftTips', [this.versionName])
                }
            },
            draftWarningInfo () {
                if (this.isActiveBranchVersion) {
                    const key = this.draftBaseVersionName === this.versionName ? 'templateOutDateCoverWarningDesc' : 'templateCoverWarningDesc'
                    return this.$t(`template.${key}`, [this.draftCreator, this.formatDraftCreateTime, this.draftBaseVersionName])
                }
                return this.$t('dropDraftTips', [this.versionName])
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
                if (this.isRollback) {
                    if (this.isTemplatePipeline) {
                        const res = await this.checkTemplatePipelineRollback({
                            ...this.$route.params,
                            version: this.version
                        })
                        if (res.data) {
                            this.showDraftConfirmDialog()
                        } else {
                            this.$showTips({
                                theme: 'error',
                                message: this.$t('template.templatePipelineRollbackNotAllowedTips')
                            })
                        }
                    } else {
                        this.showDraftConfirmDialog()
                    }
                } else {
                    if (this.isActiveBranchVersion && this.version !== this.pipelineInfo?.baseVersion) {
                        this.showDraftConfirmDialog()
                    } else {
                        this.goEdit(this.draftVersion ?? this.version)
                    }
                }
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
        &.is-active-branch-version {
            background: #F5F6FA;
            padding: 16px 12px;
            margin: 0 8px;
            border-radius: 2px;
            text-align: left;
        }
    }
</style>

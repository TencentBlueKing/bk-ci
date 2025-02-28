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
                    resourceType: 'pipeline',
                    resourceCode: pipelineId,
                    action: RESOURCE_ACTION.EDIT
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
                {{ isActiveBranchVersion ? $t('template.templateCoverWarningDesc', [draftCreator, draftBaseVersionName]) : $t('dropDraftTips', [versionName]) }}
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
        RESOURCE_ACTION
    } from '@/utils/permission'
    import { pipelineTabIdMap } from '@/utils/pipelineConst'
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
            pipelineId: {
                type: String,
                required: true
            },
            draftCreator: {
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
                hasDraftPipeline: 'atom/hasDraftPipeline'
            }),
            isRollback () {
                const { baseVersion, releaseVersion } = (this.pipelineInfo ?? {})
                const isReleaseVersion = this.version === releaseVersion
                return !(this.isActiveDraft || baseVersion === this.version || (isReleaseVersion && !this.hasDraftPipeline))
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
                        return this.$t('createDraftTips', [this.versionName])
                }
            },
            isTemplatePipeline () {
                return this.pipelineInfo?.instanceFromTemplate ?? false
            }
        },
        methods: {
            ...mapActions({
                requestPipelineSummary: 'atom/requestPipelineSummary',
                rollbackPipelineVersion: 'pipelines/rollbackPipelineVersion'
            }),
            handleClick () {
                if (this.isRollback) {
                    if (this.isTemplatePipeline) {
                        this.$bkInfo({
                            subTitle: this.$t('templateRollbackBackTips'),
                            confirmFn: () => {
                                this.$router.push({
                                    name: 'createInstance',
                                    params: {
                                        projectId: this.projectId,
                                        templateId: this.pipelineInfo?.templateId,
                                        curVersionId: this.pipelineInfo?.templateVersion
                                    },
                                    hash: `#${this.pipelineId}`
                                })
                            }
                        })
                    } else {
                        this.showDraftConfirmDialog()
                    }
                } else {
                    if (this.isActiveBranchVersion) {
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

                    const { version } = await this.rollbackPipelineVersion({
                        ...this.$route.params,
                        version: this.version
                    })

                    await this.requestPipelineSummary(this.$route.params)

                    if (version) {
                        this.goEdit(version)
                    }
                } catch (error) {
                    this.handleError(error, {
                        projectId: this.projectId,
                        resourceCode: this.pipelineId,
                        action: this.$permissionResourceAction.EDIT
                    })
                } finally {
                    this.loading = false
                }
            },
            goEdit (version) {
                this.$router.push({
                    name: 'pipelinesEdit',
                    params: {
                        ...this.$route.params,
                        version
                    },
                    query: {
                        tab: pipelineTabIdMap[this.$route.params.type] ?? 'pipeline'
                    }
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

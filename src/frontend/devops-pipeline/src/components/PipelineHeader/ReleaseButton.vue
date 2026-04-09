<template>
    <div style="height: 100%">
        <span
            v-bk-tooltips="disableTooltips"
            :class="['publish-pipeline-btn', {
                'publish-diabled': !canRelease
            }]"
            @click="showReleaseSlider"
            v-perm="permObj"
        >
            <i class="devops-icon icon-check-small" />
            {{ $t('release') }}
        </span>
        <ReleasePipelineSideSlider
            v-model="isReleaseSliderShow"
            :version="currentVersion"
            :draft-base-version-name="draftBaseVersionName"
            :draft-status="draftStatus"
            @release-success="handleReleaseSuccess"
        />
        <bk-dialog
            v-model="isPublishedDialogShow"
            :width="480"
            :mask-close="false"
            footer-position="center"
            ext-cls="published-dialog"
        >
            <header
                class="published-hint-title"
                slot="header"
            >
                <i class="devops-icon icon-exclamation"></i>
                <span>{{ $t("alreadyPublished") }}</span>
            </header>
            <div>
                <div class="published-content">
                    <span class="label">{{ $t("publisher") }}: </span>
                    <span>{{ lasterDraftInfo?.updater || "--" }} </span>
                    <span class="label"> {{ $t("publishTime") }}: </span>
                    <span>{{ formatTime(lasterDraftInfo?.updateTime) }}</span>

                    <VersionDiffEntry
                        :class="[
                            'diff-button',
                            {
                                'develop-txt-disabled': !hasDraftPipeline
                            }
                        ]"
                        text
                        :can-switch-version="false"
                        :show-button="false"
                        :disabled="!hasDraftPipeline"
                        :version="lasterDraftInfo?.version"
                        :current-editing-data="currentEditingData"
                        :diff-mode="DRAFT_STATUS.PUBLISHED"
                    >
                        <Logo
                            name="diff"
                            size="14"
                        />
                    </VersionDiffEntry>
                    <p class="published-tips">
                        <span v-html="$t('alreadyPublishedTip')"></span>
                    </p>
                </div>
            </div>
            <footer slot="footer">
                <bk-button
                    theme="primary"
                    @click="handleNewDraft"
                >
                    {{ $t("newDraft") }}
                </bk-button>
                <bk-button @click="goPipelineModel">
                    {{ $t('exitEditing') }}
                </bk-button>
            </footer>
        </bk-dialog>
    </div>
</template>

<script>
    import {
        RESOURCE_ACTION
    } from '@/utils/permission'
    import { mapActions, mapGetters, mapState } from 'vuex'
    import ReleasePipelineSideSlider from './ReleasePipelineSideSlider'
    import Logo from '@/components/Logo'
    import VersionDiffEntry from '@/components/PipelineDetailTabs/VersionDiffEntry.vue'
    import { convertTime } from "@/utils/util"
    import { DRAFT_STATUS } from '@/utils/pipelineConst'
    
    export default {
        components: {
            Logo,
            VersionDiffEntry,
            ReleasePipelineSideSlider
        },
        props: {
            projectId: {
                type: String,
                required: true
            },
            id: {
                type: String,
                required: true
            },
            currentEditingData: {
                type: Object,
                default: null
            },
            canRelease: {
                type: Boolean,
                required: true
            }
        },
        data () {
            return {
                RESOURCE_ACTION,
                isPublishedDialogShow: false,
                lasterDraftInfo: null,
                draftStatus: null,
                isReleaseSliderShow: false
            }
        },
        computed: {
            ...mapState('atom', [
                'pipelineInfo',
                'showVariable'
            ]),
            ...mapGetters({
                draftBaseVersionName: 'atom/getDraftBaseVersionName',
                isTemplate: 'atom/isTemplate'
            }),
            disableTooltips () {
                return {
                    content: this.$t('alreadyReleasedTips'),
                    disabled: this.canRelease
                }
            },
            canEdit () {
                return this.pipelineInfo?.permissions?.canEdit ?? true
            },
            currentVersion () {
                return this.pipelineInfo?.version ?? ''
            },
            releaseVersion () {
                return this.pipelineInfo?.releaseVersion ?? ''
            },
            hasDraftPipeline () {
                return (this.pipelineInfo?.version !== this.pipelineInfo?.releaseVersion) ?? false
            },
            permObj () {
                return {
                    hasPermission: this.canEdit,
                    disablePermissionApi: true,
                    permissionData: {
                        projectId: this.projectId,
                        resourceType: this.isTemplate ? 'template' : 'pipeline',
                        resourceCode: this.id,
                        action: RESOURCE_ACTION.EDIT
                    }
                }
            }
        },
        created () {
            this.DRAFT_STATUS = DRAFT_STATUS
        },
        methods: {
            ...mapActions('atom', [
                'requestPipelineSummary',
                'requestTemplateSummary',
                'requestPipeline',
                'setShowVariable'
            ]),
            ...mapActions('common', [
                'getDraftStatus',
                'getTemplateDraftStatus'
            ]),
            formatTime (time) {
                return convertTime(time)
            },
            async showReleaseSlider () {
                if (!this.canRelease) {
                    return
                }
                
                try {
                    // 检查草稿状态
                    const request = this.isTemplate ? this.getTemplateDraftStatus : this.getDraftStatus
                    const params = {
                        projectId: this.projectId,
                        actionType: 'RELEASE',
                        ...(this.pipelineInfo?.draftVersion ? {
                            version: this.pipelineInfo?.version,
                            baseDraftVersion: this.pipelineInfo?.draftVersion,
                        } : {}),
                        ...(this.isTemplate ? { templateId: this.id } : { pipelineId: this.id })
                    }
                    const draftStatus = await request(params)
                    this.draftStatus = draftStatus
                    
                    // 如果状态是 PUBLISHED，显示已发布弹窗
                    if (draftStatus.status === DRAFT_STATUS.PUBLISHED) {
                        this.lasterDraftInfo = draftStatus.release
                        this.isPublishedDialogShow = true
                    } else {
                        // 否则显示发布侧边栏
                        this.setShowVariable(false)
                        this.isReleaseSliderShow = true
                    }
                } catch (error) {
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message || error
                    })
                }
            },
            async handleNewDraft () {
                this.isPublishedDialogShow = false
                // 重新获取流水线摘要信息
                if (this.isTemplate) {
                    await this.requestTemplateSummary(this.$route.params)
                } else {
                    await this.requestPipelineSummary(this.$route.params)
                }
                await this.requestPipeline({
                    source: 'EDIT',
                    projectId: this.projectId,
                    ...(this.isTemplate ? { templateId: this.id } : {pipelineId: this.id}),
                    version: this.pipelineInfo?.version
                })
            },
            goPipelineModel () {
                const routerName = this.isTemplate ? 'TemplateOverview' : 'pipelinesHistory'
                this.$router.push({
                    name: routerName,
                    params: {
                        ...this.$route.params,
                        version: this.pipelineInfo?.releaseVersion,
                        type: 'pipeline'
                    },
                    query: this.$route.query
                })
            },
            handleReleaseSuccess () {
                this.$emit('release-success')
            }
        }
    }
</script>

<style lang="scss">
@import "@/scss/conf";

.publish-pipeline-btn {

    display: flex;
    height: 100%;
    padding: 0 20px;
    background: $primaryColor;
    align-items: center;
    color: white;
    cursor: pointer;
    font-size: 14px;

    &.publish-diabled {
        background: #DCDEE5;
        color: white !important;
        cursor: not-allowed;
    }

    .icon-check-small {
        font-size: 18px;
    }
}
.published-dialog {
    .published-hint-title {
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
    .published-content {
        font-size: 14px;
        color: #313238;
        .label {
            color: #b4b4b7;
        }
        .diff-button {
            cursor: pointer;
            margin-left: 16px;
        }
    }
    .published-tips {
        padding: 12px 16px;
        margin-top: 16px;
        background: #F5F6FA;
        border-radius: 2px;
        color: #4d4f56;
        font-size: 14px;
    }
}
.develop-txt-disabled {
    cursor: not-allowed;
    color: #c4c6cc;
}
</style>

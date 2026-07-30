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
            :current-editing-data="currentEditingData"
            @release-success="handleReleaseSuccess"
            @save-draft="handleSaveDraft"
            @re-save-draft="handleReSaveDraft"
            @close-slider="handleCloseSlider"
        />
        <ReleaseConflictDialog
            v-model="isPublishedDialogShow"
            :release-status="draftStatus?.status"
            :laster-draft-info="lasterDraftInfo"
            :current-editing-data="currentEditingData"
            @new-draft="handleNewDraft"
            @save-draft="handleSaveDraft"
            @re-save-draft="handleReSaveDraft"
            @go-pipeline-model="goPipelineModel"
        />
    </div>
</template>

<script>
    import {
        RESOURCE_ACTION
    } from '@/utils/permission'
    import { mapActions, mapGetters, mapState } from 'vuex'
    import ReleasePipelineSideSlider from './ReleasePipelineSideSlider'
    import ReleaseConflictDialog from './ReleaseConflictDialog'
    import { DRAFT_STATUS } from '@/utils/pipelineConst'
    
    export default {
        components: {
            ReleasePipelineSideSlider,
            ReleaseConflictDialog
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
            async showReleaseSlider () {
                if (!this.canRelease) {
                    return
                }
                
                try {
                    // 检查草稿状态
                    const request = this.isTemplate ? this.getTemplateDraftStatus : this.getDraftStatus
                    const { releaseVersion, version, draftVersion, versionStatus } = this.pipelineInfo ?? {}
                    const params = {
                        projectId: this.projectId,
                        actionType: 'RELEASE',
                        version,
                        versionStatus,
                        releaseVersion,
                        baseDraftVersion: draftVersion,
                        ...(this.isTemplate ? { templateId: this.id } : { pipelineId: this.id })
                    }
                    const draftStatus = await request(params)
                    this.draftStatus = draftStatus
                    
                    // 已发布 / 冲突 / 已删除时显示冲突弹窗
                    if ([DRAFT_STATUS.PUBLISHED, DRAFT_STATUS.CONFLICT, DRAFT_STATUS.DELETED].includes(draftStatus.status)) {
                        this.lasterDraftInfo = draftStatus.status === DRAFT_STATUS.PUBLISHED ? draftStatus.release : draftStatus.draft
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
                this.handleReleaseSuccess()
                await this.requestPipeline({
                    source: 'EDIT',
                    projectId: this.projectId,
                    ...(this.isTemplate ? { templateId: this.id } : {pipelineId: this.id}),
                    version: this.pipelineInfo?.version
                })
            },
            async goPipelineModel () {
                const routerName = this.isTemplate ? 'TemplateOverview' : 'pipelinesHistory'
                if (this.isTemplate) {
                    await this.requestTemplateSummary(this.$route.params)
                }
                this.$router.push({
                    name: routerName,
                    params: {
                        ...this.$route.params,
                        version: this.pipelineInfo?.releaseVersion,
                        type: 'pipeline'
                    },
                    ...(this.isTemplate ? {} : { query: this.$route.query })
                })
            },
            handleReleaseSuccess () {
                this.$emit('release-success')
            },
            handleSaveDraft () {
                this.isPublishedDialogShow = false
                this.$emit('save-draft')
            },
            handleReSaveDraft () {
                this.isPublishedDialogShow = false
                this.$emit('re-save-draft')
            },
            handleCloseSlider () {
                this.isReleaseSliderShow = false
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
</style>

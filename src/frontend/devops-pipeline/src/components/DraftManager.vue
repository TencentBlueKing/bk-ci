<template>
    <div class="draft-manager">
        <!-- 草稿列表部分 -->
        <div
            class="draft-content"
            v-if="draftList.length > 0"
        >
            <p
                class="last-save-time"
                @click="handleShowDraftList"
            >
                <span>{{ $t("lastSaveTime") }}：</span>
                {{ formatTime(draftList[0]?.updateTime) }} {{ draftList[0]?.updater }}
                <i :class="['bk-icon', `icon-angle-${isShowDraftList ? 'up' : 'down'}`]" />
            </p>

            <ul
                ref="draftListRef"
                class="draft-list"
                v-if="isShowDraftList"
                @scroll="handleScroll"
            >
                <li>
                    <i class="bk-icon icon-info-circle tips-icon" />
                    {{ $t('draftsClearedAfterVersionRelease') }}
                </li>
                <li
                    v-for="(item, index) in draftList"
                    :key="item.draftVersion"
                    :class="['draft-item', item.draftVersion === draftList[0]?.draftVersion ? 'draft-item-active' : '']"
                >
                    <p>
                        <span class="update-info">{{ formatTime(item.updateTime) }} {{ item.updater }}</span>
                        <span
                            class="version-name"
                            v-if="item.baseVersionName"
                        >{{ $t('basedOn', [item.baseVersionName]) }}</span>
                    </p>
                    <span class="options">
                        <span
                            v-if="index === 0"
                            class="update-tip"
                        >{{ $t('lastSaveTime') }}</span>
                        <VersionDiffEntry
                            :class="['diff-button',{
                                'develop-txt-disabled': !hasDraftPipeline
                            }]"
                            :text="true"
                            :disabled="!hasDraftPipeline"
                            :version="releaseVersion"
                            :latest-version="item.version"
                            :draft-version="item.draftVersion"
                        >
                            <Logo
                                name="diff"
                                size="14"
                            />
                        </VersionDiffEntry>
                        <span
                            v-if="index !== 0"
                            class="rollback-icon rollback-button"
                            @click.stop="handleRollback(item)"
                        >
                            <Logo
                                name="refresh"
                                size="12"
                                style="transform: scaleX(-1) rotate(35deg);"
                            />
                        </span>
                    </span>
                </li>
                <li
                    v-if="loading"
                    v-bkloading="{ isLoading: loading }"
                    class="loading-item"
                >
                </li>
            </ul>
        </div>

        <!-- 冲突对话框部分 -->
        <bk-dialog
            v-model="value"
            :width="480"
            :mask-close="false"
            footer-position="center"
            ext-cls="draft-manager-dialog"
            @cancel="handleClose"
        >
            <header
                class="draft-hint-title"
                slot="header"
            >
                <i class="devops-icon icon-exclamation"></i>
                <span>{{ dialogTitle }}</span>
            </header>
            <div>
                <div
                    class="conflict-draft"
                    v-if="isConflictStatus"
                >
                    <span class="label">{{ $t('conflictingDraft') }}: </span>
                    <span>{{ conflictDraftInfo?.updater }} </span>
                    <span class="label"> {{ $t('savedAt') }}: </span>
                    <span>{{ formatTime(conflictDraftInfo?.updateTime) }}</span>

                    <VersionDiffEntry
                        :class="['diff-button',{
                            'develop-txt-disabled': !hasDraftPipeline
                        }]"
                        :text="true"
                        :can-switch-version="false"
                        :disabled="!hasDraftPipeline"
                        :version="conflictDraftInfo?.version"
                        :show-button="false"
                        :draft-version="conflictDraftInfo.draftVersion"
                        :current-editing-data="currentEditingData"
                        :diff-mode="DRAFT_STATUS.CONFLICT"
                    >
                        <Logo
                            name="diff"
                            size="14"
                        />
                    </VersionDiffEntry>
                </div>
                <div
                    class="conflict-draft"
                    v-else-if="isPublishedStatus"
                >
                    <span class="label">{{ $t('publisher') }}: </span>
                    <span>{{ publishedInfo?.updater }} </span>
                    <span class="label"> {{ $t('publishTime') }}: </span>
                    <span>{{ formatTime(publishedInfo?.updateTime) }}</span>

                    <VersionDiffEntry
                        :class="['diff-button',{
                            'develop-txt-disabled': !hasDraftPipeline
                        }]"
                        :text="true"
                        :can-switch-version="false"
                        :show-button="false"
                        :disabled="!hasDraftPipeline"
                        :version="publishedInfo?.version"
                        :current-editing-data="currentEditingData"
                        :diff-mode="DRAFT_STATUS.PUBLISHED"
                    >
                        <Logo
                            name="diff"
                            size="14"
                        />
                    </VersionDiffEntry>
                </div>

                <p class="conflict-draft-tips">
                    <span
                        v-if="isConflictStatus"
                    >
                        {{ tipsText }}
                    </span>
                    <span
                        v-else-if="isPublishedStatus"
                        v-html="tipsText"
                    ></span>
                </p>
            </div>
            <footer slot="footer">
                <bk-button
                    theme="primary"
                    :loading="saveStatus"
                    @click="handleContinueSaveDraft"
                >
                    {{ primaryButtonText }}
                </bk-button>
                <bk-button @click="goPipelineModel">
                    {{ secondaryButtonText }}
                </bk-button>
                <bk-button
                    v-if="isConflictStatus"
                    @click="handleClose"
                >
                    {{ $t('returnToEditing') }}
                </bk-button>
            </footer>
        </bk-dialog>
    </div>
</template>

<script>
    import { mapState, mapActions } from 'vuex'
    import { convertTime } from '@/utils/util'
    import Logo from '@/components/Logo'
    import { DRAFT_STATUS } from '@/utils/pipelineConst'
    import VersionDiffEntry from '@/components/PipelineDetailTabs/VersionDiffEntry.vue'

    export default {
        name: 'DraftManager',
        components: {
            VersionDiffEntry,
            Logo
        },
        props: {
            // 最新草稿信息
            lasterDraftInfo: {
                type: Object,
                default: () => ({})
            },
            // 是否显示冲突对话框
            value: {
                type: Boolean,
                default: false
            },
            // 当前流水线最新版本
            releaseVersion: {
                type: Number,
                request: true
            },
            // 项目ID
            projectId: {
                type: String,
                required: true
            },
            // 流水线ID或模板ID
            uniqueId: {
                type: String,
                required: true
            },
            // 是否为模板
            isTemplate: {
                type: Boolean,
                default: false
            },
            // 当前正在编辑的数据（用于对比）
            currentEditingData: {
                type: Object,
                default: null
            }
        },
        data () {
            return {
                isShowDraftList: false,
                draftList: [],
                loading: false,
                currentPage: 1,
                pageSize: 10,
                hasMore: true
            }
        },
        computed: {
            ...mapState('atom', [
                'pipelineInfo',
                'saveStatus'
            ]),
            hasDraftPipeline () {
                return (this.pipelineInfo?.version !== this.pipelineInfo?.releaseVersion) ?? false
            },
            // 冲突状态相关的computed
            isConflictStatus () {
                return this.lasterDraftInfo?.status === DRAFT_STATUS.CONFLICT
            },
            isPublishedStatus () {
                return this.lasterDraftInfo?.status === DRAFT_STATUS.PUBLISHED
            },
            conflictDraftInfo () {
                return this.lasterDraftInfo?.draft
            },
            publishedInfo () {
                return this.lasterDraftInfo?.release
            },
            dialogTitle () {
                if (this.isConflictStatus) {
                    return this.$t('otherUserEditingDetected')
                } else if (this.isPublishedStatus) {
                    return this.$t('alreadyPublished')
                }
                return ''
            },
            primaryButtonText () {
                if (this.isConflictStatus) {
                    return this.$t('continueSaving')
                } else if (this.isPublishedStatus) {
                    return this.$t('newDraft')
                }
                return ''
            },
            secondaryButtonText () {
                if (this.isConflictStatus) {
                    return this.$t('discardChanges')
                } else if (this.isPublishedStatus) {
                    return this.$t('exitEditing')
                }
                return ''
            },
            tipsText () {
                if (this.isConflictStatus) {
                    return this.$t('reviewDifferencesAndOverrideChanges')
                } else if (this.isPublishedStatus) {
                    return this.$t('alreadyPublishedTip')
                }
                return ''
            }
        },
        watch: {
            'pipelineInfo.version': {
                handler (newVal, oldVal) {
                    if (newVal && newVal !== oldVal) {
                        this.resetAndFetch()
                    }
                },
                immediate: true
            }
        },
        created () {
            this.DRAFT_STATUS = DRAFT_STATUS
        },
        mounted () {
            // 监听全局点击事件
            document.addEventListener('click', this.handleGlobalClick)
        },
        beforeDestroy () {
            // 移除全局点击事件监听
            document.removeEventListener('click', this.handleGlobalClick)
        },
        methods: {
            ...mapActions({
                getDraftVersion: 'common/getDraftVersion',
                getTemplateDraftVersion: 'common/getTemplateDraftVersion'
            }),
            formatTime (value) {
                return convertTime(value)
            },
            async handleShowDraftList () {
                this.isShowDraftList = !this.isShowDraftList
            },
            // 重置并重新获取数据
            async resetAndFetch () {
                this.currentPage = 1
                this.hasMore = true
                this.draftList = []
                await this.loadDraftList()
            },
            // 加载草稿列表
            async loadDraftList (isLoadMore = false) {
                if (this.loading || (!isLoadMore && !this.hasMore)) {
                    return
                }

                try {
                    this.loading = true
                    const params = {
                        projectId: this.projectId,
                        [this.isTemplate ? 'templateId' : 'pipelineId']: this.uniqueId,
                        version: this.pipelineInfo?.version,
                        page: this.currentPage,
                        pageSize: this.pageSize
                    }

                    const action = this.isTemplate ? this.getTemplateDraftVersion : this.getDraftVersion
                    const res = await action(params)

                    const newRecords = res.records || []
                    
                    if (isLoadMore) {
                        this.draftList = [...this.draftList, ...newRecords]
                    } else {
                        this.draftList = newRecords
                    }

                    // 判断是否还有更多数据
                    this.hasMore = newRecords.length === this.pageSize && this.draftList.length < (res.count || 0)
                } catch (error) {
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message ?? error
                    })
                } finally {
                    this.loading = false
                }
            },
            // 滚动加载
            handleScroll (event) {
                const { scrollTop, scrollHeight, clientHeight } = event.target
                const threshold = 50 // 距离底部50px时触发加载
                
                if (scrollHeight - scrollTop - clientHeight < threshold && this.hasMore && !this.loading) {
                    this.currentPage++
                    this.loadDraftList(true)
                }
            },
            handleGlobalClick (event) {
                // 检查点击的元素是否在特定的CSS类中
                const isInExcludedElement = event.target.closest('.diff-button')
                    || event.target.closest('.rollback-button')
                    || event.target.closest('.bk-dialog')
                    || event.target.closest('.last-save-time')
                    || event.target.closest('.draft-list')
                    || event.target.closest('.bk-select-dropdown-content')

                if (this.isShowDraftList && !isInExcludedElement) {
                    this.isShowDraftList = false
                }
            },
            handleRollback (item) {
                this.$emit('rollback', item)
                this.isShowDraftList = false
            },
            handleContinueSaveDraft () {
                // 已发布状态：跳转到编辑页面
                if (this.isPublishedStatus) {
                    this.$emit('new-draft')
                    return
                }
                
                // 冲突状态：触发父组件的保存草稿逻辑，覆盖冲突的草稿
                this.$emit('continue-save-draft')
            },
            goPipelineModel () {
                this.$emit('go-pipeline-model')
            },
            handleClose () {
                this.$emit('input', false)
            },
            // 供父组件调用的刷新方法
            async refresh () {
                await this.resetAndFetch()
            }
        }
    }
</script>

<style lang="scss" scoped>
.draft-manager {
    .draft-content {
        position: relative;
        font-size: 12px;
        .last-save-time {
            display: flex;
            align-items: center;
            color: #979BA5;
            cursor: pointer;
            i {
                font-size: 18px;
            }
        }
        .draft-list {
            position: absolute;
            z-index: 2019;
            top: 22px;
            left: 50%;
            transform: translateX(-50%);
            width: 450px;
            max-height: 350px;
            overflow-y: auto;
            background-color: #fff;
            border: 1px solid #DCDEE5;
            box-shadow: 0 2px 6px 0 #0000001a;
            border-radius: 2px;
            .tips-icon {
                font-size: 15px;
            }
            li {
                height: 32px;
                padding: 8px 12px;
                &:first-child {
                    color: #979BA5;
                }
            }
            .draft-item {
                display: flex;
                align-items: center;
                justify-content: space-between;
                cursor: pointer;
                color: #4D4F56;
                &:hover {
                    background: #F5F7FA;
                }
                &:hover .options {
                    visibility: inherit;
                }
                .version-name {
                    background-color: #F0F1F5;
                    border-radius: 2px;
                    padding: 0 8px;
                }
            }
            .draft-item-active {
                background: #E1ECFF;
                .update-info {
                    color: #3A84FF;
                    margin-right: 4px;
                }
            }
            .options {
                display: flex;
                align-items: center;
                margin-top: 2px;
                .rollback-icon {
                    line-height: 22px;
                    color: #979BA5;
                    margin-left: 12px;
                }
            }
            .update-tip {
                padding: 0 4px;
                background: #FDEED8;
                border-radius: 2px;
                margin-right: 12px;
                color: #E38B02;
            }
            .loading-item {
                display: flex;
                align-items: center;
                justify-content: center;
                color: #979BA5;
                font-size: 12px;
                height: 36px;
            }
        }
    }
}
</style>

<style lang="scss">
.draft-manager-dialog {
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
    .conflict-draft {
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
    .conflict-draft-tips {
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
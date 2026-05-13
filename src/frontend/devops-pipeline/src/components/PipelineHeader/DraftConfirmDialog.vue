<template>
    <bk-dialog
        v-model="value"
        :width="480"
        footer-position="center"
        theme="primary"
        @cancel="close"
    >
        <header
            class="draft-hint-title"
            slot="header"
        >
            <i class="devops-icon icon-exclamation"></i>
            {{ draftHintTitle }}
        </header>
        <!-- 分支版本有草稿且版本与基线版本不同时提示 -->
        <div
            class="draft-hint-content"
            v-if="hasDraftPipeline && isActiveBranchVersion && version !== pipelineInfo?.baseVersion"
        >
            <p class="is-active-branch-version">
                {{ activeBranchVersionInfo }}
            </p>
        </div>
        <!-- 草稿状态 -->
        <div
            v-else-if="hasDraft && !isActiveBranchVersion"
            class="draft-hint-content"
        >
            <p
                class="draft-info"
            >
                <i18n
                    path="existingDraft"
                    class="existing-draft"
                >
                    <span>{{ draftSaveInfo?.updater }}</span>
                    <span>{{ draftSaveInfo?.updateTime }}</span>
                </i18n>
                <VersionDiffEntry
                    style="cursor: pointer;"
                    text
                    :latest-version="draftSaveInfo?.draftVersion"
                    :version="draftSaveInfo?.releaseVersion"
                >
                    <Logo
                        name="diff"
                        size="14"
                    />
                </VersionDiffEntry>
            </p>
            <!-- 回滚提示 -->
            <div
                v-if="showRollbackTips"
                class="is-active-branch-version"
            >
                {{ $t('rollbackTips', [versionName]) }}
            </div>
            <!-- 编辑提示 -->
            <template v-else>
                <!-- 版本落后 -->
                <div
                    v-if="draftStatus === DRAFT_STATUS.OUTDATED"
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
                <!-- 草稿存在状态 -->
                <div
                    v-if="draftStatus === DRAFT_STATUS.EXISTS"
                    class="is-active-branch-version"
                >
                    {{ $t('regenerateDraftOrEditExisting') }}
                </div>
            </template>
        </div>
        <!-- 基线版本落后状态 和 已发布状态 -->
        <div v-else-if="(clickActionType === 'edit' || !isRollback) && (draftStatus === DRAFT_STATUS.RELEASE_OUTDATED || draftStatus === DRAFT_STATUS.PUBLISHED)">
            <p
                class="draft-info"
            >
                <i18n
                    path="updatedPipelineVersionDesc"
                    class="existing-draft"
                >
                    <span>{{ draftSaveInfo?.releaseUpdater }}</span>
                    <span>{{ draftSaveInfo?.releaseUpdateTime }}</span>
                </i18n>
            </p>
            <div
                class="is-active-branch-version"
            >
                {{ $t('pipelineUpdatedNotice', [versionName, draftSaveInfo?.releaseVersionName]) }}
            </div>
        </div>
        <footer slot="footer">
            <bk-button
                theme="primary"
                @click="handleConfirm"
            >
                {{ showBtnText }}
            </bk-button>
            <bk-button
                v-if="hasDraft && !isActiveBranchVersion"
                @click="goEdit(draftVersion)"
            >
                {{ $t('editDraft') }}
            </bk-button>
            <bk-button @click="close">
                {{ $t(isActiveBranchVersion ? 'cancel' : 'thinkthink') }}
            </bk-button>
        </footer>
    </bk-dialog>
</template>

<script>
    import Logo from '@/components/Logo'
    import VersionDiffEntry from '@/components/PipelineDetailTabs/VersionDiffEntry'
    import { DRAFT_STATUS } from '@/utils/pipelineConst'
    import { mapState, mapGetters } from 'vuex'

    export default {
        name: 'DraftConfirmDialog',
        components: {
            Logo,
            VersionDiffEntry
        },
        props: {
            // 控制弹窗显示
            value: {
                type: Boolean,
                default: false
            },
            // 是否有草稿
            hasDraft: {
                type: Boolean,
                default: false
            },
            // 草稿状态
            draftStatus: {
                type: String,
                default: DRAFT_STATUS.NORMAL
            },
            // 草稿保存信息
            draftSaveInfo: {
                type: Object,
                default: () => ({
                    updater: '',
                    updateTime: '',
                    draftVersionName: '',
                    draftVersion: null,
                    releaseVersionName: '',
                    releaseVersion: null,
                    releaseUpdater: '',
                    releaseUpdateTime: ''
                })
            },
            // 弹窗标题
            draftHintTitle: {
                type: String,
                default: ''
            },
            // 活跃分支版本信息文案
            activeBranchVersionInfo: {
                type: String,
                default: ''
            },
            // 版本名称
            versionName: {
                type: String,
                default: ''
            },
            // 是否为活跃分支版本
            isActiveBranchVersion: {
                type: Boolean,
                default: false
            },
            // 是否是模板流水线
            isTemplatePipeline: {
                type: Boolean,
                default: false
            },
            // 是否是回滚场景
            isRollback: {
                type: Boolean,
                default: false
            },
            // 草稿版本号
            draftVersion: {
                type: Number,
                default: null
            },
            // 当前版本号
            version: {
                type: Number,
                default: null
            },
            // 区分实力流水线点击的按钮类型：edit(编辑)/rollback(回滚)
            clickActionType: {
                type: String,
                default: ''
            }
        },
        data () {
            return {
                DRAFT_STATUS
            }
        },
        computed: {
            ...mapState('atom', [
                'pipelineInfo'
            ]),
            ...mapGetters({
                hasDraftPipeline: 'atom/hasDraftPipeline'
            }),
            // 是否显示回滚提示
            showRollbackTips () {
                if (this.isTemplatePipeline && this.clickActionType) {
                    return this.clickActionType === 'rollback' && this.isRollback
                }
                if (this.isRollback && this.clickActionType === 'edit') {
                    return false
                }
                // 普通流水线：直接根据 isRollback 判断
                return this.isRollback
            },
            showBtnText () {
                if (this.isActiveBranchVersion) {
                    return this.$t('resume')
                } else if (this.showRollbackTips) {
                    return this.$t('rollbackConfirm')
                }
                return this.$t('newDraft')
            }
        },
        methods: {
            // 根据按钮类型处理确认操作
            handleConfirm () {
                // 新建草稿：回滚到当前最新版本
                if (!this.isActiveBranchVersion && !this.showRollbackTips) {
                    // 有草稿的，需要回滚后跳最新版本的编辑页；
                    // 没有草稿，直接跳最新版本的编辑页；
                    if (this.hasDraft) {
                        this.$emit('confirm', this.draftSaveInfo?.releaseVersion, 'newDraft')
                    } else {
                        this.$emit('edit-draft', this.draftSaveInfo?.releaseVersion)
                    }
                    return
                }
                // 确定回滚 或 继续：触发回滚操作，传递当前版本号
                this.$emit('confirm', this.version)
            },
            // 编辑原草稿
            goEdit (version) {
                this.$emit('edit-draft', version)
            },
            close () {
                this.$emit('cancel')
                this.$emit('input', false)
            }
        }
    }
</script>

<style lang="scss" scoped>
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
        ::v-deep span {
            color: #313239;
        }
    }
</style>

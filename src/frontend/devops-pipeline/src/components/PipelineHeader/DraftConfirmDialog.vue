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
                    <div
                        v-if="draftStatus === DRAFT_STATUS.EXISTS"
                        class="is-active-branch-version"
                    >
                        {{ $t('regenerateDraftOrEditExisting') }}
                    </div>
                </template>
            </div>
        </div>
        <footer slot="footer">
            <bk-button
                theme="primary"
                @click="rollback"
            >
                {{ $t(isActiveBranchVersion ? 'resume' : showRollbackTips ? 'rollbackConfirm' : 'newDraft') }}
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
</template>

<script>
    import Logo from '@/components/Logo'
    import VersionDiffEntry from '@/components/PipelineDetailTabs/VersionDiffEntry'
    import { DRAFT_STATUS } from '@/utils/pipelineConst'

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
            // 是否有草稿流水线
            hasDraftPipeline: {
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
                    releaseVersion: null
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
            // 是否显示回滚提示
            showRollbackTips () {
                if (this.isTemplatePipeline && this.clickActionType) {
                    return this.clickActionType === 'rollback' && this.isRollback
                }
                // 普通流水线：直接根据 isRollback 判断
                return this.isRollback
            }
        },
        methods: {
            rollback () {
                this.$emit('confirm')
            },
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

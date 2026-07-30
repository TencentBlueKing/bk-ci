<template>
    <bk-dialog
        v-model="visible"
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
            <span>{{ publishedDialogTitle }}</span>
        </header>
        <div>
            <div class="published-content">
                <span
                    v-if="releaseStatus !== DRAFT_STATUS.DELETED"
                    class="label"
                >{{ releaseStatus === DRAFT_STATUS.CONFLICT ? $t('conflictingDraft') : $t('publisher') }}: </span>
                <span>{{ lasterDraftInfo?.updater || "--" }}</span>
                <span class="label">{{ timeLabel }}: </span>
                <span>{{ formatTime(lasterDraftInfo?.updateTime) }}</span>

                <VersionDiffEntry
                    v-if="releaseStatus === DRAFT_STATUS.CONFLICT"
                    :class="[
                        'diff-button'
                    ]"
                    text
                    :can-switch-version="false"
                    :show-button="false"
                    :version="lasterDraftInfo?.version"
                    :current-editing-data="currentEditingData"
                    :diff-mode="DRAFT_STATUS.CONFLICT"
                >
                    <Logo
                        name="diff"
                        size="14"
                    />
                </VersionDiffEntry>
                <p class="published-tips">
                    <span v-html="publishedDialogContent"></span>
                </p>
            </div>
        </div>
        <footer slot="footer">
            <bk-button
                v-if="[DRAFT_STATUS.CONFLICT, DRAFT_STATUS.PUBLISHED].includes(releaseStatus)"
                theme="primary"
                @click="handleNewDraft"
            >
                {{ releaseStatus === DRAFT_STATUS.CONFLICT ? $t("editPage.atomForm.reflash") : $t("newDraft") }}
            </bk-button>
            <bk-button
                v-if="[DRAFT_STATUS.CONFLICT, DRAFT_STATUS.DELETED].includes(releaseStatus)"
                :theme="releaseStatus === DRAFT_STATUS.CONFLICT ? 'default' : 'primary'"
                @click="handleSaveDraft"
            >
                {{ $t("saveDraft") }}
            </bk-button>
            <bk-button @click="goPipelineModel">
                {{ releaseStatus === DRAFT_STATUS.CONFLICT ? $t('thinkthink') : $t('exitEditing') }}
            </bk-button>
        </footer>
    </bk-dialog>
</template>

<script>
    import Logo from '@/components/Logo'
    import VersionDiffEntry from '@/components/PipelineDetailTabs/VersionDiffEntry.vue'
    import { DRAFT_STATUS } from '@/utils/pipelineConst'
    import { convertTime } from '@/utils/util'

    export default {
        name: 'ReleaseConflictDialog',
        components: {
            Logo,
            VersionDiffEntry
        },
        props: {
            value: {
                type: Boolean,
                default: false
            },
            releaseStatus: {
                type: String,
                default: ''
            },
            lasterDraftInfo: {
                type: Object,
                default: null
            },
            currentEditingData: {
                type: Object,
                default: null
            }
        },
        computed: {
            visible: {
                get () {
                    return this.value
                },
                set (val) {
                    this.$emit('input', val)
                }
            },
            publishedDialogTitle () {
                if (this.releaseStatus === DRAFT_STATUS.DELETED) {
                    return this.$t('draftHasBeenDeleted')
                }
                if (this.releaseStatus === DRAFT_STATUS.CONFLICT) {
                    return this.$t('hasConflictDraft')
                }
                return this.$t('alreadyPublished')
            },
            publishedDialogContent () {
                if (this.releaseStatus === DRAFT_STATUS.DELETED) {
                    return this.$t('draftDeletedTip')
                }
                if (this.releaseStatus === DRAFT_STATUS.CONFLICT) {
                    return this.$t('confirmDiffContent')
                }
                return this.$t('alreadyPublishedTip')
            },
            timeLabel () {
                switch (this.releaseStatus) {
                    case DRAFT_STATUS.CONFLICT:
                        return this.$t('savedAt')
                    case DRAFT_STATUS.DELETED:
                        return this.$t('deletedAt')
                    default:
                        return this.$t('publishTime')
                }
            }
        },
        created () {
            this.DRAFT_STATUS = DRAFT_STATUS
        },
        methods: {
            formatTime (time) {
                return convertTime(time)
            },
            handleNewDraft () {
                this.$emit('new-draft')
            },
            handleSaveDraft () {
                if (this.releaseStatus === DRAFT_STATUS.DELETED) {
                    this.$emit('re-save-draft')
                    return
                }
                this.$emit('save-draft')
            },
            goPipelineModel () {
                if (this.releaseStatus === DRAFT_STATUS.CONFLICT) {
                    this.visible = false
                    return
                }
                this.$emit('go-pipeline-model')
            }
        }
    }
</script>

<style lang="scss">
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

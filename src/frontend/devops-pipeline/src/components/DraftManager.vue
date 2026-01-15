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
                {{ lasterDraftInfo?.updater }} {{ formatTime(lasterDraftInfo?.updateTime) }}
                <i :class="['bk-icon', `icon-angle-${isShowDraftList ? 'up' : 'down'}`]" />
            </p>

            <ul
                class="draft-list"
                v-if="isShowDraftList"
            >
                <li>
                    <i class="bk-icon icon-info-circle tips-icon" />
                    {{ $t('draftsClearedAfterVersionRelease') }}
                </li>
                <li
                    v-for="(item, index) in draftList"
                    :key="item.draftVersion"
                    :class="['draft-item', item.draftVersion === lasterDraftInfo?.draftVersion ? 'draft-item-active' : '']"
                >
                    <p>
                        <span class="version-name">{{ $t('basedOn', item.baseVersion) }}</span>
                        <span class="update-info">{{ item.updater }} {{ formatTime(item.updateTime) }}</span>
                    </p>
                    <span
                        v-if="index !== 0"
                        class="options"
                    >
                        <VersionDiffEntry
                            style="cursor: pointer;"
                            :text="true"
                            :base-yaml="currentEditYaml"
                            :draft-yaml="draftYaml"
                            :draft-info="item"
                            @click.native.stop="handleDiff(item.draftVersion)"
                            class="diff-button"
                        >
                            <Logo
                                name="diff"
                                size="14"
                            />
                        </VersionDiffEntry>
                        <span
                            class="rollback-icon rollback-button"
                            @click.stop="handleRollback(item)"
                        >
                            <Logo
                                name="refresh"
                                size="14"
                            />
                        </span>
                    </span>
                    <span
                        v-else
                        class="update-tip"
                    >{{ $t('lastSaveTime') }}</span>
                </li>
            </ul>
        </div>

        <!-- 冲突对话框部分 -->
        <bk-dialog
            v-model="value"
            :width="480"
            :mask-close="false"
            footer-position="center"
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
                    <span>{{ conflictDraftInfo?.updateTime }}</span>

                    <VersionDiffEntry
                        style="cursor: pointer;"
                        :text="true"
                        :base-yaml="currentEditYaml"
                        :draft-yaml="draftYaml"
                        :draft-info="conflictDraftInfo"
                        @click.native.stop="handleDiff(conflictDraftInfo?.draftVersion)"
                        class="diff-button"
                    >
                        <Logo
                            name="diff"
                            size="14"
                        />
                    </VersionDiffEntry>
                </div>
                <div v-else-if="isPublishedStatus">
                    <span class="label">{{ $t('publisher') }}: </span>
                    <span>{{ publishedInfo?.updater }} </span>
                    <span class="label"> {{ $t('publishTime') }}: </span>
                    <span>{{ publishedInfo?.updateTime }}</span>
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
                    @click="handleContinueSaveDraft"
                >
                    {{ primaryButtonText }}
                </bk-button>
                <bk-button @click="goPipelineModel">
                    {{ secondaryButtonText }}
                </bk-button>
                <bk-button
                    v-if="isConflictStatus"
                    @click="value = false"
                >
                    {{ $t('returnToEditing') }}
                </bk-button>
            </footer>
        </bk-dialog>
    </div>
</template>

<script>
    import { mapState } from 'vuex'
    import { convertTime } from '@/utils/util'
    import Logo from '@/components/Logo'
    import VersionDiffEntry from '@/components/PipelineDetailTabs/VersionDiffEntry.vue'

    export default {
        name: 'DraftManager',
        components: {
            VersionDiffEntry,
            Logo
        },
        props: {
            // 草稿列表
            draftList: {
                type: Array,
                default: () => []
            },
            // 最新草稿信息
            lasterDraftInfo: {
                type: Object,
                default: () => ({})
            },
            // 当前编辑的YAML
            currentEditYaml: {
                type: String,
                default: ''
            },
            // 草稿YAML
            draftYaml: {
                type: String,
                default: ''
            },
            // 是否显示冲突对话框
            value: {
                type: Boolean,
                default: false
            }
        },
        data () {
            return {
                isShowDraftList: false
            }
        },
        computed: {
            ...mapState('atom', [
                'pipelineInfo'
            ]),
            // 冲突状态相关的computed
            isConflictStatus () {
                return this.lasterDraftInfo?.status === 'CONFLICT'
            },
            isPublishedStatus () {
                return this.lasterDraftInfo?.status === 'PUBLISHED'
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
        mounted () {
            // 监听全局点击事件
            document.addEventListener('click', this.handleGlobalClick)
        },
        beforeDestroy () {
            // 移除全局点击事件监听
            document.removeEventListener('click', this.handleGlobalClick)
        },
        methods: {
            formatTime (value) {
                return convertTime(value)
            },
            handleShowDraftList () {
                this.isShowDraftList = !this.isShowDraftList
            },
            handleGlobalClick (event) {
                // 检查点击的元素是否在特定的CSS类中
                const isInExcludedElement = event.target.closest('.diff-button')
                    || event.target.closest('.rollback-button')
                    || event.target.closest('.bk-dialog')
                    || event.target.closest('.last-save-time')
                    || event.target.closest('.draft-list')

                if (this.isShowDraftList && !isInExcludedElement) {
                    this.isShowDraftList = false
                }
            },
            handleDiff (draftVersion) {
                this.$emit('diff', draftVersion)
            },
            handleRollback (item) {
                this.$emit('rollback', item)
            },
            handleContinueSaveDraft () {
                this.$emit('continue-save-draft')
            },
            goPipelineModel () {
                this.$emit('go-pipeline-model')
            },
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
            width: 388px;
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
                }
            }
            .options {
                // visibility: hidden;
                .rollback-icon {
                    transform: rotate(180deg);
                }
            }
            .update-tip {
                padding: 0 4px;
                background: #FDEED8;
                border-radius: 2px;
                color: #E38B02;
            }
        }
    }

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
</style>
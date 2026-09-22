<template>
    <!-- 构建终态详情 Popover：取消 / 失败 / 超时 / 成功 -->
    <bk-popover
        class="build-end-info-popover-trigger"
        placement="bottom-start"
        theme="light"
        :width="500"
        ext-cls="build-end-info-popover"
        :tippy-options="TIPPY_OPTIONS"
    >
        <slot />
        <div
            slot="content"
            class="build-end-info-popover-content"
            :style="themeStyle"
        >
            <!-- 标题：执行失败 / 已取消 / 执行成功 / 阶段成功 / 执行超时 -->
            <h3 class="bei-title">
                {{ $t(mergedConfig.titleKey) }}
            </h3>

            <!-- 摘要区：布局由 summaryLayout 决定（总耗时 / 类型+原因） -->
            <div
                :class="['bei-summary-box', {
                    'bei-summary-duration-only': isDurationOnlyLayout
                }]"
            >
                <!-- 执行成功：仅展示总耗时 -->
                <div
                    v-if="isDurationOnlyLayout"
                    class="bei-duration-main"
                >
                    <span class="bei-duration-label">{{ durationLabel }}</span>
                    <span class="bei-duration-value">{{ runningDuration || '--' }}</span>
                </div>
                <!-- 其他：类型标签 + endTypeDesc + 原因/计数 -->
                <div
                    v-else
                    class="bei-summary-main"
                >
                    <span class="bei-type-label">{{ $t(mergedConfig.typeLabelKey) }}</span>
                    <span class="bei-type-value">{{ buildEndInfo.endTypeDesc }}</span>
                    <span
                        v-if="summaryExtraText"
                        class="bei-summary-extra"
                    >
                        {{ summaryExtraText }}
                    </span>
                </div>
                <!-- meta：成功=开始+触发人；审核中=开始+已等待；取消用户=结束+操作人；其余=结束+总耗时 -->
                <div class="bei-summary-meta">
                    <template v-if="isDurationOnlyLayout">
                        <span>{{ formatStartTime }} {{ $t('details.startExecute') }}</span>
                        <template v-if="triggerUser || triggerTypeDesc">
                            <span class="meta-divider">|</span>
                            <span>{{ triggerUser || '--' }} {{ triggerTypeDesc }}</span>
                        </template>
                    </template>
                    <template v-else-if="isStartWaitMeta">
                        <span>{{ formatStartTime }} {{ $t('details.startExecute') }}</span>
                        <template v-if="waitDuration">
                            <span class="meta-divider">|</span>
                            <span>{{ $t('details.waited') }} {{ waitDuration }}</span>
                        </template>
                    </template>
                    <template v-else>
                        <span>{{ summaryMetaPrimary }}</span>
                        <template v-if="showSummaryDuration">
                            <span class="meta-divider">|</span>
                            <span>{{ durationLabel }} {{ runningDuration }}</span>
                        </template>
                    </template>
                </div>
            </div>

            <!-- 中间扩展区：父流水线取消 -->
            <div
                v-if="showExtraSection"
                class="bei-extra-section"
            >
                <div class="bei-section-title">
                    <logo
                        :name="extraSection.logo"
                        size="10"
                        class="bei-extra-icon"
                    />
                    <span>{{ $t(extraSection.titleKey) }}</span>
                </div>

                <div class="bei-extra-row">
                    <div class="bei-extra-info">
                        <span
                            v-bk-overflow-tips
                            class="bei-extra-name"
                        >{{ parentPipelineInfo.pipelineName }}</span>
                        <span class="bei-extra-build-num">#{{ parentPipelineInfo.buildNum }}</span>
                        <span
                            v-if="parentPipelineInfo.operator"
                            class="bei-extra-operator"
                        >{{ parentPipelineInfo.operator }} {{ $t('cancel') }}</span>
                    </div>
                    <i
                        v-if="relatedPipelineUrl"
                        class="devops-icon icon-jump-link bei-extra-link"
                        @click.stop="openRelatedPipeline"
                    />
                </div>
            </div>

            <!-- 位置列表：终止/失败/超时/驳回位置 -->
            <div
                v-if="positionItems.length"
                class="bei-positions-section"
            >
                <div class="bei-section-title">
                    <i class="bei-section-dot" />
                    <span>{{ $t(positionsTitleKey, [positionItems.length]) }}</span>
                </div>
                <ul class="bei-position-list">
                    <li
                        v-for="row in positionItems"
                        :key="row.key"
                        class="bei-position-entry"
                    >
                        <div class="bei-position-item">
                            <bk-tag
                                v-if="row.position"
                                class="position-index-tag"
                            >
                                {{ row.position }}
                            </bk-tag>
                            <div class="position-main">
                                <span
                                    v-bk-overflow-tips
                                    class="position-path"
                                >{{ row.path }}</span>
                                <!-- 取消场景：状态 tag 紧跟 path -->
                                <bk-tag
                                    v-if="buildEndInfo?.endCategory === BUILD_END_CATEGORY.CANCEL && row.item.statusAtEndDesc"
                                    class="position-status-tag"
                                >
                                    {{ row.item.statusAtEndDesc }}
                                </bk-tag>
                            </div>
                            <div class="position-tail">
                                <!-- 超时：有 errorCode 才展示 + 定位日志 -->
                                <span
                                    v-if="buildEndInfo?.endCategory === BUILD_END_CATEGORY.TIMEOUT && row.item.errorCode"
                                    class="position-status-text"
                                >{{ $t('details.errorCode', [row.item.errorCode]) }}</span>
                                <!-- 阶段成功：审核组 | 待审核人 / 操作人 + 驳回 -->
                                <template v-if="isStageReviewPosition || isStageReviewPendingPosition">
                                    <span
                                        v-if="row.item.reviewGroupSeq || row.item.reviewGroupName"
                                        v-bk-overflow-tips
                                        class="position-review-suggest"
                                    >{{ formatReviewGroupLabel(row.item.reviewGroupSeq, row.item.reviewGroupName) }}</span>
                                    <span
                                        v-if="(row.item.reviewGroupSeq || row.item.reviewGroupName) && (isStageReviewPendingPosition ? row.item.reviewers?.length : row.item.operator)"
                                        class="meta-divider"
                                    >|</span>
                                    <span
                                        v-if="isStageReviewPendingPosition && row.item.reviewers?.length"
                                        v-bk-overflow-tips
                                        class="position-action-text"
                                    >{{ $t('details.reviewersReview', [joinReviewers(row.item.reviewers)]) }}</span>
                                    <span
                                        v-if="isStageReviewPosition && row.item.operator"
                                        class="position-action-text"
                                    >{{ row.item.operator }} {{ $t('details.reject') }}</span>
                                </template>
                                <!-- 失败-人工审核驳回：操作人 -->
                                <span
                                    v-else-if="row.item.endType === BUILD_END_TYPE.FAIL_REVIEW && row.item.operator"
                                    class="position-action-text"
                                >{{ row.item.operator }} {{ $t('details.reject') }}</span>
                                <bk-button
                                    v-if="row.actionLabel"
                                    text
                                    theme="primary"
                                    class="position-action-btn"
                                    @click.stop="handleLocate(row.item)"
                                >
                                    {{ row.actionLabel }}
                                </bk-button>
                            </div>
                        </div>
                        <div
                            v-if="buildEndInfo?.endCategory === BUILD_END_CATEGORY.FAIL"
                            class="position-fail-detail"
                        >
                            <template v-if="buildEndInfo.endType === BUILD_END_TYPE.FAIL_EXEC">
                                <span class="position-fail-detail-accent">{{ $t('details.errorCode', [row.item.errorCode]) }}</span>
                            </template>
                            <template v-else>
                                <span
                                    v-if="row.item.endTypeDesc"
                                    class="position-fail-detail-accent"
                                >{{ row.item.endTypeDesc }}<template v-if="row.item.reason">：</template></span>
                                <span
                                    v-if="row.item.reason"
                                    class="position-fail-detail-reason"
                                >{{ row.item.reason }}</span>
                            </template>
                        </div>
                    </li>
                </ul>
            </div>
        </div>
    </bk-popover>
</template>

<script>
    import Logo from '@/components/Logo'
    import { convertMStoStringByRule, convertTime } from '@/utils/util'
    import {
        BUILD_END_CATEGORY,
        BUILD_END_TYPE,
        POSITION_ACTION_TYPE,
        SUMMARY_LAYOUT,
        getBuildEndInfoConfig,
        getBuildEndTypeRule,
        getMergedEndTypeConfig,
        formatReviewGroupLabel as buildReviewGroupLabel,
        resolvePositionAction,
        resolvePositionActionLabel
    } from './buildEndInfoConfig'

    const TIPPY_OPTIONS = {
        theme: 'light',
        interactive: true,
        arrow: true,
        appendTo: () => document.body
    }

    export default {
        name: 'BuildEndInfoPopover',
        components: {
            Logo
        },
        props: {
            buildEndInfo: {
                type: Object,
                default: null
            },
            /** 构建开始时间，SUCCESS 场景展示「开始执行」用 */
            startTime: {
                type: [Number, String],
                default: null
            },
            /** 触发人，SUCCESS 场景 meta 展示用 */
            triggerUser: {
                type: String,
                default: ''
            },
            /** 触发方式描述，如「手动触发」 */
            triggerTypeDesc: {
                type: String,
                default: ''
            }
        },
        computed: {
            /** category 级配置（CANCEL / FAIL / TIMEOUT / SUCCESS） */
            categoryConfig () {
                return getBuildEndInfoConfig(this.buildEndInfo?.endCategory)
            },
            /** endType 级展示规则 */
            endTypeRule () {
                return getBuildEndTypeRule(this.categoryConfig, this.buildEndInfo?.endType)
            },
            /** category + endType 合并后的完整配置 */
            mergedConfig () {
                return getMergedEndTypeConfig(this.categoryConfig, this.buildEndInfo?.endType) || {}
            },
            /** 是否为「仅总耗时」布局（普通执行成功） */
            isDurationOnlyLayout () {
                return (this.endTypeRule.summaryLayout || SUMMARY_LAYOUT.TYPE_REASON) === SUMMARY_LAYOUT.DURATION_ONLY
            },
            /** 是否为阶段准入驳回的位置行布局 */
            isStageReviewPosition () {
                return this.endTypeRule.positionLayout === 'stageReview'
            },
            /** 是否为阶段准入审核中的位置行布局 */
            isStageReviewPendingPosition () {
                return this.endTypeRule.positionLayout === 'stageReviewPending'
            },
            /** 摘要 meta 是否为「开始执行 + 已等待」 */
            isStartWaitMeta () {
                return this.endTypeRule.metaMode === 'startWait'
            },
            extraSection () {
                return this.endTypeRule.extraSection
            },
            parentPipelineInfo () {
                return this.buildEndInfo?.parentPipelineInfo
            },
            /** 仅父流水线取消展示扩展区 */
            showExtraSection () {
                return !!this.extraSection && !!this.parentPipelineInfo
            },
            /** 摘要主行额外文案：计数或 reason */
            summaryExtraText () {
                if (this.buildEndInfo?.endCategory === BUILD_END_CATEGORY.FAIL) {
                    return ''
                }
                if (this.endTypeRule.summaryMode === 'count') {
                    const count = this.buildEndInfo.positionCount || this.positions.length
                    return this.$t(this.mergedConfig.summaryCountKey, [count])
                }
                return this.buildEndInfo?.reason || ''
            },
            /** 摘要 meta 主文案（结束时间 + 结束/取消/操作人） */
            summaryMetaPrimary () {
                if (this.endTypeRule.metaMode === 'operator') {
                    return `${this.formatEndTime} ${this.buildEndInfo?.operator || '--'} ${this.metaActionText}`
                }
                return `${this.formatEndTime} ${this.metaActionText}`
            },
            showSummaryDuration () {
                return !this.isStartWaitMeta && !!this.runningDuration
            },
            /** CSS 变量：摘要背景色、强调色、位置圆点色 */
            themeStyle () {
                const { summaryBg, accent, sectionDot } = this.mergedConfig.theme || {}
                return {
                    '--bei-summary-bg': summaryBg,
                    '--bei-accent': accent,
                    '--bei-section-dot': sectionDot
                }
            },
            positions () {
                return this.buildEndInfo?.positions || []
            },
            /**
             * 位置列表渲染数据（预计算，避免模板内重复调用方法）
             * highlight 事件 → 画布高亮；locateLog 事件 → 高亮 + 日志侧滑
             */
            positionItems () {
                return this.positions.map((item, index) => ({
                    item,
                    key: `${item.stageId}-${item.containerId}-${item.taskId || index}`,
                    position: item.position,
                    path: String(item.componentPath || '').replace(/\//g, ' - '),
                    actionLabel: resolvePositionActionLabel(
                        this.endTypeRule,
                        item,
                        (key) => this.$t(key)
                    )
                }))
            },
            positionsTitleKey () {
                return this.endTypeRule.positionsTitleKey || this.mergedConfig.positionsTitleKey || ''
            },
            /** 摘要 meta 动作文案（结束 / 取消），与位置行 operatorText 可不同 */
            metaActionText () {
                const key = this.endTypeRule.metaActionTextKey || this.mergedConfig.actionTextKey
                return key ? this.$t(key) : ''
            },
            durationLabel () {
                const key = this.mergedConfig.durationLabelKey
                return key ? this.$t(key) : ''
            },
            formatEndTime () {
                return convertTime(this.buildEndInfo?.endTime) || '--'
            },
            formatStartTime () {
                return convertTime(this.startTime) || '--'
            },
            runningDuration () {
                const totalCostTime = Number(this.buildEndInfo?.totalCostTime)
                if (!totalCostTime || totalCostTime < 0) return ''
                return convertMStoStringByRule(totalCostTime)
            },
            waitDuration () {
                const waitCostTime = Number(this.buildEndInfo?.waitCostTime)
                if (!waitCostTime || waitCostTime < 0) return ''
                return convertMStoStringByRule(waitCostTime)
            },
            relatedPipelineUrl () {
                const info = this.parentPipelineInfo
                if (!info?.pipelineId || !info?.buildId || !info.projectId) return ''
                return `${WEB_URL_PREFIX}/pipeline/${info.projectId}/${info.pipelineId}/detail/${info.buildId}`
            }
        },
        created () {
            this.BUILD_END_CATEGORY = BUILD_END_CATEGORY
            this.BUILD_END_TYPE = BUILD_END_TYPE
        },
        methods: {
            openRelatedPipeline () {
                if (!this.relatedPipelineUrl) return
                window.open(this.relatedPipelineUrl, '_blank')
            },
            formatReviewGroupLabel (seq, name) {
                return buildReviewGroupLabel(
                    seq,
                    name,
                    (key, params) => this.$t(key, params)
                )
            },
            joinReviewers (reviewers) {
                const list = Array.isArray(reviewers) ? reviewers.filter(Boolean) : []
                const separator = String(this.$i18n?.locale).startsWith('zh') ? '、' : ', '
                return list.join(separator)
            },
            /** view / locate：画布高亮；locateLog：高亮 + 日志 */
            handleLocate (item) {
                const actionType = resolvePositionAction(this.endTypeRule, item).type
                if (!actionType) return
                if (actionType === POSITION_ACTION_TYPE.LOCATE_LOG) {
                    this.$emit('locateLog', item)
                } else {
                    this.$emit('highlight', item)
                }
            }
        }
    }
</script>

<style lang="scss">
@import "@/scss/conf";
@import "@/scss/mixins/ellipsis";

.build-end-info-popover {
    .tippy-tooltip {
        padding: 0;
        box-shadow: 0 2px 10px 0 rgba(0, 0, 0, 0.16);
    }
}

.build-end-info-popover-content {
    padding: 16px;
    color: #313238;
    text-align: left;

    .bei-title {
        margin: 0 0 12px;
        font-size: 16px;
        font-weight: 700;
        line-height: 24px;
        color: #313238;
    }

    .bei-summary-box {
        padding: 12px 16px;
        background: var(--bei-summary-bg);
        border-radius: 4px;
    }

    .bei-summary-duration-only {
        .bei-duration-main {
            display: flex;
            align-items: baseline;
            gap: 8px;
            line-height: 28px;
        }

        .bei-duration-label {
            font-size: 12px;
            color: #979ba5;
        }

        .bei-duration-value {
            font-size: 16px;
            font-weight: 700;
            color: #313238;
        }
    }

    .bei-summary-main {
        display: flex;
        flex-wrap: wrap;
        align-items: baseline;
        gap: 8px;
        line-height: 20px;
        font-size: 12px;
    }

    .bei-type-label {
        color: #979ba5;
        flex-shrink: 0;
    }

    .bei-type-value {
        color: var(--bei-accent);
        font-weight: 700;
        flex-shrink: 0;
        font-size: 16px;
    }

    .bei-summary-extra {
        color: #313238;
        font-size: 14px;
        word-break: break-all;
    }

    .bei-summary-meta {
        margin-top: 8px;
        display: flex;
        flex-wrap: wrap;
        align-items: center;
        gap: 8px;
        font-size: 12px;
        line-height: 18px;
        color: #979ba5;

        .meta-divider {
            color: #c4c6cc;
        }
    }

    .bei-extra-section,
    .bei-positions-section {
        margin-top: 14px;
    }

    .bei-section-title {
        display: flex;
        align-items: center;
        gap: 6px;
        margin-bottom: 6px;
        font-size: 12px;
        line-height: 18px;
        color: #313238;

        .bei-extra-icon {
            color: #699DF4;
            flex-shrink: 0;
        }

        .bei-section-dot {
            width: 8px;
            height: 8px;
            margin-right: 2px;
            border-radius: 50%;
            flex-shrink: 0;
            background: var(--bei-section-dot);
        }
    }

    .bei-extra-row {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 12px;
        min-height: 24px;
        padding: 2px 14px;
        border-radius: 2px;
    }

    .bei-extra-info {
        display: flex;
        align-items: center;
        gap: 8px;
        min-width: 0;
        flex: 1;
        font-size: 12px;
        color: #4D4F56;
    }

    .bei-extra-name {
        @include ellipsis();
        max-width: 220px;
        color: #313238;
    }

    .bei-extra-operator {
        color: #979BA5;
    }

    .bei-extra-build-num,
    .bei-extra-operator {
        flex-shrink: 0;
    }

    .bei-extra-link {
        color: #3a84ff;
        cursor: pointer;
        flex-shrink: 0;
        font-size: 14px;

        &:hover {
            color: #699df4;
        }
    }

    .bei-extra-text {
        padding: 8px 16px;
        border: 1px solid #EAEBF0;
        border-radius: 4px;
        background: #FAFBFD;
        font-size: 12px;
        line-height: 20px;
        color: #4D4F56;
        word-break: break-all;
        white-space: pre-wrap;
    }

    .bei-position-list {
        margin: 0;
        padding: 0;
        list-style: none;
        display: grid;
        gap: 8px;
    }

    .bei-position-entry {
        border: 1px solid #EAEBF0;
        border-radius: 4px;
        background: #FAFBFD;
        overflow: hidden;
    }

    .bei-position-item {
        display: flex;
        align-items: center;
        overflow: hidden;
        gap: 4px;
        min-height: 38px;
        padding: 8px 16px;
    }

    .position-fail-detail {
        padding: 8px 16px;
        border-top: 1px solid #EAEBF0;
        font-size: 12px;
        line-height: 18px;
        word-break: break-word;
    }

    .position-fail-detail-accent {
        color: var(--bei-accent);
    }

    .position-fail-detail-reason {
        color: #4D4F56;
    }

    .position-index-tag {
        margin: 0;
        flex-shrink: 0;
        background: #F0F1F5;
        border-color: transparent;
        color: #4D4F56;
    }

    .position-main {
        display: flex;
        align-items: center;
        gap: 4px;
        min-width: 0;
        flex: 1;
        overflow: hidden;
    }

    .position-path {
        @include ellipsis();
        flex: 0 1 auto;
        min-width: 0;
        font-size: 12px;
        color: #63656e;
    }

    .position-status-tag {
        margin: 0;
        flex-shrink: 0;
    }

    .position-tail {
        display: flex;
        align-items: center;
        gap: 8px;
        min-width: 0;
        flex: 0 1 auto;
        margin-left: auto;

        .meta-divider {
            color: #c4c6cc;
        }
    }

    .position-status-text {
        flex-shrink: 0;
        font-size: 12px;
        color: var(--bei-accent);
        white-space: nowrap;
    }

    .position-review-suggest {
        @include ellipsis();
        min-width: 0;
        max-width: 140px;
        font-size: 12px;
        color: #979ba5;
    }

    .position-action-btn,
    .position-action-text {
        flex-shrink: 0;
        font-size: 12px;
    }

    .position-action-text {
        color: #979ba5;
    }
}
</style>

<template>
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
            <h3 class="bei-title">
                {{ $t(statusConfig.titleKey) }}
            </h3>
            <div class="bei-summary-box">
                <div class="bei-summary-main">
                    <span class="bei-type-label">{{ $t(statusConfig.typeLabelKey) }}</span>
                    <span class="bei-type-value">{{ buildEndInfo.endTypeDesc }}</span>
                    <span
                        v-if="summaryExtraText"
                        class="bei-summary-extra"
                    >
                        {{ summaryExtraText }}
                    </span>
                </div>
                <div class="bei-summary-meta">
                    <span v-if="endTypeRule.metaMode === 'operator'">
                        {{ formatEndTime }} {{ buildEndInfo.operator || '--' }} {{ actionText }}
                    </span>
                    <span v-else>
                        {{ formatEndTime }} {{ actionText }}
                    </span>
                    <template v-if="runningDuration">
                        <span class="meta-divider">|</span>
                        <span>{{ $t('execedTimes') }} {{ runningDuration }}</span>
                    </template>
                </div>
            </div>

            <!-- 中间扩展区：父流水线 / 驳回原因 / 失败原因 等 -->
            <div
                v-if="showExtraSection"
                class="bei-extra-section"
            >
                <div class="bei-section-title">
                    <logo
                        v-if="extraSection.logo"
                        :name="extraSection.logo"
                        size="10"
                        class="bei-extra-icon"
                    />
                    <span>{{ $t(extraSection.titleKey) }}</span>
                </div>

                <div
                    v-if="isRelatedPipelineSection"
                    class="bei-extra-row"
                >
                    <div class="bei-extra-info">
                        <span
                            v-bk-overflow-tips
                            class="bei-extra-name"
                        >{{ parentPipelineInfo.pipelineName }}</span>
                        <span class="bei-extra-build-num">#{{ parentPipelineInfo.buildNum }}</span>
                        <span
                            v-if="buildEndInfo.parentPipelineInfo.operator"
                            class="bei-extra-operator"
                        >{{ buildEndInfo.parentPipelineInfo.operator }} {{ actionText }}</span>
                    </div>
                    <i
                        v-if="relatedPipelineUrl"
                        class="devops-icon icon-jump-link bei-extra-link"
                        @click.stop="openRelatedPipeline"
                    />
                </div>

                <div
                    v-else-if="extraSectionText"
                    class="bei-extra-text"
                >
                    {{ extraSectionText }}
                </div>
            </div>

            <div
                v-if="positions.length"
                class="bei-positions-section"
            >
                <div class="bei-section-title">
                    <i class="bei-section-dot" />
                    <span>{{ $t(statusConfig.positionsTitleKey, [positions.length]) }}</span>
                </div>
                <ul class="bei-position-list">
                    <li
                        v-for="(item, index) in positions"
                        :key="`${item.stageId}-${item.containerId}-${item.taskId || index}`"
                        class="bei-position-item"
                    >
                        <bk-tag
                            v-if="item.position"
                            class="position-index-tag"
                        >
                            {{ item.position }}
                        </bk-tag>
                        <div class="position-main">
                            <span
                                v-bk-overflow-tips
                                class="position-path"
                            >{{ formatComponentPath(item.componentPath) }}</span>
                            <bk-tag
                                v-if="item.statusAtEndDesc"
                                class="position-status-tag"
                            >
                                {{ item.statusAtEndDesc }}
                            </bk-tag>
                        </div>
                        <bk-button
                            text
                            theme="primary"
                            class="position-locate-btn"
                            @click.stop="handleLocate(item)"
                        >
                            {{ $t('details.locate') }}
                        </bk-button>
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
        EXTRA_SECTION_TYPE,
        getBuildEndInfoConfig,
        getBuildEndTypeRule
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
            status: {
                type: String,
                required: true
            },
            buildEndInfo: {
                type: Object,
                default: null
            }
        },
        computed: {
            statusConfig () {
                return getBuildEndInfoConfig(this.status)
            },
            endTypeRule () {
                return getBuildEndTypeRule(this.statusConfig, this.buildEndInfo?.endType)
            },
            extraSection () {
                return this.endTypeRule.extraSection
            },
            isRelatedPipelineSection () {
                return this.extraSection?.type === EXTRA_SECTION_TYPE.RELATED_PIPELINE
            },
            parentPipelineInfo () {
                return this.buildEndInfo?.parentPipelineInfo
            },
            extraSectionText () {
                const field = this.extraSection?.contentField
                if (this.extraSection?.type !== EXTRA_SECTION_TYPE.TEXT || !field) {
                    return ''
                }
                return this.buildEndInfo?.[field] || ''
            },
            showExtraSection () {
                if (!this.extraSection?.titleKey) return false
                if (this.isRelatedPipelineSection) return !!this.parentPipelineInfo
                return !!this.extraSectionText
            },
            summaryExtraText () {
                if (this.endTypeRule.summaryMode === 'count') {
                    const count = this.buildEndInfo.positionCount || this.positions.length
                    return this.$t(this.statusConfig.summaryCountKey, [count])
                }
                return this.buildEndInfo.reason || ''
            },
            themeStyle () {
                const { summaryBg, accent, sectionDot } = this.statusConfig.theme || {}
                return {
                    '--bei-summary-bg': summaryBg,
                    '--bei-accent': accent,
                    '--bei-section-dot': sectionDot
                }
            },
            positions () {
                return this.buildEndInfo?.positions || []
            },
            actionText () {
                return this.statusConfig.actionTextKey
                    ? this.$t(this.statusConfig.actionTextKey)
                    : ''
            },
            formatEndTime () {
                return convertTime(this.buildEndInfo?.endTime) || '--'
            },
            runningDuration () {
                const totalCostTime = Number(this.buildEndInfo.totalCostTime)
                if (!totalCostTime || totalCostTime < 0) return ''
                return convertMStoStringByRule(totalCostTime)
            },
            relatedPipelineUrl () {
                const info = this.parentPipelineInfo
                if (!info?.pipelineId || !info?.buildId || !info.projectId) return ''
                return `${WEB_URL_PREFIX}/pipeline/${info.projectId}/${info.pipelineId}/detail/${info.buildId}`
            }
        },
        methods: {
            formatComponentPath (path = '') {
                return String(path).replace(/\//g, ' - ')
            },
            openRelatedPipeline () {
                if (!this.relatedPipelineUrl) return
                window.open(this.relatedPipelineUrl, '_blank')
            },
            handleLocate (item) {
                this.$emit('locate', item)
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
        gap: 8px;
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
        padding: 2px 16px;
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

    .bei-position-item {
        display: flex;
        align-items: center;
        overflow: hidden;
        gap: 4px;
        min-height: 38px;
        padding: 8px 16px;
        border: 1px solid #EAEBF0;
        border-radius: 4px;
        background: #FAFBFD;
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

    .position-locate-btn {
        flex-shrink: 0;
        margin-left: auto;
        font-size: 12px;
    }
}
</style>

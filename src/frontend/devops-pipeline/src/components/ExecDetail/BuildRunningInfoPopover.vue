<template>
    <!-- 构建运行态详情 Popover：排队中 / 执行中 -->
    <bk-popover
        class="build-running-info-popover-trigger"
        placement="bottom-start"
        theme="light"
        :width="520"
        ext-cls="build-running-info-popover"
        :tippy-options="TIPPY_OPTIONS"
    >
        <slot />
        <div
            slot="content"
            class="build-running-info-popover-content"
            :style="themeStyle"
        >
            <div class="bri-header">
                <h3 class="bri-title">
                    {{ buildRunningInfo.runningTypeDesc }}
                </h3>
                <span
                    v-if="isQueueCategory"
                    v-bk-overflow-tips
                    class="bri-subtitle"
                >
                    {{ $t('details.queueConcurrencyHint') }}
                </span>
            </div>

            <!-- 摘要区：排队 / 执行两种布局 -->
            <!-- 摘要区：排队态 / 执行态共用容器 -->
            <div class="bri-summary-box">
                <!-- 摘要主行：排队=排队位置+已等待；执行=当前阶段+已运行 -->
                <div class="bri-summary-main">
                    <!-- 排队态主行 -->
                    <template v-if="isQueueCategory">
                        <!-- 排队位置（queuePosition > 0 才展示） -->
                        <template v-if="showQueuePosition">
                            <span class="bri-summary-label">{{ $t('details.queuePosition') }}</span>
                            <span class="bri-summary-accent">{{ $t('details.queuePositionValue', [queueDetail.queuePosition]) }}</span>
                        </template>
                        <!-- 已等待时长） -->
                        <span
                            v-if="liveWaitingDuration"
                            class="bri-summary-duration"
                        >
                            {{ $t('details.waited') }} {{ liveWaitingDuration }}
                        </span>
                    </template>
                    <!-- 执行态主行 -->
                    <template v-else>
                        <span class="bri-summary-label">{{ $t('details.currentPhase') }}</span>
                        <span class="bri-summary-accent">{{ buildRunningInfo.currentPhase || '--' }}</span>
                        <!-- 已运行时长） -->
                        <span
                            v-if="liveRunningDuration"
                            class="bri-summary-duration"
                        >
                            {{ $t('details.hasRunning') }} {{ liveRunningDuration }}
                        </span>
                    </template>
                </div>
                <!-- 摘要副行 meta：时间 / 触发信息 / 并发组 -->
                <div class="bri-summary-meta">
                    <!-- 排队态：入队时间 + 并发组名称 -->
                    <template v-if="isQueueCategory">
                        <span>{{ formatQueueTime }} {{ $t('details.joinQueue') }}</span>
                        <template v-if="queueDetail.concurrencyGroup">
                            <span class="meta-divider">|</span>
                            <span>{{ $t('details.concurrencyGroupLabel') }}：{{ queueDetail.concurrencyGroup }}</span>
                        </template>
                    </template>
                    <!-- 执行态：开始执行时间 + 触发人/触发方式 -->
                    <template v-else>
                        <span>{{ formatStartTime }} {{ $t('details.startExecute') }}</span>
                        <template v-if="triggerMetaText">
                            <span class="meta-divider">|</span>
                            <span>{{ triggerMetaText }}</span>
                        </template>
                    </template>
                </div>
            </div>

            <!-- 排队态：占用中 -->
            <div
                v-if="showOccupyingSection"
                class="bri-list-section"
            >
                <div class="bri-section-title">
                    <i
                        class="bri-section-dot"
                        :style="{ background: categoryConfig.theme.dotOccupying }"
                    />
                    <span>{{ $t('details.occupying') }}</span>
                </div>
                <ul class="bri-build-list">
                    <li
                        v-for="row in occupyingItems"
                        :key="row.key"
                        class="bri-build-item"
                    >
                        <div class="bri-build-row">
                            <span
                                v-if="row.buildNumText"
                                class="bri-build-num"
                            >{{ row.buildNumText }}</span>
                            <div class="bri-build-body">
                                <span class="bri-build-desc">{{ row.buildDescText }}</span>
                                <span class="bri-build-meta-text">{{ row.metaText }}</span>
                            </div>
                            <bk-button
                                v-if="row.url"
                                text
                                theme="primary"
                                class="bri-build-action"
                                @click.stop="openBuildDetail(row.url)"
                            >
                                {{ $t('details.view') }}
                            </bk-button>
                        </div>
                    </li>
                </ul>
            </div>

            <!-- 排队态：前方排队（可折叠） -->
            <div
                v-if="showAheadSection"
                class="bri-list-section"
            >
                <div
                    :class="['bri-section-title', { 'is-clickable': hasCollapsibleAheadItems }]"
                    @click.stop="toggleAheadExpanded"
                >
                    <i
                        class="bri-section-dot"
                        :style="{ background: categoryConfig.theme.dotAhead }"
                    />
                    <span>{{ $t('details.queuingAhead', [aheadCount]) }}</span>
                    <i
                        v-if="hasCollapsibleAheadItems"
                        :class="['devops-icon', aheadExpanded ? 'icon-angle-up' : 'icon-angle-down', 'bri-section-toggle']"
                    />
                </div>
                <ul class="bri-build-list">
                    <li
                        v-for="(row, index) in aheadItems"
                        v-show="index === 0 || aheadExpanded"
                        :key="row.key"
                        class="bri-build-item"
                    >
                        <div class="bri-build-row">
                            <span
                                v-if="row.buildNumText"
                                class="bri-build-num"
                            >{{ row.buildNumText }}</span>
                            <div class="bri-build-body">
                                <span class="bri-build-desc">{{ row.buildDescText }}</span>
                                <span class="bri-build-meta-text">{{ row.metaText }}</span>
                            </div>
                            <bk-button
                                v-if="row.url"
                                text
                                theme="primary"
                                class="bri-build-action"
                                @click.stop="openBuildDetail(row.url)"
                            >
                                {{ $t('details.view') }}
                            </bk-button>
                        </div>
                    </li>
                </ul>
            </div>

            <!-- 执行态：等待中的 Job -->
            <div
                v-if="showWaitingJobsSection"
                class="bri-list-section"
            >
                <div class="bri-section-title">
                    <i
                        class="bri-section-dot"
                        :style="{ background: categoryConfig.theme.dotWaiting }"
                    />
                    <span>{{ $t('details.waitingJobs', [waitingJobCount]) }}</span>
                </div>
                <ul class="bri-job-list">
                    <li
                        v-for="row in waitingJobItems"
                        :key="row.key"
                        class="bri-job-item"
                    >
                        <div class="bri-job-main">
                            <bk-tag class="bri-job-wait-tag">
                                {{ row.item.waitTypeDesc }}
                            </bk-tag>
                            <bk-tag
                                v-if="row.item.position"
                                class="bri-job-position-tag"
                            >
                                {{ row.item.position }}
                            </bk-tag>
                            <span
                                v-bk-overflow-tips
                                class="bri-job-path"
                            >{{ row.path }}</span>
                        </div>
                        <bk-button
                            text
                            theme="primary"
                            class="bri-job-action"
                            @click.stop="handleLocate(row.item)"
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
    import {
        BUILD_RUNNING_CATEGORY,
        calcLiveDurationMs,
        getBuildRunningInfoConfig,
        resolveRelatedBuildUrl
    } from './buildRunningInfoConfig'
    import { convertMStoStringByRule, convertTime } from '@/utils/util'

    const TIPPY_OPTIONS = {
        theme: 'light',
        interactive: true,
        arrow: true,
        appendTo: () => document.body
    }

    export default {
        name: 'BuildRunningInfoPopover',
        props: {
            buildRunningInfo: {
                type: Object,
                default: null
            },
            currentTimestamp: {
                type: [Number, String],
                default: null
            }
        },
        data () {
            return {
                now: Date.now(),
                aheadExpanded: false,
                durationTimer: null
            }
        },
        computed: {
            categoryConfig () {
                return getBuildRunningInfoConfig(this.buildRunningInfo?.runningCategory) || { theme: {} }
            },
            isQueueCategory () {
                return this.buildRunningInfo?.runningCategory === BUILD_RUNNING_CATEGORY.QUEUE
            },
            queueDetail () {
                return this.buildRunningInfo?.queueDetail || {}
            },
            showQueuePosition () {
                return Number(this.queueDetail.queuePosition) > 0
            },
            occupyingCount () {
                return Number(this.queueDetail.occupyingCount) || 0
            },
            aheadCount () {
                return Number(this.queueDetail.aheadCount) || 0
            },
            waitingJobCount () {
                return Number(this.buildRunningInfo?.waitingJobCount) || 0
            },
            themeStyle () {
                const { summaryBg, accent } = this.categoryConfig.theme || {}
                return {
                    '--bri-summary-bg': summaryBg,
                    '--bri-accent': accent
                }
            },
            liveWaitingDuration () {
                const ms = calcLiveDurationMs(
                    this.buildRunningInfo?.waitingTime,
                    this.currentTimestamp,
                    this.now
                )
                if (!ms) return ''
                return convertMStoStringByRule(ms)
            },
            liveRunningDuration () {
                const ms = calcLiveDurationMs(
                    this.buildRunningInfo?.runningTime,
                    this.currentTimestamp,
                    this.now
                )
                if (!ms) return ''
                return convertMStoStringByRule(ms)
            },
            formatQueueTime () {
                return convertTime(this.buildRunningInfo?.queueTime) || '--'
            },
            formatStartTime () {
                return convertTime(this.buildRunningInfo?.startTime) || '--'
            },
            triggerMetaText () {
                const user = this.buildRunningInfo?.triggerUser
                const desc = this.buildRunningInfo?.triggerDesc
                if (user && desc) return this.$t('details.triggerMeta', [user, desc])
                return user || desc || ''
            },
            showOccupyingSection () {
                return this.isQueueCategory && this.occupyingItems.length > 0
            },
            showAheadSection () {
                return this.isQueueCategory && this.aheadCount > 0
            },
            showWaitingJobsSection () {
                return !this.isQueueCategory && this.waitingJobCount > 0
            },
            occupyingItems () {
                return this.mapBuildBriefList(this.queueDetail.occupyingBuilds, 'occupying', 'occupying')
            },
            aheadItems () {
                return this.mapBuildBriefList(this.queueDetail.aheadBuilds, 'ahead', 'ahead')
            },
            hasCollapsibleAheadItems () {
                return this.aheadItems.length > 1
            },
            waitingJobItems () {
                return (this.buildRunningInfo?.waitingJobs || []).map((item, index) => ({
                    item,
                    key: `${item.stageId}-${item.containerId}-${index}`,
                    path: String(item.componentPath || '').replace(/\//g, ' - ')
                }))
            }
        },
        mounted () {
            this.durationTimer = window.setInterval(() => {
                this.now = Date.now()
            }, 1000)
        },
        beforeDestroy () {
            if (this.durationTimer) {
                window.clearInterval(this.durationTimer)
                this.durationTimer = null
            }
        },
        methods: {
            toggleAheadExpanded () {
                if (!this.hasCollapsibleAheadItems) return
                this.aheadExpanded = !this.aheadExpanded
            },
            mapBuildBriefList (list = [], prefix = 'build', layout = 'occupying') {
                return list.map((item, index) => {
                    const costMs = calcLiveDurationMs(item.costTime, this.currentTimestamp, this.now)
                    const costText = costMs ? convertMStoStringByRule(costMs) : ''
                    const isRunningBuild = item.status === 'RUNNING'
                    const durationKey = isRunningBuild ? 'details.runningAbout' : 'details.waitedAbout'
                    const buildNumText = item.buildNum != null && item.buildNum !== ''
                        ? `#${item.buildNum}`
                        : ''
                    const buildDescText = layout === 'ahead'
                        ? [item.pipelineName, item.triggerDesc].filter(Boolean).join(' · ')
                        : [item.pipelineName, item.buildMsg].filter(Boolean).join(' · ')
                    return {
                        item,
                        key: `${prefix}-${item.buildId || index}`,
                        buildNumText,
                        buildDescText,
                        metaText: [
                            item.triggerUser,
                            costText ? this.$t(durationKey, [costText]) : ''
                        ].filter(Boolean).join(' · '),
                        url: resolveRelatedBuildUrl(item)
                    }
                })
            },
            openBuildDetail (url) {
                if (!url) return
                window.open(url, '_blank')
            },
            handleLocate (item) {
                this.$emit('highlight', item)
            }
        }
    }
</script>

<style lang="scss">
@import "@/scss/conf";
@import "@/scss/mixins/ellipsis";

.build-running-info-popover {
    .tippy-tooltip {
        padding: 0;
        box-shadow: 0 2px 10px 0 rgba(0, 0, 0, 0.16);
    }
}

.build-running-info-popover-content {
    padding: 16px;
    color: #313238;
    text-align: left;

    .bri-header {
        display: flex;
        align-items: flex-start;
        justify-content: space-between;
        gap: 16px;
        margin-bottom: 12px;
    }

    .bri-title {
        margin: 0;
        font-size: 16px;
        font-weight: 700;
        line-height: 24px;
        color: #313238;
        flex-shrink: 0;
    }

    .bri-subtitle {
        @include ellipsis();
        font-size: 12px;
        line-height: 20px;
        color: #979ba5;
        text-align: right;
    }

    .bri-summary-box {
        padding: 12px 16px;
        background: var(--bri-summary-bg);
        border-radius: 4px;
    }

    .bri-summary-main {
        display: flex;
        flex-wrap: wrap;
        align-items: baseline;
        gap: 8px;
        line-height: 28px;
        font-size: 12px;
    }

    .bri-summary-label {
        color: #979ba5;
        flex-shrink: 0;
    }

    .bri-summary-accent {
        color: var(--bri-accent);
        font-size: 16px;
        font-weight: 700;
        flex-shrink: 0;
    }

    .bri-summary-duration {
        color: #313238;
        font-size: 14px;
    }

    .bri-summary-meta {
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

    .bri-list-section {
        margin-top: 14px;
    }

    .bri-section-title {
        display: flex;
        align-items: center;
        gap: 6px;
        margin-bottom: 6px;
        font-size: 12px;
        line-height: 18px;
        color: #313238;

        &.is-clickable {
            cursor: pointer;
            user-select: none;
        }

        .bri-section-dot {
            width: 8px;
            height: 8px;
            border-radius: 50%;
            flex-shrink: 0;
        }

        .bri-section-toggle {
            margin-left: auto;
            color: #979ba5;
            font-size: 12px;
        }
    }

    .bri-build-list,
    .bri-job-list {
        margin: 0;
        padding: 0;
        list-style: none;
        display: grid;
        gap: 8px;
    }

    .bri-build-item,
    .bri-job-item {
        border: 1px solid #DCDEE5;
        border-radius: 4px;
        background: #FAFBFD;
        overflow: hidden;
    }

    .bri-build-item {
        padding: 8px 16px;
    }

    .bri-build-row {
        display: flex;
        align-items: flex-start;
        gap: 8px;
        min-width: 0;
    }

    .bri-build-num {
        flex-shrink: 0;
        font-size: 12px;
        line-height: 20px;
        color: #4D4F56;
        padding: 0 8px;
        background-color: #F0F1F5;
    }

    .bri-build-body {
        flex: 1;
        min-width: 0;
        display: flex;
        flex-direction: column;
        gap: 4px;
    }

    .bri-build-desc {
        font-size: 12px;
        line-height: 20px;
        color: #313238;
        word-break: break-word;
        white-space: normal;
    }

    .bri-build-meta-text {
        font-size: 12px;
        line-height: 18px;
        color: #979ba5;
    }

    .bri-build-action,
    .bri-job-action {
        flex-shrink: 0;
        font-size: 12px;
    }

    .bri-build-action {
        align-self: center;
    }

    .bri-job-item {
        display: flex;
        align-items: center;
        gap: 8px;
        min-height: 38px;
        padding: 8px 16px;
    }

    .bri-job-main {
        display: flex;
        align-items: center;
        gap: 4px;
        min-width: 0;
        flex: 1;
        overflow: hidden;
    }

    .bri-job-wait-tag {
        margin: 0;
        flex-shrink: 0;
        background: #FFF3E1;
        border-color: transparent;
        color: #FF9C01;
    }

    .bri-job-position-tag {
        margin: 0;
        flex-shrink: 0;
        background: #F0F1F5;
        border-color: transparent;
        color: #4D4F56;
    }

    .bri-job-path {
        @include ellipsis();
        min-width: 0;
        font-size: 12px;
        color: #63656e;
    }
}
</style>

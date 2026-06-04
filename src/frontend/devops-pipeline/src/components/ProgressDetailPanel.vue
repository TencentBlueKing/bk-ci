<template>
    <section
        v-if="visible"
        :class="['progress-detail-panel', { 'is-page': showHeader }]"
    >
        <div
            v-if="showHeader"
            class="progress-detail-header"
        >
            <strong>{{ $t('progressDetail.title') }}</strong>
            <span
                v-if="headerMeta"
                class="progress-detail-header-meta"
            >{{ headerMeta }}</span>
            <bk-button
                v-if="canRefresh"
                class="progress-detail-refresh"
                text
                :disabled="refreshing"
                @click="reload"
            >
                <i :class="['devops-icon', 'icon-refresh', { 'spin-icon': refreshing }]"></i>
                {{ $t('progressDetail.refresh') }}
            </bk-button>
        </div>
        <bk-loading
            class="progress-detail-body"
            mode="spin"
            theme="primary"
            :is-loading="loading"
        >
            <template>
                <bk-exception
                    v-if="showEmpty && !hasProgressDetail"
                    type="empty"
                    scene="part"
                    class="progress-detail-empty"
                >
                    {{ $t('progressDetail.empty') }}
                </bk-exception>
                <div
                    v-if="progressInfo"
                    class="progress-detail-section progress-overview"
                >
                    <div class="progress-section-head">
                        <span class="progress-section-title">{{ progressTitle }}</span>
                    </div>
                    <div class="progress-overview-meta">
                        <span>{{ $t('progressDetail.currentProgress') }}</span>
                        <span v-if="progressInfo.summary">{{ $t('progressDetail.completed') }}</span>
                    </div>
                    <div class="progress-overview-value">
                        <strong>{{ progressPercent }}</strong>
                        <span
                            v-if="progressInfo.summary"
                            class="summary-value"
                        >
                            <em>{{ getSummaryParts(progressInfo.summary).current }}</em>
                            <span> / {{ getSummaryParts(progressInfo.summary).total }}</span>
                        </span>
                    </div>
                    <bk-progress
                        :theme="progressTheme"
                        :percent="progressValue"
                        :stroke-width="10"
                        :show-text="false"
                    />
                </div>
                <div
                    v-if="subtasksInfo && subtasksInfo.items.length"
                    class="progress-detail-section"
                >
                    <div class="progress-section-head">
                        <span class="progress-section-title">{{ subtasksInfo.title || $t('progressDetail.subtasks') }}</span>
                        <span
                            v-if="subtasksInfo.summary"
                            class="progress-section-summary"
                        >
                            {{ $t('progressDetail.completed') }}：
                            <b>{{ getSummaryParts(subtasksInfo.summary).current }}</b>
                            <span> / {{ getSummaryParts(subtasksInfo.summary).total }}</span>
                        </span>
                    </div>
                    <ul class="subtask-list">
                        <li
                            v-for="(item, index) in subtasksInfo.items"
                            :key="`${item.name}-${index}`"
                            :class="['subtask-item', subtaskStatusClass(item.status)]"
                        >
                            <i :class="['devops-icon', 'subtask-status-icon', subtaskStatusIcon(item.status), subtaskStatusClass(item.status)]"></i>
                            <span
                                class="subtask-name"
                                :title="item.name"
                            >{{ item.name }}</span>
                            <bk-progress
                                :theme="subtaskProgressTheme(item.status)"
                                :percent="normalizeProgress(item.progress)"
                                :stroke-width="5"
                                :show-text="false"
                            />
                            <span :class="['subtask-status', subtaskStatusClass(item.status)]">{{ subtaskDisplayText(item) }}</span>
                        </li>
                    </ul>
                </div>
                <div
                    v-if="timelineInfo && timelineInfo.items.length"
                    class="progress-detail-section timeline-section"
                >
                    <div class="progress-section-head">
                        <span class="progress-section-title">{{ timelineInfo.title || $t('progressDetail.timeline') }}</span>
                    </div>
                    <div class="timeline-wrap">
                        <bk-timeline
                            class="progress-timeline"
                            :list="timelineList"
                        />
                    </div>
                </div>
            </template>
        </bk-loading>
    </section>
</template>

<script>
    import {
        computed,
        defineComponent,
        getCurrentInstance,
        onBeforeUnmount,
        onMounted,
        ref,
        watch
    } from 'vue'
    import { PROCESS_API_URL_PREFIX } from '@/store/constants'
    import { convertMStoString } from '@/utils/util'

    export default defineComponent({
        name: 'ProgressDetailPanel',
        props: {
            buildId: {
                type: String,
                required: true
            },
            taskId: {
                type: String,
                default: ''
            },
            executeCount: {
                type: Number,
                default: undefined
            },
            taskStatus: {
                type: String,
                default: ''
            },
            showEmpty: {
                type: Boolean,
                default: false
            },
            showHeader: {
                type: Boolean,
                default: false
            },
            headerMeta: {
                type: String,
                default: ''
            },
            detailData: {
                type: Object,
                default: null
            }
        },
        setup (props) {
            const { proxy } = getCurrentInstance()
            const loading = ref(false)
            const refreshing = ref(false)
            const progressData = ref(props.detailData)
            const refreshTimer = ref(null)
            const requestId = ref(0)

            const normalizeProgress = (value) => {
                if (typeof value !== 'number') return 0
                return Math.max(0, Math.min(1, value))
            }

            const formatPercent = (value) => {
                const percent = normalizeProgress(value) * 100
                return `${Math.round(percent * 100) / 100}%`
            }

            const getSummaryParts = (summary = '') => {
                const [current = summary, total = ''] = String(summary).split('/')
                return {
                    current: current.trim(),
                    total: total.trim()
                }
            }

            const escapeHTML = (value) => {
                return String(value ?? '').replace(/[&<>"']/g, (char) => ({
                    '&': '&amp;',
                    '<': '&lt;',
                    '>': '&gt;',
                    '"': '&quot;',
                    "'": '&#39;'
                }[char]))
            }

            const hasProgressDetail = computed(() => {
                const detail = progressData.value?.progressDetail
                return !!(detail?.progress || detail?.subtasks?.items?.length || detail?.timeline?.items?.length)
            })

            const visible = computed(() => loading.value || hasProgressDetail.value || props.showEmpty || props.showHeader)
            const progressInfo = computed(() => progressData.value?.progressDetail?.progress ?? null)
            const progressValue = computed(() => normalizeProgress(progressInfo.value?.value ?? progressData.value?.taskProgressRete))

            const subtasksInfo = computed(() => {
                const subtasks = progressData.value?.progressDetail?.subtasks
                if (!subtasks) return null
                return {
                    ...subtasks,
                    items: subtasks.items ?? []
                }
            })

            const timelineInfo = computed(() => {
                const timeline = progressData.value?.progressDetail?.timeline
                if (!timeline) return null
                return {
                    ...timeline,
                    items: timeline.items ?? []
                }
            })

            const progressPercent = computed(() => formatPercent(progressValue.value))
            const progressTheme = computed(() => {
                const status = String(props.taskStatus || '').toLowerCase()
                if (['failed', 'failure'].includes(status)) return 'danger'
                if (['succeeded', 'success'].includes(status) || progressValue.value >= 1) return 'success'
                return 'primary'
            })

            const progressTitle = computed(() => {
                const totalProgressText = proxy.$t('progressDetail.totalProgress')
                const title = progressInfo.value?.title || totalProgressText
                return title.includes(totalProgressText) ? title : `${title} - ${totalProgressText}`
            })

            const shouldRefresh = computed(() => ['RUNNING', 'running'].includes(props.taskStatus))
            const canRefresh = computed(() => shouldRefresh.value)

            const formatTime = (time) => {
                if (!time) return '--'
                const date = new Date(time)
                if (Number.isNaN(date.getTime())) return time
                return date.toLocaleString()
            }

            const formatDuration = (duration) => {
                if (duration === null) return proxy.$t('progressDetail.running')
                if (duration === undefined) return '--'
                return `${proxy.$t('progressDetail.duration')} ${convertMStoString(duration)}`
            }

            const subtaskStatusText = (status) => {
                const key = String(status || 'pending').toLowerCase()
                return proxy.$t(`progressDetail.status.${key}`)
            }

            const subtaskDisplayText = (item) => {
                const status = String(item.status || 'pending').toLowerCase()
                return status === 'running' ? formatPercent(item.progress) : subtaskStatusText(status)
            }

            const subtaskStatusClass = (status) => `is-${String(status || 'pending').toLowerCase()}`

            const subtaskStatusIcon = (status) => {
                const iconMap = {
                    succeeded: 'icon-check-circle-shape',
                    failed: 'icon-close-circle',
                    running: 'icon-circle-2-1 spin-icon',
                    pending: 'icon-circle'
                }
                return iconMap[String(status || 'pending').toLowerCase()] || iconMap.pending
            }

            const subtaskProgressTheme = (status) => {
                const themeMap = {
                    succeeded: 'success',
                    failed: 'danger',
                    running: 'primary',
                    pending: 'primary'
                }
                return themeMap[String(status || 'pending').toLowerCase()] || 'primary'
            }

            const getTimelineStatus = (item) => {
                const status = String(item.status || '').toLowerCase()
                if (['failed', 'failure'].includes(status)) return 'failed'
                if (['succeeded', 'success'].includes(status)) return 'succeeded'
                if (status === 'running') return 'running'
                if (status === 'pending') return 'pending'
                if (item.duration === null) return 'running'
                if (item.startTime) return 'succeeded'
                return 'pending'
            }

            const getTimelineColor = (status) => {
                const colorMap = {
                    succeeded: 'green',
                    failed: 'red',
                    running: 'blue'
                }
                return colorMap[status] || ''
            }

            const getTimelineIcon = (status) => {
                if (!['running', 'failed'].includes(status)) return undefined
                const h = proxy.$createElement
                const iconMap = {
                    failed: 'close-circle',
                    running: 'circle-2-1 spin-icon'
                }
                return h('i', {
                    class: ['timeline-status-icon', iconMap[status] ? `bk-icon icon-${iconMap[status]}` : ''],
                    style: {
                        color: getTimelineColor(status)
                    }
                })
            }

            const createTimelineContent = (item) => {
                const h = proxy.$createElement
                return h('p', { class: 'timeline-meta' }, [
                    h('span', formatTime(item.startTime)),
                    h('span', formatDuration(item.duration))
                ])
            }

            const timelineList = computed(() => {
                return timelineInfo.value.items.map(item => {
                    const status = getTimelineStatus(item)
                    return {
                        color: getTimelineColor(status),
                        filled: status === 'succeeded',
                        icon: getTimelineIcon(status),
                        tag: escapeHTML(item.name),
                        content: createTimelineContent(item)
                    }
                })
            })

            const clearRefreshTimer = () => {
                clearTimeout(refreshTimer.value)
                refreshTimer.value = null
            }

            const fetchProgressDetail = async ({ showLoading = false, showRefreshing = false } = {}) => {
                const currentRequestId = ++requestId.value
                if (props.detailData) {
                    progressData.value = props.detailData
                    loading.value = false
                    refreshing.value = false
                    return
                }
                if (!props.taskId) {
                    loading.value = false
                    refreshing.value = false
                    return
                }
                loading.value = showLoading
                refreshing.value = showRefreshing
                try {
                    const { projectId, pipelineId } = proxy.$route.params
                    const query = new URLSearchParams({
                        buildId: props.buildId,
                        taskId: props.taskId
                    })
                    if (props.executeCount) {
                        query.append('executeCount', props.executeCount)
                    }
                    const { data } = await proxy.$ajax.get(`${PROCESS_API_URL_PREFIX}/user/builds/${projectId}/${pipelineId}/getTaskProgressDetail?${query.toString()}`)
                    if (currentRequestId === requestId.value) {
                        progressData.value = data
                    }
                } catch (err) {
                    if (currentRequestId === requestId.value) {
                        proxy.$bkMessage({
                            theme: 'error',
                            message: err.message || proxy.$t('progressDetail.loadFailed'),
                            limit: 1
                        })
                    }
                } finally {
                    if (currentRequestId === requestId.value) {
                        loading.value = false
                        refreshing.value = false
                        scheduleRefresh()
                    }
                }
            }

            const scheduleRefresh = () => {
                clearRefreshTimer()
                if (shouldRefresh.value) {
                    refreshTimer.value = setTimeout(() => fetchProgressDetail(), 3000)
                }
            }

            const reload = () => {
                clearRefreshTimer()
                fetchProgressDetail({ showRefreshing: true })
            }

            watch(() => [props.taskId, props.executeCount], reload)
            watch(() => props.detailData, (data) => {
                clearRefreshTimer()
                requestId.value += 1
                progressData.value = data
                loading.value = false
                refreshing.value = false
            })
            watch(() => props.taskStatus, () => scheduleRefresh())

            onMounted(() => {
                fetchProgressDetail({ showLoading: !progressData.value })
            })

            onBeforeUnmount(() => {
                requestId.value += 1
                clearRefreshTimer()
            })

            return {
                loading,
                refreshing,
                visible,
                hasProgressDetail,
                progressInfo,
                progressValue,
                subtasksInfo,
                timelineInfo,
                progressPercent,
                progressTheme,
                progressTitle,
                timelineList,
                canRefresh,
                reload,
                normalizeProgress,
                subtaskDisplayText,
                subtaskStatusClass,
                subtaskStatusIcon,
                subtaskProgressTheme,
                getSummaryParts
            }
        }
    })
</script>

<style lang="scss" scoped>
@import "@/scss/conf";

.progress-detail-panel {
    flex: 0 0 auto;
    margin: 0 12px 12px;
    color: $fontBoldColor;
    font-size: 12px;
    &.is-page {
        height: 100%;
        margin: 0;
        background: $bgHoverColor;
        overflow-y: auto;
    }
}
.progress-detail-header {
    display: flex;
    align-items: center;
    height: 52px;
    padding: 0 24px;
    border-bottom: 1px solid $borderWeightColor;
    background: #fff;
    strong {
        font-size: 16px;
        font-weight: 700;
        color: $fontBoldColor;
    }
}
.progress-detail-header-meta {
    margin-left: 16px;
    color: $fontColor;
}
.progress-detail-refresh {
    margin-left: 24px;
    i {
        display: inline-block;
        margin-right: 4px;
        font-size: 14px;
    }
}
.progress-detail-body {
    padding: 20px 24px 24px;
    min-height: 180px;
}
.progress-detail-empty {
    padding: 40px 0;
}
.progress-detail-section + .progress-detail-section {
    margin-top: 14px;
}
.progress-detail-section {
    padding: 18px 20px 22px;
    border: 1px solid $borderColor;
    border-radius: 2px;
    background: #fff;
    box-shadow: 0 1px 2px rgba(49, 50, 56, .04);
}
.progress-section-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 22px;
}
.timeline-section {
    .progress-section-head {
        margin-bottom: 10px;
    }
}
.progress-section-title {
    position: relative;
    padding-left: 12px;
    font-size: 14px;
    font-weight: 700;
    color: $fontBoldColor;
    &:before {
        content: "";
        position: absolute;
        left: 0;
        top: 2px;
        width: 4px;
        height: 16px;
        border-radius: 2px;
        background: $primaryColor;
    }
}
.progress-section-summary {
    padding: 3px 12px;
    border-radius: 11px;
    background: $bgHoverColor;
    color: $fontWeightColor;
    b {
        color: $primaryColor;
        font-weight: 700;
    }
    span {
        color: $fontBoldColor;
    }
}
.progress-overview-meta,
.progress-overview-value {
    display: flex;
    align-items: center;
    justify-content: space-between;
}
.progress-overview-meta {
    margin-bottom: 4px;
    color: $fontColor;
}
.progress-overview-value {
    margin-bottom: 12px;
    strong {
        font-size: 22px;
        line-height: 30px;
        font-weight: 400;
        color: $fontBoldColor;
    }
    span {
        color: $fontWeightColor;
    }
    .summary-value {
        color: $fontWeightColor;
        em {
            font-style: normal;
            color: $successColor;
        }
        span {
            color: $fontWeightColor;
        }
    }
}
.subtask-item {
    display: grid;
    grid-template-columns: 16px minmax(90px, 150px) 1fr 62px;
    align-items: center;
    column-gap: 10px;
}
.subtask-list {
    margin: 0;
    padding: 0;
    list-style: none;
}
.subtask-item + .subtask-item {
    margin-top: 16px;
}
.subtask-status-icon {
    font-size: 14px;
    line-height: 14px;
    &.is-succeeded {
        color: $successColor;
    }
    &.is-failed {
        color: $dangerColor;
    }
    &.is-running {
        color: $primaryColor;
    }
    &.is-pending {
        color: $fontColor;
    }
}
.subtask-name {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    color: $fontBoldColor;
}
.subtask-item.is-succeeded {
    .subtask-name {
        color: $fontBoldColor;
        font-weight: 500;
    }
}
.subtask-item.is-pending {
    .subtask-name {
        color: $fontWeightColor;
    }
}
.subtask-status {
    color: $fontWeightColor;
    text-align: right;
    &.is-succeeded {
        color: $successColor;
    }
    &.is-failed {
        color: $dangerColor;
    }
    &.is-running {
        color: $primaryColor;
    }
    &.is-pending {
        color: $fontColor;
    }
}

.timeline-wrap {
    padding-top: 12px;
}
.timeline-meta {
    display: flex;
    gap: 16px;
    margin: 0;
    color: #979ba5;
    font-size: 13px;
    line-height: 18px;
}
</style>

<template>
    <div
        class="lp-status"
        :class="[`is-${conclusion.tone}`, { 'is-expanded': (queueExpanded && conclusion.queueDetail) || (progressExpanded && displaySubtasks.length), 'is-queue-detail': conclusion.queueDetail }]"
    >
        <div class="lp-status-row">
            <div class="lp-status-body">
                <slot name="prefix-actions"></slot>
                <strong class="lp-status-label">{{ conclusion.label }}</strong>
                <span
                    v-if="conclusion.elapsed"
                    class="lp-meta"
                >{{ $t('duration') }} {{ displayElapsed }}</span>
                <template v-if="conclusion.node">
                    <span class="lp-vdiv"></span>
                    <span class="lp-meta">
                        <a
                            v-if="conclusion.nodeLink"
                            class="lp-node-link"
                            :href="conclusion.nodeLink"
                            target="_blank"
                            rel="noopener"
                            @click.stop
                        >{{ $t(conclusion.queueDetail ? 'logPanel.runningNode' : 'logPanel.statusNode') }}{{ conclusion.queueDetail ? ' ' : ': ' }}{{ conclusion.node }}</a>
                        <template v-else>{{ $t(conclusion.queueDetail ? 'logPanel.runningNode' : 'logPanel.statusNode') }}{{ conclusion.queueDetail ? ' ' : ': ' }}{{ conclusion.node }}</template>
                        <span
                            v-if="conclusion.nodeIp"
                            class="lp-node-ip"
                        >（{{ conclusion.nodeIp }}）</span>
                    </span>
                </template>
                <template v-if="conclusion.cancelUserId">
                    <span class="lp-vdiv"></span>
                    <span class="lp-meta lp-canceller">{{ $t('logPanel.canceller') }}: {{ conclusion.cancelUserId }}</span>
                </template>
                <template v-if="conclusion.node && (conclusion.errorCode || conclusion.locateLog || conclusion.showNodeMessageDivider)">
                    <span class="lp-vdiv"></span>
                </template>
                <template v-if="progress !== null && progress !== undefined || hasSubtasks">
                    <span class="lp-progress">
                        <template v-if="progress != null">
                            <span class="lp-progress-track">
                                <i
                                    class="lp-progress-fill"
                                    :style="{ width: progress + '%' }"
                                ></i>
                            </span>
                            <em>{{ progress }}%</em>
                        </template>
                        <button
                            v-if="hasSubtasks"
                            type="button"
                            class="lp-expand"
                            :aria-expanded="String(progressExpanded)"
                            :aria-label="$t('progressDetail.subtasks')"
                            @click.stop="$emit('toggle-progress')"
                        >
                            <i
                                class="devops-icon icon-angle-right lp-expand-icon"
                                aria-hidden="true"
                                :class="{ 'is-open': progressExpanded }"
                            ></i>
                        </button>
                    </span>
                </template>
                <span
                    v-if="conclusion.errorCode || conclusion.locateLog"
                    class="lp-error-detail"
                >
                    <span
                        v-if="conclusion.errorCode"
                        class="lp-code"
                    >{{ conclusion.errorCode }}</span>
                    <span
                        v-if="displayMessage"
                        class="lp-msg"
                    >{{ displayMessage }}</span>
                    <button
                        v-if="conclusion.locateLog"
                        type="button"
                        class="lp-link"
                        @click.stop="$emit('locate')"
                    >{{ $t('logPanel.locateLine') }}</button>
                </span>
                <span
                    v-else-if="displayMessage"
                    class="lp-msg"
                >{{ displayMessage }}</span>
                <span
                    v-if="conclusion.conditionSkipSummary"
                    class="lp-msg"
                >{{ conclusion.conditionSkipSummary }}</span>
                <a
                    v-if="conclusion.parentBuildUrl"
                    class="lp-link"
                    :href="conclusion.parentBuildUrl"
                    target="_blank"
                    rel="noopener"
                >{{ $t('logPanel.status.parentBuild') }}</a>
                <span
                    v-else-if="conclusion.parentBuildLabel"
                    class="lp-link is-disabled"
                >{{ $t('logPanel.status.parentBuild') }}</span>
                <span
                    v-if="conclusion.queueDetail && displayMessage"
                    class="lp-vdiv"
                ></span>
                <span
                    v-if="conclusion.queueDetail"
                    class="lp-queue-meta"
                >
                    <span
                        v-if="conclusion.queueDetail.position != null"
                        class="lp-queue-position"
                    >
                        <span class="lp-queue-position-label">{{ $t('logPanel.queue.positionLabel') }}</span>
                        <span class="lp-queue-position-value">{{ $t(conclusion.queueDetail.total != null ? 'logPanel.queue.positionTotalValue' : 'logPanel.queue.positionValue', [conclusion.queueDetail.position, conclusion.queueDetail.total]) }}</span>
                    </span>
                    <span v-if="conclusion.queueDetail.waitedMinutes">{{ $t('logPanel.queue.waitedMinutes', [conclusion.queueDetail.waitedMinutes]) }}</span>
                    <button
                        type="button"
                        class="lp-link"
                        :aria-expanded="String(queueExpanded)"
                        :aria-label="$t(queueExpanded ? 'logPanel.queue.hide' : 'logPanel.queue.show')"
                        :title="$t(queueExpanded ? 'logPanel.queue.hide' : 'logPanel.queue.show')"
                        @click.stop="queueExpanded = !queueExpanded"
                    >
                        <i
                            class="devops-icon icon-angle-right lp-expand-icon"
                            aria-hidden="true"
                            :class="{ 'is-open': queueExpanded }"
                        ></i>
                    </button>
                </span>
                <button
                    v-if="conclusion.conditionLink"
                    type="button"
                    class="lp-link"
                    @click.stop="$emit('go-condition')"
                >
                    {{ $t('logPanel.viewCondition') }}
                </button>
                <bk-button
                    v-if="conclusion.continueConfig"
                    size="small"
                    theme="warning"
                    class="lp-handle"
                    @click.stop="$emit('continue-config')"
                >
                    {{ $t('logPanel.continueConfig') }}
                </bk-button>
                <bk-button
                    v-if="conclusion.review"
                    size="small"
                    theme="warning"
                    class="lp-handle"
                    @click.stop="$emit('review')"
                >
                    {{ $t('logPanel.reviewAction') }}
                </bk-button>
            </div>
            <div class="lp-status-actions">
                <slot name="right-actions"></slot>
                <span
                    v-if="summaryText"
                    class="lp-summary"
                >{{ summaryText }}</span>
                <span
                    v-if="conclusion.canRetry || conclusion.canSkip"
                    class="lp-ops"
                >
                    <bk-button
                        v-if="conclusion.canRetry"
                        size="small"
                        outline
                        class="lp-ops-btn"
                        :loading="pendingAction === 'retry'"
                        :disabled="Boolean(pendingAction)"
                        @click.stop="$emit('retry')"
                    >{{ $t('logPanel.retry') }}</bk-button>
                    <bk-button
                        v-if="conclusion.canSkip"
                        size="small"
                        outline
                        class="lp-ops-btn"
                        :loading="pendingAction === 'skip'"
                        :disabled="Boolean(pendingAction)"
                        @click.stop="$emit('skip')"
                    >{{ $t('logPanel.skip') }}</bk-button>
                </span>
            </div>
        </div>
        <div
            v-if="progressExpanded && displaySubtasks.length"
            class="lp-subtasks"
        >
            <div
                v-for="(row, index) in displaySubtasks"
                :key="index"
                class="lp-subtask"
            >
                <span class="lp-subtask-main">
                    <i
                        v-if="row.status && row.icon === 'clock'"
                        class="devops-icon icon-clock subtask-status-icon"
                        :class="`is-${row.status}`"
                        aria-hidden="true"
                    ></i>
                    <logo
                        v-else-if="row.status"
                        :name="row.icon"
                        :size="16"
                        :class="['subtask-status-icon', `is-${row.status}`]"
                        aria-hidden="true"
                    />
                    <span>{{ row.name }}</span>
                    <span
                        v-if="row.progress"
                        class="lp-subtask-pct"
                    >{{ row.progress }}</span>
                </span>
                <span class="lp-subtask-aside">
                    <span
                        class="lp-subtask-range"
                    >{{ row.timeRange }}</span>
                    <span
                        class="lp-subtask-elapsed"
                        :class="{ 'is-live': row.live, 'is-idle': row.idle }"
                    >{{ row.elapsed || (row.idle ? $t('logPanel.subtaskNotStarted') : '') }}</span>
                </span>
            </div>
        </div>
        <job-queue-detail
            v-if="queueExpanded && conclusion.queueDetail"
            :queue="conclusion.queueDetail"
        />
    </div>
</template>

<script>
    import Logo from '@/components/Logo'
    import { resolveConclusionDisplayMessage, subtaskStatusIcon } from './logPanelAdapter'
    import JobQueueDetail from './JobQueueDetail'

    export default {
        name: 'LogConclusionBar',
        components: { JobQueueDetail, Logo },
        props: {
            conclusion: { type: Object, required: true },
            pendingAction: { type: String, default: '' },
            progress: { type: Number, default: null },
            hasSubtasks: { type: Boolean, default: false },
            progressExpanded: { type: Boolean, default: false },
            subtasks: { type: Array, default: () => [] }
        },
        data () {
            return {
                queueExpanded: Boolean(this.conclusion.queueExpanded)
            }
        },
        computed: {
            displayElapsed () {
                const elapsed = String(this.conclusion.elapsed || '')
                return this.conclusion.queueDetail ? elapsed.replace(/ 0s$/, '') : elapsed
            },
            displayMessage () {
                return resolveConclusionDisplayMessage(this.conclusion, key => this.$t(key))
            },
            summaryText () {
                const list = this.subtasks || []
                if (!list.length) return ''
                const done = list.filter(task => task.status === 'succeeded').length
                return this.$t('logPanel.subtaskSummary', [done, list.length])
            },
            displaySubtasks () {
                return (this.subtasks || []).map(task => {
                    const status = String(task.status || 'pending').toLowerCase()
                    return {
                        name: task.name,
                        status,
                        icon: subtaskStatusIcon(status),
                        progress: task.progress || '',
                        timeRange: task.timeRange || '',
                        elapsed: task.elapsed || '',
                        live: Boolean(task.live),
                        idle: Boolean(task.idle)
                    }
                })
            }
        },
        watch: {
            'conclusion.queueExpanded' (expanded) {
                if (typeof expanded === 'boolean') this.queueExpanded = expanded
            }
        }
    }
</script>

<style lang="scss" scoped>
    .lp-status { box-sizing: border-box; height: 37px; flex-shrink: 0; background: #242a36; border-top: 1px solid #151c28; color: #83828c; font: 12px/20px "Microsoft YaHei", sans-serif; }
    .lp-status.is-expanded { height: auto; }
    .lp-status.is-queue-detail { background: #242a36; color: #83828c; }
    .lp-status.is-queue-detail .lp-status-label { color: #83828c; }
    .lp-status.is-success { background: #2b3329; color: #83828c; }
    .lp-status.is-failed, .lp-status.is-danger { background: #3d2929; }
    .lp-status.is-canceled, .lp-status.is-queue, .lp-status.is-pause, .lp-status.is-waiting { background: #3b342b; }
    .lp-status-row { display: flex; align-items: center; box-sizing: border-box; height: 36px; min-height: 36px; padding: 8px 16px; }
    .lp-status-row > * + * { margin-left: 16px; }
    .lp-status-body { flex: 1; min-width: 0; display: flex; flex-wrap: nowrap; align-items: center; gap: 4px 16px; overflow: hidden; }
    .lp-status-body > .execute-count-picker { margin-right: -8px; }
    .lp-status-actions { display: inline-flex; align-items: center; flex-shrink: 0; min-height: 20px; }
    .lp-status-actions > * + * { margin-left: 16px; }
    .lp-status-actions ::v-deep .lp-debug-action.is-outline { height: 24px; min-height: 24px; padding: 0 12px; font-size: 12px; line-height: 22px; box-sizing: border-box; background: rgba(107, 71, 71, 0.4); border: 1px solid #979ba5; box-shadow: none; color: #c4c6cc; }
    .lp-status-label { font-weight: 700; white-space: nowrap; }
    .is-running .lp-status-label { color: #699df4; }
    .is-success .lp-status-label { color: #45e35f; }
    .is-failed .lp-status-label, .is-danger .lp-status-label, .is-failed .lp-msg, .is-danger .lp-msg { color: #f06e6e; }
    .is-canceled .lp-status-label, .is-canceled .lp-msg, .is-queue .lp-status-label, .is-pause .lp-status-label, .is-pause .lp-msg, .is-waiting .lp-status-label, .is-waiting .lp-msg { color: #f0aa50; }
    .is-canceled .lp-canceller { color: #f0aa50; }
    .lp-meta { color: #83828c; display: inline-block; min-width: 0; max-width: 26%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .lp-queue-meta { display: inline-flex; align-items: center; gap: 8px; color: #83828c; white-space: nowrap; }
    .lp-queue-position { display: inline-flex; align-items: center; gap: 4px; }
    .lp-queue-position-label { color: #83828c; }
    .lp-queue-position-value { color: #f0aa50; }
    .lp-queue-meta .lp-expand-icon { display: inline-flex; width: 12px; height: 12px; font-size: 12px; transition: transform .15s ease; }
    .lp-queue-meta .is-open { transform: rotate(90deg); }
    .is-failed .lp-meta, .is-danger .lp-meta, .is-failed .lp-node-link, .is-danger .lp-node-link, .is-failed .lp-node-ip, .is-danger .lp-node-ip { color: #979ba5; }
    .lp-node-link { color: #83828c; text-decoration: none; border-bottom: 1px dashed #63656e; &:hover { color: #699df4; border-bottom-color: #699df4; } }
    .lp-node-ip { color: #63656e; }
    .lp-vdiv { width: 1px; height: 12px; background: #4d4f56; flex-shrink: 0; }
    .lp-progress { display: inline-flex; align-items: center; }
    .lp-progress > * + * { margin-left: 8px; }
    .lp-progress-track { display: block; width: 120px; height: 4px; background: #4d4f56; border-radius: 2px; overflow: hidden; }
    .lp-progress-fill { display: block; height: 4px; background: #3a84ff; border-radius: 2px; }
    .lp-progress em { font-style: normal; color: #83828c; }
    .lp-expand { display: inline-flex; border: none; background: transparent; color: #83828c; cursor: pointer; padding: 0; .lp-expand-icon { display: inline-flex; width: 12px; height: 12px; font-size: 12px; transform-origin: center; } .is-open { transform: rotate(90deg); } }
    .lp-code { height: 16px; padding: 0 6px; border-radius: 2px; background: #503030; color: #f06e6e; font-size: 10px; line-height: 16px; flex-shrink: 0; }
    .lp-error-detail { display: inline-flex; flex: 1 1 0; align-items: center; flex-wrap: nowrap; min-width: 0; max-width: 100%; gap: 8px; overflow: hidden; }
    .lp-msg { flex: 1 1 0; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .lp-link { border: none; background: transparent; color: #699df4; font: 12px/20px "Microsoft YaHei", sans-serif; cursor: pointer; padding: 0; white-space: nowrap; }
    .lp-link.is-disabled { color: #699df4; cursor: default; }
    .lp-summary { color: #83828c; white-space: nowrap; text-align: left; }
    .lp-ops { display: inline-flex; }
    .lp-ops > * + * { margin-left: 8px; }
    .lp-ops ::v-deep button.lp-ops-btn.is-outline { height: 24px; min-height: 24px; min-width: 48px; padding: 0 12px; font-size: 12px; line-height: 22px; box-sizing: border-box; background: rgba(107, 71, 71, 0.4); border: 1px solid #979ba5; box-shadow: none; color: #c4c6cc; }
    .lp-handle { margin-left: 4px; }
    .is-pause .lp-handle, .is-waiting .lp-handle { height: 24px; min-width: 0; padding: 0 12px; border-radius: 4px; border-color: #e18732; background: #e18732; color: #fff; font-size: 12px; line-height: 22px; }
    .lp-subtasks { background: #242a36; display: flex; flex-direction: column; padding: 12px 64px; }
    .lp-subtasks > * + * { margin-top: 8px; }
    .lp-subtask { display: flex; align-items: center; justify-content: space-between; height: 20px; }
    .lp-subtask-main { display: inline-flex; flex: 1; align-items: center; min-width: 0; color: #f0f1f5; }
    .lp-subtask-main > * + * { margin-left: 8px; }
    .subtask-status-icon.is-succeeded { color: #5ac882; }
    .subtask-status-icon.is-failed { color: #f06e6e; }
    .subtask-status-icon.is-running { color: #699df4; animation: rotating infinite .6s ease-in-out; }
    .subtask-status-icon.is-pending { color: #63656e; }
    .subtask-status-icon.devops-icon { width: 16px; height: 16px; font-size: 16px; }
    @media (prefers-reduced-motion: reduce) { .subtask-status-icon.is-running { animation: none; } }
    .lp-subtask-aside { display: inline-flex; align-items: center; flex-shrink: 0; }
    .lp-subtask-aside > * + * { margin-left: 24px; }
    .lp-subtask-pct { color: #83828c; }
    .lp-subtask-range { width: 196px; color: #83828c; text-align: left; font-variant-numeric: tabular-nums; }
    .lp-subtask-elapsed { width: 72px; color: #c4c6cc; text-align: left; font-variant-numeric: tabular-nums; }
    .lp-subtask-elapsed.is-live { color: #699df4; }
    .lp-subtask-elapsed.is-idle { color: #63656e; }
</style>

<template>
    <div class="lp-status" :class="[`is-${conclusion.tone}`, { 'is-exec-open': execOpen }]">
        <div
            class="lp-status-row"
            :class="{ 'is-expandable': canToggleExpand }"
            @click="onRowClick"
        >
            <div class="lp-status-body">
                <div v-if="executeCount > 1" ref="execWrap" class="lp-exec-wrap">
                    <button
                        type="button"
                        class="lp-exec-trigger"
                        :aria-expanded="execOpen"
                        @click.stop="execOpen = !execOpen"
                    >
                        <span>第 {{ currentExecute }} 次执行</span>
                        <i class="devops-icon icon-angle-down lp-exec-arrow"></i>
                    </button>
                    <ul v-if="execOpen" class="lp-exec-menu">
                        <li
                            v-for="n in executeCount"
                            :key="n"
                            class="lp-exec-option"
                            :class="{ active: n === currentExecute }"
                            @click.stop="pickExecute(n)"
                        >第 {{ n }} 次执行</li>
                    </ul>
                </div>
                <strong class="lp-status-label">{{ conclusion.label }}</strong>
                <span v-if="conclusion.elapsed" class="lp-meta">耗时 {{ conclusion.elapsed }}</span>
                <template v-if="conclusion.node">
                    <span class="lp-vdiv"></span>
                    <span class="lp-meta">
                        运行节点
                        <a
                            v-if="conclusion.nodeLink"
                            class="lp-node-link"
                            :href="conclusion.nodeLink"
                            target="_blank"
                            rel="noopener"
                            @click.stop
                        >{{ conclusion.node }}</a>
                        <template v-else>{{ conclusion.node }}</template>
                        <span v-if="conclusion.nodeIp" class="lp-node-ip">（{{ conclusion.nodeIp }}）</span>
                    </span>
                </template>
                <template v-if="progress != null">
                    <span class="lp-progress">
                        <span class="lp-progress-track">
                            <i class="lp-progress-fill" :style="{ width: progress + '%' }"></i>
                        </span>
                        <em>{{ progress }}%</em>
                        <button
                            v-if="hasSubtasks"
                            type="button"
                            class="lp-expand"
                            @click.stop="$emit('toggle-progress')"
                        >
                            <i
                                class="devops-icon icon-angle-right"
                                :class="{ 'is-open': progressExpanded }"
                            ></i>
                        </button>
                    </span>
                </template>
                <template v-if="conclusion.message && !conclusion.errorCode && !conclusion.locateLog">
                    <span class="lp-vdiv"></span>
                    <span class="lp-msg">{{ conclusion.message }}</span>
                    <button
                        v-if="conclusion.askAssistant"
                        type="button"
                        class="lp-ai"
                        title="问助手：排查本步骤失败原因"
                        @click.stop="$emit('ask-assistant')"
                    >
                        <svg viewBox="0 0 16 16" width="16" height="16" aria-hidden="true">
                            <path d="M8 1.6l1.1 3.2L12.4 6 9.1 7.2 8 10.4 6.9 7.2 3.6 6l3.3-1.2L8 1.6z" fill="currentColor" />
                            <path d="M12.6 9.2l.6 1.6 1.6.6-1.6.6-.6 1.6-.6-1.6-1.6-.6 1.6-.6.6-1.6z" fill="currentColor" />
                        </svg>
                    </button>
                </template>
                <span v-if="conclusion.errorCode || conclusion.locateLog" class="lp-error-detail">
                    <span v-if="conclusion.errorCode" class="lp-code">{{ conclusion.errorCode }}</span>
                    <span v-if="conclusion.message" class="lp-msg">{{ conclusion.message }}</span>
                    <button
                        v-if="conclusion.locateLog"
                        type="button"
                        class="lp-link"
                        @click="$emit('locate-log')"
                    >定位日志</button>
                    <button
                        v-if="conclusion.askAssistant"
                        type="button"
                        class="lp-ai"
                        title="问助手：排查本步骤失败原因"
                        @click.stop="$emit('ask-assistant')"
                    >
                        <svg viewBox="0 0 16 16" width="16" height="16" aria-hidden="true">
                            <path d="M8 1.6l1.1 3.2L12.4 6 9.1 7.2 8 10.4 6.9 7.2 3.6 6l3.3-1.2L8 1.6z" fill="currentColor" />
                            <path d="M12.6 9.2l.6 1.6 1.6.6-1.6.6-.6 1.6-.6-1.6-1.6-.6 1.6-.6.6-1.6z" fill="currentColor" />
                        </svg>
                    </button>
                </span>
                <button
                    v-else-if="conclusion.askAssistant"
                    type="button"
                    class="lp-ai"
                    title="问助手：排查本步骤失败原因"
                    @click.stop="$emit('ask-assistant')"
                >
                    <svg viewBox="0 0 16 16" width="16" height="16" aria-hidden="true">
                        <path d="M8 1.6l1.1 3.2L12.4 6 9.1 7.2 8 10.4 6.9 7.2 3.6 6l3.3-1.2L8 1.6z" fill="currentColor" />
                        <path d="M12.6 9.2l.6 1.6 1.6.6-1.6.6-.6 1.6-.6-1.6-1.6-.6 1.6-.6.6-1.6z" fill="currentColor" />
                    </svg>
                </button>
                <button
                    v-if="conclusion.parentLink"
                    type="button"
                    class="lp-link"
                    @click.stop="$emit('go-parent')"
                >查看父构建</button>
                <button
                    v-if="conclusion.conditionLink"
                    type="button"
                    class="lp-link"
                    @click.stop="$emit('go-condition')"
                >查看运行条件</button>
                <template v-if="conclusion.queueRank || conclusion.waited">
                    <span class="lp-vdiv"></span>
                    <span v-if="conclusion.queueRank" class="lp-meta">
                        排队位置 <em class="lp-rank">{{ conclusion.queueRank }}</em>
                    </span>
                    <span v-if="conclusion.waited" class="lp-meta">已等待 {{ conclusion.waited }}</span>
                </template>
                <bk-button
                    v-if="conclusion.handleAction"
                    size="small"
                    :theme="conclusion.tone === 'pause' || conclusion.tone === 'waiting' ? 'warning' : 'primary'"
                    class="lp-handle"
                    @click="$emit('handle')"
                >{{ conclusion.handleAction }}</bk-button>
            </div>
            <div class="lp-status-actions">
                <span v-if="summaryText" class="lp-summary">{{ summaryText }}</span>
                <span v-if="conclusion.canRetry || conclusion.canSkip" class="lp-ops">
                    <bk-button
                        v-if="conclusion.canRetry"
                        size="small"
                        outline
                        class="lp-ops-btn"
                        @click="$emit('retry')"
                    >重试</bk-button>
                    <bk-button
                        v-if="conclusion.canSkip"
                        size="small"
                        outline
                        class="lp-ops-btn"
                        @click="$emit('skip')"
                    >跳过</bk-button>
                </span>
            </div>
        </div>
        <div v-if="progressExpanded && displaySubtasks.length" class="lp-subtasks">
            <div v-for="(row, i) in displaySubtasks" :key="i" class="lp-subtask">
                <span class="lp-subtask-main">
                    <status-icon
                        v-if="row.status"
                        :status="row.status"
                        small
                    />
                    <span>{{ row.name }}</span>
                    <span v-if="row.progress" class="lp-subtask-pct">{{ row.progress }}</span>
                </span>
                <span class="lp-subtask-aside">
                    <span
                        class="lp-subtask-elapsed"
                        :class="{ 'is-live': row.live, 'is-idle': row.idle }"
                    >{{ row.timeText || row.elapsed }}</span>
                    <span v-if="row.range" class="lp-subtask-range">{{ row.range }}</span>
                </span>
            </div>
        </div>
        <slot></slot>
    </div>
</template>

<script>
    import statusIcon from '../status'

    export default {
        components: { statusIcon },
        props: {
            conclusion: { type: Object, required: true },
            executeCount: { type: Number, default: 1 },
            currentExecute: { type: Number, default: 1 },
            progress: { type: Number, default: null },
            hasSubtasks: { type: Boolean, default: false },
            progressExpanded: { type: Boolean, default: false },
            subtasks: { type: Array, default: () => [] }
        },
        data () {
            return { execOpen: false }
        },
        computed: {
            canToggleExpand () {
                return this.hasSubtasks
            },
            summaryText () {
                const list = this.subtasks || []
                if (!list.length) return ''
                const done = list.filter(t => t.status === 'SUCCEED' || t.status === 'done').length
                return `已完成 ${done}/${list.length} 子任务`
            },
            displaySubtasks () {
                return (this.subtasks || []).map(task => {
                    const started = task.startedAt || ''
                    const ended = task.endedAt || ''
                    return {
                        name: task.name,
                        status: task.status || '',
                        progress: task.progress || '',
                        timeText: task.timeText || task.elapsed || '',
                        elapsed: task.elapsed || task.timeText || '',
                        range: started && ended ? `${started} ~ ${ended}` : started,
                        live: task.status === 'RUNNING' || task.status === 'running',
                        idle: ['UNEXEC', 'SKIP', 'pending'].includes(task.status)
                    }
                })
            }
        },
        mounted () {
            document.addEventListener('click', this.onDocClick, true)
        },
        beforeDestroy () {
            document.removeEventListener('click', this.onDocClick, true)
        },
        methods: {
            pickExecute (n) {
                this.execOpen = false
                if (n !== this.currentExecute) this.$emit('change-execute', n)
            },
            onRowClick (e) {
                if (!this.canToggleExpand) return
                if (e.target.closest && e.target.closest('button, a, .bk-button, .lp-exec-wrap')) return
                this.$emit('toggle-progress')
            },
            onDocClick (e) {
                if (!this.execOpen) return
                const wrap = this.$refs.execWrap
                if (wrap && !wrap.contains(e.target)) this.execOpen = false
            }
        }
    }
</script>

<style lang="scss" scoped>
.lp-status {
    flex-shrink: 0;
    background: #242a36;
    border-top: 1px solid #151c28;
    color: #83828c;
    font-size: 12px;
    line-height: 20px;
}
.lp-status.is-success { background: #2b3329; }
.lp-status.is-failed { background: #3d2929; }
.lp-status.is-canceled,
.lp-status.is-queue,
.lp-status.is-pause,
.lp-status.is-waiting { background: #3b342b; }
.lp-status.is-idle { background: #242a36; }
.lp-status.is-exec-open {
    position: relative;
    z-index: 6;
    overflow: visible;
}
.lp-status-row {
    display: flex;
    align-items: flex-start;
    gap: 8px;
    box-sizing: border-box;
    min-height: 35px;
    padding: 7px 24px 7px 16px;
}
.lp-status-row.is-expandable { cursor: pointer; }
.lp-status-body {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px;
}
.lp-status-actions {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    flex-shrink: 0;
    align-self: center;
    min-height: 20px;
}
.lp-status-label { font-weight: 700; white-space: nowrap; }
.is-running .lp-status-label { color: #699df4; }
.is-success .lp-status-label { color: #45e35f; }
.is-failed .lp-status-label,
.is-failed .lp-msg { color: #f06e6e; }
.is-canceled .lp-status-label,
.is-canceled .lp-msg,
.is-queue .lp-status-label,
.is-pause .lp-status-label,
.is-pause .lp-msg,
.is-waiting .lp-status-label,
.is-waiting .lp-msg { color: #f0aa50; }
.is-idle .lp-status-label { color: #83828c; }
.lp-meta { color: #83828c; white-space: nowrap; }
.lp-node-link {
    color: #83828c;
    text-decoration: none;
    border-bottom: 1px dashed #63656e;
    &:hover { color: #699df4; border-bottom-color: #699df4; }
}
.lp-node-ip { color: #63656e; }
.lp-vdiv { width: 1px; height: 12px; background: #4d4f56; flex-shrink: 0; }
.lp-progress { display: inline-flex; align-items: center; gap: 8px; }
.lp-progress-track {
    display: block;
    width: 120px;
    height: 4px;
    background: #4d4f56;
    border-radius: 2px;
    overflow: hidden;
}
.lp-progress-fill {
    display: block;
    height: 4px;
    background: #3a84ff;
    border-radius: 2px;
}
.lp-progress em { font-style: normal; color: #83828c; }
.lp-expand {
    display: inline-flex;
    border: none;
    background: transparent;
    color: #83828c;
    cursor: pointer;
    padding: 0;
    .devops-icon { display: inline-flex; transform-origin: center; }
    .is-open { transform: rotate(90deg); }
}
.lp-code {
    height: 16px;
    padding: 0 6px;
    border-radius: 2px;
    background: #503030;
    color: #f06e6e;
    font-size: 10px;
    line-height: 16px;
    flex-shrink: 0;
}
.lp-error-detail {
    display: inline-flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;
    min-width: min(100%, max-content);
    max-width: 100%;
}
.lp-msg { min-width: 0; white-space: normal; word-break: break-word; }
.lp-link {
    border: none;
    background: transparent;
    color: #699df4;
    font-size: 12px;
    cursor: pointer;
    padding: 0;
    white-space: nowrap;
}
.lp-ai {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 16px;
    height: 16px;
    padding: 0;
    border: none;
    background: transparent;
    color: #c4c6cc;
    cursor: pointer;
    flex-shrink: 0;
    &:hover { color: #fff; }
}
.lp-rank { font-style: normal; color: #e18732; margin-left: 4px; }
.lp-summary {
    color: #83828c;
    white-space: nowrap;
    text-align: right;
}
.lp-ops { display: inline-flex; gap: 8px; }
.lp-ops ::v-deep button.lp-ops-btn.is-outline {
    height: 24px;
    min-height: 24px;
    min-width: 48px;
    padding: 0 12px;
    font-size: 12px;
    line-height: 22px;
    box-sizing: border-box;
    background: rgba(107, 71, 71, 0.4);
    border: 1px solid #979ba5;
    box-shadow: none;
    color: #c4c6cc;
}
.lp-handle { margin-left: 4px; }
.is-pause ::v-deep .lp-handle,
.is-waiting ::v-deep .lp-handle {
    background-color: #e18732;
    border-color: #e18732;
    color: #fff;
}
.lp-exec-wrap {
    position: relative;
    width: 112px;
    height: 24px;
    flex-shrink: 0;
    z-index: 5;
}
.lp-exec-trigger {
    display: inline-flex;
    align-items: center;
    width: 112px;
    height: 24px;
    padding: 0 24px 0 8px;
    background: #464953;
    border: none;
    border-radius: 4px;
    color: #c4c6cc;
    font-size: 12px;
    line-height: 20px;
    outline: none;
    cursor: pointer;
    text-align: left;
}
.lp-exec-arrow {
    position: absolute;
    right: 8px;
    top: 50%;
    transform: translateY(-50%);
    pointer-events: none;
    color: #979ba5;
    font-size: 12px;
}
.lp-exec-menu {
    position: absolute;
    left: 0;
    top: calc(100% + 4px);
    z-index: 20;
    margin: 0;
    padding: 4px 0;
    min-width: 112px;
    list-style: none;
    background: #2c2d34;
    border: 1px solid #4d4f56;
    border-radius: 2px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.35);
}
.lp-exec-option {
    padding: 0 12px;
    height: 28px;
    line-height: 28px;
    font-size: 12px;
    color: #c4c6cc;
    cursor: pointer;
    white-space: nowrap;
    &:hover { background: #3a3f4b; color: #fff; }
    &.active { color: #3a84ff; }
}
.lp-subtasks {
    background: #21242c;
    display: flex;
    flex-direction: column;
    gap: 8px;
    padding: 12px 64px;
}
.lp-status.is-waiting .lp-subtasks,
.lp-status.is-pause .lp-subtasks,
.lp-status.is-queue .lp-subtasks,
.lp-status.is-canceled .lp-subtasks {
    background: #2b2722;
}
.lp-subtask {
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 20px;
}
.lp-subtask-main {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    color: #f0f1f5;
}
.lp-subtask-aside {
    display: inline-flex;
    align-items: center;
    gap: 12px;
    flex-shrink: 0;
}
.lp-subtask-pct { color: #83828c; }
.lp-subtask-elapsed {
    color: #c4c6cc;
    font-variant-numeric: tabular-nums;
}
.lp-subtask-elapsed.is-live { color: #699df4; }
.lp-subtask-elapsed.is-idle { color: #63656e; }
.lp-subtask-range { color: #63656e; }
</style>

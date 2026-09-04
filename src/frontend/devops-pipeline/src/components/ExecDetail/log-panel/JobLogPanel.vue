<template>
    <section class="job-log-panel">
        <div class="job-log-main">
            <aside class="job-dir">
                <button
                    v-for="item in directory"
                    :key="item.id"
                    type="button"
                    class="job-dir-item"
                    :class="[item.status, { active: selectedId === item.id, sep: item.sep }]"
                    @click="select(item)"
                >
                    <i class="status-dot"></i>
                    <span class="name">{{ item.name }}</span>
                    <span v-if="item.elapsed" class="cost">{{ item.elapsed }}</span>
                </button>
            </aside>
            <div class="job-log-body">
                <log-conclusion-bar :conclusion="jobConclusion" />
                <step-log-panel
                    :id="logQueryId"
                    :job-id="isSetup ? job.containerHashId : undefined"
                    :key="selectedId + '-' + (isSetup ? 'job' : 'step')"
                    :build-id="buildId"
                    :execute-count="selectedExecute"
                    :exec-detail="execDetail"
                    :element="selectedElement"
                    :job="job"
                    hide-conclusion
                    @fallback="$emit('fallback', $event)"
                    @ask-assistant="$emit('ask-assistant')"
                    @handle="$emit('handle')"
                />
            </div>
        </div>
    </section>
</template>

<script>
    import StepLogPanel from './StepLogPanel'
    import LogConclusionBar from './LogConclusionBar'
    import { formatElapsed, buildConclusion } from './logPanelAdapter'

    export default {
        components: { StepLogPanel, LogConclusionBar },
        props: {
            buildId: String,
            execDetail: { type: Object, required: true },
            job: { type: Object, required: true },
            plugins: { type: Array, default: () => [] }
        },
        data () {
            return {
                selectedId: ''
            }
        },
        computed: {
            directory () {
                const setupId = `startVM-${this.job.id}`
                const setup = {
                    id: setupId,
                    name: 'Set up job',
                    status: this.job.startVMStatus,
                    elapsed: formatElapsed(this.job.startTime, this.job.endTime, this.job.status),
                    setup: true
                }
                const items = [setup]
                const posts = []
                ;(this.job.elements || []).forEach(el => {
                    const row = {
                        id: el.id,
                        name: el.name,
                        status: el.status,
                        elapsed: formatElapsed(el.startTime || el.elapsed, el.endTime, el.status),
                        element: el
                    }
                    if (el.additionalOptions && el.additionalOptions.elementPostInfo) {
                        posts.push(row)
                    } else {
                        items.push(row)
                    }
                })
                if (posts.length) {
                    items.push({ id: 'sep-post', name: '收尾', sep: true })
                    items.push(...posts)
                }
                return items
            },
            selected () {
                return this.directory.find(i => i.id === this.selectedId) || this.directory[0] || {}
            },
            isSetup () {
                return !!this.selected.setup
            },
            selectedElement () {
                if (this.selected.element) return this.selected.element
                return {
                    status: this.job.status,
                    startTime: this.job.startTime,
                    endTime: this.job.endTime,
                    errorMsg: this.job.errorMsg,
                    errorCode: this.job.errorCode
                }
            },
            selectedExecute () {
                return (this.selected.element && this.selected.element.executeCount) || this.job.executeCount || 1
            },
            logQueryId () {
                return this.selected.setup ? undefined : this.selectedId
            },
            jobConclusion () {
                const failedPlugin = (this.job.elements || []).some(el =>
                    ['FAILED', 'EXEC_TIMEOUT', 'HEARTBEAT_TIMEOUT'].includes(el.status)
                )
                return buildConclusion(this.job, this.job, {
                    projectId: this.$route.params.projectId,
                    askAssistant: !failedPlugin && ['FAILED', 'HEARTBEAT_TIMEOUT'].includes(this.job.status)
                })
            }
        },
        watch: {
            'job.id': {
                immediate: true,
                handler () {
                    this.selectedId = this.directory[0] ? this.directory[0].id : ''
                }
            }
        },
        methods: {
            select (item) {
                if (item.sep) return
                this.selectedId = item.id
            }
        }
    }
</script>

<style lang="scss" scoped>
.job-log-panel, .job-log-main {
    display: flex;
    flex: 1 1 auto;
    min-height: 0;
}
.job-dir {
    width: 220px;
    flex: none;
    overflow: auto;
    background: #242a36;
    border-right: 1px solid #3a3c45;
}
.job-dir-item {
    display: flex;
    align-items: center;
    gap: 6px;
    width: 100%;
    padding: 8px 10px;
    border: 0;
    background: transparent;
    color: #c4c6cc;
    text-align: left;
    cursor: pointer;
    &.active { background: #1a6df3; color: #fff; }
    &.sep { cursor: default; color: #63656e; font-size: 12px; }
    .name { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .cost { color: #979ba5; font-size: 12px; }
    .status-dot {
        width: 6px;
        height: 6px;
        border-radius: 50%;
        background: #979ba5;
        flex: none;
    }
    &.SUCCEED .status-dot { background: #2dcb56; }
    &.FAILED, &.EXEC_TIMEOUT, &.HEARTBEAT_TIMEOUT { .status-dot { background: #ea3636; } }
    &.RUNNING .status-dot { background: #3a84ff; }
}
.job-log-body { flex: 1; min-width: 0; display: flex; flex-direction: column; }
</style>

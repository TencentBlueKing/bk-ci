<template>
    <section class="step-log-panel">
        <log-panel-toolbar
            :visible="true"
            :selected-levels="selectedLevels"
            :keyword="keyword"
            :hit-index="hitIndex"
            :hit-count="hitCount"
            :show-time="showTime"
            :wrap="wrap"
            @update:selectedLevels="onLevels"
            @update:keyword="onKeyword"
            @prev="prevHit"
            @next="nextHit"
            @toggle-time="showTime = !showTime"
            @toggle-wrap="wrap = !wrap"
            @download="downloadLog"
        />
        <log-conclusion-bar
            v-if="!hideConclusion"
            :conclusion="conclusion"
            :execute-count="executeCount || 1"
            :current-execute="currentExecute"
            :progress="progressPercent"
            :has-subtasks="!!subtasks.length"
            :progress-expanded="progressExpanded"
            :subtasks="subtasks"
            @change-execute="onExecute"
            @toggle-progress="progressExpanded = !progressExpanded"
            @locate-log="locateFirstIssue"
            @ask-assistant="$emit('ask-assistant')"
            @handle="$emit('handle')"
            @retry="$emit('retry')"
            @skip="$emit('skip')"
        />
        <log-group-bar
            v-if="subTags.length"
            :value="subTag"
            :tags="subTags"
            @input="onSubTag"
        />
        <div class="lp-body">
            <log-lines-view
                ref="lines"
                :lines="displayLines"
                :keyword="keyword"
                :show-time="showTime"
                :wrap="wrap"
                :empty-text="emptyText"
                :locate-index="locateIndex"
                :active-index="activeIndex"
                @reach-top="loadBefore"
                @stick-change="onStick"
                @jump="jumpTo"
            />
        </div>
    </section>
</template>

<script>
    import { mapActions } from 'vuex'
    import LogPanelToolbar from './LogPanelToolbar'
    import LogConclusionBar from './LogConclusionBar'
    import LogLinesView from './LogLinesView'
    import LogGroupBar from './LogGroupBar'
    import { buildConclusion } from './logPanelAdapter'
    import { PROCESS_API_URL_PREFIX } from '@/store/constants'

    export default {
        components: { LogPanelToolbar, LogConclusionBar, LogLinesView, LogGroupBar },
        props: {
            id: String,
            jobId: String,
            buildId: String,
            executeCount: Number,
            execDetail: { type: Object, required: true },
            element: { type: Object, default: () => ({}) },
            job: { type: Object, default: () => ({}) },
            hideConclusion: { type: Boolean, default: false }
        },
        data () {
            return {
                selectedLevels: ['INFO', 'WARN', 'ERROR'],
                keyword: '',
                showTime: false,
                wrap: true,
                currentExecute: this.executeCount || 1,
                subTag: '',
                subTags: [],
                logs: [],
                startLineNo: null,
                endLineNo: null,
                hasBefore: false,
                hasAfter: false,
                finished: false,
                cleaned: false,
                loading: false,
                stickBottom: true,
                hitIndex: 0,
                pollId: null,
                progressExpanded: false,
                progressPercent: null,
                subtasks: [],
                locateIndex: -1
            }
        },
        computed: {
            conclusion () {
                return buildConclusion(this.element, this.job, {
                    projectId: this.$route.params.projectId,
                    askAssistant: ['FAILED', 'EXEC_TIMEOUT', 'HEARTBEAT_TIMEOUT'].includes(this.element.status)
                })
            },
            displayLines () {
                return this.logs
            },
            hitIndexes () {
                const kw = (this.keyword || '').trim().toLowerCase()
                if (!kw) return []
                return this.logs
                    .map((l, i) => ((l.message || '').toLowerCase().includes(kw) ? i : -1))
                    .filter(i => i >= 0)
            },
            hitCount () {
                return this.hitIndexes.length
            },
            activeIndex () {
                return this.hitCount ? this.hitIndexes[this.hitIndex] : -1
            },
            queryLevels () {
                const sel = this.selectedLevels || []
                if (sel.length === 1 && sel[0] === 'ALL') return 'INFO,WARN,ERROR,DEBUG'
                return sel.join(',')
            },
            emptyText () {
                if (this.cleaned) return '构建日志已超过保留期，已被清理，无法查看。'
                if (!this.logs.length) return this.conclusion.emptyText || ''
                return ''
            },
            queryBase () {
                return {
                    projectId: this.$route.params.projectId,
                    pipelineId: this.$route.params.pipelineId,
                    buildId: this.buildId,
                    tag: this.id || undefined,
                    jobId: this.jobId || undefined,
                    subTag: this.subTag || undefined,
                    executeCount: this.currentExecute,
                    levels: this.queryLevels
                }
            }
        },
        watch: {
            id: {
                immediate: true,
                handler () { this.reload() }
            }
        },
        beforeDestroy () {
            this.stopPoll()
        },
        methods: {
            ...mapActions('atom', [
                'getLogPanelLatest',
                'getLogPanelBefore',
                'getLogPanelAfter',
                'getLogStatus',
                'getDownloadLogFromArtifactory'
            ]),
            onLevels (levels) {
                this.selectedLevels = levels
                this.hitIndex = 0
                this.reload()
            },
            onKeyword (val) {
                this.keyword = val
                this.hitIndex = 0
            },
            prevHit () {
                if (!this.hitCount) return
                this.hitIndex = (this.hitIndex - 1 + this.hitCount) % this.hitCount
            },
            nextHit () {
                if (!this.hitCount) return
                this.hitIndex = (this.hitIndex + 1) % this.hitCount
            },
            onExecute (n) {
                this.currentExecute = n
                this.reload()
            },
            onSubTag (val) {
                this.subTag = val
                this.reload()
            },
            async reload () {
                this.stopPoll()
                this.logs = []
                this.startLineNo = null
                this.endLineNo = null
                await this.loadLatest()
                this.fetchProgress()
            },
            async loadLatest () {
                if (!this.buildId || (!this.id && !this.jobId)) return
                this.loading = true
                try {
                    const res = await this.getLogPanelLatest(this.queryBase)
                    this.applyPage(res.data || {}, true)
                    this.$nextTick(() => this.$refs.lines && this.$refs.lines.scrollToBottom())
                    this.schedulePoll()
                } catch (err) {
                    this.$emit('fallback', err)
                } finally {
                    this.loading = false
                }
            },
            async loadBefore () {
                if (!this.hasBefore || this.loading || !this.startLineNo) return
                this.loading = true
                const box = this.$refs.lines && this.$refs.lines.$refs.box
                const prevHeight = box ? box.scrollHeight : 0
                try {
                    const res = await this.getLogPanelBefore({
                        ...this.queryBase,
                        endLineNo: this.startLineNo
                    })
                    const data = res.data || {}
                    const incoming = data.logs || []
                    this.logs = incoming.concat(this.logs)
                    this.startLineNo = data.startLineNo != null ? data.startLineNo : this.startLineNo
                    this.hasBefore = !!data.hasBefore
                    this.$nextTick(() => {
                        if (box) box.scrollTop = box.scrollHeight - prevHeight
                    })
                } catch (_) {}
                this.loading = false
            },
            async loadAfter () {
                if (this.loading) return
                if (this.endLineNo == null) return this.loadLatest()
                this.loading = true
                try {
                    const res = await this.getLogPanelAfter({
                        ...this.queryBase,
                        startLineNo: this.endLineNo
                    })
                    this.applyPage(res.data || {}, false)
                    if (this.stickBottom) this.$nextTick(() => this.$refs.lines && this.$refs.lines.scrollToBottom())
                    this.schedulePoll()
                } catch (_) {
                    this.schedulePoll()
                }
                this.loading = false
            },
            applyPage (data, replace) {
                this.finished = !!data.finished
                this.cleaned = !!data.cleaned
                const incoming = data.logs || []
                this.logs = replace ? incoming : this.logs.concat(incoming)
                if (data.startLineNo != null && (replace || this.startLineNo == null)) {
                    this.startLineNo = data.startLineNo
                }
                if (data.endLineNo != null) this.endLineNo = data.endLineNo
                this.hasBefore = !!data.hasBefore
                this.hasAfter = !!data.hasAfter
                this.subTags = (data.subTags || []).map(t => ({ label: t, value: t }))
            },
            onStick (atBottom) {
                this.stickBottom = atBottom
            },
            schedulePoll () {
                this.stopPoll()
                if (!this.finished || this.hasAfter) {
                    this.pollId = setTimeout(() => {
                        if (this.stickBottom) this.loadAfter()
                        else this.schedulePoll()
                    }, 1000)
                }
            },
            stopPoll () {
                clearTimeout(this.pollId)
                this.pollId = null
            },
            locateFirstIssue () {
                const idx = this.logs.findIndex(l => l.level === 'ERROR' || l.level === 'WARN')
                this.jumpTo(idx >= 0 ? idx : 0)
            },
            jumpTo (idx) {
                this.locateIndex = idx
                this.$nextTick(() => this.$refs.lines && this.$refs.lines.scrollToIndex(idx))
            },
            async fetchProgress () {
                try {
                    const { projectId, pipelineId } = this.$route.params
                    const query = new URLSearchParams({
                        buildId: this.buildId,
                        taskId: this.id
                    })
                    if (this.currentExecute) query.append('executeCount', this.currentExecute)
                    const { data } = await this.$ajax.get(
                        `${PROCESS_API_URL_PREFIX}/user/builds/${projectId}/${pipelineId}/getTaskProgressDetail?${query}`
                    )
                    const percent = data && (data.progress || data.percent || (data.overview && data.overview.percent))
                    this.progressPercent = percent == null ? null : Number(percent)
                    const items = (data && data.subtasks && data.subtasks.items) || data.subTasks || []
                    this.subtasks = items.map(it => ({
                        name: it.name,
                        timeText: it.costTime || it.elapsed || it.timeText || ''
                    }))
                    if (this.element.status === 'RUNNING' && this.subtasks.length) {
                        this.progressExpanded = true
                    }
                } catch (_) {
                    this.progressPercent = null
                    this.subtasks = []
                }
            },
            async downloadLog () {
                const pluginData = {
                    projectId: this.$route.params.projectId,
                    pipelineId: this.$route.params.pipelineId,
                    buildId: this.buildId,
                    tag: this.id,
                    executeCount: this.currentExecute
                }
                try {
                    const logStatusRes = await this.getLogStatus(pluginData)
                    const logMode = (logStatusRes.data || {}).logMode || ''
                    if (logMode === 'LOCAL') {
                        this.$bkMessage({ theme: 'primary', message: this.$t('history.uploadLog'), limit: 1 })
                        return
                    }
                    const url = logMode === 'ARCHIVED'
                        ? await this.getDownloadLogFromArtifactory(pluginData)
                        : `log/api/user/logs/${pluginData.projectId}/${pluginData.pipelineId}/${pluginData.buildId}/download?tag=${this.id}&executeCount=${this.currentExecute}`
                    location.href = url
                } catch (err) {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }
            }
        }
    }
</script>

<style lang="scss" scoped>
.step-log-panel {
    display: flex;
    flex-direction: column;
    flex: 1 1 auto;
    min-height: 0;
    background: #2c2d34;
}
.lp-body {
    flex: 1;
    min-height: 0;
    display: flex;
}
</style>

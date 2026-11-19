<template>
    <section class="job-log-panel">
        <aside class="job-log-directory">
            <button
                v-for="item in pluginList"
                :key="item.id"
                type="button"
                :class="{ active: item.id === selectedId, 'is-hook': item.additionalOptions?.elementPostInfo }"
                :aria-current="item.id === selectedId ? 'true' : undefined"
                :aria-label="`${item.name} ${statusLabel(item.status)}`"
                :title="`${item.name} ${statusLabel(item.status)}`"
                @click="selectedId = item.id"
            >
                <span class="job-log-name"><log-status
                    :status="item.status"
                    :is-hook="item.additionalOptions?.elementPostInfo"
                    outline
                /><span class="job-log-title">{{ item.name }}</span></span><small v-if="elapsedById[item.id]">{{ elapsedById[item.id] }}</small>
            </button>
        </aside>
        <step-log-panel
            v-if="canLoadSelectedLog"
            ref="panel"
            v-bind="$attrs"
            :tag="logTarget.tag"
            :job-id="logTarget.jobId"
            :task-status="selectedItem.status"
            :download-tag="logTarget.tag"
            :download-job-id="logTarget.jobId"
            :download-link="selectedDownloadLink"
            :context-key="contextKey"
            @logs-loaded="handleLogsLoaded"
            v-on="forwardedListeners"
        />
        <div
            v-else
            class="job-log-empty"
            role="status"
        >
            {{ $t(selectedItem.roundSkipped ? 'logPanel.status.skippedInRound' : canLoadStepLogForSelection ? 'logPanel.emptyLog' : 'logPanel.status.notExecuted') }}
        </div>
    </section>
</template>

<script>
    import StepLogPanel from './StepLogPanel'
    import LogStatus from '../status'
    import { buildLogTarget, hasLogTarget as checkLogTarget, isLiveElapsedStatus, resolveConclusionElapsed, resolveDirectJobDownloadUrl, shouldLoadStepLog } from './logPanelAdapter'

    export default {
        name: 'JobLogPanel',
        components: { StepLogPanel, LogStatus },
        inheritAttrs: false,
        props: {
            pluginList: { type: Array, default: () => [] },
            rootJobId: { type: String, default: '' }
        },
        data () {
            return {
                selectedId: '',
                elapsedNow: Date.now(),
                elapsedTimer: null,
                pendingLocate: null,
                loadedContextKey: ''
            }
        },
        computed: {
            forwardedListeners () {
                return {
                    ...this.$listeners,
                    'issues-change': this.handleIssuesChange
                }
            },
            selectedItem () {
                return this.pluginList.find(item => item.id === this.selectedId) || this.pluginList[0] || {}
            },
            elapsedById () {
                return this.pluginList.reduce((map, item) => {
                    map[item.id] = this.elapsed(item)
                    return map
                }, {})
            },
            logTarget () {
                return buildLogTarget(this.selectedItem)
            },
            hasLogTarget () {
                return checkLogTarget(this.logTarget)
            },
            canLoadStepLogForSelection () {
                return !this.selectedItem.roundSkipped && shouldLoadStepLog({ status: this.selectedItem.status })
            },
            canLoadSelectedLog () {
                return this.canLoadStepLogForSelection && this.hasLogTarget
            },
            contextKey () {
                const { tag, jobId } = this.logTarget
                return `${tag || ''}|${jobId || ''}|${this.$attrs['execute-count'] || ''}|${this.$attrs['archive-flag'] || ''}`
            },
            selectedDownloadLink () {
                if (!this.hasLogTarget) return ''
                const link = this.$attrs['download-link'] || ''
                if (this.logTarget.tag) {
                    return link.replace(/([?&])jobId=[^&]+/, `$1tag=${encodeURIComponent(this.logTarget.tag)}`)
                }
                return link
            }
        },
        watch: {
            pluginList: {
                deep: true,
                handler () {
                    if (!this.pluginList.some(item => item.id === this.selectedId)) this.selectedId = this.pluginList[0]?.id || ''
                    this.syncIssuesAvailability()
                    this.syncElapsedTimer()
                }
            },
            rootJobId () {
                this.selectedId = this.pluginList[0]?.id || ''
                this.loadedContextKey = ''
                this.syncIssuesAvailability()
            },
            selectedId () {
                this.loadedContextKey = ''
                if (this.pendingLocate && this.pendingLocate.itemId !== this.selectedId) this.pendingLocate = null
                this.syncIssuesAvailability()
            },
            hasLogTarget () {
                this.syncIssuesAvailability()
            }
        },
        mounted () {
            this.selectedId = this.pluginList[0]?.id || ''
            this.syncIssuesAvailability()
            this.syncElapsedTimer()
        },
        beforeDestroy () {
            clearInterval(this.elapsedTimer)
        },
        methods: {
            syncIssuesAvailability () {
                this.$emit('issues-change', false)
            },
            handleIssuesChange (hasIssues) {
                if (!this.hasLogTarget) {
                    this.$emit('issues-change', false)
                    return
                }
                this.$emit('issues-change', hasIssues)
            },
            locateFirstIssue () {
                return this.$refs.panel?.locateFirstIssue?.(...arguments) ?? false
            },
            locateItem (itemId, messages) {
                if (!this.pluginList.some(item => item.id === itemId)) return false
                this.pendingLocate = { itemId, messages }
                if (this.selectedId !== itemId) {
                    this.selectedId = itemId
                } else if (this.loadedContextKey === this.contextKey) {
                    this.handleLogsLoaded()
                }
                return true
            },
            handleLogsLoaded () {
                this.loadedContextKey = this.contextKey
                this.$nextTick(() => this.consumePendingLocate())
            },
            consumePendingLocate (retry = false, contextKey = this.contextKey) {
                const target = this.pendingLocate
                if (!target || target.itemId !== this.selectedId || contextKey !== this.contextKey || contextKey !== this.loadedContextKey) return
                const panel = this.$refs.panel
                if (!panel) return
                if (panel.loadingInitial) {
                    if (!retry) this.$nextTick(() => this.consumePendingLocate(true, contextKey))
                    return
                }
                const located = Array.isArray(target.messages) && target.messages.length
                    ? this.locateFirstIssue(target.messages)
                    : this.locateFirstIssue()
                if (this.pendingLocate === target) this.pendingLocate = null
                return located
            },
            statusLabel (status) {
                const key = `details.statusMap.${status}`
                const translated = this.$t(key)
                return translated === key ? status : translated
            },
            elapsed (item) {
                return resolveConclusionElapsed(item, { now: this.elapsedNow })
            },
            syncElapsedTimer () {
                const hasLiveItem = this.pluginList.some(item => isLiveElapsedStatus(item.status))
                if (hasLiveItem && !this.elapsedTimer) {
                    this.elapsedNow = Date.now()
                    this.elapsedTimer = setInterval(() => {
                        this.elapsedNow = Date.now()
                    }, 1000)
                } else if (!hasLiveItem && this.elapsedTimer) {
                    clearInterval(this.elapsedTimer)
                    this.elapsedTimer = null
                }
            },
            download () {
                const url = resolveDirectJobDownloadUrl({
                    downloadLink: this.$attrs['download-link'],
                    executeCount: this.$attrs['execute-count']
                })
                if (!url) return ''
                location.href = url
                return url
            },
            moveMatch (step) {
                return this.$refs.panel?.moveMatch?.(step)
            }
        }
    }
</script>

<style lang="scss" scoped>
    .job-log-panel { display: flex; flex: 1; min-width: 0; min-height: 0; }
    .job-log-directory { display: flex; flex: 0 0 280px; flex-direction: column; overflow: auto; padding: 0; background: #232429; border-right: 1px solid #4d4f56; font: 12px/20px "Microsoft YaHei", sans-serif; }
    .job-log-directory button { box-sizing: border-box; display: flex; flex-shrink: 0; align-items: center; justify-content: space-between; width: 100%; height: 36px; padding: 0 12px; color: #c4c6cc; text-align: left; background: transparent; border: 0; cursor: pointer; font: 12px/20px "Microsoft YaHei", sans-serif; }
    .job-log-directory button:hover { color: #f0f1f5; background: #2e3342; }
    .job-log-directory button:focus-visible { outline: 2px solid #3c96ff; outline-offset: -2px; }
    .job-log-name { display: inline-flex; align-items: center; min-width: 0; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
    .job-log-name > * + * { margin-left: 8px; }
    .job-log-title { min-width: 0; overflow: hidden; text-overflow: ellipsis; font: 12px/20px "Microsoft YaHei", sans-serif; }
    .job-log-name .log-status { flex: 0 0 16px; width: 16px; height: 16px; padding: 0; line-height: 16px; }
    .job-log-directory button.active { color: #fff; background: #2c2d34; } .job-log-directory button.is-hook { margin-top: 8px; border-top: 1px solid #4d4f56; } .job-log-directory small { flex-shrink: 0; margin-left: 8px; color: #83828c; font: 12px/20px "Microsoft YaHei", sans-serif; font-variant-numeric: tabular-nums; }
    .job-log-empty { display: flex; flex: 1; align-items: center; justify-content: center; color: #83828c; background: #2c2d34; font: 12px/20px "Microsoft YaHei", sans-serif; }
    @media (max-width: 900px) { .job-log-directory { flex-basis: 180px; } }
</style>

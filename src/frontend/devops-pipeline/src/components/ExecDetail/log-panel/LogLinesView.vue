<template>
    <div
        class="lp-lines-wrap"
        :class="{ 'is-empty': !renderLines.length, 'has-minimap': showMinimap }"
        :style="{ '--lp-line-number-width': lineNumberColumnWidth + 'ch' }"
    >
        <div
            ref="box"
            class="lp-lines"
            :class="{ 'is-wrap': wrap, 'is-empty': !renderLines.length }"
            @scroll="onScroll"
            @wheel="onWheel"
            @touchstart="onNativeInteraction"
            @touchmove="onUserScrollInput('touch')"
            @touchend="onUserScrollEnd"
            @pointerdown="onNativeInteraction"
            @pointerup="onUserScrollEnd"
            @keydown="onNativeInteraction"
            @keyup="onUserScrollEnd"
        >
            <button
                v-if="loadingBefore"
                type="button"
                class="lp-indicator"
                ref="loadingIndicator"
                disabled
            >
                {{ $t('loading') }}
            </button>
            <div
                v-if="useVirtual"
                class="lp-virtual-pad"
                :style="{ height: padTop + 'px' }"
            ></div>
            <div
                v-for="row in visibleRows"
                :id="'real-log-' + row.line.lineNo"
                :key="row.key"
                class="lp-line"
                :class="[
                    logLineClass(row.line),
                    {
                        'is-highlighted': String(row.line.lineNo ?? '') === String(currentLocateLineNo ?? ''),
                        'is-locate': String(row.line.lineNo ?? '') === String(currentLocateLineNo ?? ''),
                        'is-group-header': Boolean(row.line._logGroup)
                    }
                ]"
                :data-line-no="row.line.lineNo"
                :data-line-key="row.key"
                @click="onLineClick(row.line, $event)"
            >
                <span
                    class="lp-lineno"
                >{{ row.line.displayLineNo }}</span>
                <span
                    v-if="showTime"
                    class="lp-time"
                >{{ row.line.time }}</span>
                <button
                    v-if="row.line._logGroup"
                    type="button"
                    class="lp-group-toggle"
                    :aria-label="$t(row.line._logGroup.collapsed ? 'logPanel.structure.expand' : 'logPanel.structure.collapse')"
                    :aria-expanded="String(!row.line._logGroup.collapsed)"
                    :title="$t(row.line._logGroup.collapsed ? 'logPanel.structure.expand' : 'logPanel.structure.collapse')"
                    @click.stop="$emit('toggle-group', row.line._logGroup.id)"
                >
                    <i
                        class="devops-icon"
                        :class="row.line._logGroup.collapsed ? 'icon-angle-right' : 'icon-angle-down'"
                        aria-hidden="true"
                    ></i>
                </button>
                <span
                    v-if="groupIssueCount(row.line, 'error')"
                    class="lp-group-issue-count is-error"
                    :title="$t('logPanel.structure.hiddenIssues', [groupIssueCount(row.line, 'error'), groupIssueCount(row.line, 'warn')])"
                    :aria-label="$t('logPanel.structure.hiddenIssues', [groupIssueCount(row.line, 'error'), groupIssueCount(row.line, 'warn')])"
                    role="img"
                >
                    <i class="devops-icon icon-close-circle-fill"></i>{{ groupIssueCount(row.line, 'error') }}
                </span>
                <span
                    v-if="groupIssueCount(row.line, 'warn')"
                    class="lp-group-issue-count is-warn"
                    :title="$t('logPanel.structure.hiddenIssues', [groupIssueCount(row.line, 'error'), groupIssueCount(row.line, 'warn')])"
                    :aria-label="$t('logPanel.structure.hiddenIssues', [groupIssueCount(row.line, 'error'), groupIssueCount(row.line, 'warn')])"
                    role="img"
                >
                    <i class="devops-icon icon-exclamation-circle-shape"></i>{{ groupIssueCount(row.line, 'warn') }}
                </span>
                <span
                    class="lp-text"
                    v-html="renderLineHtml(row.line)"
                ></span>
                <slot
                    name="line-actions"
                    :line="row.line"
                />
            </div>
            <div
                v-if="useVirtual"
                class="lp-virtual-pad"
                :style="{ height: padBottom + 'px' }"
            ></div>
        </div>
        <log-error-minimap
            v-if="showMinimap"
            :logs="renderLines"
            :issue-logs="issueLogs"
            :display-base="displayBase"
            :viewport="viewport"
            :height-index="heightIndex"
            :index-version="indexVersion"
            :line-origin="minimapLineOrigin"
            :content-height="contentHeight"
            :scroll-metrics="minimapScrollMetrics"
            @locate="$emit('locate', $event)"
            @locate-target="scrollToSourceTarget"
            @pan="onPan"
        />
    </div>
</template>

<script>
    import { markRaw, shallowRef } from 'vue'
    import { getLogIssueLevel, getLogText, renderLogHtml, resolveLogLineNumberColumnWidth, shouldShowLogMinimap, withDisplayLineNumbers } from './logPanelAdapter'
    import { parseLargeVariableLogLine } from './logLargeVariable'

    function escapeText (value) {
        return String(value ?? '').replace(/[&<>"']/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[char]))
    }
    import LogErrorMinimap from './LogErrorMinimap'
    import {
        captureAnchorFromDom,
        captureAnchorFromIndex,
        createHeightIndex,
        DEFAULT_LINE_HEIGHT,
        estimateExplicitLineHeight,
        isCurrentLayoutUpdate,
        isCurrentWheelTransaction,
        parseScrollBoxPaddingTop,
        preserveHeightMapForLogs,
        resolveLogWheelDeltas,
        resolveLogWheelTarget,
        resolveFollowIntent,
        resolveLineKey,
        resolveLineIndexByKey,
        resolveLineCenterAdjustment,
        resolveScrollToBottomState,
        resolveVirtualWindowFromIndex,
        shouldInterceptLogWheel,
        shouldYieldLogWheelToNative,
        scrollTopForAnchor,
        scrollTopForLineKey,
        shouldAcceptMeasuredHeight,
        isLogScrollKey,
        shouldUseVirtualScroll,
        USER_SCROLL_IDLE_MS
    } from './logVirtualWindow'

    export default {
        name: 'LogLinesView',
        components: { LogErrorMinimap },
        props: {
            logs: { type: Array, default: () => [] },
            issueLogs: { type: Array, default: null },
            displayBase: { type: Number, default: 1 },
            keyword: { type: String, default: '' },
            showTime: { type: Boolean, default: false },
            wrap: { type: Boolean, default: true },
            highlightedLineNo: { type: [Number, String], default: '' },
            loadingBefore: { type: Boolean, default: false },
            followBottom: { type: Boolean, default: true }
        },
        data () {
            return {
                viewport: { top: 0, height: 100 },
                scrollTop: 0,
                clientHeight: 0,
                contentHeight: 0,
                containerWidth: 0,
                heightMap: {},
                heightIndex: markRaw(createHeightIndex()),
                indexVersion: 0,
                scrollGeneration: 0,
                followIntent: this.followBottom,
                layoutToken: 0,
                layoutInteractionToken: 0,
                userScrollPending: false,
                scrollDelta: 0,
                measurePending: false,
                pendingAnchor: null,
                locateGeneration: 0,
                activeLocate: null,
                pendingLocateScrollTop: null,
                sourceLocateLineNo: '',
                renderLines: shallowRef([])
            }
        },
        computed: {
            currentLocateLineNo () {
                return this.sourceLocateLineNo || this.highlightedLineNo
            },
            displayLines () {
                return withDisplayLineNumbers(this.logs, this.displayBase)
            },
            lineNumberColumnWidth () {
                return resolveLogLineNumberColumnWidth(this.renderLines)
            },
            showMinimap () {
                return shouldShowLogMinimap({
                    lineCount: this.renderLines.length
                })
            },
            useVirtual () {
                return shouldUseVirtualScroll(this.renderLines.length)
            },
            virtualWindow () {
                const version = this.indexVersion
                if (!this.useVirtual) {
                    return { start: 0, end: this.renderLines.length, padTop: 0, padBottom: 0, total: 0, version }
                }
                return {
                    version,
                    ...resolveVirtualWindowFromIndex(this.heightIndex, {
                        scrollTop: this.scrollTop,
                        clientHeight: this.clientHeight || 400,
                        scrollDelta: this.scrollDelta,
                        dynamicOverscan: true
                    })
                }
            },
            minimapScrollMetrics () {
                return {
                    scrollTop: this.scrollTop,
                    clientHeight: this.clientHeight,
                    contentHeight: this.contentHeight,
                    scrollHeight: this.contentHeight,
                    lineOrigin: this.lineOrigin(),
                    indexVersion: this.indexVersion
                }
            },
            minimapLineOrigin () {
                return this.lineOrigin()
            },
            windowStart () {
                return this.virtualWindow.start
            },
            windowEnd () {
                return this.virtualWindow.end
            },
            visibleLines () {
                if (!this.useVirtual) return this.renderLines
                return this.renderLines.slice(this.windowStart, this.windowEnd)
            },
            visibleRows () {
                const base = Number(this.displayBase)
                const displayBase = Number.isFinite(base) ? base : 1
                const start = this.useVirtual ? this.windowStart : 0
                return this.visibleLines.map((line, offset) => {
                    const index = Number(line.displayLineNo) - displayBase
                    return { line, key: resolveLineKey(line, Number.isFinite(index) ? index : start + offset) }
                })
            },
            padTop () {
                return this.useVirtual ? this.virtualWindow.padTop : 0
            },
            padBottom () {
                return this.useVirtual ? this.virtualWindow.padBottom : 0
            }
        },
        watch: {
            followBottom (value) {
                this.followIntent = Boolean(value)
            },
            logs: {
                handler () {
                    this.cancelPendingWheel()
                    this.cancelLocateCalibration()
                    const layout = this.beginLayoutUpdate()
                    this.pendingAnchor = layout.anchor
                    const anchorLineNo = Number(this.pendingAnchor?.lineNo)
                    if (Number.isFinite(anchorLineNo) && !this.logs.some(line => String(line.lineNo ?? line.line) === String(anchorLineNo))) {
                        const groupIndex = this.logs.findIndex(line => line._logGroup
                            && anchorLineNo >= Number(line._logGroup.startLineNo)
                            && anchorLineNo <= Number(line._logGroup.endLineNo))
                        if (groupIndex >= 0) {
                            const groupLine = this.logs[groupIndex]
                            this.pendingAnchor = {
                                lineKey: resolveLineKey(groupLine, groupIndex),
                                lineNo: String(groupLine.lineNo ?? groupLine.line),
                                viewportTop: 0
                            }
                            layout.anchor = this.pendingAnchor
                            if (layout.restore) layout.restore.anchor = this.pendingAnchor
                        }
                    }
                    this.heightMap = preserveHeightMapForLogs(this.heightMap, this.displayLines)
                    this.rebuildHeightIndex()
                    this.$nextTick(() => {
                        this.finishLayoutUpdate(layout, () => this.$emit('locate-ready'))
                    })
                }
            },
            wrap (val) {
                this.cancelPendingWheel()
                this.cancelLocateCalibration()
                const layout = this.beginLayoutUpdate()
                this.pendingAnchor = layout.anchor
                this.heightMap = {}
                this.rebuildHeightIndex()
                const generation = this.locateGeneration
                this.$nextTick(() => {
                    this.finishLayoutUpdate(layout, () => this.$nextTick(() => this.revealSearchHitInViewport(this.highlightedLineNo, generation)))
                })
            },
            showTime () {
                this.cancelPendingWheel()
                this.cancelLocateCalibration()
                const layout = this.beginLayoutUpdate()
                this.pendingAnchor = layout.anchor
                this.heightMap = {}
                this.rebuildHeightIndex()
                const generation = this.locateGeneration
                this.$nextTick(() => {
                    this.finishLayoutUpdate(layout, () => this.$nextTick(() => this.revealSearchHitInViewport(this.highlightedLineNo, generation)))
                })
            },
            loadingBefore () {
                this.$nextTick(() => {
                    this.indexVersion += 1
                    this.updateViewport()
                })
            },
        },
        created () {
            this.$watch('displayLines', next => {
                this.renderLines = next
            }, { immediate: true })
        },
        mounted () {
            this.$nextTick(() => {
                const el = this.$refs.box
                if (el) {
                    this.scrollTop = el.scrollTop
                    this.clientHeight = el.clientHeight
                    this.containerWidth = el.clientWidth
                }
                this.rebuildHeightIndex()
                this.measureVisibleRows()
                this.updateViewport()
                this.bindObserver()
                this.$emit('locate-ready')
            })
        },
        updated () {
            if (!this.measurePending) {
                this.measurePending = true
                this.$nextTick(() => {
                    this.measurePending = false
                    this.measureVisibleRows()
                })
            }
        },
        beforeDestroy () {
            this.layoutToken += 1
            this.layoutDestroyed = true
            clearTimeout(this.userScrollTimer)
            this.userScrollTimer = null
            this.cancelPendingWheel()
            this.unbindObserver()
            if (this._resizeFrame) cancelAnimationFrame(this._resizeFrame)
            this._resizeFrame = null
        },
        methods: {
            groupIssueCount (line, level) {
                const count = line?._logGroup?.issueCounts?.[level]
                return Number.isFinite(Number(count)) ? Number(count) : 0
            },
            onLineClick (line, event) {
                if (event?.target?.closest?.('.lp-ovf-view')) {
                    const ref = parseLargeVariableLogLine(getLogText(line))
                    if (ref?.queryable) {
                        this.$emit('view-large-variable', { key: ref.key, length: ref.length, tag: line?.tag, lineNo: line?.lineNo })
                    }
                    return
                }
                const groupId = line?._logGroup?.id
                if (groupId === undefined || groupId === null) return
                if (event?.target?.closest?.('a')) return
                const selection = typeof window !== 'undefined' ? window.getSelection?.() : null
                if (selection && !selection.isCollapsed && selection.toString()) return
                this.$emit('toggle-group', groupId)
            },
            logLineClass (line) {
                return 'is-' + (getLogIssueLevel(line) || String(line.level || 'INFO').toLowerCase())
            },
            lineKey (line, index) {
                return resolveLineKey(line, index)
            },
            renderLineHtml (line) {
                const ref = parseLargeVariableLogLine(getLogText(line))
                if (!ref) return renderLogHtml(line, this.keyword)
                const prefix = ref.queryable ? `output(${ref.kind}): ` : `input(${ref.kind}): ${ref.label}`
                const parts = [renderLogHtml(`${prefix}${ref.key}`, this.keyword)]
                if (ref.queryable) {
                    parts.push(`<button type="button" class="lp-ovf-view">${escapeText(this.$t('logPanel.largeVariable.view'))}</button>`)
                }
                parts.push(`<span class="lp-ovf-size">${escapeText(this.$t('logPanel.largeVariable.approxChars', [ref.length]))}</span>`)
                return parts.join(' ')
            },
            rebuildHeightIndex () {
                const heights = { ...this.heightMap }
                if (!this.wrap) {
                    this.displayLines.forEach((line, index) => {
                        const key = resolveLineKey(line, index)
                        if (!Number.isFinite(heights[key]) || heights[key] <= 0) {
                            heights[key] = estimateExplicitLineHeight(getLogText(line))
                        }
                    })
                }
                this.heightIndex.rebuild(this.displayLines, heights)
                this.indexVersion += 1
            },
            captureAnchorFromPreviousIndex () {
                if (this._stableAnchor) return this._stableAnchor
                const index = this.heightIndex
                if (!index?.size) return this.captureAnchor()
                const lineOrigin = this.lineOrigin()
                const lineIndex = index.findIndexByOffset(Math.max(0, this.scrollTop - lineOrigin))
                const line = this.displayLines[lineIndex]
                if (!line) return null
                return captureAnchorFromIndex(index, {
                    scrollTop: this.scrollTop,
                    paddingTop: lineOrigin,
                    lineIndex,
                    lineKey: resolveLineKey(line, lineIndex),
                    lineNo: line.lineNo ?? line.line
                })
            },
            beginLayoutUpdate ({ usePreviousIndex = false, anchor: anchorOverride = null } = {}) {
                const token = this.layoutToken = (this.layoutToken || 0) + 1
                const interactionToken = this.layoutInteractionToken || 0
                const previousRestore = this._layoutRestore
                const canInheritRestore = previousRestore && previousRestore.interactionToken === interactionToken
                const anchor = anchorOverride || (this.followIntent && !this.userScrollPending
                    ? null
                    : canInheritRestore
                        ? previousRestore.anchor
                        : usePreviousIndex ? this.captureAnchorFromPreviousIndex() : this.captureAnchor())
                const restore = anchor ? (canInheritRestore ? previousRestore : {
                    anchor,
                    interactionToken,
                    callbacks: []
                }) : null
                if (restore) {
                    restore.anchor = anchor
                    restore.token = token
                    restore.interactionToken = interactionToken
                    restore.running = false
                    restore.runId = (restore.runId || 0) + 1
                    this._layoutRestore = restore
                } else {
                    this._layoutRestore = null
                }
                this.pendingAnchor = anchor
                return { token, interactionToken, follow: this.followIntent, anchor, restore }
            },
            isCurrentLayout (layout) {
                return isCurrentLayoutUpdate({
                    token: layout?.token,
                    currentToken: this.layoutToken,
                    interactionToken: layout?.interactionToken,
                    currentInteractionToken: this.layoutInteractionToken,
                    destroyed: this.layoutDestroyed
                })
            },
            finishLayoutUpdate (layout, callback, attempt = 0) {
                if (callback && layout?.restore && !layout.restore.callbacks.includes(callback)) layout.restore.callbacks.push(callback)
                if (!this.isCurrentLayout(layout)) return
                if (this.userScrollPending) {
                    this.pendingLayoutUpdate = { layout, callback }
                    return
                }
                if (layout.restore) {
                    if (!layout.restore.running) {
                        layout.restore.running = true
                        layout.restore.runId = (layout.restore.runId || 0) + 1
                        this.restoreLayoutAnchor(layout, attempt, layout.restore.runId)
                    }
                    return
                }
                if (layout.follow && this.followIntent) {
                    this.finishFollowLayoutUpdate(layout, callback, attempt)
                    return
                }
                const measured = this.measureVisibleRows()
                this.$nextTick(() => {
                    if (!this.isCurrentLayout(layout) || this.userScrollPending) return
                    if (measured && attempt < 2) {
                        this.finishLayoutUpdate(layout, callback, attempt + 1)
                        return
                    }
                    this.pendingAnchor = layout.anchor
                    if (this.pendingAnchor) this.consumePendingAnchor()
                    this.updateViewport()
                    if (callback) callback()
                })
            },
            finishFollowLayoutUpdate (layout, callback, attempt = 0) {
                if (!this.isCurrentLayout(layout) || this.userScrollPending || !this.followIntent) return
                const el = this.$refs.box
                if (!el) return
                const correction = this._tailCorrection = {
                    token: layout.token,
                    interactionToken: layout.interactionToken
                }
                this.scrollToBottom()
                this.measureVisibleRows()
                this.$nextTick(() => {
                    if (this._tailCorrection !== correction || !this.isCurrentLayout(layout) || this.userScrollPending || !this.followIntent) return
                    this.scrollToBottom()
                    const measured = this.measureVisibleRows()
                    this.$nextTick(() => {
                        if (this._tailCorrection !== correction || !this.isCurrentLayout(layout) || this.userScrollPending || !this.followIntent) return
                        const gap = el.scrollHeight - el.scrollTop - el.clientHeight
                        if ((measured || gap > 1) && attempt < 5) {
                            this.finishFollowLayoutUpdate(layout, callback, attempt + 1)
                            return
                        }
                        this.updateViewport()
                        if (gap <= 1 && !measured) {
                            this._tailCorrection = null
                            if (callback) callback()
                        } else if (attempt >= 5) {
                            // Keep follow intent; a later append or layout change can retry.
                            this._tailCorrection = null
                        }
                    })
                })
            },
            restoreLayoutAnchor (layout, attempt = 0, runId) {
                if (!this.isCurrentLayout(layout) || this.userScrollPending) return
                const restore = layout.restore
                if (!restore || restore.runId !== runId) return
                const anchor = restore?.anchor
                const el = this.$refs.box
                if (!el || !anchor) return
                let targetIndex = this.heightIndex.indexOfKey(anchor.lineKey)
                if (targetIndex < 0) {
                    const lineIndex = this.displayLines.findIndex(line => String(line.lineNo ?? line.line ?? '') === String(anchor.lineNo ?? ''))
                    if (lineIndex < 0) {
                        this.completeLayoutRestore(layout, false)
                        return
                    }
                    targetIndex = lineIndex
                    anchor.lineKey = resolveLineKey(this.displayLines[lineIndex], lineIndex)
                }
                const row = this.findLineRow(anchor.lineKey ?? anchor.lineNo)
                if (!row && this.useVirtual) {
                    const index = targetIndex >= 0 ? targetIndex : this.displayLines.findIndex(line => String(line.lineNo ?? line.line ?? '') === String(anchor.lineNo ?? ''))
                    if (index < 0) {
                        this.completeLayoutRestore(layout, false)
                        return
                    }
                    this.positionLayoutTarget(index)
                    if (attempt < 8) this.$nextTick(() => this.restoreLayoutAnchor(layout, attempt + 1, runId))
                    else restore.running = false
                    return
                }
                if (!row) {
                    if (attempt < 8) this.$nextTick(() => this.restoreLayoutAnchor(layout, attempt + 1, runId))
                    else restore.running = false
                    return
                }
                this.measureVisibleRows()
                this.$nextTick(() => {
                    if (!this.isCurrentLayout(layout) || this.userScrollPending || restore.runId !== runId) return
                    const measuredRow = this.findLineRow(anchor.lineKey ?? anchor.lineNo)
                    if (!measuredRow) {
                        const index = this.heightIndex.indexOfKey(anchor.lineKey)
                        if (index >= 0) this.positionLayoutTarget(index)
                        if (attempt < 8) this.$nextTick(() => this.restoreLayoutAnchor(layout, attempt + 1, runId))
                        else restore.running = false
                        return
                    }
                    const boxRect = el.getBoundingClientRect()
                    const rowRect = measuredRow.getBoundingClientRect()
                    const rowHeight = Math.max(1, rowRect.height || measuredRow.offsetHeight || DEFAULT_LINE_HEIGHT)
                    const desiredTop = Math.max(-(rowHeight - 1), Math.min(el.clientHeight - 1, Number(anchor.viewportTop) || 0))
                    const actualTop = rowRect.top - boxRect.top
                    const delta = actualTop - desiredTop
                    if (Math.abs(delta) > 1 && attempt < 8) {
                        el.scrollTop = Math.max(0, Math.min(el.scrollHeight - el.clientHeight, el.scrollTop + delta))
                        this.scrollTop = el.scrollTop
                        this.clientHeight = el.clientHeight
                        this.updateViewport()
                        this.$nextTick(() => this.restoreLayoutAnchor(layout, attempt + 1, runId))
                        return
                    }
                    if (Math.abs(delta) <= 1) this.completeLayoutRestore(layout, true)
                    else restore.running = false
                })
            },
            positionLayoutTarget (index) {
                const el = this.$refs.box
                if (!el || index < 0) return
                const sourceTop = this.lineOrigin() + this.heightIndex.prefix(index)
                const nextTop = Math.max(0, sourceTop - Math.floor(el.clientHeight / 2))
                el.scrollTop = nextTop
                this.scrollTop = nextTop
                this.clientHeight = el.clientHeight
                this.updateViewport()
            },
            completeLayoutRestore (layout, verified) {
                if (!this.isCurrentLayout(layout)) return
                const restore = layout.restore
                this._stableAnchor = verified ? restore.anchor : null
                restore.running = false
                this._layoutRestore = null
                this.pendingAnchor = null
                this.updateViewport()
                const callbacks = restore.callbacks.slice()
                restore.callbacks.length = 0
                callbacks.forEach(callback => callback())
            },
            beginUserInteraction () {
                clearTimeout(this.userScrollTimer)
                this.userScrollTimer = null
                this.userScrollPending = false
                this.pendingLayoutUpdate = null
                this.layoutInteractionToken = (this.layoutInteractionToken || 0) + 1
                this.layoutToken = (this.layoutToken || 0) + 1
                this.pendingAnchor = null
                this._layoutRestore = null
                this._stableAnchor = null
                this._tailCorrection = null
            },
            isScrollbarPointer (event) {
                const el = this.$refs.box
                if (!el || !event || event.target !== el) return false
                const rect = el.getBoundingClientRect()
                return event.clientX >= rect.right - Math.max(12, el.offsetWidth - el.clientWidth)
            },
            onUserScrollInput (source, event) {
                this.beginUserInteraction()
                this.userScrollPending = true
                this.userScrollSource = source
                if (event && (source === 'wheel' || source === 'touch') && Number(event.deltaX || 0) !== 0 && Number(event.deltaY || 0) === 0) {
                    this.scheduleUserScrollEnd()
                    return
                }
                this.scheduleUserScrollEnd()
            },
            onUserScrollEnd () {
                if (this.userScrollPending) this.scheduleUserScrollEnd()
            },
            scheduleUserScrollEnd () {
                clearTimeout(this.userScrollTimer)
                const interactionToken = this.layoutInteractionToken || 0
                this.userScrollTimer = setTimeout(() => {
                    if (interactionToken !== (this.layoutInteractionToken || 0)) return
                    this.userScrollTimer = null
                    this.userScrollPending = false
                    this.updateFollowIntentFromScroll()
                    const pending = this.pendingLayoutUpdate
                    this.pendingLayoutUpdate = null
                    if (pending) {
                        pending.layout.follow = this.followIntent
                        this.finishLayoutUpdate(pending.layout, pending.callback)
                    }
                }, USER_SCROLL_IDLE_MS)
            },
            updateFollowIntentFromScroll () {
                const el = this.$refs.box
                if (!el) return
                const atBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 8
                this.followIntent = resolveFollowIntent({
                    currentFollow: this.followIntent,
                    atBottom,
                    userScrollPending: true
                })
                this._stableAnchor = this.followIntent ? null : this.captureAnchor()
                const pendingLayout = this.pendingLayoutUpdate?.layout
                if (pendingLayout) {
                    pendingLayout.follow = this.followIntent
                    if (this.followIntent) {
                        pendingLayout.anchor = null
                        pendingLayout.restore = null
                        this.pendingAnchor = null
                        this._layoutRestore = null
                        this._stableAnchor = null
                    } else if (this._stableAnchor && pendingLayout.restore) {
                        pendingLayout.anchor = this._stableAnchor
                        pendingLayout.restore.anchor = this._stableAnchor
                        pendingLayout.restore.running = false
                        pendingLayout.restore.runId = (pendingLayout.restore.runId || 0) + 1
                        this.pendingAnchor = this._stableAnchor
                    }
                }
                this.$emit('stick-bottom-change', this.followIntent)
            },
            measureVisibleRows () {
                const el = this.$refs.box
                if (!el) return false
                const rows = el.querySelectorAll('.lp-line[data-line-key]')
                if (!rows.length) return false
                const scrollGeneration = this.scrollGeneration
                const scrollTop = el.scrollTop
                let changed = false
                const anchor = !this._layoutRestore && !this._tailCorrection && !this._pendingWheel && !this.pendingAnchor && !this.activeLocate
                    ? this.captureAnchor()
                    : null
                rows.forEach(row => {
                    const lineKey = row.dataset.lineKey
                    if (!lineKey) return
                    const measured = row.offsetHeight
                    const cached = this.heightMap[lineKey]
                    if (shouldAcceptMeasuredHeight(cached, measured) && this.heightIndex.setHeight(lineKey, measured)) {
                        this.heightMap[lineKey] = measured
                        changed = true
                    }
                })
                if (changed) {
                    this.indexVersion += 1
                    this.$nextTick(() => {
                        if (anchor && !this._tailCorrection && !this._pendingWheel && !this.pendingAnchor && !this.activeLocate
                            && scrollGeneration === this.scrollGeneration
                            && Math.abs(el.scrollTop - scrollTop) < 1) this.applyAnchor(anchor)
                        else this.updateViewport()
                    })
                }
                return changed
            },
            onScroll () {
                const el = this.$refs.box
                if (!el) return
                if (this._pendingPanGeneration) this.cancelPendingPan()
                this.scrollGeneration += 1
                if (this._pendingWheel) {
                    this.clientHeight = el.clientHeight
                    this.updateViewport()
                    if (this._pendingWheel.top < 40) this.$emit('reach-top')
                    if (this.userScrollPending) {
                        this.updateFollowIntentFromScroll()
                        this.scheduleUserScrollEnd()
                    }
                    return
                }
                if (this.activeLocate) {
                    const expected = this.pendingLocateScrollTop
                    if (expected !== null && Math.abs(el.scrollTop - expected) < 1) this.pendingLocateScrollTop = null
                    else this.cancelLocateCalibration()
                }
                this.scrollDelta = el.scrollTop - this.scrollTop
                this.scrollTop = el.scrollTop
                this.clientHeight = el.clientHeight
                if (el.scrollTop < 40) this.$emit('reach-top')
                if (this.userScrollPending) {
                    this.updateFollowIntentFromScroll()
                    this.scheduleUserScrollEnd()
                }
                this.updateViewport()
            },
            onNativeInteraction (event) {
                this.cancelPendingWheel()
                this.cancelLocateCalibration()
                const isKeyboardEvent = event?.type === 'keydown'
                const isEditableTarget = Boolean(event?.target?.closest?.('input, textarea, select, [contenteditable]:not([contenteditable="false"]), [role="textbox"]'))
                if (isKeyboardEvent && event.key === 'End' && !isEditableTarget) {
                    event.preventDefault?.()
                    this.onPan(1)
                } else if (isKeyboardEvent && !isEditableTarget && isLogScrollKey(event.key)) this.onUserScrollInput('keyboard')
                else if (event?.type === 'touchstart') this.onUserScrollInput('touch')
                else if (event?.type === 'pointerdown' && this.isScrollbarPointer(event)) this.onUserScrollInput('scrollbar')
            },
            onWheel (event) {
                this.cancelLocateCalibration()
                this.cancelPendingPan()
                const el = this.$refs.box
                if (!el) return
                const accepted = shouldInterceptLogWheel({
                    virtualized: this.useVirtual,
                    cancelable: event.cancelable,
                    ctrlKey: event.ctrlKey,
                    metaKey: event.metaKey,
                    shiftKey: event.shiftKey,
                    deltaX: event.deltaX,
                    deltaY: event.deltaY
                })
                if (!accepted) {
                    if (!event.shiftKey && event.deltaY && Math.abs(event.deltaX) <= Math.abs(event.deltaY)) this.onUserScrollInput('wheel', event)
                    this.cancelPendingWheel()
                    return
                }
                this.onUserScrollInput('wheel', event)
                const delta = resolveLogWheelDeltas({
                    deltaX: event.deltaX,
                    deltaY: event.deltaY,
                    deltaMode: event.deltaMode,
                    lineHeight: DEFAULT_LINE_HEIGHT,
                    pageHeight: el.clientHeight
                })
                const pending = this._pendingWheel
                const fromTop = pending ? pending.top : el.scrollTop
                const fromLeft = pending ? pending.left : el.scrollLeft
                const target = resolveLogWheelTarget({
                    scrollTop: fromTop,
                    scrollLeft: fromLeft,
                    deltaTop: delta.deltaTop,
                    deltaLeft: delta.deltaLeft,
                    scrollHeight: el.scrollHeight,
                    clientHeight: el.clientHeight,
                    scrollWidth: el.scrollWidth,
                    clientWidth: el.clientWidth
                })
                if (shouldYieldLogWheelToNative({ hasPending: Boolean(pending), targetTop: target.top, scrollTop: el.scrollTop })) {
                    this.cancelPendingWheel()
                    return
                }
                event.preventDefault()
                const transaction = pending || { top: el.scrollTop, left: el.scrollLeft, scheduled: false }
                transaction.top = target.top
                transaction.left = target.left
                this._pendingWheel = transaction
                this.scrollDelta = transaction.top - fromTop
                this.scrollTop = transaction.top
                this.clientHeight = el.clientHeight
                if (!transaction.scheduled) {
                    transaction.scheduled = true
                    this.$nextTick(() => {
                        if (!isCurrentWheelTransaction(this._pendingWheel, transaction)) return
                        const box = this.$refs.box
                        if (!box) {
                            this._pendingWheel = null
                            return
                        }
                        box.scrollTop = transaction.top
                        box.scrollLeft = transaction.left
                        this._pendingWheel = null
                        this.scrollTop = box.scrollTop
                        this.scrollDelta = 0
                        this.clientHeight = box.clientHeight
                        this.updateViewport()
                        if (box.scrollTop < 40) this.$emit('reach-top')
                        this.updateFollowIntentFromScroll()
                        this.scheduleUserScrollEnd()
                    })
                }
            },
            cancelPendingWheel () {
                this.cancelPendingPan()
                if (!this._pendingWheel) return
                this._pendingWheel = null
                const el = this.$refs.box
                if (!el) return
                this.scrollDelta = el.scrollTop - this.scrollTop
                this.scrollTop = el.scrollTop
                this.clientHeight = el.clientHeight
                this.updateViewport()
            },
            cancelPendingPan () {
                if (!this._pendingPanGeneration) return
                this._panGeneration = (this._panGeneration || 0) + 1
                this._pendingPanGeneration = 0
            },
            visibleLineRange () {
                const total = this.displayLines.length
                if (!total) return { first: 0, last: 0 }
                if (this.useVirtual) {
                    const first = this.heightIndex.findIndexByOffset(this.scrollTop)
                    const last = this.heightIndex.findIndexByOffset(this.scrollTop + this.clientHeight)
                    return { first, last: Math.max(first, last) }
                }
                const el = this.$refs.box
                if (!el) return { first: 0, last: 0 }
                const rows = el.querySelectorAll('.lp-line')
                if (!rows.length) return { first: 0, last: 0 }
                const top = el.getBoundingClientRect().top
                const bottom = top + el.clientHeight
                let first = -1
                let last = -1
                for (let i = 0; i < rows.length; i++) {
                    const rect = rows[i].getBoundingClientRect()
                    if (rect.bottom > top && rect.top < bottom) {
                        if (first < 0) first = i
                        last = i
                    } else if (first >= 0 && rect.top >= bottom) {
                        break
                    }
                }
                if (first < 0) return { first: 0, last: 0 }
                return { first, last }
            },
            updateViewport () {
                const el = this.$refs.box
                if (!el) return
                const totalHeight = el.scrollHeight
                this.contentHeight = totalHeight
                this.scrollTop = this._pendingWheel ? this._pendingWheel.top : el.scrollTop
                this.clientHeight = el.clientHeight
                if (!this.displayLines.length || totalHeight <= el.clientHeight + 1) {
                    this.viewport = { top: 0, height: 100 }
                    return
                }
                const height = Math.min(100, (el.clientHeight / totalHeight) * 100)
                const maxTop = Math.max(100 - height, 0)
                this.viewport = {
                    top: Math.max(0, Math.min(maxTop, (this.scrollTop / totalHeight) * 100)),
                    height
                }
            },
            onPan (ratio) {
                this.beginUserInteraction()
                this.cancelPendingWheel()
                this.cancelLocateCalibration()
                const el = this.$refs.box
                if (!el) return
                const normalizedRatio = Math.max(0, Math.min(1, Number(ratio) || 0))
                const follow = normalizedRatio === 1
                this.followIntent = follow
                this.$emit('stick-bottom-change', follow)
                const max = Math.max(el.scrollHeight - el.clientHeight, 0)
                const nextScrollTop = follow ? max : normalizedRatio * max
                this.scrollDelta = nextScrollTop - this.scrollTop
                this.scrollTop = nextScrollTop
                this.clientHeight = el.clientHeight
                const generation = this._panGeneration = (this._panGeneration || 0) + 1
                this._pendingPanGeneration = generation
                this.$nextTick(() => {
                    if (generation !== this._panGeneration || this.$refs.box !== el) return
                    this._pendingPanGeneration = 0
                    el.scrollTop = follow
                        ? Math.max(0, el.scrollHeight - el.clientHeight)
                        : nextScrollTop
                    this.scrollTop = el.scrollTop
                    this.scrollDelta = 0
                    this.clientHeight = el.clientHeight
                    this.updateViewport()
                    if (follow) {
                        const layout = this.beginLayoutUpdate()
                        this.finishLayoutUpdate(layout)
                    } else {
                        this._stableAnchor = this.captureAnchor()
                    }
                })
            },
            lineOrigin () {
                const el = this.$refs.box
                if (!el || typeof window === 'undefined') return 8
                let origin = parseScrollBoxPaddingTop(window.getComputedStyle(el))
                const indicator = this.$refs.loadingIndicator
                if (indicator) {
                    const style = window.getComputedStyle(indicator)
                    origin += indicator.offsetHeight + (parseFloat(style.marginBottom) || 0)
                }
                return origin
            },
            bindObserver () {
                const el = this.$refs.box
                if (!el || typeof ResizeObserver === 'undefined') return
                this.unbindObserver()
                this._ro = new ResizeObserver(() => {
                    if (this._resizeFrame) return
                    this._resizeFrame = requestAnimationFrame(() => {
                        this._resizeFrame = null
                        if (!this.$refs.box) return
                        const nextWidth = el.clientWidth
                        const nextHeight = el.clientHeight
                        if (nextWidth === this.containerWidth && nextHeight === this.clientHeight) return
                        this.cancelPendingWheel()
                        const layout = this.beginLayoutUpdate({ usePreviousIndex: true })
                        if (nextWidth !== this.containerWidth) {
                            this.containerWidth = nextWidth
                            if (this.wrap) {
                                this.pendingAnchor = layout.anchor
                                this.heightMap = {}
                                this.rebuildHeightIndex()
                            }
                        }
                        this.clientHeight = nextHeight
                        this.finishLayoutUpdate(layout)
                    })
                })
                this._ro.observe(el)
            },
            unbindObserver () {
                if (this._ro) this._ro.disconnect()
                this._ro = null
            },
            scrollToBottom () {
                this.cancelPendingWheel()
                const el = this.$refs.box
                if (!el) return
                const next = resolveScrollToBottomState({
                    pendingAnchor: this.pendingAnchor,
                    scrollHeight: el.scrollHeight,
                    clientHeight: el.clientHeight
                })
                this.pendingAnchor = next.pendingAnchor
                this.scrollDelta = next.scrollTop - el.scrollTop
                el.scrollTop = next.scrollTop
                this.scrollTop = next.scrollTop
                this._stableAnchor = null
                this.clientHeight = el.clientHeight
                this.updateViewport()
            },
            scrollToLine (lineNo) {
                this.beginUserInteraction()
                this.followIntent = false
                this.$emit('stick-bottom-change', false)
                this.cancelPendingWheel()
                const el = this.$refs.box
                if (!el) return
                this.locateGeneration += 1
                const generation = this.locateGeneration
                this.activeLocate = null
                this.pendingLocateScrollTop = null
                this.sourceLocateLineNo = ''
                this.pendingAnchor = null
                const target = String(lineNo ?? '')
                const index = resolveLineIndexByKey(this.displayLines, target)
                if (index < 0) return
                if (this.useVirtual) {
                    const lineKey = resolveLineKey(this.displayLines[index], index)
                    this.activeLocate = { target, generation }
                    this.setLocateScrollTop(scrollTopForLineKey(this.heightIndex, lineKey, { clientHeight: el.clientHeight }))
                    this.$nextTick(() => this.calibrateLocateLine(target, generation, 0))
                    return
                }
                const row = this.findLineRow(target)
                if (row) {
                    const rowRect = row.getBoundingClientRect()
                    const boxRect = el.getBoundingClientRect()
                    const adjustment = resolveLineCenterAdjustment({
                        rowTop: rowRect.top,
                        rowHeight: rowRect.height,
                        boxTop: boxRect.top,
                        boxHeight: boxRect.height
                    })
                    this.setLocateScrollTop(el.scrollTop + adjustment)
                    this.$nextTick(() => this.revealSearchHitInViewport(target, generation))
                }
            },
            scrollToSourceTarget (target) {
                this.beginUserInteraction()
                this.followIntent = false
                this.$emit('stick-bottom-change', false)
                this.cancelPendingWheel()
                this.cancelLocateCalibration()
                const el = this.$refs.box
                if (!el || !target) return
                const index = this.heightIndex.indexOfKey(target.key)
                if (index < 0) return
                const sourceHeight = this.heightIndex.heights[index] || this.heightIndex.defaultHeight
                const localOffset = Math.max(0, Math.min(sourceHeight, Number(target.localOffset) || 0))
                const sourceTop = this.lineOrigin() + this.heightIndex.prefix(index)
                const sourcePosition = sourceTop + localOffset
                const nextScrollTop = Math.max(0, Math.min(el.scrollHeight - el.clientHeight, sourcePosition - el.clientHeight / 2))
                this.pendingAnchor = null
                this.activeLocate = null
                this.pendingLocateScrollTop = null
                this.sourceLocateLineNo = target.line?.lineNo ?? ''
                this.scrollDelta = nextScrollTop - this.scrollTop
                this.scrollTop = nextScrollTop
                this.clientHeight = el.clientHeight
                const generation = this._panGeneration = (this._panGeneration || 0) + 1
                this._pendingPanGeneration = generation
                this.$nextTick(() => {
                    if (generation !== this._panGeneration || this.$refs.box !== el) return
                    this._pendingPanGeneration = 0
                    el.scrollTop = nextScrollTop
                    this.scrollTop = el.scrollTop
                    this.scrollDelta = 0
                    this.clientHeight = el.clientHeight
                    this._stableAnchor = this.followIntent ? null : this.captureAnchor()
                    this.updateViewport()
                })
            },
            setLocateScrollTop (scrollTop) {
                const el = this.$refs.box
                if (!el) return
                this.scrollDelta = scrollTop - el.scrollTop
                el.scrollTop = scrollTop
                this.pendingLocateScrollTop = el.scrollTop
                this.scrollTop = el.scrollTop
                this.clientHeight = el.clientHeight
                this.updateViewport()
            },
            findLineRow (lineKey) {
                const rows = this.$refs.box?.querySelectorAll('.lp-line') || []
                const target = String(lineKey ?? '')
                return Array.from(rows).find(row => (row.dataset.lineKey || row.dataset.lineNo) === target)
            },
            calibrateLocateLine (lineNo, generation, attempt) {
                if (!this.activeLocate || this.activeLocate.generation !== generation || generation !== this.locateGeneration) return
                const el = this.$refs.box
                if (!el) return
                this.measureVisibleRows()
                const row = this.findLineRow(lineNo)
                if (!row) {
                    if (attempt >= 2) {
                        this.activeLocate = null
                        return
                    }
                    const index = resolveLineIndexByKey(this.displayLines, lineNo)
                    if (index < 0) {
                        this.activeLocate = null
                        return
                    }
                    const lineKey = resolveLineKey(this.displayLines[index], index)
                    this.setLocateScrollTop(scrollTopForLineKey(this.heightIndex, lineKey, { clientHeight: el.clientHeight }))
                    this.$nextTick(() => this.calibrateLocateLine(lineNo, generation, attempt + 1))
                    return
                }
                const rowRect = row.getBoundingClientRect()
                const boxRect = el.getBoundingClientRect()
                const adjustment = resolveLineCenterAdjustment({
                    rowTop: rowRect.top,
                    rowHeight: rowRect.height,
                    boxTop: boxRect.top,
                    boxHeight: boxRect.height
                })
                if (Math.abs(adjustment) > 1 && attempt < 2) {
                    this.setLocateScrollTop(el.scrollTop + adjustment)
                    this.$nextTick(() => this.calibrateLocateLine(lineNo, generation, attempt + 1))
                    return
                }
                this.activeLocate = null
                this.pendingLocateScrollTop = null
                this.revealSearchHitInViewport(lineNo, generation)
            },
            revealSearchHitInViewport (lineNo, generation) {
                if (generation !== this.locateGeneration) return
                const el = this.$refs.box
                const row = this.findLineRow(lineNo)
                const hit = row?.querySelector('.lp-hl')
                if (!el || !hit) return
                const boxRect = el.getBoundingClientRect()
                const hitRect = hit.getBoundingClientRect()
                const viewportLeft = boxRect.left + el.clientLeft
                const viewportRight = viewportLeft + el.clientWidth
                const viewportTop = boxRect.top + el.clientTop
                const viewportBottom = viewportTop + el.clientHeight
                let nextTop = el.scrollTop
                let nextLeft = el.scrollLeft
                if (hitRect.top < viewportTop) nextTop -= viewportTop - hitRect.top
                else if (hitRect.bottom > viewportBottom) nextTop += hitRect.bottom - viewportBottom
                if (!this.wrap) {
                    if (hitRect.left < viewportLeft) nextLeft -= viewportLeft - hitRect.left
                    else if (hitRect.right > viewportRight) nextLeft += hitRect.right - viewportRight
                }
                el.scrollTop = Math.max(0, Math.min(el.scrollHeight - el.clientHeight, nextTop))
                el.scrollLeft = Math.max(0, Math.min(el.scrollWidth - el.clientWidth, nextLeft))
                this.scrollDelta = el.scrollTop - this.scrollTop
                this.scrollTop = el.scrollTop
                this.clientHeight = el.clientHeight
                this._stableAnchor = this.followIntent ? null : this.captureAnchor()
                this.updateViewport()
            },
            cancelLocateCalibration () {
                this.locateGeneration += 1
                this.activeLocate = null
                this.pendingLocateScrollTop = null
            },
            captureAnchor () {
                const el = this.$refs.box
                if (!el || !this.displayLines.length) return null
                const scrollTop = el.scrollTop
                const boxRect = el.getBoundingClientRect()
                const viewportTop = boxRect.top + el.clientTop
                const viewportBottom = viewportTop + el.clientHeight
                const row = Array.from(el.querySelectorAll('.lp-line')).find(node => {
                    const rect = node.getBoundingClientRect()
                    return rect.bottom > viewportTop && rect.top < viewportBottom
                })
                if (row) {
                    const lineKey = row.dataset.lineKey || row.dataset.lineNo
                    return captureAnchorFromDom({
                        rowTop: row.getBoundingClientRect().top,
                        boxTop: boxRect.top,
                        lineKey,
                        lineNo: row.dataset.lineNo
                    })
                }
                const lineOrigin = this.lineOrigin()
                const lineIndex = this.heightIndex.findIndexByOffset(Math.max(0, scrollTop - lineOrigin))
                const line = this.displayLines[lineIndex]
                if (!line) return null
                return captureAnchorFromIndex(this.heightIndex, {
                    scrollTop,
                    paddingTop: lineOrigin,
                    lineKey: resolveLineKey(line, lineIndex),
                    lineNo: line.lineNo,
                    lineIndex
                })
            },
            applyAnchor (anchor) {
                const el = this.$refs.box
                if (!el || !anchor) return
                const scrollTop = scrollTopForAnchor(this.heightIndex, anchor, {
                    paddingTop: this.lineOrigin(),
                    logs: this.displayLines
                })
                this.scrollDelta = scrollTop - el.scrollTop
                el.scrollTop = scrollTop
                this.scrollTop = scrollTop
                this.updateViewport()
            },
            consumePendingAnchor () {
                if (!this.pendingAnchor) return
                const anchor = this.pendingAnchor
                this.pendingAnchor = null
                this.applyAnchor(anchor)
            },
            restoreAnchor (anchor) {
                this.cancelPendingWheel()
                if (!anchor) return
                const active = this._layoutRestore
                if (active && active.interactionToken === this.layoutInteractionToken) {
                    if (!active.anchor) active.anchor = anchor
                    this.pendingAnchor = active.anchor
                    this.finishLayoutUpdate({
                        token: active.token,
                        interactionToken: active.interactionToken,
                        follow: false,
                        anchor: active.anchor,
                        restore: active
                    })
                    return
                }
                const layout = this.beginLayoutUpdate({ anchor: anchor })
                this.finishLayoutUpdate(layout)
            },
            preserveScrollOnPrepend (previousHeight) {
                this.cancelPendingWheel()
                const el = this.$refs.box
                if (el) {
                    const previousScrollTop = el.scrollTop
                    el.scrollTop += el.scrollHeight - previousHeight
                    this.scrollDelta = el.scrollTop - previousScrollTop
                    this.scrollTop = el.scrollTop
                    this.clientHeight = el.clientHeight
                    this.updateViewport()
                }
            },
            isContentShort () {
                const el = this.$refs.box
                return el ? el.scrollHeight <= el.clientHeight : false
            },
            getScrollTop () {
                return this.$refs.box?.scrollTop || 0
            },
            getScrollHeight () {
                return this.$refs.box?.scrollHeight || 0
            }
        }
    }
</script>

<style lang="scss" scoped>
.lp-lines-wrap {
    position: relative;
    display: flex;
    flex: 1;
    min-width: 0;
    min-height: 0;
    overflow: hidden;
    background: #2c2d34;
}
.lp-lines {
    box-sizing: border-box;
    flex: 1;
    min-width: 0;
    min-height: 0;
    overflow: auto;
    padding: 8px 24px;
    font-family: Menlo, Consolas, monospace;
    font-size: 12px;
    line-height: 20px;
    color: #f0f1f5;
}
.lp-line {
    display: flex;
    align-items: flex-start;
    margin: 0;
    min-height: 20px;
}
.lp-line > * + * {
    margin-left: 17px;
}
.lp-line.is-highlighted,
.lp-line.is-locate { background: rgba(58, 132, 255, .16); }
.lp-lineno {
    width: var(--lp-line-number-width);
    flex-shrink: 0;
    min-width: 0;
    text-align: right;
    color: #979ba5;
    font-variant-numeric: tabular-nums;
    user-select: none;
}
.lp-group-toggle { display: inline-flex; flex: 0 0 12px; align-items: center; justify-content: center; width: 12px; height: 20px; padding: 0; color: #979ba5; background: transparent; border: 0; cursor: pointer; }
.lp-group-toggle:hover, .lp-group-toggle:focus-visible { color: #f0f1f5; }
.lp-group-toggle:focus-visible { outline: 1px solid #3a84ff; }
.lp-line.is-group-header { cursor: pointer; }
.lp-line.is-group-header:hover { background: rgba(255, 255, 255, .06); }
.lp-line.is-group-header:hover .lp-group-toggle { color: #f0f1f5; }
.lp-line > .lp-group-toggle + *,
.lp-line > .lp-group-issue-count + * { margin-left: 4px; }
.lp-group-issue-count { display: inline-flex; flex: 0 0 auto; align-items: center; gap: 3px; font-size: 11px; line-height: 20px; }
.lp-group-issue-count.is-error { color: #d25050; }
.lp-group-issue-count.is-warn { color: #e18732; }
.lp-virtual-pad { flex-shrink: 0; }
.lp-time {
    flex-shrink: 0;
    min-width: 23ch;
    color: #83828c;
    font-variant-numeric: tabular-nums;
}
.lp-text { flex: 1; min-width: 0; white-space: pre; }
::v-deep .lp-text a,
::v-deep .lp-text a:visited { color: #3c96ff; }
::v-deep .lp-ovf-view {
    padding: 0;
    border: 0;
    background: transparent;
    color: #3c96ff;
    font: inherit;
    line-height: 20px;
    cursor: pointer;
    &:hover { text-decoration: underline; }
    &:focus-visible { outline: 1px solid #3c96ff; outline-offset: 1px; }
}
::v-deep .lp-ovf-size { color: #83828c; }
.lp-lines.is-wrap .lp-text { white-space: pre-wrap; word-break: break-all; }
.lp-lines:not(.is-wrap) .lp-line { width: max-content; min-width: 100%; }
.lp-lines:not(.is-wrap) .lp-text { flex: 0 0 auto; }
.lp-line.is-warn .lp-text { color: #e18732; }
.lp-line.is-error .lp-text { color: #d25050; }
.lp-indicator {
    display: block;
    margin: 0 auto 8px;
    color: #699df4;
    background: transparent;
    border: 0;
}
::v-deep .lp-hl {
    color: #fff;
    background: #3a84ff;
    border-radius: 1px;
}
</style>

<style lang="scss">
.lp-lines-wrap.has-minimap .lp-lines {
    scrollbar-width: none !important;
    -ms-overflow-style: none !important;
}
.lp-lines-wrap.has-minimap .lp-lines::-webkit-scrollbar {
    display: none !important;
    width: 0 !important;
    height: 0 !important;
    background: transparent !important;
}
@supports not selector(::-webkit-scrollbar) {
    .lp-lines-wrap:not(.has-minimap) .lp-lines {
        scrollbar-width: thin;
        scrollbar-color: rgba(240, 241, 245, .22) transparent;
    }
}
@supports selector(::-webkit-scrollbar) {
    .lp-lines-wrap:not(.has-minimap) .lp-lines {
        scrollbar-width: auto;
        scrollbar-color: auto;
    }
    .lp-lines-wrap:not(.has-minimap) .lp-lines::-webkit-scrollbar {
        width: 6px;
        height: 6px;
        background: transparent;
    }
    .lp-lines-wrap:not(.has-minimap) .lp-lines::-webkit-scrollbar-track,
    .lp-lines-wrap:not(.has-minimap) .lp-lines::-webkit-scrollbar-corner {
        background: transparent;
    }
    .lp-lines-wrap:not(.has-minimap) .lp-lines::-webkit-scrollbar-thumb {
        background: rgba(240, 241, 245, .22);
        border-radius: 3px;
    }
    .lp-lines-wrap:not(.has-minimap) .lp-lines::-webkit-scrollbar-thumb:hover {
        background: rgba(240, 241, 245, .36);
    }
}
</style>

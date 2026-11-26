<template>
    <aside
        class="lp-minimap"
        @click="onTrackClick"
    >
        <div
            class="lp-minimap-badges"
            @click.stop
        >
            <span
                v-if="errorCount > 0"
                class="lp-badge is-error"
                role="img"
                :aria-label="`ERROR ${errorCount}`"
            >
                <i
                    class="devops-icon icon-close-circle-fill lp-dot is-error"
                    aria-hidden="true"
                ></i><span class="lp-badge-count">{{ errorCount }}</span>
            </span>
            <span
                v-if="warnCount > 0"
                class="lp-badge is-warn"
                role="img"
                :aria-label="`WARN ${warnCount}`"
            >
                <i
                    class="devops-icon icon-exclamation-circle-shape lp-dot is-warn"
                    aria-hidden="true"
                ></i><span class="lp-badge-count">{{ warnCount }}</span>
            </span>
        </div>
        <div
            ref="track"
            class="lp-minimap-track"
            @mousedown="onTrackDown"
        >
            <div
                class="lp-minimap-lines"
                :style="contentStyle"
            >
                <button
                    v-for="row in previewRows"
                    :key="row.index"
                    :data-index="row.index"
                    type="button"
                    class="lp-minimap-line"
                    :class="[lineClass(row.line), { 'is-sampled': minimapLayout.isSampled }]"
                    :style="previewRowStyle(row)"
                    :aria-label="lineAriaLabel(row.line)"
                    :title="lineAriaLabel(row.line)"
                    @click.stop="onPreviewClick($event, row.line)"
                >
                    <span
                        class="lp-minimap-bar"
                        :style="previewBarStyle(row)"
                        aria-hidden="true"
                    ></span>
                </button>
            </div>
            <i
                v-for="mark in issueMarks"
                :key="mark.key"
                class="devops-icon lp-minimap-tick"
                :class="[mark.cls, mark.cls === 'is-error' ? 'icon-close-circle-fill' : 'icon-exclamation-circle-shape']"
                :style="issueMarkStyle(mark)"
            ></i>
            <div
                v-if="coordinates.interactive"
                ref="viewportThumb"
                class="lp-viewport"
                role="scrollbar"
                tabindex="0"
                :aria-valuemin="0"
                :aria-valuemax="100"
                :aria-valuenow="viewportNow"
                :aria-label="$t('logPanel.locateLine')"
                :style="viewportStyle"
                @mousedown.stop.prevent="onThumbDown"
                @keydown="onViewportKeydown"
            ></div>
        </div>
    </aside>
</template>

<script>
    import { getLogIssueLevel, resolveLogMinimapCoordinates, resolveLogMinimapKeyboardRatio, resolveLogMinimapLayout, resolveLogMinimapMarkTop, resolveLogMinimapPanRatio, resolveLogMinimapPreviewRowGeometry, resolveLogMinimapTarget, resolveLogMinimapVisualMetrics } from './logPanelAdapter'

    export default {
        name: 'LogErrorMinimap',
        props: {
            logs: { type: Array, default: () => [] },
            issueLogs: { type: Array, default: null },
            displayBase: { type: Number, default: 1 },
            viewport: { type: Object, default: null },
            heightIndex: { type: Object, default: null },
            indexVersion: { type: Number, default: 0 },
            lineOrigin: { type: Number, default: 0 },
            contentHeight: { type: Number, default: 0 },
            scrollMetrics: { type: Object, default: () => ({}) },
        },
        data () {
            return { trackHeight: 0, resizeObserver: null }
        },
        computed: {
            errorCount () {
                return (this.issueLogs || this.logs).filter(line => getLogIssueLevel(line) === 'error').length
            },
            warnCount () {
                return (this.issueLogs || this.logs).filter(line => getLogIssueLevel(line) === 'warn').length
            },
            minimapLayout () {
                const indexVersion = this.indexVersion
                const previewCap = this.trackHeight
                    ? Math.max(1, Math.min(240, Math.floor(this.trackHeight / 8)))
                    : 240
                return resolveLogMinimapLayout(this.logs, {
                    previewCap,
                    trackHeight: this.trackHeight,
                    heightIndex: this.heightIndex,
                    indexVersion,
                    lineOrigin: this.lineOrigin,
                    contentHeight: this.contentHeight,
                    scrollMetrics: this.scrollMetrics
                })
            },
            previewRows () {
                return this.minimapLayout.previewRows
            },
            issueMarks () {
                return this.minimapLayout.issueMarks
            },
            coordinates () {
                return resolveLogMinimapCoordinates(this.minimapLayout, this.viewport, this.trackHeight, this.scrollMetrics)
            },
            contentStyle () {
                return { height: this.coordinates.contentHeightPx + 'px' }
            },
            viewportStyle () {
                return {
                    top: this.coordinates.viewportTopPx + 'px',
                    height: this.coordinates.viewportHeightPx + 'px'
                }
            },
            viewportNow () {
                return Math.round(Math.max(0, Math.min(100, this.coordinates.panRatio * 100)))
            }
        },
        watch: {
            logs () {
                this.cancelTrackDrag()
                this.clearDragResetTimer()
            },
            indexVersion () {
                const gesture = this._trackGesture
                if (!gesture) return
                const currentKeys = this.heightIndex?.keys || null
                if (gesture.logsRef !== this.logs
                    || gesture.indexRef !== this.heightIndex
                    || gesture.keysRef !== currentKeys) {
                    this.cancelTrackDrag()
                    return
                }
                gesture.needsRebase = true
            }
        },
        mounted () {
            this.measureTrack()
            if (typeof ResizeObserver !== 'undefined') {
                this.resizeObserver = new ResizeObserver(this.measureTrack)
                this.resizeObserver.observe(this.$refs.track)
            } else {
                window.addEventListener('resize', this.measureTrack)
            }
        },
        beforeDestroy () {
            this.cancelTrackDrag(false)
            this.clearDragResetTimer()
            if (this.resizeObserver) this.resizeObserver.disconnect()
            else window.removeEventListener('resize', this.measureTrack)
        },
        methods: {
            previewRowStyle (row) {
                const geometry = resolveLogMinimapPreviewRowGeometry(row, this.coordinates)
                return { top: `${geometry.topPx}px`, height: `${geometry.heightPx}px` }
            },
            previewBarStyle (row) {
                const metrics = resolveLogMinimapVisualMetrics(this.coordinates.scale, row.firstVisualHeight ?? row.sourceHeight ?? row.heightPx)
                const level = this.levelOf(row.line)
                const color = level === 'error' ? '#d25050' : level === 'warn' ? '#e18732' : 'rgba(240, 241, 245, 0.45)'
                return {
                    '--lp-bar-period': `${metrics.period}px`,
                    '--lp-bar-thickness': `${metrics.thickness}px`,
                    '--lp-bar-first-start': `${metrics.firstStartPx}px`,
                    '--lp-bar-first-end': `${metrics.firstEndPx}px`,
                    '--lp-bar-color': color
                }
            },
            measureTrack () {
                const trackHeight = this.$refs.track?.clientHeight || 0
                if (trackHeight !== this.trackHeight) this.cancelTrackDrag()
                this.trackHeight = trackHeight
            },
            levelOf (line) {
                return getLogIssueLevel(line) || String(line.level || 'info').toLowerCase()
            },
            lineClass (line) {
                return 'is-' + this.levelOf(line)
            },
            issueMarkTop (mark) {
                return resolveLogMinimapMarkTop(mark, this.coordinates)
            },
            issueMarkStyle (mark) {
                const diameter = resolveLogMinimapVisualMetrics(this.coordinates.scale, mark.firstVisualHeight).markerDiameter * (2 / 3)
                return {
                    left: `${6 - diameter / 2}px`,
                    top: `${this.issueMarkTop(mark)}px`,
                    width: `${diameter}px`,
                    height: `${diameter}px`,
                    marginTop: `${-diameter / 2}px`,
                    fontSize: `${diameter}px`,
                    lineHeight: `${diameter}px`
                }
            },
            lineAriaLabel (line) {
                const lineNo = line.displayLineNo || this.displayBase
                return `${this.$t('logPanel.locateLine')} ${lineNo}`
            },
            currentPanRatio () {
                return this.coordinates.panRatio || 0
            },
            emitPanRatio (ratio) {
                this.$emit('pan', Math.max(0, Math.min(1, ratio)))
            },
            onViewportKeydown (event) {
                const ratio = this.currentPanRatio()
                const nextRatio = resolveLogMinimapKeyboardRatio(ratio, event.key)
                if (nextRatio !== null) {
                    event.preventDefault()
                    this.emitPanRatio(nextRatio)
                }
            },
            ratioToLine (clientY) {
                const track = this.$refs.track
                if (!track || !this.logs.length) return this.logs[0]
                const rect = track.getBoundingClientRect()
                return resolveLogMinimapTarget(this.minimapLayout, clientY - rect.top)?.line
            },
            onTrackClick (e) {
                if (this._suppressTrackClick) {
                    this._suppressTrackClick = false
                    if (e.detail !== 0) return
                }
                const track = this.$refs.track
                const rect = track?.getBoundingClientRect()
                const target = rect && resolveLogMinimapTarget(this.minimapLayout, e.clientY - rect.top)
                if (target) this.$emit('locate-target', target)
            },
            onPreviewClick (event, line) {
                if (this._suppressTrackClick) {
                    this._suppressTrackClick = false
                    if (event.detail !== 0) return
                }
                const track = this.$refs.track
                const rect = track?.getBoundingClientRect()
                const sourceIndex = Number(event.currentTarget?.dataset?.index)
                const row = this.previewRows?.find(item => item.index === sourceIndex) || this.previewRows?.find(item => item.line === line)
                const y = event.detail === 0 && row ? row.topPx + row.heightPx / 2 : event.clientY - (rect?.top || 0)
                const target = rect && resolveLogMinimapTarget(this.minimapLayout, y)
                if (target) this.$emit('locate-target', target)
                else this.$emit('locate', line)
            },
            onTrackDown (event) {
                if (event.button !== 0 || event.target.closest('.lp-badge, .lp-viewport')) return
                const track = this.$refs.track
                if (!track) return
                this._dragged = false
                this._suppressTrackClick = false
                this.startTrackGesture('track', event.clientY)
                this._onMove = this.onTrackMove
                this._onUp = this.onTrackUp
                document.addEventListener('mousemove', this._onMove)
                document.addEventListener('mouseup', this._onUp)
                document.addEventListener('keydown', this.onDragKeydown)
                window.addEventListener('blur', this.onDragCancel)
            },
            onTrackMove (event) {
                const gesture = this._trackGesture
                if (!gesture) return
                if (!this.prepareGestureMove(gesture)) return
                if (Math.abs(event.clientY - gesture.downY) >= 3) {
                    this._dragged = true
                    this._suppressTrackClick = true
                }
                if (this._dragged) this.panFromTrackPointer(event.clientY, gesture)
                gesture.lastY = event.clientY
            },
            onTrackUp () {
                const dragged = this._dragged
                this.unbindDrag()
                window.removeEventListener('blur', this.onDragCancel)
                this._trackGesture = null
                if (dragged) this.resetDragClickSuppression()
            },
            onDragCancel () {
                this.cancelTrackDrag()
            },
            onDragKeydown (event) {
                if (event.key === 'Escape') this.cancelTrackDrag()
            },
            startTrackGesture (kind, clientY) {
                const heightIndex = this.heightIndex
                this._trackGesture = {
                    kind,
                    downY: clientY,
                    anchorY: clientY,
                    lastY: clientY,
                    startRatio: this.currentPanRatio(),
                    dragRange: this.coordinates.dragRange,
                    logsRef: this.logs,
                    indexRef: heightIndex,
                    keysRef: heightIndex?.keys || null,
                    needsRebase: false
                }
            },
            prepareGestureMove (gesture) {
                const currentKeys = this.heightIndex?.keys || null
                if (gesture.logsRef !== this.logs
                    || gesture.indexRef !== this.heightIndex
                    || gesture.keysRef !== currentKeys
                    || !this.coordinates?.interactive
                    || !(Number(this.coordinates.dragRange) > 0)) {
                    this.cancelTrackDrag()
                    return false
                }
                if (gesture.needsRebase) {
                    gesture.startRatio = this.currentPanRatio()
                    gesture.anchorY = gesture.lastY
                    gesture.dragRange = this.coordinates.dragRange
                    gesture.needsRebase = false
                }
                return Number(gesture.dragRange) > 0
            },
            panFromTrackPointer (clientY, gesture) {
                if (!gesture.dragRange) return
                const ratio = resolveLogMinimapPanRatio({
                    trackHeight: gesture.dragRange,
                    viewportHeight: 0,
                    currentRatio: gesture.startRatio,
                    deltaY: clientY - gesture.anchorY,
                    tallContent: true
                })
                this.emitPanRatio(ratio)
            },
            resetDragClickSuppression () {
                this.clearDragResetTimer()
                this._dragResetTimer = setTimeout(() => {
                    this._dragResetTimer = null
                    this._dragged = false
                    this._suppressTrackClick = false
                }, 0)
            },
            cancelTrackDrag (suppressClick = true) {
                const hasPointerGesture = Boolean(this._trackGesture || this._onMove || this._onUp)
                const shouldSuppressClick = suppressClick && (hasPointerGesture || this._suppressTrackClick)
                this.unbindDrag()
                this.clearDragResetTimer()
                this._trackGesture = null
                this._dragged = false
                this._suppressTrackClick = shouldSuppressClick
            },
            clearDragResetTimer () {
                if (this._dragResetTimer) clearTimeout(this._dragResetTimer)
                this._dragResetTimer = null
            },
            onThumbDown (e) {
                const track = this.$refs.track
                if (!track || !this.coordinates.interactive) return
                this._dragged = false
                this._suppressTrackClick = true
                this.startTrackGesture('thumb', e.clientY)
                this._onMove = this.onThumbMove
                this._onUp = this.onTrackUp
                document.addEventListener('mousemove', this._onMove)
                document.addEventListener('mouseup', this._onUp)
                document.addEventListener('keydown', this.onDragKeydown)
                window.addEventListener('blur', this.onDragCancel)
            },
            onThumbMove (event) {
                const gesture = this._trackGesture
                if (!gesture || !this.prepareGestureMove(gesture)) return
                this._dragged = true
                this._suppressTrackClick = true
                this.panFromTrackPointer(event.clientY, gesture)
                gesture.lastY = event.clientY
            },
            unbindDrag () {
                if (this._onMove) document.removeEventListener('mousemove', this._onMove)
                if (this._onUp) document.removeEventListener('mouseup', this._onUp)
                document.removeEventListener('keydown', this.onDragKeydown)
                window.removeEventListener('blur', this.onDragCancel)
                this._onMove = null
                this._onUp = null
            }
        }
    }
</script>

<style lang="scss" scoped>
.lp-minimap {
    position: relative;
    z-index: 3;
    box-sizing: border-box;
    width: 140px;
    flex: 0 0 140px;
    height: 100%;
    padding: 4px 0;
    border-left: 1px solid #4d4f56;
    background: #2c2d34;
    cursor: pointer;
    overflow: hidden;
}
.lp-minimap-badges {
    display: flex;
    align-items: center;
    column-gap: 8px;
    height: 8px;
    margin-bottom: 5px;
    padding-left: 9px;
}
.lp-badge {
    display: inline-flex;
    align-items: center;
    column-gap: 2px;
    justify-content: flex-start;
    box-sizing: border-box;
    height: 8px;
    margin: 0;
    padding: 0;
    border: 0;
    background: transparent;
    font-family: -apple-system, system-ui, "Segoe UI", Roboto, "PingFang SC", "Microsoft YaHei", sans-serif;
    font-size: 6px;
    color: #f0f1f5;
    line-height: 8px;
    cursor: default;
}
.lp-badge-count {
    display: block;
    height: 8px;
    font: 6px/8px -apple-system, system-ui, "Segoe UI", Roboto, "PingFang SC", "Microsoft YaHei", sans-serif;
}
.lp-dot {
    display: inline-flex;
    flex: 0 0 8px;
    align-items: center;
    justify-content: center;
    width: 8px;
    height: 8px;
    border-radius: 50%;
}
.lp-dot { width: 8px; height: 8px; font-size: 8px; line-height: 8px; }
.lp-dot.is-error { color: #d25050; }
.lp-dot.is-warn { color: #e18732; }
.lp-minimap-track {
    position: relative;
    width: calc(100% + 1px);
    margin-left: -1px;
    height: calc(100% - 16px);
    overflow: hidden;
    overflow: clip;
    overscroll-behavior: none;
}
.lp-minimap-lines {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    pointer-events: none;
}
.lp-minimap-lines {
    transform-origin: top left;
    pointer-events: none;
}
.lp-minimap-line {
    position: absolute;
    left: 0;
    display: block;
    width: 100%;
    box-sizing: border-box;
    min-height: 0;
    margin: 0;
    padding: 0 10px;
    border: none;
    background: transparent;
    text-align: left;
    cursor: pointer;
    overflow: hidden;
    pointer-events: auto;
}
.lp-minimap-line::before {
    position: absolute;
    z-index: 0;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    content: '';
    pointer-events: none;
}
.lp-minimap-line.is-error::before { background: #313328; }
.lp-minimap-line.is-warn::before { background: #313328; }
.lp-minimap-line.is-sampled { overflow: hidden; }
.lp-minimap-bar {
    position: absolute;
    z-index: 1;
    display: block;
    left: 10px;
    top: 0;
    width: calc((100% - 30px) * 0.8);
    height: 100%;
    background: repeating-linear-gradient(
        to bottom,
        transparent 0,
        transparent var(--lp-bar-first-start),
        var(--lp-bar-color) var(--lp-bar-first-start),
        var(--lp-bar-color) var(--lp-bar-first-end),
        transparent var(--lp-bar-first-end),
        transparent var(--lp-bar-period)
    );
    pointer-events: none;
}
.lp-minimap-tick {
    position: absolute;
    z-index: 2;
    left: 4px;
    width: 4px;
    height: 4px;
    margin-top: -2px;
    border-radius: 50%;
    pointer-events: none;
}
.lp-minimap-tick { font-size: 4px; line-height: 4px; }
.lp-minimap-tick.is-error { color: #d25050; }
.lp-minimap-tick.is-warn { color: #e18732; }
.lp-viewport {
    position: absolute;
    z-index: 3;
    left: 0;
    top: 0;
    width: 100%;
    background: rgba(151, 155, 165, 0.22);
    border: 1px solid rgba(195, 199, 208, 0.8);
    pointer-events: auto;
    cursor: grab;
    box-sizing: border-box;
    outline: none;
}
.lp-viewport:focus {
    outline: 2px solid #3c96ff;
    outline-offset: -2px;
}
.lp-viewport:focus-visible {
    outline: 2px solid #3c96ff;
    outline-offset: -2px;
}
.lp-viewport:active { cursor: grabbing; }
</style>

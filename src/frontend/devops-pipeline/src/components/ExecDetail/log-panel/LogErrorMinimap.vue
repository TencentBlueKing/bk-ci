<template>
    <aside class="lp-minimap" @click="onTrackClick">
        <div class="lp-minimap-badges">
            <span v-if="errorCount" class="lp-badge is-error">
                <i class="lp-dot is-error"></i>{{ errorCount }}
            </span>
            <span v-if="warnCount" class="lp-badge is-warn">
                <i class="lp-dot is-warn"></i>{{ warnCount }}
            </span>
        </div>
        <div class="lp-minimap-track" ref="track">
            <div class="lp-minimap-lines">
                <button
                    v-for="row in previewRows"
                    :key="row.index"
                    type="button"
                    class="lp-minimap-line"
                    :class="lineClass(row.line)"
                    :title="'定位到第 ' + (row.line.displayLineNo || row.index + 1) + ' 行'"
                    @click.stop="$emit('jump', row.index)"
                >
                    <span class="lp-minimap-text">{{ row.line.message || row.line.text }}</span>
                </button>
            </div>
            <i
                v-for="mark in issueMarks"
                :key="mark.key"
                class="lp-minimap-tick"
                :class="mark.cls"
                :style="{ top: mark.top + '%' }"
            ></i>
            <div
                v-if="viewport && lines.length"
                class="lp-viewport"
                :style="viewportStyle"
                @mousedown.stop.prevent="onThumbDown"
            ></div>
        </div>
    </aside>
</template>

<script>
    const PREVIEW_CAP = 240
    const MARK_CAP = 400

    export default {
        name: 'LogErrorMinimap',
        props: {
            lines: { type: Array, default: () => [] },
            viewport: { type: Object, default: null }
        },
        computed: {
            errorCount () {
                return this.lines.filter(line => this.levelOf(line) === 'error').length
            },
            warnCount () {
                return this.lines.filter(line => this.levelOf(line) === 'warn').length
            },
            previewRows () {
                const n = this.lines.length
                if (!n) return []
                if (n <= PREVIEW_CAP) {
                    return this.lines.map((line, index) => ({ line, index }))
                }
                const rows = []
                for (let i = 0; i < PREVIEW_CAP; i++) {
                    const index = Math.round(i * (n - 1) / (PREVIEW_CAP - 1))
                    rows.push({ line: this.lines[index], index })
                }
                return rows
            },
            issueMarks () {
                const n = Math.max(this.lines.length, 1)
                const marks = []
                for (let i = 0; i < this.lines.length && marks.length < MARK_CAP; i++) {
                    const level = this.levelOf(this.lines[i])
                    if (level !== 'error' && level !== 'warn') continue
                    marks.push({
                        key: level + '-' + i,
                        cls: 'is-' + level,
                        top: (i / n) * 100
                    })
                }
                return marks
            },
            viewportStyle () {
                const vp = this.viewport || { top: 0, height: 100 }
                return {
                    top: vp.top + '%',
                    height: vp.height + '%'
                }
            }
        },
        beforeDestroy () {
            this.unbindDrag()
        },
        methods: {
            levelOf (line) {
                return String(line.level || 'info').toLowerCase()
            },
            lineClass (line) {
                return 'is-' + this.levelOf(line)
            },
            ratioToIndex (clientY) {
                const track = this.$refs.track
                if (!track || !this.lines.length) return 0
                const rect = track.getBoundingClientRect()
                const ratio = rect.height ? (clientY - rect.top) / rect.height : 0
                return Math.min(
                    this.lines.length - 1,
                    Math.max(0, Math.floor(ratio * this.lines.length))
                )
            },
            onTrackClick (e) {
                if (this._dragged) return
                this.$emit('jump', this.ratioToIndex(e.clientY))
            },
            onThumbDown (e) {
                const track = this.$refs.track
                if (!track) return
                const rect = track.getBoundingClientRect()
                const vp = this.viewport || { top: 0, height: 100 }
                const grab = e.clientY - (rect.top + rect.height * vp.top / 100)
                const span = Math.max(100 - vp.height, 0)
                this._dragged = false
                const onMove = (ev) => {
                    this._dragged = true
                    const y = ev.clientY - rect.top - grab
                    const max = rect.height * span / 100
                    const ratio = max <= 0 ? 0 : Math.max(0, Math.min(1, y / max))
                    this.$emit('pan', ratio)
                }
                const onUp = () => {
                    this.unbindDrag()
                    setTimeout(() => { this._dragged = false }, 0)
                }
                this._onMove = onMove
                this._onUp = onUp
                document.addEventListener('mousemove', onMove)
                document.addEventListener('mouseup', onUp)
            },
            unbindDrag () {
                if (this._onMove) document.removeEventListener('mousemove', this._onMove)
                if (this._onUp) document.removeEventListener('mouseup', this._onUp)
                this._onMove = null
                this._onUp = null
            }
        }
    }
</script>

<style scoped>
.lp-minimap {
    position: relative;
    width: 206px;
    flex-shrink: 0;
    height: 100%;
    padding: 4px 10px;
    border-left: 1px solid #4d4f56;
    background: #2c2d34;
    cursor: pointer;
    overflow: hidden;
}
.lp-minimap-badges {
    display: flex;
    align-items: center;
    gap: 8px;
    height: 8px;
    margin-bottom: 5px;
}
.lp-badge {
    display: inline-flex;
    align-items: center;
    gap: 2px;
    font-size: 6px;
    color: #f0f1f5;
    line-height: 1;
}
.lp-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
}
.lp-dot.is-error { background: #d25050; }
.lp-dot.is-warn { background: #e18732; }
.lp-minimap-track {
    position: relative;
    height: calc(100% - 16px);
    overflow: hidden;
}
.lp-minimap-lines {
    position: absolute;
    inset: 0;
    display: flex;
    flex-direction: column;
}
.lp-minimap-line {
    position: relative;
    flex: 1 1 0;
    min-height: 0;
    display: block;
    width: calc(100% + 20px);
    margin-left: -10px;
    padding: 0 10px 0 20px;
    border: none;
    background: transparent;
    text-align: left;
    cursor: pointer;
    overflow: hidden;
}
.lp-minimap-line.is-error { background: #313328; }
.lp-minimap-line.is-warn { background: rgba(49, 51, 40, 0.7); }
.lp-minimap-text {
    display: block;
    overflow: hidden;
    white-space: nowrap;
    font-family: Menlo, Consolas, monospace;
    font-size: 4px;
    line-height: 1.2;
    color: #f0f1f5;
}
.lp-minimap-line.is-warn .lp-minimap-text { color: #e18732; }
.lp-minimap-line.is-error .lp-minimap-text { color: #d25050; }
.lp-minimap-tick {
    position: absolute;
    left: 13px;
    width: 6px;
    height: 6px;
    margin-top: -3px;
    border-radius: 50%;
    pointer-events: none;
}
.lp-minimap-tick.is-error { background: #d25050; }
.lp-minimap-tick.is-warn { background: #e18732; }
.lp-viewport {
    position: absolute;
    left: -10px;
    top: 0;
    width: 206px;
    min-height: 16px;
    background: rgba(151, 155, 165, 0.18);
    pointer-events: auto;
    cursor: grab;
    box-sizing: border-box;
}
.lp-viewport:active { cursor: grabbing; }
</style>

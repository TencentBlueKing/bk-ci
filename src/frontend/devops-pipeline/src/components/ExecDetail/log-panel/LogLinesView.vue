<template>
    <div class="lp-lines-wrap" :class="{ 'is-empty': !lines.length, 'has-minimap': showMinimap && lines.length }">
        <div
            ref="box"
            class="lp-lines"
            :class="{ 'is-wrap': wrap, 'is-empty': !lines.length }"
            @scroll="onScroll"
        >
            <div v-if="emptyText && !lines.length" class="lp-empty">{{ emptyText }}</div>
            <div v-if="useVirtual" class="lp-virtual-pad" :style="{ height: padTop + 'px' }"></div>
            <div
                v-for="(line, idx) in visibleLines"
                :key="(line.lineNo || idx) + '-' + (windowStart + idx)"
                class="lp-line"
                :class="[
                    'is-' + (line.level || 'INFO').toLowerCase(),
                    { 'is-hit': windowStart + idx === activeIndex, 'is-locate': windowStart + idx === locateIndex }
                ]"
            >
                <span class="lp-lineno">{{ line.displayLineNo }}</span>
                <span v-if="showTime" class="lp-time">{{ formatLogTime(line.timestamp) }}</span>
                <span class="lp-text" v-html="highlight(line.message)"></span>
            </div>
            <div v-if="useVirtual" class="lp-virtual-pad" :style="{ height: padBottom + 'px' }"></div>
        </div>
        <log-error-minimap
            v-if="showMinimap && lines.length"
            :lines="lines"
            :viewport="viewport"
            @jump="$emit('jump', $event)"
            @pan="onPan"
        />
    </div>
</template>

<script>
    import { formatLogTime, renderLogHtml } from './logPanelAdapter'
    import LogErrorMinimap from './LogErrorMinimap'

    export default {
        components: { LogErrorMinimap },
        props: {
            lines: { type: Array, default: () => [] },
            keyword: { type: String, default: '' },
            showTime: { type: Boolean, default: false },
            wrap: { type: Boolean, default: true },
            emptyText: { type: String, default: '' },
            locateIndex: { type: Number, default: -1 },
            activeIndex: { type: Number, default: -1 },
            showMinimap: { type: Boolean, default: true }
        },
        data () {
            return {
                viewport: { top: 0, height: 100 },
                scrollTop: 0,
                clientHeight: 0
            }
        },
        computed: {
            useVirtual () {
                return !this.wrap && this.lines.length > 800
            },
            windowStart () {
                if (!this.useVirtual) return 0
                const h = 20
                return Math.max(0, Math.floor(this.scrollTop / h) - 30)
            },
            windowEnd () {
                if (!this.useVirtual) return this.lines.length
                const h = 20
                const visible = Math.ceil((this.clientHeight || 400) / h) + 60
                return Math.min(this.lines.length, this.windowStart + visible)
            },
            visibleLines () {
                if (!this.useVirtual) return this.lines
                return this.lines.slice(this.windowStart, this.windowEnd)
            },
            padTop () {
                return this.useVirtual ? this.windowStart * 20 : 0
            },
            padBottom () {
                return this.useVirtual ? (this.lines.length - this.windowEnd) * 20 : 0
            }
        },
        watch: {
            lines: {
                handler () {
                    this.$nextTick(this.updateViewport)
                }
            },
            wrap () {
                this.$nextTick(this.updateViewport)
            },
            showTime () {
                this.$nextTick(this.updateViewport)
            },
            activeIndex (val) {
                if (val >= 0) this.scrollToIndex(val)
            },
            locateIndex (val) {
                if (val >= 0) this.scrollToIndex(val)
            }
        },
        mounted () {
            this.$nextTick(() => {
                const el = this.$refs.box
                if (el) {
                    this.scrollTop = el.scrollTop
                    this.clientHeight = el.clientHeight
                }
                this.updateViewport()
                this.bindObserver()
            })
        },
        beforeDestroy () {
            this.unbindObserver()
        },
        methods: {
            formatLogTime,
            highlight (text) {
                return renderLogHtml(text, this.keyword)
            },
            onScroll () {
                const el = this.$refs.box
                if (!el) return
                this.scrollTop = el.scrollTop
                this.clientHeight = el.clientHeight
                if (el.scrollTop < 40) this.$emit('reach-top')
                const atBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 24
                this.$emit('stick-change', atBottom)
                this.updateViewport()
            },
            visibleLineRange () {
                const el = this.$refs.box
                const rows = el ? el.querySelectorAll('.lp-line') : []
                const total = this.lines.length
                if (!el || !rows.length || !total) return { first: 0, last: 0 }
                if (this.useVirtual) {
                    const h = 20
                    const first = Math.min(total - 1, Math.max(0, Math.floor(el.scrollTop / h)))
                    const last = Math.min(total - 1, first + Math.max(Math.ceil(el.clientHeight / h) - 1, 0))
                    return { first, last }
                }
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
                const total = Math.max(this.lines.length, 1)
                if (!el) return
                if (el.scrollHeight <= el.clientHeight + 1) {
                    this.viewport = { top: 0, height: 100 }
                    return
                }
                const { first, last } = this.visibleLineRange()
                const span = Math.max(last - first + 1, 1)
                this.viewport = {
                    top: (first / total) * 100,
                    height: Math.max((span / total) * 100, 100 / total)
                }
            },
            onPan (ratio) {
                const el = this.$refs.box
                if (!el) return
                const max = Math.max(el.scrollHeight - el.clientHeight, 0)
                el.scrollTop = ratio * max
                this.updateViewport()
            },
            bindObserver () {
                const el = this.$refs.box
                if (!el || typeof ResizeObserver === 'undefined') return
                this.unbindObserver()
                this._ro = new ResizeObserver(() => {
                    this.clientHeight = el.clientHeight
                    this.updateViewport()
                })
                this._ro.observe(el)
            },
            unbindObserver () {
                if (this._ro) this._ro.disconnect()
                this._ro = null
            },
            scrollToBottom () {
                const el = this.$refs.box
                if (el) el.scrollTop = el.scrollHeight
            },
            scrollToIndex (idx) {
                const el = this.$refs.box
                if (!el) return
                if (this.useVirtual) {
                    el.scrollTop = Math.max(0, idx * 20 - el.clientHeight / 3)
                    this.scrollTop = el.scrollTop
                    this.updateViewport()
                    return
                }
                const row = el.querySelectorAll('.lp-line')[idx]
                if (row && row.scrollIntoView) row.scrollIntoView({ block: 'center' })
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
.lp-lines-wrap.has-minimap .lp-lines.is-wrap {
    padding-right: 230px;
}
.lp-empty {
    padding: 8px 0 0;
    color: #83828c;
    font-family: Microsoft YaHei, PingFang SC, sans-serif;
}
.lp-line {
    display: flex;
    gap: 17px;
    align-items: flex-start;
}
.lp-line.is-hit,
.lp-line.is-locate { background: rgba(58, 132, 255, 0.16); }
.lp-lineno {
    flex-shrink: 0;
    min-width: 4ch;
    text-align: right;
    color: #979ba5;
    font-variant-numeric: tabular-nums;
}
.lp-virtual-pad { flex-shrink: 0; }
.lp-time {
    flex-shrink: 0;
    min-width: 23ch;
    color: #83828c;
    font-variant-numeric: tabular-nums;
}
.lp-text { flex: 1; min-width: 0; white-space: pre; }
.lp-lines.is-wrap .lp-text { white-space: pre-wrap; word-break: break-all; }
.lp-lines:not(.is-wrap) .lp-line { width: max-content; min-width: 100%; }
.lp-lines:not(.is-wrap) .lp-text { flex: 0 0 auto; }
.lp-line.is-warn .lp-text { color: #e18732; }
.lp-line.is-error .lp-text { color: #d25050; }
.lp-line.is-debug .lp-text { color: #83828c; }
.lp-lines.is-empty {
    display: flex;
    flex-direction: column;
    justify-content: flex-start;
}
::v-deep .lp-hl {
    background: #3a84ff;
    color: #fff;
    border-radius: 1px;
}
</style>

<style lang="scss">
/* 非 scoped：Vue scoped 对 ::-webkit-scrollbar 常失效，默认浅色轨道会变成白柱 */
.lp-lines-wrap.has-minimap .lp-lines,
.lp-lines-wrap:has(.lp-minimap) .lp-lines {
    scrollbar-width: none !important;
    -ms-overflow-style: none !important;
}
.lp-lines-wrap.has-minimap .lp-lines::-webkit-scrollbar,
.lp-lines-wrap:has(.lp-minimap) .lp-lines::-webkit-scrollbar {
    display: none !important;
    width: 0 !important;
    height: 0 !important;
    background: transparent !important;
}
.lp-lines-wrap:not(.has-minimap):not(:has(.lp-minimap)) .lp-lines {
    scrollbar-width: thin;
    scrollbar-color: rgba(240, 241, 245, 0.22) #2c2d34;
}
.lp-lines-wrap:not(.has-minimap):not(:has(.lp-minimap)) .lp-lines::-webkit-scrollbar {
    width: 6px;
    height: 6px;
    background: #2c2d34;
}
.lp-lines-wrap:not(.has-minimap):not(:has(.lp-minimap)) .lp-lines::-webkit-scrollbar-track,
.lp-lines-wrap:not(.has-minimap):not(:has(.lp-minimap)) .lp-lines::-webkit-scrollbar-corner {
    background: #2c2d34;
}
.lp-lines-wrap:not(.has-minimap):not(:has(.lp-minimap)) .lp-lines::-webkit-scrollbar-thumb {
    background: rgba(240, 241, 245, 0.22);
    border-radius: 3px;
}
</style>

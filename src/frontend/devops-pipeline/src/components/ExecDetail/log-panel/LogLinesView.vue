<template>
    <div class="lp-lines-wrap" :class="{ 'is-empty': !lines.length }">
        <div
            ref="box"
            class="lp-lines"
            :class="{ 'is-wrap': wrap, 'is-empty': !lines.length }"
            @scroll="onScroll"
        >
            <div v-if="emptyText && !lines.length" class="lp-empty">{{ emptyText }}</div>
            <div
                v-for="(line, idx) in lines"
                :key="line.lineNo + '-' + idx"
                class="lp-line"
                :class="[
                    'is-' + (line.level || 'INFO').toLowerCase(),
                    { 'is-hit': idx === activeIndex, 'is-locate': idx === locateIndex }
                ]"
            >
                <span class="lp-lineno">{{ line.lineNo }}</span>
                <span v-if="showTime" class="lp-time">{{ formatClock(line.timestamp) }}</span>
                <span class="lp-text" v-html="highlight(line.message)"></span>
            </div>
        </div>
        <log-error-minimap
            v-if="showMinimap && lines.length"
            :lines="lines"
            :viewport="viewport"
            @jump="$emit('jump', $event)"
        />
    </div>
</template>

<script>
    import { formatClock } from './logPanelAdapter'
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
                viewport: { top: 0, height: 20 }
            }
        },
        watch: {
            activeIndex (val) {
                if (val >= 0) this.scrollToIndex(val)
            },
            locateIndex (val) {
                if (val >= 0) this.scrollToIndex(val)
            }
        },
        methods: {
            formatClock,
            highlight (text) {
                const raw = String(text || '')
                    .replace(/&/g, '&amp;')
                    .replace(/</g, '&lt;')
                    .replace(/>/g, '&gt;')
                if (!this.keyword) return raw
                const kw = this.keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
                return raw.replace(new RegExp(kw, 'ig'), m => `<mark class="lp-hl">${m}</mark>`)
            },
            onScroll () {
                const el = this.$refs.box
                if (!el) return
                if (el.scrollTop < 40) this.$emit('reach-top')
                const atBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 24
                this.$emit('stick-change', atBottom)
                this.updateViewport()
            },
            updateViewport () {
                const el = this.$refs.box
                const total = Math.max(this.lines.length, 1)
                if (!el) return
                const ratio = el.scrollHeight ? el.scrollTop / el.scrollHeight : 0
                const vis = el.scrollHeight ? el.clientHeight / el.scrollHeight : 1
                this.viewport = {
                    top: ratio * 100,
                    height: Math.max(vis * 100, 100 / total)
                }
            },
            scrollToBottom () {
                const el = this.$refs.box
                if (el) el.scrollTop = el.scrollHeight
            },
            scrollToIndex (idx) {
                const el = this.$refs.box
                const row = el && el.querySelectorAll('.lp-line')[idx]
                if (row && row.scrollIntoView) row.scrollIntoView({ block: 'center' })
            }
        }
    }
</script>

<style lang="scss" scoped>
.lp-lines-wrap {
    display: flex;
    flex: 1;
    min-height: 0;
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
@supports not selector(::-webkit-scrollbar) {
    .lp-lines {
        scrollbar-width: thin;
        scrollbar-color: rgba(240, 241, 245, 0.22) transparent;
    }
}
.lp-lines::-webkit-scrollbar {
    width: 6px;
    height: 6px;
}
.lp-lines::-webkit-scrollbar-track,
.lp-lines::-webkit-scrollbar-corner { background: transparent; }
.lp-lines::-webkit-scrollbar-thumb {
    background: rgba(240, 241, 245, 0.22);
    border-radius: 3px;
}
.lp-lines::-webkit-scrollbar-thumb:hover { background: rgba(240, 241, 245, 0.36); }
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
    min-width: 28px;
    text-align: right;
    color: #979ba5;
}
.lp-time {
    flex-shrink: 0;
    min-width: 7ch;
    color: #83828c;
    font-variant-numeric: tabular-nums;
}
.lp-text { flex: 1; min-width: 0; white-space: pre; }
.lp-lines.is-wrap .lp-text { white-space: pre-wrap; word-break: break-all; }
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

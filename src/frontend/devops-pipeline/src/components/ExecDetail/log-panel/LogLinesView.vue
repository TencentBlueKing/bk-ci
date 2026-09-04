<template>
    <div ref="box" class="lp-lines" @scroll="onScroll">
        <div v-if="emptyText && !lines.length" class="lp-empty">{{ emptyText }}</div>
        <div
            v-for="(line, idx) in lines"
            :key="line.lineNo + '-' + idx"
            class="lp-line"
            :class="['is-' + (line.level || 'INFO').toLowerCase(), { wrap, active: idx === locateIndex }]"
        >
            <span class="lp-no">{{ line.lineNo }}</span>
            <span v-if="showTime" class="lp-time">{{ formatClock(line.timestamp) }}</span>
            <span class="lp-text" v-html="highlight(line.message)"></span>
        </div>
    </div>
</template>

<script>
    import { formatClock } from './logPanelAdapter'

    export default {
        props: {
            lines: { type: Array, default: () => [] },
            keyword: { type: String, default: '' },
            showTime: { type: Boolean, default: false },
            wrap: { type: Boolean, default: true },
            emptyText: { type: String, default: '' },
            locateIndex: { type: Number, default: -1 }
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
                return raw.replace(new RegExp(kw, 'ig'), m => `<mark>${m}</mark>`)
            },
            onScroll () {
                const el = this.$refs.box
                if (!el) return
                if (el.scrollTop < 40) this.$emit('reach-top')
                const atBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 24
                this.$emit('stick-change', atBottom)
            },
            scrollToBottom () {
                const el = this.$refs.box
                if (el) el.scrollTop = el.scrollHeight
            },
            scrollToIndex (idx) {
                const el = this.$refs.box
                const row = el && el.children[idx]
                if (row && row.scrollIntoView) row.scrollIntoView({ block: 'center' })
            }
        }
    }
</script>

<style lang="scss" scoped>
.lp-lines {
    flex: 1;
    overflow: auto;
    font-family: Menlo, Consolas, monospace;
    font-size: 12px;
    line-height: 20px;
    padding: 8px 12px 16px;
    background: #1e1e1e;
    color: #d4d4d4;
}
.lp-empty { color: #979ba5; padding: 16px 8px; }
.lp-line { display: flex; gap: 8px; white-space: nowrap; }
.lp-line.wrap { white-space: pre-wrap; word-break: break-all; }
.lp-line.active { background: #3a2a12; }
.lp-no { width: 48px; color: #63656e; text-align: right; flex: none; }
.lp-time { color: #63656e; flex: none; }
.lp-text { flex: 1; }
.is-warn .lp-text { color: #e6a23c; }
.is-error .lp-text { color: #f56c6c; }
.is-debug .lp-text { color: #7d7f87; }
::v-deep mark { background: #e6a23c; color: #1e1e1e; }
</style>

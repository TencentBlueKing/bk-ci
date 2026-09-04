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
                    v-for="(line, index) in lines"
                    :key="index"
                    type="button"
                    class="lp-minimap-line"
                    :class="lineClass(line)"
                    :title="'定位到第 ' + (index + 1) + ' 行'"
                    @click.stop="$emit('jump', index)"
                >
                    <i v-if="isIssue(line)" class="lp-minimap-mark" :class="lineClass(line)"></i>
                    <span class="lp-minimap-text">{{ line.message || line.text }}</span>
                </button>
                <div
                    v-if="viewport && lines.length"
                    class="lp-viewport"
                    :style="{ top: viewport.top + '%', height: viewport.height + '%' }"
                ></div>
            </div>
        </div>
    </aside>
</template>

<script>
    export default {
        name: 'LogErrorMinimap',
        props: {
            lines: { type: Array, default: () => [] },
            viewport: { type: Object, default: null }
        },
        computed: {
            issueLines () {
                return this.lines.filter(line => this.isIssue(line))
            },
            errorCount () {
                return this.issueLines.filter(line => this.levelOf(line) === 'error').length
            },
            warnCount () {
                return this.issueLines.filter(line => this.levelOf(line) === 'warn').length
            }
        },
        methods: {
            levelOf (line) {
                return String(line.level || 'info').toLowerCase()
            },
            lineClass (line) {
                return `is-${this.levelOf(line)}`
            },
            isIssue (line) {
                const level = this.levelOf(line)
                return level === 'error' || level === 'warn'
            },
            onTrackClick (e) {
                const track = this.$refs.track
                if (!track || !this.lines.length) return
                const rect = track.getBoundingClientRect()
                const index = Math.min(
                    this.lines.length - 1,
                    Math.max(0, Math.floor((e.clientY - rect.top) / 8))
                )
                this.$emit('jump', index)
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
.lp-minimap-lines { position: relative; }
.lp-minimap-line {
    position: relative;
    display: block;
    width: calc(100% + 20px);
    margin-left: -10px;
    height: 8px;
    padding: 0 10px 0 20px;
    border: none;
    background: transparent;
    text-align: left;
    cursor: pointer;
    overflow: hidden;
}
.lp-minimap-line.is-error { background: #313328; }
.lp-minimap-line.is-warn { background: rgba(49, 51, 40, 0.7); }
.lp-minimap-mark {
    position: absolute;
    left: 13px;
    top: 1px;
    width: 6px;
    height: 6px;
    border-radius: 50%;
}
.lp-minimap-mark.is-error { background: #d25050; }
.lp-minimap-mark.is-warn { background: #e18732; }
.lp-minimap-text {
    display: block;
    overflow: hidden;
    white-space: nowrap;
    font-family: Menlo, Consolas, monospace;
    font-size: 4px;
    line-height: 8px;
    color: #f0f1f5;
}
.lp-minimap-line.is-warn .lp-minimap-text { color: #e18732; }
.lp-minimap-line.is-error .lp-minimap-text { color: #d25050; }
.lp-viewport {
    position: absolute;
    left: -10px;
    top: 0;
    width: 206px;
    background: rgba(151, 155, 165, 0.15);
    pointer-events: none;
}
</style>

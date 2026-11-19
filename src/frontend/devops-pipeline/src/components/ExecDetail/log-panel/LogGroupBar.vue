<template>
    <div
        ref="root"
        class="log-group-bar"
    >
        <div
            class="lgb-control"
            :class="{ 'has-overflow': hasOverflow }"
        >
            <button
                v-if="hasOverflow"
                type="button"
                class="lgb-nav"
                :aria-label="$t('logPanel.scrollGroupsLeft')"
                :title="$t('logPanel.scrollGroupsLeft')"
                :disabled="!canScrollLeft"
                @click="scrollBy(-180)"
            >
                <i
                    class="devops-icon icon-angle-left"
                    aria-hidden="true"
                ></i>
            </button>
            <div
                ref="track"
                class="lgb-track"
                @scroll="updateScrollState"
            >
                <button
                    type="button"
                    class="lgb-chip"
                    :class="{ active: value === '' }"
                    :aria-label="$t('all')"
                    :aria-pressed="value === '' ? 'true' : 'false'"
                    @click="$emit('input', '')"
                >
                    {{ $t('all') }}
                </button>
                <button
                    v-for="tag in subTags"
                    :key="tag"
                    type="button"
                    class="lgb-chip"
                    :class="{ active: value === tag }"
                    :aria-label="tag"
                    :aria-pressed="value === tag ? 'true' : 'false'"
                    :title="tag"
                    @click="$emit('input', tag)"
                >
                    {{ tag }}
                </button>
            </div>
            <button
                v-if="hasOverflow"
                type="button"
                class="lgb-nav"
                :aria-label="$t('logPanel.scrollGroupsRight')"
                :title="$t('logPanel.scrollGroupsRight')"
                :disabled="!canScrollRight"
                @click="scrollBy(180)"
            >
                <i
                    class="devops-icon icon-angle-right"
                    aria-hidden="true"
                ></i>
            </button>
        </div>
    </div>
</template>

<script>
    export default {
        name: 'LogGroupBar',
        props: {
            value: { type: String, default: '' },
            subTags: { type: Array, default: () => [] }
        },
        data () {
            return {
                canScrollLeft: false,
                canScrollRight: false,
                hasOverflow: false,
                measureTimer: null,
                resizeObserver: null
            }
        },
        watch: {
            subTags: {
                immediate: true,
                handler () {
                    this.$nextTick(this.measure)
                }
            }
        },
        mounted () {
            this.measure()
            if (typeof ResizeObserver !== 'undefined') {
                this.resizeObserver = new ResizeObserver(this.measure)
                this.resizeObserver.observe(this.$refs.root)
            } else {
                window.addEventListener('resize', this.measure)
            }
        },
        beforeDestroy () {
            if (this.resizeObserver) this.resizeObserver.disconnect()
            else window.removeEventListener('resize', this.measure)
            this.clearMeasureTimer()
        },
        methods: {
            clearMeasureTimer () {
                if (this.measureTimer) clearTimeout(this.measureTimer)
                this.measureTimer = null
            },
            scrollBy (dx) {
                const el = this.$refs.track
                if (!el) return
                el.scrollBy({ left: dx, behavior: 'smooth' })
                this.clearMeasureTimer()
                this.measureTimer = setTimeout(() => {
                    this.measureTimer = null
                    this.measure()
                }, 220)
            },
            measure () {
                const root = this.$refs.root
                const track = this.$refs.track
                if (!root || !track) return
                const style = typeof window !== 'undefined' ? window.getComputedStyle(root) : null
                const horizontalPadding = (parseFloat(style?.paddingLeft) || 0) + (parseFloat(style?.paddingRight) || 0)
                const available = Math.max(0, root.clientWidth - horizontalPadding)
                const nextOverflow = track.scrollWidth > available + 1
                if (nextOverflow !== this.hasOverflow) {
                    this.hasOverflow = nextOverflow
                    if (!nextOverflow) track.scrollLeft = 0
                    this.$nextTick(this.updateScrollState)
                } else this.updateScrollState()
            },
            updateScrollState () {
                const el = this.$refs.track
                if (!el || !this.hasOverflow) {
                    this.canScrollLeft = false
                    this.canScrollRight = false
                    return
                }
                const max = Math.max(0, el.scrollWidth - el.clientWidth)
                this.canScrollLeft = el.scrollLeft > 2
                this.canScrollRight = max > 2 && el.scrollLeft < max - 2
            }
        }
    }
</script>

<style lang="scss" scoped>
.log-group-bar {
    display: flex;
    align-items: center;
    padding: 6px 16px;
    background: #242a36;
    border-bottom: 1px solid #4d4f56;
    flex-shrink: 0;
}
.lgb-control {
    display: flex;
    align-items: center;
    width: max-content;
    max-width: 100%;
    min-width: 0;
}
.lgb-control.has-overflow { width: 100%; }
.lgb-nav {
    flex: 0 0 24px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    height: 24px;
    min-width: 24px;
    padding: 0;
    border: 1px solid #4d4f56;
    background: #2e3342;
    color: #979ba5;
    font-size: 12px;
    border-radius: 2px;
    cursor: pointer;
    line-height: 1;
}
.lgb-nav .devops-icon { width: 12px; height: 12px; font-size: 12px; }
.lgb-nav:hover:not(:disabled) { color: #f0f1f5; border-color: #979ba5; }
.lgb-nav:focus-visible, .lgb-chip:focus-visible { outline: 2px solid #3a84ff; outline-offset: -2px; }
.lgb-nav:disabled { opacity: 0.35; cursor: default; }
.lgb-track {
    flex: 0 1 auto;
    min-width: 0;
    width: max-content;
    max-width: 100%;
    display: flex;
    align-items: center;
    overflow-x: hidden;
}
.lgb-control.has-overflow .lgb-track {
    flex: 1 1 0;
    width: auto;
    overflow-x: auto;
}
.lgb-track > * + * { margin-left: 6px; }
.lgb-track::-webkit-scrollbar { display: none; }
.lgb-chip {
    flex-shrink: 0;
    height: 24px;
    padding: 0 12px;
    border: 1px solid #4d4f56;
    background: #2e3342;
    color: #979ba5;
    font-size: 12px;
    border-radius: 2px;
    cursor: pointer;
    max-width: 160px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}
.lgb-chip:hover { color: #f0f1f5; border-color: #979ba5; }
.lgb-chip.active {
    color: #fff;
    background: #3a84ff;
    border-color: #3a84ff;
}
</style>

<template>
    <div class="log-group-bar">
        <button
            type="button"
            class="lgb-nav"
            title="向左"
            :disabled="!canScrollLeft"
            @click="scrollBy(-180)"
        >&lt;&lt;</button>
        <div ref="track" class="lgb-track" @scroll="measure">
            <button
                type="button"
                class="lgb-chip"
                :class="{ active: value === '' }"
                @click="$emit('input', '')"
            >ALL</button>
            <button
                v-for="tag in tags"
                :key="tag.value || tag"
                type="button"
                class="lgb-chip"
                :class="{ active: value === (tag.value || tag) }"
                :title="tag.label || tag"
                @click="$emit('input', tag.value || tag)"
            >{{ tag.label || tag }}</button>
        </div>
        <button
            type="button"
            class="lgb-nav"
            title="向右"
            :disabled="!canScrollRight"
            @click="scrollBy(180)"
        >&gt;&gt;</button>
    </div>
</template>

<script>
    export default {
        name: 'LogGroupBar',
        props: {
            value: { type: String, default: '' },
            tags: { type: Array, default: () => [] }
        },
        data () {
            return { canScrollLeft: false, canScrollRight: false }
        },
        watch: {
            tags: {
                immediate: true,
                handler () {
                    this.$nextTick(this.measure)
                }
            }
        },
        mounted () {
            this.measure()
            window.addEventListener('resize', this.measure)
        },
        beforeDestroy () {
            window.removeEventListener('resize', this.measure)
        },
        methods: {
            scrollBy (dx) {
                const el = this.$refs.track
                if (!el) return
                el.scrollBy({ left: dx, behavior: 'smooth' })
                setTimeout(this.measure, 220)
            },
            measure () {
                const el = this.$refs.track
                if (!el) return
                const max = el.scrollWidth - el.clientWidth
                this.canScrollLeft = el.scrollLeft > 2
                this.canScrollRight = max > 2 && el.scrollLeft < max - 2
            }
        }
    }
</script>

<style scoped>
.log-group-bar {
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 6px 12px;
    background: #1f1f1f;
    border-bottom: 1px solid #2a2a2a;
    flex-shrink: 0;
}
.lgb-nav {
    flex-shrink: 0;
    height: 24px;
    min-width: 28px;
    padding: 0 6px;
    border: 1px solid #3a3a3a;
    background: #2a2a2a;
    color: #979ba5;
    font-size: 11px;
    font-family: Menlo, Consolas, monospace;
    border-radius: 2px;
    cursor: pointer;
    line-height: 1;
}
.lgb-nav:hover:not(:disabled) { color: #d4d4d4; border-color: #5a5a5a; }
.lgb-nav:disabled { opacity: 0.35; cursor: default; }
.lgb-track {
    flex: 1;
    min-width: 0;
    display: flex;
    align-items: center;
    gap: 6px;
    overflow-x: auto;
}
.lgb-track::-webkit-scrollbar { display: none; }
.lgb-chip {
    flex-shrink: 0;
    height: 24px;
    padding: 0 10px;
    border: 1px solid #3a3a3a;
    background: transparent;
    color: #979ba5;
    font-size: 12px;
    border-radius: 2px;
    cursor: pointer;
    max-width: 160px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}
.lgb-chip:hover { color: #d4d4d4; border-color: #5a5a5a; }
.lgb-chip.active {
    color: #fff;
    background: #3a84ff;
    border-color: #3a84ff;
}
</style>

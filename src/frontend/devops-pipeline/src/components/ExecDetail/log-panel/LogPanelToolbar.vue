<template>
    <div v-show="visible" class="lp-toolbar" @mousedown.stop>
        <div ref="levelWrap" class="lp-select-wrap">
            <button
                type="button"
                class="lp-select"
                :aria-expanded="levelOpen"
                @click="toggleLevel"
            >
                <span class="lp-select-label" :title="levelLabel">{{ levelLabel }}</span>
                <i class="devops-icon icon-angle-down lp-select-arrow"></i>
            </button>
            <ul
                v-if="levelOpen"
                class="lp-select-menu"
                :style="levelMenuStyle"
            >
                <template v-for="(opt, idx) in levelOptions">
                    <li
                        v-if="idx === 1"
                        :key="'sep-' + opt.value"
                        class="lp-select-sep"
                    ></li>
                    <li
                        :key="opt.value"
                        class="lp-select-option"
                        :class="{ 'is-active': isLevelActive(opt) }"
                        @mousedown.prevent="toggleLevelValue(opt)"
                    >
                        <span class="lp-select-check">{{ isLevelActive(opt) ? '✓' : '' }}</span>
                        {{ opt.label }}
                    </li>
                </template>
            </ul>
        </div>
        <div class="lp-search">
            <input
                class="lp-search-input"
                :value="keyword"
                :placeholder="$t('logPanel.searchShort')"
                @input="$emit('update:keyword', $event.target.value)"
            >
            <i class="devops-icon icon-search lp-search-icon"></i>
        </div>
        <div class="lp-pager">
            <button type="button" class="lp-icon-btn" :disabled="!hitCount" @click="$emit('prev')">
                <i class="devops-icon icon-angle-left"></i>
            </button>
            <span class="lp-pager-text">{{ hitCount ? hitIndex + 1 : 0 }} / {{ hitCount }}</span>
            <button type="button" class="lp-icon-btn" :disabled="!hitCount" @click="$emit('next')">
                <i class="devops-icon icon-angle-right"></i>
            </button>
        </div>
        <span class="lp-divider"></span>
        <button
            type="button"
            class="lp-icon-btn"
            :class="{ active: showTime }"
            v-bk-tooltips="{ content: $t('logPanel.showTimeTip'), placements: ['top'] }"
            @click="$emit('toggle-time')"
        >
            <svg viewBox="0 0 16 16" width="16" height="16" aria-hidden="true">
                <circle cx="8" cy="8" r="6.2" fill="none" stroke="currentColor" stroke-width="1.3" />
                <path d="M8 4.6v3.6l2.4 1.4" fill="none" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" />
            </svg>
        </button>
        <button
            type="button"
            class="lp-icon-btn"
            :class="{ active: wrap }"
            v-bk-tooltips="{ content: $t('logPanel.wrapTip'), placements: ['top'] }"
            @click="$emit('toggle-wrap')"
        >
            <svg viewBox="0 0 16 16" width="16" height="16" aria-hidden="true">
                <path d="M2.5 4.5h11M2.5 8h8.2a2.3 2.3 0 010 4.6H7.2" fill="none" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" />
                <path d="M8.6 10.8L7 12.6l1.6 1.6" fill="none" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
        </button>
        <button
            type="button"
            class="lp-icon-btn"
            v-bk-tooltips="{ content: $t('downloadLog'), placements: ['top'] }"
            @click="$emit('download')"
        >
            <svg viewBox="0 0 16 16" width="16" height="16" aria-hidden="true">
                <path d="M8 2.5v8.2M5.2 8.2L8 11.1l2.8-2.9" fill="none" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round" />
                <path d="M3 13.2h10" fill="none" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" />
            </svg>
        </button>
    </div>
</template>

<script>
    const LEVEL_OPTIONS = [
        { value: 'ALL', label: 'ALL', all: true },
        { value: 'INFO', label: 'INFO' },
        { value: 'WARN', label: 'WARN' },
        { value: 'ERROR', label: 'ERROR' },
        { value: 'DEBUG', label: 'DEBUG' }
    ]

    export default {
        props: {
            visible: { type: Boolean, default: true },
            selectedLevels: { type: Array, default: () => ['INFO', 'WARN', 'ERROR'] },
            keyword: { type: String, default: '' },
            hitIndex: { type: Number, default: 0 },
            hitCount: { type: Number, default: 0 },
            showTime: { type: Boolean, default: false },
            wrap: { type: Boolean, default: true }
        },
        data () {
            return {
                levelOptions: LEVEL_OPTIONS,
                levelOpen: false,
                levelMenuStyle: {}
            }
        },
        computed: {
            isAll () {
                const sel = this.selectedLevels || []
                return sel.length === 1 && sel[0] === 'ALL'
            },
            levelLabel () {
                if (this.isAll) return 'ALL'
                const sel = this.selectedLevels || []
                if (!sel.length) return '无'
                return ['INFO', 'WARN', 'ERROR', 'DEBUG'].filter(v => sel.includes(v)).join(',')
            }
        },
        watch: {
            levelOpen (open) {
                if (open) {
                    this.$nextTick(this.placeLevelMenu)
                    document.addEventListener('mousedown', this.onDocPointer, true)
                    document.addEventListener('keydown', this.onDocKey)
                    window.addEventListener('resize', this.placeLevelMenu)
                    window.addEventListener('scroll', this.placeLevelMenu, true)
                } else {
                    this.unbindMenu()
                }
            }
        },
        mounted () {
            this.dockToHeader()
        },
        beforeDestroy () {
            this.unbindMenu()
            this.undockFromHeader()
        },
        methods: {
            dockToHeader () {
                const main = this.$el && this.$el.closest && this.$el.closest('.log-main')
                const head = main && main.querySelector('.log-head')
                if (!head || this._docked) return
                this._placeholder = document.createComment('lp-toolbar')
                if (this.$el.parentNode) {
                    this.$el.parentNode.insertBefore(this._placeholder, this.$el)
                    head.appendChild(this.$el)
                    this._docked = true
                }
            },
            undockFromHeader () {
                if (this._placeholder && this._placeholder.parentNode && this.$el) {
                    this._placeholder.parentNode.insertBefore(this.$el, this._placeholder)
                    this._placeholder.parentNode.removeChild(this._placeholder)
                }
                this._placeholder = null
                this._docked = false
            },
            unbindMenu () {
                document.removeEventListener('mousedown', this.onDocPointer, true)
                document.removeEventListener('keydown', this.onDocKey)
                window.removeEventListener('resize', this.placeLevelMenu)
                window.removeEventListener('scroll', this.placeLevelMenu, true)
            },
            isLevelActive (opt) {
                if (opt.all || opt.value === 'ALL') return this.isAll
                if (this.isAll) return false
                return (this.selectedLevels || []).includes(opt.value)
            },
            toggleLevel () {
                this.levelOpen = !this.levelOpen
            },
            toggleLevelValue (opt) {
                if (opt.all || opt.value === 'ALL') {
                    this.$emit('update:selectedLevels', ['ALL'])
                    return
                }
                const cur = (this.selectedLevels || []).filter(v => v && v !== 'ALL')
                let next = cur.includes(opt.value)
                    ? cur.filter(v => v !== opt.value)
                    : cur.concat(opt.value)
                if (!next.length) next = ['ALL']
                this.$emit('update:selectedLevels', next)
            },
            placeLevelMenu () {
                const el = this.$refs.levelWrap
                if (!el) return
                const rect = el.getBoundingClientRect()
                this.levelMenuStyle = {
                    top: `${Math.round(rect.bottom + 4)}px`,
                    left: `${Math.round(rect.left)}px`,
                    minWidth: `${Math.max(96, Math.round(rect.width))}px`
                }
            },
            onDocPointer (e) {
                const wrap = this.$refs.levelWrap
                const menu = e.target && e.target.closest && e.target.closest('.lp-select-menu')
                if (wrap && wrap.contains(e.target)) return
                if (menu) return
                this.levelOpen = false
            },
            onDocKey (e) {
                if (e.key === 'Escape') this.levelOpen = false
            }
        }
    }
</script>

<style lang="scss">
.lp-toolbar {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-shrink: 0;
    height: 26px;
    margin-left: auto;
    line-height: 1;
}
.lp-select-wrap {
    position: relative;
    width: 80px;
    height: 26px;
    flex-shrink: 0;
}
.lp-select {
    display: flex;
    align-items: center;
    width: 100%;
    height: 26px;
    padding: 0 28px 0 8px;
    background: #464953;
    border: none;
    border-radius: 4px;
    color: #979ba5;
    font-size: 12px;
    line-height: 26px;
    outline: none;
    cursor: pointer;
    box-sizing: border-box;
    text-align: left;
}
.lp-select-label {
    display: block;
    max-width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    color: #f0f1f5;
}
.lp-select-arrow {
    position: absolute;
    right: 8px;
    top: 50%;
    transform: translateY(-50%);
    pointer-events: none;
    color: #979ba5;
    font-size: 12px;
}
.lp-select-menu {
    position: fixed;
    z-index: 4000;
    margin: 0;
    padding: 4px 0;
    list-style: none;
    background: #2c2d34;
    border: 1px solid #3a3f4b;
    border-radius: 2px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.28);
    color: #f0f1f5;
    font-size: 12px;
    line-height: 20px;
}
.lp-select-sep {
    height: 1px;
    margin: 4px 8px;
    padding: 0;
    background: #4d4f56;
    list-style: none;
    pointer-events: none;
}
.lp-select-option {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 4px 12px 4px 8px;
    cursor: pointer;
    white-space: nowrap;
    &:hover { background: #3a3f4b; }
    &.is-active { color: #3a84ff; }
}
.lp-select-check {
    display: inline-flex;
    width: 12px;
    justify-content: center;
    flex-shrink: 0;
    font-size: 11px;
}
.lp-search {
    position: relative;
    width: 148px;
    height: 26px;
    flex-shrink: 0;
    line-height: 1;
}
.lp-search-input {
    display: block;
    width: 100%;
    height: 26px !important;
    min-height: 26px;
    max-height: 26px;
    margin: 0;
    padding: 0 28px 0 8px;
    background: #464953;
    border: none;
    border-radius: 4px;
    color: #f0f1f5;
    font-size: 12px;
    line-height: 26px;
    outline: none;
    box-sizing: border-box;
    vertical-align: top;
    &::placeholder { color: #979ba5; }
}
.lp-search-icon {
    position: absolute;
    right: 8px;
    top: 50%;
    transform: translateY(-50%);
    color: #979ba5;
    pointer-events: none;
    font-size: 14px;
    line-height: 1;
}
.lp-pager {
    display: flex;
    align-items: center;
    gap: 4px;
    color: #fff;
    font-size: 12px;
}
.lp-pager-text { min-width: 36px; text-align: center; }
.lp-divider {
    width: 1px;
    height: 16px;
    background: #4d4f56;
}
.lp-icon-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 24px;
    height: 24px;
    padding: 0;
    border: none;
    border-radius: 4px;
    background: transparent;
    color: #c4c6cc;
    cursor: pointer;
    flex-shrink: 0;
    &:hover { color: #fff; }
    &.active {
        color: #3a84ff;
        background: rgba(58, 132, 255, 0.16);
    }
    &:disabled { opacity: 0.4; cursor: not-allowed; }
}
</style>

<template>
    <div
        class="log-panel-toolbar"
    >
        <div class="filter-tools">
            <bk-popover
                ref="levelPopover"
                class="level-picker"
                :class="{ 'is-disabled': disabled }"
                trigger="click"
                theme="log-panel-level-menu"
                placement="bottom-start"
                :arrow="false"
                :disabled="disabled"
                :tippy-options="levelPopoverOptions"
                @show="onLevelPopoverShow"
                @hide="onLevelPopoverHide"
            >
                <button
                    ref="levelTrigger"
                    type="button"
                    class="level-trigger"
                    :disabled="disabled"
                    :aria-label="$t('logPanel.level')"
                    v-bk-tooltips="{ content: $t('logPanel.level'), delay: [0, 0], allowHTML: false, disabled: disabled || levelPopupOpen }"
                    :aria-expanded="String(levelPopupOpen)"
                    @keydown.down.prevent="openLevelPopup"
                >
                    <span class="level-summary">{{ levelSummary }}</span>
                    <i
                        class="toolbar-level-icon"
                        :style="levelArrowStyle"
                        aria-hidden="true"
                    ></i>
                </button>
                <div
                    ref="levelPopup"
                    slot="content"
                    class="log-panel-level-popup"
                    @mousedown.stop
                    @mouseup.stop
                >
                    <div
                        class="log-panel-level-group"
                        role="group"
                        :aria-label="$t('logPanel.level')"
                    >
                        <button
                            type="button"
                            class="log-panel-level-all"
                            :class="{ 'is-selected': Boolean(controls.levelAll) }"
                            :disabled="disabled"
                            @click="selectAllLevels"
                        >
                            <span
                                class="level-check"
                                aria-hidden="true"
                            ></span>
                            <span class="level-option-text">ALL</span>
                        </button>
                        <div
                            class="log-panel-level-divider"
                            role="separator"
                        ></div>
                        <label
                            v-for="level in levelOptions"
                            :key="level"
                            class="log-panel-level-option"
                        >
                            <input
                                type="checkbox"
                                :checked="!controls.levelAll && selectedLevels.includes(level)"
                                :disabled="disabled"
                                @change="toggleLevel(level, $event)"
                            >
                            <span
                                class="level-check"
                                aria-hidden="true"
                            ></span>
                            <span class="level-option-text">{{ level }}</span>
                        </label>
                    </div>
                </div>
            </bk-popover>
            <div
                class="search-box"
                :class="{ 'is-disabled': disabled }"
            >
                <input
                    ref="searchInput"
                    :value="searchInput"
                    :disabled="disabled"
                    :placeholder="$t('search')"
                    :aria-label="$t('search')"
                    @input="onSearchInput"
                    @keydown.enter.prevent="onSearchEnter"
                    @compositionstart="onSearchCompositionStart"
                    @compositionend="onSearchCompositionEnd"
                >
                <button
                    v-if="searchInput"
                    type="button"
                    class="search-clear"
                    :disabled="disabled"
                    :aria-label="$t('clearSelection')"
                    v-bk-tooltips="{ content: $t('clearSelection'), delay: [0, 0], allowHTML: false, disabled }"
                    @click.stop="clearSearch"
                >
                    <i
                        class="devops-icon icon-close"
                        aria-hidden="true"
                    ></i>
                </button>
                <i
                    class="devops-icon icon-search toolbar-search-icon"
                    aria-hidden="true"
                ></i>
            </div>
        </div>
        <span class="match-nav">
            <button
                type="button"
                :disabled="disabled || !controls.total"
                :aria-label="$t('logPanel.previousHit')"
                v-bk-tooltips="{ content: $t('logPanel.previousHit'), delay: [0, 0], allowHTML: false, disabled: disabled || !controls.total }"
                @click="$emit('move', -1)"
            >
                <svg
                    class="match-nav-icon"
                    viewBox="0 0 32 32"
                    aria-hidden="true"
                    focusable="false"
                >
                    <path
                        d="M22 30c-0.26 0-0.54-0.1-0.74-0.32l-12-13c-0.36-0.38-0.36-0.98 0-1.36l12-13c0.38-0.4 1-0.44 1.42-0.060 0.4 0.38 0.44 1 0.060 1.42l-11.38 12.32 11.38 12.32c0.38 0.4 0.34 1.040-0.060 1.42-0.2 0.18-0.44 0.26-0.68 0.26z"
                        fill="currentColor"
                    ></path>
                </svg>
            </button>
            <span class="match-count">{{ controls.total ? controls.current : 0 }} / {{ controls.total }}</span>
            <button
                type="button"
                :disabled="disabled || !controls.total"
                :aria-label="$t('logPanel.nextHit')"
                v-bk-tooltips="{ content: $t('logPanel.nextHit'), delay: [0, 0], allowHTML: false, disabled: disabled || !controls.total }"
                @click="$emit('move', 1)"
            >
                <svg
                    class="match-nav-icon"
                    viewBox="0 0 32 32"
                    aria-hidden="true"
                    focusable="false"
                >
                    <path
                        d="M10 30c-0.24 0-0.48-0.080-0.68-0.26-0.4-0.38-0.44-1-0.060-1.42l11.38-12.32-11.38-12.32c-0.38-0.4-0.34-1.040 0.060-1.42s1.040-0.34 1.42 0.060l12 13c0.36 0.38 0.36 0.98 0 1.36l-12 13c-0.2 0.22-0.48 0.32-0.74 0.32z"
                        fill="currentColor"
                    ></path>
                </svg>
            </button>
        </span>
        <span class="toolbar-divider"></span>
        <button
            type="button"
            class="toolbar-action"
            :class="{ 'is-active': controls.showTime }"
            :disabled="disabled"
            :aria-label="$t(controls.showTime ? 'logPanel.hideTime' : 'logPanel.showTime')"
            v-bk-tooltips="{ content: $t(controls.showTime ? 'logPanel.hideTime' : 'logPanel.showTime'), delay: [0, 0], allowHTML: false, disabled }"
            :aria-pressed="String(controls.showTime)"
            @click="controls.showTime = !controls.showTime"
        >
            <svg
                class="toolbar-action-icon time-icon"
                viewBox="0 0 16 16"
                fill="none"
                aria-hidden="true"
                focusable="false"
            >
                <path
                    d="M8 1C11.8656 1 15 4.13438 15 8C15 11.8656 11.8656 15 8 15C4.13438 15 1 11.8656 1 8C1 4.13438 4.13438 1 8 1ZM8 2C7.18927 2 6.40354 2.15777 5.66467 2.47175C4.95079 2.77324 4.30877 3.20594 3.75735 3.75735C3.20594 4.30877 2.77324 4.95079 2.47175 5.66467C2.15777 6.40354 2 7.18927 2 8C2 8.81073 2.15933 9.59646 2.47175 10.3353C2.77324 11.0492 3.20594 11.6912 3.75735 12.2426C4.30877 12.7941 4.95079 13.2268 5.66467 13.5282C6.40354 13.8422 7.18927 14 8 14C8.81073 14 9.59646 13.8407 10.3353 13.5282C11.0492 13.2268 11.6912 12.7941 12.2426 12.2426C12.7941 11.6912 13.2268 11.0492 13.5282 10.3353C13.8422 9.59646 14 8.81073 14 8C14 7.18927 13.8407 6.40354 13.5282 5.66467C13.2268 4.95079 12.7941 4.30877 12.2426 3.75735C11.6912 3.20594 11.0492 2.77324 10.3353 2.47175C9.59646 2.15777 8.81073 2 8 2ZM7 9V4H8V8H12V9H7Z"
                    fill="currentColor"
                    fill-rule="evenodd"
                    clip-rule="evenodd"
                ></path>
            </svg>
        </button>
        <button
            type="button"
            class="toolbar-action is-wrap-toggle"
            :class="{ 'is-active': controls.wrap }"
            :disabled="disabled"
            :aria-label="$t('logPanel.wrapLog')"
            v-bk-tooltips="{ content: $t('logPanel.wrapLog'), delay: [0, 0], allowHTML: false, disabled }"
            :aria-pressed="String(controls.wrap)"
            @click="toggleWrap"
        >
            <svg
                class="wrap-icon"
                viewBox="0 0 16 16"
                fill="none"
                aria-hidden="true"
                focusable="false"
            >
                <g
                    transform="translate(1 1) scale(0.013671875)"
                    fill="currentColor"
                >
                    <path d="M102.4 0H0v1024h102.4zM1024 0H921.6v1024H1024zM358.4 665.6h256a153.6 153.6 0 0 0 0-307.2H153.6V256h460.8a256 256 0 0 1 0 512h-256V665.6z"></path>
                    <path d="M204.8 716.8L409.6 512v409.6z"></path>
                </g>
            </svg>
        </button>
        <button
            type="button"
            class="toolbar-action"
            :disabled="disabled"
            :aria-label="$t('logPanel.downloadLog')"
            v-bk-tooltips="{ content: $t('logPanel.downloadLog'), delay: [0, 0], allowHTML: false, disabled }"
            @click="$emit('download')"
        >
            <svg
                class="toolbar-action-icon download-icon"
                viewBox="0 0 16 16"
                fill="none"
                aria-hidden="true"
                focusable="false"
            >
                <path
                    d="M3 10V13H13V10H14V13.5C14 13.775 13.775 14 13.5 14H2.5C2.225 14 2 13.775 2 13.5V10H3ZM8.5 2V9.075L10.125 7.475L10.825 8.175L8.7 10.3L8 11L7.3 10.3L5.175 8.175L5.875 7.475L7.5 9.075V2H8.5Z"
                    fill="currentColor"
                    fill-rule="evenodd"
                    clip-rule="evenodd"
                ></path>
            </svg>
        </button>
    </div>
</template>

<script>
    import angleDownIcon from '../../../../../svg-sprites/devops/angle-down.svg'
    import { formatLevelSummary, normalizeLogLevels } from './logPanelAdapter'

    const LEVEL_OPTIONS = ['INFO', 'WARN', 'ERROR', 'DEBUG']
    const DEFAULT_LEVELS = ['INFO', 'WARN', 'ERROR']

    function levelsFromLegacy (value) {
        const raw = String(value || '').trim()
        if (!raw || raw === 'ALL') return [...LEVEL_OPTIONS]
        return normalizeLogLevels(raw.split(','))
    }

    export default {
        name: 'LogPanelToolbar',
        props: {
            controls: { type: Object, required: true },
            disabled: { type: Boolean, default: false }
        },
        data () {
            return {
                levelPopupOpen: false,
                levelOptions: LEVEL_OPTIONS,
                restoreLevelTriggerFocus: false,
                levelPopoverOptions: { interactive: true, offset: '0, 4', distance: 0 },
                levelArrowStyle: {
                    backgroundColor: '#979ba5',
                    mask: `url(${angleDownIcon}) no-repeat center / contain`,
                    WebkitMask: `url(${angleDownIcon}) no-repeat center / contain`
                },
                searchInput: String(this.controls.search || ''),
                searchTimer: null,
                searchComposing: false
            }
        },
        computed: {
            selectedLevels () {
                return Array.isArray(this.controls.levels) ? this.controls.levels : DEFAULT_LEVELS
            },
            levelSummary () {
                return formatLevelSummary(this.selectedLevels, Boolean(this.controls.levelAll))
            }
        },
        watch: {
            disabled (val) {
                if (val) this.closeLevelPopup({ restoreFocus: false })
            },
            'controls.search' (value) {
                this.clearSearchTimer()
                this.searchInput = String(value || '')
            }
        },
        mounted () {
            this.ensureLevelsControl()
            document.addEventListener('keydown', this.onDocumentKeyDown)
        },
        beforeDestroy () {
            document.removeEventListener('keydown', this.onDocumentKeyDown)
            this.clearSearchTimer()
        },
        methods: {
            clearSearchTimer () {
                if (this.searchTimer) clearTimeout(this.searchTimer)
                this.searchTimer = null
            },
            onSearchInput (event) {
                this.searchInput = event.target.value
                if (this.searchComposing || event.isComposing) return
                this.clearSearchTimer()
                this.searchTimer = setTimeout(this.commitSearch, 300)
            },
            onSearchCompositionStart () {
                this.searchComposing = true
                this.clearSearchTimer()
            },
            onSearchCompositionEnd (event) {
                this.searchComposing = false
                this.searchInput = event.target.value
                this.clearSearchTimer()
                this.searchTimer = setTimeout(this.commitSearch, 300)
            },
            commitSearch () {
                this.clearSearchTimer()
                if (this.disabled) return
                const search = this.searchInput
                if (String(this.controls.search || '') !== search) this.$set(this.controls, 'search', search)
            },
            onSearchEnter () {
                if (this.searchComposing || this.disabled) return
                const searchChanged = this.searchInput !== String(this.controls.search || '')
                this.commitSearch()
                if (!searchChanged) this.$emit('move', 1)
            },
            clearSearch () {
                if (this.disabled) return
                this.clearSearchTimer()
                this.searchComposing = false
                this.searchInput = ''
                if (String(this.controls.search || '') !== '') this.$set(this.controls, 'search', '')
                this.$nextTick(() => this.$refs.searchInput?.focus())
            },
            ensureLevelsControl () {
                if (typeof this.controls.levelAll !== 'boolean') this.$set(this.controls, 'levelAll', String(this.controls.level || '').trim().toUpperCase() === 'ALL')
                if (Array.isArray(this.controls.levels) && this.controls.levels.length) return
                const levels = levelsFromLegacy(this.controls.level)
                this.$set(this.controls, 'levels', levels.length ? levels : [...DEFAULT_LEVELS])
            },
            emitLevels (levels, levelAll = false) {
                if (this.disabled) return
                const normalized = normalizeLogLevels(levels)
                if (!normalized.length) return
                const previous = normalizeLogLevels(this.controls.levels)
                this.$set(this.controls, 'levelAll', Boolean(levelAll))
                if (previous.join(',') === normalized.join(',')) return
                this.$set(this.controls, 'levels', normalized)
                this.$emit('levels-change', normalized)
            },
            toggleWrap () {
                if (this.disabled) return
                this.controls.wrap = !this.controls.wrap
            },
            openLevelPopup () {
                if (this.disabled) return
                this.ensureLevelsControl()
                this.$refs.levelPopover?.showHandler()
                this.$nextTick(() => {
                    const firstCheckbox = this.$refs.levelPopup?.querySelector('input[type="checkbox"]')
                    firstCheckbox?.focus()
                })
            },
            closeLevelPopup ({ restoreFocus = true } = {}) {
                if (!this.levelPopupOpen) return
                this.restoreLevelTriggerFocus = restoreFocus
                this.$refs.levelPopover?.hideHandler()
            },
            onLevelPopoverShow () {
                this.levelPopupOpen = true
            },
            onLevelPopoverHide () {
                this.levelPopupOpen = false
                if (this.restoreLevelTriggerFocus) {
                    this.restoreLevelTriggerFocus = false
                    this.$nextTick(() => this.$refs.levelTrigger?.focus())
                }
            },
            onDocumentKeyDown (event) {
                if (!this.levelPopupOpen || event.key !== 'Escape') return
                event.preventDefault()
                this.closeLevelPopup({ restoreFocus: true })
            },
            selectAllLevels () {
                if (this.disabled) return
                this.emitLevels([...LEVEL_OPTIONS], true)
            },
            toggleLevel (level, event) {
                const current = new Set(this.controls.levelAll ? [] : this.selectedLevels)
                const checked = event.target.checked
                if (checked) current.add(level)
                else {
                    if (current.size <= 1) {
                        this.emitLevels([...LEVEL_OPTIONS], true)
                        return
                    }
                    current.delete(level)
                }
                this.emitLevels([...current], false)
            }
        }
    }
</script>

<style lang="scss" scoped>
    .log-panel-toolbar {
        display: flex;
        grid-column: 3;
        align-items: center;
        justify-self: end;
        flex-shrink: 0;
        min-width: max-content;
        color: #c4c6cc;
        font: 12px/20px "Microsoft YaHei", sans-serif;
    }
    .log-panel-toolbar > * + * { margin-left: 8px; }
    .level-picker.is-disabled,
    .search-box.is-disabled { opacity: .55; }
    .filter-tools, .match-nav {
        display: inline-flex;
        align-items: center;
    }
    .filter-tools > * + * { margin-left: 8px; }
    .match-nav > * + * { margin-left: 4px; }
    .search-box input {
        box-sizing: border-box;
        height: 26px;
        color: #c4c6cc;
        background: #464953;
        border: 0;
        border-radius: 4px;
        font: 12px/20px "Microsoft YaHei", sans-serif;
    }
    button {
        width: 24px;
        height: 26px;
        color: inherit;
        background: transparent;
        border: 0;
        cursor: pointer;
        font: inherit;
    }
    button:focus:not(:focus-visible) { outline: none; box-shadow: none; }
    button:focus-visible {
        outline: 1px solid #979ba5;
        outline-offset: -1px;
    }
    button:disabled { opacity: .4; cursor: not-allowed; }
    .level-picker { display: inline-flex; }
    .level-trigger {
        position: relative;
        display: inline-flex;
        align-items: center;
        justify-content: flex-start;
        box-sizing: border-box;
        width: 80px;
        height: 26px;
        padding: 0 28px 0 8px;
        background: #464953;
        border-radius: 4px;
        font-size: 12px;
        text-align: left;
    }
    .level-summary { display: block; width: 44px; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .toolbar-level-icon { position: absolute; top: 50%; right: 8px; width: 12px; height: 12px; transform: translateY(-50%); pointer-events: none; }
    .log-panel-toolbar .level-trigger:focus-visible {
        outline: 1px solid #979ba5;
        outline-offset: -1px;
        box-shadow: none;
    }
    .log-panel-toolbar .level-trigger:focus:not(:focus-visible) { outline: none; box-shadow: none; }
    .search-box {
        position: relative;
        display: inline-flex;
        width: 148px;
        height: 26px;
    }
    .search-box input {
        width: 100%;
        padding: 0 48px 0 8px;
    }
    .log-panel-toolbar .search-box input:focus-visible {
        outline: none;
        box-shadow: inset 0 0 0 2px #3c96ff;
    }
    .search-box input::placeholder { color: #979ba5; }
    .search-clear {
        position: absolute;
        top: 1px;
        right: 23px;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: 20px;
        height: 24px;
        padding: 0;
        color: #979ba5;
    }
    .search-clear .devops-icon { font-size: 12px; }
    .toolbar-search-icon {
        position: absolute;
        top: 5px;
        right: 8px;
        color: #979ba5;
        width: 16px;
        height: 16px;
        font-size: 16px;
        pointer-events: none;
    }
    .match-count {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        min-width: 36px;
        height: 26px;
        color: #fff;
        white-space: nowrap;
    }
    .match-nav > button { display: inline-flex; align-items: center; justify-content: center; height: 24px; padding: 0; color: #c4c6cc; border-radius: 4px; }
    .match-nav > button:not(:disabled):hover { color: #fff; }
    .match-nav .match-nav-icon { flex: 0 0 12px; width: 12px; height: 12px; }
    .toolbar-action .wrap-icon { flex: 0 0 16px; width: 16px; height: 16px; }
    .toolbar-divider {
        width: 1px;
        height: 16px;
        background: #4d4f56;
    }
    .toolbar-action { display: inline-flex; width: 24px; height: 24px; align-items: center; justify-content: center; padding: 0; }
    .toolbar-action { color: #c4c6cc; background: transparent; border: 0; border-radius: 4px; }
    .toolbar-action + .toolbar-action { margin-left: 8px; }
    .toolbar-action:not(:disabled):hover { color: #fff; }
    .toolbar-action.is-active,
    .toolbar-action.is-active:not(:disabled):hover { color: #3a84ff; background: rgba(58, 132, 255, .16); }
    .toolbar-action svg { flex: 0 0 16px; }
    .toolbar-action-icon { flex: 0 0 16px; width: 16px; height: 16px; }
</style>

<style lang="scss">
    .tippy-tooltip.log-panel-level-menu-theme {
        padding: 0;
        background: transparent;
        border: 0;
        box-shadow: none;
    }
    .log-panel-level-popup {
        box-sizing: border-box;
        width: 96px;
        min-width: 96px;
        padding: 4px 0;
        color: #f0f1f5;
        background: #2c2d34;
        border: 1px solid #3a3f4b;
        border-radius: 2px;
        box-shadow: 0 2px 8px rgba(0, 0, 0, .28);
        font: 12px/20px -apple-system, system-ui, "Segoe UI", Roboto, "PingFang SC", "Microsoft YaHei", sans-serif;
    }
    .log-panel-level-popup .log-panel-level-all {
        box-sizing: border-box;
        width: 100%;
        display: flex;
        align-items: center;
        gap: 6px;
        height: 28px;
        margin: 0;
        padding: 4px 12px 4px 8px;
        color: inherit;
        text-align: left;
        white-space: nowrap;
        background: transparent;
        border: 0;
        font: inherit;
        cursor: pointer;
    }
    .log-panel-level-option {
        position: relative;
        display: flex;
        align-items: center;
        height: 28px;
        min-height: 28px;
        gap: 6px;
        padding: 4px 12px 4px 8px;
        color: #f0f1f5;
        cursor: pointer;
        user-select: none;
    }
    .log-panel-level-divider { height: 1px; margin: 4px 8px; background: #4d4f56; }
    .log-panel-level-all:hover, .log-panel-level-option:hover { background: #3a3f4b; }
    .log-panel-level-option input { position: absolute; left: 0; width: 1px; height: 1px; opacity: 0; }
    .log-panel-level-option .level-check,
    .log-panel-level-all .level-check { display: inline-flex; flex: 0 0 12px; align-items: center; justify-content: center; width: 12px; height: 20px; }
    .log-panel-level-option input:checked + .level-check::before,
    .log-panel-level-all.is-selected .level-check::before { content: '✓'; color: #3a84ff; font: 11px/20px Arial, sans-serif; }
    .log-panel-level-option input:checked ~ .level-option-text,
    .log-panel-level-all.is-selected .level-option-text { color: #3a84ff; }
    .log-panel-level-option input:focus-visible + .level-check { outline: 2px solid #3c96ff; outline-offset: 1px; }
</style>

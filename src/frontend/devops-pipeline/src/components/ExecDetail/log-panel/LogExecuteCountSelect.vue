<template>
    <bk-popover
        v-if="countOptions.length > 1"
        ref="popover"
        class="execute-count-picker"
        trigger="click"
        theme="log-panel-execute-count-menu"
        placement="bottom-start"
        :arrow="false"
        :disabled="disabled"
        :tippy-options="popoverOptions"
        @show="onShow"
        @hide="onHide"
    >
        <button
            ref="trigger"
            type="button"
            class="execute-count-trigger"
            :disabled="disabled"
            :aria-label="$t('logPanel.executeCount')"
            :title="$t('logPanel.executeCount')"
            aria-haspopup="menu"
            :aria-expanded="String(open)"
            @keydown.down.prevent="openMenu"
            @keydown.esc.prevent="closeMenu(true)"
        >
            <span>{{ $t('logPanel.executeCountLabel', [activeCount]) }}</span>
            <i
                class="execute-count-arrow"
                :style="arrowStyle"
                aria-hidden="true"
            ></i>
        </button>
        <div
            slot="content"
            class="execute-count-menu"
            role="menu"
            :aria-label="$t('logPanel.executeCount')"
            @mousedown.stop
        >
            <button
                v-for="count in countOptions"
                :key="count"
                :ref="'option-' + count"
                type="button"
                role="menuitemradio"
                class="execute-count-option"
                :class="{ 'is-active': Number(activeCount) === count }"
                :aria-checked="String(Number(activeCount) === count)"
                @click="selectCount(count, $event)"
                @keydown="onOptionKeyDown($event, count)"
            >
                {{ $t('logPanel.executeCountLabel', [count]) }}
            </button>
        </div>
    </bk-popover>
</template>

<script>
    import angleDownIcon from '../../../../../svg-sprites/devops/angle-down.svg'

    export default {
        name: 'LogExecuteCountSelect',
        props: {
            latestCount: { type: Number, default: 1 },
            activeCount: { type: Number, default: 1 },
            contextKey: { type: String, default: '' },
            disabled: { type: Boolean, default: false }
        },
        data () {
            return {
                open: false,
                restoreFocus: false,
                popoverOptions: { interactive: true, offset: '0, 4', distance: 0 },
                arrowStyle: {
                    backgroundColor: '#979ba5',
                    mask: `url(${angleDownIcon}) no-repeat center / contain`,
                    WebkitMask: `url(${angleDownIcon}) no-repeat center / contain`
                }
            }
        },
        computed: {
            countOptions () {
                const latest = Math.max(0, Math.floor(Number(this.latestCount) || 0))
                return Array.from({ length: latest }, (_, index) => index + 1)
            }
        },
        watch: {
            latestCount () {
                this.closeMenu(false)
            },
            activeCount () {
                this.closeMenu(false)
            },
            contextKey () {
                this.closeMenu(false)
            },
            disabled (value) {
                if (value) this.closeMenu(false)
            }
        },
        mounted () {
            document.addEventListener('keydown', this.onDocumentKeyDown)
        },
        beforeDestroy () {
            document.removeEventListener('keydown', this.onDocumentKeyDown)
        },
        methods: {
            openMenu () {
                if (this.disabled || this.countOptions.length < 2) return
                this.$refs.popover?.showHandler()
            },
            closeMenu (restoreFocus = false) {
                if (!this.open) return
                this.restoreFocus = restoreFocus
                this.$refs.popover?.hideHandler()
            },
            onShow () {
                this.open = true
                this.$nextTick(() => this.focusOption(Number(this.activeCount)))
            },
            onHide () {
                this.open = false
                if (this.restoreFocus) {
                    this.restoreFocus = false
                    this.$nextTick(() => this.$refs.trigger?.focus())
                }
            },
            onDocumentKeyDown (event) {
                if (!this.open || event.key !== 'Escape') return
                event.preventDefault()
                this.closeMenu(true)
            },
            focusOption (count) {
                const option = this.$refs[`option-${count}`]
                const target = Array.isArray(option) ? option[0] : option
                target?.focus()
            },
            focusTrigger () {
                this.$refs.trigger?.focus()
            },
            onOptionKeyDown (event, count) {
                if (event.key === 'Enter' || event.key === ' ' || event.key === 'Spacebar') {
                    event.preventDefault()
                    this.selectCount(count, { keyboard: true })
                    return
                }
                let nextCount = null
                if (event.key === 'ArrowDown') nextCount = Math.min(this.latestCount, count + 1)
                else if (event.key === 'ArrowUp') nextCount = Math.max(1, count - 1)
                else if (event.key === 'Home') nextCount = 1
                else if (event.key === 'End') nextCount = this.latestCount
                else if (event.key === 'Escape') {
                    event.preventDefault()
                    this.closeMenu(true)
                }
                if (nextCount !== null) {
                    event.preventDefault()
                    this.focusOption(nextCount)
                }
            },
            selectCount (value, event = {}) {
                const count = Number(value)
                if (this.disabled || !Number.isInteger(count) || count < 1 || count > this.latestCount) return
                const changed = count !== Number(this.activeCount)
                if (changed) {
                    this.$emit('execute-change', count)
                    if (event.keyboard) this.$emit('focus-return', this.contextKey)
                }
                this.closeMenu(!changed || !event.keyboard)
            }
        }
    }
</script>

<style lang="scss" scoped>
    .execute-count-picker { display: inline-flex; flex: 0 0 auto; }
    .execute-count-trigger {
        position: relative;
        display: inline-flex;
        align-items: center;
        box-sizing: border-box;
        width: 112px;
        height: 24px;
        padding: 0 24px 0 8px;
        overflow: hidden;
        color: #c4c6cc;
        background: #464953;
        border: 0;
        border-radius: 4px;
        font: 12px/20px "Microsoft YaHei", sans-serif;
        text-align: left;
        white-space: nowrap;
        cursor: pointer;
    }
    .execute-count-trigger:hover { color: #f0f1f5; }
    .execute-count-trigger:focus:not(:focus-visible) { outline: none; box-shadow: none; }
    .execute-count-trigger:focus-visible { outline: 1px solid #979ba5; outline-offset: -1px; box-shadow: none; }
    .execute-count-trigger:disabled { opacity: .45; cursor: not-allowed; }
    .execute-count-arrow {
        position: absolute;
        top: 50%;
        right: 8px;
        width: 12px;
        height: 12px;
        transform: translateY(-50%);
        pointer-events: none;
    }
    .execute-count-menu {
        box-sizing: border-box;
        min-width: 112px;
        max-height: 80vh;
        max-height: min(80vh, 640px);
        overflow-y: auto;
        overscroll-behavior: contain;
        padding: 4px 0;
        color: #f0f1f5;
        background: #2c2d34;
        border: 1px solid #4d4f56;
        border-radius: 2px;
        box-shadow: 0 2px 8px rgba(0, 0, 0, .35);
        font: 12px/20px -apple-system, system-ui, "Segoe UI", Roboto, "PingFang SC", "Microsoft YaHei", sans-serif;
    }
    .execute-count-option {
        display: block;
        box-sizing: border-box;
        width: 100%;
        height: 28px;
        margin: 0;
        padding: 0 12px;
        color: #c4c6cc;
        background: transparent;
        border: 0;
        font: 12px/28px -apple-system, system-ui, "Segoe UI", Roboto, "PingFang SC", "Microsoft YaHei", sans-serif;
        text-align: left;
        white-space: nowrap;
        cursor: pointer;
    }
    .execute-count-option:hover { color: #fff; background: #3a3f4b; }
    .execute-count-option.is-active { color: #3a84ff; }
    .execute-count-option:focus-visible { outline: 1px solid #979ba5; outline-offset: -1px; }
</style>

<style lang="scss">
    .tippy-tooltip.log-panel-execute-count-menu-theme {
        padding: 0;
        background: transparent;
        border: 0;
        box-shadow: none;
    }
</style>

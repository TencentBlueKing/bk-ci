<template>
    <div class="diff-jump-bar">
        <span
            :class="['jump-arrow', { 'is-disabled': !state.hasPrev }]"
            v-bk-tooltips="prevTooltips"
            @click="handlePrev"
        >
            <i class="bk-icon icon-angle-up"></i>
        </span>
        <span class="jump-count">{{ countText }}</span>
        <span
            :class="['jump-arrow', { 'is-disabled': !state.hasNext }]"
            v-bk-tooltips="nextTooltips"
            @click="handleNext"
        >
            <i class="bk-icon icon-angle-down"></i>
        </span>
    </div>
</template>

<script>
    export default {
        name: 'DiffJumpBar',
        props: {
            // YamlDiff 通过 diff-change 事件上报的状态
            state: {
                type: Object,
                default: () => ({
                    count: 0,
                    currentIndex: 0,
                    hasPrev: false,
                    hasNext: false
                })
            }
        },
        computed: {
            countText () {
                const { count, currentIndex } = this.state
                if (!count) {
                    return this.$t('noDiffs')
                }
                if (!currentIndex) {
                    // 视口在第一处差异之上
                    return this.$t('totalDiffs', [count])
                }
                return `${currentIndex} / ${count}`
            },
            prevTooltips () {
                if (!this.state.count) {
                    return this.$t('noDiffs')
                }
                if (!this.state.hasPrev) {
                    // currentIndex 为 0 表示在第一处之上，否则表示已在第一处
                    return this.state.currentIndex
                        ? this.$t('alreadyFirstDiff')
                        : this.$t('noPrevDiff')
                }
                return { content: '', disabled: true }
            },
            nextTooltips () {
                if (!this.state.count) {
                    return this.$t('noDiffs')
                }
                if (!this.state.hasNext) {
                    return this.$t('alreadyLastDiff')
                }
                return { content: '', disabled: true }
            }
        },
        methods: {
            handlePrev () {
                if (this.state.hasPrev) {
                    this.$emit('prev')
                }
            },
            handleNext () {
                if (this.state.hasNext) {
                    this.$emit('next')
                }
            }
        }
    }
</script>

<style lang="scss" scoped>
    .diff-jump-bar {
        display: flex;
        align-items: center;
        flex-shrink: 0;
        .jump-arrow {
            display: flex;
            align-items: center;
            justify-content: center;
            width: 22px;
            height: 22px;
            border-radius: 2px;
            font-size: 14px;
            color: #C4C6CC;
            cursor: pointer;
            &:hover {
                background: rgba(255, 255, 255, 0.12);
            }
            &.is-disabled {
                color: #575961;
                cursor: not-allowed;
                &:hover {
                    background: transparent;
                }
            }
        }
        .jump-count {
            min-width: 56px;
            padding: 0 4px;
            text-align: center;
            font-size: 12px;
            line-height: 22px;
            color: #C4C6CC;
            user-select: none;
            white-space: nowrap;
        }
    }
</style>

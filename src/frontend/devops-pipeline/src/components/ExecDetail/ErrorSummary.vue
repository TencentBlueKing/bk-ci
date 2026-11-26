<template>
    <div class="error-log-summary">
        <button
            type="button"
            @click="toggleCollapse"
            class="error-log-summary-header"
            :aria-expanded="String(!isCollapse)"
        >
            <span>
                <logo
                    name="angle-down"
                    :size="12"
                    aria-hidden="true"
                    :class="{
                        collapsed: isCollapse
                    }"
                />
                {{ errorTypeAlias }}
            </span>
            <span>{{ error.errorCode }}</span>
        </button>
        <div
            v-if="!isCollapse"
            class="error-log-summary-content"
            v-bk-xss-html="errorMsg"
        ></div>
    </div>
</template>

<script>
    import Logo from '@/components/Logo'
    import { resolveLogErrorTypeKey } from './log-panel/logPanelAdapter'

    export default {
        components: { Logo },
        props: {
            error: {
                type: Object,
                default: () => ({})
            }
        },
        data () {
            return {
                isCollapse: false
            }
        },
        computed: {
            errorTypeAlias () {
                const type = resolveLogErrorTypeKey(this.error?.errorType)
                return type ? this.$t(`logPanel.errorType.${type}`) : '--'
            },
            errorMsg () {
                return this.error?.errorMsg?.trim().replace(/\n/g, '<br/>') ?? ''
            }
        },
        methods: {
            toggleCollapse () {
                this.isCollapse = !this.isCollapse
            }
        }
    }
</script>

<style lang="scss">
    .error-log-summary{
        position: relative;
        padding: 8px 12px;
        gap: 8px;
        display: flex;
        flex-direction: column;
        font: 12px/20px "Microsoft YaHei", sans-serif;
        background: #404145;
        border-bottom: 1px solid #313238;
        &-header {
            width: 100%;
            border: 0;
            text-align: left;
            font: inherit;
            padding: 0;
            cursor: pointer;
            background: transparent;
            flex-shrink: 0;
            color: #d25050;
            &:focus-visible {
                outline: 2px solid #3c96ff;
                outline-offset: 2px;
            }
            display: flex;
            align-items: center;
            span:first-child {
                display: flex;
                align-items: center;
                &:after {
                    content: '-';
                    margin: 0 4px;
                    font-size: 12px;
                    color: #d25050;
                }
                > svg {
                    margin-right: 8px;
                    font-weight: bold;
                    font-size: 12px;
                    transform: rotate(0);
                    &.collapsed {
                        transition: transform 0.3s ease;
                        transform: rotate(-90deg);
                    }
                }
            }
        }
        &-content {
            padding: 0 0 0 20px;
            margin: 0;
            color: white;
            width: auto;
            overflow: auto;
            max-height: 360px;
            overflow-wrap: anywhere;
            a {
                color: #3c96ff;
                &:visited {
                    color: #3c96ff;
                }
            }
        }
    }
</style>

<template>
    <div class="pending-manual-items-alert-bar">
        <bk-popover
            ref="pendingPopover"
            class="pending-manual-items-popover-trigger"
            placement="bottom-start"
            theme="light"
            :width="560"
            ext-cls="pending-manual-items-popover"
            :tippy-options="tippyOptions"
        >
            <div
                class="pending-manual-items-alert"
                @click.stop="showPendingPopover"
            >
                <i class="devops-icon icon-exclamation"></i>
                <span>{{ $t('details.pendingManualCount', [pendingItemCount]) }}</span>
                <i class="devops-icon icon-list pending-manual-items-alert-icon"></i>
            </div>
            <div
                slot="content"
                class="pending-manual-items-popover-content"
            >
                <div class="pmi-header">
                    <span class="pmi-title">{{ $t('details.pendingManual') }}</span>
                    <span class="pmi-count">{{ pendingItemCount }}</span>
                    <i
                        class="devops-icon icon-close pmi-close"
                        @click.stop="hidePendingPopover"
                    />
                </div>
                <ul class="pmi-list">
                    <li
                        v-for="row in pendingRows"
                        :key="row.key"
                        class="pmi-item"
                    >
                        <div class="pmi-item-head">
                            <span class="pmi-item-icon">
                                <logo
                                    :name="row.config.logoName"
                                    size="14"
                                />
                            </span>
                            <span
                                v-bk-overflow-tips
                                class="pmi-item-type"
                            >{{ row.item.itemTypeDesc }}</span>
                            <span
                                v-if="row.handlerText"
                                v-bk-overflow-tips
                                class="pmi-item-handler"
                            >{{ row.handlerText }}</span>
                        </div>
                        <div class="pmi-item-body">
                            <bk-tag
                                v-if="row.item.position"
                                class="pmi-position-tag"
                            >
                                {{ row.item.position }}
                            </bk-tag>
                            <span
                                v-bk-overflow-tips
                                class="pmi-item-path"
                            >{{ row.path }}</span>
                            <bk-button
                                text
                                theme="primary"
                                class="pmi-item-action"
                                @click.stop="handleAction(row.item, row.config.action)"
                            >
                                {{ $t(row.config.actionLabelKey) }}
                            </bk-button>
                        </div>
                    </li>
                </ul>
            </div>
        </bk-popover>
    </div>
</template>

<script>
    import Logo from '@/components/Logo'
    import {
        PENDING_ITEM_ACTION,
        getPendingItemConfig
    } from './buildRunningInfoConfig'

    const TIPPY_OPTIONS = {
        theme: 'light',
        interactive: true,
        arrow: true,
        trigger: 'click',
        hideOnClick: false,
        appendTo: () => document.body
    }

    export default {
        name: 'PendingManualItemsAlert',
        components: {
            Logo
        },
        props: {
            pendingItems: {
                type: Array,
                default: () => []
            },
            pendingItemCount: {
                type: Number,
                default: 0
            }
        },
        data () {
            return {
                tippyOptions: TIPPY_OPTIONS
            }
        },
        computed: {
            pendingRows () {
                return (this.pendingItems || []).map((item, index) => ({
                    item,
                    key: `${item.stageId}-${item.containerId}-${item.taskId || index}`,
                    config: getPendingItemConfig(item.itemType),
                    path: String(item.componentPath || '').replace(/\//g, ' - '),
                    handlerText: this.formatHandlerText(item)
                }))
            }
        },
        methods: {
            showPendingPopover () {
                this.$refs.pendingPopover?.showHandler()
            },
            hidePendingPopover () {
                this.$refs.pendingPopover?.hideHandler()
            },
            formatHandlerText (item) {
                const handlers = Array.isArray(item.handlers) ? item.handlers.filter(Boolean) : []
                if (handlers.length) {
                    const separator = String(this.$i18n?.locale).startsWith('zh') ? '、' : ', '
                    return `${this.$t('details.handlerLabel')}：${handlers.join(separator)}`
                }
                if (item.handlerDesc) {
                    return `${this.$t('details.handlerLabel')}：${item.handlerDesc}`
                }
                return ''
            },
            handleAction (item, action) {
                this.hidePendingPopover()
                if (action === PENDING_ITEM_ACTION.VIEW) {
                    this.$emit('highlight', item)
                    return
                }
                this.$emit('process', item)
            }
        }
    }
</script>

<style lang="scss">
@import "@/scss/mixins/ellipsis";

.pending-manual-items-popover {
    .tippy-tooltip {
        padding: 0;
        box-shadow: 0 2px 10px 0 rgba(0, 0, 0, 0.16);
    }
}

.pending-manual-items-alert-bar {
    padding: 6px 12px;
    border: 1px solid #ffdfac;
    border-radius: 2px;
    background-color: #fff4e2;
}

.pending-manual-items-alert {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    font-size: 12px;
    line-height: 20px;
    font-weight: 700;
    color: #FF9C01;
    cursor: pointer;

    .devops-icon.icon-exclamation {
        font-size: 9px;
        padding: 3px;
        line-height: 1;
        font-weight: 700;
        background-color: #FF9C01;
        color: #fff;
        border-radius: 50%;
        margin-right: 3px;
    }

    .pending-manual-items-alert-icon {
        font-size: 12px;
        font-weight: 700;
        color: #3A84FF;
        margin-left: 18px;
    }
}

.pending-manual-items-popover-content {
    padding: 16px;
    color: #313238;
    text-align: left;

    .pmi-header {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 12px;
    }

    .pmi-title {
        font-size: 16px;
        font-weight: 700;
        line-height: 24px;
        color: #313238;
    }

    .pmi-count {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        min-width: 18px;
        height: 18px;
        padding: 0 5px;
        border-radius: 9px;
        background: #FF9C01;
        color: #fff;
        font-size: 12px;
        line-height: 18px;
    }

    .pmi-close {
        margin-left: auto;
        font-size: 14px;
        color: #979ba5;
        cursor: pointer;

        &:hover {
            color: #63656e;
        }
    }

    .pmi-list {
        margin: 0;
        padding: 0;
        list-style: none;
        display: grid;
        gap: 8px;
        padding-top: 8px;
        border-top: 1px solid #DCDEE5;
    }

    .pmi-item {
        &:not(:last-child) {
            padding-bottom: 12px;
            border-bottom: 1px solid #DCDEE5;
        }
    }

    .pmi-item-head {
        display: flex;
        align-items: center;
        gap: 8px;
        min-height: 24px;
    }

    .pmi-item-icon {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
        font-size: 12px;
    }

    .pmi-item-type {
        @include ellipsis();
        flex: 1;
        min-width: 0;
        font-size: 14px;
        line-height: 22px;
        color: #313238;
        font-weight: 500;
    }

    .pmi-item-handler {
        @include ellipsis();
        flex-shrink: 0;
        max-width: 220px;
        font-size: 12px;
        line-height: 20px;
        color: #979ba5;
    }

    .pmi-item-body {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-top: 8px;
        border-radius: 2px;
        background: #F5F7FA;
        overflow: hidden;
    }

    .pmi-position-tag {
        margin: 0;
        background: #EAEBF0;
        color: #4D4F56;
        height: 100%;
        padding: 4px 8px;
    }

    .pmi-item-path {
        @include ellipsis();
        flex: 1;
        min-width: 0;
        font-size: 12px;
        padding: 4px 8px;
        color: #979BA5;
    }

    .pmi-item-action {
        flex-shrink: 0;
        font-size: 12px;
        padding: 4px 8px;
    }
}
</style>

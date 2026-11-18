<template>
    <div class="pm">
        <div class="pm-toolbar">
            <strong class="pm-title">{{ $t('logPanel.params.title') }}</strong>
            <bk-button
                v-if="showConfigShortcut"
                class="pm-config-shortcut"
                text
                theme="primary"
                @click="$emit('view-config')"
            >
                {{ shortcutLabel || $t('logPanel.params.viewConfig') }}
            </bk-button>
        </div>
        <div class="pm-body">
            <div
                v-if="loading"
                class="pm-state"
                role="status"
            >
                {{ $t('loading') }}
            </div>
            <div
                v-else-if="loadError"
                class="pm-state pm-load-error"
                role="alert"
            >
                <span>{{ loadError }}</span>
                <bk-button
                    text
                    theme="primary"
                    class="pm-retry"
                    @click="$emit('retry')"
                >
                    {{ $t('logPanel.params.retry') }}
                </bk-button>
            </div>
            <div
                v-else-if="notExecuted"
                class="pm-state pm-not-executed"
                role="status"
            >
                <i
                    class="devops-icon icon-clock pm-state-icon"
                    aria-hidden="true"
                ></i>
                <span class="pm-state-copy">
                    <strong>{{ $t('logPanel.params.notExecutedTitle') }}</strong>
                    <span>{{ $t('logPanel.params.notExecutedDescription') }}</span>
                </span>
            </div>
            <template v-else>
                <div class="pm-search-wrap">
                    <bk-input
                        v-model="keyword"
                        class="pm-search"
                        clearable
                        right-icon="bk-icon icon-search"
                        :placeholder="$t('logPanel.params.searchPlaceholder')"
                        :aria-label="$t('search')"
                    />
                </div>
                <section
                    v-for="sec in visibleSections"
                    :key="sec.id"
                    class="pm-sec"
                >
                    <button
                        type="button"
                        class="pm-fold"
                        :class="{ 'is-disabled': sec.pending }"
                        :disabled="sec.pending"
                        :aria-expanded="String(isOpen(sec))"
                        @click="toggle(sec)"
                    >
                        <i
                            class="devops-icon icon-angle-down pm-caret"
                            aria-hidden="true"
                            :class="{ 'is-closed': !isOpen(sec) }"
                        ></i>
                        <span class="pm-fold-name">{{ sectionTitle(sec.id) }}</span>
                        <bk-tag
                            v-if="sec.pending"
                            class="pm-pending"
                            theme="info"
                        >
                            {{ $t('logPanel.params.pendingOutput') }}
                        </bk-tag>
                        <bk-tag
                            v-else
                            class="pm-count"
                        >
                            {{ sectionCount(sec) }}
                        </bk-tag>
                        <span
                            v-if="sectionAside(sec.id)"
                            class="pm-aside"
                        >
                            {{ sectionAside(sec.id) }}
                        </span>
                    </button>
                    <div
                        v-if="isOpen(sec)"
                        class="pm-table-wrap"
                    >
                        <table class="pm-table">
                            <thead>
                                <tr>
                                    <th class="pm-th-key">{{ $t('logPanel.params.key') }}</th>
                                    <th class="pm-th-val">{{ $t('logPanel.params.value') }}</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr
                                    v-for="row in sec.visibleRows"
                                    :key="sec.id + '-' + row.key"
                                >
                                    <td class="pm-td-key">
                                        <span
                                            class="pm-key"
                                            :title="row.key"
                                        >{{ row.key }}</span>
                                    </td>
                                    <td class="pm-td-val">
                                        <span class="pm-val">
                                            <span
                                                v-if="row.expr && !row.secret"
                                                v-bk-tooltips="expressionTooltip(row)"
                                                class="pm-value-text pm-expr"
                                            >{{ displayValue(row) }}</span>
                                            <span
                                                v-else
                                                class="pm-value-text"
                                                :title="displayValue(row)"
                                            >{{ displayValue(row) }}</span>
                                            <bk-button
                                                text
                                                class="pm-copy"
                                                :title="$t('logPanel.params.copy')"
                                                @click.stop="copyValue(row)"
                                            >
                                                <logo
                                                    name="copy"
                                                    :size="16"
                                                    aria-hidden="true"
                                                />
                                            </bk-button>
                                        </span>
                                    </td>
                                </tr>
                                <tr v-if="!sec.visibleRows.length">
                                    <td
                                        colspan="2"
                                        class="pm-empty"
                                    >
                                        {{ emptyText(sec) }}
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                        <div
                            v-if="canExpand(sec)"
                            class="pm-expand-row"
                        >
                            <bk-button
                                text
                                theme="primary"
                                class="pm-expand"
                                @click="expandAll(sec.id)"
                            >
                                {{ $t('logPanel.params.expandAll') }}
                            </bk-button>
                        </div>
                    </div>
                </section>
            </template>
        </div>
    </div>
</template>

<script>
    import Logo from '@/components/Logo'
    import { copyToClipboard } from '@/utils/util'
    import {
        paramCopyText,
        visibleParamRows
    } from './logPanelAdapter'

    export default {
        name: 'LogParamsView',
        components: { Logo },
        props: {
            sections: { type: Array, default: () => [] },
            notExecuted: { type: Boolean, default: false },
            loading: { type: Boolean, default: false },
            loadError: { type: String, default: '' },
            showConfigShortcut: { type: Boolean, default: false },
            shortcutLabel: { type: String, default: '' }
        },
        data () {
            return {
                keyword: '',
                open: { input: true, output: true, env: false },
                expanded: { input: false, output: false, env: false }
            }
        },
        computed: {
            searching () {
                return Boolean(this.keyword.trim())
            },
            visibleSections () {
                return (this.sections || []).map((sec) => {
                    const rows = sec.rows || []
                    return {
                        ...sec,
                        total: rows.length,
                        visibleRows: visibleParamRows(rows, {
                            keyword: this.keyword,
                            expanded: this.expanded[sec.id],
                            limit: 20
                        })
                    }
                })
            }
        },
        methods: {
            sectionTitle (id) {
                const key = `logPanel.params.${id}`
                const translated = this.$t(key)
                return translated === key ? id : translated
            },
            isOpen (sec) {
                if (sec.pending) return false
                if (this.searching) return sec.visibleRows.length > 0
                return this.open[sec.id] !== false
            },
            toggle (sec) {
                if (sec.pending || this.searching) return
                this.$set(this.open, sec.id, !this.open[sec.id])
            },
            displayValue (row) {
                return paramCopyText(row)
            },
            sectionCount (sec) {
                return this.searching ? sec.visibleRows.length : sec.total
            },
            sectionAside (id) {
                if (id === 'output') return this.$t('logPanel.params.outputAside')
                if (id === 'env') return this.$t('logPanel.params.envAside')
                return ''
            },
            expressionTooltip (row) {
                return {
                    content: this.$t('logPanel.params.expression', [row.expr]),
                    placement: 'top',
                    allowHTML: false
                }
            },
            emptyText (sec) {
                const suffix = sec.id.charAt(0).toUpperCase() + sec.id.slice(1)
                if (this.searching && sec.total) {
                    return this.$t(`logPanel.params.searchEmpty${suffix}`)
                }
                return this.$t(`logPanel.params.empty${suffix}`)
            },
            canExpand (sec) {
                return !this.searching && !this.expanded[sec.id] && sec.total > 20
            },
            expandAll (id) {
                this.$set(this.expanded, id, true)
            },
            async copyValue (row) {
                try {
                    await copyToClipboard(paramCopyText(row))
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('copySuc')
                    })
                } catch (error) {
                    this.$bkMessage({
                        theme: 'error',
                        message: this.$t('copyFailed')
                    })
                }
            }
        }
    }
</script>

<style lang="scss" scoped>
    .pm {
        display: flex;
        flex: 1;
        flex-direction: column;
        width: 100%;
        min-height: 100%;
        background: #f5f7fa;
    }

    .pm-toolbar {
        z-index: 1;
        display: flex;
        align-items: center;
        flex-shrink: 0;
        height: 42px;
        padding: 0 24px;
        background: #fff;
        box-shadow: 0 2px 4px rgba(25, 25, 41, .05);
    }

    .pm-title {
        color: #313238;
        font-size: 14px;
        font-weight: 700;
        line-height: 22px;
    }

    .pm-config-shortcut { margin-left: auto; }

    .pm-body {
        display: flex;
        flex: 1;
        flex-direction: column;
        gap: 16px;
        min-height: 0;
        padding: 24px;
        overflow: auto;
        background: #f5f7fa;
    }

    .pm-state {
        display: flex;
        align-items: center;
        justify-content: center;
        min-height: 68px;
        color: #979ba5;
        font-size: 12px;
        line-height: 20px;
    }

    .pm-not-executed,
    .pm-load-error {
        justify-content: flex-start;
        gap: 8px;
        padding: 16px;
        background: #eaebf0;
        border-radius: 2px;
    }

    .pm-retry {
        height: 20px;
        min-width: 0;
        padding: 0;
        font-size: 12px;
        line-height: 20px;
    }

    .pm-state-icon {
        flex-shrink: 0;
        width: 16px;
        height: 16px;
    }

    .pm-state-copy { display: flex; flex-direction: column; }
    .pm-state-copy strong { color: #63656e; font-weight: 500; }

    .pm-search-wrap {
        position: relative;
        flex-shrink: 0;
    }

    .pm-search {
        width: 100%;
        height: 32px;

        ::v-deep .bk-input-text,
        ::v-deep .bk-form-input {
            height: 32px;
            border-color: #c4c6cc;
            border-radius: 2px;
            font-size: 12px;
        }

        ::v-deep .bk-icon {
            color: #979ba5;
            font-size: 16px;
        }

    }

    .pm-sec {
        display: flex;
        flex-shrink: 0;
        flex-direction: column;
        gap: 6px;
        min-width: 0;
    }

    .pm-fold {
        display: flex;
        align-items: center;
        gap: 8px;
        width: 100%;
        height: 32px;
        padding: 0 16px;
        color: #313238;
        font: inherit;
        text-align: left;
        cursor: pointer;
        background: #eaebf0;
        border: 0;
        border-radius: 2px;
    }

    .pm-fold.is-disabled,
    .pm-fold:disabled {
        color: #313238;
        cursor: default;
        opacity: 1;
    }

    .pm-caret {
        flex-shrink: 0;
        width: 12px;
        height: 12px;
        color: #979ba5;
        font-size: 12px;
        line-height: 12px;
        transition: transform .12s ease;

        &.is-closed {
            transform: rotate(-90deg);
        }
    }

    .pm-fold-name {
        flex-shrink: 0;
        color: #313238;
        font-size: 14px;
        line-height: 22px;
    }

    .pm-count {
        display: inline-flex !important;
        align-items: center;
        justify-content: center;
        height: 18px !important;
        min-width: 24px;
        padding: 0 8px !important;
        color: #4d4f56 !important;
        font-size: 12px !important;
        line-height: 16px !important;
        background: #f0f1f5 !important;
        border: 1px solid #dcdee5 !important;
        border-radius: 2px !important;
    }

    .pm-pending {
        display: inline-flex !important;
        align-items: center;
        height: 20px !important;
        padding: 1px 6px !important;
        color: #1768ef !important;
        font-size: 10px !important;
        line-height: 16px !important;
        background: #e1ecff !important;
        border: 1px solid #a3c5fd !important;
        border-radius: 10px !important;
    }

    .pm-aside {
        min-width: 0;
        margin-left: auto;
        overflow: hidden;
        color: #979ba5;
        font-size: 12px;
        line-height: 20px;
        text-overflow: ellipsis;
        white-space: nowrap;
    }

    .pm-table-wrap {
        min-width: 0;
    }

    .pm-table {
        width: 100%;
        table-layout: fixed;
        background: #fff;
        border: 1px solid #dcdee5;
        border-collapse: separate;
        border-spacing: 0;
        border-radius: 2px;
    }

    .pm-table th,
    .pm-table td {
        box-sizing: border-box;
        height: 36px;
        padding: 0 12px;
        color: #4d4f56;
        font-size: 12px;
        font-weight: 400;
        line-height: 20px;
        text-align: left;
        vertical-align: middle;
        border-bottom: 1px solid #dcdee5;
    }

    .pm-table th {
        color: #313238;
        background: #fafbfd;
    }

    .pm-th-key,
    .pm-td-key {
        width: 44%;
    }

    .pm-th-val,
    .pm-td-val {
        width: 56%;
    }

    .pm-table tbody tr:last-child td {
        border-bottom: 0;
    }

    .pm-table tbody tr:hover td {
        background: #f5f7fa;
    }

    .pm-key,
    .pm-value-text {
        display: block;
        min-width: 0;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }

    .pm-val {
        display: flex;
        align-items: center;
        gap: 8px;
        min-width: 0;
    }

    .pm-value-text {
        flex: 1;
    }

    .pm-expr {
        text-decoration: underline;
        text-decoration-color: #979ba5;
        text-decoration-style: dotted;
        text-underline-offset: 2px;
        cursor: default;
    }

    .pm-copy {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        flex: 0 0 16px;
        width: 16px;
        min-width: 16px;
        height: 16px;
        padding: 0;
        color: #979ba5;
        pointer-events: none;
        opacity: 0;

        &:hover {
            color: #3a84ff;
        }
    }

    .pm-table tbody tr:hover .pm-copy,
    .pm-table tbody tr:focus-within .pm-copy,
    .pm-copy:focus-visible {
        pointer-events: auto;
        opacity: 1;
    }

    .pm-copy:focus-visible {
        border-radius: 2px;
        outline: 2px solid #3a84ff;
        outline-offset: 2px;
    }

    .pm-empty {
        height: 68px !important;
        color: #979ba5 !important;
        text-align: center !important;
    }

    .pm-expand-row {
        display: flex;
        align-items: center;
        justify-content: center;
        height: 36px;
        margin-top: -1px;
        background: #fff;
        border: 1px solid #dcdee5;
        border-radius: 0 0 2px 2px;
    }

    .pm-expand {
        height: 20px;
        min-width: 0;
        padding: 0;
        font-size: 12px;
        line-height: 20px;
    }
</style>

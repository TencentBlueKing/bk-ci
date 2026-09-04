<template>
    <div class="pm">
        <div class="pm-toolbar">
            <strong class="pm-title">{{ $t('logPanel.paramsTitle') }}</strong>
            <bk-button
                text
                theme="primary"
                class="pm-cfg"
                @click="$emit('view-config')"
            >
                <i class="devops-icon icon-file"></i>
                {{ $t('logPanel.viewPluginConfig') }}
            </bk-button>
        </div>

        <div class="pm-body">
            <template v-if="notExecuted">
                <div class="pm-pending-lead">
                    <i class="devops-icon icon-clock pm-pending-icon"></i>
                    <div class="pm-pending-copy">
                        <p class="pm-pending-title">{{ $t('logPanel.notExecutedTitle') }}</p>
                        <p class="pm-pending-desc">{{ $t('logPanel.notExecutedDesc') }}</p>
                    </div>
                </div>
            </template>

            <template v-else>
                <div class="pm-sticky-search">
                    <bk-input
                        :value="keyword"
                        class="pm-search"
                        clearable
                        :placeholder="$t('logPanel.searchParams')"
                        right-icon="bk-icon icon-search"
                        @input="keyword = $event"
                        @change="keyword = $event"
                    />
                </div>

                <section
                    v-for="sec in sections"
                    :key="sec.id"
                    class="pm-sec"
                >
                    <button
                        type="button"
                        class="pm-fold"
                        :class="{ 'is-disabled': sec.pending }"
                        :disabled="sec.pending"
                        :aria-expanded="isOpen(sec)"
                        @click="toggle(sec)"
                    >
                        <i
                            class="devops-icon icon-angle-down pm-caret"
                            :class="{ 'is-closed': !isOpen(sec) }"
                        ></i>
                        <span class="pm-fold-name">{{ sec.title }}</span>
                        <span
                            v-if="sec.pending"
                            class="pm-pending"
                        >{{ $t('logPanel.outputPending') }}</span>
                        <span
                            v-else
                            class="pm-count"
                        >{{ sec.rows.length }}</span>
                        <span
                            v-if="sec.aside"
                            class="pm-aside"
                        >{{ sec.aside }}</span>
                    </button>
                    <table
                        v-if="isOpen(sec)"
                        class="pm-table"
                    >
                        <thead>
                            <tr>
                                <th class="pm-th-key">{{ $t('logPanel.paramName') }}</th>
                                <th class="pm-th-val">{{ $t('logPanel.paramValue') }}</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr
                                v-for="row in sec.rows"
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
                                            v-if="row.expr"
                                            class="pm-expr"
                                            v-bk-tooltips="{
                                                content: $t('logPanel.exprTip', [row.expr]),
                                                placements: ['top']
                                            }"
                                        >{{ displayValue(row) }}</span>
                                        <span
                                            v-else
                                            class="pm-plain"
                                            :title="displayValue(row)"
                                        >{{ displayValue(row) }}</span>
                                        <i
                                            class="devops-icon icon-clipboard pm-copy"
                                            :title="$t('logPanel.copy')"
                                            @click="copyValue(row)"
                                        ></i>
                                    </span>
                                </td>
                            </tr>
                            <tr v-if="!sec.rows.length">
                                <td
                                    colspan="2"
                                    class="pm-empty"
                                >{{ emptyText(sec) }}</td>
                            </tr>
                        </tbody>
                    </table>
                </section>
            </template>
        </div>
    </div>
</template>

<script>
    import { copyToClipboard } from '@/utils/util'

    export default {
        name: 'LogParamsView',
        props: {
            model: {
                type: Object,
                required: true
            }
        },
        data () {
            const open = (this.model.params && this.model.params.open) || {}
            return {
                keyword: '',
                open: {
                    input: open.input !== false,
                    output: open.output !== false,
                    env: open.env !== false
                }
            }
        },
        watch: {
            'model.id' () {
                const open = (this.model.params && this.model.params.open) || {}
                this.keyword = ''
                this.open = {
                    input: open.input !== false,
                    output: open.output !== false,
                    env: open.env !== false
                }
            }
        },
        computed: {
            notExecuted () {
                return [
                    'QUEUE', 'PAUSE', 'UNEXEC', 'SKIP',
                    'DEPENDENT_WAITING', 'WAITING', 'PREPARE_ENV'
                ].includes(this.model.status)
            },
            params () {
                return this.model.params || {}
            },
            searching () {
                return !!this.keyword.trim()
            },
            sections () {
                return [
                    {
                        id: 'input',
                        title: this.$t('logPanel.sectionInput'),
                        rows: this.filterRows(this.params.input),
                        total: (this.params.input || []).length,
                        pending: false
                    },
                    {
                        id: 'output',
                        title: this.$t('logPanel.sectionOutput'),
                        rows: this.filterRows(this.params.output),
                        total: (this.params.output || []).length,
                        pending: !!this.params.outputPending && !(this.params.output || []).length,
                        aside: this.$t('logPanel.outputAside')
                    },
                    {
                        id: 'env',
                        title: this.$t('logPanel.sectionEnv'),
                        rows: this.filterRows(this.params.env),
                        total: (this.params.env || []).length,
                        pending: false,
                        aside: this.$t('logPanel.envAside')
                    }
                ]
            }
        },
        methods: {
            filterRows (rows) {
                const list = rows || []
                const kw = this.keyword.trim().toLowerCase()
                if (!kw) return list
                return list.filter((r) => {
                    const key = String(r.key || '').toLowerCase()
                    const value = String(r.value == null ? '' : r.value).toLowerCase()
                    const expr = String(r.expr || '').toLowerCase()
                    return key.includes(kw) || value.includes(kw) || expr.includes(kw)
                })
            },
            isOpen (sec) {
                if (sec.pending) return false
                return this.searching ? true : !!this.open[sec.id]
            },
            toggle (sec) {
                if (sec.pending || this.searching) return
                this.$set(this.open, sec.id, !this.open[sec.id])
            },
            displayValue (row) {
                if (row.secret || row.hidden) return '******'
                if (row.value == null) return ''
                return String(row.value)
            },
            emptyText (sec) {
                if (this.searching && sec.total) {
                    return this.$t(`logPanel.searchEmpty${sec.id.charAt(0).toUpperCase()}${sec.id.slice(1)}`)
                }
                if (sec.pending) return this.$t('logPanel.outputPending')
                return this.$t(`logPanel.empty${sec.id.charAt(0).toUpperCase()}${sec.id.slice(1)}`)
            },
            async copyValue (row) {
                const text = this.displayValue(row)
                try {
                    await copyToClipboard(text)
                    this.$bkMessage({ theme: 'success', message: this.$t('copySuc') })
                } catch (e) {
                    this.$bkMessage({ theme: 'error', message: (e && e.message) || this.$t('updateFail') })
                }
            }
        }
    }
</script>

<style lang="scss" scoped>
.pm {
    flex: 1;
    min-height: 0;
    display: flex;
    flex-direction: column;
    background: #f5f7fa;
    width: 100%;
}
.pm-toolbar {
    position: relative;
    z-index: 1;
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-shrink: 0;
    height: 42px;
    padding: 0 24px;
    background: #fff;
    box-shadow: 0 2px 4px rgba(25, 25, 41, 0.05);
}
.pm-title {
    font-size: 14px;
    font-weight: 700;
    line-height: 22px;
    color: #313238;
}
.pm-cfg {
    padding: 0;
    height: auto;
    min-width: 0;
    font-size: 14px;
    line-height: 22px;
    ::v-deep span {
        display: inline-flex;
        align-items: center;
        gap: 4px;
    }
    .devops-icon {
        margin-right: 4px;
        font-size: 16px;
    }
}
.pm-body {
    --pm-search-sticky: 60px;
    --pm-fold-h: 32px;
    --pm-fold-gap: 6px;
    --pm-head-sticky: calc(var(--pm-search-sticky) + var(--pm-fold-h));
    flex: 1;
    min-height: 0;
    overflow: auto;
    display: flex;
    flex-direction: column;
    gap: 16px;
    padding: 0 24px 24px;
}
.pm-pending-lead {
    display: flex;
    align-items: flex-start;
    gap: 10px;
    margin-top: 16px;
    padding: 16px;
    background: #eaebf0;
    border-radius: 2px;
}
.pm-pending-icon {
    flex-shrink: 0;
    margin-top: 2px;
    color: #979ba5;
    font-size: 16px;
}
.pm-pending-copy {
    min-width: 0;
}
.pm-pending-title {
    margin: 0;
    font-size: 14px;
    font-weight: 700;
    line-height: 22px;
    color: #313238;
}
.pm-pending-desc {
    margin: 4px 0 0;
    font-size: 12px;
    line-height: 20px;
    color: #63656e;
}
.pm-sticky-search {
    position: sticky;
    top: 0;
    z-index: 4;
    margin: 0 -24px;
    padding: 16px 24px 12px;
    background: #f5f7fa;
    box-shadow: 0 1px 0 #eaebf0;
}
.pm-search {
    width: 100%;
}
.pm-sec {
    display: flex;
    flex-direction: column;
    gap: var(--pm-fold-gap);
    min-width: 0;
}
.pm-fold {
    position: sticky;
    top: var(--pm-search-sticky);
    z-index: 3;
    flex-shrink: 0;
    display: flex;
    align-items: center;
    gap: 8px;
    width: 100%;
    height: var(--pm-fold-h);
    padding: 0 16px;
    border: none;
    border-radius: 2px;
    background: #eaebf0;
    color: #313238;
    cursor: pointer;
    text-align: left;
}
.pm-fold.is-disabled,
.pm-fold:disabled {
    cursor: default;
    color: #313238;
    opacity: 1;
}
.pm-caret {
    flex-shrink: 0;
    color: #63656e;
    transition: transform 0.12s ease;
}
.pm-fold.is-disabled .pm-caret {
    color: #c4c6cc;
}
.pm-caret.is-closed {
    transform: rotate(-90deg);
}
.pm-fold-name {
    font-size: 14px;
    line-height: 22px;
    color: #313238;
}
.pm-count {
    height: 18px;
    min-width: 0;
    padding: 0 8px;
    background: #f0f1f5;
    border: 1px solid #dcdee5;
    border-radius: 2px;
    color: #4d4f56;
    font-size: 12px;
    line-height: 16px;
}
.pm-pending {
    height: 20px;
    padding: 2px 6px;
    background: #e1ecff;
    border: 1px solid #a3c5fd;
    border-radius: 10px;
    color: #1768ef;
    font-size: 10px;
    line-height: 16px;
}
.pm-aside {
    margin-left: auto;
    font-size: 12px;
    line-height: 20px;
    color: #979ba5;
}
.pm-table {
    width: 100%;
    border-collapse: separate;
    border-spacing: 0;
    table-layout: fixed;
    background: #fff;
    border: 1px solid #dcdee5;
    border-radius: 2px;
}
.pm-table th,
.pm-table td {
    height: 36px;
    padding: 0 12px;
    border-bottom: 1px solid #dcdee5;
    font-size: 12px;
    line-height: 20px;
    text-align: left;
    vertical-align: middle;
    box-sizing: border-box;
}
.pm-table th {
    position: sticky;
    top: var(--pm-head-sticky);
    z-index: 2;
    background: #fafbfd;
    color: #313238;
    font-weight: 400;
    border-bottom: 1px solid #dcdee5;
}
.pm-th-key,
.pm-td-key {
    width: 33.33%;
}
.pm-th-val,
.pm-td-val {
    width: 66.67%;
}
.pm-table tbody tr:last-child td {
    border-bottom: none;
}
.pm-table tbody tr:hover td {
    background: #f5f7fa;
}
.pm-key {
    display: block;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-family: Menlo, Consolas, monospace;
    color: #313238;
}
.pm-plain {
    display: block;
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
.pm-expr {
    display: block;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    color: #4d4f56;
    text-decoration: underline;
    text-decoration-style: dotted;
    text-decoration-color: #979ba5;
    text-underline-offset: 2px;
    cursor: default;
}
.pm-copy {
    flex-shrink: 0;
    display: none;
    color: #979ba5;
    cursor: pointer;
    font-size: 14px;
}
.pm-copy:hover {
    color: #3a84ff;
}
.pm-table tbody tr:hover .pm-copy {
    display: inline-flex;
}
.pm-empty {
    padding: 24px 0;
    font-size: 12px;
    line-height: 20px;
    color: #979ba5;
    text-align: center;
}
</style>

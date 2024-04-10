<template>
    <bk-table
        :data="list"
        :size="tableSize"
        height="100%"
        class="ip-table"
        :key="Object.keys(allRenderColumnMap).length"
        :scroll-loading="{ isLoading: paginationChangeLoading }"
        :row-class-name="rowClassName"
        @scroll-end="handleScrollEnd"
        @row-click="handleRowClick"
    >
        <bk-table-column
            label="IP"
            prop="ip"
            :min-width="150"
            show-overflow-tooltip
        >
            <template slot-scope="{ row }">
                <div
                    class="ip-box"
                    :class="row.result">
                    {{ row.ip }}
                </div>
            </template>
        </bk-table-column>
        <bk-table-column
            v-if="allRenderColumnMap.ipv6"
            label="IPv6"
            prop="ipv6"
            width="150"
            show-overflow-tooltip
        >
            <template slot-scope="{ row }">
                {{ row.ipv6 || '--' }}
            </template>
        </bk-table-column>
        <bk-table-column
            v-if="allRenderColumnMap.totalTime"
            :label="$t('environment.耗时(s)')"
            prop="totalTime"
            sortable
            width="100"
            show-overflow-tooltip
        >
            <template slot-scope="{ row }">
                {{ row.totalTime / 1000 }}
            </template>
        </bk-table-column>
        <bk-table-column
            v-if="allRenderColumnMap.bkCloudId"
            :label="$t('environment.管控区域')"
            prop="bkCloudId"
            sortable
            width="100"
            show-overflow-tooltip
        >
            <template slot-scope="{ row }">
                {{ row.bkCloudName || row.bkCloudId || '--' }}
            </template>
        </bk-table-column>
        <bk-table-column
            v-if="allRenderColumnMap.exitCode"
            :label="$t('environment.返回码')"
            prop="exitCode"
            width="100"
            sortable
            show-overflow-tooltip
        >
        </bk-table-column>
        <bk-table-column
            v-if="allRenderColumnMap.bkAgentId"
            label="Agent ID"
            prop="bkAgentId"
            width="100"
            show-overflow-tooltip
        >
        </bk-table-column>
        <bk-table-column
            v-if="allRenderColumnMap.bkHostId"
            label="Host ID"
            prop="bkHostId"
            sortable
            width="100"
            show-overflow-tooltip
        >
        </bk-table-column>
        <bk-table-column
            type="setting"
            width="40"
        >
            <bk-table-setting-content
                :fields="tableColumn"
                :selected="selectedTableColumn"
                :size="tableSize"
                @setting-change="handleSettingChange" />
        </bk-table-column>
        <template #empty>
            <EmptyTableStatus :type="isSearch ? 'search-empty' : 'empty'" @clear="clearFilter" />
        </template>
    </bk-table>
</template>

<script>
    import EmptyTableStatus from '@/components/empty-table-status.vue'

    const TABLE_COLUMN_CACHE = 'env_ip_list_columns'
    export default {
        components: {
            EmptyTableStatus
        },
        props: {
            list: {
                type: Array,
                default: () => []
            },
            isSearch: Boolean,
            ip: String,
            bkCloudId: Number,
            hostId: Number,
            activeGroupIndex: Number,
            paginationChangeLoading: Boolean
        },
        data () {
            return {
                tableColumn: [],
                selectedTableColumn: [],
                tableSize: 'small',
                page: 1,
                pageSize: 20
            }
        },
        computed: {
            allRenderColumnMap () {
                return this.selectedTableColumn.reduce((result, item) => {
                    result[item.id] = true
                    return result
                }, {})
            }
        },
        watch: {
            // 切换group时重置翻页
            activeGroupIndex () {
                this.page = 1
            }
        },
        created () {
            this.tableColumn = [
                {
                    id: 'ip',
                    label: 'IP',
                    disabled: true
                },
                {
                    id: 'ipv6',
                    label: 'IPv6'
                },
                {
                    id: 'totalTime',
                    label: this.$t('environment.耗时(s)')
                },
                {
                    id: 'bkCloudId',
                    label: this.$t('environment.管控区域')
                },
                {
                    id: 'exitCode',
                    label: this.$t('environment.返回码')
                },
                {
                    id: 'bkAgentId',
                    label: 'Agent ID'
                },
                {
                    id: 'bkHostId',
                    label: 'Host ID'
                }
            ]
            const columnsCache = JSON.parse(localStorage.getItem(TABLE_COLUMN_CACHE))
            if (columnsCache) {
                this.selectedTableColumn = Object.freeze(columnsCache.columns)
                this.tableSize = columnsCache.size
            } else {
                this.selectedTableColumn = Object.freeze([
                    { id: 'ip' },
                    { id: 'ipv6' },
                    { id: 'totalTime' },
                    { id: 'bkCloudId' },
                    { id: 'exitCode' },
                    { id: 'bkAgentId' },
                    { id: 'bkHostId' }
                ])
            }
        },
        methods: {
            handleSettingChange ({ fields, size }) {
                this.selectedTableColumn = Object.freeze(fields)
                this.tableSize = size
                localStorage.setItem(TABLE_COLUMN_CACHE, JSON.stringify({
                    columns: fields,
                    size
                }))
            },

            handleRowClick (row) {
                this.$emit('update:hostId', row.bkHostId)
                this.$emit('update:ip', row.ip)
                this.$emit('update:bkCloudId', row.bkCloudId)
                this.$emit('update:ipStatus', row.result)
            },

            rowClassName ({ row }) {
                return row && row.bkHostId === this.hostId ? 'active' : ''
            },

            handleScrollEnd () {
                this.page += 1
                this.$emit('on-pagination-change', this.page * this.pageSize)
            },

            clearFilter () {
                this.page = 1
                this.$emit('on-clear-filter')
            }
        }
    }
</script>

<style lang="scss">
    .ip-table .bk-table-body tr {
        cursor: pointer;
    }
    .bk-table-row.active,
    .bk-table-row.hover-row>td {
       background-color: #f0f1f5 !important;
    }
    .tippy-tooltip.dot-menu-theme {
        padding: 0;
    }
    .dot-menu-trigger {
        display: block;
        width: 30px;
        height: 30px;
        line-height: 30px;
        border-radius: 50%;
        text-align: center;
        font-size: 0;
        color: #979BA5;
        cursor: pointer;
    }
    .dot-menu-trigger:hover {
        color: #3A84FF;
        background-color: #EBECF0;
    }
    .dot-menu-trigger:before {
        content: "";
        display: inline-block;
        width: 3px;
        height: 3px;
        border-radius: 50%;
        background-color: currentColor;
        box-shadow: 0 -4px 0 currentColor, 0 4px 0 currentColor;
    }
    .dot-menu-list {
        margin: 0;
        padding: 5px 0;
        min-width: 50px;
        list-style: none;
    }
    .dot-menu-list .dot-menu-item {
        padding: 0 10px;
        font-size: 12px;
        line-height: 26px;
        cursor: pointer;
        &:hover {
            background-color: #eaf3ff;
            color: #3a84ff;
        }
    }

    .ip-box{
        position: relative;
        left: 10px;
        &.success,
        &.fail,
        &.running,
        &.waiting {
            &::before {
                position: absolute;
                width: 3px;
                height: 12px;
                margin-right: 1em;
                margin-left: -13px;
                background: #2dc89d;
                content: "";
            }
        }

        &.fail {
            &::before {
                background: #ea3636;
            }
        }

        &.running {
            &::before {
                background: #699df4;
            }
        }

        &.waiting {
            &::before {
                background: #dcdee5;
            }
        }
    }
</style>

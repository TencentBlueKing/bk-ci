<template>
    <bk-table
        :data="data"
        :size="size"
        height="100%"
        :outer-border="false">
        <bk-table-column type="index" label="序列" width="60"></bk-table-column>
        <bk-table-column label="名称/内网IP" prop="ip"></bk-table-column>
        <bk-table-column label="来源" prop="source"></bk-table-column>
        <bk-table-column label="状态" prop="status"></bk-table-column>
        <bk-table-column label="创建时间" prop="create_time"></bk-table-column>
        <bk-table-column type="setting">
            <bk-table-setting-content
                :fields="tableColumn"
                :selected="selectedTableColumn"
                :size="tableSize"
                @setting-change="handleSettingChange" />
        </bk-table-column>
    </bk-table>
</template>
<script>
    const TABLE_COLUMN_CACHE = 'env_ip_list_columns'
    export default {
        data () {
            return {
                tableColumn: [],
                selectedTableColumn: [],
                tableSize: 'small'
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
        created () {
            this.tableColumn = [
                {
                    id: 'id',
                    label: 'ID'
                }
            ]
            const columnsCache = JSON.parse(localStorage.getItem(TABLE_COLUMN_CACHE))
            if (columnsCache) {
                this.selectedTableColumn = Object.freeze(columnsCache.columns)
                this.tableSize = columnsCache.size
            } else {
                this.selectedTableColumn = Object.freeze([
                    { id: 'name' }
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
            }
        }
    }
</script>

<style>
    .dot-menu {
        display: inline-block;
        vertical-align: middle;
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
</style>

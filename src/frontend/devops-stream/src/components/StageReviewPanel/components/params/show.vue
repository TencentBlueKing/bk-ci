<template>
    <section>
        <span class="review-subtitle">{{$t('pipeline.customVars')}}</span>

        <bk-table :data="params">
            <bk-table-column :label="$t('pipeline.chineseName')" show-overflow-tooltip>
                <template slot-scope="props">
                    {{ props.row.chineseName || '--' }}<i v-bk-tooltips="{ content: props.row.desc }" v-if="props.row.desc" class="bk-icon icon-info ml5"></i>
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('name')" prop="key" :formatter="nameFormatter" show-overflow-tooltip></bk-table-column>
            <bk-table-column :label="$t('value')" prop="value" :formatter="valFormatter" show-overflow-tooltip></bk-table-column>
        </bk-table>
    </section>
</template>

<script>
    export default {
        props: {
            params: Array
        },

        methods: {
            nameFormatter (row, column, cellValue, index) {
                return (cellValue || '').replace(/^variables\./, '')
            },

            valFormatter (row, column, cellValue, index) {
                let res = cellValue || '--'
                if (Array.isArray(cellValue)) {
                    res = `[${cellValue.join(', ')}]`
                } else {
                    res = String(cellValue) || '--'
                }
                return res
            }
        }
    }
</script>

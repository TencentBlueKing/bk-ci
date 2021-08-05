<template>
    <section>
        <span class="review-subtitle">自定义参数</span>

        <bk-table :data="params">
            <bk-table-column label="参数名称" prop="key" show-overflow-tooltip></bk-table-column>
            <bk-table-column label="参数值" prop="value" :formatter="valFormatter" show-overflow-tooltip></bk-table-column>
        </bk-table>
    </section>
</template>

<script>
    export default {
        props: {
            params: Array
        },

        methods: {
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

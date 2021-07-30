<template>
    <section>
        <span class="review-subtitle">自定义参数</span>

        <bk-table :data="params">
            <bk-table-column label="参数名称" prop="key"></bk-table-column>
            <bk-table-column label="参数值" prop="value" :formatter="jsonFormatter"></bk-table-column>
        </bk-table>
    </section>
</template>

<script>
    export default {
        props: {
            params: Array
        },

        methods: {
            jsonFormatter (row, column, cellValue, index) {
                return JSON.stringify(cellValue)
            }
        }
    }
</script>

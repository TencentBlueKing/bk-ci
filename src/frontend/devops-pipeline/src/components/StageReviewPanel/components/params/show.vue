<template>
    <section>
        <span class="review-subtitle">{{ $t('stageReview.customVariables') }}</span>

        <bk-table :data="params">
            <bk-table-column :label="$t('stageReview.variableName')" prop="key" show-overflow-tooltip></bk-table-column>
            <bk-table-column :label="$t('stageReview.variableVal')" prop="value" :formatter="valFormatter" show-overflow-tooltip></bk-table-column>
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
                    res = cellValue.length ? `[${cellValue.join(', ')}]` : '--'
                } else {
                    res = String(cellValue) || '--'
                }
                return res
            }
        }
    }
</script>

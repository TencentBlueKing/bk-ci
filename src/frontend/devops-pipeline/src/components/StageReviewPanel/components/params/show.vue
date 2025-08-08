<template>
    <section>
        <span class="review-subtitle">{{ $t('stageReview.customVariables') }}</span>

        <bk-table :data="params">
            <bk-table-column
                :label="$t('stageReview.alias')"
                show-overflow-tooltip
            >
                <template slot-scope="props">
                    {{ props.row.chineseName || '--' }}<i
                        v-bk-tooltips="{ content: props.row.desc }"
                        v-if="props.row.desc"
                        class="bk-icon icon-info ml5"
                    ></i>
                </template>
            </bk-table-column>
            <bk-table-column
                :label="$t('stageReview.variableName')"
                prop="key"
                :formatter="nameFormatter"
                show-overflow-tooltip
            ></bk-table-column>
            <bk-table-column
                :label="$t('stageReview.variableVal')"
                prop="value"
                :formatter="valFormatter"
                show-overflow-tooltip
            ></bk-table-column>
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
            },

            nameFormatter (row, column, cellValue, index) {
                return (cellValue || '').replace(/^variables\./, '')
            }
        }
    }
</script>

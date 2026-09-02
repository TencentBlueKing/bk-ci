<template>
    <section class="render-related-table-main">
        <details open>
            <summary class="category-collapse-trigger">
                <bk-icon
                    type="right-shape"
                    class="icon-angle-right"
                />
                {{ tabTitle }}
                <span v-if="data?.length">
                    ({{ data.length }})
                </span>
            </summary>
            <div
                class="collapse-content"
            >
                <bk-table
                    class="variable-list-table"
                    :data="data"
                    max-height="350"
                >
                    <template v-for="column in columns">
                        <bk-table-column
                            v-if="column.prop === 'referName'"
                            :key="`link-${column.prop}`"
                            v-bind="column"
                        >
                            <template slot-scope="{ row }">
                                <span
                                    class="refer-name-link"
                                    @click="handleToPipeline(row)"
                                >
                                    {{ row.referName }}
                                </span>
                            </template>
                        </bk-table-column>
                        <bk-table-column
                            v-else
                            :key="`text-${column.prop}`"
                            show-overflow-tooltip
                            v-bind="column"
                        />
                    </template>
                </bk-table>
            </div>
        </details>
    </section>
</template>

<script setup>
    import EmptyException from '@/components/common/exception'
    import {
        VARIABLE
    } from '@/store/modules/publicVar/constants'
    const props = defineProps({
        tabTitle: String,
        isShow: Boolean,
        data: Object,
        readOnly: Boolean,
        newParamId: String,
        columns: Array
    })

    function rowClassName ({ row }) {
        if (row.varName === props.newParamId) return 'is-new'
        return ''
    }

    // 跳转到对应的流水线/模板
    function handleToPipeline (row) {
        if (!row?.referUrl) return
        window.open(row.referUrl, '_blank')
    }
</script>

<style lang="scss">
.render-related-table-main {
    .category-collapse-trigger {
        display: flex;
        align-items: center;
        cursor: pointer;
        height: 32px;
        background: #EAEBF0;
        border-radius: 2px 2px 0 0;
        padding: 0 16px;
        font-size: 12px;
        color: #4D4F56;
        .icon-angle-right {
            font-size: 10px;
            margin-right: 5px;
            transform: rotate(90deg);
            color: #979BA5;
        }
    }
    details:not([open]) .collapse-content {
        display: none;
    }

    details:not([open]) .category-collapse-trigger .icon-angle-right {
        transform: rotate(0deg);
    }
    .variable-list-table {
        .refer-name-link {
            color: #3a84ff;
            cursor: pointer;
        }
        .is-new {
            background-color: #f2fff4 !important;
        }
        .bk-table-empty-text {
            padding: 15px 0 !important;
        }
    }
}
</style>
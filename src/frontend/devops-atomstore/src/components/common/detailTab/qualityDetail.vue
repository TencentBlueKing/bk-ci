<template>
    <section class="tab-content">
        <bk-table :data="qualityData" v-bkloading="{ isLoading }" key="qualityTable">
            <bk-table-column :label="$t('store.指标名')" prop="enName" show-overflow-tooltip :width="columnWidth"></bk-table-column>
            <bk-table-column :label="$t('store.指标中文名')" prop="cnName" show-overflow-tooltip :width="columnWidth"></bk-table-column>
            <bk-table-column :label="$t('store.指标说明')" prop="desc" show-overflow-tooltip :width="columnWidth"></bk-table-column>
            <bk-table-column :label="$t('store.值类型')" prop="thresholdType" show-overflow-tooltip :width="columnWidth"></bk-table-column>
            <bk-table-column :label="$t('store.支持的操作')" prop="operationList" show-overflow-tooltip :formatter="operationFormatter" :width="columnWidth"></bk-table-column>
        </bk-table>
    </section>
</template>

<script>
    export default {
        props: {
            qualityData: {
                type: Array,
                default: () => []
            }
        },

        computed: {
            columnWidth () {
                const tabSectionDom = document.getElementsByClassName('bk-tab-section')[0]
                return tabSectionDom.clientWidth / 5
            }
        },

        methods: {
            operationFormatter (row, column, cellValue, index) {
                const opeMap = {
                    GT: '>',
                    GE: '>=',
                    LT: '<',
                    LE: '<=',
                    EQ: '='
                }
                return (cellValue || []).reduce((acc, cur) => {
                    acc += (opeMap[cur] + ' ')
                    return acc
                }, '')
            }
        }
    }
</script>

<style lang="scss" scoped>
    .tab-content {
        margin-top: 20px;
        .explain {
            color: #828282;
        }
    }
    .yaml-title {
        margin: 20px 0 10px;
        line-height: 23px;
        height: 23px;
        color: #222222;
        font-size: 14px;
        font-weight: 500;
    }
</style>

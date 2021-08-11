<template>
    <section>
        <h3 class="yaml-title">{{ $t('store.配置片段：') }}</h3>
        <codeSection v-bind="$props"></codeSection>

        <h3 class="yaml-title">{{ $t('store.输出参数：') }}</h3>
        <bk-table :data="data">
            <bk-table-column :label="$t('store.参数名')" prop="source" show-overflow-tooltip></bk-table-column>
            <bk-table-column :label="$t('store.参数说明')" prop="status" show-overflow-tooltip></bk-table-column>
        </bk-table>

        <h3 class="yaml-title">{{ $t('store.质量红线指标：') }}</h3>
        <bk-table :data="data">
            <bk-table-column :label="$t('store.指标名')" prop="source" show-overflow-tooltip></bk-table-column>
            <bk-table-column :label="$t('store.参数说明')" prop="status" show-overflow-tooltip></bk-table-column>
            <bk-table-column :label="$t('store.值类型')" prop="source" show-overflow-tooltip></bk-table-column>
            <bk-table-column :label="$t('store.支持的操作')" prop="status" show-overflow-tooltip></bk-table-column>
        </bk-table>
    </section>
</template>

<script>
    import codeSection from './codeSection'

    export default {
        components: {
            codeSection
        },

        props: {
            readOnly: {
                type: Boolean,
                default: true
            },
            limitHeight: {
                type: Boolean,
                default: true
            },
            cursorBlinkRate: {
                type: Number,
                default: 0
            },
            code: {
                type: String,
                require: true
            }
        },

        data () {
            return {
                data: []
            }
        }
    }
</script>

<style lang="scss" scoped>
    .yaml-title {
        margin: 20px 0 10px;
        line-height: 23px;
        height: 23px;
        color: #222222;
        font-size: 14px;
        font-weight: 500;
    }
</style>

<template>
    <bk-exception :type="type" scene="part">
        <div style="font-size: 14px;">{{ typeMap[type] }}</div>
        <template v-if="type === 'search-empty'">
            <div class="empty-tips">
                {{ $t('store.可以尝试 调整关键词 或') }}
                <button class="bk-text-button" @click="handleClear">{{$t('store.清空筛选条件')}}</button>
            </div>
        </template>
    </bk-exception>
</template>

<script>
    export default {
        name: 'EmptyTableStatus',
        props: {
            type: {
                type: String,
                default: 'empty'
            }
        },
        data () {
            return {
                typeMap: {
                    empty: this.$t('store.暂无数据'),
                    'search-empty': this.$t('store.搜索结果为空')
                }
            }
        },
        methods: {
            handleClear () {
                this.$emit('clear')
            }
        }
    }
</script>

<style lang="scss">
    .empty-tips {
        margin-top: 8px;
        font-size: 12px;
        color: #979BA5;
    }
</style>

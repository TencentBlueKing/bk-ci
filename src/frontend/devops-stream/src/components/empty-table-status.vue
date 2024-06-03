<template>
    <bk-exception :type="type" scene="part">
        <div style="font-size: 14px;">{{ typeMap[type] }}</div>
        <template v-if="type === 'search-empty'">
            <i18n
                tag="div"
                path="可以尝试 调整关键词 或 清空筛选条件"
                class="empty-tips">
                <button class="bk-text-button" @click="handleClear">{{$t('清空筛选条件')}}</button>
            </i18n>
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
                    empty: this.$t('noData'),
                    'search-empty': this.$t('searchEmpty')
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

<style lang="postcss">
    .empty-tips {
        margin-top: 8px;
        font-size: 12px;
        color: #979BA5;
    }
</style>

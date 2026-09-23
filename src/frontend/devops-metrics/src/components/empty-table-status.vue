<template>
    <bk-exception :type="type" scene="part">
        <div style="font-size: 14px;">{{ typeMap[type] }}</div>
        <template v-if="type === 'search-empty'">
            <i18n-t 
                tag="div"
                keypath="可以尝试 调整关键词 或 清空筛选条件"
                class="empty-tips">
                <button class="bk-text-button" @click="handleClear">{{$t('清空筛选条件')}}</button>
            </i18n-t>
        </template>
    </bk-exception>
</template>

<script>
    import { defineComponent } from 'vue';
    import { useI18n } from 'vue-i18n';


    export default defineComponent({
        name: 'EmptyTableStatus',
        props: {
            type: {
                type: String,
                default: 'empty'
            }
        },
        setup() {
            const { t } = useI18n();
            const typeMap = {
                empty: t('noData'),
                'search-empty': t('searchEmpty')
            };
            const handleClear = () => {
                this.$emit('clear');
            };
            return { typeMap, handleClear };
        }
    });
    
</script>

<style lang="postcss">
    .empty-tips {
        margin-top: 8px;
        font-size: 12px;
        color: #979BA5;
    }
    .bk-text-button {
        color: #3a84ff;
    }
</style>

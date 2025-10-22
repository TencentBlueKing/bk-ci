<template>
    <bk-exception
        :type="type"
        scene="part"
    >
        <div style="font-size: 14px;">{{ typeMap[type] }}</div>
        <template v-if="type === 'search-empty'">
            <div class="empty-tips">
                {{ $t('environment.tryAdjustingKeywordsOr') }}
                <button
                    class="bk-text-button"
                    @click="handleClear"
                >
                    {{ $t('environment.clearFilterCriteria') }}
                </button>
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
                    empty: this.$t('environment.noData'),
                    'search-empty': this.$t('environment.searchResultsEmpty')
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

<style lang="scss" scoped>
    ::v-deep .bk-exception-img.part-img {
        margin-top: 15%;
    }
    .empty-tips {
        margin-top: 8px;
        font-size: 12px;
        color: #979BA5;
    }
</style>

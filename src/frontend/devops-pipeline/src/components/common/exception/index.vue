<template>
    <bk-exception :type="type" :scene="scene">
        <div style="font-size: 14px;">{{ typeMap[type] }}</div>
        <template v-if="type === 'search-empty'">
            <i18n
                tag="div"
                path="clearFilterConditionTips"
                class="empty-tips"
            >
                <bk-button text @click="handleClear">
                    {{$t('clearFilterCondition')}}
                </bk-button>
            </i18n>
        </template>
    </bk-exception>
</template>

<script>
    export default {
        name: 'EmptyException',
        props: {
            scene: {
                type: String,
                default: 'part'
            },
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

<style lang="scss">
    .empty-tips {
        margin-top: 8px;
        font-size: 12px;
        color: #979BA5;
    }
</style>

<template>
    <section>
        <h3 class="yaml-title">{{ $t('store.配置片段：') }}</h3>
        <codeSection v-bind="$props"></codeSection>

        <!-- <h3 class="yaml-title">{{ $t('store.输出参数：') }}</h3>
        <bk-table :data="data">
            <bk-table-column :label="$t('store.参数名')" prop="source" show-overflow-tooltip></bk-table-column>
            <bk-table-column :label="$t('store.参数说明')" prop="status" show-overflow-tooltip></bk-table-column>
        </bk-table> -->

        <h3 class="yaml-title">{{ $t('store.质量红线指标：') }}</h3>
        <bk-table :data="qualityData" v-bkloading="{ isLoading }">
            <bk-table-column :label="$t('store.指标名')" prop="enName" show-overflow-tooltip></bk-table-column>
            <bk-table-column :label="$t('store.参数说明')" prop="desc" show-overflow-tooltip></bk-table-column>
            <bk-table-column :label="$t('store.值类型')" prop="thresholdType" show-overflow-tooltip></bk-table-column>
            <bk-table-column :label="$t('store.支持的操作')" prop="operationList" show-overflow-tooltip :formatter="operationFormatter"></bk-table-column>
        </bk-table>
    </section>
</template>

<script>
    import codeSection from './codeSection'
    import api from '@/api'
    import { mapActions } from 'vuex'

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
            },
            name: String,
            currentTab: String,
            qualityData: Array,
            getDataFunc: {
                type: Function,
                default: null
            }
        },

        data () {
            return {
                isLoading: false
            }
        },

        watch: {
            currentTab: {
                handler (currentVal) {
                    if (currentVal && currentVal === this.name && !this.qualityData) {
                        this.initQualityData()
                    }
                },
                immediate: true
            }
        },

        methods: {
            ...mapActions('store', [
                'setDetail'
            ]),
            
            initQualityData () {
                const code = this.$route.params.code
                this.isLoading = true
                return api.requestAtomQuality(code).then((res) => {
                    this.qualityData = res
                    this.$emit('update:qualityData', res)
                    this.setDetail({ qualityData: res })
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isLoading = false
                })
            },

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
    .yaml-title {
        margin: 20px 0 10px;
        line-height: 23px;
        height: 23px;
        color: #222222;
        font-size: 14px;
        font-weight: 500;
    }
</style>

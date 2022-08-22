<template>
    <section>
        <div class="explain">{{ $t('store.说明：') }}<span class="explain-info">{{ explainInfo }}</span></div>
        <bk-table :data="outputData" v-bkloading="{ isLoading: isLoading }" :key="outputData">
            <bk-table-column :label="$t('store.参数名')" prop="name" show-overflow-tooltip></bk-table-column>
            <bk-table-column :label="$t('store.参数说明')" prop="desc" show-overflow-tooltip></bk-table-column>
        </bk-table>
    </section>
</template>

<script>
    import api from '@/api'
    import { mapActions } from 'vuex'
    
    export default {
        props: {
            name: String,
            currentTab: String,
            outputData: Array,
            classifyCode: {
                type: String,
                default: ''
            }
        },
        data () {
            return {
                isLoading: false
            }
        },

        computed: {
            explainInfo () {
                if (this.classifyCode === 'trigger') {
                    return this.$t('store.触发器说明文案')
                }
                return this.$t('store.说明文案')
            }
        },

        watch: {
            currentTab: {
                handler (currentVal) {
                    if (currentVal && currentVal === this.name && !this.outputData) {
                        this.initOutputData()
                    }
                },
                immediate: true
            }
        },

        methods: {
            ...mapActions('store', [
                'setDetail'
            ]),
            
            initOutputData () {
                const code = this.$route.params.code
                this.isLoading = true
                return api.requestAtomOutputList(code).then((res) => {
                    this.outputData = res
                    this.$emit('update:outputData', res)
                    this.setDetail({ outputData: res })
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isLoading = false
                })
            }
        }
    }
</script>

<style lang="scss" scoped>
    .explain {
        margin: 20px 0 10px;
        color: #707070;
        font-weight: 700;
        font-size: 12px;
        .explain-info {
            font-weight: 100;
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

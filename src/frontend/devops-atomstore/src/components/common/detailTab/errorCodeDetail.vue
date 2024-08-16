<template>
    <section>
        <bk-table :data="errorCodeData" v-bkloading="{ isLoading: isLoading }" class="mt20">
            <bk-table-column :label="$t('store.错误码')" prop="errorCode" show-overflow-tooltip></bk-table-column>
            <bk-table-column :label="$t('store.错误码说明')" prop="errorMsgZhCn" show-overflow-tooltip>
                <template slot-scope="{ row }">
                    <div v-html="row.errorMsgZhCn.replace(/[\n|\r]/gm, '<br>')" style="display: inline-block; padding: 10px 0;"></div>
                </template>
            </bk-table-column>
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
            errorCodeData: Array
        },
        data () {
            return {
                isLoading: false
            }
        },
        watch: {
            currentTab: {
                handler (currentVal) {
                    if (currentVal && currentVal === this.name && !this.errorCodeData) {
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
                return api.requestAtomErrorCode('ATOM', code).then((res) => {
                    this.errorCodeData = res.errorCodeInfos
                    this.$emit('update:errorCodeData', res.errorCodeInfos)
                    this.setDetail({ errorCodeData: res.errorCodeInfos })
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
</style>

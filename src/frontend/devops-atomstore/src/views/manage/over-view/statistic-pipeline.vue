<template>
    <article class="manage-statistic-pipeline">
        <main class="g-scroll-pagination-table">
            <bk-form form-type="inline" class="statistic-pipeline-head">
                <bk-form-item :label="$t('store.修改时间')">
                    <bk-date-picker :placeholder="$t('store.请选择起止时间')" type="datetimerange" v-model="searchData.modifyTimeRange" :clearable="false"></bk-date-picker>
                </bk-form-item>
                <bk-form-item :label="$t('store.版本')">
                    <bk-input :placeholder="$t('store.请输入版本')" v-model="searchData.version"></bk-input>
                </bk-form-item>
                <bk-form-item>
                    <bk-button theme="primary" @click="handleSearch" :loading="isLoading">{{ $t('store.搜索') }}</bk-button>
                </bk-form-item>
                <bk-form-item>
                    <bk-button @click="savePipelines" :loading="isSaving">{{ $t('store.导出') }}</bk-button>
                </bk-form-item>
            </bk-form>

            <bk-table :data="pipelineList"
                :outer-border="false"
                :header-border="false"
                :header-cell-style="{ background: '#fff' }"
                :pagination="pagination"
                @page-change="pageChanged"
                @page-limit-change="pageLimitChanged"
                v-bkloading="{ isLoading }"
            >
                <bk-table-column :label="$t('store.流水线链接')">
                    <template slot-scope="props">
                        <a :href="props.row.pipelineUrl" target="_blank" class="text-link">{{ props.row.pipelineUrl }}</a>
                    </template>
                </bk-table-column>
                <bk-table-column :label="$t('store.版本')" prop="atomVersion"></bk-table-column>
                <bk-table-column :label="$t('store.最近修改人')" prop="modifier"></bk-table-column>
                <bk-table-column :label="$t('store.最近修改时间')" prop="updateTime"></bk-table-column>
                <bk-table-column :label="$t('store.最近执行人')" prop="executor"></bk-table-column>
                <bk-table-column :label="$t('store.最近执行时间')" prop="executeTime"></bk-table-column>
            </bk-table>
        </main>
    </article>
</template>

<script>
    import moment from 'moment'
    import api from '@/api'

    function formatterTime (val) {
        return moment(val).format('YYYY-MM-DD HH:mm:ss')
    }

    export default {
        data () {
            const startTime = formatterTime(moment().subtract(7, 'days'))
            const endTime = formatterTime(moment())

            return {
                searchData: {
                    version: '',
                    modifyTimeRange: [startTime, endTime]
                },
                pagination: {
                    current: 1,
                    count: 1,
                    limit: 10
                },
                pipelineList: [],
                isLoading: false,
                isSaving: false
            }
        },

        computed: {
            queryData () {
                return {
                    version: this.searchData.version,
                    startUpdateTime: formatterTime(this.searchData.modifyTimeRange[0]),
                    endUpdateTime: formatterTime(this.searchData.modifyTimeRange[1])
                }
            }
        },

        created () {
            this.getPipelineList()
        },

        methods: {
            getPipelineList () {
                const params = {
                    ...this.queryData,
                    page: this.pagination.current,
                    pageSize: this.pagination.limit
                }
                this.isLoading = true
                api.requestStatisticPipeline(this.$route.params.code, params).then((res) => {
                    this.pipelineList = res.records || []
                    this.pagination.count = res.count
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isLoading = false
                })
            },

            handleSearch () {
                this.pagination.current = 1
                this.getPipelineList()
            },

            pageLimitChanged (currentLimit, prevLimit) {
                if (currentLimit === this.pagination.limit) return

                this.pagination.current = 1
                this.pagination.limit = currentLimit
                this.getPipelineList()
            },

            pageChanged (page) {
                if (page) this.pagination.current = page
                this.getPipelineList()
            },

            savePipelines () {
                this.isSaving = true
                api.requestSavePipelinesAsCsv(this.$route.params.code, this.queryData).then((res) => {
                    if (res.status >= 200 && res.status < 300) {
                        return res.blob()
                    } else {
                        return res.json().then((result) => Promise.reject(result))
                    }
                }).then((blob) => {
                    const a = document.createElement('a')
                    const url = window.URL || window.webkitURL || window.moxURL
                    a.href = url.createObjectURL(blob)
                    a.download = `${this.$route.params.code}.csv`
                    document.body.appendChild(a)
                    a.click()
                    document.body.removeChild(a)
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isSaving = false
                })
            }
        }
    }
</script>

<style lang="scss" scoped>
    .manage-statistic-pipeline {
        background: #fff;
        padding: 3.2vh;
    }
    .statistic-pipeline-head {
        margin-bottom: 3.2vh;
        ::v-deep .bk-form-item {
            margin-left: 20px;
        }
    }
</style>

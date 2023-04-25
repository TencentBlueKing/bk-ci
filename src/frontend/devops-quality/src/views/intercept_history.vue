<template>
    <div class="intercept-history-wrapper">
        <div class="inner-header">
            <div class="title">{{$t('quality.红线记录')}}</div>
        </div>

        <section class="sub-view-port"
            v-bkloading="{
                isLoading: loading.isLoading,
                title: loading.title
            }">
            <div class="intercept-history-content" v-if="showContent">
                <div class="data-list-query">
                    <div class="search-item">
                        <div class="search-title">{{$t('quality.状态：')}}</div>
                        <bk-select v-model="searchInfo.interceptResult">
                            <bk-option v-for="(option, index) in statusList"
                                :key="index"
                                :id="option.value"
                                :name="option.label">
                            </bk-option>
                        </bk-select>
                    </div>
                    <div class="search-item search-by-pipeline">
                        <div class="search-title">{{$t('quality.流水线：')}}</div>
                        <bk-select v-model="searchInfo.pipelineId" :searchable="true">
                            <bk-option v-for="(option, index) in pipelineList"
                                :key="index"
                                :id="option.pipelineId"
                                :name="option.pipelineName">
                            </bk-option>
                        </bk-select>
                    </div>
                    <div class="search-item search-by-rule">
                        <div class="search-title">{{$t('quality.规则：')}}</div>
                        <bk-select v-model="searchInfo.ruleHashId" :searchable="true">
                            <bk-option v-for="(option, index) in ruleList"
                                :key="index"
                                :id="option.ruleHashId"
                                :name="option.name">
                            </bk-option>
                        </bk-select>
                    </div>
                    <div class="search-item search-by-date">
                        <div class="search-title">{{$t('quality.时间：')}}</div>
                        <bk-date-picker
                            :placement="'bottom-end'"
                            :placeholder="$t('quality.起止时间')"
                            :type="'daterange'"
                            @change="changeDate"
                        ></bk-date-picker>
                    </div>
                    <div class="search-button">
                        <bk-button theme="primary" @click="query">{{$t('quality.查询')}}</bk-button>
                    </div>
                </div>
                <div class="intercept-table-wrapper" v-if="showContent">
                    <bk-table
                        size="small"
                        class="intercept-history-table"
                        :data="interceptList"
                        :empty-text="$t('quality.暂时没有拦截记录')"
                        :pagination="pagination"
                        @page-change="handlePageChange"
                        @page-limit-change="limitChange">
                        <bk-table-column :label="$t('quality.序号')" prop="num" width="80">
                            <template slot-scope="props">
                                <span>{{props.row.num}}</span>
                            </template>
                        </bk-table-column>
                        <bk-table-column :label="$t('quality.关联流水线')" prop="pipelineName">
                            <template slot-scope="props">
                                <span v-if="props.row.pipelineIsDelete" class="disabled-pipeline" :title="props.row.pipelineName">{{props.row.pipelineName}}</span>
                                <a class="item-pipelinename" v-else :title="props.row.pipelineName"
                                    target="_blank"
                                    :href="`/console/pipeline/${projectId}/${props.row.pipelineId}/detail/${props.row.buildId}`">{{props.row.pipelineName}}
                                </a>
                            </template>
                        </bk-table-column>
                        <bk-table-column :label="$t('quality.规则名称')" prop="ruleName">
                            <template slot-scope="props">
                                <span>{{props.row.ruleName}}</span>
                            </template>
                        </bk-table-column>
                        <bk-table-column :label="$t('quality.状态')" prop="interceptResult" width="100">
                            <template slot-scope="props">
                                <span v-if="props.row.interceptResult === 'PASS'" style="color: #30D878;">{{$t('quality.已通过')}}</span>
                                <span v-if="props.row.interceptResult === 'FAIL'" style="color: #FFB400;">{{$t('quality.已拦截')}}</span>
                            </template>
                        </bk-table-column>
                        <bk-table-column :label="$t('quality.内容')" prop="remark" min-width="200">
                            <template slot-scope="props">
                                <bk-popover placement="top-start" :delay="500">
                                    <p class="item-remark">{{props.row.remark}}</p>
                                    <template slot="content">
                                        <p style="max-width: 300px; text-align: left; white-space: normal;word-break: break-all;font-weight: 400;">{{props.row.remark}}</p>
                                    </template>
                                </bk-popover>
                            </template>
                        </bk-table-column>
                        <bk-table-column :label="$t('quality.拦截时间')" prop="timestamp">
                            <template slot-scope="props">
                                {{localConvertTime(props.row.timestamp)}}
                            </template>
                        </bk-table-column>
                    </bk-table>
                </div>
            </div>
        </section>
    </div>
</template>

<script>
    import { convertTime } from '@/utils/util'

    export default {
        data () {
            return {
                showContent: false,
                statusList: [
                    { label: this.$t('quality.全部'), value: 'ALL' },
                    { label: this.$t('quality.已拦截'), value: 'FAIL' },
                    { label: this.$t('quality.已通过'), value: 'PASS' }
                ],
                pipelineList: [],
                ruleList: [],
                interceptList: [],
                loading: {
                    isLoading: false,
                    title: ''
                },
                searchInfo: {
                    interceptResult: '',
                    pipelineId: '',
                    ruleHashId: '',
                    startTime: '',
                    endTime: ''
                },
                pagination: {
                    current: 1,
                    count: 0,
                    limit: 10
                }
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            }
        },
        watch: {
            projectId () {
                this.$router.push({
                    name: 'qualityOverview',
                    params: {
                        projectId: this.projectId
                    }
                })
            }
        },
        created () {
            this.initData()
        },
        async mounted () {
            await this.init()
        },
        methods: {
            async init () {
                const {
                    loading,
                    pagination
                } = this

                loading.isLoading = true
                loading.title = this.$t('quality.数据加载中，请稍候')

                try {
                    await this.requestList(pagination.current, pagination.limit)
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                } finally {
                    setTimeout(() => {
                        this.loading.isLoading = false
                        this.showContent = true
                    }, 100)
                }
            },
            async query () {
                this.pagination.current = 1
                this.loading.isLoading = true
                await this.requestList(this.pagination.current, this.pagination.limit)
                setTimeout(() => {
                    this.loading.isLoading = false
                }, 100)
            },
            async requestList (page, pageSize) {
                const params = this.getParams()

                try {
                    const res = await this.$store.dispatch('quality/requestInterceptList', {
                        projectId: this.projectId,
                        page,
                        pageSize,
                        params
                    })
                    
                    this.interceptList.splice(0, this.interceptList.length)
                    if (res.records) {
                        res.records.forEach(item => {
                            this.interceptList.push(item)
                        })
                        this.pagination.count = res.count
                    }
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            async initData () {
                if (this.$route.hash === '#INTERCEPT') {
                    this.searchInfo.interceptResult = 'FAIL'
                }
                await this.requestPipelineList()
                await this.requestRuleList()
            },
            getParams () {
                const params = {}

                params.interceptResult = !this.searchInfo.interceptResult ? undefined : this.searchInfo.interceptResult === 'ALL' ? undefined : this.searchInfo.interceptResult
                params.ruleHashId = !this.searchInfo.ruleHashId ? undefined : this.searchInfo.ruleHashId === 'all' ? undefined : this.searchInfo.ruleHashId
                params.pipelineId = !this.searchInfo.pipelineId ? undefined : this.searchInfo.pipelineId === 'all' ? undefined : this.searchInfo.pipelineId
                params.startTime = this.searchInfo.startTime || undefined
                params.endTime = this.searchInfo.endTime || undefined

                return params
            },
            async requestPipelineList () {
                try {
                    const res = await this.$store.dispatch('quality/requestPipelineList', {
                        projectId: this.projectId
                    })

                    if (res) {
                        this.pipelineList.splice(0, this.pipelineList.length)
                        res.forEach(item => {
                            this.pipelineList.push(item)
                        })
                        this.pipelineList.unshift({ pipelineId: 'all', pipelineName: this.$t('quality.全部') })
                    }
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            async requestRuleList () {
                try {
                    const res = await this.$store.dispatch('quality/requestRuleList', {
                        projectId: this.projectId,
                        page: 1,
                        pageSize: 10000
                    })
                    
                    this.ruleList.splice(0, this.ruleList.length)
                    if (res.records) {
                        res.records.forEach(item => {
                            this.ruleList.push(item)
                        })
                        this.ruleList.unshift({ ruleHashId: 'all', name: this.$t('quality.全部') })
                    }
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            async handlePageChange (page) {
                this.pagination.current = page
                this.loading.isLoading = true
                await this.requestList(this.pagination.current, this.pagination.limit)
                this.loading.isLoading = false
            },
            async limitChange (limit) {
                this.pagination.limit = limit
                this.pagination.current = 1
                this.loading.isLoading = true
                await this.requestList(this.pagination.current, this.pagination.limit)
                this.loading.isLoading = false
            },
            changeDate (newValue) {
                const start = newValue[0]
                const end = newValue[1]
                this.searchInfo.startTime = Date.parse(new Date(start)) / 1000
                this.searchInfo.endTime = Date.parse(new Date(end)) / 1000
            },
            /**
             * 处理时间格式
             */
            localConvertTime (timestamp) {
                return convertTime(timestamp * 1000)
            }
        }
    }
</script>

<style lang="scss">
    @import '@/scss/conf.scss';
    
    .intercept-history-wrapper {
        overflow: auto;
        .inner-header {
            display: flex;
            justify-content: space-between;
            padding: 18px 20px;
            width: 100%;
            height: 60px;
            border-bottom: 1px solid $borderWeightColor;
            background-color: #fff;
            box-shadow:0px 2px 5px 0px rgba(51,60,72,0.03);
            .title {
                font-size: 16px;
            }
        }
        .sub-view-port {
            overflow: hidden;
        }
        .intercept-history-content {
            height: 100%;
            padding: 20px;
            overflow: auto;
            .data-list-query {
                display: flex;
            }
            .search-item {
                float: left;
                margin-right: 20px;
                width: 200px;
                .search-title {
                    margin-bottom: 6px;
                }
                .bk-selector-wrapper input {
                    padding-right: 20px;
                    text-overflow: ellipsis;
                    color: $fontWeightColor;
                }
            }
            .search-by-pipeline,
            .search-by-rule {
                width: 200px;
            }
            .search-by-date {
                width: 380px;
                margin-right: 10px;
                .bk-date-picker {
                    width: 374px;
                }
                .bk-date-range {
                    width: 100%;
                    input {
                        color: $fontWeightColor;
                    }
                }
            }
            .search-button {
                padding-top: 25px;
                width: 70px;
            }
            .intercept-history-table {
                margin-top: 20px;
                .no-data {
                    height: 160px;
                    text-align: center;
                    color: $fontWeightColor;
                }
                .item-intercept-id {
                    max-width: 160px;
                    min-width: 100px;
                }
                .item-rulename,
                .item-pipelinename,
                .item-remark {
                    display: -webkit-box;
                    -webkit-line-clamp: 1;
                    -webkit-box-orient: vertical;
                    word-break: break-all;
                    overflow: hidden;
                    max-height: 42px;
                    line-height: 1.5;
                }
                .disabled-pipeline {
                    color: #c4c6cc;
                }
                .item-pipelinename {
                    color: $primaryColor;
                    cursor: pointer;
                }
            }
        }
        @media screen and (max-width: 1400px) {
            .intercept-history-content {
                .search-item {
                    margin-right: 12px;
                }
                .search-by-pipeline,
                .search-by-rule {
                    width: 180px;
                }
                .search-by-date {
                    width: 220px;
                    .bk-date-picker {
                        width: 220px;
                    }
                }
                .item-timestamp {
                    width: 160px;
                }
            }
        }
    }
</style>

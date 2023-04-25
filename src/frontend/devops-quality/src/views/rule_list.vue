<template>
    <div class="rule-list-wrapper">
        <div class="rule-list-header">
            <div class="title">{{$t('quality.红线规则')}}</div>
        </div>

        <section class="sub-view-port"
            v-bkloading="{
                isLoading: loading.isLoading,
                title: loading.title
            }">
            <div class="rule-main-wrapper" v-if="showContent && ruleList.length">
                <div class="rule-main-container">
                    <div class="rule-main-header">
                        <bk-button theme="primary" @click="toCreateRule">
                            <i class="devops-icon icon-plus"></i>
                            <span style="margin-left: 0;">{{$t('quality.创建规则')}}</span>
                        </bk-button>
                    </div>
                    <div class="rule-table-wrapper">
                        <bk-table
                            size="small"
                            class="rule-list-table"
                            :data="ruleList"
                            :pagination="pagination"
                            @page-change="handlePageChange"
                            @page-limit-change="limitChange">
                            <bk-table-column :label="$t('quality.名称')" prop="name">
                                <template slot-scope="props">
                                    <p class="rule-name" :title="props.row.name" @click="toShowSlider(props.row.ruleHashId, 'detail')">{{props.row.name}}</p>
                                </template>
                            </bk-table-column>
                            <bk-table-column :label="$t('quality.指标')" prop="indicatorList" min-width="160" class-name="indicator-item">
                                <template slot-scope="props">
                                    <div class="rule-detail" :title="getIndicatorDesc(props.row.indicatorList)">
                                        <span v-for="(entry, key) in props.row.indicatorList" :key="key">
                                            <span>{{entry.cnName}}</span>
                                            <span>{{indexHandlerConf[entry.operation]}}</span>
                                            <span>{{entry.threshold}}</span>
                                            <br>
                                        </span>
                                    </div>
                                </template>
                            </bk-table-column>
                            <bk-table-column :label="$t('quality.控制点')" prop="controlPoint" min-width="110" class-name="controlPoint-item">
                                <template slot-scope="props">
                                    <span :title="props.row.controlPoint.cnName">{{props.row.controlPoint.cnName}}</span>
                                </template>
                            </bk-table-column>
                            <bk-table-column :label="$t('quality.生效范围')" prop="rangeSummary">
                                <template slot-scope="props">
                                    <span class="canShowPipeline" @click="toShowRange(props.row)">{{props.row.rangeSummary.length}}</span>
                                </template>
                            </bk-table-column>
                            <bk-table-column :label="$t('quality.生效流水线')" prop="pipelineCount">
                                <template slot-scope="props">
                                    <span class="canShowPipeline" @click="toShowPipeline(props.row)">{{props.row.pipelineCount}}</span>
                                </template>
                            </bk-table-column>
                            <bk-table-column :label="$t('quality.生效流水线执行数')" prop="pipelineExecuteCount" min-width="110">
                                <template slot-scope="props">
                                    <span>{{props.row.pipelineExecuteCount}}</span>
                                </template>
                            </bk-table-column>
                            <bk-table-column :label="$t('quality.拦截次数')" prop="interceptTimes">
                                <template slot-scope="props">
                                    <span @click="toShowSlider(props.row.ruleHashId, 'record')">{{props.row.interceptTimes}}</span>
                                </template>
                            </bk-table-column>
                            <bk-table-column :label="$t('quality.操作')" min-width="120">
                                <template slot-scope="props">
                                    <div class="handler-btn">
                                        <span class="copy-btn" :data-clipboard-text="props.row.rule" @click="editRule(props.row)">{{$t('quality.编辑')}}</span>
                                        <span class="switch-btn" v-if="props.row.enable" @click="switchRule(props.row)">{{$t('quality.停用')}}</span>
                                        <span class="switch-btn" v-else @click="switchRule(props.row)">{{$t('quality.启用')}}</span>
                                        <span class="delete-btn" @click="toDeleteRule(props.row)">{{$t('quality.删除')}}</span>
                                    </div>
                                </template>
                            </bk-table-column>
                        </bk-table>
                    </div>
                </div>
            </div>
            <image-empty v-if="showContent && !ruleList.length"
                :title="emptyInfo.title"
                :desc="emptyInfo.desc"
                :btns="emptyInfo.btns">
            </image-empty>

            <bk-sideslider
                class="rule-side-slider"
                :is-show.sync="sideSliderConfig.show"
                :title="ruleDetail.name"
                :quick-close="sideSliderConfig.quickClose"
                :width="sideSliderConfig.width"
                @hidden="closrSlider">
                <template slot="content">
                    <div class="rule-slider-info"
                        v-if="sideSliderConfig.show && ruleDetail"
                        v-bkloading="{
                            isLoading: sideSliderConfig.isLoading
                        }">
                        <div class="slider-main">
                            <bk-tab :active="curActiveTab">
                                <bk-tab-panel
                                    v-for="(panel, index) in panels"
                                    v-bind="panel"
                                    :key="index">
                                    <section v-if="panel.name === 'detailInfo'">
                                        <table class="detail-info">
                                            <tr>
                                                <td class="item-label">{{$t('quality.最新状态：')}}</td>
                                                <td class="item-value">
                                                    <span v-if="ruleDetail.interceptRecent">{{ruleDetail.interceptRecent}}</span>
                                                    <span v-else>--</span>
                                                </td>
                                            </tr>
                                            <tr><td class="item-label">{{$t('quality.描述：')}}</td><td class="item-value">{{ruleDetail.desc}}</td></tr>
                                            <tr>
                                                <td class="item-label threshold-label">{{$t('quality.指标：')}}</td>
                                                <td class="item-value threshold-item">
                                                    <bk-table
                                                        size="small"
                                                        class="detail-table"
                                                        :data="curThresholdList">
                                                        <bk-table-column :label="$t('quality.指标名称')" prop="cnName" min-width="200">
                                                            <template slot-scope="props">
                                                                <span :title="props.row.cnName">{{props.row.cnName}}</span>
                                                            </template>
                                                        </bk-table-column>
                                                        <bk-table-column :label="$t('quality.操作')" prop="operation">
                                                            <template slot-scope="props">
                                                                <span>{{indexHandlerConf[props.row.operation]}}</span>
                                                            </template>
                                                        </bk-table-column>
                                                        <bk-table-column :label="$t('quality.阈值')" prop="threshold">
                                                            <template slot-scope="props">
                                                                <span>{{props.row.threshold}}</span>
                                                            </template>
                                                        </bk-table-column>
                                                    </bk-table>
                                                </td>
                                            </tr>
                                            <tr>
                                                <td class="item-label">{{$t('quality.控制点：')}}</td>
                                                <td class="item-value">
                                                    <span v-if="ruleDetail.controlPoint">{{ruleDetail.controlPoint.cnName}}</span>
                                                </td>
                                            </tr>
                                            <tr>
                                                <td class="item-label">{{$t('quality.生效流水线：')}}</td>
                                                <td class="item-value">
                                                    <span v-if="ruleDetail.range">{{ruleDetail.pipelineCount}}{{$t('quality.条流水线')}}</span>
                                                </td>
                                            </tr>
                                            <tr>
                                                <td class="item-label">{{$t('quality.操作：')}}</td>
                                                <td class="item-value">
                                                    <span v-if="ruleDetail.operation === 'END'">{{$t('quality.终止后通知')}}</span>
                                                    <span v-if="ruleDetail.operation === 'AUDIT'">{{$t('quality.人工审核')}}</span>
                                                </td>
                                            </tr>
                                        </table>
                                    </section>
                                    <section v-if="panel.name === 'recordDate'">
                                        <bk-table v-if="recordList.length"
                                            size="small"
                                            class="record-table"
                                            :data="recordList"
                                            :pagination="sliderPagination"
                                            @page-change="handleSliderPageChange"
                                            @page-limit-change="sliderLimitChange">
                                            <bk-table-column :label="$t('quality.关联流水线')" prop="pipelineName">
                                                <template slot-scope="props">
                                                    <a :title="`${props.row.pipelineName}(#${props.row.buildNo})`" target="_blank" class="source-item"
                                                        :href="`/console/pipeline/${projectId}/${props.row.pipelineId}/detail/${props.row.buildId}`">{{props.row.pipelineName}}
                                                        <span>(#{{props.row.buildNo}})</span>
                                                    </a>
                                                </template>
                                            </bk-table-column>
                                            <bk-table-column :label="$t('quality.状态')" prop="interceptResult" width="80">
                                                <template slot-scope="props">
                                                    <span v-if="props.row.interceptResult === 'PASS'" style="color: #30D878;">{{$t('quality.已通过')}}</span>
                                                    <span v-if="props.row.interceptResult === 'FAIL'" style="color: #FFB400;">{{$t('quality.已拦截')}}</span>
                                                </template>
                                            </bk-table-column>
                                            <bk-table-column :label="$t('quality.内容')" prop="remark" width="360">
                                                <template slot-scope="props">
                                                    <span :title="props.row.remark">{{props.row.remark}}</span>
                                                </template>
                                            </bk-table-column>
                                            <bk-table-column :label="$t('quality.拦截时间')" prop="timestamp">
                                                <template slot-scope="props">
                                                    <span>{{localConvertTime(props.row.timestamp)}}</span>
                                                </template>
                                            </bk-table-column>
                                        </bk-table>
                                        <div class="intercept-record-empty" v-if="!recordList.length">
                                            <div class="no-data-right">
                                                <img src="../images/box.png">
                                                <p>{{$t('quality.暂时没有历史拦截记录')}}</p>
                                            </div>
                                        </div>
                                    </section>
                                </bk-tab-panel>
                            </bk-tab>
                        </div>
                    </div>
                </template>
            </bk-sideslider>
        </section>

        <effective-pipeline :pipeline-list-conf="pipelineListConf"
            :loading="dialogLoading"
            :pipeline-list="pipelineList"
            :pagination="pipelinePagination"
            :handle-page-change="handlePipelinePageChange"
            @close="closePipelineList"
        ></effective-pipeline>

        <effective-range :range-list-conf="rangeListConf"
            :range-list="rangeList"
            :pagination="rangePagination"
            :handle-page-change="handleRangePageChange"
            @close="closeRangeList"
        ></effective-range>
    </div>
</template>

<script>
    import imageEmpty from '@/components/common/imageEmpty'
    import effectivePipeline from '@/components/devops/effective-pipeline'
    import effectiveRange from '@/components/devops/effective-range'
    import { convertTime, getQueryString } from '@/utils/util'

    export default {
        components: {
            effectivePipeline,
            effectiveRange,
            'image-empty': imageEmpty
        },
        data () {
            return {
                lastClickRule: '',
                curActiveTab: '',
                showContent: false,
                ruleList: [],
                recordList: [],
                pipelineList: [],
                totalPipelineList: [],
                curThresholdList: [],
                rangeList: [],
                totalRangeList: [],
                panels: [
                    { name: 'detailInfo', label: this.$t('quality.详细信息') },
                    { name: 'recordDate', label: this.$t('quality.历史记录') }
                ],
                ruleDetail: {
                    name: ''
                },
                emptyInfo: {
                    title: this.$t('quality.创建第一条质量红线规则'),
                    desc: this.$t('quality.通过设置不同的指标和阈值，质量红线规则可以控制流水线发布的质量'),
                    btns: [
                        {
                            type: 'primary',
                            size: 'normal',
                            handler: () => this.toCreateRule(),
                            text: this.$t('quality.创建规则')
                        }
                    ]
                },
                loading: {
                    isLoading: false,
                    title: ''
                },
                dialogLoading: {
                    isLoading: false,
                    title: ''
                },
                pipelineListConf: {
                    title: '',
                    isShow: false,
                    closeIcon: false,
                    hasFooter: false
                },
                rangeListConf: {
                    title: '',
                    isShow: false,
                    closeIcon: false,
                    hasFooter: false
                },
                indexHandlerConf: {
                    LT: '<',
                    LE: '<=',
                    GT: '>',
                    GE: '>=',
                    EQ: '='
                },
                sideSliderConfig: {
                    show: false,
                    title: '',
                    quickClose: true,
                    width: 985,
                    data: {},
                    isLoading: false
                },
                sliderPagination: {
                    current: 1,
                    count: 0,
                    limit: 20
                },
                pipelinePagination: {
                    current: 1,
                    count: 0,
                    limit: 10,
                    showLimit: false
                },
                rangePagination: {
                    current: 1,
                    count: 0,
                    limit: 10,
                    showLimit: false
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
            projectId (val) {
                this.$router.push({
                    name: 'qualityOverview',
                    params: {
                        projectId: this.projectId
                    }
                })
            }
        },
        created () {
            const urlParams = getQueryString('linkId')
            if (urlParams) {
                this.toShowSlider(urlParams, 'detail')
            }
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
                    }, 100)
                }
            },
            /**
             * 获取拦截规则列表
             */
            async requestList (page, pageSize) {
                try {
                    const res = await this.$store.dispatch('quality/requestRuleList', {
                        projectId: this.projectId,
                        page,
                        pageSize
                    })
                    
                    this.ruleList.splice(0, this.ruleList.length)
                    if (res.records) {
                        res.records.forEach(item => {
                            this.ruleList.push(item)
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

                this.showContent = true
            },
            async deleteRule (hashId) {
                let message, theme
                try {
                    await this.$store.dispatch('quality/deleteRule', {
                        projectId: this.projectId,
                        ruleHashId: hashId
                    })

                    message = this.$t('quality.删除成功')
                    theme = 'success'
                } catch (err) {
                    message = err.data ? err.data.message : err
                    theme = 'error'
                } finally {
                    this.$bkMessage({
                        message,
                        theme
                    })
                    this.requestList(this.pagination.current, this.pagination.limit)
                }
            },
            /**
             * 获取历史记录列表
             */
            async requestRecordList (page, pageSize) {
                try {
                    const res = await this.$store.dispatch('quality/requestRecordList', {
                        projectId: this.projectId,
                        ruleHashId: this.lastClickRule,
                        page,
                        pageSize
                    })

                    this.recordList.splice(0, this.recordList.length)
                    if (res.records) {
                        res.records.forEach(item => {
                            this.recordList.push(item)
                        })
                        this.sliderPagination.count = res.count
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
            /**
             * 获取流水线列表
             */
            async requestEffectPipeline (rangeSummary) {
                const params = {
                    pipelineId: [],
                    templateId: []
                }
                rangeSummary.forEach(item => {
                    item.type === 'PIPELINE' ? params.pipelineId.push(item.id) : params.templateId.push(item.id)
                })
                
                this.dialogLoading.isLoading = true
                try {
                    const res = await this.$store.dispatch('quality/requestEffectPipeline', {
                        projectId: this.projectId,
                        params
                    })
                    
                    this.totalPipelineList.splice(0, this.totalPipelineList.length)
                    if (res.length) {
                        res.forEach(item => {
                            this.totalPipelineList.push(item)
                        })
                        setTimeout(() => {
                            this.updatePipelineList()
                            this.pipelinePagination.count = res.length
                        }, 100)
                    }
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                }
                this.dialogLoading.isLoading = false
            },
            toDeleteRule (row) {
                if (row.permissions.canDelete) {
                    this.$bkInfo({
                        type: 'warning',
                        theme: 'warning',
                        subTitle: this.$t('quality.确定删除规则({0})？', [row.name]),
                        confirmFn: async () => {
                            this.deleteRule(row.ruleHashId)
                        }
                    })
                } else {
                    const params = {
                        noPermissionList: [
                            { resource: this.$t('quality.quality'), option: this.$t('quality.删除规则') }
                        ],
                        applyPermissionUrl: PERM_URL_PREFIX
                    }
                    this.$showAskPermissionDialog(params)
                }
            },
            async toSwitchRule (row) {
                let message, theme
                try {
                    await this.$store.dispatch('quality/toSwitchRule', {
                        projectId: this.projectId,
                        ruleHashId: row.ruleHashId,
                        isEnable: row.enable
                    })

                    message = row.enable ? this.$t('quality.停用成功') : this.$t('quality.启用成功')
                    theme = 'success'
                } catch (err) {
                    message = err.data ? err.data.message : err
                    theme = 'error'
                } finally {
                    this.$bkMessage({
                        message,
                        theme
                    })
                    this.requestList(this.pagination.current, this.pagination.limit)
                }
            },
            async handlePageChange (page) {
                this.pagination.current = page
                this.loading.isLoading = true
                await this.requestList(this.pagination.current, this.pagination.limit)
                this.loading.isLoading = false
            },
            async limitChange (limit) {
                this.pagination.current = 1
                this.pagination.limit = limit
                this.loading.isLoading = true
                await this.requestList(this.pagination.current, this.pagination.limit)
                this.loading.isLoading = false
            },
            async handleSliderPageChange (page) {
                this.sliderPagination.current = page
                this.sideSliderConfig.isLoading = true
                await this.requestRecordList(this.sliderPagination.current, this.sliderPagination.limit)
                this.sideSliderConfig.isLoading = false
            },
            async sliderLimitChange (limit) {
                if (limit !== this.sliderPagination.limit) {
                    this.sliderPagination.current = 1
                    this.sliderPagination.limit = limit
                    this.sideSliderConfig.isLoading = true
                    await this.requestRecordList(this.sliderPagination.current, this.sliderPagination.limit)
                    this.sideSliderConfig.isLoading = false
                }
            },
            /**
             *  流水线当前页码改变的回调函数
             */
            handlePipelinePageChange (page) {
                this.pipelinePagination.current = page
                this.updatePipelineList(page)
            },
            handleRangePageChange (page) {
                this.rangePagination.current = page
                this.updateRangeList(page)
            },
            updatePipelineList (page) {
                let start
                let end

                if (!page) {
                    start = 0
                    end = this.pipelinePagination.limit
                } else {
                    start = (page - 1) * this.pipelinePagination.limit
                    end = start + this.pipelinePagination.limit
                }

                this.pipelineList.splice(0, this.pipelineList.length, ...this.totalPipelineList.slice(start, end))
            },
            updateRangeList (page) {
                let start
                let end

                if (!page) {
                    start = 0
                    end = this.rangePagination.limit
                } else {
                    start = (page - 1) * this.rangePagination.limit
                    end = start + this.rangePagination.limit
                }

                this.rangeList.splice(0, this.rangeList.length, ...this.totalRangeList.slice(start, end))
            },
            getIndicatorDesc (indicatorList) {
                let tips = ''
                indicatorList.forEach((item, index) => {
                    const isWrap = index === (indicatorList.length - 1) ? '' : '\n'
                    tips += `${item.cnName}${this.indexHandlerConf[item.operation]}${item.threshold}${isWrap}`
                })

                return tips
            },
            /**
             * 处理时间格式
             */
            localConvertTime (timestamp) {
                return convertTime(timestamp * 1000)
            },
            toCreateRule () {
                this.$router.push({
                    name: 'createRule',
                    params: {
                        projectId: this.projectId
                    }
                })
            },
            editRule (row) {
                if (row.permissions.canEdit) {
                    this.$router.push({
                        name: 'editRule',
                        params: {
                            projectId: this.projectId,
                            ruleId: row.ruleHashId
                        }
                    })
                } else {
                    const params = {
                        noPermissionList: [
                            { resource: this.$t('quality.quality'), option: this.$t('quality.编辑规则') }
                        ],
                        applyPermissionUrl: PERM_URL_PREFIX
                    }
                    this.$showAskPermissionDialog(params)
                }
            },
            switchRule (row) {
                if (row.permissions.canEnable) {
                    const infoTitle = row.enable ? this.$t('quality.停用') : this.$t('quality.启用')
                    const h = this.$createElement
                    const content = h('p', {
                        style: {
                            textAlign: 'center'
                        }
                    }, this.$t('quality.确定{0}规则({1})？', [infoTitle, row.name]))

                    this.$bkInfo({
                        title: infoTitle,
                        subHeader: content,
                        confirmFn: async () => {
                            this.toSwitchRule(row)
                        }
                    })
                } else {
                    const params = {
                        noPermissionList: [
                            { resource: this.$t('quality.quality'), option: this.$t('quality.启用和停用规则') }
                        ],
                        applyPermissionUrl: PERM_URL_PREFIX
                    }
                    this.$showAskPermissionDialog(params)
                }
            },
            async toShowSlider (ruleHashId, type) {
                this.curActiveTab = type === 'detail' ? 'detailInfo' : 'recordDate'
                this.lastClickRule = ruleHashId
                this.sideSliderConfig.isLoading = true
                this.sideSliderConfig.show = true
                this.sliderPagination.current = 1
                this.sliderPagination.limit = 20
                await this.requestRecordList(this.sliderPagination.current, this.sliderPagination.limit)

                try {
                    const res = await this.$store.dispatch('quality/requestRuleDetail', {
                        projectId: this.projectId,
                        ruleHashId: ruleHashId
                    })

                    if (res) {
                        this.ruleDetail = res
                        this.curThresholdList = res.indicators
                    }
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                } finally {
                    this.sideSliderConfig.isLoading = false
                }
            },
            closrSlider () {
                this.ruleDetail.name = ''
            },
            toShowPipeline (row) {
                this.pipelineListConf.isShow = true
                this.pipelineListConf.title = row.name
                this.pipelinePagination.current = 1
                this.pipelineList = []
                this.requestEffectPipeline(row.rangeSummary)
            },
            closePipelineList () {
                this.pipelineListConf.isShow = false
            },
            toShowRange (row) {
                this.totalRangeList.splice(0, this.totalRangeList.length, ...row.rangeSummary)
                this.rangePagination.count = row.rangeSummary.length
                this.updateRangeList()
                this.rangeListConf.isShow = true
            },
            closeRangeList () {
                this.rangeListConf.isShow = false
            }
        }
    }
</script>

<style lang="scss">
    @import '@/scss/conf.scss';

    .rule-list-wrapper {
        overflow: auto;
        .rule-list-header {
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
        .rule-main-wrapper {
            padding: 20px;
            height: 100%;
            overflow: auto;
        }
        .rule-main-container {
            height: 100%;
        }
        .rule-table-wrapper {
            margin: 20px auto;
        }
        .rule-list-table {
            td {
                &:first-child,
                &:last-child,
                .intercept-count {
                    color: $primaryColor;
                }
                &:first-child,
                .intercept-count {
                    cursor: pointer;
                }
            }
            .rule-detail {
                display: -webkit-box;
                -webkit-line-clamp: 3;
                -webkit-box-orient: vertical;
                word-break: break-all;
                overflow: hidden;
                max-height: 60px;
                line-height: 1.5;
            }
            .rule-name {
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
            }
            .handler-btn {
                span {
                    display: inline-block;
                    margin-right: 16px;
                    cursor: pointer;
                }
                .delete-btn {
                    margin-right: 0;
                }
            }
            .canShowPipeline {
                color: $primaryColor;
                cursor: pointer;
            }
            td.indicator-item .cell {
                padding: 10px 15px;
            }
            td.controlPoint-item .cell {
                -webkit-line-clamp: 3;
            }
        }
        .bk-sideslider-wrapper {
            padding-bottom: 0;
        }
        .bk-sideslider-content {
            height: calc(100% - 60px);
            overflow: hidden;
        }
        .rule-slider-info {
            height: 100%;
            overflow: hidden;
            .slider-main {
                padding: 32px 42px;
                overflow: auto;
                height: 100%;
            }
            .threshold-item {
                width: 400px;
            }
            .threshold-label {
                vertical-align: top;
            }
            .match-rule-table {
                td:last-child {
                    color: $dangerColor;
                }
            }
            .empty-tips {
                text-align: center;
                padding: 50px;
            }
            .detail-info {
                margin: 20px 40px 20px 10px;
                td {
                    padding: 6px 0;
                }
                .item-label {
                    text-align: right;
                    color: #333C48;
                }
                .item-value {
                    padding-left: 8px;
                }
            }
            .record-table {
                th, td {
                    p {
                        display: -webkit-box;
                        -webkit-line-clamp: 1;
                        -webkit-box-orient: vertical;
                        word-break: break-all;
                        overflow: hidden;
                        max-height: 42px;
                        line-height: 1.5;
                    }
                }
                .source-item {
                    color: $primaryColor;
                    cursor: pointer;
                }
            }
            .intercept-record-empty {
                flex: 1;
                .no-data-right {
                    text-align: center;
                    margin: 200px auto;
                    p {
                        line-height: 60px;
                    }
                }
            }
        }
    }
</style>

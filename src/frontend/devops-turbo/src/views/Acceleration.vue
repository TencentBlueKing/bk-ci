<template>
    <div class="turbo-task-wrapper" v-bkloading="{ isLoading: loading.isloading }">
        <header-process :process-head="title"></header-process>
        <div class="task-container container-wrapper sub-view-port">
            <div class="filter-wrapper">
                <div class="filter-item">
                    <label class="filter-label">流水线</label>
                    <bk-select
                        v-model="searchParam.bsPipelineId"
                        :searchable="true"
                        :clearable="true">
                        <bk-option v-for="(option, index) in pipelineList"
                            :key="index"
                            :id="option.pipelineId"
                            :name="option.pipelineName">
                        </bk-option>
                    </bk-select>
                </div>
                <div class="filter-item">
                    <label class="filter-label">状态</label>
                    <bk-select
                        v-model="searchParam.taskStatus"
                        :clearable="true">
                        <bk-option v-for="(option, index) in statusList"
                            :key="index"
                            :id="option.paramCode"
                            :name="option.paramName">
                        </bk-option>
                    </bk-select>
                </div>
                <div class="filter-button">
                    <bk-button theme="primary" @click="query(1)">查询</bk-button>
                </div>
            </div>
            <div class="table-wrapper">
                <bk-table
                    size="small"
                    class="turbo-table"
                    :data="tableList"
                    :pagination="pageConf"
                    :empty-text="'暂无数据'"
                    @page-change="pageChange"
                    @page-limit-change="pageSelectChanged">
                    <bk-table-column label="编号" width="70">
                        <template slot-scope="props">
                            <span>{{ pageConf.count - props.$index - (pageConf.current - 1) * pageConf.limit }}</span>
                        </template>
                    </bk-table-column>
                    <bk-table-column label="任务名称" prop="taskName">
                        <template slot-scope="props">
                            <span :title="props.row.taskName">{{ props.row.taskName }}</span>
                        </template>
                    </bk-table-column>
                    <bk-table-column label="流水线/本地构建路径" prop="bsPipelineName" min-width="120">
                        <template slot-scope="props">
                            <div :title="props.row.bsPipelineName" class="turbo-td task-program">
                                <div v-if="props.row.bsPipelineId === '-1' || !props.row.bsPipelineId">{{ props.row.bsPipelineName || '--' }}</div>
                                <a v-else :href="`/console/pipeline/${projectId}/${props.row.bsPipelineId}/history`" target="_blank" class="text-link">{{ props.row.bsPipelineName || '--' }}</a>
                            </div>
                        </template>
                    </bk-table-column>
                    <bk-table-column label="加速方案">
                        <template slot-scope="props">
                            <span>{{ programme(props.row.ccacheEnabled, props.row.banDistcc) }}</span>
                        </template>
                    </bk-table-column>
                    <bk-table-column label="加速资源" min-width="110">
                        <template slot-scope="props">
                            <span>{{ props.row.machineNum && props.row.cpuNum ? `${props.row.machineNum}台机器（共${props.row.cpuNum}核）` : '--' }}</span>
                        </template>
                    </bk-table-column>
                    <bk-table-column label="最近状态">
                        <template slot-scope="props">
                            <div :class="['turbo-td status', statuClass(props.row.latestStatus)]">{{ props.row.latestStatusName }}</div>
                        </template>
                    </bk-table-column>
                    <bk-table-column label="加速次数">
                        <template slot-scope="props">
                            <span>{{ props.row.acceRecordCount }}</span>
                        </template>
                    </bk-table-column>
                    <bk-table-column label="平均耗时">
                        <template slot-scope="props">
                            <span>{{ props.row.averageCompileTime }}</span>
                        </template>
                    </bk-table-column>
                    <bk-table-column label="操作" width="80">
                        <template slot-scope="props">
                            <div>
                                <show-tooltip placement="bottom-end" :content="&quot;可在这里继续配置任务，可在这里查看加速效果~&quot;" :always="true"
                                    @confirm="closeTooltip"
                                    v-if="props.$index === 0 && showTurboTooltips">
                                    <a href="javascript:;" class="text-link dropdown-menu-more">
                                        <bk-popover theme="light" placement="left">
                                            <span class="text-link">更多</span>
                                            <div slot="content" class="handle-menu-tips">
                                                <p class="entry-link" @click.stop="toggleSlider(props.row)">
                                                    查看报表
                                                </p>
                                                <p class="entry-link" @click.stop="openSetting(props.row)">
                                                    配置
                                                </p>
                                                <p class="entry-link" @click.stop="deleteTask(props.row.taskId)">
                                                    删除
                                                </p>
                                            </div>
                                        </bk-popover>
                                    </a>
                                </show-tooltip>
                                <a href="javascript:;" class="text-link dropdown-menu-more" v-else>
                                    <bk-popover theme="light" placement="left">
                                        <span class="text-link">更多</span>
                                        <div slot="content" class="handle-menu-tips">
                                            <p class="entry-link" @click.stop="toggleSlider(props.row)">
                                                查看报表
                                            </p>
                                            <p class="entry-link" @click.stop="openSetting(props.row)">
                                                配置
                                            </p>
                                            <p class="entry-link" @click.stop="deleteTask(props.row.taskId)">
                                                删除
                                            </p>
                                        </div>
                                    </bk-popover>
                                </a>
                            </div>
                        </template>
                    </bk-table-column>
                </bk-table>
            </div>
        </div>
        <bk-sideslider
            :title="moreSettings.title"
            :is-show.sync="moreSettings.isShow"
            :quick-close="moreSettings.quickClose"
            :width="moreSettings.width"
            :direction="moreSettings.direction"
            @hidden="hiddenSlider">
            <div slot="content" class="turbo-slider">
                <bk-tab :active.sync="sliderTabActive" type="unborder-card">
                    <bk-tab-panel
                        v-for="(panel, index) in sliderTab"
                        v-bind="panel"
                        :key="index">
                        <chart-slider v-if="sliderTabActive === 'chart' && moreSettings.isShow" :task="taskInfo"></chart-slider>
                        <build-slider-index v-if="sliderTabActive === 'config'"
                            :task="taskInfo"
                            @buildSetting="buildSetHandler"
                            @toggleSlider="toggleSlider">
                        </build-slider-index>
                    </bk-tab-panel>
                </bk-tab>
            </div>
        </bk-sideslider>
    </div>
</template>

<script>
    import headerProcess from '@/components/turbo/headerProcess'
    import chartSlider from '@/components/acceleration/chart-slider'
    import buildSliderIndex from '@/components/acceleration/buildSliderIndex'

    export default {
        components: {
            headerProcess,
            chartSlider,
            buildSliderIndex
        },
        data () {
            return {
                showTurboTooltips: false,
                lastCliCKBuildId: '',
                sliderTabActive: 'chart',
                hasHash: '',
                title: {
                    title: '加速任务',
                    list: [],
                    hasLink: false
                },
                sliderTab: [
                    {
                        label: '报表',
                        name: 'chart'
                    },
                    {
                        label: '配置',
                        name: 'config'
                    }
                ],
                moreSettings: {
                    isShow: false,
                    quickClose: true,
                    title: '标题',
                    width: 665,
                    direction: 'right'
                },
                loading: {
                    isloading: true,
                    title: ''
                },
                pipelineLoading: false,
                dateDefault: {
                    startTime: '',
                    endTime: ''
                },
                statusList: [],
                pageSelectConfig: {
                    count: 0,
                    list: [
                        { id: 10, name: 10 },
                        { id: 20, name: 20 },
                        { id: 50, name: 50 },
                        { id: 100, name: 100 }
                    ],
                    pageSelected: 10,
                    hasSelect: true
                },
                pageTabConf: {
                    page: 1,
                    totalPage: 1
                },
                pipelineList: [],
                tableList: [],
                taskInfo: {},
                searchParam: {
                    bsPipelineId: '',
                    taskStatus: '',
                    page: '',
                    pageSize: ''
                },
                pageConf: {
                    totalPage: 1,
                    limit: 10,
                    current: 1,
                    show: false,
                    limitList: [10, 20, 50, 100],
                    count: 0
                }
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            tooltipEventList () {
                const tooltipEventList = localStorage.getItem('tooltipEventList')
                return tooltipEventList ? JSON.parse(tooltipEventList) : []
            }
        },
        watch: {
            projectId: async function () {
                await this.init()
            }
        },
        created () {
            this.requestTaskStatus()
            this.init()
            this.addClickListenr()
        },
        beforeDestroy () {
            this.removeClickListenr()
        },
        methods: {
            openSetting (task) {
                this.sliderTabActive = 'config'
                this.toggleSlider(task)
            },
            tabSwitch (type) {
                this.sliderTabActive = type
            },
            toggleSlider (task) {
                const zIndex = this.moreSettings.isShow ? 0 : 1000
                this.setParentzIndex(zIndex)
                this.moreSettings.isShow = !this.moreSettings.isShow
                task && (this.taskInfo = task) && (this.moreSettings.title = task.taskName)
            },
            hiddenSlider () {
                this.sliderTabActive = 'chart'
                this.setParentzIndex(0)
            },
            toggleMore (buildId) {
                this.lastCliCKBuildId = buildId
                this.tableList.forEach(item => {
                    if (item.taskId === this.lastCliCKBuildId) {
                        item.isMore = !item.isMore
                    } else {
                        item.isMore = false
                    }
                })
            },
            programme (ccache, distcc) {
                return ccache === 'true' && distcc === 'false'
                    ? 'ccache+distcc' : (ccache === 'true' && distcc === 'true'
                        ? 'ccache' : (ccache === 'false' && distcc === 'false'
                            ? 'distcc' : '---'))
            },
            statuClass (latestStatus) {
                let statusClass = ''
                this.statusList.forEach(status => {
                    (status.paramCode.toString() === latestStatus.toString()) && (statusClass = status.paramValue)
                })
                
                return statusClass
            },

            deleteTask (taskId) {
                this.$bkInfo({
                    title: '确认要删除',
                    subTitle: '删除后该任务对应脚本原子将无法加速编译',
                    confirm: '确认',
                    cancel: '取消',
                    confirmFn: async (done) => {
                        try {
                            const res = await this.$store.dispatch('turbo/deleteTask', {
                                taskId: taskId
                            })
                            if (res) {
                                this.query()
                            }
                        } catch (err) {
                            this.$bkMessage({
                                message: err.message ? err.message : err,
                                theme: 'error'
                            })
                        } finally {
                            // done()
                        }
                    }
                })
            },
            pageChange (page) {
                this.pageConf.current = page
                this.query()
            },
            async query (pageParam) {
                const { searchParam, pageConf, projectId, loading, $store } = this
                if (pageParam === 1) {
                    this.pageConf.current = 1
                }
                loading.isloading = true
                try {
                    const params = Object.assign({}, searchParam, { page: pageConf.current }, { pageSize: pageConf.limit })
                    
                    if (this.hasHash) {
                        Object.assign(params, { taskId: this.hasHash })
                    }
                    const res = await $store.dispatch('turbo/requestTaskList', {
                        projectId: projectId,
                        params
                    })
                    if (res) {
                        res.records.forEach(item => {
                            Object.assign(item, {
                                isMore: false
                            })
                        })
                        this.tableList = res.records.concat()
                        this.pageConf.count = res.count
                        this.pageConf.totalPage = res.totalPages || 1
                        
                        if (this.hasHash) {
                            const task = this.tableList.find(item => item.taskId === this.hasHash)
                            if (task) {
                                this.hasHash = ''
                                this.openSetting(task)
                            }
                        }
                    }
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                } finally {
                    loading.isloading = false
                }
            },
            async requestPipelineList () {
                const { pipelineList } = this
                this.pipelineLoading = true
                try {
                    const res = await this.$store.dispatch('turbo/requestPipelineList', {
                        projectId: this.projectId
                    })
                    if (res) {
                        pipelineList.splice(0, pipelineList.length)
                        res.map(item => {
                            pipelineList.push(item)
                        })
                        if (this.hasHash) {
                            this.pageSelectChanged()
                        }
                    }
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                } finally {
                    this.pipelineLoading = false
                }
            },

            /**
             * 修改回调
             */
            buildSetHandler (taskParams) {
                this.query()
                // console.log('buildSetHandler: ', taskParams)
                // let taskIndex
                // this.tableList.forEach((item, index) => {
                //     (item.taskId === taskParams.taskId) && (taskIndex = index)
                // })
                // if (taskIndex || taskIndex === 0) {
                //     this.tableList[taskIndex] = Object.assign({}, taskParams)
                // }
            },

            /**
             *  每页条数下拉框改变的回调函数
             */
            async pageSelectChanged (limit) {
                this.pageConf.limit = limit
                this.pageConf.current = 1
                // this.pageTabConf.page = 1
                this.query()
            },
            async requestTaskStatus () {
                try {
                    const res = await this.$store.dispatch('turbo/requestTaskStatus')
                    if (res) {
                        res.forEach(item => {
                            this.statusList.push(item)
                        })
                    }
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                }
            },
            numberTrans (num) {
                if (num <= 0) {
                    return 0
                }
                const result = (num.toString()).indexOf('.')
                if (result !== -1) {
                    return num.toFixed(3)
                } else {
                    return num
                }
            },
            init () {
                if (!this.tooltipEventList.includes('accelerate_task_handler')) {
                    this.showTurboTooltips = true
                }
                const { hash } = this.$route
                if (hash) {
                    const hashArr = hash.slice(1).split('&')
                    this.hasHash = hashArr[1]
                    // this.searchParam.bsPipelineId = hashArr[0]
                }
                this.requestPipelineList()
                if (!this.hasHash) {
                    this.pageSelectChanged(10)
                }
                this.moreSettings.isShow = false
            },
            clickHandler (event) {
                if (!/more-handler-id/.test(event.target.className)) {
                    this.lastCliCKBuildId = ''
                    this.tableList.forEach(item => {
                        item.isMore = false
                    })
                }
                // if (event.target.id !== 'moreHandler' && event.target.id !== 'toggleIcon') {
                //     this.lastCliCKBuildId = ''
                //     this.buildList.forEach(item => {
                //         item.isMore = false
                //     })
                //     if (event.target.id !== 'codeCheck' && event.target.id !== 'codeRecord') {
                //         this.buildList = this.buildList.concat([])
                //     }
                // }
            },
            addClickListenr () {
                document.addEventListener('mouseup', this.clickHandler)
            },
            removeClickListenr () {
                document.removeEventListener('mouseup', this.clickHandler)
            },
            closeTooltip () {
                this.showTurboTooltips = false
                this.tooltipEventList.push('accelerate_task_handler')
                localStorage.setItem('tooltipEventList', JSON.stringify(this.tooltipEventList))
            },
            setParentzIndex (zIndex) {
                const parentDom = document.querySelector('.navigation-bar-container') || document.querySelector('.bk-sideslider')
                if (parentDom) {
                    parentDom.style.zIndex = zIndex
                }
            }
        }
    }
</script>

<style lang="scss">
    @import '../assets/scss/conf.scss';

    .turbo-task-wrapper {
        height: 100%;
        overflow: hidden;
        .bk-sideslider-wrapper {
            top: 50px;
        }
        .slider-tab {
            margin-bottom: 12px;
            ul {
                border-bottom: 1px solid $borderWeightColor;
            }
            li {
                position: relative;
                float: left;
                padding: 0 13px;
                height: 34px;
                line-height: 34px;
                cursor: pointer;
                &.active {
                    font-weight: bold;
                    color: $fontColorLabel;
                    &:after {
                        content: '';
                        position: absolute;
                        left: 0;
                        bottom: -1px;
                        display: block;
                        width: 100%;
                        border-bottom: 2px solid $primaryColor;
                    }
                }
            }
        }
        .task-id {
            min-width: 50px;
        }
        .task-name {
            min-width: 125px;
        }
        .pipeline-name {
            min-width: 130px;
        }
        .task-program {
            div {
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
                display: inline-block;
            }
            min-width: 110px;
        }
        .resource {
            min-width: 140px;
        }
        .status {
            min-width: 70px;
        }
        .times {
            min-width: 65px;
        }
        .time-average {
            min-width: 70px;
        }
        .more {
            min-width: 50px;
        }
    }
    .handle-menu-tips {
        .entry-link {
            padding: 4px 0;
            font-size: 12px;
            cursor: pointer;
            color: $fontWeightColor;
            > a {
                color: $fontWeightColor;
            }
            &:hover {
                color: $primaryColor;
                > a {
                    color: $primaryColor;
                }
            }
        }
    }
</style>

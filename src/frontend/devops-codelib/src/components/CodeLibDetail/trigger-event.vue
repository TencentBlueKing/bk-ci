<template>
    <section class="trigger-event">
        <header class="header-warpper">
            <bk-date-picker
                class="date-picker mr15"
                :value="daterange"
                type="datetimerange"
                :placeholder="$t('codelib.选择日期范围')"
                :options="{
                    disabledDate: time => time.getTime() > Date.now()
                }"
                :shortcuts="shortcuts"
                :key="repoId"
                @clear="handleClearDaterange"
                @change="handleChangeDaterange"
                @pick-success="handlePickSuccess"
            >
            </bk-date-picker>
            <search-select
                ref="searchSelect"
                class="search-input"
                v-model="searchValue"
                :data="searchList"
                clearable
                :show-condition="false"
                :placeholder="filterTips"
            >
            </search-select>
            <span
                class="refresh-icon"
                @click="handleRefresh"
            >
                <bk-icon type="refresh" />
            </span>
        </header>
        <section
            v-if="eventList.length"
            class="timeline-warpper"
            @scroll.passive="handleScroll"
            v-bkloading="{ isLoading: pageLoading }"
        >
            <ul class="trigger-timeline">
                <li
                    class="timeline-dot"
                    v-for="(data, key, index) in timelineMap"
                    :key="index"
                >
                    <div class="timeline-section">
                        <TimelineCollapse
                            :search-value="searchValue"
                            :data="data"
                            :cur-repo="curRepo"
                            :time="key"
                            @replay="replayEvent"
                        />
                    </div>
                </li>
            </ul>
            <div
                class="timeline-footer"
                v-if="!showEnd"
                v-bkloading="{ isLoading: isLoadingMore }"
            >
                <a
                    v-if="!hasLoadEnd"
                    @click="getListData"
                >{{ $t('codelib.加载更多') }}</a>
                <span
                    v-else
                    class="load-end"
                >{{ $t('codelib.到底啦') }}</span>
            </div>
        </section>
        <EmptyTableStatus
            v-else
            v-bkloading="{ isLoading: pageLoading }"
            :type="isSearch ? 'search-empty' : 'empty'"
            @clear="resetFilter"
        />
    </section>
</template>
<script>
    import {
        mapActions
    } from 'vuex'
    import EmptyTableStatus from '../empty-table-status.vue'
    import TimelineCollapse from './timeline-collapse.vue'
    import '@blueking/search-select/dist/styles/index.css'

    export default {
        name: 'basicSetting',
        components: {
            EmptyTableStatus,
            TimelineCollapse,
            SearchSelect: () => import('@blueking/search-select')
        },
        props: {
            curRepo: {
                type: Object,
                default: () => {}
            },
            repoInfo: {
                type: Object,
                default: () => {}
            },
            triggerTypeList: {
                type: Object,
                default: () => {}
            },
            eventTypeList: {
                type: Object,
                default: () => {}
            }
        },
        data () {
            const setDefaultDaterange = () => {
                // 获取当前日期
                const now = new Date()

                // 获取一周前的日期
                const oneWeekAgo = new Date()
                oneWeekAgo.setDate(now.getDate() - 7)

                // 创建开始和结束日期对象
                const start = new Date(oneWeekAgo.setHours(0, 0, 0))
                const end = new Date(now.setHours(23, 59, 59))
                return [start, end]
            }
            
            return {
                eventList: [],
                timelineMap: {},
                searchValue: [],
                daterangeCache: [],
                daterange: setDefaultDaterange(),
                page: 1,
                pageSize: 20,
                catchRepoId: '',
                isLoadingMore: false,
                hasLoadEnd: false,
                pageLoading: false,
                isInitTime: true,
                shortcuts: [],
                setDefaultDaterange
            }
        },
        computed: {
            repoId () {
                return this.$route.query.id
            },
            projectId () {
                return this.$route.params.projectId
            },
            filterTips () {
                return this.searchList.map(item => item.name).join(' / ')
            },
            searchList () {
                const list = [
                    {
                        name: this.$t('codelib.事件ID'),
                        id: 'eventId'
                    },
                    {
                        name: this.$t('codelib.触发器类型'),
                        id: 'triggerType',
                        children: this.triggerTypeList
                    },
                    {
                        name: this.$t('codelib.事件类型'),
                        id: 'eventType',
                        children: this.eventTypeList
                    },
                    {
                        name: this.$t('codelib.触发人'),
                        id: 'triggerUser'
                    },
                    {
                        name: this.$t('codelib.流水线名称'),
                        id: 'pipelineId',
                        remoteMethod: (keyword) => {
                            return this.fetchPipelinesByName({
                                projectId: this.projectId,
                                keyword
                            })
                        }
                    },
                    {
                        name: this.$t('codelib.触发结果'),
                        id: 'reason',
                        children: [
                            { name: this.$t('codelib.触发成功'), id: 'TRIGGER_SUCCESS' },
                            { name: this.$t('codelib.触发失败'), id: 'TRIGGER_FAILED' },
                            { name: this.$t('codelib.触发器不匹配'), id: 'TRIGGER_NOT_MATCH' }
                        ]
                    }
                ]
                return list.filter((data) => {
                    return !this.searchValue.find(val => val.id === data.id)
                })
            },
            isSearch () {
                if (this.isInitTime) {
                    return false
                }
                return this.daterange[0] || this.searchValue.length
            },
            eventId () {
                return this.$route.query.eventId || ''
            },
            scmType () {
                return this.$route.query.scmType || ''
            }
        },
        watch: {
            async repoId (id) {
                this.daterange = this.setDefaultDaterange()
                await this.triggerRepo()
                this.catchRepoId = id
                this.isInitTime = true
            },
            daterange (newVal, oldVal) {
                if (oldVal[0]) this.isInitTime = false
                this.page = 1
                this.hasLoadEnd = false
                this.eventList = []
                this.timelineMap = {}
                if ((this.catchRepoId === this.repoId) && !this.eventId) {
                    this.getListData()
                }
            },
            searchValue (newVal, oldVal) {
                this.isInitTime = false
                this.page = 1
                this.hasLoadEnd = false
                this.eventList = []
                this.timelineMap = {}
                if (this.catchRepoId === this.repoId) {
                    this.getListData()
                }
            }
        },
        created () {
            this.catchRepoId = this.repoId
            if (!this.eventId) {
                this.getListData()
            }
            this.shortcuts = [
                {
                    text: this.$t('codelib.今天'),
                    value () {
                        const end = new Date()
                        const start = new Date(end.getFullYear(), end.getMonth(), end.getDate())
                        return [start, end]
                    }
                },
                {
                    text: this.$t('codelib.昨天'),
                    value () {
                        const time = new Date()
                        const end = new Date(time.getFullYear(), time.getMonth(), time.getDate() - 1, 23, 59, 59)
                        const start = new Date(time.getFullYear(), time.getMonth(), time.getDate() - 1)
                        return [start, end]
                    }
                },
                {
                    text: this.$t('codelib.近3天'),
                    value () {
                        const end = new Date()
                        const start = new Date()
                        start.setTime(start.getTime() - 3600 * 1000 * 24 * 3)
                        return [start, end]
                    }
                },
                {
                    text: this.$t('codelib.近7天'),
                    value () {
                        const end = new Date()
                        const start = new Date()
                        start.setTime(start.getTime() - 3600 * 1000 * 24 * 7)
                        return [start, end]
                    }
                }
            ]
            if (this.eventId) {
                this.searchValue.push({
                    id: 'eventId',
                    name: this.$t('codelib.事件ID'),
                    values: [{
                        id: this.eventId,
                        name: this.eventId
                    }]
                })
                this.daterange = ['', '']
            }
        },
        methods: {
            ...mapActions('codelib', [
                'fetchTriggerEventList',
                'fetchPipelinesByName'
            ]),
         
            handleScroll (event) {
                const target = event.target
                const bottomDis = target.scrollHeight - target.clientHeight - target.scrollTop
                if (bottomDis <= 10 && !this.hasLoadEnd && !this.isLoadingMore) this.getListData()
            },
            getListData () {
                if (this.hasLoadEnd) return
                if (this.page === 1) {
                    this.pageLoading = true
                } else {
                    this.isLoadingMore = true
                }
                const daterange = this.daterange.map(i => i && new Date(i).getTime())
                const params = {}
                this.searchValue.forEach(i => {
                    params[`${i.id}`] = (i.values && i.values[0].id) || i.name
                })
                
                this.fetchTriggerEventList({
                    projectId: this.projectId,
                    repositoryHashId: this.repoId,
                    page: this.page,
                    pageSize: this.pageSize,
                    ...params,
                    triggerType: params.triggerType || this.scmType,
                    startTime: daterange[0],
                    endTime: daterange[1]
                }).then(res => {
                    if (this.page === 1) {
                        this.eventList = []
                    }
                    this.eventList = [...this.eventList, ...res.records]
                    this.timelineMap = {}
                    
                    this.eventList.forEach(item => {
                        const eventDate = new Date(item.eventTime)
                        const year = eventDate.getFullYear()
                        const month = eventDate.getMonth() + 1
                        const day = eventDate.getDate()
                        const dateKey = `${year}-${month}-${day}`

                        if (!this.timelineMap[dateKey]) {
                            this.timelineMap[dateKey] = []
                        }
                        
                        this.timelineMap[dateKey].push(item)
                    })
                    this.showEnd = res.count <= this.pageSize
                    this.hasLoadEnd = res.count === this.eventList.length
                    this.page += 1
                }).finally(() => {
                    this.pageLoading = false
                    this.isLoadingMore = false
                    if (this.eventId) {
                        const query = { ...this.$route.query }
                        delete query.eventId
                        this.$router.push({
                            query: {
                                ...query
                            }
                        })
                    }
                })
            },
            resetFilter () {
                this.catchRepoId = ''
                this.daterange = []
                this.searchValue = []
                this.page = 1
                this.hasLoadEnd = false
                this.eventList = []
                this.timelineMap = {}
                this.getListData()
            },
            triggerRepo () {
                this.catchRepoId = ''
                this.searchValue = []
                this.page = 1
                this.hasLoadEnd = false
                this.eventList = []
                this.timelineMap = {}
                this.getListData()
            },

            handleClearDaterange () {
                this.daterange = ['', '']
            },
            
            handleChangeDaterange (date, type) {
                const startTime = new Date(date[0]).getTime() || ''
                const endTime = new Date(date[1]).getTime() || ''
                this.daterangeCache = [startTime, endTime]
            },
            
            handlePickSuccess () {
                this.daterange = this.daterangeCache
            },

            async handleRefresh () {
                this.page = 1
                this.pageLoading = true
                this.hasLoadEnd = false
                await this.getListData()
            },

            replayEvent () {
                this.pageLoading = true
                this.hasLoadEnd = false
                setTimeout(() => {
                    this.daterange = this.setDefaultDaterange()
                }, 1000)
            }
        }
    }
</script>
<style lang='scss' scoped>
    .trigger-event {
        height: 100%;
        .header-warpper {
            display: flex;
        }
        .refresh-icon {
            display: flex;
            justify-content: space-around;
            align-items: center;
            flex-shrink: 0;
            width: 32px;
            height: 32px;
            border: 1px solid #c4c6cc;
            color: #63656e;
            margin-left: 10px;
            font-size: 18px;
            cursor: pointer;
        }
        .date-picker {
            max-width: 400px;
            min-width: 340px;
        }
        .search-select {
            width: 100%;
        }
        .timeline-warpper {
            height: calc(100% - 32px);
            padding-top: 20px;
            margin-top: 8px;
            overflow-y: scroll;
            &::-webkit-scrollbar {
                background-color: #fff;
                height: 0px !important;
                width: 0px !important;
            }
        }
    }
    .trigger-timeline {
        padding: 0 8px;
        .timeline-dot {
            border-left: 1px solid #d8d8d8;
            font-size: 0;
            margin-top: 13px;
            padding-bottom: 14px;
            padding-left: 16px;
            position: relative;
            &::before {
                background: #fff;
                border: 2px solid #d8d8d8;
                border-radius: 50%;
                -webkit-box-sizing: border-box;
                box-sizing: border-box;
                content: "";
                display: inline-block;
                position: absolute;
                height: 15px;
                left: -8px;
                top: -16px;
                width: 15px;
            }
        }
        .timeline-dot:last-child {
            border-left: 1px solid transparent;
            padding-bottom: 0;
        }
        .timeline-section {
            position: relative;
            top: -15px;
        }
    }
    .empty-data {
        display: flex;
        justify-content: center;
        align-items: center;
        .empty-icon {
            width: 120px;
            margin-top: 20%;
            img {
                width: 100%;
            }
        }
        .search-empty-icon {
            width: 70%;
            margin-top: 10%;
        }
    }
    .timeline-footer {
        text-align: center;
        font-size: 12px;
        .load-end {
            color: #C4C6CC;
        }
        ::v-deep .bk-loading {
            background-color: #fff !important;
        }
    }
    .search-input {
        flex: 1;
        background: white;
        ::placeholder {
            color: #c4c6cc;
        }
    }
</style>

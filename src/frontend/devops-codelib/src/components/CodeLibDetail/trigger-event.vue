<template>
    <section class="trigger-event">
        <header class="header-warpper">
            <bk-date-picker
                class="date-picker mr15"
                v-model="daterange"
                type="daterange"
                :placeholder="$t('codelib.选择日期范围')"
                :options="{
                    disabledDate: time => time.getTime() > Date.now()
                }"
            >
            </bk-date-picker>
            <bk-search-select
                class="search-select"
                v-model="searchValue"
                :data="searchList"
                clearable
                :show-condition="false"
                :placeholder="$t('codelib.触发器类型/事件类型/触发人/流水线名称')"
            >
            </bk-search-select>
        </header>
        <section
            v-if="eventList.length"
            class="timeline-warpper"
            @scroll.passive="handleScroll"
            v-bkloading="{ isLoading: pageLoading }"
        >
            <ul class="trigger-timeline">
                <li class="timeline-dot"
                    v-for="(data, key, index) in timelineMap" :key="index"
                >
                    <div class="timeline-section">
                        <TimelineCollapse
                            :data="data"
                            :time="key"
                        />
                    </div>
                </li>
            </ul>
            <div class="timeline-footer" v-bkloading="{ isLoading: isLoadingMore }">
                <a v-if="!hasLoadEnd" @click="getListData">{{ $t('codelib.加载更多') }}</a>
                <span v-else class="load-end">{{ $t('codelib.到底啦') }}</span>
            </div>
        </section>
        <EmptyTableStatus
            v-else
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

    export default {
        name: 'basicSetting',
        components: {
            EmptyTableStatus,
            TimelineCollapse
        },
        props: {
            curRepo: {
                type: Object,
                default: () => {}
            }
        },
        data () {
            return {
                eventList: [],
                timelineMap: {},
                searchValue: [],
                daterange: ['', ''],
                eventTypeList: [],
                triggerTypeList: [],
                page: 1,
                pageSize: 20,
                catchRepoId: '',
                isLoadingMore: false,
                hasLoadEnd: false,
                pageLoading: false
            }
        },
        computed: {
            repoId () {
                return this.$route.query.id
            },
            projectId () {
                return this.$route.params.projectId
            },
            triggerType () {
                return this.curRepo.type || ''
            },
            // triggerTypeList () {
            //     return [
            //         { name: this.$t('codelib.手动触发'), id: 'MANUAL' },
            //         { name: this.$t('codelib.定时触发'), id: 'TIME_TRIGGER' },
            //         { name: this.$t('codelib.服务触发'), id: 'SERVICE' },
            //         { name: this.$t('codelib.流水线触发'), id: 'PIPELINE' },
            //         { name: this.$t('codelib.远程触发'), id: 'REMOTE' }
            //     ]
            // },
            // eventTypeList () {
            //     const listMap = {
            //         GITHUB: [
            //             { name: this.$t('codelib.创建Branch/Tag事件'), id: 'CREATE' },
            //             { name: this.$t('codelib.PR事件'), id: 'PULL_REQUEST' }
            //         ],
            //         CODE_SVN: [
            //             { name: this.$t('codelib.POST_COMMIT事件'), id: 'POST_COMMIT' },
            //             { name: this.$t('codelib.LOCK_COMMIT事件'), id: 'LOCK_COMMIT' },
            //             { name: this.$t('codelib.PRE_COMMIT事件'), id: 'PRE_COMMIT' }
            //         ],
            //         CODE_GIT: [
            //             { name: this.$t('codelib.推送事件'), id: 'PUSH' },
            //             { name: this.$t('codelib.标签推送事件'), id: 'TAG_PUSH' },
            //             { name: this.$t('codelib.合并请求事件'), id: 'MERGE_REQUEST' },
            //             { name: this.$t('codelib.议题事件'), id: 'ISSUES' },
            //             { name: this.$t('codelib.评论事件'), id: 'NOTE' },
            //             { name: this.$t('codelib.评审事件'), id: 'REVIEW' }
            //         ],
            //         CODE_GITLAB: [
            //             { name: this.$t('codelib.推送事件'), id: 'PUSH' },
            //             { name: this.$t('codelib.标签推送事件'), id: 'TAG_PUSH' },
            //             { name: this.$t('codelib.合并请求事件'), id: 'MERGE_REQUEST' },
            //             { name: this.$t('codelib.议题事件'), id: 'ISSUES' },
            //             { name: this.$t('codelib.评论事件'), id: 'NOTE' },
            //             { name: this.$t('codelib.评审事件'), id: 'REVIEW' }
            //         ],
            //         CODE_TGIT: [
            //             { name: this.$t('codelib.推送事件'), id: 'PUSH' },
            //             { name: this.$t('codelib.标签推送事件'), id: 'TAG_PUSH' },
            //             { name: this.$t('codelib.合并请求事件'), id: 'MERGE_REQUEST' },
            //             { name: this.$t('codelib.议题事件'), id: 'ISSUES' },
            //             { name: this.$t('codelib.评论事件'), id: 'NOTE' },
            //             { name: this.$t('codelib.评审事件'), id: 'REVIEW' }
            //         ],
            //         CODE_P4: [
            //             { name: this.$t('codelib.CHANGE_COMMIT事件'), id: 'CHANGE_COMMIT' },
            //             { name: this.$t('codelib.PUSH_SUBMIT事件'), id: '.PUSH_SUBMIT' },
            //             { name: this.$t('codelib.CHANGE_CONTENT事件'), id: 'CHANGE_CONTENT' },
            //             { name: this.$t('codelib.CHANGE_SUBMIT事件'), id: 'CHANGE_SUBMIT' },
            //             { name: this.$t('codelib.PUSH_CONTENT事件'), id: 'PUSH_CONTENT' },
            //             { name: this.$t('codelib.PUSH_COMMIT事件'), id: 'PUSH_COMMIT' },
            //             { name: this.$t('codelib.FIX_ADD事件'), id: 'FIX_ADD' },
            //             { name: this.$t('codelib.FIX_DELETE事件'), id: 'FIX_DELETE' },
            //             { name: this.$t('codelib.FORM_COMMIT事件'), id: 'FORM_COMMIT' },
            //             { name: this.$t('codelib.SHELVE_COMMIT事件'), id: 'SHELVE_COMMIT' },
            //             { name: this.$t('codelib.SHELVE_DELETE事件'), id: 'SHELVE_DELETE' },
            //             { name: this.$t('codelib.SHELVE_SUBMIT事件'), id: 'SHELVE_SUBMIT' }
            //         ]
            //     }
            //     return listMap[this.triggerType] || []
            // },
            searchList () {
                const list = [
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
                        id: 'pipelineName'
                    }
                ]
                return list.filter((data) => {
                    return !this.searchValue.find(val => val.id === data.id)
                })
            },
            isSearch () {
                return this.daterange[0] || this.searchValue.length
            }
        },
        watch: {
            async repoId (id) {
                await this.resetFilter()
                this.catchRepoId = id
            },
            daterange () {
                this.page = 1
                this.hasLoadEnd = false
                this.eventList = []
                this.timelineMap = {}
                if (this.catchRepoId === this.repoId) {
                    this.getListData()
                }
            },
            searchValue () {
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
            this.getEventTypeList()
            this.getTriggerTypeList()
            this.getListData()
        },
        methods: {
            ...mapActions('codelib', [
                'fetchTriggerEventList',
                'fetchEventType',
                'fetchTriggerType'
            ]),
            getEventTypeList () {
                this.fetchEventType().then(res => {
                    this.eventTypeList = res.map(i => {
                        return {
                            ...i,
                            name: i.value
                        }
                    })
                })
            },
            getTriggerTypeList () {
                this.fetchTriggerType().then(res => {
                    this.triggerTypeList = res.map(i => {
                        return {
                            ...i,
                            name: i.value
                        }
                    })
                })
            },
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
                    triggerType: params.triggerType || this.triggerType,
                    startTimeEndTime: daterange[0],
                    endTimeStartTime: daterange[1]
                }).then(res => {
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

                    this.hasLoadEnd = res.count === this.eventList.length
                    console.log(this.hasLoadEnd, 'hasLoadEnd')
                    this.page += 1
                }).finally(() => {
                    this.pageLoading = false
                    this.isLoadingMore = false
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
        .date-picker {
            max-width: 300px;
            min-width: 200px;
        }
        .search-select {
            width: 100%;
        }
        .timeline-warpper {
            height: calc(100% - 32px);
            padding-top: 20px;
            overflow-y: scroll;
            &::-webkit-scrollbar {
                background-color: #fff;
                height: 0px !important;
                width: 0px !important;
            }
        }
    }
    .trigger-timeline {
        padding: 8px;
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
</style>

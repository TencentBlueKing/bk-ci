<template>
    <InfiniteScroll
        ref="infiniteScroll"
        class="trigger-event-wrapper"
        :data-fetcher="fetchTriggerEventList"
        :page-size="pageSize"
        scroll-box-class-name="trigger-event-timeline"
        v-slot="{
            queryList,
            isLoadingMore,
            list
        }"
    >
        <header class="trigger-event-filter-bar">
            <bk-date-picker
                v-model="dateTimeRange"
                :placeholder="$t('pickTimeRange')"
                :shortcuts="shortcuts"
                type="datetimerange"
                @change="handleFilterChange(queryList)"
            />
            <search-select
                :placeholder="filterPlaceHolder"
                :data="filterData"
                v-model="searchKey"
                @change="handleFilterChange(queryList)"
            >
            </search-select>
        </header>
        <div class="trigger-event-timeline">
            <trigger-event-timeline v-if="list.length > 0 || isLoadingMore" :list="list" />
            <empty-exception v-else :type="emptyType" @clear="clearFilter(queryList)" />
        </div>

    </InfiniteScroll>
</template>

<script>
    import InfiniteScroll from '@/components/InfiniteScroll'
    import EmptyException from '@/components/common/exception'
    import SearchSelect from '@blueking/search-select'
    import { mapActions } from 'vuex'
    import { weekAgo } from '@/utils/util'
    import TriggerEventTimeline from './TriggerEventTimeline.vue'

    import '@blueking/search-select/dist/styles/index.css'
    const DEFAULT_DATE_RANGE = weekAgo()
    export default {
        components: {
            SearchSelect,
            InfiniteScroll,
            EmptyException,
            TriggerEventTimeline
        },
        data () {
            return {
                dateTimeRange: [...DEFAULT_DATE_RANGE],
                searchKey: [],
                triggerEventList: [],
                triggerTypeList: [],
                eventTypeList: [],
                page: 1,
                pageSize: 36,
                isLoading: false
            }
        },
        computed: {
            filterData () {
                return [
                    {
                        name: this.$t('eventID'),
                        id: 'eventId'
                    },
                    {
                        name: this.$t('triggerType'),
                        id: 'triggerType',
                        children: this.triggerTypeList
                    },
                    {
                        name: this.$t('eventType'),
                        id: 'eventType',
                        multiable: true,
                        children: this.eventTypeList
                    },
                    {
                        name: this.$t('details.trigger'),
                        id: 'triggerUser'
                    }
                ]
            },
            filterPlaceHolder () {
                return this.filterData.map(item => item.name).join(' / ')
            },
            isSearching () {
                return this.searchKey?.length > 0 || this.dateTimeRange.some(item => !!item)
            },
            emptyType () {
                return this.isSearching ? 'search-empty' : 'empty'
            },
            shortcuts () {
                return [{
                            text: this.$t('今天'),
                            value () {
                                const end = new Date()
                                const start = new Date(end.getFullYear(), end.getMonth(), end.getDate())
                                return [start, end]
                            }
                        },
                        {
                            text: this.$t('昨天'),
                            value () {
                                const time = new Date()
                                const end = new Date(time.getFullYear(), time.getMonth(), time.getDate() - 1, 23, 59, 59)
                                const start = new Date(time.getFullYear(), time.getMonth(), time.getDate() - 1)
                                return [start, end]
                            }
                        },
                        {
                            text: this.$t('近3天'),
                            value () {
                                const end = new Date()
                                const start = new Date()
                                start.setTime(start.getTime() - 3600 * 1000 * 24 * 3)
                                return [start, end]
                            }
                        },
                        {
                            text: this.$t('近7天'),
                            value () {
                                const end = new Date()
                                const start = new Date()
                                start.setTime(start.getTime() - 3600 * 1000 * 24 * 7)
                                return [start, end]
                            }
                        }]
            }
        },
        watch: {
            '$route.params.pipelineId': {
                handler (val) {
                    this.$nextTick(() => {
                        this.$refs.infiniteScroll?.updateList?.()
                    })
                }
            }
        },
        beforeMount () {
            this.init()
        },
        methods: {
            ...mapActions('pipelines', [
                'getTriggerEventList',
                'getTriggerTypeList',
                'getEventTypeList'
            ]),
            async init () {
                try {
                    this.searchKey = this.filterData.reduce((acc, cur) => {
                        const valuesText = this.$route.query[cur.id]
                        if (valuesText) {
                            acc.push({
                                ...cur, values: [{ id: valuesText, name: valuesText }]
                            })
                        }
                        return acc
                    }, [])

                    const [
                        triggerTypeList,
                        eventTypeList
                    ] = await Promise.all([
                        this.getTriggerTypeList(),
                        this.getEventTypeList()
                    ])

                    this.triggerTypeList = triggerTypeList.map(item => ({
                        id: item.id,
                        name: item.value
                    }))
                    this.eventTypeList = eventTypeList.map(item => ({
                        id: item.id,
                        name: item.value
                    }))
                } catch (error) {
                    console.error(error)
                }
            },
            async fetchTriggerEventList (page, pageSize) {
                try {
                    this.isLoading = true
                    const params = {
                        projectId: this.$route.params.projectId,
                        pipelineId: this.$route.params.pipelineId,
                        ...this.getSearchQuery(),
                        page,
                        pageSize
                    }
                    const res = await this.getTriggerEventList(params)
                    return res
                } catch (error) {
                    console.error(error)
                } finally {
                    this.isLoading = false
                }
            },
            handleFilterChange (queryList) {
                this.$nextTick(() => {
                    queryList(1)
                })
            },
            getSearchQuery () {
                return this.searchKey.reduce((acc, item) => {
                    acc[item.id] = item.values.map(value => value.id).join(',')
                    return acc
                }, {
                    startTime: this.dateTimeRange[0] ? +(new Date(this.dateTimeRange[0])) : undefined,
                    endTime: this.dateTimeRange[1] ? +(new Date(this.dateTimeRange[1])) : undefined
                })
            },
            clearFilter (queryList) {
                this.dateTimeRange = []
                this.searchKey = []

                this.$nextTick(() => {
                    queryList(1)
                })
            }

        }
    }
</script>

<style lang="scss">
    .trigger-event-wrapper {
        display: flex;
        flex-direction: column;
        padding: 24px;
        height: 100%;
        overflow: auto;
        background: white;
        .trigger-event-filter-bar {
            display: grid;
            grid-gap: 10px;
            grid-template-columns: 342px 1fr;
            margin-bottom: 16px;
            flex-shrink: 0;
            ::placeholder {
                color: #979BA5;
            }
        }
        .trigger-event-timeline {
            display: flex;
            flex: 1;
            overflow: scroll;
            align-items: center;
            padding: 0 12px;
            align-items: center;
            .bk-timeline {
                align-self: flex-start;
                width: 100%;
                .bk-timeline-dot .bk-timeline-content {
                    max-width: none; // TODO: hack
                }
            }
        }
    }
</style>

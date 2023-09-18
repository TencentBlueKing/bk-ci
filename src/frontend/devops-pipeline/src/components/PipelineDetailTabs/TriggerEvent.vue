<template>
    <InfiniteScroll
        ref="infiniteScroll"
        class="trigger-event-wrapper"
        :data-fetcher="fetchTriggerEventList"
        :page-size="pageSize"
        scroll-box-class-name="trigger-event-timeline"
        v-slot="{
            queryList,
            list
        }"
    >
        <header class="trigger-event-filter-bar">
            <bk-date-picker
                v-model="dateTimeRange"
                :placeholder="$t('pickTimeRange')"
                type="datetimerange"
                @change="handleFilterChange(queryList)"
            />
            <search-select
                :placeholder="$t('triggerEventfilterTips')"
                :data="filterData"
                v-model="searchKey"
                @change="handleFilterChange(queryList)"
            >
            </search-select>
        </header>
        <div class="trigger-event-timeline">
            <bk-exception v-if="isSearching && list.length === 0" type="search-empty" scene="part" />
            <bk-exception v-else-if="list.length === 0" type="empty" scene="part" />
            <bk-timeline
                v-if="list.length > 0"
                :list="getTimelineList(list)"
            />
        </div>

    </InfiniteScroll>
</template>

<script>
    import { mapActions } from 'vuex'
    import TriggerEventChildren from './TriggerEventChildren.vue'
    import InfiniteScroll from '@/components/InfiniteScroll'
    import SearchSelect from '@blueking/search-select'
    import moment from 'moment'

    import '@blueking/search-select/dist/styles/index.css'
    export default {
        components: {
            // eslint-disable-next-line vue/no-unused-components
            TriggerEventChildren,
            SearchSelect,
            InfiniteScroll
        },
        data () {
            return {
                dateTimeRange: [],
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
                        name: this.$t('triggerType'),
                        id: 'triggerType',
                        multiable: true,
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
            isSearching () {
                console.log(this.searchKey, this.dateTimeRange)
                return this.searchKey?.length > 0 || this.dateTimeRange.some(item => !!item)
            }
        },
        created () {
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
            getTimelineList (originList) {
                const dateMap = originList.reduce((acc, item) => {
                    const date = moment(item.eventTime).format('YYYY-MM-DD')
                    console.log(date)
                    if (!acc.has(date)) {
                        acc.set(date, [])
                    }
                    acc.get(date).push(item)
                    return acc
                }, new Map())
                const list = []
                dateMap.forEach((events, date) => {
                    list.push({
                        tag: date,
                        content: this.$createElement(TriggerEventChildren, {
                            props: {
                                events
                            }
                        })
                    })
                })
                console.log(originList, list)
                return list
            },
            handleFilterChange (queryList) {
                console.log('change')
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
                .bk-timeline-dot .bk-timeline-content {
                    max-width: none; // TODO: hack
                }
            }
        }
    }
</style>

<template>
    <div class="build-history-filter-bar">
        <bk-date-picker
            :value="historyPageStatus.dateTimeRange"
            :placeholder="$t('pickTimeRange')"
            type="datetimerange"
            @change="handleDateRangeChange"
        />
        <search-select
            class="pipeline-history-search-select"
            :placeholder="filterTips"
            :data="filterData"
            :values="historyPageStatus.searchKey"
            @change="updateSearchKey"
        >
        </search-select>
    </div>
</template>

<script>
    import { PROCESS_API_URL_PREFIX } from '@/store/constants'
    import { coverStrTimer } from '@/utils/util'
    import SearchSelect from '@blueking/search-select'
    import moment from 'moment'
    import { mapActions, mapGetters } from 'vuex'

    import '@blueking/search-select/dist/styles/index.css'

    export default {
        name: 'filter-bar',
        components: {
            SearchSelect
        },
        props: {
            resetQueryCondition: Function
        },
        data () {
            return {
                triggerList: [],
                statusList: [],
                repoList: [],
                branchList: []
            }
        },
        computed: {
            ...mapGetters({
                historyPageStatus: 'pipelines/getHistoryPageStatus'
            }),
            conditionsMap () {
                return {
                    trigger: this.triggerList,
                    status: this.statusList,
                    materialAlias: this.repoList,
                    materialBranch: this.branchList
                }
            },
            datePickerConf () {
                return {
                    format: 'yyyy-MM-dd HH:mm:ss',
                    type: 'datetimerange'
                }
            },
            filterData () {
                return [
                    {
                        name: this.$t('status'),
                        id: 'status',
                        multiable: true,
                        children: this.statusList
                    },
                    {
                        name: this.$t('materialRepo'),
                        id: 'materialAlias',
                        multiable: true,
                        children: this.repoList
                    },
                    {
                        name: this.$t('triggerRepo'),
                        id: 'triggerRepo',
                        multiable: true,
                        children: this.repoList
                    },
                    {
                        name: 'Commit ID',
                        id: 'materialCommitId'
                    },
                    {
                        name: 'Commit Message',
                        id: 'materialCommitMessage'
                    },
                    {
                        name: this.$t('history.triggerType'),
                        id: 'trigger',
                        multiable: true,
                        children: this.triggerList
                    },
                    {
                        name: this.$t('materialBranch'),
                        id: 'materialBranch',
                        multiable: true,
                        children: this.branchList
                    },
                    {
                        name: this.$t('triggerBranch'),
                        id: 'triggerBranch',
                        multiable: true,
                        children: this.branchList
                    },
                    {
                        name: this.$t('history.remark'),
                        id: 'remark'
                    }
                ]
            },
            filterTips () {
                return this.filterData.map(item => item.name).join('/')
            }
        },
        created () {
            this.init()
            this.handlePathQuery()
        },
        methods: {
            ...mapActions('pipelines', [
                'setHistoryPageStatus'
            ]),
            async init () {
                try {
                    const [statusList, repoList, branchList, triggerList] = await Promise.all([
                        'status',
                        'repo',
                        `branchName?materialAlias=${this.$route.query.materialAlias ?? ''}`,
                        'trigger'
                    ].map(this.getConditionList))
                    this.statusList = statusList.map(item => ({
                        name: item.value,
                        id: item.id
                    }))
                    this.repoList = repoList.map(item => ({
                        name: item,
                        id: item
                    }))
                    this.branchList = branchList.map(item => ({
                        name: item,
                        id: item
                    }))
                    this.triggerList = triggerList.map(item => ({
                        name: item.value,
                        id: item.id
                    }))
                    this.historyPageStatus.searchKey.forEach(item => {
                        if (this.conditionsMap[item.id]) {
                            item.values = item.values.map(item => ({
                                id: item,
                                name: this.conditionsMap[item.id].find(val => val.id === item)?.name ?? 'unknown'
                            }))
                        }
                    })
                } catch (error) {
                    console.error(error)
                }
            },
            async handlePathQuery () {
                // TODO 筛选参数目前不支持带#字符串回填
                const { $route, historyPageStatus } = this
                const pathQuery = $route.query
                const queryArr = Object.keys(pathQuery)
    
                if (queryArr.length) {
                    const hasTimeRange = queryArr.includes('startTimeStartTime') && queryArr.includes('endTimeEndTime')
                    const newSearchKey = queryArr.map(key => {
                        const newItem = this.filterData.find(item => item.id === key)
                        if (!newItem) return null
                        newItem.values = newItem.multiable
                            ? pathQuery[key].split(',').map(v => ({
                                id: v,
                                name: v
                            }))
                            : [{ id: pathQuery[key], name: pathQuery[key] }]
                        return newItem
                    }).filter(item => !!item)
                    
                    this.setHistoryPageStatus({
                        dateTimeRange: hasTimeRange
                            ? [
                                coverStrTimer(parseInt(pathQuery.startTimeStartTime)),
                                coverStrTimer(parseInt(pathQuery.endTimeEndTime))
                            ]
                            : [],
                        query: {
                            ...historyPageStatus.query,
                            ...(hasTimeRange
                                ? {
                                    startTimeStartTime: pathQuery.startTimeStartTime,
                                    endTimeEndTime: pathQuery.endTimeEndTime
                                }
                                : {})
                        },
                        searchKey: newSearchKey
                    })
                }
            },
            formatTime (date) {
                try {
                    return moment(date).valueOf() || ''
                } catch (e) {
                    return ''
                }
            },
            handleDateRangeChange (value) {
                const name = 'dateTimeRange'
                const { startTimeStartTime, endTimeEndTime, ...newQuery } = this.historyPageStatus.query
                const startTime = this.formatTime(value[0])
                const endTime = this.formatTime(value[1])
                if (startTime) {
                    newQuery.startTimeStartTime = [startTime]
                }
                if (endTime) {
                    newQuery.endTimeEndTime = [endTime]
                }
                this.setHistoryPageStatus({
                    [name]: value,
                    query: newQuery
                })
                this.startQuery()
            },

            async getConditionList (condition) {
                try {
                    const { $route: { params }, $ajax } = this
                    const url = `${PROCESS_API_URL_PREFIX}/user/builds/${params.projectId}/${params.pipelineId}/historyCondition/${condition}`
                    const res = await $ajax.get(url)

                    return res.data
                } catch (e) {
                    console.error(e)
                }
            },
            startQuery () {
                this.$emit('query')
            },
            updateSearchKey (searchKey) {
                this.setHistoryPageStatus({
                    searchKey
                })
                this.startQuery()
            }
        }
    }
</script>

<style lang="scss">

    .build-history-filter-bar {
        display: grid;
        align-items: center;
        grid-gap: 8px;
        grid-auto-flow: column;
        grid-template-columns: min-content 1fr;
        margin: 0 0 16px 0;
        max-height: 66px;
        flex-shrink: 0;

        .pipeline-history-search-select {
            background-color: white;
            ::placeholder {
                color: #c4c6cc;
            }
        }

        .search-history-btn {
            cursor: pointer;
            padding: 0 10px;
            font-size: 14px;
            &:hover {
                color: #3c96ff;
            }
        }
    }
</style>

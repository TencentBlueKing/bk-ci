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
                statusList: [],
                triggerList: []
            }
        },
        computed: {
            ...mapGetters({
                historyPageStatus: 'pipelines/getHistoryPageStatus'
            }),
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
                        children: this.statusList.map(item => ({
                            id: item.id,
                            name: item.value
                        }))
                    },
                    {
                        name: this.$t('materialRepo'),
                        id: 'materialAlias',
                        // multiable: true,
                        remoteMethod:
                            async (search) => {
                                const repoList = await this.getConditionList('repo', {
                                    type: 'MATERIAL',
                                    search
                                })
                                return repoList.map(item => ({
                                    name: item,
                                    id: item
                                }))
                            },
                        inputInclude: true
                    },
                    {
                        name: this.$t('triggerRepo'),
                        id: 'triggerAlias',
                        // multiable: true,
                        remoteMethod: async (search) => {
                            const repoList = await this.getConditionList('repo', {
                                type: 'TRIGGER',
                                search
                            })
                            return repoList.map(item => ({
                                name: item,
                                id: item
                            }))
                        },
                        inputInclude: true
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
                        name: this.$t('details.trigger'),
                        id: 'triggerUser'
                    },
                    {
                        name: this.$t('history.triggerType'),
                        id: 'trigger',
                        multiable: true,
                        children: this.triggerList.map(item => ({
                            id: item.id,
                            name: item.value
                        }))
                    },
                    {
                        name: this.$t('materialBranch'),
                        id: 'materialBranch',
                        // multiable: true,
                        remoteMethod: async (search) => {
                            const repoList = await this.getConditionList('branchName', {
                                type: 'MATERIAL',
                                alias: this.getSearchKeyById('materialAlias'),
                                search
                            })
                            return repoList.map(item => ({
                                name: item,
                                id: item
                            }))
                        },
                        inputInclude: true
                    },
                    {
                        name: this.$t('triggerBranch'),
                        id: 'triggerBranch',
                        // multiable: true,
                        remoteMethod: async (search) => {
                            const repoList = await this.getConditionList('branchName', {
                                type: 'TRIGGER',
                                alias: this.getSearchKeyById('triggerAlias'),
                                search
                            })
                            return repoList.map(item => ({
                                name: item,
                                id: item
                            }))
                        },
                        inputInclude: true
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
        },
        methods: {
            ...mapActions('pipelines', [
                'setHistoryPageStatus'
            ]),
            async init () {
                try {
                    const [statusList, triggerList] = await Promise.all([
                        'status',
                        'trigger'
                    ].map(this.getConditionList))
                    const conditionsMap = {
                        status: statusList,
                        trigger: triggerList
                    }
                    this.statusList = statusList
                    this.triggerList = triggerList
                    this.handlePathQuery(conditionsMap)
                } catch (error) {
                    console.error(error)
                }
            },
            handlePathQuery (conditionsMap) {
                // TODO 筛选参数目前不支持带#字符串回填
                const { $route, historyPageStatus } = this
                const pathQuery = $route.query
                const queryArr = Object.keys(pathQuery)
                const page = pathQuery?.page ? parseInt(pathQuery?.page, 10) : 1
                const pageSize = pathQuery?.pageSize ? parseInt(pathQuery?.pageSize, 10) : 20

                if (queryArr.length) {
                    const hasTimeRange = queryArr.includes('startTimeStartTime') && queryArr.includes('endTimeEndTime')
                    const newSearchKey = queryArr.map(key => {
                        const newItem = this.filterData.find(item => item.id === key)
                        if (!newItem) return null
                        const valueMap = conditionsMap[key]?.reduce((acc, item) => {
                                acc[item.id] = item.value
                                return acc
                            }, {})

                        newItem.values = newItem.multiable
                            ? pathQuery[key].split(',').map(v => ({
                                id: v,
                                name: valueMap?.[v] ?? v
                            }))
                            : [{ id: pathQuery[key], name: valueMap?.[pathQuery[key]] ?? pathQuery[key] }]
                        return newItem
                    }).filter(item => !!item)

                    this.setHistoryPageStatus({
                        page,
                        pageSize,
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
                                : {}),
                            ...(pathQuery.archiveFlag ? { archiveFlag: pathQuery.archiveFlag } : {})
                        },
                        searchKey: newSearchKey
                    })
                }
                this.startQuery(page)
            },
            formatTime (date) {
                try {
                    return +new Date(date)
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

            async getConditionList (condition, query = {}) {
                try {
                    const { $route: { params }, $ajax } = this
                    const querySearch = new URLSearchParams(query)
                    const url = `${PROCESS_API_URL_PREFIX}/user/builds/${params.projectId}/${params.pipelineId}/historyCondition/${condition}?${querySearch}`
                    const res = await $ajax.get(url)

                    return res.data
                } catch (e) {
                    console.error(e)
                }
            },
            startQuery (page = 1) {
                this.$emit('query', page)
            },
            updateSearchKey (searchKey) {
                this.setHistoryPageStatus({
                    searchKey
                })
                this.startQuery()
            },
            getSearchKeyById (id) {
                try {
                    const values = this.historyPageStatus.searchKey.find(item => item.id === id)?.values
                    return Array.isArray(values) ? values.map(i => i.id).join(',') : ''
                } catch (error) {
                    return ''
                }
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

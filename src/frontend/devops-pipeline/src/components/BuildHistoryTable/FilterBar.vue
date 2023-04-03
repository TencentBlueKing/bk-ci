<template>
    <div class="build-history-filter-bar">
        <div class="history-filter-select" v-for="item in filterConfig" :key="item.id">
            <label :for="item.id">{{item.label}}：</label>
            <component :is="item.component"
                :style="`width: ${item.width}px`"
                :name="item.name"
                :id="item.id"
                :value="item.value"
                :options-conf="item.optionsConf"
                :date-picker-conf="item.datePickerConf"
                :handle-change="item.handleChange"
            />
        </div>
        <bk-search-select
            class="search-select"
            :placeholder="$t('history.filterTips')"
            :data="filterData"
            :show-condition="false"
            :strink="false"
            display-key="value"
            :values="searchKey"
            :remote-method="handleRemoteMethod"
            @input-change="handleSearchInput"
            @change="updateSearchKey"
        ></bk-search-select>
        <span type="default" class="search-history-btn" @click="resetQueryCondition">{{ $t('history.reset') }}</span>
    </div>
</template>

<script>
    import Selector from '@/components/AtomFormComponent/Selector'
    import SelectInput from '@/components/AtomFormComponent/SelectInput'
    import TimePicker from '@/components/AtomFormComponent/TimePicker'
    import { PROCESS_API_URL_PREFIX } from '@/store/constants'
    import { getQueryParamList, debounce } from '../../utils/util'
    import moment from 'moment'

    export default {
        name: 'filter-bar',
        components: {
            Selector,
            SelectInput,
            TimePicker
        },
        props: {
            setHistoryPageStatus: Function,
            resetQueryCondition: Function,
            query: Array,
            searchKey: Array
        },
        data () {
            return {
                triggerList: []
            }
        },
        computed: {
            filterConfig () {
                return [{
                    id: 'status',
                    label: this.$t('status'),
                    name: 'status',
                    value: this.query.status,
                    width: 160,
                    handleChange: this.handleFilterItemChange,
                    component: 'selector',
                    optionsConf: {
                        url: `${PROCESS_API_URL_PREFIX}/user/builds/{projectId}/{pipelineId}/historyCondition/status`,
                        searchable: true,
                        multiple: true,
                        clearable: true,
                        placeholder: this.$t('status'),
                        searchPlaceholder: this.$t('status'),
                        paramName: 'value'
                    }

                }, {
                    id: 'materialAlias',
                    label: this.$t('history.repo'),
                    name: 'materialAlias',
                    width: 220,
                    value: this.query.materialAlias,
                    component: 'select-input',
                    handleChange: debounce((...args) => {
                        this.query.materialBranch = []
                        this.handleFilterItemChange(...args)
                    }, 500),
                    optionsConf: {
                        url: `${PROCESS_API_URL_PREFIX}/user/builds/{projectId}/{pipelineId}/historyCondition/repo`,
                        searchable: true,
                        multiple: true,
                        clearable: true,
                        placeholder: 'materialAlias'
                    }

                }, {
                    id: 'materialBranch',
                    label: this.$t('history.branch'),
                    name: 'materialBranch',
                    width: 160,
                    value: this.query.materialBranch,
                    component: 'select-input',
                    handleChange: debounce(this.handleFilterItemChange, 500),
                    optionsConf: {
                        url: `${PROCESS_API_URL_PREFIX}/user/builds/{projectId}/{pipelineId}/historyCondition/branchName?${getQueryParamList(this.query.materialAlias, 'alias')}`,
                        searchable: true,
                        multiple: true,
                        clearable: true,
                        placeholder: 'materialBranch'
                    }
                }, {
                    id: 'dateTimeRange',
                    label: this.$t('history.date'),
                    name: 'dateTimeRange',
                    width: 320,
                    value: this.query.dateTimeRange,
                    component: 'time-picker',
                    handleChange: debounce(this.handleDateRangeChange, 500),
                    datePickerConf: {
                        format: 'yyyy-MM-dd HH:mm:ss',
                        type: 'datetimerange'
                    }
                }]
            },
            filterData () {
                return [
                    {
                        value: 'commitid',
                        id: 'materialCommitId'
                    },
                    {
                        value: 'commitMessage',
                        id: 'materialCommitMessage'
                    },
                    {
                        value: this.$t('history.triggerType'),
                        id: 'trigger',
                        remote: true,
                        multiable: true,
                        children: this.triggerList
                    },
                    {
                        value: this.$t('history.remark'),
                        id: 'remark'
                    }
                ]
            }
        },
        methods: {
            handleFilterItemChange (name, value) {
                this.setHistoryPageStatus({
                    queryMap: {
                        searchKey: this.searchKey,
                        query: {
                            ...this.query,
                            [name]: value
                        }
                    }
                })
                this.startQuery()
            },
            handleDateRangeChange (name, value) {
                const newQuery = {
                    ...this.query,
                    [name]: value,
                    startTimeStartTime: undefined,
                    endTimeEndTime: undefined
                }
                if (!!value[0] && !!value[1]) {
                    const startTime = moment(value[0]).valueOf() || ''
                    const endTime = moment(value[1]).valueOf() || ''

                    newQuery.startTimeStartTime = [startTime]
                    newQuery.endTimeEndTime = [endTime]
                }
                this.setHistoryPageStatus({
                    queryMap: {
                        searchKey: this.searchKey,
                        query: newQuery
                    }
                })

                this.startQuery()
            },
            async handleRemoteMethod (...args) {
                if (this.triggerList.length > 0) return this.triggerList
                try {
                    const { $route: { params }, $ajax } = this
                    const url = `${PROCESS_API_URL_PREFIX}/user/builds/${params.projectId}/${params.pipelineId}/historyCondition/trigger`
                    const res = await $ajax.get(url)
                    this.triggerList = res.data
                    return res.data
                } catch (e) {

                }
            },
            startQuery () {
                this.$emit('query')
            },
            updateSearchKey (searchKey) {
                this.setHistoryPageStatus({
                    queryMap: {
                        searchKey,
                        query: this.query
                    }
                })
                this.startQuery()
            }
        }
    }
</script>

<style lang="scss">
    div {
        outline: none;
    }
    .build-history-filter-bar {
        display: flex;
        align-items: center;
        margin: 0 0 10px 0;
        max-height: 66px;
        .history-filter-select {
            font-size: 12px;
            display: flex;
            align-items: center;
            margin-right: 12px;
            .bk-selector{
                flex: 1;
            }
        }
        .search-select {
            flex: 1;
            margin-right: 12px
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

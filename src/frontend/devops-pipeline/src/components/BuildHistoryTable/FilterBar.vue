<template>
    <div class="build-history-filter-bar">
        <div class="history-filter-select" v-for="item in filterConfig" :key="item.id">
            <label :for="item.id">{{item.label}}：</label>
            <selector
                :style="`width: ${item.width}px`"
                :name="item.name"
                :id="item.id"
                :value="item.value"
                :options-conf="item.optionsConf"
                :handle-change="item.handleChange"
            />
        </div>
        <bk-search-select
            class="search-select"
            placeholder="支持commitid等关键字过滤"
            :data="filterData"
            :show-condition="false"
            :strink="false"
            display-key="value"
            :values="searchKey"
            :remote-method="handleRemoteMethod"
            @input-change="handleSearchInput"
            @change="updateSearchKey"
        ></bk-search-select>
        <span type="default" class="search-history-btn" @click="resetQueryCondition">重置</span>
    </div>
</template>

<script>
    import Selector from '@/components/AtomFormComponent/Selector'
    import { PROCESS_API_URL_PREFIX } from '@/store/constants'
    import { getQueryParamList } from '../../utils/util'
    
    export default {
        name: 'filter-bar',
        components: {
            Selector
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
                    label: '状态',
                    name: 'status',
                    value: this.query.status,
                    width: 220,
                    handleChange: this.handleFilterItemChange,
                    optionsConf: {
                        url: `${PROCESS_API_URL_PREFIX}/user/builds/{projectId}/{pipelineId}/historyCondition/status`,
                        searchable: true,
                        multiple: true,
                        clearable: true,
                        placeholder: 'status',
                        paramName: 'value'
                    }
                    
                }, {
                    id: 'materialAlias',
                    label: '代码库',
                    name: 'materialAlias',
                    width: 320,
                    value: this.query.materialAlias,
                    handleChange: (...args) => {
                        this.query.materialBranch = []
                        this.handleFilterItemChange(...args)
                    },
                    optionsConf: {
                        url: `${PROCESS_API_URL_PREFIX}/user/builds/{projectId}/{pipelineId}/historyCondition/repo`,
                        searchable: true,
                        multiple: true,
                        clearable: true,
                        placeholder: 'materialAlias'
                    }
                    
                }, {
                    id: 'materialBranch',
                    label: '分支',
                    name: 'materialBranch',
                    width: 220,
                    value: this.query.materialBranch,
                    handleChange: this.handleFilterItemChange,
                    optionsConf: {
                        url: `${PROCESS_API_URL_PREFIX}/user/builds/{projectId}/{pipelineId}/historyCondition/branchName?${getQueryParamList(this.query.materialAlias, 'alias')}`,
                        searchable: true,
                        multiple: true,
                        clearable: true,
                        placeholder: 'materialBranch'
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
                        value: '触发方式',
                        id: 'trigger',
                        remote: true,
                        multiable: true,
                        children: this.triggerList
                    },
                    {
                        value: '备注',
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
            display: flex;
            align-items: center;
            margin-right: 12px;
            .bk-selector{
                flex: 1;
                // &.open {
                //     background-color: #5C6270;
                //     .bk-selector-input {
                //         color: white;
                //     }
                // }
            }
            // .bk-selector-input {
            //     flex: 1;
            //     border: 0;
            //     background-color: transparent;
            // }
        }
        .search-select {
            flex: 1;
            margin-right: 12px
        }
        .search-history-btn {
            cursor: pointer;
            padding: 0 10px;
            &:hover {
                color: #3c96ff;
            }
        }
    }
</style>

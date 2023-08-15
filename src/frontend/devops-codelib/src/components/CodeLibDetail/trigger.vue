<template>
    <div class="trigger-wrapper">
        <bk-search-select
            class="search-select"
            v-model="searchValue"
            :data="searchList"
            clearable
            :show-condition="false"
            :placeholder="$t('codelib.触发器类型/事件类型')"
        >
        </bk-search-select>
        <bk-table
            v-bkloading="{ isLoading }"
            :data="triggerData"
            :pagination="pagination"
            @page-change="handlePageChange"
            @page-limit-change="handlePageLimitChange"
        >
            <bk-table-column :label="$t('codelib.触发器类型')" prop="triggerType">
                <template slot-scope="{ row }">
                    {{ row.triggerType }}
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('codelib.事件类型')" prop="eventType">
                <template slot-scope="{ row }">
                    {{ row.eventType }}
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('codelib.事件')" prop="event">
                <template slot-scope="{ row }">
                    {{ row.event }}
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('codelib.流水线数量')" prop="pipelineCount">
                <template slot-scope="{ row }">
                    <a>{{ row.pipelineCount }}</a>
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('codelib.操作')" width="150">
                <template slot-scope="{ row }">
                    <bk-button
                        class="mr10"
                        theme="primary"
                        text
                        @click="handelShowDetail(row)"
                    >
                        {{ $t('codelib.详情') }}
                    </bk-button>
                    <bk-button
                        class="mr10"
                        theme="primary"
                        text
                        @click="handelShowDetail(props.row)"
                    >
                        {{ $t('codelib.关联流水线') }}
                    </bk-button>
                </template>
            </bk-table-column>
        </bk-table>
    </div>
</template>

<script>
    import {
        mapActions
    } from 'vuex'
    export default {
        data () {
            return {
                isLoading: false,
                triggerData: [],
                searchValue: [],
                triggerTypeList: [],
                eventTypeList: [],
                pagination: {
                    current: 1,
                    count: 0,
                    limit: 20
                }
            }
        },
        computed: {
            repoId () {
                return this.$route.query.id
            },
            projectId () {
                return this.$route.params.projectId
            },
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
                    }
                ]
                return list.filter((data) => {
                    return !this.searchValue.find(val => val.id === data.id)
                })
            }
        },
        watch: {
            repoId (val) {
                this.pagination.current = 1
                this.fetchTriggerData()
            }
        },
        created () {
            this.getEventTypeList()
            this.getTriggerTypeList()
            this.fetchTriggerData()
        },
        methods: {
            ...mapActions('codelib', [
                'fetchEventType',
                'fetchTriggerType',
                'fetchTriggerData'
            ]),
            /**
             * 获取触发器数据
             */
            async getTriggerData () {
                this.isLoading = true
                await this.fetchTriggerData().then(res => {
                    this.triggerData = res
                }).finally(() => {
                    this.isLoading = false
                })
            },
            /**
             * 获取事件类型
             */
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
            /**
             * 获取触发器类型
             */
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

            handlePageChange (page) {
                this.pagination.current = page
            },

            handlePageLimitChange (limit) {
                this.pagination.limit = limit
            }
        }
    }
</script>

<style lang="scss" scoped>
    .trigger-wrapper {
        .search-select {
            margin-bottom: 16px;
        }
    }
</style>

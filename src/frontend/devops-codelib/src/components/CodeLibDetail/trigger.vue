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
            class="trigger-table"
            :data="triggerData"
            :pagination="pagination"
            max-height="615"
            @page-change="handlePageChange"
            @page-limit-change="handlePageLimitChange"
        >
            <bk-table-column :label="$t('codelib.事件')" prop="eventType">
                <template slot-scope="{ row }">
                    <img style="width: 18px; height: 18px;" :src="`https:${row.atomLogo}`" alt="">
                    {{ row.eventType }}
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('codelib.触发条件')" prop="eventType" show-overflow-tooltip>
                <template slot-scope="{ row }">
                    <div v-if="Object.keys(row.triggerCondition).length">
                        <div v-for="(item, key, index) in row.triggerCondition" :key="index" class="condition-item">
                            <span v-if="['CODE_GIT', 'CODE_GITLAB', 'GITHUB', 'CODE_TGIT'].includes(scmType)">
                                - {{ triggerConditionKeyMap[row.eventType][key] }}:
                            </span>
                            <span v-else>
                                - {{ triggerConditionKeyMap[key] }}:
                            </span>
                            <template v-if="Array.isArray(item)">
                                <span v-for="(i, itemIndex) in item" :key="i">
                                    {{ triggerConditionValueMap[i] || i }}
                                    <span v-if="itemIndex + 1 !== item.length">,</span>
                                </span>
                            </template>
                            <template v-else-if="key.includes('Paths') && item">
                                <!-- 路径 -->
                                <div
                                    v-for="path in item.split(',')"
                                    :key="path"
                                    style="margin-left: 20px;"
                                >
                                    - {{ path }}
                                </div>
                            </template>
                            <span v-else>{{ triggerConditionValueMap[item] || item }}</span>
                        </div>
                    </div>
                    <div v-else>--</div>
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('codelib.流水线数量')" prop="pipelineCount">
                <template slot-scope="{ row }">
                    <!-- <a :href="`/console/pipeline/${projectId}/list`" target="_blank">{{ row.pipelineRefCount }}</a> -->
                    {{ row.pipelineRefCount }}
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
                </template>
            </bk-table-column>
        </bk-table>
        <atom-detail
            ref="atomDetailRef"
            :atom="curAtom">
        </atom-detail>
    </div>
</template>

<script>
    import {
        mapActions
    } from 'vuex'
    import atomDetail from '../atom-detail.vue'
    export default {
        components: {
            atomDetail
        },
        props: {
            curRepo: {
                type: Object,
                default: () => {}
            }
        },
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
                },
                eventType: '',
                triggerType: '',
                curAtom: {}
            }
        },
        computed: {
            repoId () {
                return this.$route.query.id
            },
            projectId () {
                return this.$route.params.projectId
            },
            scmType () {
                return this.curRepo.type || ''
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
            },
            triggerConditionKeyMap () {
                let obj = {}
                console.log(this.scmType, 'this.scmType')
                if (['CODE_GIT', 'CODE_GITLAB', 'GITHUB', 'CODE_TGIT'].includes(this.scmType)) {
                    obj = {
                        PUSH: {
                            branchName: this.$t('codelib.分支'),
                            excludeBranchName: this.$t('codelib.排除分支'),
                            includePaths: this.$t('codelib.路径'),
                            excludePaths: this.$t('codelib.排除路径'),
                            includeUsers: this.$t('codelib.人员'),
                            excludeUsers: this.$t('codelib.排除人员')
                        },
                        PULL_REQUEST: {
                            branchName: this.$t('codelib.分支'),
                            excludeBranchName: this.$t('codelib.排除的目标分支'),
                            includeSourceBranchName: this.$t('codelib.源分支'),
                            excludeSourceBranchName: this.$t('codelib.排除的源分支'),
                            includePaths: this.$t('codelib.路径'),
                            excludePaths: this.$t('codelib.排除路径'),
                            includeUsers: this.$t('codelib.人员'),
                            excludeUsers: this.$t('codelib.排除人员')
                        },
                        MERGE_REQUEST: {
                            branchName: this.$t('codelib.分支'),
                            excludeBranchName: this.$t('codelib.排除的目标分支'),
                            includeSourceBranchName: this.$t('codelib.源分支'),
                            excludeSourceBranchName: this.$t('codelib.排除的源分支'),
                            includePaths: this.$t('codelib.路径'),
                            excludePaths: this.$t('codelib.排除路径'),
                            includeUsers: this.$t('codelib.人员'),
                            excludeUsers: this.$t('codelib.排除人员')
                        },
                        MERGE_REQUEST_ACCEPT: {
                            branchName: this.$t('codelib.分支'),
                            excludeBranchName: this.$t('codelib.排除的目标分支'),
                            includeSourceBranchName: this.$t('codelib.源分支'),
                            excludeSourceBranchName: this.$t('codelib.排除的源分支'),
                            includePaths: this.$t('codelib.路径'),
                            excludePaths: this.$t('codelib.排除路径'),
                            includeUsers: this.$t('codelib.人员'),
                            excludeUsers: this.$t('codelib.排除人员')
                        },
                        TAG_PUSH: {
                            tagName: 'Tag',
                            excludeTagName: this.$t('codelib.排除Tag'),
                            fromBranches: this.$t('codelib.来源分支')
                        },
                        NOTE: {
                            includeNoteComment: this.$t('codelib.评论内容'),
                            includeNoteTypes: this.$t('codelib.评论类型')
                        },
                        REVIEW: {
                            includeCrState: this.$t('codelib.CR状态')
                        },
                        ISSUES: {
                            includeIssueAction: this.$t('codelib.动作')
                        },
                        CREATE: {
                            branchName: this.$t('codelib.要素名'),
                            excludeBranchName: this.$t('codelib.排除要素名'),
                            excludeUsers: this.$t('codelib.排除人员')
                        }
                    }
                } else if (this.scmType === 'CODE_SVN') {
                    obj = {
                        relativePath: this.$t('codelib.相对路径'),
                        excludePaths: this.$t('codelib.排除路径'),
                        includeUsers: this.$t('codelib.人员'),
                        excludeUsers: this.$t('codelib.排除人员')
                    }
                } else if (this.scmType === 'CODE_P4') {
                    obj = {
                        includePaths: this.$t('codelib.路径'),
                        excludePaths: this.$t('codelib.排除路径')
                    }
                }
                return obj
            },
            triggerConditionValueMap () {
                return {
                    approving: this.$t('codelib.评审中'),
                    approved: this.$t('codelib.评审通过'),
                    change_denied: this.$t('codelib.评审被拒绝'),
                    change_required: this.$t('codelib.代码要求修改'),
                    Commit: this.$t('codelib.对提交进行评论'),
                    Review: this.$t('codelib.对评审进行评论'),
                    Issue: this.$t('codelib.对缺陷进行评论'),
                    open: this.$t('codelib.创建'),
                    close: this.$t('codelib.关闭'),
                    reopen: this.$t('codelib.重新打开'),
                    update: this.$t('codelib.更新')
                }
            }
        },
        watch: {
            repoId (val) {
                this.pagination.current = 1
                this.getTriggerData()
            },
            searchValue (val) {
                const paramsMap = {}
                val.forEach(item => {
                    const id = item.id
                    const value = item.values[0].id

                    paramsMap[id] = value
                })
                this.pagination.current = 1
                this.triggerType = paramsMap.triggerType || ''
                this.eventType = paramsMap.eventType || ''
                this.getTriggerData()
            }
        },
        created () {
            this.getEventTypeList()
            this.getTriggerTypeList()
            this.getTriggerData()
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
                await this.fetchTriggerData({
                    projectId: this.projectId,
                    repositoryHashId: this.repoId,
                    page: this.pagination.current,
                    pageSize: this.pagination.limit,
                    triggerType: this.triggerType,
                    eventType: this.eventType
                }).then(res => {
                    this.pagination.count = res.count
                    this.triggerData = res.records
                }).finally(() => {
                    this.isLoading = false
                })
            },
            /**
             * 获取事件类型
             */
            getEventTypeList () {
                this.fetchEventType({
                    scmType: this.scmType
                }).then(res => {
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
                this.fetchTriggerType({
                    scmType: this.scmType
                }).then(res => {
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
                this.getTriggerData()
            },

            handlePageLimitChange (limit) {
                this.pagination.limit = limit
                this.getTriggerData()
            },

            handelShowDetail (row) {
                this.curAtom = row
                this.$refs.atomDetailRef.isShow = true
            }
        }
    }
</script>

<style lang="scss" scoped>
    .trigger-wrapper {
        .search-select {
            margin-bottom: 16px;
        }
        .trigger-table {
            ::v-deep .cell {
                max-height: 500px !important;
                -webkit-line-clamp: 300 !important;
                padding: 10px 15px !important;
            }
            .condition-item {
                line-height: 20px;
            }
        }
    }
</style>

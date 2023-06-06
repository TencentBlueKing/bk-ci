<template>
    <div>
        <div class="expand-btn" v-if="isListFlod" @click="handleExpandList">
            <Icon
                name="angle-double-right"
                class="angle-double-right-icon"
                size="16"
            />
            {{ $t('codelib.expandList') }}
        </div>
        <bk-table
            ref="list"
            v-if="tableHeight"
            v-bkloading="{ isLoading }"
            class="devops-codelib-table"
            :data="records"
            :size="tableSize"
            :height="tableHeight"
            :outer-border="false"
            :row-class-name="rowClassName"
            :pagination="pagination"
            @row-click="handleRowSelect"
            @sort-change="handleSortChange"
            @page-change="handlePageChange"
            @page-limit-change="handlePageLimitChange"
        >
            <bk-table-column
                v-if="allColumnMap.aliasName"
                :label="$t('codelib.aliasName')"
                sortable
                prop="aliasName"
                show-overflow-tooltip
            >
                <template slot-scope="props">
                    <Icon
                        v-if="isListFlod"
                        class="codelib-type-icon"
                        :name="codelibIconMap[props.row.type]"
                        size="16"
                    />
                    <a
                        @click="handleShowDetail(props.row)"
                    >
                        {{ props.row.aliasName }}
                    </a>
                    <span
                        v-if="props.row.enablePac"
                        class="pac-icon"
                    >
                        <Icon name="PACcode" size="22" class="pac-code-icon" />
                        PAC
                    </span>
                </template>
            </bk-table-column>
            <bk-table-column
                v-if="allColumnMap.url"
                :label="$t('codelib.address')"
                sortable
                prop="url"
                show-overflow-tooltip
            >
                <template slot-scope="props">
                    <Icon
                        class="codelib-type-icon"
                        :name="codelibIconMap[props.row.type]"
                        size="16"
                    />
                    <a
                        @click="handleToCodelib(props.row.url)"
                    >
                        {{ props.row.url }}
                    </a>
                </template>
            </bk-table-column>
            <bk-table-column
                v-if="allColumnMap.authType"
                :label="$t('codelib.auth')"
                show-overflow-tooltip
            >
                <template slot-scope="props">
                    <span>
                        {{ props.row.authType }}@
                    </span>
                    <a
                        v-if="!['OAUTH'].includes(props.row.authType)"
                        :href="`/console/ticket/${projectId}/editCredential/${props.row.authIdentity}`"
                        target="_blank"
                    >
                        {{ props.row.authIdentity }}
                    </a>
                    <span v-else>
                        {{ props.row.authIdentity }}
                    </span>
                </template>
            </bk-table-column>
            <bk-table-column
                v-if="allColumnMap.recentlyEditedBy"
                :label="$t('codelib.recentlyEditedBy')"
                prop="updatedUser"
                show-overflow-tooltip
                width="200"
            >
            </bk-table-column>
            <bk-table-column
                v-if="allColumnMap.lastModifiedTime"
                :label="$t('codelib.lastModifiedTime')"
                prop="updatedTime"
                width="250"
            >
                <template slot-scope="props">
                    {{ prettyDateTimeFormat(Number(props.row.updatedTime + '000')) }}
                </template>
            </bk-table-column>
            <bk-table-column
                v-if="!isListFlod"
                :label="$t('codelib.operation')"
                show-overflow-tooltip
                width="100"
            >
                <template slot-scope="props">
                    <bk-button
                        theme="primary"
                        text
                        @click.stop="deleteCodeLib(props.row)"
                    >
                        {{ $t('codelib.delete') }}
                    </bk-button>
                </template>
            </bk-table-column>
            <bk-table-column
                v-if="!isListFlod"
                type="setting"
            >
                <bk-table-setting-content
                    :fields="tableColumn"
                    :selected="selectedTableColumn"
                    :size="tableSize"
                    @setting-change="handleSettingChange" />
            </bk-table-column>
            <template #empty>
                <EmptyTableStatus :type="aliasName ? 'search-empty' : 'empty'" @clear="resetFilter" />
            </template>
        </bk-table>
        <UsingPipelinesDialog
            :pipelines-list="pipelinesList"
            :is-show.sync="pipelinesDialogPayload.isShow"
            :is-loadig-more="pipelinesDialogPayload.isLoadingMore"
            :has-load-end="pipelinesDialogPayload.hasLoadEnd"
            :fetch-pipelines-list="fetchPipelinesList"
        />
    </div>
</template>

<script>
    import { mapActions, mapState } from 'vuex'
    import {
        TABLE_COLUMN_CACHE,
        CODE_REPOSITORY_CACHE,
        listColumnsCache
    } from '../../config/'
    import {
        getOffset,
        prettyDateTimeFormat
    } from '@/utils/'
    import EmptyTableStatus from '../empty-table-status.vue'
    import UsingPipelinesDialog from '../UsingPipelinesDialog.vue'
    
    export default {
        components: {
            EmptyTableStatus,
            UsingPipelinesDialog
        },
        props: {
            switchPage: {
                type: Function,
                required: true
            },
            count: Number,
            totalPages: Number,
            page: Number,
            pageSize: Number,
            defaultPagesize: Number,
            records: {
                type: Array,
                required: true,
                default: () => []
            },
            isListFlod: {
                type: Boolean,
                default: false
            },
            curRepoId: {
                type: String,
                default: ''
            },
            curRepo: {
                type: Object,
                default: () => {}
            },
            limit: {
                type: Number
            },
            aliasName: {
                type: String,
                default: ''
            },
            refreshCodelibList: {
                type: Function
            }
        },

        data () {
            return {
                selectId: '',
                tableHeight: '',
                selectedTableColumn: [],
                tableSize: 'small',
                pagination: {
                    showTotalCount: true,
                    current: this.page,
                    count: this.count,
                    limit: this.pageSize,
                    limitList: [10, 20, 50, 100],
                    small: false
                },
                isLoading: false,
                sortByMap: {
                    aliasName: 'ALIAS_NAME',
                    url: 'URL',
                    type: 'TYPE',
                    null: ''
                },
                sortTypeMap: {
                    ascending: 'ASC',
                    descending: 'DESC',
                    null: ''
                },
                codelibIconMap: {
                    CODE_SVN: 'code-SVN',
                    CODE_GIT: 'code-Git',
                    CODE_GITLAB: 'code-Gitlab',
                    GITHUB: 'code-Github',
                    CODE_TGIT: 'code-TGit',
                    CODE_P4: 'code-P4'
                },
                pipelinesDialogPayload: {
                    isShow: false,
                    isLoadingMore: false,
                    hasLoadEnd: false,
                    page: 1,
                    pageSize: 20,
                    repositoryHashId: ''
                },
                pipelinesList: []
            }
        },

        computed: {
            ...mapState('codelib', ['gitOAuth']),

            projectId () {
                return this.$route.params.projectId
            },

            /**
             * @desc 展示的列表列
             * @returns { Object }
             */
            allColumnMap () {
                if (this.isListFlod) {
                    return {
                        aliasName: true
                    }
                }
                return this.selectedTableColumn.reduce((result, item) => {
                    result[item.id] = true
                    return result
                }, {})
            }
        },

        watch: {
            page (val) {
                this.pagination.current = val
            },

            count (val) {
                this.pagination.count = val
            },

            pageSize (val) {
                this.pagination.limit = val
            },
            curRepoId: {
                handler (val) {
                    this.selectId = val
                },
                immediate: true
            },
            isListFlod: {
                handler (val) {
                    this.pagination.small = val
                },
                immediate: true
            },
            defaultPagesize (val) {
                const limitList = new Set([10, 20, 50, 100, val])
                this.pagination.limitList = [...limitList].sort((a, b) => a - b)
            }
        },

        mounted () {
            this.calcTableHeight()
            window.addEventListener('resize', this.calcTableHeight)
            this.$once('hook:beforeDestroy', () => {
                window.removeEventListener('resize', this.calcTableHeight)
            })
        },

        created () {
            this.tableColumn = [
                {
                    id: 'aliasName',
                    label: this.$t('codelib.aliasName'),
                    disabled: true
                },
                {
                    id: 'url',
                    label: this.$t('codelib.address'),
                    disabled: true
                },
                {
                    id: 'authType',
                    label: this.$t('codelib.authIdentity')
                },
                {
                    id: 'recentlyEditedBy',
                    label: this.$t('codelib.recentlyEditedBy')
                },
                {
                    id: 'lastModifiedTime',
                    label: this.$t('codelib.lastModifiedTime')
                }
            ]
            const columnsCache = listColumnsCache.getItem(TABLE_COLUMN_CACHE)
            if (columnsCache) {
                this.selectedTableColumn = Object.freeze(columnsCache.columns)
                this.tableSize = columnsCache.size
            } else {
                this.selectedTableColumn = Object.freeze([
                    { id: 'aliasName' },
                    { id: 'url' },
                    { id: 'authType' },
                    { id: 'recentlyEditedBy' },
                    { id: 'lastModifiedTime' }
                ])
            }
        },

        methods: {
            ...mapActions('codelib', [
                'toggleCodelibDialog',
                'updateCodelib',
                'deleteRepo',
                'fetchUsingPipelinesList'
            ]),
            prettyDateTimeFormat,

            /**
             * @desc 计算表格高度
             */
            calcTableHeight () {
                const { top } = getOffset(document.getElementById('codelib-list-content'))
                const windowHeight = window.innerHeight
                this.tableHeight = windowHeight - top - 70
            },

            rowClassName ({ row }) {
                return row.repositoryHashId === this.selectId ? 'active' : ''
            },

            handleShowDetail (codelib) {
                if (this.isListFlod) return
                this.$emit('update:curRepo', codelib)
                this.$emit('update:isListFlod', true)
            },

            handleToCodelib (url) {
                window.open(url, '__blank')
            },

            handleRowSelect (row) {
                this.$emit('update:curRepo', row)
                if (this.isListFlod) {
                    this.selectId = row.repositoryHashId
                    this.$router.push({
                        query: {
                            id: row.repositoryHashId,
                            page: this.page,
                            limit: this.pagination.limit
                        }
                    })
                    localStorage.setItem(CODE_REPOSITORY_CACHE, JSON.stringify({
                        id: this.selectId,
                        page: this.page,
                        limit: this.pagination.limit
                    }))
                    this.$emit('updateFlod', true)
                    this.$emit('update:curRepoId', row.repositoryHashId)
                }
            },

            handleSettingChange ({ fields, size }) {
                this.selectedTableColumn = Object.freeze(fields)
                this.tableSize = size
                listColumnsCache.setItem(TABLE_COLUMN_CACHE, {
                    columns: fields,
                    size
                })
            },

            /**
             * 展开列表
             */
            handleExpandList () {
                this.$router.push({
                    query: {}
                })
                localStorage.removeItem(CODE_REPOSITORY_CACHE)
                this.$emit('updateFlod', false)
            },

            handlePageChange (current) {
                this.pagination.current = current
                this.switchPage(current, this.pagination.limit)
            },

            handlePageLimitChange (limit) {
                if (this.pagination.limit === Number(limit)) return

                this.pagination.current = 1
                this.pagination.limit = limit
                this.switchPage(1, limit)

                localStorage.setItem(CODE_REPOSITORY_CACHE, JSON.stringify({
                    id: this.selectId,
                    page: this.page,
                    limit: this.pagination.limit
                }))
            },

            async fetchPipelinesList () {
                if (this.pipelinesDialogPayload.isLoadingMore) return
                this.pipelinesDialogPayload.isLoadingMore = true
                await this.fetchUsingPipelinesList({
                    projectId: this.projectId,
                    repositoryHashId: this.pipelinesDialogPayload.repositoryHashId,
                    page: this.pipelinesDialogPayload.page,
                    pageSize: this.pipelinesDialogPayload.pageSize
                }).then(res => {
                    this.pipelinesList = [...this.pipelinesList, ...res.records]
                    if (this.pipelinesDialogPayload.page === 1 && this.pipelinesList.length) {
                        this.pipelinesDialogPayload.isShow = true
                    }
                    this.pipelinesDialogPayload.hasLoadEnd = res.totalPages === this.pipelinesDialogPayload.page
                    this.pipelinesDialogPayload.page += 1
                }).finally(() => {
                    this.pipelinesDialogPayload.isLoadingMore = false
                })
            },

            async deleteCodeLib (row) {
                if (row.repositoryHashId !== this.pipelinesDialogPayload.repositoryHashId) {
                    this.pipelinesDialogPayload.repositoryHashId = row.repositoryHashId
                    this.pipelinesList = []
                }
                this.pipelinesDialogPayload.page = 1
                
                await this.fetchPipelinesList()

                if (!this.pipelinesList.length) {
                    this.$bkInfo({
                        title: this.$t('codelib.是否删除该代码库？'),
                        confirmFn: () => {
                            this.deleteRepo({
                                projectId: this.projectId,
                                repositoryHashId: row.repositoryHashId
                            }).then(() => {
                                this.refreshCodelibList()
                                this.$bkMessage({
                                    message: this.$t('codelib.successfullyDeleted'),
                                    theme: 'success'
                                })
                            })
                        }
                    })
                }
            },

            // async editCodeLib (codelib) {
            //     const { repositoryHashId, type, authType, svnType } = codelib
            //     const { credentialTypes, typeName } = getCodelibConfig(
            //         type,
            //         svnType,
            //         authType
            //     )
            //     this.updateCodelib({
            //         '@type': typeName
            //     })
            //     const CodelibDialog = {
            //         repositoryHashId,
            //         showCodelibDialog: true,
            //         projectId: this.projectId,
            //         credentialTypes,
            //         authType,
            //         codelib
            //     }
            //     this.toggleCodelibDialog(CodelibDialog)
            // },

            handleSortChange ({ prop, order }) {
                const sortBy = this.sortByMap[prop]
                const sortType = this.sortTypeMap[order]
                this.$emit('handleSortChange', { sortBy, sortType })
            },

            resetFilter () {
                this.$refs.list.clearSort()
                this.$emit('update:aliasName', '')
                this.$emit('handleSortChange', { sortBy: '', sortType: '' })
            }
        }
    }
</script>

<style lang="scss">
.expand-btn {
    position: absolute;
    z-index: 100;
    height: 42px;
    line-height: 42px;
    left: 320px;
    font-size: 10px;
    color: #3A84FF;
    cursor: pointer;
    text-align: center;
    .angle-double-right-icon {
        position: relative;
        top: 3px;
    }
}
.devops-codelib-table {
    outline: 1px solid #dfe0e5;
    margin-top: 20px;
    ::-webkit-scrollbar {
        background-color: #fff;
        height: 5px !important;
        width: 5px !important;
    }
    .devops-codelib-table-body td {
        white-space: nowrap;
        text-overflow: ellipsis;
        overflow: hidden;
    }
    > footer {
        display: flex;
        height: 36px;
        align-items: center;
        margin-top: 30px;
        .codelib-count {
            margin-right: 20px;
        }
        .codelib-page-size {
            width: 62px;
            display: inline-block;
        }
        .codelib-paging {
            margin-left: auto;
        }
    }
    .codelib-type-icon {
        position: relative;
        top: 3px;
    }
    .pac-icon {
        position: relative;
        font-size: 12px;
        margin-left: 10px;
        color: #699DF4;
        background-color: #E1ECFF;
        width: 60px;
        height: 24px;
        display: inline-table;
        line-height: 24px;
        border-radius: 12px;
        text-align: right;
        padding-right: 5px;
    }
    .pac-code-icon {
        position: absolute;
        left: 1px;
        top: 1px;
    }
    // .codelib-aliasName {
    //     position: relative;
    //     span {
    //         position: absolute;
    //         left: 90%;
    //     }
    // }
}
</style>

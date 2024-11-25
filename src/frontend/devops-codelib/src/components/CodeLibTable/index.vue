<template>
    <div>
        <div
            class="expand-btn"
            v-if="isListFlod"
            @click="handleExpandList"
        >
            <bk-icon
                type="angle-double-right"
                class="angle-double-right-icon"
            />
            {{ $t('codelib.expandList') }}
        </div>
        <bk-table
            ref="list"
            v-if="tableHeight"
            v-bkloading="{ isLoading }"
            :class="{
                'devops-codelib-table': true,
                'flod-table': isListFlod
            }"
            :data="records"
            :size="tableSize"
            :height="tableHeight"
            :outer-border="false"
            :row-class-name="rowClassName"
            :pagination="pagination"
            :default-sort="sortField"
            @header-dragend="handelHeaderDragend"
            @row-click="handleRowSelect"
            @sort-change="handleSortChange"
            @page-change="handlePageChange"
            @page-limit-change="handlePageLimitChange"
        >
            <bk-table-column
                v-if="allColumnMap.aliasName"
                :label="$t('codelib.aliasName')"
                :width="tableWidthMap.aliasName"
                sortable
                prop="aliasName"
            >
                <template slot-scope="props">
                    <div
                        :class="{
                            'codelib-name-warpper': true
                        }"
                        v-perm="{
                            hasPermission: props.row.canView,
                            disablePermissionApi: true,
                            permissionData: {
                                projectId: projectId,
                                resourceType: RESOURCE_TYPE,
                                resourceCode: props.row.repositoryHashId,
                                action: RESOURCE_ACTION.VIEW
                            }
                        }"
                    >
                        <div
                            v-if="isListFlod"
                            class="mask"
                        ></div>
                        <Icon
                            class="codelib-logo"
                            :name="codelibIconMap[props.row.type]"
                            size="16"
                        />
                        <div
                            :class="{
                                'codelib-name': true,
                                'name-flod': isListFlod,
                                'name-disabled': !props.row.canView
                            }"
                            v-bk-overflow-tips
                            @click="handleShowDetail(props.row)"
                        >
                            {{ props.row.aliasName }}
                        </div>
                        <span
                            v-if="props.row.enablePac"
                            class="pac-icon"
                        >
                            <Icon
                                name="PACcode"
                                size="22"
                                class="pac-code-icon"
                            />
                            PAC
                        </span>
                    </div>
                </template>
            </bk-table-column>
            <bk-table-column
                v-if="allColumnMap.url"
                :label="$t('codelib.address')"
                :width="tableWidthMap.url"
                sortable
                prop="url"
                show-overflow-tooltip
            >
                <template slot-scope="props">
                    {{ props.row.url }}
                    <!-- <a
                        @click="handleToCodelib(props.row.url)"
                    >
                        {{ props.row.url }}
                    </a> -->
                </template>
            </bk-table-column>
            <bk-table-column
                v-if="allColumnMap.authType"
                :label="$t('codelib.auth')"
                :width="tableWidthMap.authType"
                prop="authType"
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
                :width="tableWidthMap.updatedUser"
                prop="updatedUser"
                show-overflow-tooltip
            >
            </bk-table-column>
            <bk-table-column
                v-if="allColumnMap.lastModifiedTime"
                :label="$t('codelib.lastModifiedTime')"
                :width="tableWidthMap.updatedTime"
                prop="updatedTime"
            >
                <template slot-scope="props">
                    {{ prettyDateTimeFormat(Number(props.row.updatedTime + '000')) }}
                </template>
            </bk-table-column>
            <bk-table-column
                v-if="!isListFlod"
                fixed="right"
                :label="$t('codelib.operation')"
                prop="operation"
                :width="tableWidthMap.operation"
                show-overflow-tooltip
            >
                <template slot-scope="props">
                    <bk-button
                        v-if="props.row.type === 'CODE_GIT' && !props.row.enablePac"
                        theme="primary"
                        text
                        class="mr10"
                        v-perm="{
                            hasPermission: props.row.canView,
                            disablePermissionApi: true,
                            permissionData: {
                                projectId: projectId,
                                resourceType: RESOURCE_TYPE,
                                resourceCode: props.row.repositoryHashId,
                                action: RESOURCE_ACTION.VIEW
                            }
                        }"
                        @click="toDetailInit(props.row)"
                    >
                        {{ $t('codelib.去开启PAC') }}
                    </bk-button>
                    <span
                        v-bk-tooltips="{
                            content: $t('codelib.请先关闭 PAC 模式，再删除代码库'),
                            disabled: !props.row.enablePac
                        }"
                    >
                        <bk-button
                            theme="primary"
                            text
                            v-perm="{
                                hasPermission: props.row.canDelete,
                                disablePermissionApi: true,
                                permissionData: {
                                    projectId: projectId,
                                    resourceType: RESOURCE_TYPE,
                                    resourceCode: props.row.repositoryHashId,
                                    action: RESOURCE_ACTION.DELETE
                                }
                            }"
                            :disabled="props.row.enablePac"
                            @click.stop="deleteCodeLib(props.row)"
                        >
                            {{ $t('codelib.delete') }}
                        </bk-button>
                    </span>
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
                    @setting-change="handleSettingChange"
                />
            </bk-table-column>
            <template #empty>
                <EmptyTableStatus
                    :type="aliasName ? 'search-empty' : 'empty'"
                    @clear="resetFilter"
                />
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
    import { RESOURCE_ACTION, RESOURCE_TYPE } from '@/utils/permission'
    import {
        TABLE_COLUMN_CACHE,
        CODE_REPOSITORY_CACHE,
        CODE_REPOSITORY_SEARCH_VAL,
        CACHE_CODELIB_TABLE_WIDTH_MAP,
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
            isSearch: Boolean,
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
                RESOURCE_ACTION,
                RESOURCE_TYPE,
                scmType: '',
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
                pipelinesList: [],
                tableWidthMap: {}
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
            },
            sortField () {
                const { sortType, sortBy } = this.$route.query
                const prop = sortBy ?? localStorage.getItem('codelibSortBy')
                const order = sortType ?? localStorage.getItem('codelibSortType')
                return {
                    prop: this.getkeyByValue(this.sortByMap, prop),
                    order: this.getkeyByValue(this.sortTypeMap, order)
                }
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
            },
            'pipelinesDialogPayload.isShow' (val) {
                if (!val) {
                    this.pipelinesList = []
                }
            }
        },

        mounted () {
            this.calcTableHeight()
            window.addEventListener('resize', this.calcTableHeight)
            this.$once('hook:beforeDestroy', () => {
                window.removeEventListener('resize', this.calcTableHeight)
            })

            this.tableWidthMap = JSON.parse(localStorage.getItem(CACHE_CODELIB_TABLE_WIDTH_MAP)) || {
                aliasName: 400,
                url: '',
                authType: 200,
                updatedUser: 200,
                updatedTime: 200,
                operation: 150
            }
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
                    label: this.$t('codelib.auth')
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
            getkeyByValue (obj, value) {
                return Object.keys(obj).find(key => obj[key] === value)
            },
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

            // handleToCodelib (url) {
            //     window.open(url, '__blank')
            // },

            handleRowSelect (row) {
                this.$emit('update:curRepo', row)
                if (this.isListFlod) this.toDetailInit(row)
            },

            toDetailInit (row) {
                this.selectId = row.repositoryHashId
                this.scmType = row.type
                this.$router.push({
                    query: {
                        ...this.$route.query,
                        id: row.repositoryHashId,
                        page: this.page,
                        scmType: row.type,
                        limit: this.pagination.limit
                    }
                })
                localStorage.setItem(CODE_REPOSITORY_CACHE, JSON.stringify({
                    scmType: row.type,
                    id: row.repositoryHashId,
                    page: this.page,
                    limit: this.pagination.limit,
                    projectId: this.projectId
                }))
                this.$emit('update:isListFlod', true)
                this.$emit('update:curRepoId', row.repositoryHashId)
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
                localStorage.removeItem(CODE_REPOSITORY_SEARCH_VAL)
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
                    scmType: this.scmType,
                    id: this.selectId,
                    page: this.page,
                    limit: this.pagination.limit,
                    projectId: this.projectId
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
                    this.pipelinesDialogPayload.hasLoadEnd = res.count === this.pipelinesList.length
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
                            }).catch((e) => {
                                this.$bkMessage({
                                    message: e.message || e,
                                    theme: 'error'
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
                this.$emit('handleSortChange', { sortBy, sortType, prop, order })
            },

            resetFilter () {
                this.$refs.list.clearSort()
                this.$emit('update:aliasName', '')
                this.$emit('handleSortChange', { sortBy: '', sortType: '' })
                localStorage.removeItem(CODE_REPOSITORY_SEARCH_VAL)
                this.$router.push({
                    query: {
                        ...this.$route.query,
                        searchName: ''
                    }
                })
            },

            handelHeaderDragend (newWidth, oldWidth, column) {
                this.tableWidthMap[column.property] = newWidth
                localStorage.setItem(CACHE_CODELIB_TABLE_WIDTH_MAP, JSON.stringify(this.tableWidthMap))
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
    left: 310px;
    font-size: 12px;
    color: #3A84FF;
    cursor: pointer;
    text-align: center;
    .angle-double-right-icon {
        position: relative;
        top: 3px;
        font-size: 24px !important;
    }
}
.flod-table {
    td,
    th {
        width: 400px !important;
    }
    .bk-table-empty-block {
        width: 400px !important;
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
    .codelib-name-warpper {
        display: inline-flex;
        align-items: center;
        position: initial;
        width: 100%;
        .mask {
            position: absolute;
            top: 0;
            bottom: 0;
            left: 0;
            right: 0;
        }
        .codelib-logo {
            flex-shrink: 0;
        }
        .codelib-name {
            color: #3c96ff;
            cursor: pointer;
            margin-left: 3px;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
        }
    }
    .is-flod-warpper {
        display: initial;
    }
    .pac-icon {
        position: relative;
        flex-shrink: 0;
        font-size: 12px;
        margin-left: 10px;
        color: #699DF4;
        background-color: #E1ECFF;
        width: 60px;
        height: 24px;
        display: inline-grid;
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
    .name-flod {
        position: relative;
        display: inline-block;
        max-width: 280px;
        overflow: hidden;
        white-space: nowrap;
        text-overflow: ellipsis;
    }
    .name-disabled {
        color: #dcdee5 !important;
    }
}
</style>

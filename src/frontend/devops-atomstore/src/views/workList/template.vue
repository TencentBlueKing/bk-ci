<template>
    <main>
        <div class="content-header">
            <div class="atom-total-row">
                <bk-button
                    theme="primary"
                    icon="plus"
                    @click="updateTemplate"
                >
                    {{ $t('store.上架模板') }}
                </bk-button>
            </div>
            <search-select
                class="search-select"
                :data="filterData"
                :placeholder="$t('store.模板名称/模板描述/模板类型/所属项目/状态/更新人')"
                :values="searchValue"
                @change="handleSearchChange"
            >
            </search-select>
        </div>
        <main class="g-scroll-pagination-table">
            <bk-table
                style="margin-top: 15px;"
                :header-cell-style="{ background: '#fff' }"
                :data="renderList"
                :size="tableSize"
                :pagination="pagination"
                @page-change="pageChanged"
                @page-limit-change="pageCountChanged"
                v-bkloading="{ isLoading }"
            >
                <bk-table-column
                    v-if="allRenderColumnMap.templateName"
                    :label="$t('store.模板名称')"
                    :min-width="260"
                    show-overflow-tooltip
                    sortable
                    prop="templateName"
                >
                    <template slot-scope="props">
                        <div
                            class="tempalte-name"
                            @click="routerAtoms(props.row.templateCode)"
                        >
                            <img
                                :src="props.row.logoUrl"
                                width="40"
                                height="40"
                            >
                            <span
                                class="atom-name"
                                :title="props.row.templateName"
                            >{{ props.row.templateName }}</span>
                            
                            <span class="pac-tag">
                                <i class="devops-icon icon-code" />
                                PAC
                            </span>
                        </div>
                    </template>
                </bk-table-column>
                <bk-table-column
                    v-if="allRenderColumnMap.desc"
                    :label="$t('store.模板描述')"
                    prop="desc"
                    show-overflow-tooltip
                ></bk-table-column>
                <bk-table-column
                    v-if="allRenderColumnMap.typeName"
                    :label="$t('store.模板类型')"
                    prop="typeName"
                    :min-width="100"
                    show-overflow-tooltip
                    :filters="templateTypeFilters"
                    :filter-method="templateTypeFilterMethod "
                    :filter-multiple="false"
                ></bk-table-column>
                <bk-table-column
                    v-if="allRenderColumnMap.projectName"
                    :label="$t('store.所属项目')"
                    prop="projectName"
                    :min-width="100"
                    show-overflow-tooltip
                    :filters="projectFilters"
                    :filter-method="projectFilterMethod "
                    :filter-multiple="false"
                ></bk-table-column>
                <bk-table-column
                    v-if="allRenderColumnMap.templateStatus"
                    :label="$t('store.状态')"
                    :min-width="100"
                    show-overflow-tooltip
                    prop="templateStatus"
                >
                    <template slot-scope="props">
                        <div
                            class="bk-spin-loading bk-spin-loading-mini bk-spin-loading-primary"
                            v-if="props.row.templateStatus === 'AUDITING'"
                        >
                            <div class="rotate rotate1"></div>
                            <div class="rotate rotate2"></div>
                            <div class="rotate rotate3"></div>
                            <div class="rotate rotate4"></div>
                            <div class="rotate rotate5"></div>
                            <div class="rotate rotate6"></div>
                            <div class="rotate rotate7"></div>
                            <div class="rotate rotate8"></div>
                        </div>
                        <span
                            class="atom-status-icon success"
                            v-if="props.row.templateStatus === 'RELEASED'"
                        ></span>
                        <span
                            class="atom-status-icon fail"
                            v-if="props.row.templateStatus === 'GROUNDING_SUSPENSION'"
                        ></span>
                        <span
                            class="atom-status-icon obtained"
                            v-if="props.row.templateStatus === 'AUDIT_REJECT' || props.row.templateStatus === 'UNDERCARRIAGED'"
                        ></span>
                        <span
                            class="atom-status-icon devops-icon icon-initialize"
                            v-if="props.row.templateStatus === 'INIT'"
                        ></span>
                        <span>{{ $t(templateStatusMap[props.row.templateStatus]) }}</span>
                    </template>
                </bk-table-column>
                <bk-table-column
                    v-if="allRenderColumnMap.version"
                    :label="$t('store.最新版本')"
                    prop="version"
                    show-overflow-tooltip
                ></bk-table-column>
                <bk-table-column
                    v-if="allRenderColumnMap.modifier"
                    :label="$t('store.更新人')"
                    prop="modifier"
                    :min-width="100"
                    show-overflow-tooltip
                ></bk-table-column>
                <bk-table-column
                    v-if="allRenderColumnMap.updateTime"
                    :label="$t('store.修改时间')"
                    prop="updateTime"
                    :min-width="150"
                    :formatter="timeFormatter"
                    show-overflow-tooltip
                    sortable
                ></bk-table-column>
                <bk-table-column
                    v-if="allRenderColumnMap.operate"
                    :label="$t('store.操作')"
                    width="300"
                    flxed="right"
                    class-name="handler-btn"
                >
                    <template slot-scope="props">
                        <span
                            class="shelf-btn"
                            v-if="props.row.templateStatus === 'INIT' || props.row.templateStatus === 'UNDERCARRIAGED'
                                || props.row.templateStatus === 'GROUNDING_SUSPENSION' || props.row.templateStatus === 'AUDIT_REJECT'"
                            @click="editHandle(props.row.templateCode)"
                        > {{ $t('store.上架') }} </span>
                        <span
                            class="shelf-btn"
                            v-if="props.row.templateStatus === 'RELEASED'"
                            @click="editHandle(props.row.templateCode)"
                        > {{ $t('store.升级') }} </span>
                        <span>
                            <a
                                target="_blank"
                                style="color:#3c96ff;"
                                :href="`/console/pipeline/${props.row.projectCode}/template/${props.row.templateCode}/instanceList`"
                            > {{ $t('store.源模板') }} </a>
                        </span>
                        <span
                            class="shelf-btn"
                            v-if="props.row.templateStatus === 'RELEASED'"
                            @click="installAHandle(props.row.templateCode)"
                        > {{ $t('store.安装') }} </span>
                        <span
                            class="schedule-btn"
                            v-if="props.row.templateStatus === 'AUDITING'"
                            @click="toTemplateProgress(props.row.templateCode)"
                        > {{ $t('store.进度') }} </span>
                        <span
                            class="obtained-btn"
                            v-if="props.row.templateStatus === 'AUDIT_REJECT' || props.row.templateStatus === 'RELEASED' || (props.row.templateStatus === 'GROUNDING_SUSPENSION' && props.row.releaseFlag)"
                            @click="offline(props.row)"
                        > {{ $t('store.下架') }} </span>
                        <span
                            style="margin-right:0"
                            @click="deleteTemplate(props.row)"
                            v-if="['INIT', 'GROUNDING_SUSPENSION', 'UNDERCARRIAGED'].includes(props.row.templateStatus)"
                        > {{ $t('store.移除') }} </span>
                    </template>
                </bk-table-column>
                <bk-table-column
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
                        :type="searchValue.length ? 'search-empty' : 'empty'"
                        @clear="searchValue = []"
                    />
                </template>
            </bk-table>
        </main>

        <bk-dialog
            theme="primary"
            v-model="offlineTempConfig.show"
            render-directive="if"
            header-position="left"
            :ok-text="$t('store.下架')"
            :title="offlineTempConfig.title"
            :mask-close="offlineTempConfig.quickClose"
            :confirm-fn="submitofflineTemp"
            :on-close="closeOfflineTemp"
        >
            <div
                class="offline-atom-form"
                v-bkloading="{
                    isLoading: offlineTempConfig.isLoading
                }"
            >
                <div class="form-item">
                    <label class="label"> {{ $t('store.模板') }}： </label>
                    <span class="value">{{ curHandlerTemp.templateName }}</span>
                </div>
                <div class="tips-content">
                    <p>{{ offlineTips }}</p>
                    <p
                        v-for="(row, index) in tempPromptList"
                        :key="index"
                    >
                        <span>{{ row }}</span>
                    </p>
                </div>
            </div>
        </bk-dialog>
    </main>
</template>

<script>
    import { TEMPLATE_TABLE_COLUMN_CACHE, templateStatusList } from '@/store/constants'
    import { debounce } from '@/utils/index'
    import SearchSelect from '@blueking/search-select'
    import '@blueking/search-select/dist/styles/index.css'

    export default {
        components: {
            SearchSelect
        },

        data () {
            return {
                templateStatusMap: templateStatusList,
                isSearch: false,
                itemUrl: '/console/pm',
                itemText: this.$t('store.新建项目'),
                offlineTips: this.$t('store.下架后：'),
                renderList: [],
                templateList: [],
                projectList: [],
                tempPromptList: [
                    this.$t('store.1、不再在模版市场中展示该模板'),
                    this.$t('store.2、已使用模版的流水线可以继续使用，但有「模版已下架」标识')
                ],
                curHandlerTemp: {},
                relateTemplateForm: {
                    projectCode: '',
                    template: '',
                    name: ''
                },
                isLoading: false,
                templateErrors: {
                    projectError: false,
                    tplError: false
                },
                offlineTempConfig: {
                    show: false,
                    isLoading: false,
                    title: this.$t('store.确认下架该模板？'),
                    quickClose: true,
                    width: 565
                },
                statusList: {
                    publish: this.$t('store.已发布'),
                    commiting: this.$t('store.提交中'),
                    fail: this.$t('store.上架失败'),
                    testing: this.$t('store.测试中'),
                    auditing: this.$t('store.审核中'),
                    obtained: this.$t('store.已下架'),
                    draft: this.$t('store.草稿')
                },
                pagination: {
                    current: 1,
                    count: 1,
                    limit: 10
                },
                projectFilters: [],
                templateTypeFilters: [{ text: this.$t('store.流水线'), value: 'PIPELINE' }],
                tableSize: 'small',
                tableColumn: [],
                selectedTableColumn: [],
                searchValue: []
            }
        },
        computed: {
            allRenderColumnMap () {
                return this.selectedTableColumn.reduce((result, item) => {
                    result[item.id] = true
                    return result
                }, {})
            },
            filterData () {
                return [
                    {
                        name: this.$t('store.模板名称'),
                        id: 'templateName'
                    },
                    {
                        name: this.$t('store.模板描述'),
                        id: 'desc'
                    },
                    {
                        name: this.$t('store.模板类型'),
                        id: 'typeName'
                    },
                    {
                        name: this.$t('store.所属项目'),
                        id: 'projectName'
                    },
                    {
                        name: this.$t('store.状态'),
                        id: 'templateStatus'
                    },
                    {
                        name: this.$t('store.更新人'),
                        id: 'modifier'
                    }
                ]
            },
            searchParams () {
                return this.searchValue.reduce((acc, filter) => {
                    acc[filter.id] = filter.values.map(val => val.id).join(',')
                    return acc
                }, {})
            }
        },

        watch: {
            searchValue () {
                this.isLoading = true
                debounce(this.search)
            }
        },

        mounted () {
            this.tableColumn = [
                {
                    id: 'templateName',
                    label: this.$t('store.模板名称'),
                    width: 300,
                    disabled: true,
                    sortable: true,
                    showOverflowTooltip: true
                },
                {
                    id: 'desc',
                    label: this.$t('store.模板描述'),
                    width: 300,
                    showOverflowTooltip: true
                },
                {
                    id: 'typeName',
                    label: this.$t('store.模板类型'),
                    width: 150
                },
                {
                    id: 'projectName',
                    label: this.$t('store.所属项目'),
                    width: 150
                },
                {
                    id: 'templateStatus',
                    label: this.$t('store.状态'),
                    width: 150
                },
                {
                    id: 'version',
                    label: this.$t('store.最新版本'),
                    width: 120
                },
                {
                    id: 'modifier',
                    label: this.$t('store.更新人'),
                    width: 100
                },
                {
                    id: 'updateTime',
                    label: this.$t('store.修改时间'),
                    width: 150
                },
                {
                    id: 'operate',
                    disabled: true,
                    label: this.$t('store.操作')
                }
            ]
            const columnsCache = JSON.parse(localStorage.getItem(TEMPLATE_TABLE_COLUMN_CACHE))
            if (columnsCache) {
                this.selectedTableColumn = columnsCache.columns
                this.tableSize = columnsCache.size
            } else {
                this.selectedTableColumn = [
                    { id: 'templateName' },
                    { id: 'desc' },
                    { id: 'typeName' },
                    { id: 'projectName' },
                    { id: 'templateStatus' },
                    { id: 'version' },
                    { id: 'modifier' },
                    { id: 'updateTime' },
                    { id: 'operate' }
                ]
            }

            this.requestList()
        },

        methods: {
            async requestList () {
                this.isLoading = true
                const page = this.pagination.current
                const pageSize = this.pagination.limit
                try {
                    const res = await this.$store.dispatch('store/requestTemplateList', {
                        templateName: '',
                        page,
                        pageSize,
                        ...this.searchParams
                    })

                    this.renderList.splice(0, this.renderList.length, ...(res.records || []))
                    if (this.renderList.length) {
                        this.pagination.count = res.count
                    }
                } catch (err) {
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                } finally {
                    this.isLoading = false
                }
            },

            timeFormatter (row, column, cellValue, index) {
                const date = new Date(cellValue)
                const year = date.toISOString().slice(0, 10)
                const time = date.toTimeString().split(' ')[0]
                return `${year} ${time}`
            },

            deleteTemplate (row) {
                this.isLoading = true
                let message = this.$t('store.移除成功')
                let theme = 'success'

                this.$store.dispatch('store/deleteTemplate', row.templateCode).then((res) => {
                    this.requestList()
                }).catch((err) => {
                    message = err.message || err
                    theme = 'error'
                }).finally(() => {
                    this.$bkMessage({ message, theme })
                    this.isLoading = false
                })
            },

            async pageCountChanged (currentLimit, prevLimit) {
                if (currentLimit === this.pagination.limit) return

                this.pagination.current = 1
                this.pagination.limit = currentLimit
                await this.requestList()
            },

            async pageChanged (page) {
                this.pagination.current = page
                await this.requestList()
            },

            search () {
                this.isSearch = true
                this.pagination.current = 1
                this.requestList()
            },

            closeOfflineTemp () {
                this.offlineTempConfig.show = false
            },

            async submitofflineTemp () {
                let message, theme

                this.offlineTempConfig.isLoading = true
                try {
                    await this.$store.dispatch('store/offlineTemplate', {
                        templateCode: this.curHandlerTemp.templateCode
                    })

                    message = this.$t('store.下架成功')
                    theme = 'success'
                    this.offlineTempConfig.show = false
                    this.requestList()
                } catch (err) {
                    message = err.message ? err.message : err
                    theme = 'error'
                } finally {
                    this.$bkMessage({
                        message,
                        theme
                    })

                    this.offlineTempConfig.isLoading = false
                }
            },

            routerAtoms (code) {
                this.$router.push({
                    name: 'version',
                    params: {
                        code,
                        type: 'template'
                    }
                })
            },

            toTemplateProgress (templateCode) {
                this.$router.push({
                    name: 'upgradeTemplate',
                    params: {
                        templateCode
                    }
                })
            },

            updateTemplate () {
                this.$router.push({
                    name: 'editTemplate',
                    query: {
                        type: 'apply'
                    }
                })
            },

            offline (form) {
                this.offlineTempConfig.show = true
                this.curHandlerTemp = form
            },

            installAHandle (code) {
                this.$router.push({
                    name: 'install',
                    query: {
                        code,
                        type: 'template',
                        from: 'templateWork'
                    }
                })
            },

            editHandle (templateCode) {
                this.$router.push({
                    name: 'editTemplate',
                    params: {
                        templateCode
                    },
                    query: {
                        type: 'edit'
                    }
                })
            },
            projectFilterMethod (value, row, column) {
                const property = column.property
                return row[property] === value
            },
            templateTypeFilterMethod (value, row, column) {
                const property = column.property
                return row[property] === value
            },
            handleSettingChange ({ fields, size }) {
                this.selectedTableColumn = fields
                this.tableSize = size
                localStorage.setItem(TEMPLATE_TABLE_COLUMN_CACHE, JSON.stringify({
                    columns: fields,
                    size
                }))
            },
            handleSearchChange (value) {
                this.searchValue = value
            }
        }
    }
</script>
<style scoped lang="scss">
    .bk-icon-tooltips {
        padding-top: 3px;
        padding-left: 10px;
    }
    .content-header {
        display: flex;
        justify-content: space-between;
        width: 100%;
        .search-select {
            width: 510px;
            ::placeholder {
                color: #d1d2d7;
            }
        }
    }
    .g-scroll-pagination-table {
        .bk-table {
            height: auto;
            .tempalte-name {
                display: flex;
                align-items: center;
                img {
                    margin: 5px 5px;
                }
                .pac-tag {
                    margin-left: 10px;
                    background: #E1ECFF;
                    border-radius: 12px;
                    width: 60px;
                    height: 22px;
                    line-height: 1;
                    display: grid;
                    align-items: center;
                    grid-auto-flow: column;
                    font-size: 12px;
                    color: #699DF4;
                    cursor: pointer;
                    &:hover {
                        color: #3A84FF;
                        .devops-icon {
                            background: #3A84FF;
                        }
                    }
                    .devops-icon {
                        width: 20px;
                        height: 20px;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        background: #699DF4;
                        color: white;
                        border-radius: 50%;
                        flex-shrink: 0;
                    }
                }
            }
        }
    }
    .offline-atom-form {
        color: #696a70;
        .value {
            color: #070707;
        }
        
        .tips-content {
            margin-top: 25px;
            p {
                text-align: left;
            }
        }
    }
</style>

<template>
    <article class="template-manage-entry">
        <aside class="template-aside">
            <div
                v-for="(item, idx) in navList"
                :key="idx"
            >
                <div
                    :class="['nav-item', activeTab === item.name ? 'active' : '']"
                    @click="handleChangeMenu(item.name)"
                >
                    <div>
                        <Logo
                            :class="item.icon"
                            size="14"
                            :name="item.icon"
                        />
                        <span>{{ item.label }}</span>
                    </div>
                    <span class="nav-num">{{ item.num }}</span>
                </div>
                <p
                    v-if="item.isAll"
                    class="item-border"
                ></p>
            </div>
        </aside>
        <div class="template-main">
            <div class="template-main-header">
                <div>
                    <bk-button
                        :theme="'primary'"
                        @click="handleClick"
                    >
                        新建模板
                    </bk-button>
                    <bk-button>
                        安装/导入模板
                    </bk-button>
                </div>
                <search-select
                    class="search-input"
                    :data="filterData"
                    :placeholder="$t('template.searchPlaceholder')"
                    :values="searchValue"
                    @change="handleSearchChange"
                >
                </search-select>
            </div>
            <template-manage-entry
                ref="selfTemp"
                :type="activeTab"
                :data="tableData"
                :pagination="pagination"
                :is-loading="isLoading"
                @limit-change="handlePageLimitChange"
                @page-change="handlePageChange"
                @clear="handleClear"
            />
        </div>
    </article>
</template>
  
  <script>
    import dayjs from 'dayjs'
    import Logo from '@/components/Logo'
    import SearchSelect from '@blueking/search-select'
    import '@blueking/search-select/dist/styles/index.css'
    import TemplateManageEntry from '@/components/template/templateManageTable.vue'
    import {
        RESOURCE_ACTION,
        TEMPLATE_RESOURCE_ACTION,
        PROJECT_RESOURCE_ACTION
    } from '@/utils/permission'

    export default {
        components: {
            Logo,
            SearchSelect,
            TemplateManageEntry
        },
        data () {
            return {
                activeTab: 'UNKNOWN',
                searchValue: [],
                isLoading: false,
                tableData: [],
                pagination: {
                    current: 1,
                    count: 6,
                    limit: 20
                }
            }
        },
        computed: {
            navList () {
                return [
                    {
                        label: this.$t('全部模板'),
                        name: 'UNKNOWN',
                        isAll: true,
                        icon: 'time-circle-fill',
                        num: 60
                    },
                    {
                        label: this.$t('流水线模板'),
                        name: 'PIPELINE',
                        icon: 'pipeline-group-item-icon',
                        num: 10
                    },
                    {
                        label: this.$t('Stage模板'),
                        name: 'STAGE',
                        icon: 'pipeline-group-item-icon',
                        num: 6
                    },
                    {
                        label: this.$t('Job模板'),
                        name: 'JOB',
                        icon: 'pipeline-group-item-icon',
                        num: 10
                    },
                    {
                        label: this.$t('Step模板'),
                        name: 'STEP',
                        icon: 'pipeline-group-item-icon',
                        num: 6
                    }
                ]
            },
            filterData () {
                return [{
                    name: this.$t('template.name'),
                    id: 'name'
                }, {
                    name: this.$t('template.desc'),
                    id: 'desc'
                }, {
                    name: this.$t('template.type'),
                    id: 'templateType'
                }, {
                    name: this.$t('template.source'),
                    id: 'source'
                }, {
                    name: this.$t('template.lastModifiedBy'),
                    id: 'lastModifiedBy'
                }]
            },
            projectId () {
                return this.$route.params.projectId
            },
            TEMPLATE_RESOURCE_ACTION () {
                return TEMPLATE_RESOURCE_ACTION
            },
            RESOURCE_ACTION () {
                return RESOURCE_ACTION
            },
            PROJECT_RESOURCE_ACTION () {
                return PROJECT_RESOURCE_ACTION
            }
        },
        mounted () {
            this.fetchTableData()
        },
        methods: {
            handleChangeMenu (name) {
                this.activeTab = name
                this.fetchTableData()
            },
            sourceFilterMethod (value, row, column) {
                const property = column.property
                return row[property] === value
            },
            async fetchTableData (params = { }) {
                this.isLoading = true
                try {
                    const param = {
                        projectId: this.projectId,
                        page: this.pagination.current,
                        pageSize: this.pagination.limit,
                        type: this.activeTab,
                        ...params
                    }
                    const res = await this.$store.dispatch('pipelines/getTemplateList', param)
                    res.records = [
                        {
                            id: '1be6f63b1bea43ecbbdccc7c9844a572',
                            projectId: '项目ID',
                            name: '模板名称',
                            desc: '简介',
                            mode: '公共/约束/自定义模式',
                            category: '应用范畴',
                            type: 'PIPELINE',
                            logoUrl: 'logo地址',
                            enablePac: true, // '是否开启PAC',
                            lastedVersion: 8, // '最新版本号',
                            lastedVersionStatus: '最新版本状态',
                            lastedVersionName: '最新版本名称',
                            lastedSettingVersion: '最新设置版本号',
                            source: '模板来源',
                            storeFlag: true, // '是否从研发商店安装至项目',
                            srcTemplateId: '父模板ID',
                            srcTemplateProjectId: '父模板项目ID',
                            debugPipelineCount: 4, // '调试流水线数',
                            instancePipelineCount: 3, // '实例流水线数',
                            creator: '创建人',
                            updater: '更新人',
                            updateTime: 1617855344000,
                            canView: true, // '是否有模版查看权限',
                            canEdit: true, // '是否有模版编辑权限',
                            canDelete: true // '是否有模版删除权限'
                        }
                    ]

                    this.tableData = (res.records || []).map(x => {
                        x.updateTime = dayjs(x.updateTime).format('YYYY-MM-DD HH:mm:ss')
                        x.templateActions = [
                            {
                                text: this.$t('copy'),
                                handler: this.copyTemplate,
                                hasPermission: x.canEdit,
                                disablePermissionApi: true,
                                permissionData: {
                                    projectId: this.projectId,
                                    resourceType: 'pipeline_template',
                                    resourceCode: this.projectId,
                                    action: this.RESOURCE_ACTION.CREATE
                                }
                            },
                            {
                                text: this.$t('template.toStore'),
                                handler: this.toRelativeStore,
                                hasPermission: x.canEdit,
                                disablePermissionApi: true,
                                permissionData: {
                                    projectId: this.projectId,
                                    resourceType: 'pipeline_template',
                                    resourceCode: x.templateId,
                                    action: this.TEMPLATE_RESOURCE_ACTION.EDIT
                                }
                            },
                            {
                                text: this.$t('delete'),
                                handler: this.deleteTemplate,
                                hasPermission: x.canDelete,
                                disablePermissionApi: true,
                                permissionData: {
                                    projectId: this.projectId,
                                    resourceType: 'pipeline_template',
                                    resourceCode: x.templateId,
                                    action: this.TEMPLATE_RESOURCE_ACTION.EDIT
                                }
                            }
                        ]
                        return x
                    })
                    this.pagination.count = res.count
                } catch (err) {
                    this.$showTips({
                        message: err.message || err,
                        theme: 'error'
                    })
                } finally {
                    this.isLoading = false
                }
            },
            handlePageLimitChange (limit) {
                this.pagination.limit = limit
                this.fetchTableData()
            },
            handlePageChange (page) {
                this.pagination.current = page
                this.fetchTableData()
            },
            formatValue (originVal) {
                return originVal.reduce((acc, filter) => {
                    acc[filter.id] = filter.values.map(val => val.id).join(',')
                    return acc
                }, {})
            },
            handleSearchChange (value) {
                const formatVal = value.reduce((acc, filter) => {
                    acc[filter.id] = filter.values.map(val => val.id).join(',')
                    return acc
                }, {})
                this.fetchTableData(formatVal)
            },
            handleClear () {
                this.searchValue = []
                this.fetchTableData()
            }
        }
    }
  </script>

<style lang="scss" scoped>
.template-manage-entry{
    height: 100%;
    display: flex;
    box-sizing: border-box;

    .template-aside{
        width: 240px;
        background: #FFFFFF;
        padding-top: 8px;
        box-shadow: 1px 0 0 0 #EAEBF0, 1px 0 0 0 #DCDEE5;

        .nav-item {
            display: flex;
            justify-content: space-between;
            padding: 0 32px;
            height: 40px;
            align-items: center;
            font-size: 14px;
            color: #4D4F56;
            cursor: pointer;
            svg {
                vertical-align: middle;
                border: 1px solid #ccc;
                margin: 0 10px;
            }
            .nav-num {
                height: 16px;
                padding: 0 8px;
                background: #F0F1F5;
                border-radius: 8px;
                font-size: 12px;
                color: #979BA5;
                text-align: center;
                line-height: 16px;
            }
        }
        .active{
            background-color: #E1ECFF;
            color: #3A84FF;
        }
        .item-border {
            width: 190px;
            border-bottom: 1px solid #DCDEE5;
            margin: 8px 24px;
        }
   }

   .template-main {
        width: 100%;
        padding: 24px 36px 12px 24px;

        &-header {
            display: flex;
            justify-content: space-between;
            margin-bottom: 16px;
            height: 32px;

            .search-input {
                width: 600px;
            }
        }
   }
}
</style>

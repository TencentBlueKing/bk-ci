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
                        :disabled="!hasCreatePermission"
                        @click="handleShowCreateTemplateDialog"
                    >
                        {{ $t('template.addTemplate') }}
                    </bk-button>
                    <bk-button
                        :disabled="!hasCreatePermission"
                        @click="handleShowInstallTemplateDialog"
                    >
                        {{ $t('template.installOrImportTemplate') }}
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
            <templateTable
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

        <bk-dialog
            width="800"
            v-model="copyTemp.isShow"
            header-position="left"
            ext-cls="form-dialog"
            :title="copyTemp.title"
            :close-icon="copyTemp.closeIcon"
            @confirm="copyConfirmHandler"
            @cancel="copyCancelHandler"
        >
            <template>
                <section class="copy-pipeline bk-form">
                    <div class="bk-form-item">
                        <label class="bk-label">{{ $t('template.name') }}：</label>
                        <div class="bk-form-content">
                            <input
                                type="text"
                                class="bk-form-input"
                                :placeholder="$t('template.nameInputTips')"
                                v-model="copyTemp.templateName"
                                :class="{ 'is-danger': copyTemp.nameHasError }"
                                @input="copyTemp.nameHasError = false"
                                name="copyTemplateName"
                                v-validate="&quot;required|max:30&quot;"
                                maxlength="30"
                            >
                        </div>
                        <p
                            v-if="errors.has('copyTemplateName')"
                            class="error-tips err-name"
                        >
                            {{ $t('template.nameErrTips') }}
                        </p>
                    </div>

                    <div class="bk-form-item">
                        <label class="bk-label tip-bottom">{{ $t('template.applySetting') }}
                            <span
                                v-bk-tooltips.bottom-end="'选“是”则将流水线设置应用于复制后的模版'"
                                class="bottom-end"
                            >
                                <i class="bk-icon icon-info-circle"></i>
                            </span>
                        </label>
                        <div class="bk-form-content">
                            <bk-radio-group v-model="copyTemp.isCopySetting">
                                <bk-radio
                                    v-for="(entry, key) in copySettings"
                                    :key="key"
                                    :value="entry.value"
                                    class="form-radio"
                                >
                                    {{ entry.label }}
                                </bk-radio>
                            </bk-radio-group>
                        </div>
                    </div>
                </section>
            </template>
        </bk-dialog>

        <CreateTemplateDialog
            :value.sync="showAddTemplateDialog"
            @confirm="handleConfirmCreate"
        />

        <InstallTemplateDialog
            :value.sync="showInstallTemplateDialog"
        />
    </article>
</template>
  
<script>
    import dayjs from 'dayjs'
    import Logo from '@/components/Logo'
    import SearchSelect from '@blueking/search-select'
    import '@blueking/search-select/dist/styles/index.css'
    import templateTable from './templateTable'
    import CreateTemplateDialog from './CreateTemplateDialog'
    import InstallTemplateDialog from './InstallTemplateDialog'
    // import { navConfirm } from '@/utils/util'
    import {
        RESOURCE_ACTION,
        TEMPLATE_RESOURCE_ACTION,
        PROJECT_RESOURCE_ACTION,
        TEMPLATE_CREATE
    } from '@/utils/permission'

    export default {
        components: {
            Logo,
            SearchSelect,
            templateTable,
            CreateTemplateDialog,
            InstallTemplateDialog
        },
        data () {
            return {
                hasCreatePermission: false,
                activeTab: 'UNKNOWN',
                searchValue: [],
                isLoading: false,
                tableData: [],
                pagination: {
                    current: 1,
                    count: 6,
                    limit: 20
                },
                copyTemp: {
                    isShow: false,
                    title: this.$t('template.saveAsTemplate'),
                    closeIcon: false,
                    quickClose: true,
                    padding: '0 20px',
                    srcTemplateId: '',
                    templateName: '',
                    isCopySetting: true
                },
                copySettings: [
                    { label: this.$t('true'), value: true },
                    { label: this.$t('false'), value: false }
                ],
                showAddTemplateDialog: false,
                showInstallTemplateDialog: false
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
                    id: 'fuzzySearchName'
                }, {
                    name: this.$t('template.desc'),
                    id: 'desc'
                }, {
                    name: this.$t('template.type'),
                    id: 'type'
                }, {
                    name: this.$t('template.source'),
                    id: 'source'
                }, {
                    name: this.$t('template.lastModifiedBy'),
                    id: 'updater'
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
            this.hasPipelineTemplatePermission()
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
            async hasPipelineTemplatePermission () {
                try {
                    this.hasCreatePermission = await this.$store.dispatch('pipelines/hasPipelineTemplatePermission', {
                        projectId: this.projectId,
                        permission: TEMPLATE_CREATE
                    })
                } catch (err) {
                    this.$showTips({ message: err.message || err, theme: 'error' })
                }
            },
            async fetchTableData (params = { }) {
                this.isLoading = true
                try {
                    const param = {
                        projectId: this.projectId,
                        page: this.pagination.current,
                        pageSize: this.pagination.limit,
                        ...(this.activeTab !== 'UNKNOWN' && { type: this.activeTab }),
                        ...params
                    }
                    const res = await this.$store.dispatch('pipelines/getTemplateList', param)

                    this.tableData = (res.records || []).map(x => {
                        x.updateTime = dayjs(x.updateTime).format('YYYY-MM-DD HH:mm:ss')
                        x.templateActions = [
                            {
                                text: this.$t('copy'), // 复制
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
                                text: this.$t('template.shelfStore'), // 上架研发商店
                                handler: this.toRelativeStore,
                                hasPermission: x.canEdit,
                                disablePermissionApi: true,
                                permissionData: {
                                    projectId: this.projectId,
                                    resourceType: 'pipeline_template',
                                    resourceCode: x.id,
                                    action: this.TEMPLATE_RESOURCE_ACTION.EDIT
                                }
                            },
                            {
                                text: this.$t('template.convertToCustom'), // 转为自定义
                                // handler: this.toRelativeStore,
                                hasPermission: x.canEdit,
                                disablePermissionApi: true,
                                permissionData: {
                                    projectId: this.projectId,
                                    resourceType: 'pipeline_template',
                                    resourceCode: x.id,
                                    action: this.TEMPLATE_RESOURCE_ACTION.EDIT
                                }
                            },
                            {
                                text: this.$t('template.export'), // 导出
                                // handler: this.toRelativeStore,
                                hasPermission: x.canEdit,
                                disablePermissionApi: true,
                                permissionData: {
                                    projectId: this.projectId,
                                    resourceType: 'pipeline_template',
                                    resourceCode: x.id,
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
                                    resourceCode: x.id,
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
            handleShowCreateTemplateDialog () {
                this.showAddTemplateDialog = true
            },
            handleShowInstallTemplateDialog () {
                this.showInstallTemplateDialog = true
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
            },
            /**
             * 复制
             * @param row
             */
            copyTemplate (row) {
                if (!row.canEdit) return

                this.copyTemp.templateName = `${row.name}_copy`
                this.copyTemp.isShow = true
                this.copyTemp.srcTemplateId = row.id
            },
            /**
             * 上架研发商店-关联商店
             * @param row
             */
            toRelativeStore (row) {
            // if (!this.isEnabledPermission && !row.canEdit) return

            // const href = `${WEB_URL_PREFIX}/store/workList/template?projectCode=${this.projectId}&templateId=${row.id}`
            // window.open(href, '_blank')
            },
            /**
             * 删除模板
             * @param row
             */
            deleteTemplate (row) {
            // if (!this.isEnabledPermission && !row.canEdit) return
            // const content = `${this.$t('template.deleteTemplateTips', [row.name])}`

            // navConfirm({ type: 'warning', content })
            //     .then((val) => {
            //         val && this.confirmDeleteTemplate(row)
            //     }).catch(() => {})
            },
            async confirmDeleteTemplate (row) {
            // this.$parent.isLoading = true
            // try {
            //     await this.$store.dispatch('pipelines/deleteTemplate', {
            //         projectId: this.projectId,
            //         templateId: row.id
            //     })

            //     this.getListData()
            //     this.$showTips({ message: this.$t('template.deleteSuc'), theme: 'success' })
            // } catch (err) {
            //     this.$showTips({
            //         message: err.message || err,
            //         theme: 'error'
            //     })
            // } finally {
            //     this.$parent.isLoading = false
            // }
            },

            async copyConfirmHandler (row) {
                const valid = await this.$validator.validate()
                if (!valid) return

                this.$parent.isLoading = true
                const templateName = this.copyTemp.templateName || ''
                if (!templateName.trim()) {
                    this.copyTemp.nameHasError = true; return
                }

                const postData = {
                    projectId: this.projectId,
                    srcTemplateId: this.copyTemp.srcTemplateId,
                    copySetting: this.copyTemp.isCopySetting,
                    templateName: this.copyTemp.templateName
                }
                console.log('🚀 ~ copyConfirmHandler ~ postData:', postData)
            // this.$store.dispatch('pipelines/copyTemplate', postData).then(({ id: templateId }) => {
            //     this.copyCancelHandler()
            //     this.$showTips({ message: this.$t('template.copySuc'), theme: 'success' })
            //     this.$router.push({
            //         name: 'templateEdit',
            //         params: { templateId }
            //     })
            // }).catch((err) => {
            //     const message = err.message || err
            //     this.$showTips({ message, theme: 'error' })
            // }).finally(() => {
            //     this.$parent.isLoading = false
            // })
            },

            copyCancelHandler () {
                this.copyTemp.isShow = false
                this.copyTemp.templateName = ''
                this.copyTemp.pipelineId = ''
                this.copyTemp.nameHasError = false
                this.copyTemp.isCopySetting = true
            },

            handleConfirmCreate () {
                this.tableData = []
                this.pagination.current = 1
                this.fetchTableData()
            }
        }
    }
</script>

<style lang="scss" scoped>
.template-manage-entry{
    width: 100%;
    height: 100%;
    overflow: hidden;
    display: flex;
    box-sizing: border-box;
    border: 1px solid #dcdee5;

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
        width: calc(100% - 240px);
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
.form-dialog {
     .err-name {
         text-align: left;
         margin-left: 150px;
         margin-bottom: -21px;
     }
     .form-radio {
         margin-right: 30px;
         margin-top: 5px;
     }
 }
</style>

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
                        @click="handleCreateClick"
                    >
                        新建模板
                    </bk-button>
                    <bk-button
                        :disabled="!hasCreatePermission"
                    >
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
                                v-bk-tooltips.bottom-end="$t('template.tipsSetting')"
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
    </article>
</template>
  
<script>
    import dayjs from 'dayjs'
    import Logo from '@/components/Logo'
    import SearchSelect from '@blueking/search-select'
    import '@blueking/search-select/dist/styles/index.css'
    import templateTable from './templateTable'
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
            templateTable
        },
        data () {
            const that = this
            return {
                hasCreatePermission: false,
                activeTab: 'ALL',
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
                    title: that.$t('template.saveAsTemplate'),
                    closeIcon: false,
                    quickClose: true,
                    padding: '0 20px',
                    srcTemplateId: '',
                    templateName: '',
                    isCopySetting: true
                },
                copySettings: [
                    { label: that.$t('true'), value: true },
                    { label: that.$t('false'), value: false }
                ],
                navList: [
                    {
                        label: this.$t('template.allTemplate'),
                        name: 'ALL',
                        isAll: true,
                        icon: 'group'
                    },
                    {
                        label: this.$t('template.pipelineTemplate'),
                        name: 'PIPELINE',
                        icon: 'pipeline'
                    },
                    {
                        label: this.$t('template.stageTemplate'),
                        name: 'STAGE',
                        icon: 'pipeline-group-item-icon'
                    },
                    {
                        label: this.$t('template.jobTemplate'),
                        name: 'JOB',
                        icon: 'pipeline-group-item-icon'
                    },
                    {
                        label: this.$t('template.stepTemplate'),
                        name: 'STEP',
                        icon: 'pipeline-group-item-icon'
                    }
                ]
            }
        },
        computed: {
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
            const historyTab = localStorage.getItem('TEMPLATE_TYPE_CACHE')
            if (historyTab) {
                this.activeTab = historyTab
            }
            this.getType2Count()
            this.fetchTableData()
            this.hasPipelineTemplatePermission()
        },
        methods: {
            handleChangeMenu (name) {
                this.activeTab = name
                localStorage.setItem('TEMPLATE_TYPE_CACHE', this.activeTab)
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
            async getType2Count () {
                try {
                    const nums = await this.$store.dispatch('pipelines/getType2Count', {
                        projectId: this.projectId
                    })
                    this.navList = this.navList.map(item => {
                        const key = item.name.toLowerCase()
                        item.num = nums[key] || 0
                        return item
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
                        ...(this.activeTab !== 'ALL' && { type: this.activeTab }),
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
                                isShow: true,
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
                                isShow: x.source === 'MARKET',
                                permissionData: {
                                    projectId: this.projectId,
                                    resourceType: 'pipeline_template',
                                    resourceCode: x.id,
                                    action: this.TEMPLATE_RESOURCE_ACTION.EDIT
                                }
                            },
                            {
                                text: this.$t('template.convertToCustom'), // 转为自定义
                                handler: this.convertToCustom,
                                hasPermission: x.canEdit,
                                disablePermissionApi: true,
                                isShow: x.source === 'CUSTOM',
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
                                isShow: true,
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
                                isShow: true,
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
            handleCreateClick () {

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
                if (!row.canEdit) return

                const href = `${WEB_URL_PREFIX}/store/workList/template?projectCode=${this.projectId}&templateId=${row.id}`
                window.open(href, '_blank')
            },
            /**
             * 转为自定义
             * @param row
             */
            convertToCustom (row) {
                if (!row.canEdit) return

                const h = this.$createElement
                this.$bkInfo({
                    width: 480,
                    title: this.$t('template.templateToCustom'),
                    extCls: 'custom_template',
                    subHeader: h('div', [
                        h('p', {
                            class: 'template-title',
                            directives: [
                                {
                                    name: 'bk-tooltips',
                                    value: row.name
                                }
                            ]
                        }, [
                            h('span', `${this.$t('templateName')} : `),
                            h('span', { class: 'template-name-info' }, row.name)
                        ]),
                        h('div', { class: 'custom-tip' }, this.$t('template.customTip'))
                    ]),
                    confirmLoading: true,
                    confirmFn: () => {
                        
                    }
                })
            },
            /**
             * 删除模板
             * @param row
             */
            deleteTemplate (row) {
                if (!row.canEdit) return
                const title = row.source === 'CUSTOM' ? this.$t('template.deleteCustom') : this.$t('template.deleteStore')

                const h = this.$createElement
                this.$bkInfo({
                    title,
                    okText: this.$t('delete'),
                    extCls: 'delete_template',
                    subHeader: h('div', [
                        h('p', { class: 'template-title' }, [
                            h('span', `${this.$t('templateName')} : `),
                            h('span', { class: 'template-name-info' }, row.name)
                        ])
                    ]),
                    confirmLoading: true,
                    confirmFn: () => {
                        this.confirmDeleteTemplate(row)
                    }
                })
            },
            async confirmDeleteTemplate (row) {
                this.isLoading = true
                try {
                    await this.$store.dispatch('pipelines/templateDelete', {
                        projectId: this.projectId,
                        templateId: row.id
                    })

                    this.fetchTableData()
                    this.$showTips({ message: this.$t('template.deleteSuc'), theme: 'success' })
                } catch (err) {
                    this.$showTips({
                        message: err.message || err,
                        theme: 'error'
                    })
                } finally {
                    this.isLoading = false
                }
            },
            async copyConfirmHandler (row) {
                const valid = await this.$validator.validate()
                if (!valid) return

                this.isLoading = true
                const templateName = this.copyTemp.templateName || ''
                if (!templateName.trim()) {
                    this.copyTemp.nameHasError = true; return
                }

                const postData = {
                    projectId: this.projectId,
                    srcTemplateId: this.copyTemp.srcTemplateId,
                    copySetting: this.copyTemp.isCopySetting,
                    name: this.copyTemp.templateName
                }
                this.$store.dispatch('pipelines/templateCopy', postData).then((templateId) => {
                    console.log('🚀 ~ templateId:', templateId)
                    this.copyCancelHandler()
                    this.$showTips({ message: this.$t('template.copySuc'), theme: 'success' })
                    // this.$router.push({
                    //     name: 'templateEdit',
                    //     params: { templateId }
                    // })
                }).catch((err) => {
                    const message = err.message || err
                    this.$showTips({ message, theme: 'error' })
                }).finally(() => {
                    this.isLoading = false
                })
            },
            copyCancelHandler () {
                this.copyTemp.isShow = false
                this.copyTemp.templateName = ''
                this.copyTemp.pipelineId = ''
                this.copyTemp.nameHasError = false
                this.copyTemp.isCopySetting = true
            }
        }
    }
</script>

<style lang="scss" scoped>
@import '@/scss/mixins/ellipsis';

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
    }
}
.template-name-info {
    color: #313238;
}
.custom-tip {
    margin-top: 16px;
    padding: 12px 16px;
    background: #F5F7FA;
    border-radius: 2px;
}
.template-title {
    width: 336px;
    @include ellipsis();
}
</style>
<style>
.delete_template {
    font-size: 14px;
    color: #4D4F56;

    .bk-dialog-sub-header {
        padding-left: 24px !important;
    }
    .bk-dialog-footer {
        padding: 0 65px 48px !important;
    }
}
.custom_template {
    font-size: 14px;
    color: #4D4F56;
    .bk-dialog-sub-header {
        padding: 5px 32px 21px !important;
    }
    .bk-dialog-footer {
        padding: 0 65px 24px !important;
    }
}
</style>

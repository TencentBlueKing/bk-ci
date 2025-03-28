<template>
    <article class="template-manage-entry">
        <div class="template-main">
            <div class="template-main-header">
                <div class="search-group">
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
                    <ul class="search-tab">
                        <li
                            v-for="(item, idx) in searchTab"
                            :key="idx"
                        >
                            <p
                                :class="[currentMode === item.countKey ? 'active' : '']"
                                v-bk-overflow-tips="{ content: `${$t(item.i18nKey)}${countMap[item.countKey] ?? 0}` }"
                                @click="handleChangeSearchTab(item.countKey)"
                            >
                                <Logo
                                    :class="item.icon"
                                    size="13"
                                    :name="item.icon"
                                />
                                <span>{{ $t(item.i18nKey) }}</span>
                                <span class="nav-num">({{ countMap[item.countKey] ?? 0 }})</span>
                            </p>
                        </li>
                    </ul>
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
                :type="templateViewId"
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
        <CreateTemplateDialog
            :value.sync="showAddTemplateDialog"
            @confirm="handleConfirmCreate"
        />
        <InstallTemplateDialog
            :value.sync="showInstallTemplateDialog"
        />
    </article>
</template>

<script setup>
    import { onMounted, ref, computed, nextTick, watch } from 'vue'
    import dayjs from 'dayjs'
    import SearchSelect from '@blueking/search-select'
    import '@blueking/search-select/dist/styles/index.css'
    import templateTable from './templateTable'
    import CreateTemplateDialog from './CreateTemplateDialog'
    import InstallTemplateDialog from './InstallTemplateDialog'
    import UseInstance from '@/hook/useInstance'
    import Logo from '@/components/Logo'
    import {
        RESOURCE_ACTION,
        TEMPLATE_RESOURCE_ACTION
    } from '@/utils/permission'
    import {
        // TEMPLATE_VIEW_ID_CACHE,
        TEMPLATE_VIEW_ID_MAP,
        ALL_TEMPLATE_VIEW_ID,
        TEMPLATE_ACTION_MAP,
        ALL_SOURCE,
        CUSTOM_SOURCE,
        MARKET_SOURCE
    } from '@/store/modules/templates/constants'

    const { proxy, i18n, bkMessage, bkInfo, h, validator } = UseInstance()
    const hasCreatePermission = ref(false)
    const searchValue = ref([])
    const isLoading = ref(false)
    const tableData = ref([])
    const showAddTemplateDialog = ref(false)
    const showInstallTemplateDialog = ref(false)
    const pagination = ref({
        current: 1,
        count: 0,
        limit: 20
    })
    const copyTemp = ref({
        isShow: false,
        title: i18n.t('template.saveAsTemplate'),
        closeIcon: false,
        quickClose: true,
        padding: '0 20px',
        srcTemplateId: '',
        templateName: '',
        isCopySetting: true
    })
    const copySettings = ref([
        { label: i18n.t('true'), value: true },
        { label: i18n.t('false'), value: false }
    ])
    const filterData = computed(() => [
        {
            name: i18n.t('template.name'),
            id: 'fuzzySearchName'
        },
        {
            name: i18n.t('template.desc'),
            id: 'desc'
        },
        {
            name: i18n.t('template.source'),
            id: 'source'
        },
        {
            name: i18n.t('template.lastModifiedBy'),
            id: 'updater'
        }
    ])
    const countMap = ref({})
    const currentMode = ref('all')
    const searchTab = ref([
        {
            i18nKey: ALL_SOURCE,
            icon: 'template-application',
            countKey: 'all'
        },
        {
            i18nKey: CUSTOM_SOURCE,
            icon: 'template-view',
            countKey: 'custom'
        },
        {
            i18nKey: MARKET_SOURCE,
            icon: 'template-view',
            countKey: 'market'
        }
    ])
    const modeMap = {
        all: 'ALL',
        custom: 'CUSTOMIZE',
        market: 'CONSTRAINT'
    }
    const projectId = computed(() => proxy.$route.params.projectId)
    const templateViewId = computed(() => {
        return proxy.$route.params.viewId
    })
    const searchParams = computed(() => searchValue.value.reduce((acc, filter) => {
        acc[filter.id] = filter.values.map(val => val.id).join(',')
        return acc
    }, {}))

    watch(() => searchValue.value, () => {
        fetchTableData()
    })
    watch(() => templateViewId.value, () => {
        searchValue.value = []
    })
    onMounted(() => {
        searchValue.value = echoQueryParameters()
        hasPipelineTemplatePermission()
    })

    async function fetchTypeCount () {
        try {
            const postData = {
                projectId: projectId.value,
                ...searchParams.value
            }
            const res = await proxy.$store.dispatch('templates/getSourceCount', postData)
            countMap.value = res
        } catch (error) {
            console.log('error', error)
        }
    }

    function echoQueryParameters () {
        const { mode, ...restQuery } = proxy.$route.query
        currentMode.value = mode || 'all'
        const result = []
        if (restQuery) {
            for (const [key, value] of Object.entries(restQuery)) {
                const filterItem = filterData.value.find(item => item.id === key)
                if (filterItem) {
                    result.push({
                        id: key,
                        name: filterItem.name,
                        values: [
                            {
                                id: value,
                                name: value
                            }
                        ]
                    })
                }
            }
            return result
        }
    }

    async function hasPipelineTemplatePermission () {
        try {
            hasCreatePermission.value = await proxy.$store.dispatch('templates/hasPipelineTemplatePermission', {
                projectId: projectId.value,
                permission: TEMPLATE_ACTION_MAP.CREATE
            })
        } catch (err) {
            bkMessage({ message: err.message || err, theme: 'error' })
        }
    }
            
    async function fetchTableData () {
        isLoading.value = true
        try {
            const param = {
                projectId: projectId.value,
                page: pagination.value.current,
                pageSize: pagination.value.limit,
                ...(currentMode.value !== 'all' && { mode: modeMap[currentMode.value] }),
                ...(templateViewId.value !== ALL_TEMPLATE_VIEW_ID && { type: TEMPLATE_VIEW_ID_MAP[templateViewId.value] }),
                ...searchParams.value
            }
            proxy.$router.replace({
                query: {
                    mode: currentMode.value,
                    ...searchParams.value
                }
            })
            fetchTypeCount()
            const res = await proxy.$store.dispatch('templates/getTemplateList', param)
            tableData.value = (res.records || []).map(x => {
                x.updateTime = dayjs(x.updateTime).format('YYYY-MM-DD HH:mm:ss')
                x.templateActions = [
                    {
                        text: i18n.t('copy'), // 复制
                        handler: copyTemplate,
                        hasPermission: x.canEdit,
                        disablePermissionApi: true,
                        isShow: true,
                        permissionData: {
                            projectId: projectId.value,
                            resourceType: 'pipeline_template',
                            resourceCode: projectId.value,
                            action: RESOURCE_ACTION.CREATE
                        }
                    },
                    {
                        text: i18n.t('template.shelfStore'), // 上架研发商店
                        handler: toRelativeStore,
                        hasPermission: x.canEdit,
                        disablePermissionApi: true,
                        isShow: x.mode === 'CUSTOMIZE',
                        permissionData: {
                            projectId: projectId.value,
                            resourceType: 'pipeline_template',
                            resourceCode: x.id,
                            action: TEMPLATE_RESOURCE_ACTION.EDIT
                        }
                    },
                    {
                        text: i18n.t('template.convertToCustom'), // 转为自定义
                        handler: convertToCustom,
                        hasPermission: x.canEdit,
                        disablePermissionApi: true,
                        isShow: x.source === 'MARKET',
                        permissionData: {
                            projectId: projectId.value,
                            resourceType: 'pipeline_template',
                            resourceCode: x.id,
                            action: TEMPLATE_RESOURCE_ACTION.EDIT
                        }
                    },
                    {
                        text: i18n.t('template.export'), // 导出
                        handler: exportTemplate,
                        hasPermission: x.canEdit,
                        disablePermissionApi: true,
                        isShow: true,
                        permissionData: {
                            projectId: projectId.value,
                            resourceType: 'pipeline_template',
                            resourceCode: x.id,
                            action: TEMPLATE_RESOURCE_ACTION.EDIT
                        }
                    },
                    {
                        text: i18n.t('delete'),
                        handler: deleteTemplate,
                        hasPermission: x.canDelete,
                        disablePermissionApi: true,
                        isShow: true,
                        permissionData: {
                            projectId: projectId.value,
                            resourceType: 'pipeline_template',
                            resourceCode: x.id,
                            action: TEMPLATE_RESOURCE_ACTION.EDIT
                        }
                    }
                ]
                return x
            })
            pagination.value.count = res.count
        } catch (err) {
            bkMessage({
                message: err.message || err,
                theme: 'error'
            })
        } finally {
            isLoading.value = false
        }
    }
    function handlePageLimitChange (limit) {
        pagination.value.limit = limit
        fetchTableData()
    }
    function handlePageChange (page) {
        pagination.value.current = page
        fetchTableData()
    }
    function handleShowCreateTemplateDialog () {
        showAddTemplateDialog.value = true
    }
    function handleShowInstallTemplateDialog () {
        showInstallTemplateDialog.value = true
    }
    function handleChangeSearchTab (key) {
        currentMode.value = key
        fetchTableData()
    }
    function handleSearchChange (value) {
        searchValue.value = value
    }
    function handleClear () {
        searchValue.value = []
        fetchTableData()
    }
    /**
     * 复制
     * @param row
     */
    function copyTemplate (row) {
        if (!row.canEdit) return

        copyTemp.value.templateName = `${row.name}_copy`
        copyTemp.value.isShow = true
        copyTemp.value.srcTemplateId = row.id
    }
    /**
     * 上架研发商店-关联商店
     * @param row
     */
    function toRelativeStore (row) {
        if (!row.canEdit) return

        const href = `${WEB_URL_PREFIX}/store/workList/template?projectCode=${projectId.value}&templateId=${row.id}`
        window.open(href, '_blank')
    }
    /**
     * 转为自定义
     * @param row
     */
    async function convertToCustom (row) {
        if (!row.canEdit) return
        nextTick(() => {
            bkInfo({
                width: 480,
                title: i18n.t('template.templateToCustom'),
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
                        h('span', `${i18n.t('templateName')} : `),
                        h('span', { class: 'template-name-info' }, row.name)
                    ]),
                    h('div', { class: 'custom-tip' }, i18n.t('template.customTip'))
                ]),
                confirmLoading: true,
                confirmFn: () => {
                            
                }
            })
        })
    }
    /**
     * 删除模板
     * @param row
     */
    function deleteTemplate (row) {
        if (!row.canEdit) return
        const title = row.source === 'CUSTOM' ? i18n.t('template.deleteCustom') : i18n.t('template.deleteStore')

        bkInfo({
            title,
            okText: i18n.t('delete'),
            extCls: 'delete_template',
            subHeader: h('div', [
                h('p', {
                    class: 'template-title-delete',
                    directives: [
                        {
                            name: 'bk-tooltips',
                            value: row.name
                        }
                    ]
                }, [
                    h('span', `${i18n.t('templateName')} : `),
                    h('span', { class: 'template-name-info' }, row.name)
                ])
            ]),
            confirmLoading: true,
            confirmFn: () => {
                confirmDeleteTemplate(row)
            }
        })
    }
    async function confirmDeleteTemplate (row) {
        isLoading.value = true
        try {
            await proxy.$store.dispatch('templates/deleteTemplate', {
                projectId: projectId.value,
                templateId: row.id
            })

            fetchTableData()
            bkMessage({ message: i18n.t('template.deleteSuc'), theme: 'success' })
        } catch (err) {
            bkMessage({
                message: err.message || err,
                theme: 'error'
            })
        } finally {
            isLoading.value = false
        }
    }
    /**
     * 导出模板
     * @param row
     */
    async function exportTemplate (row) {
        try {
            const params = {
                projectId: projectId.value,
                templateId: row.id
            }
            const res = await proxy.$store.dispatch('templates/exportYamlTemplate', params, {
                responseType: 'blob'
            })
            const blob = new Blob([res], { type: 'application/x-yaml' })
            const url = window.URL || window.webkitURL || window.moxURL

            const a = document.createElement('a')
            a.href = url.createObjectURL(blob)
            a.download = `${row.name}.yaml`
            a.click()
            window.URL.revokeObjectURL(url)
        } catch (error) {
            bkMessage({
                message: error.message || error,
                theme: 'error'
            })
        }
    }
    /**
     * 复制模板
     * @param row
     */
    async function copyConfirmHandler (row) {
        const valid = await validator.validate()
        if (!valid) return
        isLoading.value = true
        const templateName = copyTemp.value.templateName || ''
        if (!templateName.trim()) {
            copyTemp.value.nameHasError = true; return
        }

        const postData = {
            projectId: projectId.value,
            params: {
                srcTemplateId: copyTemp.value.srcTemplateId,
                copySetting: copyTemp.value.isCopySetting,
                name: copyTemp.value.templateName
            }
        }
        try {
            const res = await proxy.$store.dispatch('templates/templateCopy', postData)
            copyCancelHandler()
            bkMessage({ message: i18n.t('template.copySuc'), theme: 'success' })
            proxy.$router.push({
                name: 'TemplateOverview',
                params: {
                    templateId: res.templateId,
                    version: res.version,
                    type: 'instanceList'
                }
            })
        } catch (error) {
            const message = error.message || error
            bkMessage({ message, theme: 'error' })
        } finally {
            isLoading.value = false
        }
    }
    function copyCancelHandler () {
        copyTemp.value.isShow = false
        copyTemp.value.templateName = ''
        copyTemp.value.pipelineId = ''
        copyTemp.value.nameHasError = false
        copyTemp.value.isCopySetting = true
    }

    function handleConfirmCreate () {
        pagination.value.current = 1
        fetchTableData()
    }
</script>

<style lang="scss" scoped>
@import '@/scss/conf';
.template-manage-entry{
    width: 100%;
    height: 100%;
    overflow: hidden;
    display: flex;
    box-sizing: border-box;
    border: 1px solid #dcdee5;

   .template-main {
        width: 100%;
        padding: 24px 36px 12px 24px;
        &-header {
            display: flex;
            justify-content: space-between;
            margin-bottom: 16px;
            height: 32px;

            &-group {
                display: flex;
            }

            .search-group {
                display: flex;

                .search-tab {
                    display: flex;
                    padding: 4px;
                    margin-left: 8px;
                    background: #EAEBF0;
                    height: 32px;
                    border-radius: 2px;
    
                    li{
                        height: 24px;
                        line-height: 24px;
                        font-size: 12px;
                        color: #4D4F56;
                        text-align: center;
                        border-radius: 2px;
                        cursor: pointer;

                        p {
                            padding: 0 12px;
                            max-width: 120px;
                            overflow: hidden;
                            text-overflow: ellipsis;
                            white-space: nowrap;
                        }
    
                        svg {
                            vertical-align: middle;
                            color: #979BA5;
                            margin-right: 4px;
                        }
                    }
    
                    .active {
                        padding: 0 12px;
                        background-color: #fff;
                        color: $primaryColor;
                        border-radius: 2px;
      
                        svg {
                            color: $primaryColor;
                        }
                    }
                }
    
            }
            .search-input {
                background: white;
                flex: 1;
                min-width: 200px;
                margin-left: 10vw;
                ::placeholder {
                    color: #c4c6cc;
                }
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
</style>
<style lang="scss">
@import '@/scss/mixins/ellipsis';

.template-title {
    max-width: 415px;
    @include ellipsis();
}
.template-title-delete {
    max-width: 355px;
    @include ellipsis();
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

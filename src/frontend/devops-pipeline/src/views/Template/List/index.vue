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
                        class="ml10"
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
            <template-table
                ref="selfTemp"
                :type="templateViewId"
                :data="tableData"
                :pagination="pagination"
                :is-loading="isTableLoading"
                @limit-change="handlePageLimitChange"
                @page-change="handlePageChange"
                @clear="handleClear"
            />
        </div>

        <CopyTemplateDialog
            :copy-temp="copyTemp"
            @confirm="copyConfirmHandler"
            @cancel="copyCancelHandler"
        />
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
    import Logo from '@/components/Logo'
    import CopyTemplateDialog from '@/components/Template/CopyTemplateDialog.vue'
    import UseInstance from '@/hook/useInstance'
    import useTemplateActions from '@/hook/useTemplateActions'
    import {
        ALL_SOURCE,
        ALL_TEMPLATE_VIEW_ID,
        CUSTOM_SOURCE,
        MARKET_SOURCE,
        TEMPLATE_ACTION_MAP,
        TEMPLATE_VIEW_ID_MAP
    } from '@/store/modules/templates/constants'
    import {
        RESOURCE_ACTION,
        TEMPLATE_RESOURCE_ACTION
    } from '@/utils/permission'
    import SearchSelect from '@blueking/search-select'
    import '@blueking/search-select/dist/styles/index.css'
    import dayjs from 'dayjs'
    import { computed, onMounted, ref, watch } from 'vue'
    import CreateTemplateDialog from './CreateTemplateDialog'
    import InstallTemplateDialog from './InstallTemplateDialog'
    import TemplateTable from './templateTable'

    const {
        copyTemp,
        isTableLoading,
        copyTemplate,
        exportTemplate,
        deleteTemplate,
        copyConfirmHandler,
        copyCancelHandler,
        goTemplateOverview,
        toRelativeStore,
        convertToCustom
    } = useTemplateActions()

    const { proxy, i18n, bkMessage } = UseInstance()
    const hasCreatePermission = ref(false)
    const searchValue = ref([])
    const tableData = ref([])
    const showAddTemplateDialog = ref(false)
    const showInstallTemplateDialog = ref(false)
    const pagination = ref({
        current: 1,
        count: 0,
        limit: 20
    })
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
        isTableLoading.value = true
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
                        handler: () => copyTemplate(x),
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
                        handler: () => toRelativeStore(x),
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
                        handler: () => convertToCustom(x, fetchTableData),
                        hasPermission: x.canEdit,
                        disablePermissionApi: true,
                        isShow: x.mode === 'CONSTRAINT',
                        permissionData: {
                            projectId: projectId.value,
                            resourceType: 'pipeline_template',
                            resourceCode: x.id,
                            action: TEMPLATE_RESOURCE_ACTION.EDIT
                        }
                    },
                    {
                        text: i18n.t('template.export'), // 导出
                        handler: () => exportTemplate(x),
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
                        handler: () => deleteTemplate(x, fetchTableData),
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
            isTableLoading.value = false
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
    function handleConfirmCreate (createData) {
        pagination.value.current = 1
        goTemplateOverview(createData)
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
        display: flex;
        flex-direction: column;
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

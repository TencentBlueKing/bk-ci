<template>
    <section class="pipeline-public-var-main">
        <bk-alert
            v-if="showPublicVarTips"
            class="public-var-tips"
            type="info"
            closable
            :title="$t('publicVar.publicVarTips')"
            @close="handleClosePublicVarTips"
        />
        <div
            :class="{
                'public-var-list-box': true,
                'not-tips': !showPublicVarTips
            }"
        >
            <div class="header-wrapper">
                <div>
                    <bk-button
                        theme="primary"
                        icon="plus"
                        class="mr10"
                        v-perm="{
                            permissionData: {
                                projectId: projectId,
                                resourceType: RESOURCE_TYPE.VARIABLE,
                                resourceCode: projectId,
                                action: VARIABLE_RESOURCE_ACTION.CREATE
                            }
                        }"
                        @click="handleAddGroup"
                    >
                        {{ $t('publicVar.addParamGroup') }}
                    </bk-button>
                    <bk-button
                        @click="handleShowImportDialog"
                        v-perm="{
                            permissionData: {
                                projectId: projectId,
                                resourceType: RESOURCE_TYPE.VARIABLE,
                                resourceCode: projectId,
                                action: VARIABLE_RESOURCE_ACTION.CREATE
                            }
                        }"
                    >
                        {{ $t('publicVar.importParamGroup') }}
                    </bk-button>
                </div>
                <search-select
                    ref="searchSelect"
                    class="search-input"
                    v-model="searchValue"
                    :data="searchList"
                    clearable
                    :show-condition="false"
                    :placeholder="filterTips"
                />
            </div>
            <bk-table
                :class="{
                    'public-list-table': true,
                    'show-detail': showDetail
                }"
                :data="renderTableData"
                height="100%"
                v-bkloading="{ isLoading }"
                :row-class-name="rowClassName"
                :row-style="rowStyle"
                :pagination="pagination"
                @row-click="handleRowClick"
                @page-change="handlePageChange"
                @page-limit-change="handlePageLimitChange"
            >
                <bk-table-column
                    :label="$t('publicVar.paramGroupId')"
                    width="200"
                    prop="groupName"
                    show-overflow-tooltip
                >
                    <template slot-scope="{ row }">
                        <a
                            class="group-name-link"
                            v-perm="{
                                hasPermission: row?.permission?.canView,
                                disablePermissionApi: true,
                                permissionData: {
                                    projectId: projectId,
                                    resourceType: RESOURCE_TYPE.VARIABLE,
                                    resourceCode: row.groupName,
                                    action: VARIABLE_RESOURCE_ACTION.VIEW
                                }
                            }"
                            @click="handleViewDetail(row)"
                        >
                            {{ row.groupName }}
                        </a>
                    </template>
                </bk-table-column>
                <bk-table-column
                    width="140"
                    prop="referCount"
                    :label="$t('publicVar.pipelineNum')"
                >
                    <template slot-scope="{ row }">
                        <bk-button
                            text
                            v-perm="{
                                hasPermission: row?.permission?.canView,
                                disablePermissionApi: true,
                                permissionData: {
                                    projectId: projectId,
                                    resourceType: RESOURCE_TYPE.VARIABLE,
                                    resourceCode: row.groupName,
                                    action: VARIABLE_RESOURCE_ACTION.VIEW
                                }
                            }"
                            @click="handleViewRef(row)"
                        >
                            {{ row.referCount ?? '--' }}
                        </bk-button>
                    </template>
                </bk-table-column>
                <template v-if="!showDetail">
                    <bk-table-column
                        :label="$t('publicVar.paramNum')"
                        width="140"
                        prop="varCount"
                    />
                    <bk-table-column
                        :label="$t('publicVar.paramGroupDesc')"
                        prop="desc"
                        show-overflow-tooltip
                    />
                    <bk-table-column
                        :label="$t('publicVar.lastModifiedBy')"
                        prop="modifier"
                        show-overflow-tooltip
                    />
                    <bk-table-column
                        :label="$t('publicVar.lastModifiedDate')"
                        prop="updateTime"
                        show-overflow-tooltip
                    />
                    <bk-table-column
                        :label="$t('publicVar.operation')"
                        width="140"
                    >
                        <template slot-scope="{ row }">
                            <div class="operation-wrapper">
                                <bk-button
                                    text
                                    class="mr10"
                                    v-perm="{
                                        hasPermission: row?.permission?.canDelete,
                                        disablePermissionApi: true,
                                        permissionData: {
                                            projectId: projectId,
                                            resourceType: RESOURCE_TYPE.VARIABLE,
                                            resourceCode: row.groupName,
                                            action: VARIABLE_RESOURCE_ACTION.EDIT
                                        }
                                    }"
                                    @click="handleEditGroup(row)"
                                >
                                    {{ $t('edit') }}
                                </bk-button>
                                <ext-menu
                                    ext-cls="more-trigger"
                                    :config="row.publicVarActions"
                                />
                            </div>
                        </template>
                    </bk-table-column>
                </template>
                <empty-exception
                    slot="empty"
                    :type="exceptionType"
                    @clear="handleClearSearchValue"
                >
                    <template
                        slot="sub-content"
                    >
                        <bk-button
                            v-if="exceptionType === 'empty'"
                            text
                            class="empty-tips"
                            v-perm="{
                                permissionData: {
                                    projectId: projectId,
                                    resourceType: RESOURCE_TYPE.VARIABLE,
                                    resourceCode: projectId,
                                    action: VARIABLE_RESOURCE_ACTION.CREATE
                                }
                            }"
                            @click="handleAddGroup"
                        >
                            {{ $t('publicVar.createNow') }}
                        </bk-button>
                    </template>
                </empty-exception>
            </bk-table>
            <param-group-detail
                ref="paramGroupDetailRef"
                :is-show.sync="showDetail"
                :title="detailTitle"
                :show-tips="showPublicVarTips"
                :show-type="showType"
                :read-only="readOnly"
                :default-tab="defaultTab"
                :handle-edit-group="handleEditGroup"
                @release-success="handleReleaseSuccess"
            />
            <importParamGroupPopup
                :is-show.sync="showImportDialog"
                :success-fn="handleImportSuccess"
            />
        </div>
    </section>
</template>

<script setup>
    import { ref, watch, computed, onMounted } from 'vue'
    import { convertTime, randomString, navConfirm } from '@/utils/util'
    import {
        ADD_VARIABLE,
        EDIT_VARIABLE,
        OPERATE_TYPE,
        PUBLIC_VAR_TIPS_CLOSED_TIME
    } from '@/store/modules/publicVar/constants'
    import {
        RESOURCE_TYPE,
        VARIABLE_RESOURCE_ACTION
    } from '@/utils/permission'
    import UseInstance from '@/hook/useInstance'
    import SearchSelect from '@blueking/search-select'
    import ParamGroupDetail from './ParamGroupDetail.vue'
    import ExtMenu from '@/components/pipelineList/extMenu'
    import EmptyException from '@/components/common/exception'
    import ImportParamGroupPopup from './ImportParamGroupPopup.vue'

    const { proxy } = UseInstance()
    const isLoading = ref(false)
    const searchValue = ref([])
    const showDetail = ref(false)
    const showType = ref('')
    const detailTitle = ref('')
    const showImportDialog = ref(false)
    const THREE_MONTHS_MS = 90 * 24 * 60 * 60 * 1000 // 三个月的毫秒数
    const showPublicVarTips = ref(checkShouldShowTips())
    const readOnly = ref(false)
    const tableData = ref([])
    const newNameFlag = ref('')
    const activeNameFlag = ref('')
    const defaultTab = ref('basicInfo')
    const paramGroupDetailRef = ref(null)
    const pagination = ref({
        current: 1,
        count: 0,
        limit: 20
    })
    const operateType = computed(() => proxy.$store.state.publicVar.operateType)
    const groupData = computed(() => proxy.$store.state.publicVar.groupData)
    const projectId = computed(() => proxy.$route.params?.projectId)
    const searchList = computed(() => {
        return [
            {
                name: proxy.$t('publicVar.varId'),
                id: 'filterByVarName',
                default: true
            },
            {
                name: proxy.$t('publicVar.varAlias'),
                id: 'filterByVarAlias'
            },
            {
                name: proxy.$t('publicVar.paramGroupId'),
                id: 'filterByGroupName'
            },
            {
                name: proxy.$t('publicVar.paramGroupDesc'),
                id: 'filterByGroupDesc'
            },
            {
                name: proxy.$t('publicVar.lastModifiedBy'),
                id: 'filterByUpdater'
            }
        ]
    })
    const searchParams = computed(() => {
        return searchValue.value.reduce((acc, filter) => {
            acc[filter.id] = filter.values.map(val => val.id).join(',')
            return acc
        }, {})
    })
    const filterTips = computed(() => searchList.value.map(item => item.name).join('/'))
    const exceptionType = computed(() => searchValue.value.length ? 'search-empty': 'empty')
    const renderTableData = computed(() => {
        return tableData.value.map((row) => {
            return {
                ...row,
                updateTime: convertTime(row.updateTime),
                publicVarActions: [
                    {
                        text: proxy.$t('copy'),
                        handler: handleCopyGroup,
                        data: row,
                        permissionData: {
                            projectId: projectId.value,
                            resourceType: RESOURCE_TYPE.VARIABLE,
                            resourceCode: projectId.value,
                            action: VARIABLE_RESOURCE_ACTION.CREATE
                        }
                    },
                    {
                        text: proxy.$t('publicVar.export'),
                        handler: handleExportGroup,
                        data: row
                    },
                    // {
                    //     text: proxy.$t('publicVar.offline'),
                    //     handler: handleOfflineGroup,
                    //     data: row,
                    // },
                    {
                        text: proxy.$t('delete'),
                        handler: handleDeleteGroup,
                        data: row,
                        hasPermission: row?.permission?.canDelete,
                        disablePermissionApi: true,
                        disable: row.referCount > 0,
                        permissionData: {
                            projectId: projectId,
                            resourceType: RESOURCE_TYPE.VARIABLE,
                            resourceCode: row.groupName,
                            action: VARIABLE_RESOURCE_ACTION.DELETE
                        }
                    },
                ]
            }
        })
    })
    watch(() => searchValue.value, () => {
        pagination.value.current = 1
        fetchVariableGroupList()
    })

    function handleShowImportDialog () {
        showImportDialog.value = true
        proxy.$store.dispatch('publicVar/updateOperateType', OPERATE_TYPE.CREATE)
    }

    async function confirmLeaveDetail () {
        // 如果当前已经在详情页，检查是否有未保存的变更
        if (showDetail.value && paramGroupDetailRef.value?.hasGroupDataChanged()) {
            try {
                const leave = await navConfirm({
                    content: proxy.$t('editPage.closeConfirmMsg'),
                    type: 'warning',
                    cancelText: proxy.$t('cancel')
                })
                if (!leave) {
                    // 用户取消，不切换
                    return false
                }
            } catch (e) {
                // 用户取消
                return false
            }
        }
        return true
    }

    async function handleViewDetail (row) {
        const canLeave = await confirmLeaveDetail()
        if (!canLeave) return
        paramGroupDetailRef.value?.resetInitialGroupData?.()

        activeNameFlag.value = row.groupName
        showDetail.value = true
        readOnly.value = true
        defaultTab.value = 'basicInfo'
        proxy.$store.dispatch('publicVar/updateOperateType', OPERATE_TYPE.UPDATE)
        proxy.$store.dispatch('publicVar/updateGroupData', {
            ...groupData.value,
            ...row
        })
    }
    function handleClearSearchValue () {
        searchValue.value = []
    }
    async function handleAddGroup (groupData = {}) {
        const canLeave = await confirmLeaveDetail()
        if (!canLeave) return
        
        paramGroupDetailRef.value?.resetInitialGroupData?.()
        showDetail.value = true
        showType.value = ADD_VARIABLE
        readOnly.value = false
        defaultTab.value = 'basicInfo'
        detailTitle.value = proxy.$t('publicVar.addParamGroup')
        proxy.$store.dispatch('publicVar/updateGroupData', {
            ...groupData
        })
        proxy.$store.dispatch('publicVar/updateOperateType', OPERATE_TYPE.CREATE)
        paramGroupDetailRef.value?.init?.()
    }
    function handleEditGroup (row) {
        showDetail.value = true
        showType.value = EDIT_VARIABLE
        readOnly.value = false
        activeNameFlag.value = row.groupName
        defaultTab.value = 'basicInfo'
        detailTitle.value = proxy.$t('publicVar.editParamGroup')
        proxy.$store.dispatch('publicVar/updateOperateType', OPERATE_TYPE.UPDATE)
        proxy.$store.dispatch('publicVar/updateGroupData', {
            ...groupData.value,
            groupName: row.groupName,
            desc: row.desc,
            updateTime: row.updateTime,
            modifier: row.modifier
        })
        paramGroupDetailRef.value?.init?.()
    }
    function handlePageChange (page) {
        pagination.value.current = page
        fetchVariableGroupList()
    }
    function handlePageLimitChange (limit) {
        pagination.value.current = 1
        pagination.value.limit = limit
        fetchVariableGroupList()
    }

    function handleRowClick (row) {
        if (!showDetail.value) return
        handleViewDetail(row)
    }
    /**
     * 检查是否应该显示提示
     * 如果用户关闭过提示，且距离关闭时间不足三个月，则不显示
     */
    function checkShouldShowTips () {
        try {
            const closedTime = localStorage.getItem(PUBLIC_VAR_TIPS_CLOSED_TIME)
            if (!closedTime) {
                return true
            }
            const closedTimestamp = parseInt(closedTime, 10)
            const currentTime = Date.now()
            const timeDiff = currentTime - closedTimestamp
            // 如果距离关闭时间超过三个月，则重新显示
            return timeDiff >= THREE_MONTHS_MS
        } catch (e) {
            console.error('读取本地缓存失败:', e)
            return true
        }
    }
    
    /**
     * 关闭提示并记录关闭时间到本地缓存
     */
    function handleClosePublicVarTips () {
        showPublicVarTips.value = false
        localStorage.setItem(PUBLIC_VAR_TIPS_CLOSED_TIME, Date.now().toString())
    }
    function rowStyle () {
        if (showDetail.value) {
            return {
                'cursor': 'pointer'
            }
        }
    }
    function rowClassName ({ row }) {
        if (row.groupName === newNameFlag.value && !showImportDialog.value && !showDetail.value) return 'is-new'
        if (row.groupName === activeNameFlag.value && showDetail.value) return 'is-active'
        return ''
    }
    async function fetchVariableGroupList () {
        try {
            isLoading.value = true
            const res  = await proxy.$store.dispatch('publicVar/fetchVariableGroup', {
                projectId: projectId.value,
                params: {
                    page: pagination.value.current,
                    pageSize: pagination.value.limit,
                    ...searchParams.value
                }
            })
            tableData.value = res.records
            pagination.value.count = res.count
        } catch (e) {
            proxy.$bkMessage({
                theme: 'error',
                message: e.message || e
            })
            console.error(e)
        } finally {
            isLoading.value = false
        }
    }
    async function getVariablesByGroupName (groupName) {
        try {
            const variables = await proxy.$store.dispatch('publicVar/getVariables', {
                groupName,
            })
            return variables.map(i => ({
                ...i,
                buildFormProperty: {
                    ...i.buildFormProperty,
                    published: false
                }
            }))
        } catch (e) {
            console.error(e)
        }
    }
    async function handleCopyGroup (data, item) {
        const publicVars = await getVariablesByGroupName(item.data.groupName)
        const groupData = {
            ...item.data,
            groupName: `${item.data.groupName}_${randomString(4)}`,
            publicVars
        }
        handleAddGroup(groupData)
    }
    async function handleExportGroup (data, item) {
        try {
            await proxy.$store.dispatch('publicVar/exportVariable', {
                projectId: projectId.value,
                groupName: item.data.groupName
            })
        } catch (e) {
            console.error(e)
        }
    }
    async function handleViewRef (row) {
        const canLeave = await confirmLeaveDetail()
        if (!canLeave) return

        paramGroupDetailRef.value?.resetInitialGroupData?.()
        activeNameFlag.value = row.groupName
        showDetail.value = true
        readOnly.value = true
        defaultTab.value = 'referenceList'
        proxy.$store.dispatch('publicVar/updateOperateType', OPERATE_TYPE.UPDATE)
        proxy.$store.dispatch('publicVar/updateGroupData', {
            ...groupData.value,
            groupName: row.groupName,
            desc: row.desc,
            updateTime: row.updateTime,
            modifier: row.modifier
        })
    }
    function handleDeleteGroup (data, item) {
        const groupName = item.data.groupName
        proxy.$bkInfo({
            type: 'warning',
            title: proxy.$t('publicVar.confirmDeleteGroup'),
            subTitle: proxy.$t('publicVar.deleteSubTitle', [groupName]),
            async confirmFn () {
                try {
                    await proxy.$store.dispatch('publicVar/deleteVariableGroup', {
                        groupName
                    })
                    proxy.$bkMessage({
                        theme: 'success',
                        message: proxy.$t('deleteSuc')
                    })
                    await fetchVariableGroupList()
                } catch (e) {
                    proxy.$bkMessage({
                        theme: 'error',
                        message: e.message || e
                    })
                }
            }
        })
    }
    async function handleImportSuccess (yaml, name) {
        try {
            const res = await proxy.$store.dispatch('publicVar/importVarByYaml', {
                projectId: projectId.value,
                type: operateType.value,
                yaml
            })
            showImportDialog.value = false
            searchValue.value = []
            pagination.value.current = 1
            newNameFlag.value = res ?? ''
            await fetchVariableGroupList()
            proxy.$bkMessage({
                theme: 'success',
                message: proxy.$t('publicVar.newVarGroupSuccess')
            })
            setTimeout(() => {
                newNameFlag.value = ''
            }, 5000)
        } catch (e) {
            proxy.$bkMessage({
                theme: 'error',
                message: e.message || e
            })
            console.error(e)
        }
    }
    function checkViewManageAuth () {
        proxy.$store.dispatch('pipelines/checkViewManageAuth', {
            projectId: projectId.value
        })
    }
    async function handleReleaseSuccess (groupName) {
        showDetail.value = false
        await fetchVariableGroupList()
        newNameFlag.value = groupName
        setTimeout(() => {
            newNameFlag.value = ''
        }, 5000)
    }
    onMounted(() => {
        fetchVariableGroupList()
        checkViewManageAuth()
    })
</script>
<style lang="scss">
    .pipeline-public-var-main {
        width: 100%;
        .public-var-tips {
            width: 100%;
        }
        .public-var-list-box {
            padding: 20px;
            height: calc(100% - 90px);
            &.not-tips {
                height: calc(100% - 56px);
            }
        }
        .header-wrapper {
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-bottom: 20px;
            .search-input {
                width: 600px;
                background-color: white;
                ::placeholder {
                    color: #c4c6cc;
                }
            }
        }
        .operation-wrapper {
            display: flex;
            align-items: center;
        }
    }
    .public-list-table {
        &.show-detail {
            width: 370px !important;
        }
        .group-name-link {
            color: #3a84ff;
            cursor: pointer;
        }
        .is-new {
            background-color: #f2fff4 !important;
        }
        .is-active {
            background-color: #e2edff !important;
        }
    }
</style>

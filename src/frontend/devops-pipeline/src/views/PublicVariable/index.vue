<template>
    <section class="pipeline-public-var-main">
        <bk-alert
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
                        :disabled="showDetail"
                        v-perm="{
                            hasPermission: isManage,
                            disablePermissionApi: true,
                            permissionData: {
                                projectId: projectId,
                                resourceType: 'project',
                                resourceCode: projectId,
                                action: PROJECT_RESOURCE_ACTION.MANAGE
                            }
                        }"
                        @click="handleAddGroup"
                    >
                        {{ $t('publicVar.addParamGroup') }}
                    </bk-button>
                    <bk-button
                        @click="handleShowImportDialog"
                        :disabled="showDetail"
                        v-perm="{
                            hasPermission: isManage,
                            disablePermissionApi: true,
                            permissionData: {
                                projectId: projectId,
                                resourceType: 'project',
                                resourceCode: projectId,
                                action: PROJECT_RESOURCE_ACTION.MANAGE
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
                    :readonly="showDetail"
                    :data="searchList"
                    clearable
                    :show-condition="false"
                    :placeholder="filterTips"
                />
            </div>
            <bk-table
                class="public-list-table"
                :data="renderTableData"
                height="100%"
                v-bkloading="{ isLoading }"
                :row-class-name="rowClassName"
                :pagination="pagination"
                @page-change="handlePageChange"
                @page-limit-change="handlePageLimitChange"
            >
                <bk-table-column
                    :label="$t('publicVar.paramGroupId')"
                    width="200"
                    prop="groupName"
                >
                    <template slot-scope="{ row }">
                        <bk-button
                            text
                            @click="handleViewDetail(row)"
                        >
                            {{ row.groupName }}
                        </bk-button>
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
                    />
                    <bk-table-column
                        :label="$t('publicVar.lastModifiedBy')"
                        prop="modifier"
                    />
                    <bk-table-column
                        :label="$t('publicVar.lastModifiedDate')"
                        prop="updateTime"
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
                                        hasPermission: isManage,
                                        disablePermissionApi: true,
                                        permissionData: {
                                            projectId: projectId,
                                            resourceType: 'project',
                                            resourceCode: projectId,
                                            action: PROJECT_RESOURCE_ACTION.MANAGE
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
                                hasPermission: isManage,
                                disablePermissionApi: true,
                                permissionData: {
                                    projectId: projectId,
                                    resourceType: 'project',
                                    resourceCode: projectId,
                                    action: PROJECT_RESOURCE_ACTION.MANAGE
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
                :is-show.sync="showDetail"
                :title="detailTitle"
                :show-tips="showPublicVarTips"
                :show-type="showType"
                :read-only="readOnly"
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
    import { convertTime } from '@/utils/util'
    import {
        ADD_VARIABLE,
        EDIT_VARIABLE,
        OPERATE_TYPE
    } from '@/store/modules/publicVar/constants'
    import {
        PROJECT_RESOURCE_ACTION,
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
    const showPublicVarTips = ref(true)
    const readOnly = ref(false)
    const tableData = ref([])
    const newNameFlag = ref('')
    const activeNameFlag = ref('')
    const pagination = ref({
        current: 1,
        count: 0,
        limit: 20
    })
    const operateType = computed(() => proxy.$store.state.publicVar.operateType)
    const groupData = computed(() => proxy.$store.state.publicVar.groupData)
    const isManage = computed(() => proxy.$store?.state?.pipelines?.isManage)
    const projectId = computed(() => proxy.$route.params?.projectId)
    const searchList = computed(() => {
        return [
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
            },
            {
                name: proxy.$t('publicVar.varId'),
                id: 'filterByVarName'
            },
            {
                name: proxy.$t('publicVar.varAlias'),
                id: 'filterByVarAlias'
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
                        hasPermission: isManage.value,
                        disablePermissionApi: true,
                        permissionData: {
                            projectId: projectId.value,
                            resourceType: 'project',
                            resourceCode: projectId.value,
                            action: PROJECT_RESOURCE_ACTION.MANAGE
                        }
                    },
                    {
                        text: proxy.$t('publicVar.export'),
                        handler: handleExportGroup,
                        data: row,
                        hasPermission: isManage.value,
                        disablePermissionApi: true,
                        permissionData: {
                            projectId: projectId.value,
                            resourceType: 'project',
                            resourceCode: projectId.value,
                            action: PROJECT_RESOURCE_ACTION.MANAGE
                        }
                    },
                    {
                        text: proxy.$t('publicVar.offline'),
                        handler: handleOfflineGroup,
                        data: row,
                        hasPermission: isManage.value,
                        disablePermissionApi: true,
                        permissionData: {
                            projectId: projectId.value,
                            resourceType: 'project',
                            resourceCode: projectId.value,
                            action: PROJECT_RESOURCE_ACTION.MANAGE
                        }
                    },
                    {
                        text: proxy.$t('delete'),
                        handler: handleDeleteGroup,
                        data: row,
                        hasPermission: isManage.value,
                        disablePermissionApi: true,
                        disable: row.referCount > 0,
                        permissionData: {
                            projectId: projectId.value,
                            resourceType: 'project',
                            resourceCode: projectId.value,
                            action: PROJECT_RESOURCE_ACTION.MANAGE
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
        if (showDetail.value) return
        showImportDialog.value = true
        proxy.$store.dispatch('publicVar/updateOperateType', OPERATE_TYPE.CREATE)
    }
    function handleViewDetail (row) {
        if (showDetail.value) return
        activeNameFlag.value = row.groupName
        showDetail.value = true
        readOnly.value = true
        proxy.$store.dispatch('publicVar/updateOperateType', OPERATE_TYPE.UPDATE)
        proxy.$store.dispatch('publicVar/updateGroupData', {
            ...groupData.value,
            groupName: row.groupName,
            desc: row.desc,
            updateTime: row.updateTime,
            modifier: row.modifier
        })
    }
    function handleClearSearchValue () {
        searchValue.value = []
    }
    function handleAddGroup (groupData = {}) {
        if (showDetail.value) return
        showDetail.value = true
        showType.value = ADD_VARIABLE
        readOnly.value = false
        detailTitle.value = proxy.$t('publicVar.addParamGroup')
        proxy.$store.dispatch('publicVar/updateGroupData', groupData)
        proxy.$store.dispatch('publicVar/updateOperateType', OPERATE_TYPE.CREATE)
    }
    function handleEditGroup (row) {
        showDetail.value = true
        showType.value = EDIT_VARIABLE
        readOnly.value = false
        activeNameFlag.value = row.groupName
        detailTitle.value = proxy.$t('publicVar.editParamGroup')
        proxy.$store.dispatch('publicVar/updateOperateType', OPERATE_TYPE.UPDATE)
        proxy.$store.dispatch('publicVar/updateGroupData', {
            ...groupData.value,
            groupName: row.groupName,
            desc: row.desc,
            updateTime: row.updateTime,
            modifier: row.modifier
        })
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
    function handleClosePublicVarTips () {
        showPublicVarTips.value = false
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
        const groupData = {
            ...item.data,
            groupName: `${item.data.groupName}_copy`,
            publicVars: await getVariablesByGroupName(item.data.groupName)
        }
        await handleAddGroup(groupData)
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
    function handleOfflineGroup () {
        
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
        // &.show-detail {
            // width: 370px !important;
            // z-index: 5000;
            // table tbody tr {
            //     cursor: pointer !important;
            // }
        // }
        .is-new {
            background-color: #f2fff4 !important;
        }
        .is-active {
            background-color: #e2edff !important;
        }
    }
</style>

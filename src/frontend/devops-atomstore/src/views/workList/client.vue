<template>
    <main>
        <div class="content-header">
            <div class="atom-total-row">
                <bk-button
                    theme="primary"
                    @click="showAddPage"
                >
                    {{ $t('store.注册应用') }}
                </bk-button>
            </div>
            <bk-input
                :placeholder="$t('store.请输入关键字搜索')"
                class="search-input"
                :clearable="true"
                :right-icon="'bk-icon icon-search'"
                v-model="searchKey"
                @clear="handleSearch"
                @right-icon-click="handleSearch"
                @enter="handleSearch"
            >
            </bk-input>
        </div>
        <main
            class="g-scroll-pagination-table"
            v-bkloading="{ isLoading }"
        >
            <bk-table
                style="margin-top: 15px;"
                :header-cell-style="{ background: '#fff' }"
                :data="applications"
                :pagination="pagination"
                @page-change="handleCurrentChange"
                @page-limit-change="handleLimitChange"
            >
                <bk-table-column
                    :label="$t('store.应用名称')"
                    prop="name"
                    show-overflow-tooltip
                >
                    <template slot-scope="props">
                        <span
                            class="atom-name"
                            :title="props.row.name"
                            @click="handelToAppDetail(props.row)"
                        >{{ props.row.name }}</span>
                    </template>
                </bk-table-column>
                <bk-table-column
                    :label="$t('store.版本')"
                    prop="version"
                >
                    <template slot-scope="props">
                        <div class="version-container">
                            <div
                                v-for="(versionItem, index) in getVersionList(props.row)"
                                :key="index"
                                class="version-item"
                                @click="handleVersionClick(versionItem)"
                            >
                                <Status :status="calcStatus(versionItem.status)"></Status>
                                <span :class="{ 'g-text-link': isProgressStatus(versionItem.status), 'g-text-padding': true }">
                                    {{ versionItem.version }}
                                </span>
                            </div>
                        </div>
                    </template>
                </bk-table-column>
                <bk-table-column
                    :label="$t('store.修改人')"
                    prop="modifier"
                ></bk-table-column>
                <bk-table-column
                    :label="$t('store.修改时间')"
                    prop="updateTime"
                    width="160"
                ></bk-table-column>
                <bk-table-column
                    :label="$t('store.操作')"
                    width="250"
                    class-name="handler-btn"
                >
                    <template slot-scope="props">
                        <bk-button
                            v-if="isProgressStatus(props.row.status)"
                            text
                            theme="primary"
                            class="mr10"
                            @click="goProgressDetail(props.row)"
                        >
                            {{ $t('store.进度') }}
                        </bk-button>
                        <bk-button
                            v-if="showUpgradeButton(props.row)"
                            text
                            theme="primary"
                            class="mr10"
                            @click="handleUpdateApp(props.row)"
                        >
                            {{ $t('store.升级') }}
                        </bk-button>
                        <bk-button
                            v-if="showTakedownButton(props.row)"
                            text
                            theme="primary"
                            class="mr10"
                            @click="takeDownVersion(props.row)"
                        >
                            {{ $t('store.下架') }}
                        </bk-button>
                        <bk-button
                            v-if="showPutupButton(props.row)"
                            text
                            theme="primary"
                            class="mr10"
                            @click="handleUpdateApp(props.row)"
                        >
                            {{ $t('store.上架') }}
                        </bk-button>
                        <bk-button
                            v-if="props.row.status === 'RELEASED'"
                            text
                            theme="primary"
                            class="mr10"
                            @click="handleDownload(props.row)"
                        >
                            {{ $t('store.下载') }}
                        </bk-button>
                        <bk-button
                            v-if="!props.row.releaseFlag"
                            text
                            theme="primary"
                            @click="handleDelete(props.row)"
                        >
                            {{ $t('store.删除') }}
                        </bk-button>
                    </template>
                </bk-table-column>
                <template #empty>
                    <EmptyTableStatus
                        :type="searchKey ? 'search-empty' : 'empty'"
                        @clear="handleKeywordChange"
                    />
                </template>
            </bk-table>
        </main>
        
        <OperationApp
            :is-show="isShowAdd"
            @confirm="hideAddPage"
            @cancel="hideAddPage"
        />

        <TakeDownSlider
            :is-show="isShowTakedown"
            :infos="infos"
            :show-version="false"
            :store-code="infos ? infos.storeCode : ''"
            @takedownSuccess="handleTakedownSuccess"
        />
    </main>
</template>

<script setup>
    import { ref, onMounted } from 'vue'
    import OperationApp from './components/operationApp'
    import TakeDownSlider from './components/takeDownSlider'
    import UseInstance from '@/hook/useInstance.js'
    import Status from './status'
    import { UpgradeStatus, PROGRESS_STATUS } from '@/utils/constants'

    const { proxy } = UseInstance()
    const { $store, $router, $bkMessage, $bkInfo, $t } = proxy

    const successIcon = 'check-circle-fill-2 text-[#2DCB56]'
    const failIcon = 'close-circle-fill text-[#EA3636]'
    const infoIcon = 'info-circle-shape text-[#C4C6CC]'
    const initIcon = 'zhuangtai text-[#3A84FF]'

    // 响应式数据
    const isLoading = ref(false)
    const showPutupStatus = [UpgradeStatus.INIT, UpgradeStatus.UNDERCARRIAGED]
    const isShowAdd = ref(false)
    const isShowTakedown = ref(false)
    const infos = ref(null)
    const deleteNameConfirm = ref('')
    const searchKey = ref('')
    const applications = ref([])

    const pagination = ref({
        current: 1,
        count: 0,
        limit: 10
    })

    onMounted(() => {
        fetchApplications()
        getOauth()
    })
    
    // 检查OAuth
    async function getOauth () {
        try {
            await $store.dispatch('store/checkIsOAuth')
        } catch (error) {
            console.error(error)
        }
    }
    
    function calcStatus (status) {
        let icon = ''
        switch (status) {
            case 'COMMITTING':
            case 'BUILDING':
            case 'TESTING':
            case 'AUDITING':
            case 'EDITING':
            case 'UNDERCARRIAGING':
            case 'CODECCING':
                icon = 'doing'
                break
            case 'RELEASED':
                icon = 'success'
                break
            case 'GROUNDING_SUSPENSION':
            case 'CODECC_FAIL':
            case 'BUILD_FAIL':
                icon = 'fail'
                break
            case 'AUDIT_REJECT':
            case 'UNDERCARRIAGED':
                icon = 'info'
                break
            case 'INIT':
                icon = 'init'
                break
        }
        return icon
    }

    // 获取应用列表
    async function fetchApplications () {
        isLoading.value = true
        try {
            const res = await $store.dispatch('store/getComponentsList', {
                name: searchKey.value,
                page: pagination.value.current,
                pageSize: pagination.value.limit,
            })
            applications.value = res.records || []
            pagination.value.count = res.count || 0
        } catch (error) {
            $bkMessage({
                theme: 'error',
                message: error.message || error
            })
            applications.value = []
            pagination.value.count = 0
        } finally {
            isLoading.value = false
        }
    }
    
    // 判断是否为进度状态
    function isProgressStatus (status) {
        return PROGRESS_STATUS.includes(status)
    }

    // 点击版本
    function handleVersionClick (versionItem) {
        if (isProgressStatus(versionItem.status)) {
            goProgressDetail(versionItem)
        }
    }

    // 显示注册页面
    function showAddPage () {
        isShowAdd.value = true
    }

    // 隐藏注册页面
    function hideAddPage (refresh = false) {
        if (refresh) fetchApplications()
        isShowAdd.value = false
    }

    // 获取版本列表（包含主版本和处理中的版本）
    function getVersionList (row) {
        return [row, ...(row.processingVersionInfos || [])]
    }

    // 显示升级按钮
    function showUpgradeButton (row) {
        return [
            UpgradeStatus.GROUNDING_SUSPENSION,
            UpgradeStatus.AUDIT_REJECT,
            UpgradeStatus.RELEASED
        ].includes(row.status) && (!row.processingVersionInfos || row.processingVersionInfos?.length <= 0)
    }

    // 显示下架按钮
    function showTakedownButton (row) {
        return [
            UpgradeStatus.GROUNDING_SUSPENSION,
            UpgradeStatus.AUDIT_REJECT,
            UpgradeStatus.RELEASED
        ].includes(row.status) && row.releaseFlag
    }

    // 显示上架按钮
    function showPutupButton (row) {
        return showPutupStatus.includes(row.status)
    }

    // 跳转到进度详情
    function goProgressDetail (data) {
        $router.push({
            name: 'progressDetail',
            params: {
                storeCode: data.storeCode,
                storeId: data.storeId
            }
        })
    }

    // 升级/上架应用
    function handleUpdateApp (data) {
        $router.push({
            name: 'addReleaseVersion',
            params: {
                storeCode: data.storeCode,
                storeId: data.storeId
            }
        })
    }

    // 下架版本
    function takeDownVersion (data) {
        isShowTakedown.value = true
        infos.value = data
    }

    // 下载应用
    async function handleDownload (data) {
        try {
            const res = await $store.dispatch('store/downloadApp', {
                version: data.version,
                storeCode: data.storeCode
            })
            if (res) {
                location.href = res
            }
        } catch (error) {
            $bkMessage({
                theme: 'error',
                message: error.message || error
            })
        }
    }

    // 跳转到应用详情
    function handelToAppDetail (row) {
        $router.push({
            name: 'statisticData',
            params: {
                type: 'devx',
                code: row.storeCode
            }
        })
    }

    // 删除应用
    function handleDelete (row) {
        const h = proxy.$createElement
        $bkInfo({
            type: 'warning',
            title: $t('store.确定删除应用', [row.name]),
            subHeader: h('div', [
                h('p', {
                    style: {
                        fontSize: '14px',
                        color: '#ea3636',
                        marginBottom: '20px'
                    }
                }, $t('store.删除时将清理数据，删除后不可恢复！避免误操作，请输入标识【{0}】后再提交删除', [row.name])),
                h('input', {
                    class: 'bk-form-input',
                    on: {
                        input: (e) => {
                            deleteNameConfirm.value = e.target.value
                        }
                    }
                })
            ]),
            confirmFn: async () => {
                if (deleteNameConfirm.value !== row.name) {
                    $bkMessage({
                        theme: 'error',
                        message: deleteNameConfirm.value
                            ? $t('store.输入的内容【{0}】与应用标识【{1}】不匹配', [deleteNameConfirm.value, row.name])
                            : $t('store.请按提示输入应用标识')
                    })
                    return false
                }
                await deleteApp(row)
            },
            cancelFn: () => {
                deleteNameConfirm.value = ''
            }
        })
    }

    // 删除应用API调用
    async function deleteApp (row) {
        try {
            const res = await $store.dispatch('store/deleteApp', row.storeCode)
            if (res) {
                $bkMessage({
                    theme: 'success',
                    message: $t('store.X删除成功', [row.name])
                })
                deleteNameConfirm.value = ''
                fetchApplications()
            }
        } catch (error) {
            if (error.status === 2120937) {
                $bkInfo({
                    type: 'warning',
                    subHeader: proxy.$createElement('p', {
                        style: {
                            fontSize: '14px',
                            marginBottom: '20px'
                        }
                    }, error.message)
                })
            } else {
                $bkMessage({
                    theme: 'error',
                    message: error.message
                })
            }
            deleteNameConfirm.value = ''
        }
    }

    // 下架成功回调
    function handleTakedownSuccess () {
        isShowTakedown.value = false
        infos.value = null
        fetchApplications()
    }

    // 搜索处理
    function handleSearch () {
        pagination.value.current = 1
        fetchApplications()
    }

    // 清除搜索关键字
    function handleKeywordChange () {
        searchKey.value = ''
        handleSearch()
    }

    // 页码改变
    function handleCurrentChange (page) {
        pagination.value.current = page
        fetchApplications()
    }

    // 每页数量改变
    function handleLimitChange (limit) {
        pagination.value.limit = limit
        pagination.value.current = 1
        fetchApplications()
    }
</script>

<style lang="scss" scoped>
.version-container {
  display: flex;
  align-items: center;
}

.version-item {
  display: flex;
  align-items: center;
  margin-right: 10px;
}

.version-loading {
  padding: 0 5px;
}

.g-text-link {
  color: #3a84ff;
  cursor: pointer;
}

.g-text-padding {
  padding-right: 5px;
}

.mr10 {
  margin-right: 10px;
}

.atom-name {
  color: #3a84ff;
  cursor: pointer;
}
</style>

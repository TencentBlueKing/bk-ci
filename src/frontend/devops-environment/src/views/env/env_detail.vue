<template>
    <div
        class="env-entry-main"
        v-bkloading="{ isLoading: !envDetailLoaded }"
    >
        <template v-if="envList.length">
            <header class="env-info-header">
                <span
                    v-bk-overflow-tips
                    class="env-name"
                >
                    {{ currentEnv?.name || '--' }}
                </span>
                <bk-tag>{{ envNodeTypeDisplayName }}</bk-tag>
                <span class="env-type-tag">
                    {{ envTypeDisplayName }}
                </span>
            </header>
            <div
                class="env-content-main"
            >
                <template v-if="envDetailLoaded">
                    <bk-tab
                        :active.sync="tabActive"
                        type="unborder-card"
                        ext-cls="env-details-tab"
                    >
                        <bk-tab-panel
                            v-for="(panel, index) in panels"
                            v-bind="panel"
                            :key="index"
                        />
                    </bk-tab>
                    <component
                        :is="renderComponent"
                        :key="currentEnv?.envType"
                    />
                </template>
            </div>
        </template>

        <template v-else>
            <empty-node
                is-env
                :to-create-node="handleCreateEnv"
                :empty-info="emptyInfo"
            />
        </template>
    </div>
</template>

<script>
    import { ref, watch, computed, onMounted, onBeforeUnmount } from 'vue'
    import {
        ENV_TYPE_MAP
    } from '@/store/constants'
    import useInstance from '@/hooks/useInstance'
    import useEnvDetail from '@/hooks/useEnvDetail'
    import useCreateEnv from '@/hooks/useCreateEnv'
    import useEnvAside from '@/hooks/useEnvAside'
    import Node from './components/Node/index.vue'
    import EnvParam from './components/EnvParam/index.vue'
    import BasicInfo from './components/BasicInfo/index.vue'
    import SharedSettings from './components/SharedSetting/index.vue'
    import BuildTask from './components/BuildTask/index.vue'
    import DeployTask from './components/DeployTask/index.vue'
    import AuthManage from './components/Auth/index.vue'
    import emptyNode from '../empty_node'

    export default {
        name: 'EnvDetail',
        components: {
            Node,
            EnvParam,
            BasicInfo,
            SharedSettings,
            BuildTask,
            DeployTask,
            emptyNode
        },
        setup () {
            const { proxy } = useInstance()
            const {
                currentEnv,
                fetchEnvDetail,
                envDetailLoaded,
                setEnvDetailLoaded,
                projectId
            } = useEnvDetail()
            const {
                envList
            } = useEnvAside()
            

            const emptyInfo = ref({
                title: proxy.$t('environment.envInfo.emptyEnv'),
                desc: proxy.$t('environment.envInfo.emptyEnvTips'),
                btnText: proxy.$t('environment.createEnvrionment')
            })
            
            // 从路由参数中获取初始 tab，如果没有则默认为 'node'
            const initialTab = proxy.$route.params.tabName || 'node'
            const tabActive = ref(initialTab)
            const renderComponent = computed(() => {
                const comMap = {
                    node: Node,
                    variable: EnvParam,
                    basicInfo: BasicInfo,
                    sharedSettings: SharedSettings,
                    buildTask: BuildTask,
                    deployTask: DeployTask,
                    auth: AuthManage
                }
                return comMap[tabActive.value]
            })

            const envNodeTypeDisplayName = computed(() => {
                const envNodeTypeMap = {
                    'TAG': proxy.$t('environment.dynamic'),
                    'NODE': proxy.$t('environment.static')
                }
                return envNodeTypeMap[currentEnv.value?.envNodeType]
            })
            const envTypeDisplayName = computed(() => {
                const envTypeMap = {
                    'DEV': proxy.$t('environment.envInfo.DEVEnvType'),
                    'PROD': proxy.$t('environment.envInfo.PRODEnvType'),
                    'BUILD': proxy.$t('environment.envInfo.BUILDEnvType'),
                    'DEVX': proxy.$t('environment.envInfo.DEVXEnvType')
                }
                return envTypeMap[currentEnv.value?.envType]
            })
            const panels = computed(() => [
                {
                    name: 'node',
                    label: proxy.$t('environment.node')
                },
                ...(currentEnv.value?.envType === ENV_TYPE_MAP.BUILD ? [
                    {
                        name: 'variable',
                        label: proxy.$t('environment.environmentVariable')
                    },
                    {
                        name: 'sharedSettings',
                        label: proxy.$t('environment.sharedSettings')
                    }
                ] : []),
                {
                    name: 'basicInfo',
                    label: proxy.$t('environment.basicInfo')
                },
                ...(currentEnv.value?.envType === ENV_TYPE_MAP.BUILD ? [
                    {
                        name: 'buildTask',
                        label: proxy.$t('environment.nodeInfo.buildTask')
                    }
                ] : []),
                {
                    name: 'auth',
                    label: proxy.$t('environment.authManage')
                }
            ])
            
            // 获取可用的 tab 名称列表
            const availableTabs = computed(() => panels.value.map(p => p.name))

            // 监听 tabActive 变化，更新路由参数
            watch(() => tabActive.value, (newTab) => {
                const currentRoute = proxy.$route
                // 只有在有 envId 且 tabName 不同时才进行导航
                if (currentRoute.params.envId && currentRoute.params.tabName !== newTab) {
                    proxy.$router.replace({
                        name: 'envDetail',
                        params: {
                            ...currentRoute.params,
                            tabName: newTab
                        }
                    }).catch(err => {
                        throw err
                    })
                }
            }, {
                immediate: true
            })

            // 监听路由参数变化，更新 tabActive
            watch(() => proxy.$route.params.tabName, (newTabName) => {
                if (newTabName && newTabName !== tabActive.value) {
                    tabActive.value = newTabName
                }
            })
            
            // 监听 currentEnv 和 availableTabs 变化，检查当前 tab 是否可用
            watch([() => currentEnv.value?.envType, availableTabs], ([envType, tabs]) => {
                const currentTabName = tabActive.value
                // 当 currentEnv 加载完成后，检查当前 tab 是否在可用列表中
                if (envType && currentTabName && !tabs.includes(currentTabName)) {
                    tabActive.value = 'node'
                    proxy.$router.replace({
                        name: 'envDetail',
                        params: {
                            ...proxy.$route.params,
                            tabName: 'node'
                        }
                    }).catch(err => {
                        throw err
                    })
                }
            }, {
                immediate: true
            })

            // 监听 envId 变化，当 envId 存在但没有 tabName 时，添加默认的 tabName
            watch(() => proxy.$route.params.envId, async (newEnvId) => {
                if (newEnvId) {
                    await fetchEnvDetail()
                }
                if (newEnvId && !proxy.$route.params.tabName) {
                    proxy.$router.replace({
                        name: 'envDetail',
                        params: {
                            ...proxy.$route.params,
                            tabName: tabActive.value
                        }
                    }).catch(err => {
                        throw err
                    })
                }
            })

            watch(() => projectId.value, async (newProjectId) => {
                if (newProjectId) {
                    setEnvDetailLoaded(false)
                }
            })

            const {
                showCreateEnvDialog
            } = useCreateEnv()
            const curEnvType = computed(() => proxy.$route.params.envType)
            const handleCreateEnv = () => {
                console.log(curEnvType.value, 1)
                showCreateEnvDialog(curEnvType.value)
            }
            onMounted(async () => {
                await fetchEnvDetail()
            })
            onBeforeUnmount(() => {
                setEnvDetailLoaded(false)
            })
            return {
                renderComponent,
                currentEnv,
                envList,
                emptyInfo,
                tabActive,
                panels,
                envDetailLoaded,
                envTypeDisplayName,
                envNodeTypeDisplayName,
                handleCreateEnv
            }
        }
    }
</script>

<style lang="scss" scoped>
.env-entry-main {
    display: flex;
    flex-direction: column;
    padding: 24px;
    height: 100%;
    background: #f5f7fa;
}
.env-info-header {
    display: flex;
    align-items: center;
    height: 54px;
    line-height: 54px;
    background: #FAFBFD;
    padding: 0 24px;
    .env-name {
        font-weight: 700;
        font-size: 14px;
        max-width: 300px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        color: #63656E;
        margin-right: 16px;
    }
    .env-type-tag {
        height: 22px;
        padding: 0 6px;
        line-height: 22px;
        font-size: 12px;
        text-align: center;
        color: #63656E;
        margin-left: 8px;
        border: 1px solid #979ba54d;
        border-radius: 2px;
    }
}
.env-content-main {
    flex: 1;
    padding: 0 24px;
    background-color: #fff;
    overflow: hidden;
    box-shadow: 0 2px 2px 0 #00000026;
    display: flex;
    flex-direction: column;
}
</style>
<style lang="scss">
.env-details-tab {
    .bk-tab-header {
        margin-bottom: 18px;
    }
    .bk-tab-section {
        display: none !important;
    }
}
</style>

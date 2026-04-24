<template>
    <section
        class="env-group-aside"
        v-bkloading="{ isLoading }"
    >
        <bk-button
            class="add-env-btn"
            icon="plus"
            v-perm="{
                permissionData: {
                    projectId: projectId,
                    resourceType: ENV_RESOURCE_TYPE,
                    resourceCode: projectId,
                    action: ENV_RESOURCE_ACTION.CREATE
                }
            }"
            :key="projectId"
            @click="() => showCreateEnvDialog()"
        >
            {{ $t('environment.createEnvrionment') }}
        </bk-button>
        <bk-input
            v-model="envName"
            class="search-env-input mt10"
            :placeholder="$t('environment.searchEnvPlaceholder')"
            right-icon="bk-icon icon-search"
            clearable
            @enter="fetchEnvList"
            @clear="fetchEnvList"
        />
        <div class="filer-type-menu">
            <p class="title">{{ $t('environment.typeFilter') }}</p>
            <div class="type-list">
                <span
                    v-for="type in envTypList"
                    :key="type.id"
                    :class="{
                        'type-item': true,
                        'active': envType === type.id
                    }"
                    @click="handleChangeEnvType(type.id)"
                >
                    {{ type.name }}
                    <span
                        class="node-count"
                    >
                        ({{ type.count ?? 0 }})
                    </span>
                </span>
            </div>
        </div>
        <div class="env-list-menu">
            <p class="title">{{ $t('environment.environmentList') }}</p>
            <div class="env-list">
                <span
                    v-for="env in envList"
                    :key="env.id"
                    :class="{
                        'env-item': true,
                        'active': envId === env.envHashId
                    }"
                >
                    <span
                        class="env-content"
                        @click="handleChangeEnv(env.envHashId)"
                    >
                        <span
                            class="env-name"
                            v-bk-overflow-tips
                        >
                            {{ env.name }}
                        </span>
                        <span
                            :class="{
                                'count-tag': true,
                                'active': envId === env.envHashId
                            }"
                        >
                            {{ env.nodeCount ?? 0 }}
                        </span>
                    </span>
                    <bk-dropdown-menu
                        trigger="click"
                        ext-cls="env-operation-dropdown"
                        @show="handleDropdownShow(env.envHashId)"
                        @hide="handleDropdownHide"
                    >
                        <i
                            slot="dropdown-trigger"
                            class="bk-icon icon-more env-operation-btn"
                        ></i>
                        <ul
                            class="bk-dropdown-list"
                            slot="dropdown-content"
                        >
                            <li>
                                <span
                                    @click="handleDeleteEnv(env)"
                                >
                                    {{ $t('environment.delete') }}
                                </span>
                            </li>
                        </ul>
                    </bk-dropdown-menu>
                </span>
            </div>
        </div>
        <CreateEnvDialog
            @success="handleCreateEnvSuccess"
        />
    </section>
</template>

<script>
    import { watch, computed, onMounted } from 'vue'
    import {
        ENV_TYPE_MAP
    } from '@/store/constants'
    import {
        ENV_RESOURCE_ACTION,
        ENV_RESOURCE_TYPE
    } from '@/utils/permission'
    import UseInstance from '@/hooks/useInstance'
    import CreateEnvDialog from '@/components/CreateEnvDialog.vue'
    import useCreateEnv from '@/hooks/useCreateEnv'
    import useEnvAside from '@/hooks/useEnvAside'

    export default {
        name: 'GroupAside',
        components: {
            CreateEnvDialog
        },
        setup () {
            const { proxy } = UseInstance()
            const {
                showCreateEnvDialog
            } = useCreateEnv()
            const {
                isLoading,
                envName,
                envType,
                envList,
                envCountData,
                totalEnvCount,
                initData,
                fetchEnvList,
                deleteEnv
            } = useEnvAside()
            const projectId = computed(() => proxy.$route.params.projectId)
            const envId = computed(() => proxy.$route.params?.envId)
            const envTypList = computed(() => ([
                {
                    id: ENV_TYPE_MAP.ALL,
                    name: proxy.$t('environment.allEnv'),
                    count: totalEnvCount.value
                },
                {
                    id: ENV_TYPE_MAP.BUILD,
                    name: proxy.$t('environment.envInfo.BUILDEnvType'),
                    count: envCountData.value[ENV_TYPE_MAP.BUILD] ?? 0
                },
                // {
                //     id: ENV_TYPE_MAP.PROD,
                //     name: proxy.$t('environment.envInfo.PRODEnvType'),
                //     count: envCountData.value[ENV_TYPE_MAP.PROD] ?? 0
                // },
                // {
                //     id: ENV_TYPE_MAP.DEV,
                //     name: proxy.$t('environment.envInfo.DEVEnvType'),
                //     count: envCountData.value[ENV_TYPE_MAP.DEV] ?? 0
                // },
                // {
                //     id: ENV_TYPE_MAP.DEVX,
                //     name: proxy.$t('environment.envInfo.DEVXEnvType'),
                //     count: envCountData.value[ENV_TYPE_MAP.DEVX] ?? 0
                // }
            ]))
            const handleCreateEnvSuccess = (envId) => {
                fetchEnvList()
                proxy.$router.replace({
                    params: {
                        ...proxy.$route.params,
                        envId
                    }
                })
            }
            const handleChangeEnvType = async (type) => {
                await proxy.$router.replace({
                    name: 'envDetail',
                    params: {
                        ...proxy.$route.params,
                        envType: type
                    }
                })
                await fetchEnvList()
            }
            const handleChangeEnv = (envHashId) => {
                proxy.$router.replace({
                    name: 'envDetail',
                    params: {
                        ...proxy.$route.params,
                        envId: envHashId
                    }
                })
            }

            // 处理删除环境
            const handleDeleteEnv = async (env) => {
                proxy.$bkInfo({
                    title: proxy.$t('environment.confirmDeleteEnv'),
                    subTitle: `${proxy.$t('environment.envName')}: ${env.name}`,
                    confirmLoading: true,
                    okText: proxy.$t('environment.delete'),
                    theme: 'danger',
                    confirmFn: async () => {
                        try {
                            await deleteEnv(env.envHashId)
                            
                            proxy.$bkMessage({
                                message: proxy.$t('environment.successfullyDeleted'),
                                theme: 'success'
                            })
                            
                            // 如果删除的是当前选中的环境，需要重定向到第一个环境
                            if (envId.value === env.envHashId) {
                                const firstEnv = envList.value.find(e => e.envHashId !== env.envHashId)
                                if (firstEnv) {
                                    proxy.$router.replace({
                                        name: 'envDetail',
                                        params: {
                                            ...proxy.$route.params,
                                            envId: firstEnv.envHashId
                                        }
                                    })
                                } else {
                                    // 如果没有其他环境了，跳转到环境类型页面
                                    proxy.$router.replace({
                                        name: 'envDetail',
                                        params: {
                                            ...proxy.$route.params,
                                            envId: undefined
                                        }
                                    })
                                }
                            }
                        } catch (error) {
                            proxy.$bkMessage({
                                message: error.message || error,
                                theme: 'error'
                            })
                        }
                    }
                })
            }

            watch(() => projectId.value, async (newProjectId) => {
                if (newProjectId) {
                    await initData()
                }
            })

            onMounted(() => {
                initData()
            })
        
            return {
                envId,
                envType,
                envName,
                envList,
                envTypList,
                projectId,
                isLoading,
                initData,
                fetchEnvList,
                showCreateEnvDialog,
                ENV_RESOURCE_ACTION,
                ENV_RESOURCE_TYPE,

                handleChangeEnv,
                handleChangeEnvType,
                handleCreateEnvSuccess,
                handleDeleteEnv
            }
        }
    }
</script>

<style lang="scss">
    .env-group-aside {
        display: flex;
        flex-direction: column;
        padding: 16px;
        height: 100%;
        background-color: #FFFFFF;
        .add-env-btn {
            width: 100%;
            &:hover {
                color: #3A84FF;
                border-color: #3A84FF;
            }
        }
    }
    .filer-type-menu {
        margin-top: 16px;
        font-size: 12px;
        &::after {
            content: '';
            display: block;
            width: calc(100% + 32px);
            height: 1px;
            background: #DCDEE5;
            margin: 16px 0 12px -16px;
        }
        .title {
            color: #979BA5;
            margin-bottom: 8px;
        }
        .type-list {
            display: flex;
            flex-direction: column;
            gap: 16px;
        }
        .type-item {
            padding: 4px 16px;
            cursor: pointer;
            &.active {
                color: #3a84ff;
            }
        }
        .node-count {
            margin-left: 4px;
            color: #979BA5 !important;
        }
    }
    .env-list-menu {
        font-size: 12px;
        .title {
            color: #979BA5;
            margin-bottom: 8px;
        }
        .env-list {
            display: flex;
            flex-direction: column;
            min-height: 300px;
            padding-bottom: 40px;
            gap: 4px;
            margin: 0 -16px;
            background: #FFFFFF;
            overflow-y: auto;
            max-height: calc(100vh - 440px);
            &::-webkit-scrollbar {
                width: 4px;
            }
            &::-webkit-scrollbar-thumb {
                background-color: #dcdee5;
                border-radius: 2px;
                &:hover {
                    background-color: #979ba5;
                }
            }
            &::-webkit-scrollbar-track {
                background-color: transparent;
            }
        }
        .env-item {
            display: flex;
            justify-content: space-between;
            align-items: center;
            height: 32px;
            line-height: 32px;
            padding: 8px 16px;
            cursor: pointer;
            border-radius: 2px;
            transition: all 0.3s;
            position: relative;
            
            &:hover {
                background: #f5f7fa;
                .env-operation-btn {
                    opacity: 1;
                }
            }
            
            &.active {
                background: #E1ECFF;
                color: #3a84ff;
                &::after {
                    content: '';
                    display: block;
                    position: absolute;
                    right: 0;
                    width: 2px;
                    height: 32px;
                    background: #3A84FF;
                }
            }
        }
        
        .env-content {
            display: flex;
            justify-content: space-between;
            align-items: center;
            flex: 1;
            margin-right: 8px;
        }
        
        .env-operation-btn {
            opacity: 0;
            transition: opacity 0.3s;
            font-size: 16px;
            cursor: pointer;
            padding: 4px;
            border-radius: 50%;
            color: #979BA5;
            background: #EAEBF0;
            &:hover {
                background: #E1ECFF;
                color: #3A84FF;
            }
        }
        }
        .env-name {
            flex: 1;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            margin-right: 12px;
        }
        .count-tag {
            display: inline-block;
            width: 30px;
            height: 16px;
            line-height: 16px;
            background: #F0F1F5;
            border-radius: 8px;
            font-size: 12px;
            text-align: center;
            color: #979BA5;
            background: #EAEBF0;
            &.active {
                color: #3A84FF;
                background: #FFFFFF;
            }
        }
    
    // 环境操作下拉菜单样式
    .env-operation-dropdown {
        .bk-dropdown-list {
            li {
                span {
                    display: inline-block;
                    min-width: 68px;
                    padding: 0 16px;
                    color: #63656E;
                    &:hover {
                        background: #F5F7FA;
                    }
                }
            }
        }
    }
</style>
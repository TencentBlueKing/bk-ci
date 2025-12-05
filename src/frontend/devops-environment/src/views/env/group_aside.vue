<template>
    <section class="env-group-aside">
        <bk-button
            class="add-env-btn"
            icon="plus"
            @click="showCreateEnvDialog"
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
            <div
                class="env-list"
                v-bkloading="{ isLoading }"
            >
                <span
                    v-for="env in envList"
                    :key="env.id"
                    :class="{
                        'env-item': true,
                        'active': envId === env.envHashId
                    }"
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
            </div>
        </div>
        <CreateEnvDialog
            @success="handleCreateEnvSuccess"
        />
    </section>
</template>

<script>
    import { ref, computed, onMounted } from 'vue'
    import {
        ENV_TYPE_MAP
    } from '@/store/constants'
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
                initData,
                fetchEnvList
            } = useEnvAside()
            const envId = computed(() => proxy.$route.params?.envId)
            const envTypList = computed(() => ([
                {
                    id: ENV_TYPE_MAP.ALL,
                    name: proxy.$t('environment.allEnv'),
                    count: envList.value.length
                },
                {
                    id: ENV_TYPE_MAP.BUILD,
                    name: proxy.$t('environment.envInfo.buildEnvType')
                },
                {
                    id: ENV_TYPE_MAP.PROD,
                    name: proxy.$t('environment.envInfo.testEnvType')
                },
                {
                    id: ENV_TYPE_MAP.DEV,
                    name: proxy.$t('environment.envInfo.devEnvType')
                }
            ]))
            const handleCreateEnvSuccess = () => {
                // todo..
                fetchEnvList()
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
            onMounted(() => {
                console.log(123)
                initData()
            })
        
            return {
                envId,
                envType,
                envName,
                envList,
                envTypList,
                isLoading,
                initData,
                fetchEnvList,
                showCreateEnvDialog,
                handleChangeEnv,
                handleChangeEnvType,
                handleCreateEnvSuccess
            }
        }
    }
</script>

<style lang="scss" >
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
            padding: 8px 32px;
            cursor: pointer;
            border-radius: 2px;
            transition: all 0.3s;
            &:hover {
                background: #f5f7fa;
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
            &.active {
                color: #3A84FF;
                background: #FFFFFF;
            }
        }
    }
</style>
<template>
    <div class="environment-list-wrapper">
        <div
            class="filter-bar"
            v-if="showContent && (envList.length || searchValue.length || isRequesting)"
        >
            <bk-button
                :key="projectId"
                v-perm="{
                    permissionData: {
                        projectId: projectId,
                        resourceType: ENV_RESOURCE_TYPE,
                        resourceCode: projectId,
                        action: ENV_RESOURCE_ACTION.CREATE
                    }
                }"
                theme="primary"
                @click="toCreateEnv"
            >
                {{ $t('environment.new') }}
            </bk-button>
            <SearchSelect
                class="search-input ml15"
                v-model="searchValue"
                :placeholder="filterPlaceHolder"
                :data="filterData"
                :show-condition="false"
                clearable
                key="search"
            ></SearchSelect>
        </div>

        <section
            class="sub-view-port"
            v-bkloading="{
                isLoading: loading.isLoading,
                title: loading.title
            }"
        >
            <bk-table
                v-if="showContent && (envList.length || searchValue.length || isRequesting)"
                size="small"
                :data="envList"
                row-class-name="env-item-row"
                @row-click="toEnvDetail"
            >
                <bk-table-column
                    :label="$t('environment.envInfo.name')"
                    prop="name"
                ></bk-table-column>
                <bk-table-column
                    :label="$t('environment.envInfo.type')"
                    prop="envType"
                >
                    <template slot-scope="props">
                        <span v-if="props.row.envType === 'DEV'">{{ $t('environment.envInfo.devEnvType') }}</span>
                        <span v-if="props.row.envType === 'PROD'">{{ $t('environment.envInfo.testEnvType') }}</span>
                        <span v-if="props.row.envType === 'BUILD'">{{ $t('environment.envInfo.buildEnvType') }}</span>
                    </template>
                </bk-table-column>
                <bk-table-column
                    :label="$t('environment.envInfo.nodeCount')"
                    prop="nodeCount"
                >
                    <template slot-scope="props">
                        {{ props.row.nodeCount }}
                    </template>
                </bk-table-column>
                <bk-table-column
                    :label="$t('environment.envInfo.creationTime')"
                    prop="createdTime"
                >
                    <template slot-scope="props">
                        {{ localConvertTime(props.row.createdTime) }}
                    </template>
                </bk-table-column>
                <bk-table-column
                    :label="$t('environment.operation')"
                    width="160"
                >
                    <template slot-scope="props">
                        <template v-if="props.row.canUse">
                            <span
                                v-perm="{
                                    hasPermission: props.row.canDelete,
                                    disablePermissionApi: true,
                                    permissionData: {
                                        projectId: projectId,
                                        resourceType: ENV_RESOURCE_TYPE,
                                        resourceCode: props.row.envHashId,
                                        action: ENV_RESOURCE_ACTION.DELETE
                                    }
                                }"
                                :class="{ 'handler-text': props.row.canDelete }"
                                @click.stop="confirmDelete(props.row)"
                            >
                                {{ $t('environment.delete') }}
                            </span>
                        </template>
                        <template v-else>
                            <bk-button
                                theme="primary"
                                outline
                                @click="handleApplyPermission(props.row)"
                            >
                                {{ $t('environment.applyPermission') }}
                            </bk-button>
                        </template>
                    </template>
                </bk-table-column>
                <template #empty>
                    <EmptyTableStatus
                        :type="searchValue.length ? 'search-empty' : 'empty'"
                        @clear="clearFilter"
                    />
                </template>
            </bk-table>

            <empty-node
                v-if="showContent && !envList.length && !searchValue.length && !isRequesting"
                :is-env="true"
                :to-create-node="toCreateEnv"
                :empty-info="emptyInfo"
            ></empty-node>
        </section>
    </div>
</template>

<script>
    import emptyNode from './empty_node'
    import { convertTime } from '@/utils/util'
    import { ENV_RESOURCE_ACTION, ENV_RESOURCE_TYPE } from '@/utils/permission'
    import EmptyTableStatus from '@/components/empty-table-status'
    import SearchSelect from '@blueking/search-select'
    import '@blueking/search-select/dist/styles/index.css'

    export default {
        components: {
            emptyNode,
            SearchSelect,
            EmptyTableStatus
        },
        data () {
            return {
                ENV_RESOURCE_TYPE,
                ENV_RESOURCE_ACTION,
                showContent: false, // 显示内容
                envList: [], // 换环境列表
                loading: {
                    isLoading: false,
                    title: ''
                },
                emptyInfo: {
                    title: this.$t('environment.envInfo.emptyEnv'),
                    desc: this.$t('environment.envInfo.emptyEnvTips')
                },
                searchValue: [],
                isRequesting: false,
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            filterData () {
                const data = [
                    {
                        name: this.$t('environment.environmentName'),
                        id: 'envName',
                        default: true
                    },
                    {
                        name: this.$t('environment.environmentType'),
                        id: 'envType',
                        children: [
                            {
                                id: 'DEV',
                                name: this.$t('environment.envInfo.devEnvType')
                            },
                            {
                                id: 'PROD',
                                name: this.$t('environment.envInfo.testEnvType')
                            },
                            {
                                id: 'BUILD',
                                name: this.$t('environment.envInfo.buildEnvType')
                            }
                        ]
                    },
                    {
                        name: this.$t('environment.node'),
                        id: 'nodeHashId',
                        remoteMethod:
                            async (search) => {
                                const nodeList = await this.$store.dispatch('environment/requestNodeList', {
                                    projectId: this.projectId,
                                    params: {
                                        displayName: search
                                    }
                                })
                                return nodeList.records.map(item => ({
                                    name: item.displayName,
                                    id: item.nodeHashId
                                }))
                            },
                        inputInclude: true
                    }
                ]
                return data.filter(data => {
                    return !this.searchValue.find(val => val.id === data.id)
                })
            },
            filterPlaceHolder () {
                return this.filterData.map(item => item.name).join(' / ')
            }
        },
        watch: {
            projectId: async function (val) {
                this.searchValue = []
                await this.requestList()
            },
            searchValue (val) {
                const requestParams = {}
                val?.forEach(i => {
                    if (i.values?.length) requestParams[i.id] = i.values[0].id
                })
                this.requestList(requestParams)
            }
        },
        async mounted () {
            await this.requestList()
        },
        methods: {
            /**
             * 获取环境列表
             */
            async requestList (params = {}) {
                this.isRequesting = true
                try {
                    this.loading.isLoading = true
                    this.loading.title = this.$t('environment.loadingTitle')
                    const res = await this.$store.dispatch('environment/requestEnvList', {
                        projectId: this.projectId,
                        params
                    })
                    this.envList = [...res]
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                } finally {
                    this.showContent = true
                    this.isRequesting = false
                    this.loading.isLoading = false
                }
            },
            toCreateEnv () {
                this.$router.push({ name: 'createEnv' })
            },
            handleApplyPermission (row) {
                this.handleNoPermission({
                    projectId: this.projectId,
                    resourceType: ENV_RESOURCE_TYPE,
                    resourceCode: row.envHashId,
                    action: ENV_RESOURCE_ACTION.USE
                })
            },
            /**
             * 删除环境
             */
            async confirmDelete (row) {
                const id = row.envHashId
                
                this.$bkInfo({
                    type: 'warning',
                    theme: 'warning',
                    title: this.$t('environment.delete'),
                    subTitle: this.$t('environment.envInfo.deleteEnvTips', [row.name]),
                    confirmFn: async () => {
                        let message, theme
                        try {
                            await this.$store.dispatch('environment/toDeleteEnv', {
                                projectId: this.projectId,
                                envHashId: id
                            })

                            message = this.$t('environment.successfullyDeleted')
                            theme = 'success'
                            this.$bkMessage({
                                message,
                                theme
                            })
                        } catch (e) {
                            this.handleError(
                                e,
                                {
                                    projectId: this.projectId,
                                    resourceType: ENV_RESOURCE_TYPE,
                                    resourceCode: row.envHashId,
                                    action: ENV_RESOURCE_ACTION.DELETE
                                }
                            )
                        } finally {
                            this.requestList()
                        }
                    }
                })
            },
            /**
             * 跳转环境详情
             */
            toEnvDetail (row) {
                if (!row.canUse) return
                this.$router.push({
                    name: 'envDetail',
                    params: {
                        envId: row.envHashId
                    }
                })
            },
            /**
             * 处理时间格式
             */
            localConvertTime (timestamp) {
                return convertTime(timestamp * 1000)
            },
            clearFilter () {
                this.searchValue = []
            },
        }
    }
</script>

<style lang='scss'>
    @import './../scss/conf';

    .environment-list-wrapper {
        width: 100%;
        height: calc(100% - 48px);
        padding: 20px;
        overflow-y: auto;
        .sub-view-port {
            height: calc(100% - 52px);
            overflow: auto
        }
        .env-item-row {
            cursor: pointer;
        }

        .handler-text {
            color: $primaryColor;
            cursor: pointer;
        }
        .search-input {
            max-width: 40%;
            flex: 1;
            background: #fff;
            ::placeholder {
                color: #c4c6cc;
            }
        }
        .filter-bar {
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-bottom: 20px;
        }
    }
</style>

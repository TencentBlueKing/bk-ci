<template>
    <div class="environment-list-wrapper">
        <content-header class="env-header">
            <div slot="left">{{ $t('environment.environment') }}</div>
            <div slot="right" v-if="showContent && envList.length">
                <span
                    v-perm="{
                        permissionData: {
                            projectId: projectId,
                            resourceType: ENV_RESOURCE_TYPE,
                            resourceCode: projectId,
                            action: ENV_RESOURCE_ACTION.CREATE
                        }
                    }"
                >
                    <bk-button theme="primary" @click="toCreateEnv">{{ $t('environment.new') }}</bk-button>
                </span>
            </div>
        </content-header>

        <section class="sub-view-port"
            v-bkloading="{
                isLoading: loading.isLoading,
                title: loading.title
            }">
            <bk-table v-if="showContent && envList.length"
                size="small"
                :data="envList"
                row-class-name="env-item-row"
                @row-click="toEnvDetail">
                <bk-table-column :label="$t('environment.envInfo.name')" prop="name"></bk-table-column>
                <bk-table-column :label="$t('environment.envInfo.type')" prop="envType">
                    <template slot-scope="props">
                        <span v-if="props.row.envType === 'DEV'">{{ $t('environment.envInfo.devEnvType') }}</span>
                        <span v-if="props.row.envType === 'PROD'">{{ $t('environment.envInfo.testEnvType') }}</span>
                        <span v-if="props.row.envType === 'BUILD'">{{ $t('environment.envInfo.buildEnvType') }}</span>
                    </template>
                </bk-table-column>
                <bk-table-column :label="$t('environment.envInfo.nodeCount')" prop="nodeCount">
                    <template slot-scope="props">
                        {{ props.row.nodeCount }}
                    </template>
                </bk-table-column>
                <bk-table-column :label="$t('environment.envInfo.creationTime')" prop="createdTime">
                    <template slot-scope="props">
                        {{ localConvertTime(props.row.createdTime) }}
                    </template>
                </bk-table-column>
                <bk-table-column :label="$t('environment.operation')" width="160">
                    <template slot-scope="props">
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
                        >
                            <span
                                :class="{ 'handler-text': props.row.canDelete }"
                                @click.stop="confirmDelete(props.row)"
                            >
                                {{ $t('environment.delete') }}
                            </span>
                        </span>
                    </template>
                </bk-table-column>
            </bk-table>

            <empty-node v-if="showContent && !envList.length"
                :is-env="true"
                :to-create-node="toCreateEnv"
                :empty-info="emptyInfo"></empty-node>
        </section>
    </div>
</template>

<script>
    import emptyNode from './empty_node'
    import { convertTime } from '@/utils/util'
    import { ENV_RESOURCE_ACTION, ENV_RESOURCE_TYPE } from '@/utils/permission'

    export default {
        components: {
            emptyNode
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
                }
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            }
        },
        watch: {
            projectId: async function (val) {
                await this.init()
            }
        },
        async mounted () {
            await this.init()
        },
        methods: {
            async init () {
                const {
                    loading
                } = this

                loading.isLoading = true
                loading.title = this.$t('environment.loadingTitle')

                try {
                    this.requestList()
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                } finally {
                    setTimeout(() => {
                        this.loading.isLoading = false
                    }, 1000)
                }
            },
            /**
             * 获取环境列表
             */
            async requestList () {
                try {
                    const res = await this.$store.dispatch('environment/requestEnvList', {
                        projectId: this.projectId
                    })

                    this.envList.splice(0, this.envList.length)
                    res.forEach(item => {
                        this.envList.push(item)
                    })
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                }

                this.showContent = true
            },
            toCreateEnv () {
                this.$router.push({ name: 'createEnv' })
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
                this.$router.push({
                    name: 'envDetail',
                    params: {
                        envId: row.envHashId
                    },
                    query: {
                        canEdit: row.canEdit
                    }
                })
            },
            /**
             * 处理时间格式
             */
            localConvertTime (timestamp) {
                return convertTime(timestamp * 1000)
            }
        }
    }
</script>

<style lang='scss'>
    @import './../scss/conf';

    .environment-list-wrapper {
        height: 100%;
        overflow: hidden;
        .env-header {
            display: flex;
            justify-content: space-between;
            padding: 18px 20px;
            width: 100%;
            height: 60px;
            border-bottom: 1px solid $borderWeightColor;
            background-color: #fff;
            box-shadow:0px 2px 5px 0px rgba(51,60,72,0.03);
        }
        .env-item-row {
            cursor: pointer;
        }

        .handler-text {
            color: $primaryColor;
            cursor: pointer;
        }
    }
</style>

<template>
    <div class="environment-list-wrapper">
        <content-header class="env-header">
            <div slot="left">{{ $t('environment.environment') }}</div>
            <div slot="right" v-if="showContent && envList.length">
                <bk-button theme="primary" @click="toCreateEnv">{{ $t('environment.new') }}</bk-button>
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
                :row-class-name="'env-item-row'"
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
                        <span class="node-count-item">{{ props.row.nodeCount }}</span>
                    </template>
                </bk-table-column>
                <bk-table-column :label="$t('environment.envInfo.creationTime')" prop="createdTime">
                    <template slot-scope="props">
                        {{ localConvertTime(props.row.createdTime) }}
                    </template>
                </bk-table-column>
                <bk-table-column :label="$t('environment.operation')" width="160">
                    <template slot-scope="props">
                        <span class="handler-text" @click.stop="confirmDelete(props.row)">{{ $t('environment.delete') }}</span>
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

    export default {
        components: {
            emptyNode
        },
        data () {
            return {
                showContent: false, // 显示内容
                envList: [], // 换环境列表
                loading: {
                    isLoading: false,
                    title: ''
                },
                emptyInfo: {
                    title: this.$t('environment.emptyEnv'),
                    desc: this.$t('environment.emptyEnvTips')
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
                    res.map(item => {
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

                const h = this.$createElement
                const content = h('p', {
                    style: {
                        textAlign: 'center'
                    }
                }, `${this.$t('environment.comfirm')}${this.$t('environment.delete')}${this.$t('environment.environment')}(${row.name})？`)

                this.$bkInfo({
                    title: this.$t('environment.delete'),
                    subHeader: content,
                    confirmFn: async () => {
                        let message, theme
                        try {
                            await this.$store.dispatch('environment/toDeleteEnv', {
                                projectId: this.projectId,
                                envHashId: id
                            })

                            message = this.$t('environment.successfullyDeleted')
                            theme = 'success'
                        } catch (err) {
                            message = err.data ? err.data.message : err
                            theme = 'error'
                        } finally {
                            this.$bkMessage({
                                message,
                                theme
                            })
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

        .node-count-item,
        .handler-text {
            color: $primaryColor;
            cursor: pointer;
        }
    }
</style>

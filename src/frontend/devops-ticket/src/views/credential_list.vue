<template>
    <article class="credential-list">
        <inner-header>
            <template slot="left">
                <span class="inner-header-title">我的凭据</span>
            </template>
        </inner-header>

        <section class="sub-view-port" v-bkloading="{ isLoading: loading.isLoading, title: loading.title }">
            <div v-if="showContent && renderList.length" class="table-container">
                <bk-table
                    :data="renderList"
                    size="small"
                    :show-pagination-info="true"
                    :pagination="pagination"
                    @page-change="handlePageChange"
                    @page-limit-change="handlePageCountChange">
                    <bk-table-column label="名称" prop="credentialId"></bk-table-column>
                    <bk-table-column label="类型" prop="credentialType" :formatter="changeTicketType"></bk-table-column>
                    <bk-table-column label="描述" prop="credentialRemark"></bk-table-column>
                    <bk-table-column label="操作" width="200">
                        <template slot-scope="props">
                            <bk-button theme="primary" text :disabled="!props.row.permissions || !props.row.permissions.edit" @click="handleEditCredential(props.row.credentialId)">编辑</bk-button>
                            <bk-button theme="primary" text :disabled="!props.row.permissions || !props.row.permissions.delete" @click="handleDeleteCredentail(props.row.credentialId)">删除</bk-button>
                        </template>
                    </bk-table-column>
                </bk-table>
            </div>

            <empty-tips :title="tip.title" :desc="tip.desc" :btns="tip.btns" v-if="showContent && !renderList.length"></empty-tips>
        </section>
    </article>
</template>

<script>
    import innerHeader from '@/components/devops/inner_header'
    import { mapState } from 'vuex'
    import EmptyTips from '@/components/devops/emptyTips'

    export default {
        components: {
            EmptyTips,
            'inner-header': innerHeader
        },
        data () {
            return {
                viewId: 0,
                loading: {
                    isLoading: true,
                    title: ''
                },
                pagination: {
                    count: 0,
                    limit: 10,
                    current: 1
                },
                showContent: false,
                credentialList: [],
                renderList: [],
                tip: {
                    title: '暂无凭据',
                    desc: '您可以在新增凭据中新增一个凭据',
                    btns: [
                        {
                            text: '新增凭据',
                            type: 'primary',
                            size: 'normal',
                            handler: this.addCredentialHandler
                        }
                    ]
                }
            }
        },
        
        computed: {
            ...mapState('ticket', [
                'ticketType'
            ]),
            projectId () {
                return this.$route.params.projectId
            }
        },
        watch: {
            projectId: async function () {
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
                loading.title = '数据加载中，请稍候'

                try {
                    this.requestList()
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })

                    console.error(err)
                    console.error('获取凭据列表错误')
                } finally {
                    setTimeout(() => {
                        this.loading.isLoading = false
                    }, 300)
                }
            },
            updateList () {
                const { limit, current } = this.pagination
                this.renderList.splice(0, this.renderList.length, ...this.credentialList.slice((current - 1) * limit, current * limit))
            },
            handlePageChange (current) {
                this.pagination.current = current
                this.updateList()
            },
            handlePageCountChange (limit) {
                this.pagination.limit = limit
                if (this.pagination.current * limit > this.credentialList.length) {
                    this.pagination.current = 1
                }
                this.updateList()
            },
            async requestList () {
                try {
                    const res = await this.$store.dispatch('ticket/requestCredentialList', {
                        projectId: this.projectId,
                        page: 1,
                        pageSize: 1000
                    })
                    
                    this.credentialList.splice(0, this.credentialList.length, ...res.records)
                    this.updateList()
                    this.pagination.count = res.count
                } catch (err) {
                    if (err.code === 403) {
                        this.credentialList.splice(0, this.credentialList.length)
                        this.updateList()
                        this.showContent = true
                        return
                    }
                    const message = err.message ? err.message : err
                    const theme = 'error'
                    this.$bkMessage({
                        message,
                        theme
                    })
                }
                this.showContent = true
            },
            async handleDeleteCredentail (id) {
                this.$bkInfo({
                    title: '确认删除该凭据?',
                    confirmFn: async () => {
                        let message, theme

                        try {
                            await this.$store.dispatch('ticket/toDeleteCredential', {
                                projectId: this.projectId,
                                id: id
                            })
                            message = '删除凭据成功'
                            theme = 'success'
                        } catch (err) {
                            message = err.message ? err.message : err
                            theme = 'error'
                        } finally {
                            this.$bkMessage({
                                message,
                                theme
                            })
                        }
                        this.requestList()
                    }
                })
            },
            handleEditCredential (id) {
                this.$router.push({
                    name: 'editCredential',
                    params: {
                        credentialId: id
                    }
                })
            },
            changeTicketType (row, col, type) {
                const curType = this.ticketType.find(item => item.id === type)
                return curType && curType.name ? curType.name : type
            },
            addCredentialHandler () {
                this.$router.push('createCredential')
            }
        }
    }
</script>

<style lang="scss">
    @import './../scss/conf';
    .table-container {
        margin: 35px;
    }
</style>

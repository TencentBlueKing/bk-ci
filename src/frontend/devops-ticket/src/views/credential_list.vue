<template>
    <article class="credential-certificate-content">
        <content-header>
            <template slot="left">
                <span>{{ $t('ticket.myCredential') }}</span>
            </template>
        </content-header>

        <section class="sub-view-port" v-bkloading="{ isLoading: loading.isLoading, title: loading.title }">
            <div v-if="showContent && renderList.length" class="table-container">
                <bk-table
                    :data="renderList"
                    size="small"
                    :show-pagination-info="true"
                    :pagination="pagination"
                    @page-change="handlePageChange"
                    @page-limit-change="handlePageCountChange">
                    <bk-table-column :label="$t('ticket.id')" prop="credentialId"></bk-table-column>
                    <bk-table-column :label="$t('ticket.name')" prop="credentialName"></bk-table-column>
                    <bk-table-column :label="$t('ticket.type')" prop="credentialType" :formatter="changeTicketType"></bk-table-column>
                    <bk-table-column :label="$t('ticket.remark')" prop="credentialRemark"></bk-table-column>
                    <bk-table-column :label="$t('ticket.operation')" width="200">
                        <template slot-scope="props">
                            <bk-button theme="primary" :class="{ 'cert-operation-btn': true, disabled: !hasPermission(props.row.permissions, 'edit') }" text @click="handleEditCredential(props.row)">{{ $t('ticket.edit') }}</bk-button>
                            <bk-button theme="primary" :class="{ 'cert-operation-btn': true, disabled: !hasPermission(props.row.permissions, 'delete') }" text @click="handleDeleteCredentail(props.row)">{{ $t('ticket.delete') }}</bk-button>
                        </template>
                    </bk-table-column>
                </bk-table>
            </div>

            <empty-tips :title="tip.title" :desc="tip.desc" :btns="tip.btns" v-if="showContent && !renderList.length"></empty-tips>
        </section>
    </article>
</template>

<script>
    import { mapGetters } from 'vuex'
    import EmptyTips from '@/components/devops/emptyTips'

    export default {
        components: {
            EmptyTips
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
                    title: this.$t('ticket.credential.emptyCredential'),
                    desc: this.$t('ticket.credential.emptyCredentialTips'),
                    btns: [
                        {
                            text: this.$t('ticket.createCredential'),
                            type: 'primary',
                            size: 'normal',
                            handler: this.addCredentialHandler
                        }
                    ]
                }
            }
        },

        computed: {
            ...mapGetters('ticket', [
                'getTicketType'
            ]),
            projectId () {
                return this.$route.params.projectId
            },
            ticketType () {
                return this.getTicketType()
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
                loading.title = this.$t('ticket.loadingTitle')

                try {
                    this.requestList()
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })

                    console.error(err)
                    console.error(this.$t('ticket.credential.getCredentialError'))
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
            async handleDeleteCredentail (credential) {
                if (this.hasPermission(credential.permissions, 'delete')) {
                    this.$bkInfo({
                        width: 500,
                        theme: 'warning',
                        type: 'warning',
                        subTitle: this.$t('ticket.credential.deleteCredentialTips', [credential.credentialId]),
                        confirmFn: async () => {
                            let message, theme

                            try {
                                await this.$store.dispatch('ticket/toDeleteCredential', {
                                    projectId: this.projectId,
                                    id: credential.credentialId
                                })
                                message = this.$t('ticket.credential.successfullyDeletedCredential')
                                theme = 'success'
                                this.requestList()
                            } catch (err) {
                                message = err.message ? err.message : err
                                theme = 'error'
                            } finally {
                                message && this.$bkMessage({
                                    message,
                                    theme
                                })
                            }
                        }
                    })
                } else {
                    this.$showAskPermissionDialog({
                        noPermissionList: [{
                            actionId: this.$permissionActionMap.delete,
                            resourceId: this.$permissionResourceMap.credential,
                            instanceId: [{
                                id: credential.credentialId,
                                name: credential.credentialId
                            }],
                            projectId: this.projectId
                        }],
                        applyPermissionUrl: `/backend/api/perm/apply/subsystem/?client_id=ticket&project_code=${this.projectId}&service_code=ticket&role_creator=credential`
                    })
                }
            },
            handleEditCredential (credential) {
                if (this.hasPermission(credential.permissions, 'edit')) {
                    this.$router.push({
                        name: 'editCredential',
                        params: {
                            credentialType: credential.credentialType,
                            credentialId: credential.credentialId
                        }
                    })
                } else {
                    this.$showAskPermissionDialog({
                        noPermissionList: [{
                            actionId: this.$permissionActionMap.edit,
                            resourceId: this.$permissionResourceMap.credential,
                            instanceId: [{
                                id: credential.credentialId,
                                name: credential.credentialId
                            }],
                            projectId: this.projectId
                        }]
                    })
                }
            },
            hasPermission (permissions, action) {
                return permissions && permissions[action]
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
    .credential-operation-btn[disabled] {
      cursor: url(../images/cursor-lock.png),auto !important;
    }
</style>

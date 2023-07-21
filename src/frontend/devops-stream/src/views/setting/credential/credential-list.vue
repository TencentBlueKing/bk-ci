<template>
    <article class="credential-home" v-bkloading="{ isLoading }">
        <header class="home-header">
            <bk-button theme="primary" @click="addCredential">{{$t('setting.createCredential')}}</bk-button>
        </header>

        <bk-table :data="credentialList"
            :pagination="pagination"
            :outer-border="false"
            :header-border="false"
            :header-cell-style="{ background: '#fafbfd' }"
            @page-change="pageChange"
            @page-limit-change="pageLimitChange"
            :empty-text="$t('setting.ticket.emptyCredential')"
            class="credential-table"
        >
            <bk-table-column label="Key" prop="credentialId" show-overflow-tooltip></bk-table-column>
            <bk-table-column :label="$t('displayName')" prop="credentialName" show-overflow-tooltip></bk-table-column>
            <bk-table-column :label="$t('type')" prop="credentialType"></bk-table-column>
            <bk-table-column :label="$t('description')" prop="credentialRemark" show-overflow-tooltip></bk-table-column>
            <bk-table-column :label="$t('operation')" width="200" class-name="handler-btn">
                <template slot-scope="props">
                    <span :class="{ 'update-btn': true, disabled: !props.row.permissions.edit }" @click="editCredential(props.row)">{{$t('edit')}}</span>
                    <span :class="{ 'update-btn': true, disabled: !props.row.permissions.edit }" @click="credentialSetting(props.row.credentialId)">{{$t('settings')}}</span>
                    <span :class="{ 'update-btn': true, disabled: !props.row.permissions.delete }" @click="deleteCredential(props.row)">{{$t('delete')}}</span>
                </template>
            </bk-table-column>
        </bk-table>

        <edit-credential :show="show" :form="form" @hidden="hidden" @success="successOpt"></edit-credential>

        <bk-dialog v-model="deleteObj.show"
            :mask-close="false"
            :loading="isDelLoading"
            theme="danger"
            header-position="left"
            :title="$t('delete')"
            @confirm="requestDelete">
            {{$t('deleteTips')}} 【{{deleteObj.id}}】？
        </bk-dialog>
    </article>
</template>

<script>
    import { mapState } from 'vuex'
    import { setting } from '@/http'
    import EditCredential from '@/components/setting/edit-credential'

    export default {
        components: {
            EditCredential
        },

        data () {
            return {
                credentialList: [],
                pagination: {
                    count: 0,
                    current: 1,
                    limit: 10
                },
                form: {},
                show: false,
                deleteObj: {
                    show: false
                },
                isLoading: false,
                isDelLoading: false
            }
        },

        computed: {
            ...mapState(['appHeight', 'projectId'])
        },

        created () {
            this.getTicketList()
        },

        methods: {
            getTicketList () {
                const params = {
                    page: this.pagination.current,
                    pageSize: this.pagination.limit
                }
                this.isLoading = true
                setting.getTicketList(this.projectId, params).then((res) => {
                    const data = res || {}
                    this.pagination.count = data.count || 0
                    this.credentialList = data.records || []
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isLoading = false
                })
            },

            requestDelete () {
                this.isDelLoading = true
                setting.deleteTicket(this.projectId, this.deleteObj.id).then(() => {
                    this.getTicketList()
                    this.deleteObj.show = false
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isDelLoading = false
                })
            },

            pageChange (current) {
                if (current === this.pagination.current) return
                this.pagination.current = current
                this.getTicketList()
            },

            pageLimitChange (limit) {
                if (limit === this.pagination.limit) return
                this.pagination.current = 1
                this.pagination.limit = limit
                this.getTicketList()
            },

            editCredential (row) {
                if (!row.permissions.edit) return
                this.form = row
                this.show = true
            },

            credentialSetting (id) {
                this.$router.push({
                    name: 'credentialSettings',
                    params: {
                        credentialId: id
                    }
                })
            },

            deleteCredential (row) {
                if (!row.permissions.delete) return
                this.deleteObj.show = true
                this.deleteObj.id = row.credentialId
            },

            addCredential () {
                this.show = true
            },

            successOpt () {
                this.hidden()
                this.getTicketList()
            },

            hidden () {
                this.show = false
                this.form = {}
            }
        }
    }
</script>

<style lang="postcss" scoped>
    .credential-home {
        padding: 16px;
        height: calc(100vh - 61px);
        overflow: auto;
    }
    .home-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 20px;
    }
</style>

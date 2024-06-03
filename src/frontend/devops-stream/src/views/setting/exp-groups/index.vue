<template>
    <article class="group-home" v-bkloading="{ isLoading }">
        <header class="home-header">
            <bk-button theme="primary" @click="addGroup">{{$t('setting.userGroup.createGroup')}}</bk-button>
        </header>

        <bk-table :data="groupList"
            :pagination="pagination"
            :outer-border="false"
            :header-border="false"
            :header-cell-style="{ background: '#fafbfd' }"
            @page-change="pageChange"
            @page-limit-change="pageLimitChange"
            :empty-text="$t('setting.userGroup.emptyTips')"
            class="group-table"
        >
            <bk-table-column label="Id" prop="groupHashId"></bk-table-column>
            <bk-table-column :label="$t('name')" prop="name"></bk-table-column>
            <bk-table-column :label="$t('setting.userGroup.innerUsers')" prop="innerUsers">
                <template slot-scope="props">
                    <span>{{props.row.innerUsers.join(';')}}</span>
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('setting.userGroup.outerUsers')" prop="outerUsers">
                <template slot-scope="props">
                    <span>{{props.row.outerUsers.join(';')}}</span>
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('description')" prop="remark"></bk-table-column>
            <bk-table-column :label="$t('operation')" width="200" class-name="handler-btn">
                <template slot-scope="props">
                    <span :class="{ 'update-btn': true, disabled: !props.row.permissions.canEdit }" @click="editGroup(props.row)">{{$t('edit')}}</span>
                    <span :class="{ 'update-btn': true, disabled: !props.row.permissions.canDelete }" @click="deleteGroup(props.row)">{{$t('delete')}}</span>
                </template>
            </bk-table-column>
        </bk-table>

        <edit-exp-group :show="show" :form="form" @hidden="hidden" @success="successOpt"></edit-exp-group>

        <bk-dialog v-model="deleteObj.show"
            :mask-close="false"
            :loading="isDelLoading"
            theme="danger"
            header-position="left"
            :title="$t('delete')"
            @confirm="requestDelete">
            {{$t('deleteTips')}} delete【{{deleteObj.name}}】？
        </bk-dialog>
    </article>
</template>

<script>
    import { mapState } from 'vuex'
    import { setting } from '@/http'
    import EditExpGroup from '@/components/setting/edit-exp-group'

    export default {
        components: {
            EditExpGroup
        },

        data () {
            return {
                groupList: [],
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
            ...mapState(['projectId'])
        },

        created () {
            this.getExpGroupList()
        },

        methods: {
            getExpGroupList () {
                const params = {
                    page: this.pagination.current,
                    pageSize: this.pagination.limit
                }
                this.isLoading = true
                setting.getExpGroupList(this.projectId, params).then((res) => {
                    const data = res || {}
                    this.pagination.count = data.count || 0
                    this.groupList = data.records || []
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isLoading = false
                })
            },

            requestDelete () {
                this.isDelLoading = true
                setting.deleteExpGroup(this.projectId, this.deleteObj.id).then(() => {
                    this.getExpGroupList()
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
                this.getExpGroupList()
            },

            pageLimitChange (limit) {
                if (limit === this.pagination.limit) return
                this.pagination.current = 1
                this.pagination.limit = limit
                this.getExpGroupList()
            },

            editGroup (row) {
                if (!row.permissions.canEdit) return
                this.form = row
                this.show = true
            },

            deleteGroup (row) {
                if (!row.permissions.canDelete) return
                this.deleteObj.show = true
                this.deleteObj.id = row.groupHashId
                this.deleteObj.name = row.name
            },

            addGroup () {
                this.show = true
            },

            successOpt () {
                this.hidden()
                this.getExpGroupList()
            },

            hidden () {
                this.show = false
                this.form = {}
            }
        }
    }
</script>

<style lang="postcss" scoped>
    .group-home {
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

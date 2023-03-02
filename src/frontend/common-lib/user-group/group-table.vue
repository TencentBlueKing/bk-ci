<template>
    <article class="group-table">
        <bk-table
            v-bkloading="{ isLoading }"
            :data="memberList">
            <bk-table-column :label="$t('userGroup')" prop="groupName"></bk-table-column>
            <bk-table-column :label="$t('createdTime')" prop="createdTime">
                <template #default="{ row }">
                    <span>{{ row.createdTime ? row.createdTime : '--' }} </span>
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('expiredTime')" prop="expiredDisplay">
                <template #default="{ row }">
                    <span>{{ row.expiredDisplay ? row.expiredDisplay + $t('day') : '--' }} </span>
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('status')" prop="status">
                <template #default="{ row }">
                    <div class="status-content">
                        <img :src="statusIcon(row.status)" class="status-icon">
                        {{ statusFormatter(row.status) }}
                    </div>
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('operation')">
                <template #default="{ row }">
                    <bk-button class="btn" theme="primary" text @click="handleViewDetail(row)">{{ $t('permissionDetail') }}</bk-button>
                    <bk-button class="btn" theme="primary" text v-if="row.status === 'NOT_JOINED'" @click="handleApply(row)">{{ $t('applyJoin') }}</bk-button>
                    <bk-button class="btn" theme="primary" text v-if="['EXPIRED', 'NORMAL'].includes(row.status)" @click="handleRenewal(row)">{{ $t('renewal') }}</bk-button>
                    <bk-button class="btn" theme="primary" text v-if="['EXPIRED', 'NORMAL'].includes(row.status)" @click="handleShowLogout(row)">{{ $t('exit') }}</bk-button>
                </template>
            </bk-table-column>
        </bk-table>
        <bk-sideslider
            :is-show.sync="showDetail"
            :width="640"
            quick-close
            @hidden="handleHidden"
        >
            <template #header>
                <div class="detail-title">
                    {{ $t('permissionDetail') }}
                    <span class="group-name">{{ groupName }}</span>
                </div>
            </template>
            <template #content>
                <div class="detail-content" v-bkloading="{ isLoading: isDetailLoading }">
                    <div class="title">{{ $t('pipelineManage') }}</div>
                    <div class="content">
                        <bk-checkbox
                            v-for="(item, index) in groupPolicies"
                            :key="index"
                            v-model="item.permission"
                            disabled
                            class="permission-item"
                        >
                            {{ item.actionName }}
                        </bk-checkbox>
                    </div>
                </div>
            </template>
        </bk-sideslider>
        <bk-dialog
            :value="logout.isShow"
            :title="$t('确认退出用户组')"
            :loading="logout.loading"
            @confirm="handleLogout"
            @cancel="handleCancelLogout"
        >
            {{ $t('exitGroupTips', [logout.name]) }}
        </bk-dialog>
        <apply-dialog
            :is-show.sync="apply.isShow"
            v-bind="apply"
            :resource-type="resourceType"
        />
    </article>
</template>

<script>
    import ApplyDialog from './apply-dialog.vue'
    import syncDefault from './svg/sync-default.svg'
    import syncSuccess from './svg/sync-success.svg'
    import syncFailed from './svg/sync-failed.svg'

    const initFormData = () => {
        return {
            isShow: false,
            groupName: '',
            groupId: '',
            expiredDisplay: '',
            title: '',
            type: ''
        }
    }
    export default {
        name: 'UserTable',

        components: {
            ApplyDialog
        },

        props: {
            // 资源类型
            resourceType: {
                type: String,
                default: ''
            },
            // 资源ID
            resourceCode: {
                type: String,
                default: ''
            },
            // 项目id => englishName
            projectCode: {
                type: String,
                default: ''
            }
        },

        data () {
            return {
                showDetail: false,
                logout: {
                    loading: false,
                    isShow: false,
                    groupId: '',
                    name: ''
                },
                apply: initFormData(),
                memberList: [],
                isLoading: false,
                isDetailLoading: false,
                groupPolicies: [],
                groupName: ''
            }
        },

        mounted () {
            this.getMemberList()
        },

        methods: {
            handleHidden () {
                this.showDetail = false
            },

            getMemberList () {
                this.isLoading = true
                return this.$ajax
                    .get(`/auth/api/user/auth/resource/${this.projectCode}/${this.resourceType}/${this.resourceCode}/groupMember`)
                    .then((res) => {
                        this.memberList = res.data
                    })
                    .catch((err) => {
                        this.$bkMessage({
                            theme: 'error',
                            message: err.message || err
                        })
                    })
                    .finally(() => {
                        this.isLoading = false
                    })
            },

            handleViewDetail (row) {
                const { groupId, groupName } = row
                this.groupName = groupName
                this.showDetail = true
                this.isDetailLoading = true
                this.$ajax
                    .get(`/auth/api/user/auth/resource/group/${this.projectCode}/${this.resourceType}/${groupId}/groupPolicies`)
                    .then(({ data }) => {
                        this.groupPolicies = data
                    })
                    .catch((err) => {
                        this.$bkMessage({
                            theme: 'error',
                            message: err.message || err
                        })
                    })
                    .finally(() => {
                        this.isDetailLoading = false
                    })
            },

            statusFormatter (status) {
                const map = {
                    NOT_JOINED: this.$t('notJoined'),
                    NORMAL: this.$t('normal'),
                    EXPIRED: this.$t('expired')
                }
                return map[status]
            },

            statusIcon (status) {
                const map = {
                    NOT_JOINED: syncDefault,
                    NORMAL: syncSuccess,
                    EXPIRED: syncFailed
                }
                return map[status]
            },

            handleRenewal (row) {
                this.apply.isShow = true
                this.apply.groupName = row.groupName
                this.apply.groupId = row.groupId
                this.apply.expiredDisplay = row.expiredDisplay
                this.apply.title = this.$t('renewal')
                this.apply.type = 'renewal'
            },

            handleApply (row) {
                this.apply.isShow = true
                this.apply.groupName = row.groupName
                this.apply.groupId = row.groupId
                this.apply.title = this.$t('applyJoin')
                this.apply.type = 'apply'
            },

            handleShowLogout (row) {
                this.logout.isShow = true
                this.logout.groupId = row.groupId
                this.logout.name = row.groupName
            },

            handleCancelLogout () {
                this.logout.isShow = false
            },

            handleLogout () {
                this.logout.loading = true
                return this.$ajax
                    .delete(`/auth/api/user/auth/resource/group/${this.projectCode}/${this.resourceType}/${this.logout.groupId}/member`)
                    .then(() => {
                        this.handleCancelLogout()
                        this.getMemberList()
                    })
                    .catch((err) => {
                        this.$bkMessage({
                            theme: 'error',
                            message: err.message || err
                        })
                    })
                    .finally(() => {
                        this.logout.loading = false
                    })
            }
        }
    }
</script>

<style lang="scss" scoped>
    .btn {
        margin-right: 5px;
    }
    .group-name {
        font-size: 12px;
        color: #979BA5;
        margin-left: 10px;
    }
    .status-content {
        display: flex;
        align-items: center;
    }
    .status-icon {
        height: 16px;
        width: 16px;
        margin-right: 5px;
    }
    .detail-content {
        padding: 20px;
        .title {
            font-size: 14px;
            color: #313238;
            margin-left: 20px;
            &::before {
                content: '';
                position: absolute;
                left: 30px;
                top: 22px;
                width: 4px;
                height: 16px;
                background: #699DF4;
                border-radius: 1px;
            }
        }
        .content {
            margin-top: 15px;
        }
        .permission-item {
            min-width: 150px;
            margin-bottom: 10px;
            cursor: default !important;
        }
    }
</style>

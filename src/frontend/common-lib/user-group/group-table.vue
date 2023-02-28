<template>
    <article class="group-table">
        <bk-table
            v-bkloading="{ isLoading }"
            :data="memberList"
            :border="['outer']">
            <bk-table-column :label="$t('用户组')" prop="groupName"></bk-table-column>
            <bk-table-column :label="$t('添加时间')" prop="createdTime"></bk-table-column>
            <bk-table-column :label="$t('有效期至')" prop="expiredTime"></bk-table-column>
            <bk-table-column :label="$t('状态')" prop="status" :formatter="statusFormatter"></bk-table-column>
            <bk-table-column :label="$t('操作')">
                <template #default="{ row }">
                    <bk-button class="btn" theme="primary" text>{{ $t('权限详情') }}</bk-button>
                    <bk-button class="btn" theme="primary" text>{{ $t('申请加入') }}</bk-button>
                    <bk-button class="btn" theme="primary" text>{{ $t('续期') }}</bk-button>
                    <bk-button class="btn" theme="primary" text @click="handleShowLogout(row)">{{ $t('退出') }}</bk-button>
                </template>
            </bk-table-column>
        </bk-table>
        <bk-sideslider
            :is-show="showDetail"
            :width="640"
            quick-close
            @hidden="handleHidden"
        >
            <template #header>
                <div class="detail-title">
                    {{ $t('权限详情') }}
                    <span class="group-name">{{ 'CI-App2.0迭代-查看者' }}</span>
                </div>
            </template>
            <template #default>
                <div class="detail-content">
                    todo..
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
            {{ $t('退出后，将无法再使用【[0]】所赋予的权限。', [logout.name]) }}
        </bk-dialog>
        <apply-dialog
            :is-show.sync="apply.isShow"
            :group-name="apply.groupName"
            :group-id="apply.groupId"
            :expired-time="apply.expiredTime"
        />
    </article>
</template>

<script>
    import ApplyDialog from './apply-dialog.vue'

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
                apply: {
                    isShow: false,
                    groupName: '',
                    groupId: '',
                    expiredTime: ''
                },
                memberList: [],
                isLoading: false
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

            statusFormatter (row, column, cellValue, index) {
                const map = {
                    NOT_JOINED: this.$t('未加入'),
                    NORMAL: this.$t('正常'),
                    EXPIRED: this.$t('已过期')
                }
                return map[cellValue]
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
</style>

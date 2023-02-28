<template>
    <user-group
        :resource-type="resourceType"
        :resource-code="resourceCode"
        :project-code="projectCode"
        :group-list="groupList"
        :is-loading="isLoading"
        :iam-iframe-path="iamIframePath"
        :has-permission="hasPermission"
        :is-enable-permission="isEnablePermission"
        :open-manage="handleOpenManage"
        :close-manage="handleCloseManage"
        :delete-group="handleDeleteGroup"
        :fetch-group-list="fetchGroupList"
        :show-create-group="false"
    />
</template>

<script>
    import UserGroup from '../../../../common-lib/user-group/index.vue'
    import { mapActions } from 'vuex'

    export default {
        name: 'auth-tab',
        components: {
            UserGroup
        },
        data () {
            return {
                hasPermission: false,
                isEnablePermission: false,
                iamIframePath: 'user-group-detail/29912',
                resourceType: 'pipeline',
                groupList: [],
                memberGroupList: [],
                isLoading: false
            }
        },
        computed: {
            projectCode () {
                return this.$route.params.projectId
            },
            resourceCode () {
                return this.$route.params.pipelineId
            }
        },
        async created () {
            await this.fetchHasManagerPermissionFromApi()
            await this.fetchEnablePermissionFromApi()
            await this.getUserList()
        },
        methods: {
            ...mapActions('pipelines', [
                'fetchHasManagerPermission',
                'fetchEnablePermission',
                'enableGroupPermission',
                'disableGroupPermission',
                'fetchUserGroupList',
                'deleteGroup'
            ]),
            async getUserList () {
                // 管理员获取用户组数据
                if (this.isEnablePermission && this.hasPermission) {
                    await this.fetchGroupList()
                }
            },
            /**
             * 是否为资源的管理员
             */
            fetchHasManagerPermissionFromApi () {
                this.isLoading = true
                const {
                    projectCode,
                    resourceType,
                    resourceCode
                } = this

                return this
                    .fetchHasManagerPermission({
                        projectCode,
                        resourceType,
                        resourceCode
                    })
                    .then((res) => {
                        this.hasPermission = res?.data
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
            /**
             * 是否开启了权限管理
             */
            fetchEnablePermissionFromApi () {
                this.isLoading = true
                const {
                    projectCode,
                    resourceType,
                    resourceCode
                } = this

                return this
                    .fetchEnablePermission({
                        projectCode,
                        resourceType,
                        resourceCode
                    })
                    .then((res) => {
                        this.isEnablePermission = res?.data
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
            /**
             * 开启权限管理
             */
            handleOpenManage () {
                const {
                    resourceType,
                    resourceCode,
                    projectCode
                } = this

                return this
                    .enableGroupPermission({
                        resourceType,
                        resourceCode,
                        projectCode
                    })
                    .then((res) => {
                        if (res?.data) {
                            this.$bkMessage({
                                theme: 'success',
                                message: this.$t('开启成功')
                            })
                            this.isEnablePermission = true
                            this.getUserList()
                        }
                    })
                    .catch((err) => {
                        this.$bkMessage({
                            theme: 'error',
                            message: err.message || err
                        })
                    })
            },
            /**
             * 关闭权限管理
             */
            handleCloseManage () {
                const {
                    resourceType,
                    resourceCode,
                    projectCode
                } = this

                return this
                    .disableGroupPermission({
                        resourceType,
                        resourceCode,
                        projectCode
                    })
                    .then((res) => {
                        if (res?.data) {
                            this.isEnablePermission = false
                            this.$bkMessage({
                                theme: 'success',
                                message: this.$t('关闭成功')
                            })
                        }
                    })
                    .catch((err) => {
                        this.$bkMessage({
                            theme: 'error',
                            message: err.message || err
                        })
                    })
            },

            /**
             * 获取用户组列表 (管理员、创建者)
             */
            fetchGroupList () {
                const {
                    resourceType,
                    resourceCode,
                    projectCode
                } = this

                return this
                    .fetchUserGroupList({
                        resourceType,
                        resourceCode,
                        projectCode
                    })
                    .then((res) => {
                        this.groupList = res.data
                        this.iamIframePath = `user-group-detail/${res[0]?.groupId}?role_id=${res[0]?.managerId}`
                    })
                    .catch((err) => {
                        this.$bkMessage({
                            theme: 'error',
                            message: err.message || err
                        })
                    })
            },
            
            /**
             * 删除用户组
             */
            handleDeleteGroup (group) {
                const {
                    resourceType,
                    projectCode
                } = this

                return this
                    .deleteGroup({
                        resourceType,
                        projectCode,
                        groupId: group.id
                    })
                    .then(() => {
                        this.$bkMessage({
                            theme: 'success',
                            message: this.$t('删除成功')
                        })
                        this.fetchGroupList()
                    })
                    .catch((err) => {
                        this.$bkMessage({
                            theme: 'error',
                            message: err.message || err
                        })
                    })
            }
        }
    }
</script>

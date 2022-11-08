<template>
    <div class="release-setting-wrapper" v-bkloading="{ isLoading: loading.isLoading, title: loading.title }">
        <content-header>
            <div slot="left">{{ $route.meta.title }}</div>
        </content-header>
        <section class="sub-view-port">
            <bk-tab :active.sync="curTab" type="unborder-card">
                <bk-tab-panel
                    v-for="(panel, index) in panels"
                    v-bind="panel"
                    :key="index"
                >
                    <template v-if="curTab === 'experienceGroup'">
                        <div v-if="showContent && experienceList.length" class="table-operate-bar">
                            <bk-button theme="primary" @click="toCreateGroup">新增</bk-button>
                        </div>
                        <bk-table v-if="showContent && experienceList.length" :data="experienceList">
                            <bk-table-column label="名称" prop="name"></bk-table-column>
                            <bk-table-column label="内部人员" prop="innerUsersCount">
                                <template slot-scope="props">
                                    <bk-popover placement="bottom" :trigger="props.row.innerUsersCount ? 'mouseenter focus' : 'manual'">
                                        <span class="handler-inner">{{ props.row.innerUsersCount }}</span>
                                        <template slot="content">
                                            <p style="max-width: 300px; text-align: left; white-space: normal;word-break: break-all;font-weight: 400;">
                                                {{ props.row.innerUsers.join(",") }}
                                            </p>
                                        </template>
                                    </bk-popover>
                                </template>
                            </bk-table-column>
                            <bk-table-column label="外部人员" prop="outerUsersCount">
                                <template slot-scope="props">
                                    <bk-popover placement="bottom" :trigger="props.row.outerUsersCount ? 'mouseenter focus' : 'manual'">
                                        <span class="handler-outer">{{ props.row.outerUsersCount }}</span>
                                        <template slot="content">
                                            <p style="max-width: 300px; text-align: left; white-space: normal;word-break: break-all;font-weight: 400;">
                                                <span>{{ props.row.outerUsers.join(',') }}</span>
                                            </p>
                                        </template>
                                    </bk-popover>
                                </template>
                            </bk-table-column>
                            <bk-table-column label="创建人" prop="creator"></bk-table-column>
                            <bk-table-column label="描述" prop="remark"></bk-table-column>
                            <bk-table-column label="操作" prop="creator">
                                <template slot-scope="props">
                                    <div class="handler-group">
                                        <span class="handler-btn edit-btn" @click="toEditGroup(props.row)">编辑</span>
                                        <span class="handler-btn delete-btn" @click="toDeleteGruop(props.row)">删除</span>
                                    </div>
                                </template>
                            </bk-table-column>
                        </bk-table>
                        <empty-data v-if="showContent && !experienceList.length"
                            :empty-info="emptyInfo"
                            :to-create-fn="toCreateGroup">
                        </empty-data>
                    </template>
                </bk-tab-panel>
            </bk-tab>

            <experience-group
                :node-select-conf="nodeSelectConf"
                :create-group-form="createGroupForm"
                :outers-list="outersList"
                :loading="dialogLoading"
                :on-change="onChange"
                :error-handler="errorHandler"
                @after-submit="afterCreateGroup"
                :cancel-fn="cancelFn"
            >
            </experience-group>
        </section>
    </div>
</template>

<script>
    import emptyData from './empty-data'
    import experienceGroup from './create_group'
    import { getQueryString } from '@/utils/util'

    export default {
        components: {
            emptyData,
            experienceGroup
        },
        data () {
            return {
                curTab: 'experienceGroup',
                experienceList: [],
                showContent: false,
                outersList: [],
                loading: {
                    isLoading: false,
                    title: ''
                },
                dialogLoading: {
                    isLoading: false,
                    title: ''
                },
                nodeSelectConf: {
                    title: '',
                    isShow: false,
                    closeIcon: false,
                    hasHeader: false,
                    quickClose: false
                },
                createGroupForm: {
                    idEdit: false,
                    name: '',
                    internal_list: [],
                    external_list: [],
                    desc: ''
                },
                errorHandler: {
                    nameError: false
                },
                emptyInfo: {
                    title: '暂无体验组',
                    desc: '您可以新增一个体验组'
                },
                urlParams: getQueryString('groupId') || ''
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            panels () {
                return [
                    {
                        name: 'experienceGroup',
                        label: '体验组'
                    }
                ]
            }
        },
        watch: {
            projectId () {
                this.$router.push({
                    name: 'experienceList',
                    params: {
                        projectId: this.projectId
                    }
                })
            }
        },
        async mounted () {
            await this.init()
            this.fetchOutersList()
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
                } finally {
                    setTimeout(() => {
                        this.loading.isLoading = false
                    }, 1000)
                }
            },
            /**
             * 获取外部体验人员列表
             */
            async fetchOutersList () {
                this.loading.isLoading = true
                try {
                    const res = await this.$store.dispatch('experience/fetchOutersList', {
                        projectId: this.projectId
                    })
                    res.forEach(item => {
                        this.outersList.push({
                            id: item.username,
                            name: item.username
                        })
                    })
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                } finally {
                    this.loading.isLoading = false
                }
            },
            /**
             * 获取列表
             */
            async requestList () {
                try {
                    const res = await this.$store.dispatch('experience/requestGroupList', {
                        projectId: this.projectId
                    })

                    this.experienceList.splice(0, this.experienceList.length)
                    res.records.forEach(item => {
                        this.experienceList.push(item)
                        if (this.urlParams === item.groupHashId) {
                            setTimeout(() => {
                                this.toEditGroup(item)
                            }, 800)
                        }
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
            toCreateGroup () {
                this.createGroupForm = {
                    isEdit: false,
                    groupHashId: '',
                    name: '',
                    internal_list: [],
                    external_list: [],
                    desc: ''
                }
                this.nodeSelectConf.title = '新增体验组'
                this.nodeSelectConf.isShow = true
            },
            onChange (tags) {
                this.createGroupForm.internal_list = tags
            },
            validate () {
                let errorCount = 0
                if (!this.createGroupForm.name) {
                    this.errorHandler.nameError = true
                    errorCount++
                }

                if (errorCount > 0) {
                    return false
                }

                return true
            },
            afterCreateGroup () {
                this.requestList()
                this.nodeSelectConf.isShow = false
            },
            cancelFn () {
                if (!this.dialogLoading.isLoading) {
                    this.nodeSelectConf.isShow = false
                }
            },
            async toEditGroup (row) {
                if (row.permissions.canEdit) {
                    this.nodeSelectConf.title = '编辑体验组'
                    this.nodeSelectConf.isShow = true
                    this.dialogLoading.isLoading = true

                    try {
                        const res = await this.$store.dispatch('experience/toGetGroupDetail', {
                            projectId: this.projectId,
                            groupHashId: row.groupHashId
                        })

                        this.createGroupForm.isEdit = true
                        this.createGroupForm.groupHashId = row.groupHashId
                        this.createGroupForm.name = res.name
                        this.createGroupForm.external_list = res.outerUsers
                        this.createGroupForm.desc = res.remark
                        this.createGroupForm.internal_list = res.innerUsers
                    } catch (err) {
                        const message = err.data ? err.data.message : err
                        const theme = 'error'

                        this.$bkMessage({
                            message,
                            theme
                        })
                    } finally {
                        this.dialogLoading.isLoading = false
                    }
                } else {
                    this.$showAskPermissionDialog({
                        noPermissionList: [{
                            actionId: this.$permissionActionMap.edit,
                            resourceId: this.$permissionResourceMap.experienceGroup,
                            instanceId: [{
                                id: row.groupHashId,
                                name: row.name
                            }],
                            projectId: this.projectId
                        }],
                        applyPermissionUrl: `/backend/api/perm/apply/subsystem/?client_id=code&project_code=${this.projectId}&service_code=experience&role_manager=group:${row.groupHashId}`
                    })
                }
            },
            toDeleteGruop (row) {
                if (row.permissions.canDelete) {
                    this.$bkInfo({
                        title: '确认',
                        subTitle: '确认删除该体验组',
                        confirmFn: async () => {
                            let message, theme

                            try {
                                await this.$store.dispatch('experience/toDeleteGroups', {
                                    projectId: this.projectId,
                                    groupHashId: row.groupHashId
                                })

                                message = '删除成功'
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
                } else {
                    this.$showAskPermissionDialog({
                        noPermissionList: [{
                            actionId: this.$permissionActionMap.delete,
                            resourceId: this.$permissionResourceMap.experienceGroup,
                            instanceId: [{
                                id: row.groupHashId,
                                name: row.name
                            }],
                            projectId: this.projectId
                        }],
                        applyPermissionUrl: `/backend/api/perm/apply/subsystem/?client_id=code&project_code=${this.projectId}&service_code=experience&role_manager=group:${row.groupHashId}`
                    })
                }
            }
        }
    }
</script>

<style lang="scss">
    @import './../../scss/conf';

    %flex {
        display: flex;
        align-items: center;
    }

    .release-setting-wrapper {
        height: 100%;
        .table-operate-bar {
            margin: 10px 0;
        }

        .paas-ci-empty {
            height: calc(100% - 70px);
        }

        .handler-btn {
            margin-right: 8px;
            position: relative;
            cursor: pointer;
            color: $primaryColor;

            &:last-child {
                margin: 0;
            }
        }
        .bk-tab-label-item{
            background-color: transparent !important;
        }
    }
</style>

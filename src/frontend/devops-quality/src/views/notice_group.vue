<template>
    <div class="notice-group-wrapper">
        <div class="inner-header">
            <div class="title">{{$t('quality.通知组')}}</div>
        </div>

        <section
            class="sub-view-port"
            v-bkloading="{
                isLoading: loading.isLoading,
                title: loading.title
            }">

            <div class="group-content">
                <bk-button theme="primary" class="create-group-btn" v-if="showContent && noticeGroupList.length"
                    @click="toCreateGroup">{{$t('quality.新增')}}</bk-button>
                <div class="table-container" v-if="showContent && noticeGroupList.length">
                    <bk-table
                        size="small"
                        class="experience-table"
                        :data="noticeGroupList">
                        <bk-table-column :label="$t('quality.名称')" prop="name">
                            <template slot-scope="props">
                                <span>{{props.row.name}}</span>
                            </template>
                        </bk-table-column>
                        <bk-table-column :label="$t('quality.通知人员')" prop="innerUsersCount">
                            <template slot-scope="props">
                                <bk-popover placement="bottom" v-if="props.row.innerUsersCount">
                                    <span class="handler-inner">{{props.row.innerUsersCount}}</span>
                                    <template slot="content">
                                        <p style="max-width: 300px; text-align: left; white-space: normal;word-break: break-all;font-weight: 400;">
                                            <span v-for="(entry, index) in props.row.innerUsers" :key="index">{{entry.replace('"', '')}}<span v-if="index !== (props.row.innerUsers.length - 1)">,</span></span>
                                        </p>
                                    </template>
                                </bk-popover>
                                <span class="handler-inner" v-else>{{props.row.innerUsersCount}}</span>
                            </template>
                        </bk-table-column>
                        <bk-table-column :label="$t('quality.创建人')" prop="creator">
                            <template slot-scope="props">
                                <span>{{props.row.creator}}</span>
                            </template>
                        </bk-table-column>
                        <bk-table-column :label="$t('quality.描述')" prop="remark" min-width="160">
                            <template slot-scope="props">
                                <span>{{props.row.remark}}</span>
                            </template>
                        </bk-table-column>
                        <bk-table-column :label="$t('quality.操作')" width="150">
                            <template slot-scope="props">
                                <span class="handler-btn edit-btn" @click="toEditGroup(props.row)">{{$t('quality.编辑')}}</span>
                                <span class="handler-btn delete-btn" @click="toDeleteGruop(props.row)">{{$t('quality.删除')}}</span>
                            </template>
                        </bk-table-column>
                    </bk-table>
                </div>
                <empty-data v-if="showContent && !noticeGroupList.length"
                    :empty-info="emptyInfo"
                    :to-create-fn="toCreateGroup">
                </empty-data>
            </div>

            <createGroup :node-select-conf="nodeSelectConf"
                :create-group-form="createGroupForm"
                :loading="dialogLoading"
                :on-change="onChange"
                :on-init="onInit"
                :error-handler="errorHandler"
                :display-result="displayResult"
                @confirmFn="confirmFn"
                :cancel-fn="cancelFn"></createGroup>
        </section>
    </div>
</template>

<script>
    import createGroup from '@/components/devops/create_group'
    import { getQueryString } from '@/utils/util'
    import { mapGetters } from 'vuex'
    import emptyData from './empty_data'

    export default {
        components: {
            emptyData,
            createGroup
        },
        data () {
            return {
                noticeGroupList: [],
                showContent: false,
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
                    hasFooter: false
                },
                createGroupForm: {
                    idEdit: false,
                    name: '',
                    internal_list: [],
                    external_list: '',
                    desc: ''
                },
                errorHandler: {
                    nameError: false
                },
                emptyInfo: {
                    title: this.$t('quality.暂无通知组'),
                    desc: this.$t('quality.您可以新增一个通知组')
                },
                urlParams: getQueryString('groupId') || ''
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            ...mapGetters('quality', [
                'getUserGroup'
            ])
        },
        watch: {
            projectId () {
                this.$router.push({
                    name: 'qualityOverview',
                    params: {
                        projectId: this.projectId
                    }
                })
            },
            getUserGroup (val) {
                val.forEach(item => {
                    if (this.createGroupForm.internal_list.indexOf(item) === -1) {
                        this.createGroupForm.internal_list.push(item)
                    }
                })
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
                loading.title = this.$t('quality.数据加载中，请稍候')

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
                    }, 100)
                }
            },
            /**
             * 获取列表
             */
            async requestList () {
                try {
                    const res = await this.$store.dispatch('quality/requestGroupList', {
                        projectId: this.projectId
                    })

                    this.noticeGroupList.splice(0, this.noticeGroupList.length)
                    if (res.records) {
                        res.records.forEach(item => {
                            this.noticeGroupList.push(item)
                        })
                    }
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
                    external_list: '',
                    desc: ''
                }
                // document.getElementById('placeholder-text').style.display = 'block';
                this.nodeSelectConf.title = this.$t('quality.新增通知组')
                this.nodeSelectConf.isShow = true
            },
            onChange (name, val) {
                this.createGroupForm.internal_list = val
            },
            onInit (e, arr) {
                // this.createGroupForm.internal_list.splice(0, this.createGroupForm.internal_list, ...arr)
            },
            displayResult () {
                if (this.nodeSelectConf.isShow) {
                    this.createGroupForm.external_list = this.createGroupForm.external_list.replace(/[^\d;,]/g, '')
                }
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
            async confirmFn (params) {
                let message, theme
                this.dialogLoading.isLoading = true

                try {
                    if (this.createGroupForm.isEdit) {
                        await this.$store.dispatch('quality/editUserGroups', {
                            projectId: this.projectId,
                            groupHashId: this.createGroupForm.groupHashId,
                            params: params
                        })
                    } else {
                        await this.$store.dispatch('quality/createUserGroups', {
                            projectId: this.projectId,
                            params: params
                        })
                    }

                    message = this.$t('quality.保存成功')
                    theme = 'success'
                    this.requestList()
                    this.nodeSelectConf.isShow = false
                } catch (err) {
                    message = err.message ? err.message : err
                    theme = 'error'
                } finally {
                    this.$bkMessage({
                        message,
                        theme
                    })
                    this.dialogLoading.isLoading = false
                }
            },
            cancelFn () {
                if (!this.dialogLoading.isLoading) {
                    this.nodeSelectConf.isShow = false
                    this.errorHandler.nameError = false
                }
            },
            async toEditGroup (row) {
                if (row.permissions.canEdit) {
                    this.nodeSelectConf.title = this.$t('quality.编辑通知组')
                    this.nodeSelectConf.isShow = true
                    this.dialogLoading.isLoading = true

                    try {
                        const res = await this.$store.dispatch('quality/toGetGroupDetail', {
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
                        this.$bkMessage({
                            message: err.data ? err.data.message : err,
                            theme: 'error'
                        })
                    } finally {
                        this.dialogLoading.isLoading = false
                    }
                } else {
                    const params = {
                        noPermissionList: [
                            { resource: this.$t('quality.通知组'), option: this.$t('quality.编辑') }
                        ],
                        applyPermissionUrl: PERM_URL_PREFIX
                    }

                    this.$showAskPermissionDialog(params)
                }
            },
            toDeleteGruop (row) {
                if (row.permissions.canDelete) {
                    this.$bkInfo({
                        type: 'warning',
                        theme: 'warning',
                        subTitle: this.$t('quality.确定删除通知组({0})？', [row.name]),
                        confirmFn: async () => {
                            let message, theme

                            try {
                                await this.$store.dispatch('quality/toDeleteGroups', {
                                    projectId: this.projectId,
                                    groupHashId: row.groupHashId
                                })

                                message = this.$t('quality.删除成功')
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
                    const params = {
                        noPermissionList: [
                            { resource: this.$t('quality.通知组'), option: this.$t('quality.删除') }
                        ],
                        applyPermissionUrl: PERM_URL_PREFIX
                    }

                    this.$showAskPermissionDialog(params)
                }
            }
        }
    }
</script>

<style lang="scss">
    @import '@/scss/conf.scss';

    %flex {
        display: flex;
        align-items: center;
    }
    .notice-group-wrapper {
        .inner-header {
            display: flex;
            justify-content: space-between;
            padding: 18px 20px;
            width: 100%;
            height: 60px;
            border-bottom: 1px solid $borderWeightColor;
            background-color: #fff;
            box-shadow:0px 2px 5px 0px rgba(51,60,72,0.03);
            .title {
                font-size: 16px;
            }
        }
        .group-content {
            height: calc(100% - 60px);
        }
        .create-group-btn {
            margin: 20px 0 0 20px;
        }
        .experience-table {
            td {
                .cell {
                    padding: 10px 15px;
                    span {
                        display: inline-block;
                        overflow: hidden;
                    }
                }
                &:last-child {
                    color: $primaryColor;
                }
            }
            .desc-col {
                overflow: hidden;
                text-overflow: ellipsis;
            }
            .handler-group {
                min-width: 130px;
            }
            .handler-btn {
                margin-right: 16px;
                position: relative;
                cursor: pointer;
                &:last-child {
                    margin: 0;
                }
            }
            .tips-td {
                position: relative;
            }
        }
        .paas-ci-empty {
            height: calc(100% - 70px);
        }
        .table-container {
            position: relative;
            padding: 20px;
            min-height: 320px;
            overflow: auto;
        }
    }
</style>

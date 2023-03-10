<template>
    <div
        v-bkloading="{ isLoading: isDataLoading }"
        style="width: 100%"
    >
        <div class="manage-header">
            {{ $t('projectManage') }}
        </div>
        <section class="biz-pm biz-pm-index biz-create-pm">
            <template v-if="projectList.length || isDataLoading">
                <div class="action-layout">
                    <bk-button
                        theme="primary"
                        icon="plus"
                        class="mr10"
                        @click="handleNewProject"
                    >
                        {{ $t('newProject') }}
                    </bk-button>
                    <bk-input
                        v-model="inputValue"
                        class="search-input"
                        right-icon="bk-icon icon-search"
                        :placeholder="$t('searchProject')"
                    ></bk-input>
                </div>
                <bk-table
                    class="biz-table mt20"
                    size="medium"
                    :data="curProjectList"
                    :pagination="pagination"
                    @page-change="pageChange"
                    @page-limit-change="limitChange"
                >
                    <bk-table-column
                        :label="$t('projectName')"
                        prop="logoAddr"
                        width="300"
                    >
                        <template slot-scope="{ row }">
                            <div class="project-name-cell">
                                <span
                                    v-if="row.logoAddr"
                                    class="avatar"
                                >
                                    <img
                                        class="avatar-addr"
                                        :src="row.logoAddr"
                                    >
                                </span>
                                <span
                                    v-else
                                    class="avatar"
                                    :class="['project-avatar', `match-color-${matchForCode(row.projectCode)}`]"
                                >
                                    {{ row.projectName.substr(0, 1) }}
                                </span>
                                <div class="info">
                                    <bk-button text @click="goToProjectManage(row)">{{ row.projectName }}</bk-button>
                                </div>
                            </div>
                        </template>
                    </bk-table-column>
                    <bk-table-column
                        :label="$t('projectId')"
                        prop="englishName"
                    />
                    <bk-table-column
                        :label="$t('projectDesc')"
                        prop="description"
                    />
                    <bk-table-column
                        :label="$t('administrator')"
                        prop="creator"
                    />
                    <bk-table-column
                        :label="$t('projectStatus')"
                        prop="creator"
                    >
                        <template slot-scope="{ row }">
                            <span class="project-status">
                                <bk-switcher
                                    :value="row.enabled"
                                    class="mr5"
                                    size="small"
                                    theme="primary"
                                    :disabled="[1, 3].includes(row.approvalStatus)"
                                    @change="handleChangeEnabled(row)"
                                />
                                <span class="mr5">
                                    {{ row.enabled ? approvalStatusMap[row.approvalStatus] : $t('已停用') }}
                                </span>
                                <icon
                                    v-bk-tooltips="{ content: $t('新建项目申请已拒绝') }"
                                    v-if="row.approvalStatus === 3"
                                    class="devops-icon status-icon"
                                    :size="16"
                                    name="warning-circle-small"
                                />
                                <icon
                                    v-bk-tooltips="{ content: $t('项目信息修改申请审批中') }"
                                    v-if="row.approvalStatus === 4"
                                    class="devops-icon status-icon"
                                    :size="16"
                                    name="wait-small"
                                />
                            </span>
                        </template>
                    </bk-table-column>
                    <bk-table-column
                        :label="$t('projectOperation')"
                        width="200"
                    >
                        <template slot-scope="{ row }">
                            <bk-button
                                class="mr5"
                                text
                                @click="handleGoUserGroup(row)"
                            >
                                {{ $t('userGroupManage') }}
                            </bk-button>
                            <bk-button
                                text
                                @click="handleGoExtend(row)"
                            >
                                {{ $t('extendManage') }}
                            </bk-button>
                        </template>
                    </bk-table-column>
                </bk-table>
            </template>
            <empty-tips
                v-else
                :show-lock="true"
                :title="$t('notFindProject')"
                :desc="$t('notFindProjectTips')"
            >
                <bk-button
                    icon-left="icon-plus"
                    theme="primary"
                    @click="handleNewProject()"
                >
                    {{ $t('newProject') }}
                </bk-button>
                
                <bk-button
                    theme="success"
                    @click="handleApplyProject"
                >
                    {{ $t('applyProject') }}
                </bk-button>
            </empty-tips>
            <apply-project-dialog ref="applyProjectDialog"></apply-project-dialog>
        </section>
    </div>
</template>

<script>
    import { mapActions } from 'vuex'
    import ApplyProjectDialog from '../components/ApplyProjectDialog/index.vue'
    import {
        handleProjectNoPermission,
        RESOURCE_ACTION
    } from '@/utils/permission'

    export default ({
        name: 'ProjectManage',
        components: {
            ApplyProjectDialog
        },
        data () {
            return {
                isDataLoading: false,
                projectList: [],
                pagination: {
                    current: 1,
                    count: 0,
                    limit: 10
                },
                matchColorList: [
                    'green',
                    'yellow',
                    'red',
                    'blue'
                ],
                inputValue: '',
                approvalStatusMap: {
                    1: this.$t('创建中'),
                    2: this.$t('已启用'),
                    3: this.$t('创建中'),
                    4: this.$t('已启用')
                }
            }
        },
        computed: {
            curProjectList () {
                const { limit, current } = this.pagination
                const list = this.projectList.filter(i => i.projectName.includes(this.inputValue)) || []
                this.pagination.count = list.length
                return list.slice(limit * (current - 1), limit * current)
            }
        },
        created () {
            this.fetchProjects()
        },
        methods: {
            ...mapActions(['fetchProjectList', 'toggleProjectEnable']),
            async fetchProjects () {
                this.isDataLoading = true
                await this.fetchProjectList().then(res => {
                    this.projectList = res
                }).catch(() => [])
                this.isDataLoading = false
            },

            matchForCode (projectCode) {
                const event = projectCode.substr(0, 1)
                const key = event.charCodeAt() % 4
                return this.matchColorList[key]
            },

            handleNewProject () {
                const { origin } = window.location
                window.location.href = `${origin}/console/manage/apply`
            },

            handleApplyProject () {
                this.$refs.applyProjectDialog.isShow = true
            },

            handleGoUserGroup (row) {
                const { projectCode, relationId, routerTag } = row
                const projectTag = this.getProjectTag(routerTag)
                switch (projectTag) {
                    case 'v0':
                        window.open(`/console/perm/my-project?project_code=${projectCode}`)
                        break
                    case 'v3':
                        window.open(`/console/ps/${projectCode}/${relationId}/member?x-devops-project-id=${projectCode}`)
                        break
                    case 'rbac':
                        window.open(`/console/manage/${projectCode}/group?x-devops-project-id=${projectCode}`)
                        break
                }
            },

            handleGoExtend (row) {
                const { englishName: projectCode, routerTag } = row
                const projectTag = this.getProjectTag(routerTag)
                switch (projectTag) {
                    case 'v0':
                    case 'v3':
                        window.open(`/console/store/serviceManage/${projectCode}`)
                        break
                    case 'rbac':
                        window.open(`/console/manage/${projectCode}/expand`)
                        break
                }
            },

            pageChange (page) {
                this.pagination.current = page
            },

            limitChange (limit) {
                this.pagination.current = 1
                this.pagination.limit = limit
            },

            goToProjectManage (row) {
                const { englishName: projectCode, relationId, routerTag } = row
                const projectTag = this.getProjectTag(routerTag)
                switch (projectTag) {
                    case 'v0':
                        window.open(`/console/perm/my-project?project_code=${projectCode}`)
                        break
                    case 'v3':
                        window.open(`/console/ps/${projectCode}/${relationId}/member?x-devops-project-id=${projectCode}`)
                        break
                    case 'rbac':
                        window.open(`/console/manage/${projectCode}/show?x-devops-project-id=${projectCode}`)
                        break
                }
            },
            handleChangeEnabled (row) {
                const { englishName: projectCode, enabled, projectName, routerTag } = row
                this.toggleProjectEnable({
                    projectCode: projectCode,
                    enabled: enabled
                }).then(() => {
                    row.enabled = !row.enabled
                    this.$bkMessage({
                        message: row.enabled ? this.$t('启用项目成功') : this.$t('停用项目成功'),
                        theme: 'success'
                    })
                }).catch((error) => {
                    if (error.code === 403) {
                        const projectTag = this.getProjectTag(routerTag)
                        const url = projectTag === 'rbac'
                            ? `/console/permission/apply?project_code=${projectCode}&resourceType=project&resourceName=${projectName}&action=project_enable&iamResourceCode=${projectCode}&groupId`
                            : `/console/perm/apply-perm?project_code=${projectCode}`
                        handleProjectNoPermission(
                            {
                                projectId: projectCode,
                                resourceCode: projectCode,
                                action: RESOURCE_ACTION.ENABLE
                            },
                            {
                                actionName: this.$t('enableDisableProject'),
                                groupInfoList: [{ url }],
                                resourceName: projectName,
                                resourceTypeName: this.$t('project')
                            }
                        )
                    } else {
                        this.$bkMessage({
                            message: error.message || error,
                            theme: 'error'
                        })
                    }
                })
            },
            getProjectTag (routerTag) {
                if (/v3/.test(routerTag)) {
                    return 'v3'
                }
                if (/rbac/.test(routerTag)) {
                    return 'rbac'
                }
                return 'v0'
            }
        }
    })
</script>

<style lang="scss" scoped>
    @import '../assets/scss/mixins/ellipsis';
    .manage-header {
        width: 100%;
        height: 60px;
        padding: 0 30px;
        border-bottom: 1px solid #dde4eb;
        box-shadow: 0 2px 5px rgb(0 0 0 / 3%);
        display: flex;
        background: #fff;
        align-items: center;
        font-size: 16px;
        color: #313238;
    }
    .biz-pm-index {
        width: 100%;
        height: calc(100% - 60px);
        overflow-y: scroll;
        padding: 24px;
    }
    .action-layout {
        display: flex;
        justify-content: space-between;
        .search-input {
            width: 320px;
        }
    }
    .biz-order {
        padding: 0;
        min-width: 20px;
        text-align: center;
    }
    .biz-pm-page {
        text-align: center;
        margin-top: 30px;
    }
    .biz-pm-header {
        margin: 30px 0 25px 0;
        height: 36px;
        line-height: 36px;
        .title {
            float: left;
            font-size: 18px;
            color: #333948;
            a {
                color: #333948;
            }
        }
        .action {
            float: right;
        }
        .search-input-row {
            float: right;
            margin-left: 45px;
            width: 220px;
        }
    }
    .biz-table {
        font-weight: normal;
        td:first-child {
            display: flex;
            align-items: center;
        }
        .title {
            color: #7b7d8a;
            font-weight: bold;
            white-space: nowrap;
            padding: 0;
            margin: 0 0 5px 0;
            a {
                color: #333948;
                &:hover {
                    color: #3c96ff;
                }
            }
        }
        .action {
            text-align: center;
        }
        .time {
            color: #a3a4ac;
        }
        .disabled {
            color: #c3cdd7;
            .title,
            .time,
            .desc {
                color: #c3cdd7 !important;
            }
        }
        .project-name-cell {
            display: flex;
            align-items: center;
            .avatar {
                display: inline-block;
                position: relative;
                margin-right: 10px;
                width: 32px;
                height: 32px;
                line-height: 30px;
                border-radius: 16px;
                text-align: center;
                color: #fff;
                font-size: 16px;
            }
            .avatar-addr {
                width: 100%;
                height: 100%;
                border-radius: 16px;
                object-fit: cover;
            }
            .match-color-green {
                background-color: #30D878;
            }
            .match-color-yellow {
                background-color: #FFB400;
            }
            .match-color-red {
                background-color: #FF5656;
            }
            .match-color-blue {
                background-color: #3C96FF;
            }
        }
        
    }
    .biz-pm-form {
        margin: 0 auto 15px auto;
    }
    .bk-form-checkbox {
        margin-right: 35px;
    }
    .desc {
        word-break: break-all;
    }
    .biz-text-bum {
        position: absolute;
        bottom: 8px;
        right: 10px;
        font-size: 12px;
    }

    .create-project-dialog {
        button.disabled {
            background-color: #fafafa;
            border-color: #e6e6e6;
            color: #cccccc;
            cursor: not-allowed;
            &:hover {
                background-color: #fafafa;
                border-color: #e6e6e6;
            }
        }
    }

    .biz-guide-box {
        background-color: #fff;
        padding: 75px 30px;
        border-radius: 4px;
        box-shadow: 0 0 3px rgba(0, 0, 0, .1);
        text-align: center;
        margin-top: 30px;
        .title {
            font-size: 22px;
            color: #333;
        }
    }
    .project-status {
        display: flex;
    }
    .status-icon {
        margin-right: 5px;
    }
</style>

<style lang="scss">
    @import '../assets/scss/conf.scss';
    @import '../assets/scss/mixins/scroller.scss';

    @media screen and (max-width: $mediaWidth) {
        .biz-create-pm .bk-dialog-body {
            max-height: 440px;
            // overflow: auto;
            @include scroller(#9e9e9e);
        }
    }
</style>

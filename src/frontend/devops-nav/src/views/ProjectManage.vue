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
                        clearable
                        right-icon="bk-icon icon-search"
                        :placeholder="$t('searchProject')"
                    ></bk-input>
                </div>

                <div class="filter-operation">
                    <span
                        :class="{ 'is-selected': isEnabled }"
                        @click="isEnabled = true"
                    >{{ $t('启用中') }}</span>
                    <span
                        :class="{ 'is-selected': !isEnabled }"
                        @click="isEnabled = false"
                    >{{ $t('已停用') }}</span>
                </div>
                <bk-table
                    class="biz-table"
                    size="medium"
                    :data="curProjectList"
                    :default-sort="sortField"
                    :pagination="pagination"
                    @sort-change="handleSortChange"
                    @page-change="pageChange"
                    @page-limit-change="limitChange"
                >
                    <bk-table-column
                        :label="$t('projectName')"
                        sortable="custom"
                        prop="projectName"
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
                                    <bk-button
                                        text
                                        v-perm="{
                                            hasPermission: row.canView,
                                            disablePermissionApi: true,
                                            permissionData: {
                                                projectId: row.projectCode,
                                                resourceType: 'project',
                                                resourceCode: row.projectCode,
                                                action: RESOURCE_ACTION.VIEW
                                            }
                                        }"
                                        :key="row.projectCode"
                                        @click="goToProjectManage(row)"
                                    >
                                        {{ row.projectName }}
                                    </bk-button>
                                </div>
                            </div>
                        </template>
                    </bk-table-column>
                    <bk-table-column
                        :label="$t('projectId')"
                        sortable="custom"
                        prop="englishName"
                    />
                    <bk-table-column
                        :label="$t('projectDesc')"
                        prop="description"
                        width="300"
                    />
                    <bk-table-column
                        :label="$t('projectCreator')"
                        prop="creator"
                    />
                    <bk-table-column
                        :label="$t('projectStatus')"
                        prop="creator"
                    >
                        <template slot-scope="{ row, $index }">
                            <span class="project-status">
                                <div
                                    class="enable-switcher"
                                    v-perm="{
                                        hasPermission: row.managePermission,
                                        disablePermissionApi: true,
                                        permissionData: {
                                            projectId: row.projectCode,
                                            resourceType: 'project',
                                            resourceCode: row.projectCode,
                                            action: RESOURCE_ACTION.ENABLE
                                        }
                                    }"
                                    @click="handleChangeEnabled(row, $index)"
                                >
                                </div>
                                <bk-switcher
                                    :value="row.enabled"
                                    class="mr5"
                                    size="small"
                                    theme="primary"
                                    :disabled="[1, 3, 4].includes(row.approvalStatus) || !row.managePermission"
                                />
                                <span class="mr5">
                                    {{ row.enabled ? approvalStatusMap[row.approvalStatus] : $t('已停用') }}
                                </span>
                                <div
                                    v-bk-tooltips="{ content: $t('新建项目申请已拒绝') }"
                                    v-if="row.approvalStatus === 3"
                                    class="devops-icon status-icon"
                                >
                                    <img src="../assets/scss/logo/warning-circle-small.svg">
                                </div>
                                <div
                                    v-bk-tooltips="{ content: $t('项目信息修改申请审批中') }"
                                    v-if="row.approvalStatus === 4"
                                    class="devops-icon status-icon"
                                >
                                    <img src="../assets/scss/logo/wait-small.svg">
                                </div>
                            </span>
                        </template>
                    </bk-table-column>
                    <bk-table-column
                        :label="$t('projectOperation')"
                    >
                        <template slot-scope="{ row }">
                            <bk-button
                                class="mr5"
                                text
                                v-perm="{
                                    hasPermission: row.managePermission,
                                    disablePermissionApi: true,
                                    permissionData: {
                                        projectId: row.projectCode,
                                        resourceType: 'project',
                                        resourceCode: row.projectCode,
                                        action: RESOURCE_ACTION.MANAGE
                                    }
                                }"
                                :disabled="row.approvalStatus === 1"
                                @click="handleGoUserGroup(row)"
                            >
                                {{ $t('projectMembers') }}
                            </bk-button>
                            <!-- <bk-button
                                text
                                :disabled="row.approvalStatus === 1"
                                @click="handleGoExtend(row)"
                            >
                                {{ $t('extendManage') }}
                            </bk-button> -->
                            <bk-button
                                text
                                @click="handleQuitClick(row)"
                            >
                                {{ $t('projectExit') }}
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
                    @click="handleNewProject"
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

        <bk-dialog
            :width="710"
            header-position="center"
            footer-position="center"
            v-model="showDialog"
            :auto-close="false"
            :render-directive="'if'"
            ext-cls="exit-project-dialog"
        >
            <template slot="header">
                <img
                    src="../assets/scss/logo/warninfo.svg"
                    :width="42"
                    :height="42"
                >
                <h2 class="dialog-header"> {{ $t('存在需清理或交接的权限/授权') }} </h2>
            </template>
            <main v-bkloading="{ isLoading: isMainLoading }">
                <div class="project-content">
                    <p class="tips">
                        <span>
                            <img
                                src="../assets/scss/logo/warning-circle-fill.svg"
                                :width="14"
                                :height="14"
                            >
                            <i18n
                                path="检测到X项权限/授权不能直接退出，请先清理资源、或在下方填写交接人移交给新负责人"
                                tag="span"
                            >
                                <span class="tips-num">{{ exitProject.transferNeededNum }}</span>
                            </i18n>
                        </span>
                        <span class="refresh">
                            <img
                                src="../assets/scss/logo/refresh.svg"
                                :width="12"
                                :height="12"
                            >
                            <span @click="handleRefresh">{{ $t('刷新') }}</span>
                        </span>
                    </p>
                    <ul
                        class="service-list"
                    >
                        <li
                            v-for="(label, tips) in exitProjectCounts"
                            :key="tips"
                            v-if="label.count"
                        >
                            <p class="item">
                                <span class="item-name">{{ $t(tips) }}</span>
                                <span class="item-num">{{ label.count }}</span>
                            </p>
                            <p class="go-detail">
                                <img
                                    src="../assets/scss/logo/jump-link-line.svg"
                                    :width="12"
                                    :height="12"
                                >
                                <span @click="goToPermission(label.key)">{{ $t('详情') }}</span>
                            </p>
                        </li>
                    </ul>
                </div>
                <div class="handover-content">
                    <bk-form
                        ref="formRef"
                        :model="handOverForm"
                        form-type="vertical"
                        ext-cls="exit-form"
                    >
                        <bk-form-item
                            required
                            property="name"
                            label-position="right"
                            :label="$t('批量交接-交接人')"
                            :rules="[
                                { required: true, message: $t('请输入移交人'), trigger: 'blur' }
                            ]"
                        >
                            <ProjectUserSelector
                                :project-id="projectId"
                                class="selector-input"
                                @change="handleChangeOverFormName"
                                @removeAll="handleClearOverFormName"
                            >
                            </ProjectUserSelector>
                        </bk-form-item>
                    </bk-form>
                    <p class="label-tip">{{ $t('可以批量交接给交接人，交接人同意后，将成功退出项目') }}</p>
                </div>
            </main>
            <template slot="footer">
                <div class="bk-dialog-outer">
                    <bk-button
                        theme="danger"
                        :loading="quitLoading"
                        @click="handleHandoverConfirm"
                    >
                        {{ $t('confirm') }}
                    </bk-button>
                    <bk-button
                        class="close-btn"
                        @click="handleClosed"
                    >
                        {{ $t('cancel') }}
                    </bk-button>
                </div>
            </template>
        </bk-dialog>
    </div>
</template>

<script>
    import {
        handleProjectNoPermission,
        RESOURCE_ACTION
    } from '@/utils/permission'
    import { mapActions } from 'vuex'
    import ApplyProjectDialog from '../components/ApplyProjectDialog/index.vue'
    import ProjectUserSelector from '@/components/ProjectUserSelector/index.vue'
    import authInfo from '@/utils/auth'
    
    const PROJECT_SORT_FILED = {
        projectName: 'PROJECT_NAME',
        englishName: 'ENGLISH_NAME'
    }
    
    const ORDER_ENUM = {
        ascending: 'ASC',
        descending: 'DESC'
    }
    export default ({
        name: 'ProjectManage',
        components: {
            ApplyProjectDialog,
            ProjectUserSelector
        },
        data () {
            return {
                RESOURCE_ACTION,
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
                },
                isEnabled: true, // 查询过滤-已启用项目
                showDialog: false,
                confirmLoading: false,
                projectId: '',
                projectName: '',
                handOverForm: {
                    id: '',
                    name: '',
                    type: ''
                },
                exitProject: {},
                quitLoading: false,
                isMainLoading: false,
                targetMember: null
            }
        },
        computed: {
            curProjectList () {
                const { limit, current } = this.pagination
                const list = this.projectList.filter(i => i.projectName.includes(this.inputValue) && i.enabled === this.isEnabled) || []
                this.pagination.count = list.length
                return list.slice(limit * (current - 1), limit * current)
            },

            sortField () {
                const { sortType, collation } = this.$route.query
                const prop = sortType || localStorage.getItem('projectSortType')
                const order = collation || localStorage.getItem('projectSortCollation')
                return {
                    prop: this.getkeyByValue(PROJECT_SORT_FILED, prop),
                    order: this.getkeyByValue(ORDER_ENUM, order)
                }
            },

            exitProjectCounts () {
                return {
                    流水线权限代持: {
                        count: this.exitProject.pipelineAuthorizationCount,
                        key: 'pipeline'
                    },
                    代码库授权: {
                        count: this.exitProject.repositoryAuthorizationCount,
                        key: 'repertory'
                    },
                    部署节点授权: {
                        count: this.exitProject.envNodeAuthorizationCount,
                        key: 'env_node'
                    },
                    资源唯一拥有者: {
                        count: this.exitProject.uniqueManagerCount,
                        key: 'uniqueManager'
                    }
                }
            }
        },
        watch: {
            inputValue (val) {
                this.pagination.current = 1
            },
            isEnabled (val) {
                this.pagination.current = 1
            }
        },
        created () {
            this.fetchProjects()
        },
        methods: {
            ...mapActions([
                'fetchProjectList',
                'toggleProjectEnable',
                'checkMemberExitsProject',
                'memberExitsProject'
            ]),
            getkeyByValue (obj, value) {
                return Object.keys(obj).find(key => obj[key] === value)
            },
            async fetchProjects (params) {
                this.isDataLoading = true
                await this.fetchProjectList(params).then(res => {
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
                const { origin } = window.location
                window.location.href = `${origin}/console/permission/apply`
            },

            handleGoUserGroup (row) {
                const { projectCode, relationId, routerTag } = row
                const projectTag = this.getProjectTag(routerTag)
                switch (projectTag) {
                    case 'v0':
                        window.location.href = `/console/perm/my-project?project_code=${projectCode}`
                        break
                    case 'v3':
                        window.location.href = `/console/ps/${projectCode}/${relationId}/member?x-devops-project-id=${projectCode}`
                        break
                    case 'rbac':
                        window.location.href = `/console/manage/${projectCode}/group?x-devops-project-id=${projectCode}`
                        break
                }
            },

            handleGoExtend (row) {
                const { englishName: projectCode, routerTag } = row
                const projectTag = this.getProjectTag(routerTag)
                switch (projectTag) {
                    case 'v0':
                    case 'v3':
                        window.location.href = `/console/store/serviceManage/${projectCode}`
                        break
                    case 'rbac':
                        window.location.href = `/console/manage/${projectCode}/expand`
                        break
                }
            },

            /**
             * 无异常情况，正常退出
             */
            normalExit () {
                const h = this.$createElement
                this.$bkInfo({
                    title: this.$t('确认退出项目?'),
                    extCls: 'info-box',
                    width: 480,
                    theme: 'danger',
                    confirmLoading: true,
                    subHeader: h('div', { class: 'info-content' },
                                 [
                                     h('div', { class: 'info-project' },
                                       [
                                           h('span', { class: 'label' }, this.$t('项目：')),
                                           h('span', { class: 'value' }, this.projectName)
                                       ]
                                     ),
                                     h('div', { class: 'info-tips' }, this.$t('退出时，将清理你在此项目下获得的权限，确认退出吗？'))
                                 ]),
                    confirmFn: async () => {
                        try {
                            const params = {
                                projectId: this.projectId,
                                handoverParams: {
                                    targetMember: {
                                        id: this.targetMember.username,
                                        name: this.targetMember.chineseName,
                                        type: 'user',
                                        departed: true
                                    }
                                }
                            }
                            await this.memberExitsProject(params)
                            this.$bkMessage({
                                message: this.$t('已成功退出项目'),
                                theme: 'success'
                            })
                            this.projectList = this.projectList.filter(item => item.englishName !== this.projectId)
                        } catch (error) {
                            this.$bkMessage({
                                message: error.message,
                                theme: 'error'
                            })
                        }
                    }
                })
            },
            /**
             * 通过「组织架构」获得权限，无法退出
             * @param row
             */
            unableToExit (departments, managers) {
                const h = this.$createElement
                this.$bkInfo({
                    type: 'warning',
                    theme: 'danger',
                    title: this.$t('不能退出项目'),
                    extCls: 'info-box',
                    width: 480,
                    showFooter: false,
                    subHeader: h('div', { class: 'info-content footer-none' },
                                 [
                                     h('div', { class: 'info-project' },
                                       [
                                           h('span', { class: 'label' }, this.$t('项目：')),
                                           h('span', { class: 'value' }, this.projectName)
                                       ]
                                     ),
                                     h('div', { class: 'info-tips' }, [
                                         h('span', this.$t('存在通过组织架构')),
                                         h('span', { class: 'reminder' }, departments),
                                         h('span', this.$t('获得权限的资源，无法直接退出。请联系项目管理员')),
                                         h('span', { class: 'reminder' },
                                           managers.length > 2
                                               ? (
                                                   [h('span', `${managers.slice(0, 2).join('，')}.....`),
                                                    h('span', {
                                                        style: { color: '#3A84FF', cursor: 'pointer' },
                                                        directives: [
                                                            {
                                                                name: 'bk-tooltips',
                                                                value: managers.join('，'),
                                                                modifiers: { click: true }
                                                            }
                                                        ]
                                                    }, this.$t('展开'))]
                                               )
                                               : managers.join(', ')
                                         ),
                                         h('span', this.$t('，评估按照组织架构添加权限是否合理。'))
                                     ])
                                 ]
                    )
                })
            },
            /**
             * 需要完成交接，才能退出
             */
            async handleHandoverConfirm () {
                try {
                    const valid = await this.$refs.formRef.validate()
                    if (valid) {
                        const { displayName, ...handoverTo } = this.handOverForm
                        const params = {
                            projectId: this.projectId,
                            handoverParams: {
                                targetMember: {
                                    id: this.targetMember.username,
                                    name: this.targetMember.chineseName,
                                    type: 'user',
                                    departed: true
                                },
                                handoverTo: {
                                    ...handoverTo,
                                    departed: true
                                }
                            }
                        }
                        this.quitLoading = true

                        const res = await this.memberExitsProject(params)
                        this.quitLoading = false
                        this.showDialog = false
                        const h = this.$createElement
                        this.$bkInfo({
                            title: this.$t('提交成功?'),
                            extCls: 'info-box',
                            theme: 'danger',
                            width: 480,
                            okText: this.$t('查看进度'),
                            cancelText: this.$t('关闭'),
                            subHeader: h('div',
                                         { class: 'info-content' },
                                         [
                                             h('div', { class: 'info-tips' },
                                               [
                                                   h('p', { class: 'info-text' }, this.$t('已成功提交「移交权限」申请，等待交接人确认。')),
                                                   h('p', { class: 'info-text' }, [
                                                       h('span', this.$t('可在“')),
                                                       h('span', {
                                                           style: { color: '#3A84FF', cursor: 'pointer' },
                                                           on: {
                                                               click: () => {
                                                                   window.open(`${window.location.origin}/console/permission/my-handover?flowNo=${res}&type=handoverFromMe`, '_blank')
                                                               }
                                                           }
                                                       }, this.$t('我的交接')),
                                                       h('span', this.$t('”中查看进度。'))
                                                   ])
                                               ]
                                             )
                                         ]),
                            confirmFn: () => {
                                this.projectList = this.projectList.filter(item => item.englishName !== this.projectId)
                                window.open(`${window.location.origin}/console/permission/my-handover?flowNo=${res}&type=handoverFromMe`, '_blank')
                            },
                            cancelFn: () => {
                                this.projectList = this.projectList.filter(item => item.englishName !== this.projectId)
                            }
                        })
                    }
                } catch (error) {
                    this.$bkMessage({
                        message: error.message || error.content,
                        theme: 'error'
                    })
                } finally {
                    this.quitLoading = false
                }
            },

            async updateExitProject (projectId) {
                try {
                    const res = await this.checkMemberExitsProject({ projectId })
                    const {
                        uniqueManagerCount = 0,
                        pipelineAuthorizationCount = 0,
                        repositoryAuthorizationCount = 0,
                        envNodeAuthorizationCount = 0
                    } = res

                    this.exitProject = {
                        ...res,
                        transferNeededNum: uniqueManagerCount + pipelineAuthorizationCount + repositoryAuthorizationCount + envNodeAuthorizationCount
                    }
                } catch (error) {
                    console.log('error', error)
                }
            },

            async handleQuitClick (row) {
                this.targetMember = await authInfo.requestCurrentUser()
                this.projectId = row.englishName
                this.projectName = row.projectName
  
                await this.updateExitProject(this.projectId)

                const {
                    departmentJoinedCount = 0,
                    departments,
                    managers,
                    transferNeededNum
                } = this.exitProject

                if (departmentJoinedCount > 0) {
                    this.unableToExit(departments, managers)
                } else if (transferNeededNum > 0) {
                    this.showDialog = true
                } else {
                    this.normalExit(this.projectName)
                }
            },

            async handleRefresh () {
                try {
                    this.isMainLoading = true
                    await this.updateExitProject(this.projectId)
                    if (!this.exitProject.departmentJoinedCount && !this.exitProject.transferNeededNum) {
                        this.showDialog = false
                        this.normalExit(this.projectName)
                    }
                } catch (error) {
                    console.error('error', error)
                } finally {
                    this.isMainLoading = false
                }
            },

            goToPermission (key) {
                if (key === 'uniqueManager') {
                    window.open(`${window.location.origin}/console/permission/my-permission?projectCode=${this.projectId}&uniqueManagerGroupsQueryFlag=true`, '_blank')
                } else {
                    window.open(`${window.location.origin}/console/permission/auth/${key}?projectCode=${this.projectId}`, '_blank')
                }
            },

            handleClosed () {
                this.showDialog = false
                this.handleClearOverFormName()
            },

            getHandOverForm () {
                return {
                    id: '',
                    name: '',
                    type: ''
                }
            },

            handleChangeOverFormName ({ list, userList }) {
                if (!list) {
                    Object.assign(this.handOverForm, this.getHandOverForm())
                    return
                }
                const val = list[0]
                this.handOverForm = userList.find(i => i.id === val)
            },

            handleClearOverFormName () {
                Object.assign(this.handOverForm, this.getHandOverForm())
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
                        window.location.href = `/console/perm/my-project?project_code=${projectCode}`
                        break
                    case 'v3':
                        window.location.href = `/console/ps/${projectCode}/${relationId}/member?x-devops-project-id=${projectCode}`
                        break
                    case 'rbac':
                        window.location.href = `/console/manage/${projectCode}/show?x-devops-project-id=${projectCode}`
                        break
                }
            },
            handleChangeEnabled (row) {
                if ([1, 3, 4].includes(row.approvalStatus)) return
                const { englishName: projectCode, enabled, projectName, routerTag } = row
                this.toggleProjectEnable({
                    projectCode: projectCode,
                    enabled: !enabled
                }).then(async () => {
                    row.enabled = !row.enabled
                    this.$bkMessage({
                        message: row.enabled ? this.$t('启用项目成功') : this.$t('停用项目成功'),
                        theme: 'success'
                    })
                    this.fetchProjects()
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
            getProjectTag () {
                return 'rbac'
            },

            handleSortChange ({ prop, order }) {
                const sortType = PROJECT_SORT_FILED[prop] || ''
                const collation = ORDER_ENUM[order] || ''
                localStorage.setItem('projectSortType', sortType)
                localStorage.setItem('projectSortCollation', collation)
                this.$router.push({
                    ...this.$route,
                    query: {
                        ...this.$route.query,
                        sortType,
                        collation
                    }
                })
                this.fetchProjects({
                    sortType,
                    collation
                })
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
    .filter-operation {
        display: flex;
        margin-top: 20px;
        span {
            display: inline-block;
            height: 36px;
            line-height: 36px;
            padding: 0 20px;
            border: 1px solid #dfe0e5;
            border-bottom: none;
            cursor: pointer;
            &.is-selected {
                color: #3a84ff;
            }
            &:first-child {
                border-right: none;
            }
            &:hover {
                color: #3a84ff;
            }
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
    .enable-switcher {
        width: 26px;
        height: 16px;
        position: absolute;
        z-index: 200;
        opacity: 0;
        cursor: pointer;
    }
    .status-icon {
        width: 16px;
        margin-right: 5px;
    }
    .info-box {
        .bk-dialog-sub-header {
            padding: 0 32px 24px !important;
        }
        .info-content {
            margin-top: 6px;
            text-align: left;
            .info-project {
                font-size: 14px;
                margin-bottom: 16px;
                .label {
                    color: #4D4F56;
                }
                .value {
                    color: #313238;
                }
            }
            .info-text {
                font-size: 14px;
                color: #4D4F56;
                line-height: 22px;
            }
            .info-tips {
                width: 100%;
                padding: 12px 16px;
                font-size: 14px;
                line-height: 22px;
                background-color: #F5F6FA;
                border-radius: 2px;
                .reminder {
                    color: #FFB219;
                }
            }
        }
        .footer-none {
            margin-bottom: 18px;
        }
    }
    .dialog-header {
        margin-top: 18px;
        color: #313238;
        font-size: 20px;
    }
    .bk-dialog-outer {
        padding-bottom: 25px;
        .close-btn {
            margin-left: 8px;
        }
    }
    .project-content {
        margin: 24px 0 16px 0;
        .tips {
            display: flex;
            justify-content: space-between;
            align-items: center;
            font-size: 14px;
            color: #63656E;
            .tips-num {
                color: #FFB219;
            }
            .refresh {
                text-align: right;
                color: #3A84FF;
                font-size: 12px;
                cursor: pointer;
                width: 70px;
                flex-shrink: 0;
                white-space: nowrap;
            }
        }
        .service-list {
            margin-top: 14px;
            overflow-y: auto;
            &::-webkit-scrollbar-thumb {
                background-color: #c4c6cc !important;
                border-radius: 5px !important;
                &:hover {
                background-color: #979ba5 !important;
                }
            }
            &::-webkit-scrollbar {
                width: 8px !important;
                height: 8px !important;
            }
            li {
                display: flex;
                align-items: center;
                justify-content: space-between;
                padding: 0 16px;
                margin-bottom: 8px;
                background-color: #F0F1F5;
                border-radius: 2px;
                font-size: 12px;
                .item {
                    height: 32px;
                    line-height: 32px;
                    .item-name {
                        color: #313238;
                    }
                    .item-num {
                        background-color: #EAEBF0;
                        border-radius: 2px;
                        padding: 0 8px;
                        margin-left: 8px;
                    }
                }
                .go-detail {
                    color: #3A84FF;
                    cursor: pointer;
                    font-size: 12px;
                    svg {
                        vertical-align: middle;
                        margin-right: 6px;
                    }
                }

            }
        }
        img {
            vertical-align: middle;
        }
    }
    .handover-content {
        position: relative;
        padding-top: 16px;
        font-size: 12px;
        border-top: 1px solid #C4C6CC;
        .label-tip {
            color: #979BA5;
            margin-top: 4px;
        }
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
    .info-box {
        .bk-dialog-sub-header {
            padding: 0 32px 24px !important;
        }
    }
    .exit-form {
        .bk-label-text {
            font-size: 12px !important;
            color: #4D4F56;
        }
    }
    .exit-project-dialog {
        .bk-dialog-footer{
            background-color: #fff;
            border: none;
        }
    }
</style>

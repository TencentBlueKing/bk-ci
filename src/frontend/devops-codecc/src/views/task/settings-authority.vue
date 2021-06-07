<template>
    <div>
        <div v-if="taskDetail.createFrom === 'gongfeng_scan'">
            <div class="no-authority">
                <div class="desc">
                    {{$t('CodeCC开源扫描任务权限与工蜂仓库权限保持同步。')}}
                </div>
                <bk-button size="large" theme="primary" @click="hanldeToGongFeng()">{{$t('前往工蜂配置')}}</bk-button>
            </div>
        </div>
        <bk-tab v-else class="settings-authority-tab" :active.sync="active" type="unborder-card" @tab-change="changeTab">
            <div v-if="!pipelineCondition">
                <bk-tab-panel
                    v-for="(panel, index) in codeccPanels"
                    v-bind="panel"
                    :key="index">
                </bk-tab-panel>
            </div>
            <div v-if="pipelineCondition">
                <bk-tab-panel
                    v-for="(panel, index) in linePanels"
                    v-bind="panel"
                    :key="index">
                </bk-tab-panel>
            </div>
            <div v-if="pipelineCondition" class="header-tab-right">{{$t('权限与创建该任务的流水线保持一致')}}</div>
            <div>
                <div v-show="tabSelect === 'role'">
                    <bk-collapse class="collapse" v-for="(item, index) in roleList" :key="index" v-model="roleActive">
                        <bk-collapse-item :name="item.role_code">
                            <div>
                                <b class="item-title">{{$t(`${changeName(item.role_name)}`)}}</b>
                                <span v-if="item.role_code === 'manager'">{{$t('(执行检查，问题管理，查看问题，查看报表，任务设置)')}}</span>
                                <span v-else-if="item.role_code === 'viewer'">{{$t('(查看问题，查看报表)')}}</span>
                                <span v-else-if="item.role_code === 'member' || 'executor'">{{$t('(执行检查，问题管理，查看问题，查看报表)')}}</span>
                            </div>
                            <bk-form slot="content">
                                <bk-form-item :label-width="80" :label="$t('用户')">
                                    <bk-select v-model="item.user_list"
                                        multiple
                                        searchable>
                                        <bk-option v-for="users in userList" :key="users" :id="users" :name="users">
                                        </bk-option>
                                    </bk-select>
                                    <div class="setting-extra" v-if="item.extra_user_list.length">
                                        <bk-popover placement="bottom" :content="item.extra_user_list.join(',')">
                                            <p class="setting-extra-list">{{$t('附加：')}}{{item.extra_user_list.join(',')}}</p>
                                        </bk-popover>
                                        <bk-popover class="setting-extra-tip" placement="bottom-end" width="260" content="附加人员或组是拥有你项目下任意流水线相关权限的人员或组，由项目管理员授权，如需移除，请联系你的项目管理员。">
                                            <p>{{$t('为什么会有附加人员或组？')}}</p>
                                        </bk-popover>
                                    </div>
                                </bk-form-item>
                                <bk-form-item :label-width="80" :label="$t('用户组')">
                                    <bk-select v-model="item.group_list"
                                        multiple
                                        searchable>
                                        <bk-option v-for="groupName in userGroupList" :key="groupName" :id="groupName.groupRoleId" :name="groupName.groupName">
                                        </bk-option>
                                    </bk-select>
                                    <div class="setting-extra" v-if="item.extra_group_list.map(item => item.group_name).join().length">
                                        <bk-popover placement="bottom" :content="item.extra_group_list.map(item => item.group_name).join(',')">
                                            <p class="setting-extra-list">附加：{{item.extra_group_list.map(item => item.group_name).join(',')}}</p>
                                        </bk-popover>
                                    </div>
                                </bk-form-item>
                            </bk-form>
                        </bk-collapse-item>
                    </bk-collapse>
                    <bk-button theme="primary" class="mt10" :disabled="isDisabled" @click="saveUserSetting">{{$t('保存')}}</bk-button>
                </div>
                <div v-show="tabSelect === 'function'">
                    <bk-collapse class="collapse" v-for="item in policyList" :key="item" v-model="funcActive">
                        <bk-collapse-item :name="item.policy_code">
                            <div>
                                <b class="item-title">{{$t(`${item.policy_name}`)}}</b>
                                <span v-if="item.policy_code === 'analyze'">{{$t('(触发开始检查)')}}</span>
                                <span v-else-if="item.policy_code === 'defect_manage'">{{$t('(忽略问题、标记问题、修改处理人)')}}</span>
                                <span v-else-if="item.policy_code === 'defect_view'">{{$t('(查看问题列表、查看代码片段)')}}</span>
                                <span v-else-if="item.policy_code === 'report_view'">{{$t('(查看数据报表)')}}</span>
                                <span v-else-if="item.policy_code === 'task_manage'">{{$t('(基础信息修改、规则集配置、通知报告、扫描触发、路径屏蔽、人员权限、任务管理)')}}</span>
                            </div>
                            <bk-form slot="content">
                                <bk-form-item :label-width="80" :label="$t('用户')">
                                    <bk-select v-model="item.user_list"
                                        multiple
                                        searchable>
                                        <bk-option v-for="users in userList" :key="users" :id="users" :name="users">
                                        </bk-option>
                                    </bk-select>
                                    <div class="setting-extra" v-if="item.extra_user_list.length">
                                        <bk-popover placement="bottom" :content="item.extra_user_list.join(',')">
                                            <p class="setting-extra-list">{{$t('附加：')}}{{item.extra_user_list.join(',')}}</p>
                                        </bk-popover>
                                        <bk-popover class="setting-extra-tip" placement="bottom-end" width="260" content="附加人员或组是拥有你项目下任意流水线相关权限的人员或组，由项目管理员授权，如需移除，请联系你的项目管理员。">
                                            <p>{{$t('为什么会有附加人员或组？')}}</p>
                                        </bk-popover>
                                    </div>
                                </bk-form-item>
                                <bk-form-item :label-width="80" :label="$t('用户组')">
                                    <bk-select v-model="item.group_list"
                                        searchable
                                        multiple>
                                        <bk-option v-for="groupName in userGroupList" :key="groupName" :id="groupName.groupRoleId" :name="groupName.groupName">
                                        </bk-option>
                                    </bk-select>
                                    <div class="setting-extra" v-if="item.extra_group_list.map(item => item.group_name).join().length">
                                        <bk-popover placement="bottom" :content="item.extra_group_list.map(item => item.group_name).join(',')">
                                            <p class="setting-extra-list">{{$t('附加：')}}{{item.extra_group_list.map(item => item.group_name).join(',')}}</p>
                                        </bk-popover>
                                    </div>
                                </bk-form-item>
                            </bk-form>
                        </bk-collapse-item>
                    </bk-collapse>
                    <bk-button theme="primary" class="mt10" :disabled="isDisabled" @click="saveUserSetting">{{$t('保存')}}</bk-button>
                </div>
            </div>
        </bk-tab>
    </div>
    
</template>
<script>
    import axios from 'axios'
    import cookie from 'cookie'
    import { mapState } from 'vuex'

    export default {
        data () {
            return {
                codeccPanels: [
                    { name: 'role', label: this.$t('按角色') },
                    { name: 'function', label: this.$t('按功能') }
                ],
                linePanels: [
                    { name: 'role', label: this.$t('按角色') }
                ],
                active: 'role',
                tabSelect: 'role',
                funcActive: 'analyze',
                params: {},
                roleList: [],
                policyList: [],
                authList: [],
                userList: [],
                userGroupList: [],
                pipelineLise: []
            }
        },
        computed: {
            ...mapState('task', {
                taskDetail: 'detail',
                status: 'status',
                codes: 'codes'
            }),
            taskId () {
                return this.$route.params.taskId
            },
            projectId () {
                return this.$route.params.projectId
            },
            pipelineId () {
                return this.taskDetail.pipelineId
            },
            pipelineCondition () {
                return this.taskDetail.createFrom === 'bs_pipeline'
            },
            isDisabled () {
                return this.status.gongfengProjectId
            },
            roleActive () {
                if (this.pipelineCondition) {
                    return ['manager', 'viewer', 'executor']
                }
                return ['manager', 'viewer', 'member']
            }
        },
        created () {
            if (this.taskDetail.createFrom !== 'gongfeng_scan') {
                this.init()
            }
        },
        methods: {
            changeTab (name) {
                this.tabSelect = name
            },
            init () {
                this.$store.dispatch('task/getCodeMessage')
                this.getCodeccList()
            },
            getCodeccList () {
                axios
                    .get(`${window.DEVOPS_API_URL}/project/api/user/projects/${this.projectId}`,
                         { withCredentials: true,
                           headers:
                               { 'X-DEVOPS-PROJECT-ID': this.projectId }
                         })
                    .then(res => {
                        this.params = this.pipelineCondition
                            ? { 'projectId': res.data.data.projectId, 'pipelineId': this.pipelineId }
                            : { 'projectId': res.data.data.projectId, 'taskId': this.taskId }
                        this.$store.commit('setMainContentLoading', true)
                    }).finally(() => {
                        this.pipelineCondition ? this.getPipeLineAuth() : this.getCodeccAuth()
                        this.getUserList()
                        this.getGroupName()
                    })
            },
            getCodeccAuth () {
                axios
                    .get(`${window.DEVOPS_API_URL}/backend/api/perm/service/codecc/mgr_resource/permission/?project_id=${this.params.projectId}&resource_type_code=task&resource_code=${this.params.taskId}`,
                         { withCredentials: true
                         })
                    .then(res => {
                        this.authList = res.data
                        // 在刚取值的时候对用户组数据进行处理，用数值表示用户组组名
                        this.roleList = res.data.data.role
                        this.roleList.map((role) => {
                            role.group_list = role.group_list.map(grouplist => grouplist.group_id)
                        })
                        this.policyList = res.data.data.policy
                        this.policyList.map((policy) => {
                            policy.group_list = policy.group_list.map(grouplist => grouplist.group_id)
                        })
                    }).finally(() => {
                        this.$store.commit('setMainContentLoading', false)
                    })
            },
            getUserList () {
                axios
                    .get(`${window.DEVOPS_API_URL}/project/api/user/users/projects/${this.projectId}/list`,
                         { withCredentials: true
                         })
                    .then(res => {
                        this.userList = res.data.data
                    })
            },
            getGroupName () {
                axios
                    .get(`${window.DEVOPS_API_URL}/experience/api/user/groups/${this.projectId}/projectGroupAndUsers`,
                         { withCredentials: true
                         })
                    .then(res => {
                        this.userGroupList = res.data.data
                    })
            },
            getPipeLineAuth () {
                axios
                    .get(`${window.DEVOPS_API_URL}/backend/api/perm/service/pipeline/mgr_resource/permission/?project_id=${this.params.projectId}&resource_type_code=pipeline&resource_code=${this.pipelineId}`,
                         { withCredentials: true
                         })
                    .then(res => {
                        this.pipelineLise = res.data
                        this.roleList = res.data.data.role
                        // 前端调整一下流水线role权限数据顺序，和codecc权限样式保持一致
                        if (this.roleList) {
                            [this.roleList[0], this.roleList[1]] = this.roleList[0].role_code === 'executor' ? [this.roleList[1], this.roleList[0]] : [this.roleList[0], this.roleList[1]]
                        }
                        this.roleList.map((role) => {
                            role.group_list = role.group_list.map(grouplist => grouplist.group_id)
                        })
                    }).finally(() => {
                        this.$store.commit('setMainContentLoading', false)
                    })
            },
            saveUserSetting () {
                const data = this.pipelineCondition
                    ? {
                        project_id: this.params.projectId,
                        resource_type_code: 'pipeline',
                        resource_code: this.pipelineId,
                        role: this.roleList
                    }
                    : {
                        project_id: this.params.projectId,
                        resource_type_code: 'task',
                        resource_code: this.params.taskId,
                        role: this.roleList,
                        policy: this.policyList
                    }
                this.isDisabled = true
                try {
                    const url = this.pipelineCondition
                        ? `${window.DEVOPS_API_URL}/backend/api/perm/service/pipeline/mgr_resource/permission/`
                        : `${window.DEVOPS_API_URL}/backend/api/perm/service/codecc/mgr_resource/permission/`
                    axios.put(url, data, { withCredentials: true, headers: { 'X-CSRFToken': cookie.parse(document.cookie).backend_csrftoken } })
                        .then(res => {
                            if (res.data && res.data.code === 0) {
                                this.$bkMessage({
                                    message: '保存权限成功',
                                    theme: 'success'
                                })
                            } else {
                                this.$bkMessage({
                                    message: res.data.message || '保存权限失败',
                                    theme: 'error'
                                })
                            }
                        })
                    this.handleUpdateMembers(data)
                } catch (err) {
                    this.$showTips({
                        message: err.message || err,
                        theme: 'error'
                    })
                }
                this.isDisabled = false
            },
            changeName (name) {
                let roleName = name
                switch (name) {
                    case '成员':
                        roleName = '开发人员'
                        break
                    case '拥有者':
                        roleName = '管理员'
                        break
                    case '执行者':
                        roleName = '开发人员'
                        break
                    case '查看者':
                        roleName = '关注者'
                        break
                    default:
                        break
                }
                return roleName
            },
            handleUpdateMembers (data) {
                const userGroupList = this.userGroupList
                let taskOwner = []
                let taskMember = [];
                ['policy', 'role'].map(item => {
                    const list = data[item] || []
                    list.map(user => {
                        const { user_list, extra_user_list, group_list, extra_group_list } = user
                        const groupList = handleGroupMembers(group_list)
                        const extraGroupList = handleGroupMembers(extra_group_list)
                        if (user.role_code === 'manager') {
                            taskOwner = Array.from(new Set(taskOwner.concat(user_list).concat(extra_user_list).concat(groupList).concat(extraGroupList)))
                        } else {
                            taskMember = Array.from(new Set(taskMember.concat(user_list).concat(extra_user_list).concat(groupList).concat(extraGroupList)))
                        }
                    })
                })
                function handleGroupMembers (group = []) {
                    return group.reduce((pre, cur) => {
                        const id = cur.group_id || cur
                        return pre.concat(userGroupList.find(user => user.groupRoleId === id).users || [])
                    }, [])
                }
                this.$store.dispatch('task/updateMembers', {
                    taskId: this.taskId,
                    taskOwner,
                    taskMember
                })
            },
            hanldeToGongFeng () {
                window.open(`${this.codes.repoUrl}/-/project_members`, '_blank')
            }
        }
    }
</script>

<style lang="postcss" scoped>
    @import '../../css/variable.css';
    .settings-authority-tab {
        margin: -17px -5px;
        border-top: none;
    }
    .header-tab-right {
        position: relative;
        top: -50px;
        text-align: right;
        margin-bottom: -16px;
        padding-right: 15px;
        font-size: 12px;
        color: $fontLightColor;
    }
    .bk-collapse {
        :hover {
            color: #63656e;
        }
        .bk-collapse-item {
            padding-bottom: 10px;
            >>>.bk-collapse-item-header {
                border: 1px solid $borderColor;
                background: $bgLightColor;
                font-size: 12px;
                .item-title {
                    font-weight: 700;
                    margin-right: 15px;
                }
            }
            >>>.bk-collapse-item-content {
                border: 1px solid $borderColor;
                border-top: none;
                font-size: 12px;
                padding: 20px 15px 15px 15px;
                .setting-extra {
                    margin: 5px 0px 0px 0px;
                    cursor: default;
                    .setting-extra-tip {
                        color: #3c96ff;
                        position: absolute;
                        right: 0;
                        :hover {
                            color: #3c96ff;
                        }
                    }
                }
            }
        }
    }

    .no-authority {
        padding-top: 150px;
        text-align: center;

        .desc {
            font-size: 14px;
            margin: 8px 0;
        }
    }
    
</style>

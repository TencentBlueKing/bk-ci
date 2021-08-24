<template>
    <article class="setting-member-home">
        <ul class="member-group-home" v-bkloading="{ isLoading: isLoadingRole }">
            <li :class="{ 'member-group-item': true, active: curRole.name === role.name }"
                :key="role.id"
                v-for="role in roleList"
                @click="changeRole(role)"
            >
                <bk-input v-if="role.isEdit"
                    v-model="role.name"
                    v-focus
                    :placeholder="$t('permission.editPlaceHolder')"
                    left-icon="icon-id"
                    @enter="editGroup(false, role)"
                    @blur="toggleShowEditGroup(role)"
                ></bk-input>
                <template v-else>
                    <span class="role-name">{{ role.name }}</span>
                    <bk-dropdown-menu align="right" trigger="click" class="group-operate hover-show" v-if="!role.defaultRole">
                        <template slot="dropdown-trigger">
                            <span class="dropdown-trigger-btn bk-icon icon-cog-shape"></span>
                        </template>
                        <ul class="bk-dropdown-list" slot="dropdown-content">
                            <li :class="{ disabled: !hasPermission }" v-bk-tooltips="permissionTip">
                                <a href="javascript:;" @click.stop="toggleShowEditGroup(role)">{{ $t('rename') }}</a>
                            </li>
                            <li :class="{ disabled: !hasPermission }" v-bk-tooltips="permissionTip">
                                <a href="javascript:;" @click.stop="showDelete(role)">{{ $t('delete') }}</a>
                            </li>
                        </ul>
                    </bk-dropdown-menu>
                </template>
            </li>
            <li class="member-group-item add-group">
                <span v-if="!isAddGroup" @click="toggleShowAddGroup(true)" :class="{ disabled: !hasPermission }" v-bk-tooltips="permissionTip">
                    {{ $t('permission.addRole') }}
                </span>
                <bk-input v-else
                    v-focus
                    :placeholder="$t('permission.editPlaceHolder')"
                    left-icon="icon-id"
                    @blur="toggleShowAddGroup(false)"
                    @enter="addGroup(false, arguments[0])"
                ></bk-input>
            </li>
        </ul>

        <main class="member-home-user">
            <header class="user-header">
                <section class="opt-wrapper">
                    <div v-bk-tooltips="permissionTip" v-if="curRole.id !== 'bkAllMember'" class="mr15">
                        <bk-button @click="bulkDeleteGroupUser"
                            :loading="isGroupUserDeleting"
                            :disabled="selectedUserList.length <= 0 || !hasPermission"
                        >{{ $t('permission.removeUser') }}</bk-button>
                    </div>
                    <div v-bk-tooltips="permissionTip" v-if="curRole.id !== 'bkAllMember'">
                        <bk-button theme="primary"
                            @click="memberVisible = true"
                            :disabled="!hasPermission"
                        >{{ $t('permission.addUser') }}</bk-button>
                    </div>
                </section>
                <bk-input v-model="searchUser" class="user-search" :placeholder="$t('permission.searchUser')" right-icon="bk-icon icon-search" clearable></bk-input>
            </header>

            <bk-table :data="displayMember"
                :outer-border="false"
                :header-border="false"
                :header-cell-style="{ background: '#fff' }"
                :pagination="pagination"
                v-bkloading="{ isLoading: isLoadingMember }"
                @selection-change="chooseUser"
                @page-change="pageChange"
                @page-limit-change="pageLimitChange">
                <bk-table-column type="selection" width="60"></bk-table-column>
                <bk-table-column :label="$t('permission.userName')">
                    <template slot-scope="props">
                        {{props.row.id}}({{props.row.name}})
                    </template>
                </bk-table-column>
                <bk-table-column :label="$t('permission.userType')" prop="type" :formatter="typeFormatter"></bk-table-column>
            </bk-table>
        </main>

        <add-member :show.sync="memberVisible" :role="curRole" :add-group="addGroup" @fresh="getMemberList"></add-member>

        <bk-dialog v-model="deleteObj.show"
            :confirm-fn="requestDelete"
            :loading="deleteObj.isLoading"
            :title="$t('permission.ensureDelete')"
        >
            {{ $t('permission.confirmDelete', [deleteObj.name]) }}
        </bk-dialog>
    </article>
</template>

<script>
    import { mapActions } from 'vuex'
    import addMember from './add-member'

    export default {
        components: {
            addMember
        },

        directives: {
            focus: {
                inserted (el) {
                    el.children[1].firstChild.focus()
                },
                update (el, binding) {
                    binding.value && el.children[1].firstChild.focus()
                }
            }
        },

        data () {
            return {
                hasPermission: false,
                userList: [],
                roleList: [],
                curRole: {},
                pagination: {
                    current: 1,
                    count: 0,
                    limit: 10
                },
                deleteObj: {
                    show: false,
                    id: '',
                    name: '',
                    isLoading: false
                },
                isAddGroup: false,
                memberVisible: false,
                isGroupUserDeleting: false,
                selectedUserList: [],
                isLoadingMember: false,
                isLoadingRole: false,
                searchUser: ''
            }
        },

        computed: {
            projectId () {
                return this.$route.params.iamId
            },

            projectCode () {
                return this.$route.params.projectId
            },

            curProject () {
                const projectList = window.getLsCacheItem('projectList') || []
                const curProject = projectList.find((project) => (project.projectCode === this.projectCode))
                return curProject
            },

            computedMember () {
                const userList = this.userList.filter(user => user.name.includes(this.searchUser))
                this.pagination.count = userList.length
                return userList
            },

            displayMember () {
                const startIndex = (this.pagination.current - 1) * this.pagination.limit
                const endIndex = startIndex + this.pagination.limit
                return this.computedMember.slice(startIndex, endIndex)
            },

            permissionTip () {
                return {
                    content: this.$t('permission.noPermissionTip'),
                    disabled: this.hasPermission
                }
            }
        },

        created () {
            this.initData()
        },

        methods: {
            ...mapActions([
                'getProjectDefaultRole',
                'getProjectRoles',
                'getProjectRoleUsers',
                'addMemberGroup',
                'deleteMemberGroup',
                'editMemberGroup',
                'deleteGroupMember',
                'getAllMember',
                'hasProjectPermission'
            ]),

            initData () {
                Promise.all([this.getRoleList(), this.projectPermission()]).then(() => {
                    const firstRole = this.roleList[1] || {}
                    this.changeRole(firstRole)
                })
            },

            projectPermission () {
                return this.hasProjectPermission(this.projectId).then((res) => {
                    this.hasPermission = res
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                })
            },

            getRoleList () {
                this.isLoadingRole = true
                return Promise.all([this.getProjectRoles(this.projectId), this.getProjectDefaultRole()]).then(([roles, defaultRoles]) => {
                    const roleList = [{ id: 'bkAllMember', name: this.$t('permission.allMember'), defaultRole: true }]
                    defaultRoles.forEach((defaultRole) => {
                        defaultRole.defaultRole = true
                        const exitRoleIndex = roles.findIndex((role) => (role.code === defaultRole.code))
                        if (exitRoleIndex > -1) {
                            const role = roles.splice(exitRoleIndex, 1) || []
                            roleList.push(...role)
                        } else {
                            roleList.push(defaultRole)
                        }
                    })
                    roleList.push(...roles)
                    this.roleList = roleList
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isLoadingRole = false
                })
            },

            changeRole (role) {
                if (role.name === this.curRole.name) return

                this.curRole = role
                this.getMemberList()
            },

            getMemberList () {
                this.userList = []
                if (this.curRole.id === undefined) return

                const postData = {
                    projectId: this.projectId,
                    roleId: this.curRole.id
                }
                this.isLoadingMember = true
                const memberMethod = this.curRole.id === 'bkAllMember' ? this.getAllMember : this.getProjectRoleUsers
                memberMethod(postData).then((res) => {
                    this.userList = res.results || []
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isLoadingMember = false
                })
            },

            pageChange (page) {
                this.pagination.current = page
            },

            pageLimitChange (pageSize) {
                this.pagination.limit = pageSize
            },

            toggleShowEditGroup (role) {
                if (!this.hasPermission) return

                this.$set(role, 'isEdit', !role.isEdit)
            },

            editGroup (defaultGroup, role) {
                const postData = {
                    projectId: this.projectId,
                    roleId: role.id,
                    body: {
                        name: role.name,
                        code: role.name,
                        defaultGroup,
                        projectName: this.curProject.projectName,
                        type: ''
                    }
                }
                this.editMemberGroup(postData).then(() => {
                    this.toggleShowEditGroup(role)
                    this.getRoleList()
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                })
            },

            toggleShowAddGroup (isShow) {
                if (!this.hasPermission) return

                this.isAddGroup = isShow
            },

            addGroup (defaultGroup, name, code, type = '') {
                const postData = {
                    projectCode: this.projectCode,
                    projectId: this.projectId,
                    body: {
                        name,
                        code: code || name,
                        defaultGroup,
                        projectName: this.curProject.projectName,
                        type
                    }
                }
                return this.addMemberGroup(postData).then((res) => {
                    this.toggleShowAddGroup(false)
                    this.getRoleList()
                    return res
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                })
            },

            showDelete (role) {
                if (!this.hasPermission) return

                Object.assign(this.deleteObj, role)
                this.deleteObj.show = true
            },

            requestDelete () {
                const deleteData = {
                    projectId: this.projectId,
                    roleId: this.deleteObj.id
                }
                this.deleteObj.isLoading = true
                this.deleteMemberGroup(deleteData).then(() => {
                    this.deleteObj.show = false
                    this.getRoleList()
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.deleteObj.isLoading = false
                })
            },

            chooseUser (user) {
                this.selectedUserList = user
            },

            bulkDeleteGroupUser () {
                this.isGroupUserDeleting = true
                const requestDelete = (type) => {
                    const userList = this.selectedUserList.filter(x => x.type === type.toLowerCase())
                    if (userList.length <= 0) return Promise.resolve()

                    const id = userList.map(x => x.id).join(',')
                    const postData = {
                        projectId: this.projectId,
                        roleId: this.curRole.id,
                        id,
                        type,
                        isAdmin: this.curRole.code === 'manager'
                    }
                    return this.deleteGroupMember(postData)
                }
                Promise.all([requestDelete('USER'), requestDelete('DEPARTMENT')]).then(() => {
                    this.getMemberList()
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isGroupUserDeleting = false
                })
            },

            typeFormatter (row, column, cellValue, index) {
                const typeMap = {
                    user: this.$t('permission.user'),
                    department: this.$t('permission.department')
                }
                return typeMap[cellValue]
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '../../assets/scss/mixins/ellipsis';

    article.setting-member-home {
        display: flex;
        flex-direction: row;
        padding: 0;
    }
    .member-group-home {
        width: 200px;
        position: relative;
        height: 100%;
        overflow-y: auto;
        overflow-x: hidden;
        margin-right: 15px;
        background: #fafbfd;
        .member-group-item {
            width: 200px;
            cursor: pointer;
            padding: 0 20px;
            height: 60px;
            line-height: 60px;
            border-bottom: 1px solid #dde4eb;
            position: relative;
            &.active {
                background-color: #fff;
                color: #3c96ff;
                &:before {
                    width: 3px;
                    height: 100%;
                    background-color: #3c96ff;
                    content: "";
                    position: absolute;
                    top: 0;
                    left: 0;
                }
            }
            &:hover {
                .hover-show {
                    display: block;
                }
                .hover-hide {
                    display: none;
                }
            }
            &.add-group {
                color: #3c96ff;
                border: none;
            }
            .role-name {
                @include ellipsis(140px);
            }
            .hover-show {
                display: none;
                float: right;
            }
            .hover-hide {
                float: right;
            }
            .bk-badge {
                border-color: #fff !important;
            }
            .group-operate {
                line-height: 20px;
                margin-top: 20px;
                .icon-cog-shape {
                    font-size: 16px;
                    line-height: 20px;
                    margin-right: 3px;
                }
            }
        }
    }
    .member-home-user {
        flex: 1;
        height: 100%;
        overflow-y: auto;
        margin-right: 15px;
        .user-header {
            display: flex;
            justify-content: space-between;
            margin: 14px 0;
            .opt-wrapper {
                display: flex;
                align-items: center;
            }
        }
        .user-search {
            width: 200px;
        }
    }
    .disabled {
        a, a:hover {
            color: #c4c6cc !important;
            cursor: not-allowed;
            background-color: #fff !important;
        }
    }
</style>
<style lang="scss">
    .member-dialog .bk-dialog-body {
        padding-bottom: 0;
    }
    .select-dialog-main {
        display: flex;
        height: 400px;
        .member-title {
            font-weight: normal;
            color: #313238;
            font-size: 16px;
            line-height: 22px;
            padding: 10px 0;
            .member-clear {
                float: right;
                font-size: 12px;
                color: #3a84ff;
                cursor: pointer;
                margin-right: 5px;
            }
        }
        .bk-big-tree, .member-select-list {
            max-height: 371px;
            overflow: auto;
            &::-webkit-scrollbar-thumb {
                min-height: 24px;
                border-radius: 3px;
                background-color: #dcdee5;
            }
            &::-webkit-scrollbar {
                width: 6px;
                height: 6px;
            }
        }
        .project-setting-member {
            flex: 1;
            .node-options .node-icon.bk-icon {
                font-size: 16px;
            }
        }
        .select-dialog-show {
            background: #f5f6fa;
            border-left: 1px solid #e7e8ea;
            flex: 1;
            margin: 6px 0;
            padding: 0 14px;
            li {
                line-height: 32px;
                display: flex;
                align-items: center;
                justify-content: space-between;
                background: #fff;
                border-radius: 2px;
                font-size: 14px;
                height: 32px;
                line-height: 32px;
                margin-bottom: 2px;
                padding: 0 12px;
                .bk-icon {
                    margin-right: 5px;
                }
                .icon-close-line {
                    color: #3a84ff;
                    cursor: pointer;
                    display: none;
                }
                &:hover .icon-close-line {
                    display: inline-block;
                }
            }
        }
    }
</style>

<template>
    <bk-dialog
        v-model="isShowDialog"
        width="700"
        :title="$t('permission.chooseUser')"
        :mask-close="false"
        draggable
        header-position="left"
        ext-cls="add-member-dialog"
        @after-leave="handleAfterLeave">
        <div class="add-member-content-wrapper" v-bkloading="{ isLoading, opacity: 1 }" :style="style">
            <div v-show="!isLoading">
                <div class="left">
                    <div class="tab-wrapper">
                        <section
                            v-for="(item, index) in panels"
                            :key="item.name"
                            :class="['tab-item', { 'has-margin-left': index !== 0 }]"
                            @click.stop="handleTabChange(item)">
                            {{ item.label }}
                            <span class="active-line" v-if="tabActive === item.name"></span>
                        </section>
                    </div>
                    <bk-input v-if="isOrganization"
                        v-model="keyword"
                        clearable
                        left-icon="bk-icon icon-search"
                        class="member-input"
                        :placeholder="$t('permission.inputDeptPlaceholder')"
                    >
                    </bk-input>
                    <div class="member-tree-wrapper" v-if="isOrganization" v-bkloading="{ isLoading: isSearching }">
                        <div class="search-content" v-if="keyword">
                            <template v-if="hasSeachResult">
                                <bk-virtual-scroll
                                    style="height: 309px;"
                                    :item-height="30"
                                    :list="searchedDepartment"
                                >
                                    <template slot-scope="item">
                                        <section class="dom-virtual" @click="selectSearchDept(item.data)">
                                            <i class="bk-icon node-icon file-icon icon-folder-shape"></i>
                                            <span>{{item.data.name}}</span>
                                            <span class="node-checkbox"
                                                :class="{
                                                    'is-checked': item.data.is_selected
                                                }"
                                            >
                                            </span>
                                        </section>
                                    </template>
                                </bk-virtual-scroll>
                            </template>
                            <div class="search-empty-wrapper" v-else>
                                <bk-exception class="exception-wrap-item exception-part" type="empty" scene="part"></bk-exception>
                            </div>
                        </div>
                        <div class="tree" v-else>
                            <infinite-tree
                                ref="memberTreeRef"
                                :all-data="treeList"
                                style="height: 309px;"
                                :key="infiniteTreeKey"
                                @async-load-nodes="handleRemoteLoadNode"
                                @on-select="handleOnSelected">
                            </infinite-tree>
                        </div>
                    </div>
                    <div class="manual-wrapper" v-if="!isOrganization">
                        <bk-input
                            :placeholder="$t('permission.manualTip')"
                            type="textarea"
                            :rows="14"
                            v-model="manualValue"
                        >
                        </bk-input>
                        <bk-button
                            theme="primary"
                            style="width: 100%; margin-top: 35px;"
                            @click="handleAddManualUser">
                            {{ $t('permission.addToList') }}
                        </bk-button>
                    </div>
                </div>
                <div class="right">
                    <div class="header">
                        <div class="has-selected">
                            <template>
                                {{ $t('chosen') }}
                                <template v-if="isShowSelectedText">
                                    <span class="organization-count">{{ hasSelectedDepartments.length }}</span>{{ $t('permission.deptNum') }}
                                    <span class="user-count">{{ hasSelectedUsers.length }}</span>{{ $t('permission.userNum') }}
                                </template>
                                <template v-else>
                                    <span class="user-count">0</span>
                                </template>
                            </template>
                        </div>
                        <bk-button theme="primary" text :disabled="!isShowSelectedText" @click="handleDeleteAll">{{ $t('clear') }}</bk-button>
                    </div>
                    <div class="content">
                        <div class="organization-item" v-for="item in hasSelectedDepartments" :key="item.id">
                            <i class="bk-icon node-icon file-icon icon-folder-shape"></i>
                            <span class="organization-name" :title="item.name">{{ item.name }}</span>
                            <span class="user-count" v-if="item.showCount">{{ '(' + item.count + `)` }}</span>
                            <i class="bk-icon icon-close-circle-shape" @click="handleDelete(item, 'DEPARTMENT')"></i>
                        </div>
                        <div class="user-item" v-for="item in hasSelectedUsers" :key="item.id">
                            <i class="bk-icon icon-user-shape"></i>
                            <span class="user-name" :title="item.name">
                                {{ item.name }}
                            </span>
                            <i class="bk-icon icon-close-circle-shape" @click="handleDelete(item, 'USER')"></i>
                        </div>
                        <div class="selected-empty-wrapper" v-if="isSelectedEmpty">
                            <bk-exception class="exception-wrap-item exception-part" type="empty" scene="part"></bk-exception>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <div slot="footer">
            <bk-button theme="primary" :disabled="isDisabled" @click="handleSave" :loading="isSaving">{{ $t('okLabel') }}</bk-button>
            <bk-button style="margin-left: 10px;" @click="handleCancel">{{ $t('cancelLabel') }}</bk-button>
        </div>
    </bk-dialog>
</template>
<script>
    import { mapActions } from 'vuex'
    import InfiniteTree from './infinite-tree'

    export default {
        components: {
            InfiniteTree
        },

        props: {
            show: {
                type: Boolean,
                default: false
            },
            disabled: {
                type: Boolean,
                default: false
            },
            role: {
                type: Object,
                default: () => ({})
            },
            addGroup: {
                type: Function
            }
        },

        data () {
            return {
                isShowDialog: false,
                keyword: '',
                hasSelectedUsers: [],
                searchedDepartment: [],
                hasSelectedDepartments: [],
                treeList: [],
                infiniteTreeKey: -1,
                panels: [
                    { name: 'organization', label: this.$t('permission.chooseDepartment') },
                    { name: 'manual', label: this.$t('permission.inputUser') }
                ],
                tabActive: 'organization',
                manualValue: '',
                isSaving: false,
                isSearching: false,
                isLoading: false
            }
        },

        computed: {
            isDisabled () {
                return this.isLoading || (this.hasSelectedUsers.length < 1 && this.hasSelectedDepartments.length < 1)
            },
            hasSeachResult () {
                return this.searchedDepartment.length > 0
            },
            isShowSelectedText () {
                return this.hasSelectedDepartments.length > 0 || this.hasSelectedUsers.length > 0
            },
            isSelectedEmpty () {
                return this.hasSelectedDepartments.length < 1 && this.hasSelectedUsers.length < 1
            },
            isOrganization () {
                return this.tabActive === 'organization'
            },
            style () {
                return {
                    height: '383px'
                }
            }
        },

        watch: {
            show: {
                handler (value) {
                    this.isShowDialog = !!value
                    if (this.isShowDialog) {
                        this.infiniteTreeKey = new Date().getTime()
                        this.hasSelectedUsers = []
                        this.hasSelectedDepartments = []
                        this.getTopLevelDepts()
                    }
                },
                immediate: true
            },
            keyword (val) {
                if (val) {
                    this.infiniteTreeKey = new Date().getTime()
                    this.searchDepts(val)
                } else {
                    this.clearSearch()
                }
            }
        },

        methods: {
            ...mapActions([
                'getDeptsByLevel',
                'getDeptByParentId',
                'getDeptByName',
                'addRoleMembers'
            ]),

            handleTabChange ({ name }) {
                this.tabActive = name
            },

            getTopLevelDepts () {
                this.isLoading = true
                this.getDeptsByLevel(0).then((res = {}) => {
                    this.treeList = (res.results || []).map((item) => {
                        item.visiable = true
                        item.level = 0
                        item.loading = false
                        item.showRadio = true
                        item.selected = false
                        item.expanded = false
                        item.disabled = false
                        item.type = 'DEPARTMENT'
                        item.async = item.has_children
                        item.is_selected = this.hasSelectedDepartments.map(item => item.id).includes(item.id)
                        return item
                    })
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isLoading = false
                })
            },

            handleRemoteLoadNode (payload) {
                payload.loading = true
                this.getDeptByParentId(payload.id).then((res) => {
                    const depts = res.results || []
                    depts.forEach((child) => {
                        child.visiable = payload.expanded
                        child.level = payload.level + 1
                        child.loading = false
                        child.showRadio = true
                        child.selected = false
                        child.expanded = false
                        child.type = 'DEPARTMENT'
                        child.async = child.has_children
                        child.isNewMember = false
                        child.parentNodeId = payload.id
                        child.is_selected = this.hasSelectedDepartments.map(item => item.id).includes(child.id)
                    })

                    const curIndex = this.treeList.findIndex(item => item.id === payload.id)
                    if (curIndex === -1) return
                    this.treeList.splice(curIndex + 1, 0, ...depts)
                    if (!payload.children) {
                        payload.children = []
                    }
                    payload.children.splice(0, payload.children.length, ...depts)
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    payload.loading = false
                })
            },

            searchDepts (keyword) {
                window.clearTimeout(this.searchDepts.timeId)
                this.searchDepts.timeId = window.setTimeout(() => {
                    this.isSearching = true
                    const hasSelectedDepartmentIds = this.hasSelectedDepartments.map(item => item.id)
                    this.getDeptByName(keyword).then((res) => {
                        this.searchedDepartment = (res || []).map((item) => {
                            item.is_selected = hasSelectedDepartmentIds.includes(item.id)
                            return item
                        })
                    }).catch((err) => {
                        this.$bkMessage({ theme: 'error', message: err.message || err })
                    }).finally(() => {
                        this.isSearching = false
                    })
                }, 300)
            },

            clearSearch () {
                this.searchedDepartment = []
                const hasSelectedDepartmentIds = this.hasSelectedDepartments.map(item => item.id)
                this.treeList.forEach((item) => {
                    item.is_selected = hasSelectedDepartmentIds.includes(item.id)
                })
            },

            selectSearchDept (node) {
                node.is_selected = !node.is_selected
                this.handleOnSelected(node.is_selected, node)
            },

            handleOnSelected (newVal, node) {
                if (newVal) {
                    this.hasSelectedDepartments.push(node)
                } else {
                    this.hasSelectedDepartments = [...this.hasSelectedDepartments.filter(item => item.id !== node.id)]
                }
            },

            handleAddManualUser () {
                const hasSelectedUserIds = this.hasSelectedUsers.map(user => user.id)
                const users = (this.manualValue || '').split(';').filter((user) => {
                    return user && !hasSelectedUserIds.includes(user)
                }).map((user) => {
                    const filterUser = user.replace(/[^a-zA-Z_]/gi, '')
                    return { id: filterUser, name: filterUser, type: 'USER' }
                })
                this.hasSelectedUsers.push(...users)
            },

            handleDeleteAll () {
                if (this.searchedDepartment.length) {
                    this.searchedDepartment.forEach(organ => {
                        organ.is_selected = false
                    })
                }
                this.hasSelectedUsers.splice(0, this.hasSelectedUsers.length, ...[])
                this.hasSelectedDepartments.splice(0, this.hasSelectedDepartments.length, ...[])
                this.$refs.memberTreeRef && this.$refs.memberTreeRef.clearAllIsSelectedStatus()
            },

            handleDelete (item, type) {
                if (this.keyword) {
                    if (this.searchedDepartment.length) {
                        this.searchedDepartment.forEach(organ => {
                            if (organ.id === item.id) {
                                organ.is_selected = false
                            }
                        })
                    }
                } else if (this.$refs.memberTreeRef) {
                    this.$refs.memberTreeRef.setSingleSelectedStatus(item.id, false)
                }
                if (type === 'USER') {
                    this.hasSelectedUsers = [...this.hasSelectedUsers.filter(user => user.id !== item.id)]
                } else {
                    this.hasSelectedDepartments = [...this.hasSelectedDepartments.filter(organ => organ.id !== item.id)]
                }
            },

            handleAfterLeave () {
                this.keyword = ''
                this.manualValue = ''
                this.hasSelectedUsers.splice(0, this.hasSelectedUsers.length, ...[])
                this.hasSelectedDepartments.splice(0, this.hasSelectedDepartments.length, ...[])
                this.searchedDepartment.splice(0, this.searchedDepartment.length, ...[])
                this.treeList.splice(0, this.treeList.length, ...[])
                this.$refs.memberTreeRef && this.$refs.memberTreeRef.clearAllIsSelectedStatus()
                this.$emit('update:show', false)
                this.$emit('on-after-leave')
            },

            handleCancel () {
                this.$emit('update:show', false)
                this.$emit('on-cancel')
            },

            handleSave () {
                this.isSaving = true
                this.addRole().then((id) => {
                    if (id === undefined) return

                    const body = [...this.hasSelectedUsers, ...this.hasSelectedDepartments].map((item) => {
                        return {
                            type: item.type,
                            id: item.id
                        }
                    })
                    const postData = {
                        projectId: this.$route.params.iamId,
                        roleId: id,
                        isAdmin: this.role.code === 'manager',
                        body
                    }
                    return this.addRoleMembers(postData).then(() => {
                        this.$emit('fresh')
                        this.handleCancel()
                    })
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isSaving = false
                })
            },

            addRole () {
                return new Promise((resolve, reject) => {
                    if (this.role.id === undefined) {
                        this.addGroup(true, this.role.name, this.role.code, this.role.code).then(resolve, reject)
                    } else {
                        resolve(this.role.id)
                    }
                })
            }
        }
    }
</script>
<style lang='scss' scoped>
    .add-member-dialog {
        .title {
            line-height: 26px;
            color: #313238;
            .member-title {
                display: inline-block;
                max-width: 470px;
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
                vertical-align: top;
            }
            .expired-at-title {
                display: inline-block;
                max-width: 290px;
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
                vertical-align: top;
            }
        }
        .limit-wrapper {
            float: left;
            margin-top: 5px;
        }
        .node-icon {
            position: relative;
            font-size: 14px;
            margin: 0 2px;
            color: #a3c5fd;
        }
        .add-member-content-wrapper {
            height: 383px;
            .left {
                display: inline-block;
                width: 320px;
                height: 383px;
                border-right: 1px solid #dcdee5;
                float: left;
                .tab-wrapper {
                    position: relative;
                    top: -15px;
                    display: flex;
                    justify-content: flex-start;
                    height: 32px;
                    line-height: 32px;
                    border-bottom: 1px solid #d8d8d8;
                    .tab-item {
                        position: relative;
                        cursor: pointer;
                        &.has-margin-left {
                            margin-left: 20px;
                        }
                        .active-line {
                            position: absolute;
                            bottom: -1px;
                            left: 0;
                            width: 100%;
                            height: 2px;
                            background: #3a84ff;
                        }
                    }
                }
                .member-input {
                    width: 310px;
                    margin-bottom: 5px;
                }
                .member-tree-wrapper {
                    min-height: 309px;
                    position: relative;
                }
                .tree {
                    max-height: 309px;
                    overflow: auto;
                    &::-webkit-scrollbar {
                        width: 4px;
                        background-color: lighten(transparent, 80%);
                    }
                    &::-webkit-scrollbar-thumb {
                        height: 5px;
                        border-radius: 2px;
                        background-color: #e6e9ea;
                    }
                }
                .search-content {
                    .search-empty-wrapper {
                        position: absolute;
                        left: 50%;
                        top: 50%;
                        text-align: center;
                        transform: translate(-50%, -50%);
                        img {
                            width: 120px;
                        }
                        .empty-tips {
                            position: relative;
                            top: -20px;
                            font-size: 12px;
                            color: #dcdee5;
                        }
                    }
                }
                .manual-wrapper {
                    padding-right: 10px;
                    .manual-error-text {
                        position: absolute;
                        width: 320px;
                        line-height: 1;
                        margin-top: 4px;
                        font-size: 12px;
                        color: #ff4d4d;
                    }
                }
            }
            .right {
                display: inline-block;
                padding-left: 10px;
                width: 327px;
                height: 383px;
                .header {
                    display: flex;
                    justify-content: space-between;
                    position: relative;
                    top: 6px;
                    .organization-count {
                        margin-right: 3px;
                        color: #2dcb56;
                    }
                    .user-count {
                        margin-right: 3px;
                        color: #2dcb56
                    }
                }
                .content {
                    position: relative;
                    margin-top: 15px;
                    padding-left: 10px;
                    height: 345px;
                    overflow: auto;
                    &::-webkit-scrollbar {
                        width: 4px;
                        background-color: lighten(transparent, 80%);
                    }
                    &::-webkit-scrollbar-thumb {
                        height: 5px;
                        border-radius: 2px;
                        background-color: #e6e9ea;
                    }
                    .organization-item {
                        padding: 5px 0;
                        .organization-name {
                            display: inline-block;
                            max-width: 200px;
                            overflow: hidden;
                            text-overflow: ellipsis;
                            white-space: nowrap;
                            vertical-align: top;
                        }
                        .user-count {
                            color: #c4c6cc;
                        }
                    }
                    .folder-icon, .icon-user-shape {
                        font-size: 17px;
                        color: #a3c5fd;
                        margin: 0 2px;
                    }
                    .icon-close-circle-shape {
                        display: block;
                        margin: 4px 6px 0 0;
                        color: #c4c6cc;
                        cursor: pointer;
                        float: right;
                        &:hover {
                            color: #3a84ff;
                        }
                    }
                    .user-item {
                        padding: 5px 0;
                        .user-name {
                            display: inline-block;
                            max-width: 200px;
                            overflow: hidden;
                            text-overflow: ellipsis;
                            white-space: nowrap;
                            vertical-align: top;
                        }
                    }
                    .user-icon {
                        font-size: 16px;
                        color: #a3c5fd;
                    }
                    .selected-empty-wrapper {
                        position: absolute;
                        left: 50%;
                        top: 50%;
                        transform: translate(-50%, -50%);
                        img {
                            width: 120px;
                        }
                    }
                }
            }
        }
        .dom-virtual {
            padding: 5px 0;
            border-radius: 2px;
            cursor: pointer;
            .node-checkbox {
                display: inline-block;
                position: relative;
                top: 3px;
                width: 16px;
                height: 16px;
                margin: 0 6px 0 0;
                border: 1px solid #979ba5;
                border-radius: 50%;
                float: right;
                &.is-checked {
                    border-color: #3a84ff;
                    background-color: #3a84ff;
                    background-clip: border-box;
                    &:after {
                        content: "";
                        position: absolute;
                        top: 1px;
                        left: 4px;
                        width: 4px;
                        height: 8px;
                        border: 2px solid #fff;
                        border-left: 0;
                        border-top: 0;
                        transform-origin: center;
                        transform: rotate(45deg) scaleY(1);
                    }
                    &.is-disabled {
                        background-color: #dcdee5;
                    }
                }
                &.is-disabled {
                    border-color: #dcdee5;
                    cursor: not-allowed;
                }
                &.is-indeterminate {
                    border-width: 7px 4px;
                    border-color: #3a84ff;
                    background-color: #fff;
                    background-clip: content-box;
                    &:after {
                        visibility: hidden;
                    }
                }
            }
            &:hover {
                color: #3a84ff;
                background: #eef4ff;
            }
        }
    }
</style>

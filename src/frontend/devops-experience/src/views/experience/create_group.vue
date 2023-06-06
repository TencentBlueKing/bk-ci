<template>
    <bk-sideslider
        :title="title"
        :is-show.sync="visible"
        :quick-close="false"
        :width="640"
    >
        <bk-form
            v-bkloading="{ isLoading }"
            class="group-form-content"
            slot="content"
            form-type="vertical"
            :model="createGroupForm"
            ref="createGroupForm"
            :rules="groupRules"
        >
            <bk-form-item label="体验组名称" required property="name">
                <bk-input
                    placeholder="最长不超过10个汉字"
                    maxlength="10"
                    name="groupName"
                    v-model="createGroupForm.name"
                />
            </bk-form-item>
            <bk-form-item label="描述" property="remark">
                <bk-input
                    type="textarea"
                    placeholder="请输入"
                    max-length="100"
                    name="groupDesc"
                    v-model="createGroupForm.remark"
                />
            </bk-form-item>
            <bk-form-item label="人员">
                <section class="group-importer">
                    <bk-tab
                        :label-height="32"
                        :active.sync="activeTab"
                        @tab-change="handleTabChange"
                    >
                        <bk-tab-panel
                            :key="panel.name"
                            v-for="(panel) in panels"
                            v-bind="panel"
                        >
                        </bk-tab-panel>
                        <header class="group-importer-header">
                            <bk-select
                                :value="importType"
                                @selected="handleImportTypeSelected"
                                :loading="loadingGroup"
                                :clearable="false"
                            >
                                <bk-option v-for="option in importTypeList"
                                    :key="option.id"
                                    :id="option.id"
                                    :name="option.name"
                                />
                            </bk-select>
                            <component
                                :is="typeComponent"
                                v-bind="typeProps.props"
                                v-on="typeProps.listeners"
                            />
                            <bk-button
                                theme="primary"
                                outline
                                :disabled="adding"
                                :icon="adding ? 'loading' : ''"
                                @click="handlerAddUser"
                            >
                                添加
                            </bk-button>
                        </header>
                    </bk-tab>
                    <bk-table
                        ref="filterTable"
                        :data="userList"
                        :pagination="pagination"
                        @page-change="handlePageChange"
                        @page-limit-change="handlePageLimitChange"
                    >
                        <bk-table-column
                            label="人员名称"
                            prop="name"
                            sortable
                            show-overflow-tooltip
                            :filters="nameFilter"
                            :filter-method="filterMethod"
                            
                        />
                        <bk-table-column
                            label="内部/外部"
                            prop="typeLabel"
                            :filters="typeFilters"
                            :filter-method="typeFilterMethod"
                        />
                        <bk-table-column
                            label="所属组织架构"
                            prop="deptFullName"
                            show-overflow-tooltip
                            :filters="orgFilters"
                            :filter-method="filterMethod"
                        />
                        <bk-table-column
                            label="操作"
                        >
                            <template slot-scope="props">
                                <bk-button theme="primary" text @click="remove(props.row)">移除</bk-button>
                            </template>
                        </bk-table-column>
                    </bk-table>
                </section>
            </bk-form-item>
        </bk-form>
        
        <footer class="group-import-footer" slot="footer">
            <bk-button :disabled="isLoading || submitting" theme="primary" @click="handleSubmit">
                提交
            </bk-button>
            <bk-button :disabled="isLoading || submitting" @click="cancelFn">
                取消
            </bk-button>
        </footer>
    </bk-sideslider>
</template>

<script>
    import OrgnizationSelector from '@/components/OrgnizationSelector'
    import { mapActions, mapGetters } from 'vuex'
    const userTypeEnum = [
        'unknow',
        '内部人员',
        '外部人员',
        '内部组织'
    ]
    export default {
        components: {
            OrgnizationSelector
        },
        props: {
            visible: Boolean,
            isLoading: Boolean,
            createGroupForm: Object,
            errorHandler: Object,
            handleGroupFieldChange: Function,
            cancelFn: Function,
            title: String
        },
        data () {
            return {
                adding: false,
                userGroupList: [],
                externalUserList: [],
                activeTab: 'manual',
                importType: 1,
                loadingGroup: false,
                submitting: false,
                innerUsers: [],
                outerUsers: [],
                innerOrg: null,
                pagination: {
                    current: 1,
                    count: 0,
                    limit: 10
                }
            }
        },
        computed: {
            ...mapGetters('experience', [
                'getUserGroup'
            ]),
            projectId () {
                return this.$route.params.projectId
            },
            groupRules () {
                return {
                    name: [{
                        required: true,
                        message: '用户组名称不能为空',
                        trigger: 'blur'
                    }]
                }
            },
            panels () {
                return [{
                    name: 'manual',
                    label: '手动添加'
                }, {
                    name: 'import',
                    label: '从用户组导入'
                }]
            },
            manualOptions () {
                return userTypeEnum.map((item, index) => {
                    return {
                        id: index,
                        name: item,
                        hidden: index === 0
                    }
                }).filter(item => !item.hidden)
            },
            isManual () {
                return this.activeTab === 'manual'
            },
            importTypeList () {
                return this.isManual ? this.manualOptions : this.userGroupList
            },
            typeFilters () {
                return userTypeEnum.slice(1).map((item, index) => {
                    return {
                        text: item,
                        value: index
                    }
                })
            },
            orgFilters () {
                return Array.from(new Set(this.createGroupForm.members
                    .filter(item => item.deptFullName)
                    .map(item => item.deptFullName)))
                    .map(item => {
                        return {
                            text: item,
                            value: item
                        }
                    })
            },
            typeComponent () {
                switch (this.importType) {
                    case 3:
                        return 'orgnization-selector'
                    case 2:
                        return 'bk-select'
                    case 1:
                    default:
                        return 'bk-member-selector'
                }
            },
            typeProps () {
                switch (this.importType) {
                    case 3:
                        return {
                            props: {
                                value: this.innerOrg?.id
                            },
                            listeners: {
                                input: (org) => {
                                    this.innerOrg = org
                                    console.log(this.innerOrg)
                                }
                            }
                        }
                    case 2:
                        return {
                            props: {
                                list: this.externalUserList,
                                enableVirtualScroll: this.externalUserList.length,
                                multiple: true,
                                value: this.outerUsers
                            },
                            listeners: {
                                input: (value) => {
                                    this.outerUsers = value
                                }
                            }
                        }
                    case 1:
                    default:
                        return {
                            props: {
                                placeholder: '全公司人员有效',
                                disabled: !this.isManual,
                                value: this.innerUsers
                            },
                            listeners: {
                                input: (value) => {
                                    this.innerUsers = value
                                }
                            }
                        }
                }
            },
            userList () {
                return this.createGroupForm.members.map(item => ({
                    ...item,
                    typeLabel: userTypeEnum[item.type]
                }))
            },
            memberNames () {
                return this.createGroupForm.members.map(item => item.name)
            },
            nameFilter () {
                return this.memberNames.map(item => ({
                    text: item,
                    value: item
                }))
            },
            userSet () {
                return new Set(this.memberNames)
            }
        },
        watch: {
            'userList.length': function (len) {
                this.pagination.count = len
            },
            visible (val) {
                if (!val) { // 重置
                    this.activeTab = 'manual'
                    this.importType = 1
                    this.submitting = false
                    this.innerUsers = []
                    this.outerUsers = []
                    this.innerOrg = null
                    this.pagination = {
                        current: 1,
                        count: 0,
                        limit: 10
                    }
                }
            }
        },
        created () {
            this.fetchOuterUserList()
        },
        methods: {
            ...mapActions('experience', [
                'requestUserGroup',
                'updateselectUserGroup',
                'editUserGroups',
                'fetchOutersList',
                'requestUserOrg'
            ]),
            /**
             * 获取外部体验人员列表
             */
            async fetchOuterUserList () {
                try {
                    const res = await this.fetchOutersList({
                        projectId: this.projectId
                    })
                    this.externalUserList = res.map(item => {
                        return {
                            id: item.username,
                            name: item.username
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
            },
            async requestGroups () {
                try {
                    this.loadingGroup = true
                    const res = await this.requestUserGroup({
                        projectId: this.projectId
                    })

                    this.userGroupList = res.map(item => ({
                        id: item.groupId,
                        name: `${item.groupName} (${item.users?.length ?? 0})`,
                        ...item
                    }))
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                } finally {
                    this.loadingGroup = false
                }
            },
            handleTabChange (_) {
                if (!this.isManual) {
                    this.importType = 0
                    this.requestGroups()
                } else {
                    this.importType = 1
                }
                this.innerUsers = []
                this.outerUsers = []
            },
            handlePageLimitChange (limit) {
                this.pagination.limit = limit
            },
            handlePageChange (page) {
                this.pagination.current = page
            },
            handleImportTypeSelected (value) {
                const option = this.importTypeList.find(item => item.id === value)
                this.importType = value
                if (!this.isManual) {
                    this.innerUsers = Array.from(new Set([
                        ...this.innerUsers,
                        ...option.users
                    ]))
                    return
                }
                this.innerUsers = []
                this.outerUsers = []
            },
            async handlerAddUser () {
                switch (this.importType) {
                    case 3:
                        if (this.userSet.has(this.innerOrg?.name)) {
                            this.$bkMessage({
                                message: `内部组织${this.innerOrg.name}已存在`,
                                theme: 'error'
                            })
                            this.innerOrg = null
                            return
                        }
                        this.handleGroupFieldChange(
                            'members',
                            [
                                {
                                    name: this.innerOrg.name,
                                    id: this.innerOrg.id,
                                    type: 3,
                                    deptFullName: this.innerOrg.name
                                },
                                ...this.createGroupForm.members
                            ]
                        )
                        this.innerOrg = null
                        break
                    case 2: {
                        const list = this.outerUsers.filter(item => !this.userSet.has(item)).map(item => ({
                            name: item,
                            id: item,
                            type: 2,
                            deptFullName: '--'
                        }))
                        if (list.length > 0) {
                            this.outerUsers = []
                            this.handleGroupFieldChange(
                                'members',
                                [
                                    ...list,
                                    ...this.createGroupForm.members
                                ]
                            )
                        } else {
                            this.$bkMessage({
                                message: `外部体验人员${this.outerUsers.join(',')}已存在`,
                                theme: 'error'
                            })
                        }

                        break
                    }
                    
                    case 1:
                    default: {
                        try {
                            this.adding = true
                            const list = this.innerUsers.filter(item => !this.userSet.has(item))
                            if (list.length > 0) {
                                const res = await this.requestUserOrg({
                                    type: 1,
                                    names: list
                                })
                                this.handleGroupFieldChange(
                                    'members',
                                    [
                                        ...res.map((item) => ({
                                            ...item,
                                            id: item.name,
                                            type: 1
                                        })),
                                        ...this.createGroupForm.members
                                    ]
                                )
                            } else {
                                this.$bkMessage({
                                    message: `内部体验人员${this.innerUsers.join(',')}已存在`,
                                    theme: 'error'
                                })
                            }
                            this.innerUsers = []
                            !this.isManual && (this.importType = 0)
                        } catch (error) {
                            
                        } finally {
                            this.adding = false
                        }
                    }
                }
            },
            remove (row) {
                this.handleGroupFieldChange(
                    'members',
                    this.createGroupForm.members.filter(item => item.name !== row.name)
                )
            },
            async handleSubmit () {
                let message, theme
                try {
                    this.submitting = true
                    await this.$refs.createGroupForm.validate()

                    const {
                        createGroupForm,
                        editUserGroups
                    } = this
                    
                    await editUserGroups({
                        projectId: this.projectId,
                        params: createGroupForm
                    })

                    message = '保存成功'
                    theme = 'success'
                    this.$emit('after-submit')
                } catch (e) {
                    message = e.message || e.content
                    theme = 'error'
                } finally {
                    this.submitting = false
                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            clearSort () {
                this.$refs.filterTable.clearSort()
            },
            clearFilter () {
                this.$refs.filterTable.clearFilter()
            },
            filterMethod (value, row, column) {
                const property = column.property
                return row[property] === value
            },
            typeFilterMethod (value, row) {
                return row.type === value
            }
        }
    }
</script>

<style lang="scss">
    .group-form-content {
        padding: 24px;
        height: calc(100vh - 114px);
        .group-importer {
            height: 100%;
            background-color: #F5F7FA;
            border-radius: 2px;
            padding: 16px;
            .bk-tab {
                margin-bottom: 16px;
            }
            .bk-tab.bk-tab-border-card>.bk-tab-header {
                background-color: transparent;
                border: 0;
                background-image: none !important;
                .bk-tab-label-item {
                    background: #EAEBF0;
                    border: 0;
                    margin-right: 8px;
                    border-radius: 4px 4px 0 0;
                    &:active {
                        background: white;
                    }
                }
            }
            .bk-tab-section {
                border: 0;
                padding: 8px 16px;
                background-color: white;;
            }
            .group-importer-header {
                display: grid;
                grid-template-columns: minmax(120px, auto) minmax(300px, auto) minmax(min-content, auto);
                grid-gap: 10px;
                align-items: flex-start;
            }
        }
    }
    .group-import-footer {
        padding: 0 24px;
    }
</style>

<template>
    <bk-sideslider
        :title="title"
        :is-show.sync="visible"
        :quick-close="false"
        :before-close="beforeClose"
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
                    placeholder="最长不超过20个汉字"
                    maxlength="20"
                    name="groupName"
                    :input-style="inputStyle"
                    v-model.trim="createGroupForm.name"
                />
            </bk-form-item>
            <bk-form-item label="描述" property="remark">
                <bk-input
                    type="textarea"
                    placeholder="请输入"
                    :maxlength="200"
                    name="groupDesc"
                    v-model.trim="createGroupForm.remark"
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
                                placeholder="请选择"
                                :value="importType"
                                @selected="handleImportTypeSelected"
                                :loading="loadingGroup"
                                :clearable="false"
                            >
                                <bk-option v-for="option in importTypeList"
                                    :key="option.id"
                                    :id="option.id"
                                    :disabled="option.disabled"
                                    :name="option.name"
                                />
                            </bk-select>
                            <component
                                :is="typeComponent"
                                ref="inputComp"
                                v-bind="typeProps.props"
                                v-on="typeProps.listeners"
                            />
                            <bk-button
                                theme="primary"
                                outline
                                :disabled="adding || !hasUser"
                                :icon="adding ? 'loading' : ''"
                                @click="handleAddUser"
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
                        @filter-change="handleFilterChange"
                    >
                        
                        <bk-exception
                            v-if="Object.keys(filters).length > 0"
                            slot="empty"
                            type="search-empty"
                            scene="part"
                        >
                            <p class="memeber-search-empty-title">搜索结果为空</p>
                            <p class="memeber-search-empty-desc">
                                可以尝试
                                <span>调整关键词</span>
                                或
                                <bk-button
                                    text
                                    theme="primary"
                                    @click="clearFilter"
                                >
                                    清空筛选条件
                                </bk-button>
                            </p>
                        </bk-exception>
                        
                        <bk-table-column
                            label="名称"
                            prop="name"
                            column-key="name"
                            sortable
                            :key="nameFilter.length"
                            show-overflow-tooltip
                            filter-searchable
                            :filters="nameFilter"
                            
                        />
                        <bk-table-column
                            label="内部/外部"
                            prop="typeLabel"
                            column-key="type"
                            :filters="typeFilters"
                        />
                        <bk-table-column
                            label="所属组织架构"
                            prop="deptFullName"
                            column-key="deptFullName"
                            show-overflow-tooltip
                            filter-searchable
                            :filters="orgFilters"
                            
                        />
                        <bk-table-column
                            label="操作"
                            width="80"
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
            <bk-button :disabled="isLoading || submitting" @click="beforeClose">
                取消
            </bk-button>
        </footer>
    </bk-sideslider>
</template>

<script>
    import OrgnizationSelector from '@/components/OrgnizationSelector'
    import { mapActions, mapGetters } from 'vuex'

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
                filters: {},
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
                    count: this.createGroupForm.members.length,
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
            inputStyle () {
                return {
                    'box-shadow': '0 0 0 30px white inset !important'
                }
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
                return [
                    {
                        id: 1,
                        name: '内部人员'
                    },
                    {
                        id: 3,
                        name: '内部组织'
                    },
                    {
                        id: 2,
                        name: '外部人员'
                    }
                ]
            },
            isManual () {
                return this.activeTab === 'manual'
            },
            importTypeList () {
                return this.isManual ? this.manualOptions : this.userGroupList
            },
            typeFilters () {
                return this.manualOptions.map(item => {
                    return {
                        text: item.name,
                        value: item.id
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
            hasUser () {
                switch (this.importType) {
                    case 3:
                        return !!this.innerOrg
                    case 2:
                        return this.outerUsers.length > 0
                    case 1:
                    default:
                        return this.innerUsers.length > 0
                }
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
                                placeholder: !this.isManual ? '请从左侧选择已有用户组' : '请输入',
                                key: this.isManual,
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
            filterMembers () {
                const filterKeys = Object.keys(this.filters)
                if (filterKeys.length === 0) { // 没有过滤条件
                    return this.createGroupForm.members
                }
                console.log(this.filters, filterKeys)
                return this.createGroupForm.members.filter(item => {
                    return filterKeys.every(key => {
                        return this.filters[key].includes(item[key])
                    })
                })
            },
            userList () {
                const start = (this.pagination.current - 1) * this.pagination.limit
                return this.filterMembers.map(item => ({
                    ...item,
                    typeLabel: this.manualOptions.find(opt => opt.id === item.type)?.name ?? 'unknow'
                })).slice(start, start + this.pagination.limit)
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
            'filterMembers.length': function (len) {
                this.pagination.count = len
                this.pagination.current = 1
            },
            visible (val) {
                if (!val) { // 重置
                    this.activeTab = 'manual'
                    this.importType = 1
                    this.submitting = false
                    this.innerUsers = []
                    this.outerUsers = []
                    this.innerOrg = null
                    
                    this.filters = {}
                    this.clearFilter()
                    this.clearSort()
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
                        disabled: !item.users?.length,
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
                    this.importType = undefined
                    this.requestGroups()
                } else {
                    this.importType = 1
                }
                this.innerUsers = []
                this.outerUsers = []
                this.innerOrg = null
            },
            handlePageLimitChange (limit) {
                this.pagination.limit = limit
            },
            handlePageChange (page) {
                console.log(123, page, this.pagination)
                this.pagination.current = page
            },
            handleImportTypeSelected (value) {
                this.importType = value
                const option = this.importTypeList.find(item => item.id === value)
                if (!this.isManual) {
                    this.innerUsers = option.users
                    return
                }
                this.innerUsers = []
                this.outerUsers = []
            },
            async handleAddUser () {
                switch (this.importType) {
                    case 3: {
                        if (!this.innerOrg) return
                        if (this.userSet.has(this.innerOrg.name)) {
                            this.$bkMessage({
                                message: `内部组织${this.innerOrg.name}已存在`,
                                theme: 'error'
                            })
                            this.$refs.inputComp?.clear?.()
                            this.innerOrg = null
                            return
                        }
                        const fullName = this.getOrgFullName(this.innerOrg)
                        this.handleGroupFieldChange(
                            'members',
                            [
                                {
                                    name: this.innerOrg.name,
                                    id: this.innerOrg.id,
                                    type: 3,
                                    deptFullName: fullName
                                },
                                ...this.createGroupForm.members
                            ]
                        )
                        this.$refs.inputComp?.clear?.()
                        this.innerOrg = null
                        break
                    }
                        
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
                        } else if (this.outerUsers.length > 0) {
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
                            } else if (this.innerUsers.length > 0) {
                                this.$bkMessage({
                                    message: `内部体验人员${this.innerUsers.join(',')}已存在`,
                                    theme: 'error'
                                })
                            }
                            this.innerUsers = []
                            !this.isManual && (this.importType = undefined)
                        } catch (error) {
                            console.error(error)
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
            getOrgFullName (org) {
                const arr = []
                let temp = org
                while (temp.parent !== null) {
                    temp = temp.parent
                    arr.unshift(temp.name)
                }
                return arr.join('/')
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
            beforeClose () {
                this.$bkInfo({
                    title: '离开后，新编辑的数据将会丢失',
                    type: 'warning',
                    theme: 'warning',
                    confirmFn: () => {
                        this.cancelFn()
                    }
                })
            },
            clearSort () {
                this.$refs.filterTable.clearSort()
            },
            clearFilter () {
                this.filters = {}
                this.$refs.filterTable.clearFilter()
            },
            filterMethod (value, row, column) {
                const property = column.property
                return row[property] === value
            },
            handleFilterChange (filter) {
                const filters = {
                    ...this.filters,
                    ...filter
                }
                this.filters = Object.keys(filters).filter(key => {
                    return filters[key].length > 0
                }).reduce((acc, key) => {
                    acc[key] = filters[key]
                    return acc
                }, {})
            }
            
        }
    }
</script>

<style lang="scss">
    .bk-table-filter-panel .panel-checkbox-group {
        max-height: 360px;
        overflow: auto;
    }
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
            .memeber-search-empty-title {
                font-size: 12px;
            }
            .memeber-search-empty-desc {
                font-size: 12px;
                > span {
                    color: #979BA5;
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

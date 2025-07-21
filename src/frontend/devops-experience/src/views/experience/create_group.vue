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
            <bk-form-item
                :label="$t('experience.group_name')"
                required
                property="name"
            >
                <bk-input
                    :placeholder="$t('experience.group_name_placeholder')"
                    maxlength="20"
                    name="groupName"
                    :input-style="inputStyle"
                    v-model.trim="createGroupForm.name"
                />
            </bk-form-item>
            <bk-form-item
                :label="$t('experience.description')"
                property="remark"
            >
                <bk-input
                    type="textarea"
                    :placeholder="$t('experience.description_placeholder')"
                    :maxlength="200"
                    name="groupDesc"
                    v-model.trim="createGroupForm.remark"
                />
            </bk-form-item>
            <bk-form-item :label="$t('experience.members')">
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
                                :placeholder="$t('experience.select_placeholder')"
                                :value="importType"
                                @selected="handleImportTypeSelected"
                                :loading="loadingGroup"
                                :clearable="false"
                            >
                                <bk-option
                                    v-for="option in importTypeList"
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
                                {{ $t('experience.add_button') }}
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
                            <p class="memeber-search-empty-title">{{ $t('experience.search_empty_title') }}</p>
                            <p class="memeber-search-empty-desc">
                                {{ $t('experience.search_empty_desc') }}
                                <span>{{ $t('experience.adjust_keyword') }}</span>
                                {{ $t('experience.or') }}
                                <bk-button
                                    text
                                    theme="primary"
                                    @click="clearFilter"
                                >
                                    {{ $t('experience.clear_filter') }}
                                </bk-button>
                            </p>
                        </bk-exception>
                        
                        <bk-table-column
                            :label="$t('experience.name_column')"
                            prop="name"
                            column-key="name"
                            sortable
                            :key="nameFilter.length"
                            show-overflow-tooltip
                            filter-searchable
                            :filters="nameFilter"
                        />
                        <bk-table-column
                            :label="$t('experience.type_column')"
                            prop="typeLabel"
                            column-key="type"
                            :filters="typeFilters"
                        />
                        <bk-table-column
                            :label="$t('experience.org_column')"
                            prop="deptFullName"
                            column-key="deptFullName"
                            show-overflow-tooltip
                            filter-searchable
                            :filters="orgFilters"
                        />
                        <bk-table-column
                            :label="$t('experience.action_column')"
                            width="80"
                        >
                            <template slot-scope="props">
                                <bk-button
                                    theme="primary"
                                    text
                                    @click="remove(props.row)"
                                >
                                    {{ $t('experience.remove_button') }}
                                </bk-button>
                            </template>
                        </bk-table-column>
                    </bk-table>
                </section>
            </bk-form-item>
        </bk-form>
        
        <footer
            class="group-import-footer"
            slot="footer"
        >
            <bk-button
                :disabled="isLoading || submitting"
                theme="primary"
                @click="handleSubmit"
            >
                {{ $t('experience.submit_button') }}
            </bk-button>
            <bk-button
                :disabled="isLoading || submitting"
                @click="beforeClose"
            >
                {{ $t('experience.cancel_button') }}
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
                        message: this.$t('experience.validation.group_name_required'),
                        trigger: 'blur'
                    }]
                }
            },
            panels () {
                return [{
                    name: 'manual',
                    label: this.$t('experience.labels.manual_add')
                }, {
                    name: 'import',
                    label: this.$t('experience.labels.import_from_group')
                }]
            },
            manualOptions () {
                return [
                    {
                        id: 1,
                        name: this.$t('experience.innerMember')
                    },
                    {
                        id: 3,
                        name: this.$t('experience.innerOrgs')
                    },
                    {
                        id: 2,
                        name: this.$t('experience.outerMember')
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
                        return 'bk-input'
                    case 1:
                    default:
                        return this.isManual ? 'bk-member-selector' : 'bk-tag-input'
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
                                value: this.outerUsers
                            },
                            listeners: {
                                change: (value) => {
                                    this.outerUsers = value
                                },
                                enter: (value) => {
                                    this.outerUsers = value
                                    this.handleAddUser()
                                }
                            }
                        }
                    case 1:
                    default:
                        return {
                            props: {
                                placeholder: this.$t(`experience.${!this.isManual ? 'selectGroupFromLeft' : 'description_placeholder'}`),
                                key: this.isManual,
                                disabled: !this.isManual,
                                value: this.innerUsers,
                                allowCreate: !this.isManual
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
                return this.createGroupForm.members.map(item => item.id)
            },
            nameFilter () {
                const nameSet = new Set()
                return this.createGroupForm.members.reduce((acc, item) => {
                    if (!nameSet.has(item.name)) {
                        nameSet.add(item.name)
                        acc.push({
                            text: item.name,
                            value: item.name
                        })
                    }
                    return acc
                }, [])
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
                    this.outerUsers = ''
                    this.innerOrg = null
                    
                    this.filters = {}
                    this.clearFilter()
                    this.clearSort()
                }
            }
        },
    
        methods: {
            ...mapActions('experience', [
                'requestUserGroup',
                'updateselectUserGroup',
                'editUserGroups',
                'requestUserOrg',
                'isCpValid'
            ]),
            /**
             * 获取外部体验人员列表
             */
            
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
                this.outerUsers = ''
                this.innerOrg = null
            },
            handlePageLimitChange (limit) {
                this.pagination.limit = limit
            },
            handlePageChange (page) {
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
                this.outerUsers = ''
            },
            async handleAddUser () {
                try {
                    switch (this.importType) {
                        case 3: {
                            if (!this.innerOrg) return
                            if (this.userSet.has(this.innerOrg.id)) {
                                this.$bkMessage({
                                    message: this.$t('experience.messages.org_exists', { name: this.innerOrg.name }),
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
                            this.clearFilter()
                            break
                        }
                        
                        case 2: {
                            await this.addOutterUsers(this.outerUsers)
                            this.outerUsers = ''
                            break
                        }
                        case 1:
                        default: {
                            try {
                                this.adding = true
                                const list = this.innerUsers.filter(item => !this.userSet.has(item) && item.indexOf('@tai') === -1)
                                
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
                                } else if (this.innerUsers.filter(item => item.indexOf('@tai') === -1).length > 0) {
                                    this.$bkMessage({
                                        message: this.$t('experience.messages.member_exists', { names: this.innerUsers.join(',') }),
                                        theme: 'error'
                                    })
                                }
                                const outerUsers = this.innerUsers.filter(item => item.indexOf('@tai') > -1).join(',')
                                if (outerUsers) {
                                    await this.addOutterUsers(outerUsers)
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
                } catch (error) {
                    this.$bkMessage({
                        message: error.message || error,
                        theme: 'error'
                    })
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

                    message = this.$t('experience.save_success')
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
            async addOutterUsers (outerUsers) {
                const { illegalUserIds, legalUserIds } = await this.isCpValid(outerUsers)
                const list = legalUserIds
                    .filter(item => !this.userSet.has(item))
                const conflictUserIds = legalUserIds
                    .filter(item => this.userSet.has(item))
                this.$bkNotify({
                    title: this.$t('experience.messages.external_user_result'),
                    limit: 1,
                    message: this.$createElement('div', {}, [
                        list.length > 0 ? this.$createElement('p', {}, this.$t('experience.messages.add_success', { users: list.join(',') })) : null,
                        illegalUserIds.length > 0 ? this.$createElement('p', {}, this.$t('experience.messages.add_failed', { users: illegalUserIds.join(',') })) : null,
                        conflictUserIds.length > 0 ? this.$createElement('p', {}, this.$t('experience.messages.already_exists', { users: conflictUserIds.join(',') })) : null
                    ]),
                    theme: illegalUserIds.length > 0 ? 'error' : conflictUserIds.length > 0 ? 'warning' : 'success'
                })
                
                if (list.length > 0) {
                    this.handleGroupFieldChange(
                        'members',
                        [
                            ...list.map(item => ({
                                name: item,
                                id: item,
                                type: 2,
                                deptFullName: '--'
                            })),
                            ...this.createGroupForm.members
                        ]
                    )
                }
            },
            beforeClose () {
                this.$bkInfo({
                    title: this.$t('experience.dialog.leave_warning'),
                    type: 'warning',
                    theme: 'warning',
                    okText: this.$t('experience.confirm'),
                    cancelText: this.$t('experience.cancel'),
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
    .bk-sideslider-wrapper {
        top: 50px;
        .group-form-content {
            padding: 24px;
            height: calc(100vh - 150px);
            
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
    }
    .group-import-footer {
        padding: 0 24px;
    }
</style>

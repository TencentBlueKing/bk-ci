<template>
    <div class="release-setting-wrapper">
        <content-header>
            <div slot="left">{{ $t(`experience.${$route.meta.title}`) }}</div>
        </content-header>
        <section
            class="sub-view-port"
            v-bkloading="{ isLoading: loading.isLoading, title: $t('experience.loading_title') }"
        >
            <bk-tab
                :active.sync="curTab"
                type="unborder-card"
            >
                <bk-tab-panel
                    v-for="(panel, index) in panels"
                    v-bind="panel"
                    :key="index"
                >
                    <template v-if="curTab === 'experienceGroup'">
                        <div
                            v-if="showContent && experienceList.length"
                            class="table-operate-bar"
                        >
                            <span
                                v-perm="{
                                    tooltips: $t('experience.no_permission'),
                                    permissionData: {
                                        projectId: projectId,
                                        resourceType: EXPERIENCE_GROUP_RESOURCE_TYPE,
                                        resourceCode: projectId,
                                        action: EXPERIENCE_GROUP_RESOURCE_ACTION.CREATE
                                    }
                                }"
                            >
                                <bk-button
                                    theme="primary"
                                    @click="toCreateGroup"
                                >{{ $t('experience.add') }}</bk-button>
                            </span>
                        </div>
                        <bk-table
                            v-if="showContent && experienceList.length"
                            :data="experienceList"
                            :pagination="pagination"
                            @page-change="requestList"
                            @page-limit-change="handlePageLimitChange"
                        >
                            <bk-table-column
                                :label="$t('experience.name')"
                                show-overflow-tooltip
                                prop="name"
                            ></bk-table-column>
                            <bk-table-column
                                :label="$t('experience.inner_users')"
                                prop="innerUsersCount"
                            >
                                <template slot-scope="props">
                                    <bk-popover
                                        placement="bottom"
                                        :disabled="props.row.innerUsersCount <= 0"
                                    >
                                        <span class="handler-inner">{{ props.row.innerUsersCount }}</span>
                                        <template slot="content">
                                            <p style="max-width: 300px; text-align: left; white-space: normal;word-break: break-all;font-weight: 400;">
                                                {{ props.row.innerUsers.join(",") }}
                                            </p>
                                        </template>
                                    </bk-popover>
                                </template>
                            </bk-table-column>
                            <bk-table-column
                                :label="$t('experience.inner_orgs')"
                                prop="deptsCount"
                            >
                                <template slot-scope="props">
                                    <bk-popover
                                        placement="bottom"
                                        :disabled="props.row.deptsCount <= 0"
                                    >
                                        <span class="handler-inner">{{ props.row.deptsCount }}</span>
                                        <template slot="content">
                                            <p style="max-width: 300px; text-align: left; white-space: normal;word-break: break-all;font-weight: 400;">
                                                {{ props.row.depts.join(",") }}
                                            </p>
                                        </template>
                                    </bk-popover>
                                </template>
                            </bk-table-column>
                            <bk-table-column
                                :label="$t('experience.outer_users')"
                                prop="outerUsersCount"
                            >
                                <template slot-scope="props">
                                    <bk-popover
                                        placement="bottom"
                                        :disabled="props.row.outerUsersCount <= 0"
                                    >
                                        <span class="handler-outer">{{ props.row.outerUsersCount }}</span>
                                        <template slot="content">
                                            <p style="max-width: 300px; text-align: left; white-space: normal;word-break: break-all;font-weight: 400;">
                                                <span>{{ props.row.outerUsers.join(',') }}</span>
                                            </p>
                                        </template>
                                    </bk-popover>
                                </template>
                            </bk-table-column>
                            <bk-table-column
                                :label="$t('experience.groupCreator')"
                                prop="creator"
                            ></bk-table-column>
                            <bk-table-column
                                :label="$t('experience.description')"
                                show-overflow-tooltip
                                prop="remark"
                            ></bk-table-column>
                            <bk-table-column
                                :label="$t('experience.actions')"
                                prop="creator"
                            >
                                <template slot-scope="props">
                                    <div class="handler-group">
                                        <bk-button
                                            class="mr5"
                                            v-perm="{
                                                hasPermission: props.row.permissions.canEdit,
                                                disablePermissionApi: true,
                                                tooltips: $t('experience.no_permission'),
                                                permissionData: {
                                                    projectId: projectId,
                                                    resourceType: EXPERIENCE_GROUP_RESOURCE_TYPE,
                                                    resourceCode: props.row.groupHashId,
                                                    action: EXPERIENCE_GROUP_RESOURCE_ACTION.EDIT
                                                }
                                            }"
                                            text
                                            @click="toEditGroup(props.row)"
                                        >
                                            {{ $t('experience.edit') }}
                                        </bk-button>
                                        <bk-button
                                            text
                                            v-perm="{
                                                hasPermission: props.row.permissions.canDelete,
                                                disablePermissionApi: true,
                                                tooltips: $t('experience.no_permission'),
                                                permissionData: {
                                                    projectId: projectId,
                                                    resourceType: EXPERIENCE_GROUP_RESOURCE_TYPE,
                                                    resourceCode: props.row.groupHashId,
                                                    action: EXPERIENCE_GROUP_RESOURCE_ACTION.DELETE
                                                }
                                            }"
                                            @click="toDeleteGruop(props.row)"
                                        >
                                            {{ $t('experience.delete') }}
                                        </bk-button>
                                    </div>
                                </template>
                            </bk-table-column>
                        </bk-table>
                        <empty-data
                            v-if="showContent && !experienceList.length"
                            :empty-info="emptyInfo"
                            :to-create-fn="toCreateGroup"
                        >
                        </empty-data>
                    </template>
                </bk-tab-panel>
            </bk-tab>

            <experience-group
                v-bind="groupSideslider"
                :create-group-form="createGroupForm"
                :handle-group-field-change="handleGroupFieldChange"
                :error-handler="errorHandler"
                @after-submit="afterCreateGroup"
                :cancel-fn="cancelFn"
            >
            </experience-group>
        </section>
    </div>
</template>

<script>
    import { EXPERIENCE_GROUP_RESOURCE_ACTION, EXPERIENCE_GROUP_RESOURCE_TYPE } from '@/utils/permission'
    import { getQueryString } from '@/utils/util'
    import experienceGroup from './create_group'
    import emptyData from './empty-data'

    export default {
        components: {
            emptyData,
            experienceGroup
        },
        data () {
            const { projectId } = this.$route.params
            return {
                EXPERIENCE_GROUP_RESOURCE_ACTION,
                EXPERIENCE_GROUP_RESOURCE_TYPE,
                curTab: 'experienceGroup',
                experienceList: [],
                showContent: false,
                loading: {
                    isLoading: false,
                    title: this.$t('experience.loading_title')
                },
                groupSideslider: {
                    title: '',
                    visible: false,
                    isLoading: false
                },
                createGroupForm: {
                    groupHashId: undefined,
                    name: '',
                    members: [],
                    remark: ''
                },
                errorHandler: {
                    nameError: false
                },
                emptyInfo: {
                    title: this.$t('experience.no_experience_group'),
                    desc: this.$t('experience.add_experience_group_tips'),
                    permissionData: {
                        projectId: projectId,
                        resourceType: EXPERIENCE_GROUP_RESOURCE_TYPE,
                        resourceCode: projectId,
                        action: EXPERIENCE_GROUP_RESOURCE_ACTION.CREATE
                    }
                },
                urlParams: getQueryString('groupId') || '',
                pagination: {
                    current: 1,
                    count: 0,
                    limit: 10
                }
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
                        label: this.$t('experience.experience_group')
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
            await this.requestList()
        },
        methods: {
            handlePageLimitChange (limit) {
                this.pagination.limit = limit
                this.requestList()
            },
            async requestList (page = 1) {
                const {
                    loading
                } = this

                loading.isLoading = true
                loading.title = this.$t('experience.loading_title')
                try {
                    const res = await this.$store.dispatch('experience/requestGroupList', {
                        projectId: this.projectId,
                        page,
                        pageSize: this.pagination.limit
                    })
                    this.pagination.count = res.count
                    this.pagination.current = page
                    this.experienceList = res.records.map(item => {
                        if (this.urlParams === item.groupHashId) {
                            setTimeout(() => {
                                this.toEditGroup(item)
                            }, 800)
                        }
                        return item
                    })
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                } finally {
                    setTimeout(() => {
                        this.loading.isLoading = false
                    }, 1000)
                }

                this.showContent = true
            },
            toCreateGroup () {
                this.createGroupForm = {
                    groupHashId: undefined,
                    name: '',
                    members: [],
                    remark: ''
                }
                this.groupSideslider.title = this.$t('experience.add_experience_group')
                this.groupSideslider.visible = true
            },
            handleGroupFieldChange (name, value) {
                this.createGroupForm[name] = value
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
                this.groupSideslider.visible = false
            },
            cancelFn () {
                if (!this.groupSideslider.isLoading) {
                    this.groupSideslider.visible = false
                }
            },
            async toEditGroup (row) {
                if (row.permissions.canEdit) {
                    this.groupSideslider.title = row.name
                    this.groupSideslider.visible = true
                    this.groupSideslider.isLoading = true

                    try {
                        const res = await this.$store.dispatch('experience/toGetGroupDetail', {
                            projectId: this.projectId,
                            groupHashId: row.groupHashId
                        })

                        this.createGroupForm = res
                    } catch (err) {
                        const message = err.data ? err.data.message : err
                        const theme = 'error'

                        this.$bkMessage({
                            message,
                            theme
                        })
                    } finally {
                        this.groupSideslider.isLoading = false
                    }
                }
            },
            toDeleteGruop (row) {
                if (row.permissions.canDelete) {
                    this.$bkInfo({
                        title: this.$t('experience.confirm'),
                        subTitle: this.$t('experience.confirm_delete_group'),
                        confirmFn: async () => {
                            let message, theme

                            try {
                                await this.$store.dispatch('experience/toDeleteGroups', {
                                    projectId: this.projectId,
                                    groupHashId: row.groupHashId
                                })

                                message = this.$t('experience.delete_success')
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
        .sub-view-port .bk-tab-label-item{
            background-color: transparent;
        }
    }
</style>

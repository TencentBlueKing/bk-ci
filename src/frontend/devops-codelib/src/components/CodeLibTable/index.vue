<template>
    <bk-table class="devops-codelib-table"
        :data="records"
        :pagination="pagination"
        @sort-change="handleSortChange"
        @page-change="handlePageChange"
        @page-limit-change="handlePageCountChange"
        v-bkloading="{ isLoading }"
    >
        <bk-table-column type="index" :label="$t('codelib.index')" align="center" width="60"></bk-table-column>
        <bk-table-column :label="$t('codelib.aliasName')" sortable prop="aliasName"></bk-table-column>
        <bk-table-column :label="$t('codelib.address')" sortable prop="url"></bk-table-column>
        <bk-table-column :label="$t('codelib.type')" sortable prop="type" :formatter="typeFormatter"></bk-table-column>
        <bk-table-column :label="$t('codelib.authIdentity')">
            <template slot-scope="props">
                <span>{{ props.row.authType }}@</span><!--
                --><a class="text-link"
                    v-if="!['OAUTH'].includes(props.row.authType)"
                    :href="`/console/ticket/${projectId}/editCredential/${props.row.authIdentity}`"
                    target="_blank"
                >{{ props.row.authIdentity }}</a><!--
                --><span v-else>{{ props.row.authIdentity }}</span>
            </template>
        </bk-table-column>
        <bk-table-column :label="$t('codelib.operation')" width="150">
            <template slot-scope="props">
                <template v-if="props.row.canUse">
                    <bk-button
                        theme="primary"
                        v-perm="{
                            hasPermission: props.row.canEdit,
                            disablePermissionApi: true,
                            permissionData: {
                                projectId: projectId,
                                resourceType: RESOURCE_TYPE,
                                resourceCode: props.row.repositoryHashId,
                                action: RESOURCE_ACTION.EDIT
                            }
                        }"
                        text
                        @click="editCodeLib(props.row)"
                    >{{ $t('codelib.edit') }}</bk-button>
                    <bk-button
                        theme="primary"
                        v-perm="{
                            hasPermission: props.row.canDelete,
                            disablePermissionApi: true,
                            permissionData: {
                                projectId: projectId,
                                resourceType: RESOURCE_TYPE,
                                resourceCode: props.row.repositoryHashId,
                                action: RESOURCE_ACTION.DELETE
                            }
                        }"
                        text
                        @click="deleteCodeLib(props.row)"
                    >{{ $t('codelib.delete') }}</bk-button>
                </template>
                <template v-else>
                    <bk-button
                        theme="primary"
                        outline
                        @click="handleApplyPermission(props.row)"
                    >{{ $t('codelib.applyPermission') }}</bk-button>
                </template>
            </template>
        </bk-table-column>
    </bk-table>
</template>

<script>
    import { mapActions, mapState } from 'vuex'
    import { getCodelibConfig } from '../../config/'
    import { RESOURCE_ACTION, RESOURCE_TYPE } from '../../utils/permission'

    export default {
        props: {
            switchPage: {
                type: Function,
                required: true
            },
            count: Number,
            hasCreatePermission: Boolean,
            totalPages: Number,
            page: Number,
            pageSize: Number,
            records: {
                type: Array,
                required: true,
                default: () => []
            }
        },

        data () {
            return {
                RESOURCE_ACTION,
                RESOURCE_TYPE,
                pagination: {
                    current: this.page,
                    count: this.count,
                    limit: this.pageSize
                },
                isLoading: false,
                sortByMap: {
                    aliasName: 'ALIAS_NAME',
                    url: 'URL',
                    type: 'TYPE',
                    null: ''
                },
                sortTypeMap: {
                    ascending: 'ASC',
                    descending: 'DESC',
                    null: ''
                }
            }
        },

        computed: {
            ...mapState('codelib', ['gitOAuth']),

            currentPage: {
                get () {
                    return this.page
                },
                set (page) {
                    this.switchPage(page, this.pageSize)
                }
            },

            projectId () {
                return this.$route.params.projectId
            }
        },

        watch: {
            page (val) {
                this.pagination.current = val
            },

            count (val) {
                this.pagination.count = val
            },

            pageSize (val) {
                this.pagination.limit = val
            }
        },

        created () {
            const { repoId, repoType } = this.$route.params
            if (repoId && repoType) {
                // 如果路径带有仓库ID，则弹出对应的编辑窗口
                this.editCodeLib({
                    type: repoType,
                    repositoryHashId: repoId
                })
            }
        },

        methods: {
            ...mapActions('codelib', [
                'toggleCodelibDialog',
                'requestDetail',
                'updateCodelib',
                'deleteRepo',
                'checkGitOAuth',
                'checkTGitOAuth'
            ]),

            typeFormatter (row, column, cellValue, index) {
                return cellValue.replace('CODE_', '')
            },

            handlePageChange (current) {
                this.pagination.current = current
                this.switchPage(current, this.pagination.limit)
            },

            handlePageCountChange (limit) {
                if (this.pagination.limit === limit) return

                this.pagination.current = 1
                this.pagination.limit = limit
                this.switchPage(1, limit)
            },

            async editCodeLib (codelib) {
                const { repositoryHashId, type, authType, svnType } = codelib
                const { credentialTypes, typeName } = getCodelibConfig(
                    type,
                    svnType,
                    authType
                )
                this.updateCodelib({
                    '@type': typeName
                })
                const CodelibDialog = {
                    repositoryHashId,
                    showCodelibDialog: true,
                    projectId: this.projectId,
                    credentialTypes,
                    authType,
                    codelib,
                    instance: this
                }
                this.toggleCodelibDialog(CodelibDialog)
            },
            
            handleApplyPermission (row) {
                this.handleNoPermission({
                    projectId: this.projectId,
                    resourceType: RESOURCE_TYPE,
                    resourceCode: row.repositoryHashId,
                    action: RESOURCE_ACTION.DELETE
                })
            },
            deleteCodeLib ({ repositoryHashId, aliasName }) {
                this.$bkInfo({
                    theme: 'warning',
                    type: 'warning',
                    subTitle: this.$t('codelib.deleteCodelib', [aliasName]),
                    confirmFn: () => {
                        const { projectId, currentPage, pageSize, count, totalPages } = this
                        this.isLoading = true

                        this.deleteRepo({ projectId, repositoryHashId }).then(() => {
                            this.$bkMessage({
                                message: `${this.$t('codelib.codelib')}${aliasName}${this.$t('codelib.successfullyDeleted')}`,
                                theme: 'success'
                            })

                            this.$router.push({
                                name: 'codelibHome',
                                params: {
                                    projectId: this.projectId,
                                    repoType: '',
                                    repoId: ''
                                }
                            })
                            if (count - 1 <= pageSize * (totalPages - 1)) {
                                // 删除列表项之后，如果不足页数的话直接切换到上一页
                                this.switchPage(currentPage - 1, pageSize)
                            } else {
                                this.switchPage(currentPage, pageSize)
                            }
                        }).catch((e) => {
                            this.handleError(
                                e,
                                {
                                    projectId: this.projectId,
                                    resourceType: RESOURCE_TYPE,
                                    resourceCode: repositoryHashId,
                                    action: RESOURCE_ACTION.DELETE
                                }
                            )
                        }).finally(() => {
                            this.isLoading = false
                        })
                    }
                })
            },
            
            handleSortChange ({ prop, order }) {
                const sortBy = this.sortByMap[prop]
                const sortType = this.sortTypeMap[order]
                this.$emit('handleSortChange', { sortBy, sortType })
            }
        }
    }
</script>

<style lang="scss">
.devops-codelib-table {
    margin-top: 20px;
    min-width: 1200px;
    .devops-codelib-table-body td {
        white-space: nowrap;
        text-overflow: ellipsis;
        overflow: hidden;
    }
    > footer {
        display: flex;
        height: 36px;
        align-items: center;
        margin-top: 30px;
        .codelib-count {
            margin-right: 20px;
        }
        .codelib-page-size {
            width: 62px;
            display: inline-block;
        }
        .codelib-paging {
            margin-left: auto;
        }
    }
}
</style>

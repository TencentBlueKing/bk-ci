<template>
    <bk-table class="devops-codelib-table"
        :data="records"
        :pagination="pagination"
        @page-change="handlePageChange"
        @page-limit-change="handlePageCountChange"
    >
        <bk-table-column type="index" label="序列" align="center" width="60"></bk-table-column>
        <bk-table-column label="别名" prop="aliasName"></bk-table-column>
        <bk-table-column label="地址" prop="url"></bk-table-column>
        <bk-table-column label="类型" prop="type" :formatter="typeFormatter"></bk-table-column>
        <bk-table-column label="操作" width="150">
            <template slot-scope="props">
                <bk-button theme="primary" text @click="editCodeLib(props.row)">编辑</bk-button>
                <bk-button theme="primary" text @click="deleteCodeLib(props.row)">删除</bk-button>
            </template>
        </bk-table-column>
    </bk-table>
</template>

<script>
    import { mapActions, mapState } from 'vuex'
    import { getCodelibConfig } from '../../config/'
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
            },
            pagination () {
                return {
                    current: this.page,
                    count: this.count,
                    limit: this.pageSize
                }
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
                    authType
                }
                this.toggleCodelibDialog(CodelibDialog)
            },

            deleteCodeLib ({ repositoryHashId, aliasName }) {
                const subHeader = this.$createElement(
                    'p',
                    {
                        style: {
                            textAlign: 'center'
                        }
                    },
                    `删除代码库${aliasName}`
                )
                this.$bkInfo({
                    subHeader,
                    title: `确认`,
                    confirmFn: async () => {
                        try {
                            const {
                                projectId,
                                currentPage,
                                pageSize,
                                count,
                                totalPages
                            } = this
                            await this.deleteRepo({
                                projectId,
                                repositoryHashId
                            })
                            this.$bkMessage({
                                message: `代码库${aliasName}删除成功`,
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
                        } catch (e) {
                            this.$bkMessage({
                                message: e.message,
                                theme: 'error'
                            })
                        }
                    }
                })
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

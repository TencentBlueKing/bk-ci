<template>
    <bk-dialog
        width="900"
        header-position="left"
        :value="showProjectDialog"
        :mask-close="false"
        :close-icon="false"
        @confirm="handleConfirm"
        @cancel="handleCancel"
    >
        <header class="share-project-dialog-header">
            <h2>{{ $t('environment.addProject') }}</h2>
            <bk-input
                class="share-project-search-input"
                right-icon="bk-icon icon-search"
                :placeholder="$t('environment.search')"
                :clearable="true"
                show-clear-only-hover
                v-model="searchVal"
                @change="handleSearch"
            />
        </header>
        <div class="env-share-project-list">
            <bk-table
                ref="shareDiaglogTable"
                :height="tableHieght"
                :data="projects"
                :pagination="pagination"
                @page-change="handlePageChange"
                @page-limit-change="handlePageLimitChange"
                @selection-change="handleSelectChange"
            >
                <bk-table-column
                    type="selection"
                    width="60"
                ></bk-table-column>
                <bk-table-column
                    :label="$t('environment.project')"
                    prop="name"
                ></bk-table-column>
                <bk-table-column
                    :label="$t('environment.projectId')"
                    prop="projectId"
                ></bk-table-column>
                <bk-table-column
                    :label="$t('environment.envInfo.creator')"
                    prop="creator"
                ></bk-table-column>
                <bk-table-column
                    :label="$t('environment.envInfo.creationTime')"
                    prop="formatTime"
                ></bk-table-column>
            </bk-table>
        </div>
    </bk-dialog>
</template>

<script>
    import { mapActions } from 'vuex'
    import { throttle, convertTime } from '@/utils/util'
    export default {
        name: 'select-env-share-dialog',
        props: {
            showProjectDialog: Boolean,
            projectId: {
                type: String,
                required: true
            },
            envHashId: {
                type: String,
                required: true
            }
        },
        data () {
            return {
                pagination: {
                    current: 1,
                    count: 20,
                    limit: 10
                },
                selection: [],
                projects: [],
                searchVal: '',
                searching: false
            }
        },
        computed: {
            handleSearch () {
                return throttle(this.getProjects, 500)
            },
            tableHieght () {
                return window.innerHeight * 0.4
            }
        },
        watch: {
            showProjectDialog (val) {
                if (!val) {
                    this.reset()
                } else {
                    this.getProjects()
                }
            }
        },
        created () {
            this.getProjects()
        },
        methods: {
            ...mapActions('environment', [
                'requestProjects'
            ]),
            async getProjects () {
                if (this.searching) return
                try {
                    this.searching = true
                    const { records, count } = await this.requestProjects({
                        projectId: this.projectId,
                        envHashId: this.envHashId,
                        page: this.searchVal ? 1 : this.pagination.current,
                        pageSize: this.pagination.limit,
                        search: this.searchVal
                    })
                    this.projects = records.map(item => ({
                        ...item,
                        formatTime: convertTime(item.createTime)
                    }))
                    this.pagination.count = count
                } catch (error) {
                    console.trace(error)
                } finally {
                    this.searching = false
                }
            },
            handlePageChange (current) {
                this.pagination.current = current
                this.$nextTick(() => {
                    this.getProjects()
                })
            },
            handlePageLimitChange (limit) {
                this.pagination.limit = limit
                this.$nextTick(() => {
                    this.getProjects()
                })
            },
            handleSelectChange (selection) {
                this.selection = selection
            },
            reset () {
                if (this.$refs.shareDiaglogTable) {
                    this.$refs.shareDiaglogTable.clearSelection()
                }
                this.pagination.current = 1
                this.searchVal = ''
            },
            handleConfirm () {
                this.$emit('confirm', this.selection)
            },
            
            handleCancel () {
                this.$emit('cancel')
            }
        }
    }
</script>

<style lang="scss">
    .share-project-dialog-header {
        display: flex;
        justify-content: space-between;
        margin: 12px 0 24px 0;

        .share-project-search-input {
            width: 360px;
        }
    }
</style>

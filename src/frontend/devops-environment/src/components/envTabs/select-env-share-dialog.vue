<template>
    <bk-dialog :value="showProjectDialog"
        width="900"
        header-position="left"
        :mask-close="false"
        @confirm="handleConfirm"
        @cancel="handleCancel"
        :title="$t('environment.addProject')"
    >
        <div class="env-share-project-list">
            <bk-table
                :height="500"
                :data="projects"
                :pagination="pagination"
                @page-change="handlePageChange"
                @page-limit-change="handlePageLimitChange"
                @selection-change="handleSelectChange"
            >
                <bk-table-column type="selection" width="60"></bk-table-column>
                <bk-table-column :label="$t('environment.project')" prop="name"></bk-table-column>
                <bk-table-column :label="$t('environment.projectId')" prop="projectId"></bk-table-column>
                <bk-table-column :label="$t('environment.envInfo.creator')" prop="creator"></bk-table-column>
                <bk-table-column :label="$t('environment.envInfo.creationTime')" prop="formatTime"></bk-table-column>
            </bk-table>
        </div>
    </bk-dialog>
</template>

<script>
    import { mapActions } from 'vuex'
    import { convertTime } from '@/utils/util'
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
                    limit: 20
                },
                selection: [],
                projects: []
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
                try {
                    const { records, count } = await this.requestProjects({
                        projectId: this.projectId,
                        envHashId: this.envHashId,
                        page: this.pagination.current,
                        pageSize: this.pagination.limit
                    })
                    this.projects = records.map(item => ({
                        ...item,
                        formatTime: convertTime(item.createTime)
                    }))
                    this.pagination.count = count
                } catch (error) {
                    console.trace(error)
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
            handleConfirm () {
                this.$emit('confirm', this.selection)
                this.selection = []
            },
            handleCancel () {
                this.$emit('cancel', this.selection)
                this.selection = []
            }
        }
    }
</script>

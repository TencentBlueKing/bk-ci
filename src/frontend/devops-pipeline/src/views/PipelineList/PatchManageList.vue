<template>
    <main class="pipeline-list-main">
        <h5 class="current-pipeline-group-name">
            <ArchiveViewName v-if="isArchiveView" />
            <span v-else>{{ currentViewName }}3</span>

            <bk-button
                theme="default"
                v-if="!isArchiveView"
                class="historical-task"
                @click="goHistoricalTask"
            >
                {{ $t('historicalTask') }}
            </bk-button>
        </h5>
        <header class="pipeline-list-main-header">
            <div class="pipeline-list-main-header-left-area">
                <bk-button
                    v-if="!isArchiveView"
                    :disabled="!isSelected"
                    @click="togglePatchAddTo"
                >
                    {{ $t('patchAddTo') }}
                </bk-button>
                <bk-button
                    v-if="!isArchiveView"
                    :disabled="!isSelected"
                    @click="toggleArchive"
                >
                    {{ $t('archive.batchArchiving') }}
                </bk-button>
                <bk-button
                    v-if="!isArchiveView"
                    :disabled="!isSelected"
                    @click="toggleCrossProjectCopy"
                >
                    {{ $t('crossProjectCopy') }}
                </bk-button>
                <span v-bk-tooltips="notAllowPatchDeleteTips">
                    <bk-button
                        :disabled="!isSelected || isPacGroup"
                        @click="toggleDeleteConfirm"
                    >
                        {{ $t('patchDelete') }}
                    </bk-button>
                </span>
                <bk-button
                    class="exit-patch-text-btn"
                    text
                    @click="exitPatch"
                >
                    {{ $t('exitPatch') }}
                </bk-button>
            </div>
            <div class="pipeline-list-main-header-right-area">
                <pipeline-searcher
                    v-model="filters"
                    :is-archive-view="isArchiveView"
                />
            </div>
        </header>
        <div
            class="pipeline-list-box"
            ref="tableBox"
        >
            <pipeline-table-view
                ref="pipelineTable"
                :fetch-pipelines="getPipelines"
                :filter-params="filters"
                :max-height="tableHeight"
                @selection-change="handleSelectChange"
                is-patch-view
            />
        </div>
        <add-to-group-dialog
            is-patch
            :add-to-dialog-show="addToDialogShow"
            :pipeline-list="selected"
            @done="refreshList"
            @close="togglePatchAddTo"
        />
        <remove-confirm-dialog
            type="delete"
            :is-show="isConfirmShow"
            @close="toggleDeleteConfirm"
            @done="refreshList"
            :pipeline-list="selected"
        />
        <archive-dialog
            type="archiveBatch"
            :is-archive-dialog-show="isArchiveShow"
            :pipeline-list="selected"
            @toggleSelection="toggleSelection"
            @cancel="toggleArchive"
            @done="refreshList"
        />
        <delete-archived-dialog
            type="archiveBatch"
            :is-show-delete-archived-dialog="isDeleteArchiveShow"
            :pipeline-list="selected"
            @done="refreshList"
            @cancel="toggleDeleteConfirm"
        />
    </main>
</template>

<script>
    import PipelineTableView from '@/components/pipelineList/PipelineTableView'
    import AddToGroupDialog from '@/views/PipelineList/AddToGroupDialog'
    import RemoveConfirmDialog from '@/views/PipelineList/RemoveConfirmDialog'
    import { mapGetters, mapActions } from 'vuex'
    import PipelineSearcher from './PipelineSearcher'
    import { ARCHIVE_VIEW_ID } from '@/store/constants'
    import { PipelineBatchTaskStep } from '@/store/modules/crossProjectCopy/constants'
    import ArchiveDialog from '@/views/PipelineList/ArchiveDialog'
    import DeleteArchivedDialog from '@/views/PipelineList/DeleteArchivedDialog'
    import ArchiveViewName from '@/components/pipelineList/archiveViewName'

    export default {
        name: 'patch-manage-list',
        components: {
            PipelineSearcher,
            PipelineTableView,
            AddToGroupDialog,
            ArchiveViewName,
            ArchiveDialog,
            DeleteArchivedDialog,
            RemoveConfirmDialog
        },
        data () {
            const { page, pageSize, sortType, collation, ...restQuery } = this.$route.query
            return {
                selected: [],
                addToDialogShow: false,
                filters: restQuery,
                isConfirmShow: false,
                isArchiveShow: false,
                isDeleteArchiveShow: false,
                tableHeight: null
            }
        },
        computed: {
            ...mapGetters('pipelines', [
                'groupMap'
            ]),
            currentViewName () {
                return this.$t(this.groupMap?.[this.$route.params.viewId]?.name ?? this.groupMap?.[this.$route.params.viewId]?.i18nKey)
            },
            isPacGroup () {
                return this.groupMap?.[this.$route.params.viewId]?.pac
            },
            isSelected () {
                return this.selected.length > 0
            },
            notAllowPatchDeleteTips () {
                return {
                    content: this.$t('notAllowPatchDeletePacPipelineTips'),
                    maxWidth: 360,
                    disabled: !this.isPacGroup,
                    delay: [300, 0]
                }
            },
            isArchiveView () {
                return this.$route.params.viewId === ARCHIVE_VIEW_ID
            }
        },
        mounted () {
            this.updateTableHeight()
            window.addEventListener('resize', this.updateTableHeight)
        },
        beforeDestroy () {
            window.removeEventListener('resize', this.updateTableHeight)
        },
        methods: {
            ...mapActions('crossProjectCopy', [
                'createBatchTask'
            ]),
            updateTableHeight () {
                this.tableHeight = this.$refs.tableBox?.offsetHeight
            },
            async toggleCrossProjectCopy () {
                const pipelineIds = this.selected.map(item => item.pipelineId)

                try {
                    const params = {
                        taskName: '',
                        taskType: 'PIPELINE_COPY',
                        pipelineIds
                    }
                    
                    const taskId = await this.createBatchTask({
                        projectId: this.$route.params.projectId,
                        params
                    })
                    this.$router.push({
                        name: 'crossProjectCopy',
                        params: {
                            taskId,
                            tab: PipelineBatchTaskStep.CONFIG
                        }
                    })
                } catch (error) {
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message || error
                    })
                }
            },
            exitPatch () {
                this.$router.push({
                    name: 'PipelineManageList',
                    params: {
                        viewId: this.$route.params.viewId
                    }
                })
            },
            togglePatchAddTo () {
                if (this.addToDialogShow) {
                    this.$refs.pipelineTable?.clearSelection?.()
                }
                this.addToDialogShow = !this.addToDialogShow
            },
            toggleArchive () {
                this.isArchiveShow = !this.isArchiveShow
            },
            handleSelectChange (selected) {
                this.selected = selected
            },
            toggleDeleteConfirm () {
                if (this.isArchiveView) {
                    this.isDeleteArchiveShow = !this.isDeleteArchiveShow
                } else {
                    this.isConfirmShow = !this.isConfirmShow
                }
            },
            refreshList () {
                this.$refs.pipelineTable?.refresh?.()
            },
            toggleSelection (list) {
                this.$refs.pipelineTable?.toggleSelection?.(list)
            },
            goHistoricalTask () {
                this.$router.push({
                    name: 'batchHistoricalTask',
                    params: {
                        projectId: this.$route.params.projectId
                    }
                })
            }
        }
    }
</script>

<style lang="scss" scoped>
  .exit-patch-text-btn {
    margin-left: 12px;
  }
</style>

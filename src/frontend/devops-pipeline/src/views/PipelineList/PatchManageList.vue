<template>
    <main class="pipeline-list-main">
        <div class="current-pipeline-group-name">
            <ArchiveViewName v-if="isArchiveView" />
            <h5 v-else>
                {{ currentViewName }}
            </h5>
        </div>
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
    import { mapGetters } from 'vuex'
    import PipelineSearcher from './PipelineSearcher'
    import { ARCHIVE_VIEW_ID } from '@/store/constants'
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
                return this.$t(this.groupMap?.[this.$route.params.viewId]?.name ?? '')
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
            updateTableHeight () {
                this.tableHeight = this.$refs.tableBox.offsetHeight
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
            }
        }
    }
</script>

<style lang="scss" scoped>
  .exit-patch-text-btn {
    margin-left: 12px;
  }
</style>

<template>
    <main class="pipeline-list-main">
        <h5 class="current-pipeline-group-name">{{ currentViewName }}</h5>
        <header class="pipeline-list-main-header">
            <div class="pipeline-list-main-header-left-area">
                <bk-button
                    :disabled="!isSelected"
                    @click="togglePatchAddTo"
                >
                    {{ $t('patchAddTo') }}
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
    </main>
</template>

<script>
    import PipelineTableView from '@/components/pipelineList/PipelineTableView'
    import AddToGroupDialog from '@/views/PipelineList/AddToGroupDialog'
    import RemoveConfirmDialog from '@/views/PipelineList/RemoveConfirmDialog'
    import { mapGetters } from 'vuex'
    import PipelineSearcher from './PipelineSearcher'
    export default {
        name: 'patch-manage-list',
        components: {
            PipelineSearcher,
            PipelineTableView,
            AddToGroupDialog,
            RemoveConfirmDialog
        },
        data () {
            const { page, pageSize, sortType, collation, ...restQuery } = this.$route.query
            return {
                selected: [],
                addToDialogShow: false,
                filters: restQuery,
                isConfirmShow: false,
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
            handleSelectChange (selected) {
                this.selected = selected
            },
            toggleDeleteConfirm () {
                this.isConfirmShow = !this.isConfirmShow
            },
            refreshList () {
                this.$refs.pipelineTable?.refresh?.()
            }
        }
    }
</script>

<style lang="scss" scoped>
  .exit-patch-text-btn {
    margin-left: 12px;
  }
</style>

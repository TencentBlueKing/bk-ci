<template>
    <main class="pipeline-list-main">
        <h5 class="current-pipeline-group-name">{{currentViewName}}</h5>
        <header class="pipeline-list-main-header">
            <div class="pipeline-list-main-header-left-area">
                <bk-button
                    :disabled="!isSelected"
                    @click="togglePatchAddTo"
                >
                    {{$t('patchAddTo')}}
                </bk-button>
                <bk-button
                    :disabled="!isSelected"
                    @click="toggleDeleteConfirm"
                >
                    {{$t('patchDelete')}}
                </bk-button>
                <bk-button text @click="exitPatch">{{$t('exitPatch')}}</bk-button>

            </div>
            <div class="pipeline-list-main-header-right-area">
                <bk-input
                    :placeholder="$t('patchSearchInputPlaceholder')"
                    right-icon="bk-icon icon-search"
                />

            </div>
        </header>
        <pipeline-table-view
            ref="pipelineTable"
            :fetch-pipelines="getPipelines"
            @selection-change="handleSelectChange"
            is-patch-view
        ></pipeline-table-view>
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
    import { mapGetters, mapActions } from 'vuex'
    import moment from 'moment'
    import PipelineTableView from '@/components/pipelineList/PipelineTableView'
    import AddToGroupDialog from '@/views/PipelineList/AddToGroupDialog'
    import RemoveConfirmDialog from '@/views/PipelineList/RemoveConfirmDialog'
    export default {
        name: 'patch-manage-list',
        components: {
            PipelineTableView,
            AddToGroupDialog,
            RemoveConfirmDialog
        },
        data () {
            return {
                selected: [],
                addToDialogShow: false,
                isConfirmShow: false
            }
        },
        computed: {
            ...mapGetters('pipelines', [
                'groupMap'
            ]),
            currentViewName () {
                return this.$t(this.groupMap?.[this.$route.params.viewId]?.name ?? '')
            },
            isSelected () {
                return this.selected.length > 0
            }
        },
        created () {
            moment.locale(this.$i18n.locale)
        },
        methods: {
            ...mapActions('pipelines', [
                'requestAllPipelinesListByFilter'
            ]),
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
                    console.log(this.$refs.pipelineTable?.clearSelection)
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
            async getPipelines () {
                try {
                    const { viewId } = this.$route.params
                    const result = await this.requestAllPipelinesListByFilter({
                        projectId: this.$route.params.projectId,
                        viewId
                    })

                    return {
                        ...result,
                        records: result.records.map(item => ({
                            ...item,
                            latestBuildStartDate: item.latestBuildStartTime ? moment(item.latestBuildStartTime).fromNow() : '--'
                        }))
                    }
                } catch (e) {
                    this.$showTips({
                        message: e.message || e,
                        theme: 'error'
                    })
                }
            },
            refreshList () {
                this.$refs.pipelineTable?.refresh?.()
            }
        }
    }
</script>

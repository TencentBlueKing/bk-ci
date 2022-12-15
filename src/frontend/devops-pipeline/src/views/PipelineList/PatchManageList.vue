<template>
    <main class="pipeline-list-main">
        <h5 class="current-pipeline-group-name">{{currentViewName}}</h5>
        <header class="pipeline-list-main-header">
            <div>
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
                <bk-button class="exit-patch-text-btn" text @click="exitPatch">{{$t('exitPatch')}}</bk-button>

            </div>
            <div class="pipeline-list-main-header-right-area">
                <pipeline-searcher
                    v-model="filters"
                />
            </div>
        </header>
        <pipeline-table-view
            ref="pipelineTable"
            :fetch-pipelines="getPipelines"
            :filter-params="filters"
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

<template>
    <aside class="pipeline-group-aside">
        <header class="pipeline-group-aside-header">
            <div :class="{
                'pipeline-group-item': true,
                active: $route.params.viewId === ALL_PIPELINE_VIEW_ID
            }" @click="switchViewId(ALL_PIPELINE_VIEW_ID)">
                <logo class="pipeline-group-item-icon" size="12" name="group" />
                <span class="pipeline-group-item-name">
                    {{$t(ALL_PIPELINE_VIEW_ID)}}
                </span>
            </div>
            <div :class="{
                'pipeline-group-item': true,
                active: $route.params.viewId === DELETED_VIEW_ID
            }" @click="switchViewId(DELETED_VIEW_ID)">
                <logo class="pipeline-group-item-icon" size="12" name="delete" />
                <span class="pipeline-group-item-name">
                    {{$t('restore.recycleBin')}}
                </span>
            </div>
        </header>
        <article class="pipeline-group-container">
            <div class="pipeline-group-classify-block" v-for="block in pipelineGroupTree" :key="block.title">
                <h3 @click="toggle(block.id)" class="pipeline-group-classify-header">
                    <i class="devops-icon icon-down-shape pipeline-group-item-icon" />
                    {{block.title}}
                </h3>
                <div
                    :class="{
                        'pipeline-group-item': true,
                        'sticky-top': item.top,
                        active: $route.params.viewId === item.id
                    }"
                    v-if="block.show"
                    v-for="item in block.children"
                    :key="item.id"
                    @click="switchViewId(item.id)"
                >

                    <i v-if="item.icon" size="12" :class="`pipeline-group-item-icon devops-icon icon-${item.icon}`" />
                    <bk-input
                        v-if="item.id === editingGroupId"
                        v-bk-focus="1"
                        :disabled="renaming"
                        @blur="submitRename(item)"
                        v-model="newViewName"
                    />
                    <span v-else class="pipeline-group-item-name">
                        {{$t(item.name)}}
                    </span>
                    <span class="pipeline-group-item-sum">{{item.pipelineCount}}</span>
                    <ext-menu :class="{ hidden: item.actions.length <= 0 }" :data="item" :config="item.actions"></ext-menu>
                </div>
            </div>
        </article>
        <footer class="add-pipeline-group-footer">
            <bk-button text theme="primary" icon="plus" @click="showAddPipelineGroupDialog">
                {{$t('addPipelineGroup')}}
            </bk-button>
        </footer>
        <bk-dialog
            v-model="isAddPipelineGroupDialogShow"
            width="480"
            theme="primary"
            :mask-close="false"
            header-position="left"
            :title="$t('addPipelineGroup')"
            :loading="isAdding || checkingPermission"
            @confirm="submitPipelineAdd"
        >
            <bk-form v-bkloading="{ isLoading: checkingPermission || isAdding }" form-type="vertical" :model="newPipelineGroup">
                <bk-form-item property="name" required :label="$t('pipelineGroupName')">
                    <bk-input v-model="newPipelineGroup.name" />
                </bk-form-item>
                <bk-form-item required property="projected" :label="$t('visibleRange')">
                    <bk-radio-group class="pipeline-group-visible-range-group" v-model="newPipelineGroup.projected">
                        <bk-radio :value="false">{{$t('personalVis')}}</bk-radio>
                        <bk-radio :disabled="!canAddProjectedGroup" :value="true">{{$t('projectVis')}}</bk-radio>
                    </bk-radio-group>
                </bk-form-item>
            </bk-form>

        </bk-dialog>
        <pipeline-group-edit-dialog @close="handleCloseEditCount" :group="activeGroup" />
    </aside>

</template>

<script>
    import { mapActions, mapGetters } from 'vuex'
    import {
        PROCESS_API_URL_PREFIX,
        ALL_PIPELINE_VIEW_ID,
        DELETED_VIEW_ID
    } from '@/store/constants'
    import Logo from '@/components/Logo'
    import ExtMenu from '@/components/pipelineList/extMenu'
    import PipelineGroupEditDialog from '@/views/PipelineList/PipelineGroupEditDialog'

    export default {
        components: {
            Logo,
            ExtMenu,
            PipelineGroupEditDialog
        },
        data () {
            return {
                ALL_PIPELINE_VIEW_ID,
                DELETED_VIEW_ID,
                isLoading: false,
                isPatchOperate: false,
                checkingPermission: false,
                canAddProjectedGroup: false,
                editingGroupId: null,
                renaming: false,
                newViewName: '',
                showClassify: {
                    personalViewList: true,
                    projectViewList: true
                },

                isAdding: false,
                isAddPipelineGroupDialogShow: false,
                newPipelineGroup: {
                    name: '',
                    projected: false
                },
                activeGroup: null,
                isSticking: false,
                isDeleting: false
            }
        },
        computed: {
            ...mapGetters('pipelines', [
                'pipelineGroupDict',
                'groupMap',
                'hideActionGroups'
            ]),
            pipelineGroupTree () {
                return [{
                    title: `${this.$t('personalViewList')}(${this.pipelineGroupDict.personalViewList.length})`,
                    id: 'personalViewList',
                    show: this.showClassify.personalViewList,
                    children: this.pipelineGroupDict.personalViewList.map((view) => ({
                        ...view,
                        actions: this.pipelineGroupActions(view)
                    }))
                }, {
                    title: `${this.$t('projectViewList')}(${this.pipelineGroupDict.projectViewList.length})`,
                    id: 'projectViewList',
                    show: this.showClassify.projectViewList,
                    children: this.pipelineGroupDict.projectViewList.map((view) => ({
                        ...view,
                        actions: this.pipelineGroupActions(view)
                    }))
                }]
            }
        },
        created () {
            this.requestGetGroupLists(this.$route.params)
        },

        methods: {
            ...mapActions('pipelines', [
                'requestGetGroupLists',
                'addPipelineGroup',
                'updatePipelineGroup',
                'deletePipelineGroup',
                'toggleStickyTop'
            ]),
            pipelineGroupActions (group) {
                if (this.hideActionGroups.includes(group.id)) return []
                return [
                    {
                        text: this.$t('rename'),
                        disabled: this.renaming,
                        handler: (group) => {
                            this.editingGroupId = group.id
                            this.newViewName = group.name
                        }
                    },
                    {
                        text: this.$t('pipelineCountEdit'),
                        handler: () => {
                            this.activeGroup = group
                        }
                    },
                    {
                        text: this.$t('pipelineGroupAuth'),
                        handler: () => {
                            this.$router.push({
                                name: 'pipelineListAuth',
                                params: {
                                    viewId: group.id
                                }
                            })
                        }
                    },
                    {
                        text: this.$t(group.top ? 'unStickyTop' : 'stickyTop'),
                        disabled: this.isSticking,
                        handler: () => this.stickTop(group)
                    },
                    {
                        text: this.$t('delete'),
                        disabled: this.isDeleting,
                        handler: () => {
                            this.$bkInfo({
                                type: 'warning',
                                title: this.$t('deleteGroupTitle', [group.name]),
                                subTitle: this.$t('deleteGroupTips'),
                                cancelText: this.$t('close'),
                                confirmFn: () => {
                                    this.deleteGroup(group)
                                }
                            })
                        }
                    }
                ]
            },
            toggle (id) {
                console.log(id)
                this.showClassify[id] = !this.showClassify[id]
            },
            resetEditing () {
                this.editingGroupId = ''
                this.newViewName = ''
            },
            async submitRename (view) {
                if (this.renaming) return
                let message = this.$t('renameSuccess', [this.newViewName])
                let theme = 'success'
                if (this.newViewName === view.name) {
                    this.resetEditing()
                    return
                }
                try {
                    this.renaming = true
                    await this.updatePipelineGroup({
                        projectId: this.$route.params.projectId,
                        id: view.id,
                        projected: view.projected,
                        name: this.newViewName
                    })
                    this.resetEditing()
                } catch (error) {
                    message = error.message || error
                    theme = 'danger'
                } finally {
                    this.renaming = false
                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            async stickTop (view) {
                if (this.isSticking) return
                let message = view.top ? 'unStickyTop' : 'stickyTop'
                let theme = 'success'
                try {
                    this.isSticking = true
                    await this.toggleStickyTop({
                        projectId: this.$route.params.projectId,
                        viewId: view.id,
                        enabled: !view.top
                    })
                    message = this.$t(`${message}Success`, [view.name])
                } catch (error) {
                    message = error.message || error
                    theme = 'danger'
                } finally {
                    this.isSticking = false
                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            async deleteGroup (view) {
                if (this.isDeleting) return
                let message = 'deleteSuc'
                let theme = 'success'
                try {
                    this.isDeleting = true
                    await this.deletePipelineGroup({
                        projectId: this.$route.params.projectId,
                        ...view
                    })
                } catch (error) {
                    message = error.message || error
                    theme = 'danger'
                } finally {
                    this.isDeleting = false
                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            handleCloseEditCount () {
                this.activeGroup = null
            },
            showAddPipelineGroupDialog () {
                this.isAddPipelineGroupDialogShow = true
                this.checkHasProjectedGroupPermission()
            },
            async checkHasProjectedGroupPermission () {
                try {
                    this.checkingPermission = true
                    const { projectId } = this.$route.params
                    const res = await this.$ajax(`${PROCESS_API_URL_PREFIX}/user/pipelineViews/projects/${projectId}/checkPermission`)
                    this.canAddProjectedGroup = res
                } catch (error) {
                    this.canAddProjectedGroup = false
                } finally {
                    this.checkingPermission = false
                }
            },
            switchViewId (id) {
                this.$router.push({
                    params: {
                        ...this.$route.params,
                        viewId: id
                    }
                })
            },
            async submitPipelineAdd () {
                if (this.isAdding) return
                let message = this.$t('创建流水线组成功')
                let theme = 'success'
                try {
                    this.isAdding = true
                    await this.addPipelineGroup({
                        ...this.newPipelineGroup,
                        projectId: this.$route.params.projectId,
                        viewType: 2,
                        logic: 'AND',
                        filters: [],
                        pipelineIds: [],
                        pipelineCount: 0
                    })
                    this.isAddPipelineGroupDialogShow = false
                } catch (e) {
                    message = e.message || e
                    theme = 'danger'
                } finally {
                    this.isAdding = false
                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            }
        }
    }

</script>

<style lang="scss">
    @import '@/scss/mixins/ellipsis';
    @import '@/scss/conf';

    .pipeline-group-aside {
        display: flex;
        flex-direction: column;
        width: 280px;
        background: white;
        padding: 16px 16px 0 16px;
        border-right: 1px solid #DCDEE5;
        .pipeline-group-item-icon {
            display: inline-flex;
            margin-right: 10px;
        }
        .pipeline-group-aside-header {
            padding: 0 16px 16px 0;
            border-bottom: 1px solid #DCDEE5;
            box-sizing: content-box;
        }

        .pipeline-group-classify-block {
            padding-bottom: 24px;
            transition: all 0.3s ease;
            &:not(:last-child) {
                border-bottom: 1px solid #DCDEE5;
            }
        }
        .pipeline-group-classify-header {
            display: flex;
            align-items: center;
            height: 20px;
            margin-bottom: 8px;
            font-size: 14px;
            font-weight: normal;
            color: #979BA5;
        }
        .pipeline-group-container {
            flex: 1;
        }
        .add-pipeline-group-footer {
            display: flex;
            align-items: center;
            justify-content: center;
            height: 52px;
            border-top: 1px solid #DCDEE5;
        }
            .pipeline-group-item {
            display: flex;
            align-items: center;
            justify-content: space-between;
            height: 40px;
            padding: 0 16px;
            font-size: 14px;
            cursor: pointer;
            &.-header {
                padding-bottom: 16px;
                margin-bottom: 16px;
                border-bottom: 1px solid #EAEBF0;
            }
            .pipeline-group-item-name {
                flex: 1;
                @include ellipsis();
            }
            .pipeline-group-item-sum {
                @include ellipsis();
                width: 30px;
                font-size: 12px;
                background: #F0F1F5;
                border-radius: 8px;
                text-align: center;
                margin: 0 6px;
                color: $fontWeightColor;
            }

            &.sticky-top {
                background-color: #F5F7FA;
            }

            &:hover,
            &.active {
                background: #E1ECFF;
                color: $primaryColor;
            }

        }
    }

</style>

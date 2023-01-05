<template>
    <aside v-bkloading="{ isLoading }" class="pipeline-group-aside">
        <div class="pipeline-group-aside-main">
            <header class="pipeline-group-aside-header">
                <div :class="{
                    'pipeline-group-item': true,
                    active: $route.params.viewId === sumView.id
                }" @click="switchViewId(sumView.id)">
                    <logo class="pipeline-group-item-icon" size="12" :name="sumView.icon" />
                    <span class="pipeline-group-item-name">
                        {{$t(sumView.name)}}
                    </span>
                    <span v-if="sumView.pipelineCount" class="pipeline-group-item-sum group-header-sum">{{sumView.pipelineCount}}</span>
                </div>
            </header>
            <article class="pipeline-group-container">
                <template v-for="block in pipelineGroupTree">
                    <h3
                        @click="toggle(block.id)"
                        class="pipeline-group-classify-header"
                        :style="`top: ${block.stickyTop}`"
                        :key="block.title"
                    >
                        <i :class="['devops-icon', 'pipeline-group-item-icon', {
                            'icon-down-shape': block.show,
                            'icon-right-shape': !block.show
                        }]"
                        />
                        <span class="pipeline-group-header-name">{{block.title}}</span>
                        <span v-bk-tooltips="block.tooltips">
                            <bk-button
                                text
                                theme="primary"
                                class="add-pipeline-group-btn"
                                :disabled="block.disabled"
                                @click.stop="showAddPipelineGroupDialog(block.projected)"
                            >
                                <logo name="increase" size="16"></logo>
                            </bk-button>
                        </span>
                    </h3>
                    <div class="pipeline-group-block" :key="block.title">
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
                            <logo v-if="item.icon" size="12" class="pipeline-group-item-icon" :name="item.icon" />
                            <bk-input
                                v-if="item.id === editingGroupId"
                                v-bk-focus="1"
                                :disabled="renaming"
                                @blur="submitRename(item)"
                                @enter="submitRename(item)"
                                v-model.trim="newViewName"
                            />
                            <span v-else class="pipeline-group-item-name">
                                {{item.name}}
                            </span>
                            <span
                                v-if="$route.params.viewId === item.id && currentPipelineCountDetail.deleteCount > 0"
                                class="pipeline-group-item-sum has-delete-count"
                            >
                                <span class="normal-count">{{currentPipelineCountDetail.normalCount}}</span>
                                <span class="delete-count">
                                    <logo name="delete" size="8" />
                                    {{currentPipelineCountDetail.deleteCount}}
                                </span>
                            </span>
                            <span v-else class="pipeline-group-item-sum">
                                {{item.pipelineCount}}
                            </span>
                            <span @click.stop>
                                <ext-menu :class="{ hidden: item.actions.length <= 0 }" :data="item" :config="item.actions" />
                            </span>
                        </div>
                    </div>
                </template>
            </article>
        </div>
        <footer :class="['recycle-pipeline-group-footer', {
            active: $route.params.viewId === DELETED_VIEW_ID
        }]" @click="goRecycleBin">
            <logo class="pipeline-group-item-icon" name="delete" size="16"></logo>
            <span>{{$t('restore.recycleBin')}}</span>
        </footer>
        <bk-dialog
            v-model="isAddPipelineGroupDialogShow"
            width="480"
            theme="primary"
            :mask-close="false"
            header-position="left"
            :title="$t('addPipelineGroup')"
            :loading="isAdding"
            @cancel="closeAddPipelineGroupDialog"
        >
            <bk-form ref="newPipelineGroupForm" v-bkloading="{ isLoading: isAdding }" form-type="vertical" :model="newPipelineGroup">
                <bk-form-item property="name" :rules="groupNameRules" :label="$t('pipelineGroupName')">
                    <bk-input v-model.trim="newPipelineGroup.name" />
                </bk-form-item>
                <bk-form-item required property="projected" :label="$t('visibleRange')">
                    <bk-radio-group class="pipeline-group-visible-range-group" v-model="newPipelineGroup.projected">
                        <bk-radio :value="false">{{$t('personalVis')}}</bk-radio>
                        <bk-radio
                            v-bk-tooltips="projectedGroupDisableTips"
                            :disabled="!isManage"
                            :value="true"
                        >
                            {{$t('projectVis')}}
                        </bk-radio>
                    </bk-radio-group>
                </bk-form-item>
            </bk-form>
            <footer slot="footer">
                <bk-button
                    theme="primary"
                    :disabled="!isValidGroupName"
                    @click="submitPipelineAdd"
                >
                    {{$t('confirm')}}
                </bk-button>
                <bk-button @click="closeAddPipelineGroupDialog">
                    {{$t('cancel')}}
                </bk-button>
            </footer>
        </bk-dialog>

    </aside>

</template>

<script>
    import { mapActions, mapGetters, mapState } from 'vuex'
    import {
        DELETED_VIEW_ID,
        UNCLASSIFIED_PIPELINE_VIEW_ID
    } from '@/store/constants'
    import { cacheViewId } from '@/utils/util'
    import Logo from '@/components/Logo'
    import ExtMenu from '@/components/pipelineList/extMenu'

    export default {
        components: {
            Logo,
            ExtMenu
        },
        data () {
            return {
                DELETED_VIEW_ID,
                isLoading: false,
                isPatchOperate: false,
                editingGroupId: null,
                renaming: false,
                newViewName: '',
                showClassify: {
                    personalViewList: true,
                    projectViewList: true
                },
                isAdding: false,
                isAddPipelineGroupDialogShow: false,
                isValidGroupName: false,
                newPipelineGroup: {
                    name: '',
                    projected: false
                },
                isSticking: false,
                isDeleting: false
            }
        },
        computed: {
            ...mapState('pipelines', [
                'sumView',
                'isManage',
                'hardViews'
            ]),
            ...mapGetters('pipelines', [
                'pipelineGroupDict',
                'groupMap',
                'fixedGroupIdSet',
                'groupNamesMap'
            ]),
            groupNameRules () {
                return [{
                    validator: this.checkGroupNameValid,
                    message: (val) => {
                        switch (true) {
                            case val.length === 0:
                                return this.$t('groupNameNotAllowEmpty')
                            case val.length > 16:
                                return this.$t('groupNameTooLong')
                            default:
                                return this.$t('pipelineGroupRepeatTips', [val])
                        }
                    },
                    trigger: 'change'
                }]
            },
            pipelineGroupTree () {
                return [{
                    title: `${this.$t('personalViewList')}(${this.pipelineGroupDict.personalViewList.length - this.hardViews.length})`,
                    id: 'personalViewList',
                    show: this.showClassify.personalViewList,
                    tooltips: {
                        disalbed: true
                    },
                    stickyTop: '66px',
                    children: this.pipelineGroupDict.personalViewList.map((view) => ({
                        ...view,
                        icon: view.icon ?? 'pipelineGroup',
                        name: view.i18nKey ? this.$t(view.i18nKey) : view.name,
                        actions: this.pipelineGroupActions(view)
                    }))
                }, {
                    title: `${this.$t('projectViewList')}(${this.pipelineGroupDict.projectViewList.length - 1})`,
                    id: 'projectViewList',
                    show: this.showClassify.projectViewList,
                    projected: true,
                    disabled: !this.isManage,
                    tooltips: this.projectedGroupDisableTips,
                    stickyTop: '106px',
                    children: this.pipelineGroupDict.projectViewList.map((view) => ({
                        ...view,
                        icon: view.id === UNCLASSIFIED_PIPELINE_VIEW_ID ? 'unGroup' : 'pipelineGroup',
                        actions: this.pipelineGroupActions(view)
                    }))
                }]
            },
            projectedGroupDisableTips () {
                return {
                    content: this.$t('projectedGroupDisableTips'),
                    disabled: this.isManage
                }
            },
            currentPipelineCountDetail () {
                const viewId = this.$route.params.viewId
                const currentGroup = this.groupMap[viewId]
                return currentGroup?.pipelineCountDetail ?? currentGroup.pipelineCount ?? 0
            }
        },
        watch: {
            '$route.params.projectId': function () {
                this.closeAddPipelineGroupDialog()
                this.$nextTick(this.refreshPipelineGroup)
            }
        },
        created () {
            this.refreshPipelineGroup()
        },
        methods: {
            ...mapActions('pipelines', [
                'requestGetGroupLists',
                'addPipelineGroup',
                'updatePipelineGroup',
                'deletePipelineGroup',
                'toggleStickyTop',
                'requestGroupPipelineCount'
            ]),
            checkGroupNameValid (name) {
                const valid = this.newPipelineGroup.projected !== this.groupNamesMap[name]?.projected && name.length <= 16 && name.length > 0
                this.isValidGroupName = valid
                return valid
            },
            goRecycleBin () {
                this.switchViewId(DELETED_VIEW_ID)
            },
            async refreshPipelineGroup () {
                this.isLoading = true
                const res = await this.requestGetGroupLists(this.$route.params)
                this.isLoading = false
                return res
            },
            pipelineGroupActions (group) {
                if (this.fixedGroupIdSet.has(group.id)) return []
                const hasPermission = !group.projected || this.isManage
                return [
                    ...(hasPermission
                        ? [
                            {
                                text: this.$t('rename'),
                                disabled: this.renaming,
                                handler: (group) => {
                                    this.editingGroupId = group.id
                                    this.newViewName = group.name
                                }
                            }
                        ]
                        : []),
                    // {
                    //     text: this.$t('pipelineGroupAuth'),
                    //     handler: () => {
                    //         this.$router.push({
                    //             name: 'pipelineListAuth',
                    //             params: {
                    //                 viewId: group.id
                    //             }
                    //         })
                    //     }
                    // },
                    {
                        text: this.$t(group.top ? 'unStickyTop' : 'stickyTop'),
                        disabled: this.isSticking,
                        handler: () => this.stickTop(group)
                    },
                    ...(hasPermission
                        ? [
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
                        : [])
                ]
            },
            toggle (id) {
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
                    if (!this.newViewName) {
                        throw new Error(this.$t('groupNameNotAllowEmpty'))
                    }
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
                    console.log(message)
                    theme = 'error'
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
                    this.refreshPipelineGroup()
                } catch (error) {
                    message = error.message || error
                    theme = 'error'
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
                let message = this.$t('deleteSuc')
                let theme = 'success'
                try {
                    this.isDeleting = true
                    await this.deletePipelineGroup({
                        projectId: this.$route.params.projectId,
                        ...view
                    })
                    this.requestGetGroupLists(this.$route.params)
                    this.switchViewId(UNCLASSIFIED_PIPELINE_VIEW_ID)
                } catch (error) {
                    message = error.message || error
                    theme = 'error'
                } finally {
                    this.isDeleting = false
                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            showAddPipelineGroupDialog (isProjected = false) {
                this.isAddPipelineGroupDialogShow = true
                this.newPipelineGroup.projected = isProjected
            },
            closeAddPipelineGroupDialog () {
                this.isAddPipelineGroupDialogShow = false
                this.isValidGroupName = false
                Object.assign(this.newPipelineGroup, {
                    name: '',
                    projected: false
                })
                this.$refs.newPipelineGroupForm?.clearError?.()
            },
            updateGroupPipelineCount (viewId) {
                this.requestGroupPipelineCount({
                    projectId: this.$route.params.projectId,
                    viewId
                })
            },
            switchViewId (viewId) {
                if (viewId !== this.$route.params.viewId) {
                    this.updateGroupPipelineCount(viewId)

                    cacheViewId(this.$route.params.projectId, viewId)
                    this.$router.push({
                        name: 'PipelineManageList',
                        params: {
                            ...this.$route.params,
                            viewId
                        }
                    })
                }
            },
            async submitPipelineAdd () {
                if (this.isAdding) return false
                const formValid = await this.$refs.newPipelineGroupForm?.validate?.()

                if (!formValid) {
                    return false
                }
                let message = this.$t('addPipelineGroupSuc')
                let theme = 'success'
                try {
                    this.isAdding = true
                    const viewId = await this.addPipelineGroup({
                        ...this.newPipelineGroup,
                        projectId: this.$route.params.projectId,
                        viewType: 2,
                        logic: 'AND',
                        filters: [],
                        pipelineIds: [],
                        pipelineCount: 0
                    })
                    this.closeAddPipelineGroupDialog()
                    this.switchViewId(viewId)
                } catch (e) {
                    message = e.message || e
                    theme = 'error'
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
        padding: 0;
        border-right: 1px solid #DCDEE5;
        .pipeline-group-item-icon {
            display: inline-flex;
            margin-right: 10px;
            color: #C4C6CC;

        }
        .pipeline-group-aside-main {
            flex: 1;
            display: flex;
            flex-direction: column;
            overflow: overlay;
        }
        .pipeline-group-aside-header {
            border-bottom: 1px solid #DCDEE5;
            box-sizing: content-box;
            position: sticky;
            top: 0;
            padding: 16px 0 10px 0;
            z-index: 2;
            background: white;
            >.pipeline-group-item {
                padding-right: 38px;
            }
        }

        .pipeline-group-classify-header {
            display: flex;
            align-items: center;
            height: 40px;
            padding: 0 16px;
            font-size: 14px;
            font-weight: normal;
            color: #979BA5;
            position: sticky;
            margin: 0;
            background: white;
            z-index: 1;

            .pipeline-group-header-name {
                flex: 1;
            }
            .add-pipeline-group-btn {
                display: flex;
                align-items: center;
                font-size: 0;
            }
        }
        .pipeline-group-block {
            transition: all 0.3s ease;
            padding-bottom: 12px;
            &:not(:last-child) {
                border-bottom: 1px solid #DCDEE5;
            }
        }
        
        .recycle-pipeline-group-footer {
            display: flex;
            align-items: center;
            height: 52px;
            border-top: 1px solid #DCDEE5;
            padding: 0 0 0 32px;
            cursor: pointer;
            font-size: 14px;
            &:hover,
            &.active {
                color: $primaryColor;
                .pipeline-group-item-icon {
                    color: $primaryColor;
                }
            }
        }
        .pipeline-group-item {
            display: flex;
            align-items: center;
            justify-content: space-between;
            height: 40px;
            padding: 0 16px 0 32px;
            font-size: 14px;
            cursor: pointer;
            &.-header {
                padding: 0 16px 16px 16px;
                margin-bottom: 16px;
                border-bottom: 1px solid #EAEBF0;
            }
            .pipeline-group-item-name {
                flex: 1;
                @include ellipsis();
            }
            .pipeline-group-item-sum {
                display: flex;
                font-size: 12px;
                background: #F0F1F5;
                border-radius: 8px;

                justify-content: center;
                width: 30px;
                margin: 0 6px;
                color: #979BA5;
                transition: all .3s ease;
                &.has-delete-count {
                    width: 60px;
                    .normal-count {
                        background: white;
                    }
                }
                .normal-count,
                .delete-count {
                    @include ellipsis();
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    min-width: auto;
                    border-radius: 8px;
                    width: 30px;
                    &.delete-count {
                        color: #C4C6CC;
                        >:first-child {
                            margin-right: 2px;
                        }
                    }
                }
            }

            &.sticky-top {
                background-color: #F5F7FA;
            }
            &:hover {
                background: #F5F7FA;
            }

            &.active {
                background: #E1ECFF;
                .pipeline-group-item-icon,
                .pipeline-group-item-sum,
                .pipeline-group-item-name {
                    color: $primaryColor;
                }
                .pipeline-group-item-sum:not(.has-delete-count) {
                    background: white;
                }
            }

        }
    }

</style>

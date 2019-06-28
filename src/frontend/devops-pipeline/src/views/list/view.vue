<template>
    <div class="pipeline-view-list" v-bkloading="{
        isLoading: loading.isLoading,
        title: loading.title
    }">
        <template v-if="showContent && viewList.length">
            <div class="view-list-content">
                <div class="info-header">
                    <bk-button theme="primary" @click="createView()">新建视图</bk-button>
                </div>
                <div class="view-table-wrapper">
                    <bk-table
                        :data="viewList"
                        size="small">
                        <bk-table-column label="名称" prop="name"></bk-table-column>
                        <bk-table-column label="类型" prop="projectId" :formatter="showViewType"></bk-table-column>
                        <bk-table-column label="描述" prop="creator"></bk-table-column>
                        <bk-table-column label="操作" width="150">
                            <template slot-scope="props">
                                <bk-button theme="primary" text :disabled="!isManagerUser && props.row.projected" @click="editView(props.row)">编辑</bk-button>
                                <bk-button theme="primary" text :disabled="!isManagerUser && props.row.projected" @click="deleteView(props.row)">删除</bk-button>
                            </template>
                        </bk-table-column>
                    </bk-table>
                </div>
            </div>
        </template>
        <empty-tips v-if="showContent && !viewList.length"
            :title="emptyTipsConfig.title"
            :desc="emptyTipsConfig.desc"
            :btns="emptyTipsConfig.btns">
        </empty-tips>
        <create-view-dialog @updateViewList="requestViewList"></create-view-dialog>
    </div>
</template>

<script>
    import { mapActions, mapMutations } from 'vuex'
    import emptyTips from '@/components/pipelineList/imgEmptyTips'
    import createViewDialog from '@/components/createViewDialog'
    import { navConfirm } from '@/utils/util'

    export default {
        components: {
            emptyTips,
            createViewDialog
        },
        data () {
            return {
                isManagerUser: true,
                showContent: false,
                viewList: [],
                loading: {
                    isLoading: false,
                    title: ''
                },
                emptyTipsConfig: {
                    title: '创建第一个视图',
                    desc: '设置合理的视图，有助于你快速定位到自己想找的流水线',
                    btns: [
                        {
                            theme: 'primary',
                            size: 'normal',
                            handler: () => this.createView(),
                            text: '新建视图'
                        }
                    ]
                }
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            }
        },
        async mounted () {
            await this.init()
        },
        methods: {
            ...mapActions('pipelines', [
                'requestViewSettingInfo'
            ]),
            ...mapMutations('pipelines', [
                'updateViewSettingInfo'
            ]),
            /**
             *  初始化页面数据
             */
            async init () {
                await this.requestGrouptLists()
                await this.requestViewList()
            },
            /** *
             * 获取标签及其分组
             */
            async requestGrouptLists () {
                const { $store } = this
                try {
                    const res = await $store.dispatch('pipelines/requestGetGroupLists', {
                        projectId: this.projectId
                    })
                    $store.commit('pipelines/updateGroupLists', res)
                } catch (err) {
                    this.$showTips({
                        message: err.message || err,
                        theme: 'error'
                    })
                }
            },
            async requestViewSetting () {
                try {
                    const viewSetting = await this.requestViewSettingInfo({ projectId: this.projectId })
                    if (viewSetting.currentViewId) {
                        this.updateViewSettingInfo(viewSetting)
                    }
                } catch (err) {
                    this.$showTips({
                        theme: 'error',
                        message: err.message || err
                    })
                }
            },
            /** *
             * 获取流水线视图列表
             */
            async requestViewList (flag) {
                const { $store, loading } = this

                loading.isLoading = true

                try {
                    const res = await $store.dispatch('pipelines/requestPipelineViewList', {
                        projectId: this.projectId
                    })
                    this.viewList.splice(0, this.viewList.length)
                    res.map(item => {
                        this.viewList.push(item)
                    })
                    if (flag) {
                        await this.requestViewSetting()
                    }
                } catch (err) {
                    this.$showTips({
                        message: err.message || err,
                        theme: 'error'
                    })
                } finally {
                    this.loading.isLoading = false
                    this.showContent = true
                }
            },
            /** *
             * 编辑视图
             */
            editView (row) {
                if ((this.isManagerUser && row.projected) || !row.projected) {
                    const obj = {
                        id: row.id,
                        projected: false,
                        name: '',
                        logic: 'AND',
                        filters: [
                            { id: 'filterByName', name: '流水线名称', '@type': 'filterByName', pipelineName: '' }
                        ]
                    }
                    this.$store.commit('pipelines/updateViewForm', obj)
                    this.$store.commit('pipelines/toggleViewCreateDialog', true)
                }
            },
            async deletePipelineView (view) {
                let message, theme
                const {
                    loading
                } = this

                loading.isLoading = true
                try {
                    await this.$store.dispatch('pipelines/deletePipelineView', {
                        projectId: this.projectId,
                        viewId: view.id
                    })

                    this.requestViewList('flag')
                    message = '删除视图成功'
                    theme = 'success'
                } catch (err) {
                    message = err.message || err
                    theme = 'error'
                } finally {
                    message && this.$showTips({
                        message,
                        theme
                    })
                    loading.isLoading = false
                }
            },
            /**
             *  删除视图
             */
            deleteView (view) {
                if ((this.isManagerUser && view.projected) || !view.projected) {
                    const content = `删除${view.name}`

                    navConfirm({ title: `确认`, content })
                        .then(() => {
                            this.deletePipelineView(view)
                        }).catch(() => {})
                }
            },
            createView () {
                const obj = {
                    type: 'PERSION',
                    title: '',
                    condition: 'VERSUS',
                    viewFilterList: [
                        { id: 'filterByName', name: '流水线名称', '@type': 'filterByName', pipelineName: '' }
                    ]
                }
                this.$store.commit('pipelines/updateViewForm', obj)
                this.$store.commit('pipelines/toggleViewCreateDialog', true)
            },
            showViewType (row, cell, value) {
                return value ? '项目视图' : '个人视图'
            }
        }
    }
</script>

<style lang='scss'>
    @import './../../scss/conf';
    .pipeline-view-list {
        min-height: calc(100% - 40px);
        .view-list-content {
            margin: 0 auto;
            padding-top: 16px;
            width: 1080px;
        }
        .view-table-wrapper {
            margin-top: 16px;
        }
    }
</style>

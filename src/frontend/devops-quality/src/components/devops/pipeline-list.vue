<template>
    <bk-dialog v-model="showPipelineList"
        :width="'900'"
        :position="{ top: 100 }"
        :ext-cls="'pipeline-list-wrapper'"
        :close-icon="pipelineListConf.closeIcon"
        :show-footer="pipelineListConf.hasFooter">
        <div v-bkloading="{
            isLoading: loading
        }">
            <div class="pipeline-list-header">
                <div class="title">{{$t('quality.选择流水线')}}</div>
                <div><i class="devops-icon icon-close" @click="cancel()" style="color: #C3CDD7;"></i></div>
            </div>
            <div class="query-pipeline-row">
                <!-- <bk-select v-model="sortType">
                    <bk-option-group
                        v-for="(group, index) in pipelineViewList"
                        :name="group.name"
                        :key="index">
                        <bk-option v-for="(option, optionIndex) in group.children"
                            :key="optionIndex"
                            :id="option.id"
                            :name="option.name">
                        </bk-option>
                    </bk-option-group>
                </bk-select> -->
                <div class="search-input-row">
                    <input
                        class="bk-form-input"
                        type="text"
                        v-model="searchName"
                        @keyup.enter="toSearchPipeline()"
                    >
                    <i class="devops-icon icon-search" @click="toSearchPipeline()"></i>
                </div>
            </div>
            <div class="pipeline-content">
                <bk-table
                    size="small"
                    ref="effectPipeline"
                    class="effect-pipeline-table"
                    :data="pipelineList"
                    :empty-text="$t('quality.暂无数据')"
                    :pagination="pagination"
                    @page-change="handlePageChange"
                    @select="selectItem"
                    @select-all="selectAll">
                    <bk-table-column type="selection" width="60" align="center"></bk-table-column>
                    <bk-table-column :label="$t('quality.流水线名称')" prop="pipelineName" width="360">
                        <template slot-scope="props">
                            <span :title="props.row.pipelineName">{{props.row.pipelineName}}</span>
                        </template>
                    </bk-table-column>
                    <bk-table-column :label="$t('quality.插件个数')" prop="taskCount">
                        <template slot-scope="props">
                            <span>{{props.row.taskCount}}</span>
                        </template>
                    </bk-table-column>
                    <bk-table-column :label="$t('quality.执行总次数')" prop="buildCount">
                        <template slot-scope="props">
                            <span>{{props.row.buildCount}}</span>
                        </template>
                    </bk-table-column>
                    <bk-table-column :label="$t('quality.最后执行时间')" prop="latestBuildStartTime">
                        <template slot-scope="props">
                            <span>{{!props.row.latestBuildStartTime ? '-' : localConvertTime(props.row.latestBuildStartTime)}}</span>
                        </template>
                    </bk-table-column>
                </bk-table>
            </div>
            <div class="footer">
                <button class="bk-button bk-primary" type="button" @click="confirm()">{{$t('quality.确认')}}</button>
                <button class="bk-button bk-default" type="button" @click="cancel()">{{$t('quality.取消')}}</button>
            </div>
        </div>
    </bk-dialog>
</template>

<script>
    import { convertTime } from '@/utils/util'

    export default {
        props: {
            isShow: {
                type: Boolean,
                default: false
            },
            selectedPielines: {
                type: Array,
                default: []
            }
        },
        data () {
            return {
                showPipelineList: false,
                loading: false,
                isInit: false,
                sortType: '',
                pipelineList: [],
                localSelected: [],
                pipelineViewList: [],
                pagination: {
                    current: 1,
                    count: 0,
                    limit: 10,
                    showLimit: false
                },
                pipelineListConf: {
                    closeIcon: false,
                    hasHeader: false,
                    hasFooter: false,
                    quickClose: false
                }
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            }
        },
        watch: {
            isShow (newVal) {
                this.showPipelineList = newVal
                if (newVal) {
                    this.isInit = true
                    this.pagination.current = 1
                    this.requestList(this.pagination.current, this.pagination.limit)
                } else {
                    this.searchName = ''
                    this.sortType = ''
                }
            },
            pipelineList: {
                deep: true,
                handler (val) {
                    this.allNodeSelected = val.every(item => {
                        return item.isSelected
                    })
                }
            },
            sortType (newVal) {
                if (this.isShow) {
                    this.pagination.current = 1
                    this.requestList(this.pagination.current, this.pagination.limit, true)
                }
            }
        },
        created () {
            // this.requestViewList()
        },
        methods: {
            // async requestViewList () {
            //     try {
            //         const res = await this.$store.dispatch('quality/requestPipelineViews', {
            //             projectId: this.projectId
            //         })
                    
            //         this.pipelineViewList.splice(0, this.pipelineViewList.length)

            //         res.map(item => {
            //             if (item.viewList.length) {
            //                 this.pipelineViewList.push({
            //                     name: item.label,
            //                     children: item.viewList
            //                 })
            //             }
            //         })
            //     } catch (err) {
            //         const message = err.message ? err.message : err
            //         const theme = 'error'

            //         this.$bkMessage({
            //             message,
            //             theme
            //         })
            //     }
            // },
            async requestList (page, pageSize, isPageTurn) {
                const params = {
                    viewId: this.sortType || undefined,
                    keywords: this.searchName && this.searchName.trim(),
                    page,
                    pageSize
                }

                if (!isPageTurn) {
                    this.localSelected = JSON.parse(JSON.stringify(this.selectedPielines))
                }
                
                this.loading = true
                try {
                    const res = await this.$store.dispatch('quality/requestViewPipelines', {
                        projectId: this.projectId,
                        params
                    })
                    
                    if (res.records) {
                        this.pipelineList.splice(0, this.pipelineList.length, ...res.records || [])
                        
                        this.localSelected.forEach(selection => {
                            const matchItem = this.pipelineList.find(val => val.pipelineId === selection)
                            if (matchItem) {
                                this.$refs.effectPipeline.toggleRowSelection(matchItem, true)
                            }
                        })
                        this.pagination.count = res.count
                    }
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                } finally {
                    this.loading = false
                }
            },
            handlePageChange (page) {
                this.pagination.current = page
                this.requestList(this.pagination.current, this.pagination.limit, true)
            },
            selectItem (selection, row) {
                const curPageData = this.localSelected.filter(item => !this.pipelineList.some(pipeline => pipeline.pipelineId === item))
                this.localSelected.splice(0, this.localSelected.length)
                selection.map(item => this.localSelected.push(item.pipelineId))
                this.localSelected = this.localSelected.concat(curPageData)
            },
            selectAll (selection) {
                if (selection.length) {
                    selection.forEach(item => {
                        if (!this.localSelected.includes(item.pipelineId)) {
                            this.localSelected.push(item.pipelineId)
                        }
                    })
                } else {
                    this.localSelected = this.localSelected.filter(item => !this.pipelineList.some(pipeline => item === pipeline.pipelineId))
                }
            },
            toSearchPipeline () {
                this.pagination.current = 1
                this.requestList(this.pagination.current, this.pagination.limit, true)
            },
            /**
             * 处理时间格式
             */
            localConvertTime (timestamp) {
                return convertTime(timestamp)
            },
            confirm () {
                this.$emit('comfire', this.localSelected)
            },
            cancel () {
                this.$emit('close')
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '@/scss/conf.scss';

    .pipeline-list-wrapper{
        .pipeline-list-header {
            padding: 20px 24px 8px;
            display: flex;
            justify-content: space-between;
            border-bottom: 0;
            .title {
                line-height: 20px;
                font-size: 20px;
                color: #333C48;
            }
            .icon-close {
                position: absolute;
                top: 24px;
                right: 24px;
                font-size: 12px;
                cursor: pointer;
            }
        }
        .query-pipeline-row {
            display: flex;
            // justify-content: space-between;
            justify-content: flex-end;
            padding: 0 24px 12px;
            .bk-select {
                width: 210px;
                height: 30px;
            }
        }
        .search-input-row {
            position: relative;
            padding: 0 10px;
            width: 347px;
            height: 30px;
            border: 1px solid #dde4eb;
            background-color: #fff;
            .bk-form-input {
                padding: 0;
                border: 0;
                -webkit-box-shadow: border-box;
                box-shadow: border-box;
                outline: none;
                width: 300px;
                height: 28px;
                margin-left: 0;
            }
            .icon-search {
                float: right;
                margin-top: 8px;
                color: #c3cdd7;
                cursor: pointer;
            }
        }
        .pipeline-content {
            height: 546px;
            padding: 0 24px;
            overflow: hidden;
        }
        .ci-paging {
            margin-right: 0;
        }
        .footer {
            padding: 12px 24px;
            text-align: right;
            background: #FAFBFD;
            border-top: 1px solid #DDE4EB;
        }
        .ci-paging {
            margin: 10px;
            margin-right: 0;
        }
    }
</style>

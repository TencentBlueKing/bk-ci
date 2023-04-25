<template>
    <bk-dialog v-model="showTemplateList"
        :width="'900'"
        :position="{ top: 100 }"
        :ext-cls="'template-list-wrapper'"
        :close-icon="templateListConf.closeIcon"
        :show-footer="templateListConf.hasFooter">
        <div v-bkloading="{
            isLoading: loading
        }">
            <div class="template-list-header">
                <div class="title">{{$t('quality.选择模板')}}</div>
                <div><i class="devops-icon icon-close" @click="cancel()" style="color: #C3CDD7;"></i></div>
            </div>
            <div class="query-template-row">
                <span class="template-prompt">{{$t('quality.选中后将对该模板当前和后续实例化的流水线生效')}}</span>
                <div class="search-input-row">
                    <input class="bk-form-input" type="text"
                        v-model="searchName"
                        @keyup.enter="toSearchPipeline()">
                    <i class="devops-icon icon-search" @click="toSearchPipeline()"></i>
                </div>
            </div>
            <div class="template-content">
                <bk-table
                    size="small"
                    ref="effectTemplate"
                    class="effect-pipeline-table"
                    :data="templateList"
                    :empty-text="$t('quality.暂无数据')"
                    :pagination="pagination"
                    @page-change="handlePageChange"
                    @select="selectItem"
                    @select-all="selectAll">
                    <bk-table-column type="selection" width="60" align="center"></bk-table-column>
                    <bk-table-column :label="$t('quality.模板名称')" prop="name" width="260">
                        <template slot-scope="props">
                            <span :title="props.row.name">{{props.row.name}}</span>
                        </template>
                    </bk-table-column>
                    <bk-table-column :label="$t('quality.来源')" prop="templateType">
                        <template slot-scope="props">
                            <span>{{templateTypeFilter(props.row.templateType)}}</span>
                        </template>
                    </bk-table-column>
                    <bk-table-column :label="$t('quality.关联代码库')" width="320">
                        <template slot-scope="props">
                            <section class="codelib-box" :title="handleFormat(props.row.associateCodes)">
                                <div class="codelib-item" v-for="(entry, eIndex) in (props.row.associateCodes || []).slice(0, 3)" :key="eIndex">{{entry}}</div>
                            </section>
                        </template>
                    </bk-table-column>
                    <bk-table-column :label="$t('quality.流水线实例')" prop="associatePipelines">
                        <template slot-scope="props">
                            <a class="instance-count"
                                target="_blank"
                                :href="`/console/pipeline/${projectId}/template/${props.row.templateId}/instance`"
                            >{{props.row.associatePipelines.length}}
                            </a>
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
    export default {
        props: {
            isShow: {
                type: Boolean,
                default: false
            },
            selectedTemplates: {
                type: Array,
                default: []
            }
        },
        data () {
            return {
                showTemplateList: false,
                loading: false,
                sortType: '',
                templateList: [],
                localSelected: [],
                pagination: {
                    current: 1,
                    count: 0,
                    limit: 10,
                    showLimit: false
                },
                templateListConf: {
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
                this.showTemplateList = newVal
                if (newVal) {
                    this.pagination.current = 1
                    this.requestList(this.pagination.current, this.pagination.limit)
                } else {
                    this.searchName = ''
                }
            }
        },
        methods: {
            handleFormat (codes) {
                let tips = ''
                codes.forEach(item => {
                    tips += `${item}\n`
                })

                return tips
            },
            templateTypeFilter (val) {
                let res = ''
                switch (val) {
                    case 'constraint':
                    case 'CONSTRAINT':
                        res = this.$t('quality.研发商店')
                        break
                    default:
                        res = this.$t('quality.自定义')
                        break
                }
                return res
            },
            async requestList (page, pageSize, isPageTurn) {
                const params = {
                    keywords: this.searchName && this.searchName.trim(),
                    page,
                    pageSize
                }

                if (!isPageTurn) {
                    this.localSelected = JSON.parse(JSON.stringify(this.selectedTemplates))
                }
                
                this.loading = true
                try {
                    const res = await this.$store.dispatch('quality/requestPipelineTemplate', {
                        projectId: this.projectId,
                        params
                    })
                    
                    if (res.models.length) {
                        this.templateList.splice(0, this.templateList.length, ...res.models || [])
                        
                        this.localSelected.forEach(selection => {
                            const matchItem = this.templateList.find(val => val.templateId === selection)
                            if (matchItem) {
                                this.$refs.effectTemplate.toggleRowSelection(matchItem, true)
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
                const curPageData = this.localSelected.filter(item => !this.templateList.some(template => template.templateId === item))
                this.localSelected.splice(0, this.localSelected.length)
                selection.map(item => this.localSelected.push(item.templateId))
                this.localSelected = this.localSelected.concat(curPageData)
            },
            selectAll (selection) {
                if (selection.length) {
                    selection.forEach(item => {
                        if (!this.localSelected.includes(item.templateId)) {
                            this.localSelected.push(item.templateId)
                        }
                    })
                } else {
                    this.localSelected = this.localSelected.filter(item => !this.templateList.some(pipeline => item === pipeline.templateId))
                }
            },
            toSearchPipeline () {
                this.pagination.current = 1
                this.requestList(this.pagination.current, this.pagination.limit, true)
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

<style lang="scss">
    @import '@/scss/conf.scss';

    .template-list-wrapper{
        .bk-dialog-tool {
            display: none;
        }
        .bk-dialog-body {
            padding: 0;
        }
        .template-list-header {
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
        .query-template-row {
            display: flex;
            justify-content: space-between;
            padding: 0 24px 12px;
            .bk-select {
                width: 210px;
                height: 30px;
            }
        }
        .template-prompt {
            line-height: 30px;
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
        .template-content {
            min-height: 546px;
            padding: 0 24px;
            overflow: hidden;
        }
        .codelib-item {
            white-space: nowrap;
            overflow: hidden;
            max-width: 406px;
            text-overflow: ellipsis;
            font-size: 12px;
            color: #C3CDD7;
        }
        .instance-count {
            color: $primaryColor;
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

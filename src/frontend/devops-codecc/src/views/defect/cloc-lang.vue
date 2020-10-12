<template>
    <div v-bkloading="{ isLoading: !mainContentLoading && contentLoading, opacity: 0.3 }">
        <div class="cloc-list" v-if="taskDetail.enableToolList.find(item => item.toolName === 'CLOC')">
            <div class="breadcrumb">
                <div class="breadcrumb-name">
                    <bk-tab :active.sync="active" @tab-change="handleTableChange" type="unborder-card">
                        <bk-tab-panel
                            v-for="(panel, index) in panels"
                            v-bind="panel"
                            :key="index">
                        </bk-tab-panel>
                    </bk-tab>
                </div>
            </div>
            <div class="main-container" ref="mainContainer">
                <div class="main-content-inner main-content-list">
                    <div class="catalog">
                        <span class="mr10">
                            共{{mainLangNo}}种主要语言
                        </span>
                    </div>
                    <bk-table class="cloc-list-table"
                        :data="clocList"
                        size="medium"
                        v-show="isFetched"
                        v-bkloading="{ isLoading: fileLoading, opacity: 0.6 }">
                        <bk-table-column class-name="name" :label="$t('语言')" prop="language"></bk-table-column>
                        <bk-table-column :label="$t('总行数')" align="right" class-name="pr40" prop="sumLines"></bk-table-column>
                        <bk-table-column :label="$t('占比')" align="right" class-name="pr40" prop="proportion">
                            <template slot-scope="props">
                                <span>{{props.row.proportion + '%'}}</span>
                            </template>
                        </bk-table-column>
                        <bk-table-column :label="$t('代码行')" align="right" class-name="pr40" prop="sumCode">
                            <template slot-scope="props">
                                <span v-bk-tooltips="{ content: $t('代码行比率') + handleRate(props.row.sumCode, props.row.sumLines) }">
                                    {{props.row.sumCode}}
                                </span>
                            </template>
                        </bk-table-column>
                        <bk-table-column :label="$t('注释行')" align="right" class-name="pr40" prop="sumComment">
                            <template slot-scope="props">
                                <span v-bk-tooltips="{ content: $t('注释率') + handleRate(props.row.sumComment, props.row.sumLines) }">
                                    {{props.row.sumComment}}
                                </span>
                            </template>
                        </bk-table-column>
                        <bk-table-column :label="$t('空白行')" align="right" class-name="pr40" prop="sumBlank">
                            <template slot-scope="props">
                                <span v-bk-tooltips="{ content: $t('空白率') + handleRate(props.row.sumBlank, props.row.sumLines) }">
                                    {{props.row.sumBlank}}
                                </span>
                            </template>
                        </bk-table-column>
                        <div slot="empty">
                            <div class="codecc-table-empty-text">
                                <img src="../../images/empty.png" class="empty-img">
                                <div>{{$t('没有查询到数据')}}</div>
                            </div>
                        </div>
                    </bk-table>
                </div>
            </div>
        </div>
        <div class="cloc-list" v-else>
            <div class="main-container large boder-none">
                <div class="no-task">
                    <empty title="" :desc="$t('CodeCC集成了代码统计工具，可以检测代码中所包含的语言种类，以及代码行、注释行、空白行等占比')">
                        <template v-slot:action>
                            <bk-button size="large" theme="primary" @click="addTool({ from: 'cloc' })">{{$t('配置规则集')}}</bk-button>
                        </template>
                    </empty>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
    import { mapState } from 'vuex'
    import Empty from '@/components/empty'
    export default {
        components: {
            Empty
        },
        data () {
            return {
                contentLoading: true,
                fileLoading: false,
                isFetched: false,
                panels: [
                    { name: 'list', label: this.$t('按目录') },
                    { name: 'lang', label: this.$t('按语言') }
                ],
                active: 'lang',
                clocList: [],
                mainLangNo: 0
            }
        },
        computed: {
            ...mapState('task', {
                taskDetail: 'detail'
            })
        },
        created () {
            if (!this.taskDetail.nameEn || this.taskDetail.enableToolList.find(item => item.toolName === 'CLOC')) {
                this.init(true)
            }
        },
        methods: {
            async init (isInit) {
                isInit ? this.contentLoading = true : this.fileLoading = true
                const res = await this.$store.dispatch('defect/lintListCloc', { toolId: 'CLOC', type: 'LANGUAGE' })
                if (isInit) {
                    this.contentLoading = false
                    this.isFetched = true
                }
                if (res) {
                    this.fileLoading = false
                    const { otherInfo, totalInfo, languageInfo } = res
                    const languageInfoNew = languageInfo.filter(item => item.language !== 'OTHERS')
                    this.mainLangNo = languageInfoNew.length
                    this.clocList = [totalInfo, ...languageInfoNew, otherInfo]
                }
            },
            handleTableChange (value) {
                this.$router.push({ name: `defect-cloc-${value}` })
            },
            handleRate (num, sum) {
                return (num * 100 / sum).toFixed(2) + '%'
            },
            addTool (query) {
                if (this.taskDetail.createFrom.indexOf('pipeline') !== -1) {
                    const that = this
                    this.$bkInfo({
                        title: this.$t('配置规则集'),
                        subTitle: this.$t('此代码检查任务为流水线创建，规则集需前往相应流水线配置。'),
                        maskClose: true,
                        confirmFn (name) {
                            window.open(`${window.DEVOPS_SITE_URL}/console/pipeline/${that.taskDetail.projectId}/${that.taskDetail.pipelineId}/edit#${that.taskDetail.atomCode}`, '_blank')
                        }
                    })
                } else {
                    this.$router.push({ name: 'task-settings-checkerset', query })
                }
            }
        }
        
    }
</script>

<style lang="postcss" scoped>
    @import './defect-list.css';
    .cloc-list {
        padding: 16px 20px 0px 16px;
    }
    .main-container {
        border-top: 1px solid #dcdee5;
        margin: 0px!important;
        background: white;
    }
    .breadcrumb {
        padding: 0px!important;
        .breadcrumb-name {
            background: white;
        }
    }
    .bk-table.cloc-list-table {
        margin: 10px 0;
        >>>.bk-table-body .name {
            font-weight: bold;
            /* font-size: 14px!important; */
        }
    }
    .catalog {
        font-size: 14px;
    }
</style>

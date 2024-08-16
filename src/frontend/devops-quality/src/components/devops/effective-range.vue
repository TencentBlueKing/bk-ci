<template>
    <bk-dialog v-model="rangeListConf.isShow"
        :width="'900'"
        :position="{ top: 100 }"
        :ext-cls="'range-list-wrapper'"
        :close-icon="rangeListConf.closeIcon"
        :show-footer="rangeListConf.hasFooter">
        <div>
            <div class="range-list-header">
                <div class="title">{{$t('quality.生效范围')}}</div>
                <div><i class="devops-icon icon-close" @click="close" style="color: #C3CDD7;"></i></div>
            </div>
            <div class="range-content">
                <bk-table
                    size="small"
                    class="effective-range-table"
                    :data="rangeList"
                    :pagination="pagination"
                    @page-change="handlePageChange">
                    <bk-table-column :label="$t('quality.名称')" prop="name" width="320">
                        <template slot-scope="props">
                            <span>{{props.row.name}}</span>
                        </template>
                    </bk-table-column>
                    <bk-table-column :label="$t('quality.类型')" width="80">
                        <template slot-scope="props">
                            <span>{{props.row.type === 'PIPELINE' ? $t('quality.单流水线') : $t('quality.模板')}}</span>
                        </template>
                    </bk-table-column>
                    <bk-table-column :label="$t('quality.指标相关插件')">
                        <template slot-scope="props">
                            <span v-if="props.row.lackElements.length" :title="$t('quality.缺少{0}插件', [props.row.lackElements.join('、')])" style="color:#F5A623;">
                                {{
                                    $t('quality.缺少{0}插件', [props.row.lackElements.join('、')])
                                }}
                            </span>
                            <span v-else style="color:#00C873;">{{$t('quality.指标所需插件完整')}}</span>
                        </template>
                    </bk-table-column>
                    <bk-table-column :label="$t('quality.操作建议')" width="100">
                        <template slot-scope="props">
                            <a class="add-btn"
                                v-if="props.row.type === 'PIPELINE' && (props.row.lackElements.length)"
                                target="_blank"
                                :href="`/console/pipeline/${projectId}/${props.row.id}/edit`"
                            >{{$t('quality.去修改')}}</a>
                            <a class="add-btn"
                                v-else-if="props.row.type === 'TEMPLATE' && (props.row.lackElements.length)"
                                target="_blank"
                                :href="`/console/pipeline/${projectId}/template/${props.row.id}/edit`"
                            >{{$t('quality.去修改')}}</a>
                            <span v-else>-</span>
                        </template>
                    </bk-table-column>
                </bk-table>
            </div>
        </div>
    </bk-dialog>
</template>

<script>
    export default {
        props: {
            rangeListConf: Object,
            pagination: Object,
            handlePageChange: Function,
            rangeList: Array
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            }
        },
        methods: {
            close () {
                this.$emit('close')
            }
        }
    }
</script>

<style lang="scss">
    @import '@/scss/conf.scss';

    .range-list-wrapper{
        .bk-dialog-tool {
            display: none;
        }
        .bk-dialog-body {
            padding: 0;
        }
        .range-list-header {
            padding: 20px;
            height: 56px;
            display: flex;
            justify-content: space-between;
            border-bottom: 1px solid $borderWeightColor;
            .title {
                line-height: 16px;
                color: $fontWeightColor;
            }
            .devops-icon {
                cursor: pointer;
            }
        }
        .range-content {
            height: 566px;
            padding: 20px;
            overflow: auto;
        }
        .effective-range-table {
            .item-pipelinename {
                display: block;
                white-space: nowrap;
                overflow: hidden;
                text-overflow: ellipsis;
                color: $primaryColor;
                cursor: pointer;
            }
            .add-btn  {
                cursor: pointer;
                color: $primaryColor;
            }
        }
        .ci-paging {
            margin: 20px auto 0;
        }
    }
</style>

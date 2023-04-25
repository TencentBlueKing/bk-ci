<template>
    <bk-dialog v-model="pipelineListConf.isShow"
        :width="'900'"
        :position="{ top: 100 }"
        :ext-cls="'pipeline-list-wrapper'"
        :close-icon="pipelineListConf.closeIcon"
        :show-footer="pipelineListConf.hasFooter">
        <div v-bkloading="{
            isLoading: loading.isLoading,
            title: loading.title
        }">
            <div class="pipeline-list-header">
                <div class="title">{{$t('quality.生效流水线')}}</div>
                <div><i class="devops-icon icon-close" @click="close" style="color: #C3CDD7;"></i></div>
            </div>
            <div class="pipeline-content">
                <bk-table
                    size="small"
                    class="effective-pipeline-table"
                    :data="pipelineList"
                    :pagination="pagination"
                    @page-change="handlePageChange">
                    <bk-table-column :label="$t('quality.流水线名称')" prop="pipelineName" width="360">
                        <template slot-scope="props">
                            <a class="item-pipelinename" :title="props.row.pipelineName"
                                target="_blank"
                                :href="`/console/pipeline/${projectId}/${props.row.pipelineId}/detail/${props.row.latestBuildId}`">{{props.row.pipelineName}}
                            </a>
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
                            <span>{{localConvertTime(props.row.latestBuildStartTime)}}</span>
                        </template>
                    </bk-table-column>
                </bk-table>
            </div>
        </div>
    </bk-dialog>
</template>

<script>
    import { convertTime } from '@/utils/util'

    export default {
        props: {
            pipelineListConf: Object,
            loading: Object,
            pagination: Object,
            handlePageChange: Function,
            pipelineList: Array
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            }
        },
        methods: {
            close () {
                this.$emit('close')
            },
            /**
             * 处理时间格式
             */
            localConvertTime (timestamp) {
                return convertTime(timestamp)
            }
        }
    }
</script>

<style lang="scss">
    @import '@/scss/conf.scss';

    .pipeline-list-wrapper{
        .bk-dialog-tool {
            display: none;
        }
        .bk-dialog-body {
            padding: 0;
        }
        .pipeline-list-header {
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
        .pipeline-content {
            height: 566px;
            padding: 20px;
            overflow: auto;
        }
        .effective-pipeline-table {
            .item-pipelinename {
                display: block;
                white-space: nowrap;
                overflow: hidden;
                text-overflow: ellipsis;
                color: $primaryColor;
                cursor: pointer;
            }
        }
        .ci-paging {
            margin: 20px auto 0;
        }
    }
</style>

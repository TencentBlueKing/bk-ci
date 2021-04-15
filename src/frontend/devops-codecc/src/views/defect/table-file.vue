<template>
    <bk-table
        class="file-list-table"
        ref="fileListTable"
        v-bkloading="{ isLoading: fileLoading, opacity: 0.6 }"
        :height="screenHeight"
        :data="list"
        :row-class-name="handleRowClassName"
        @row-click="handleFileListRowClick"
        @sort-change="handleSortChange"
        @selection-change="handleSelectionChange"
        @select-all="toSelectAll">
        <bk-table-column :selectable="handleSelectable" type="selection" width="60" align="center">
        </bk-table-column>
        <bk-table-column width="15" class-name="mark-row">
            <template slot-scope="props">
                <span v-if="props.row.mark" class="cc-icon-mark"></span>
            </template>
        </bk-table-column>
        <bk-table-column :label="$t('文件名称')" prop="fileName" sortable="custom">
            <template slot-scope="props">
                <span v-bk-tooltips="{ content: props.row.filePath, delay: 600 }">{{props.row.fileName}}</span>
            </template>
        </bk-table-column>
        <bk-table-column :label="$t('问题数')" prop="defectCount" sortable="custom" width="90"></bk-table-column>
        <bk-table-column :label="$t('规则数')" prop="checkerList" width="90">
            <template slot-scope="props">
                <span v-bk-tooltips="{ content: props.row.checkerList && props.row.checkerList.join('</br>'), delay: 600 }">{{props.row.checkerList && props.row.checkerList.length}}</span>
            </template>
        </bk-table-column>
        <bk-table-column :label="$t('级别')" prop="severity">
            <template slot-scope="props">
                <span>{{formatSeverity(props.row.severityList)}}</span>
            </template>
        </bk-table-column>
        <bk-table-column :label="$t('处理人')" prop="authorList" min-width="70">
            <template slot-scope="props">
                <span>
                    {{props.row.authorList && props.row.authorList.join(';')}}
                </span>
            </template>
        </bk-table-column>
        <bk-table-column :label="$t('所属路径')" prop="filePath" min-width="130">
            <template slot-scope="props">
                <span v-bk-tooltips="{ content: props.row.filePath, delay: 600 }">
                    {{props.row.filePath}}
                </span>
            </template>
        </bk-table-column>
        <bk-table-column
            prop="fileUpdateTime"
            sortable="custom"
            width="110"
            :label="$t('提交日期')">
            <template slot-scope="props">
                <span>{{props.row.fileUpdateTime | formatDate('date')}}</span>
            </template>
        </bk-table-column>
        
        <bk-table-column :label="$t('操作')" width="60">
            <template slot-scope="props">
                <!-- 已修复问题没有这些操作 -->
                <span v-if="!(props.row.status & 2)" class="cc-operate-more" @click.prevent.stop>
                    <bk-popover theme="light" placement="bottom" trigger="click">
                        <span class="bk-icon icon-more"></span>
                        <div slot="content" class="handle-menu-tips">
                            <!-- 待修复问题的操作 -->
                            <!-- <template v-if="props.row.status === 1">
                                <p v-if="props.row.mark" class="entry-link" @click.stop="handleMark(0, false, props.row.entityId)">
                                    {{$t('取消标记')}}
                                </p>
                                <p v-else class="entry-link" @click.stop="handleMark(1, false, props.row.entityId)">
                                    {{$t('标记处理')}}
                                </p>
                            </template> -->
                            <!-- 已忽略问题的操作 -->
                            <p v-if="props.row.status & 4" class="entry-link" @click.stop="handleIgnore('RevertIgnore', false, props.row.entityId)">
                                {{$t('恢复忽略')}}
                            </p>
                            <p v-else class="entry-link" @click.stop="handleIgnore('IgnoreDefect', false, props.row.entityId, props.row.filePath)">
                                {{$t('批量忽略')}}
                            </p>
                        </div>
                    </bk-popover>
                </span>
            </template>
        </bk-table-column>
        <div slot="append" v-show="isFileListLoadMore">
            <div class="table-append-loading">
                {{$t('正在加载第x-y个，请稍后···', { x: nextPageStartNum, y: nextPageEndNum })}}
            </div>
        </div>
        <div slot="empty">
            <div class="codecc-table-empty-text">
                <img src="../../images/empty.png" class="empty-img">
                <div>{{$t('没有查询到数据')}}</div>
            </div>
        </div>
    </bk-table>
</template>

<script>
    import defectTable from '@/mixins/defect-table'

    export default {
        mixins: [defectTable]
    }
</script>

<style scoped lang="postcss">
    @import '../../css/variable.css';
    
    .file-list-table {
        >>> .list-row {
            cursor: pointer;
            &.grey-row {
                color: #c3cdd7;
            }
        }
    }
    .cc-operate-more {
        >>>.icon-more {
            font-size: 20px;
        }
    }
    .handle-menu-tips {
        text-align: center;
        .entry-link {
            padding: 4px 0;
            font-size: 12px;
            cursor: pointer;
            color: $fontWeightColor;
            > a {
                color: $fontWeightColor;
            }
            &:hover {
                color: $primaryColor;
                > a {
                    color: $primaryColor;
                }
            }
        }
    }
    .cc-icon-mark {
        display: inline-block;
        background: url(../../images/mark.svg) no-repeat;
        height: 14px;
        width: 14px;
        margin-bottom: -2px;
    }
</style>

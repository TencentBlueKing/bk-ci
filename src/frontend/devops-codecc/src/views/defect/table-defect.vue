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
                <span v-if="props.row.status === 1 && props.row.mark === 1" v-bk-tooltips="'已标记处理'" class="codecc-icon icon-mark"></span>
                <span v-if="props.row.status === 1 && props.row.mark === 2" v-bk-tooltips="'标记处理后重新扫描仍为问题'" class="codecc-icon icon-mark re-mark"></span>
            </template>
        </bk-table-column>
        <!-- <bk-table-column :label="$t('ID')" prop="defectId" sortable="custom"></bk-table-column> -->
        <bk-table-column :label="$t('位置')" prop="fileName" sortable="custom">
            <template slot-scope="props">
                <span v-bk-tooltips="{ content: props.row.filePath + ':' + props.row.lineNum, delay: 600 }">{{props.row.fileName}}:{{props.row.lineNum}}</span>
            </template>
        </bk-table-column>
        <bk-table-column :label="$t('规则')" prop="checker"></bk-table-column>
        <bk-table-column :label="$t('规则描述')" prop="message" min-width="120"></bk-table-column>
        <!-- <bk-table-column :label="$t('类型子类')" prop="displayType"></bk-table-column> -->
        <bk-table-column :label="$t('处理人')" prop="author" min-width="70">
            <template slot-scope="props">
                <div
                    v-if="props.row.status === 1"
                    @mouseenter="handleAuthorIndex(props.$index)"
                    @mouseleave="handleAuthorIndex(-1)"
                    @click.stop="handleAuthor(1, props.row.entityId, props.row.author)">
                    <span>{{props.row.author || '--'}}</span>
                    <span v-if="hoverAuthorIndex === props.$index" class="bk-icon icon-edit2 fs18"></span>
                </div>
                <div v-else>
                    <span>
                        {{props.row.author}}
                    </span>
                </div>
            </template>
        </bk-table-column>
        <bk-table-column :label="$t('级别')" prop="severity" sortable="custom" width="80">
            <template slot-scope="props">
                <span :class="`color-${{ 1: 'major', 2: 'minor', 4: 'info' }[props.row.severity]}`">{{defectSeverityMap[props.row.severity]}}</span>
            </template>
        </bk-table-column>
        <bk-table-column
            prop="lineUpdateTime"
            sortable="custom"
            width="110"
            :label="$t('提交日期')">
            <template slot-scope="props">
                <span>{{props.row.lineUpdateTime | formatDate('date')}}</span>
            </template>
        </bk-table-column>
        <bk-table-column :label="$t('首次发现')" prop="createBuildNumber" sortable="custom" width="100">
            <template slot-scope="props">
                <span>{{props.row.createBuildNumber ? '#' + props.row.createBuildNumber : '--'}}</span>
            </template>
        </bk-table-column>
        <bk-table-column :label="$t('最新状态')" prop="status" width="80">
            <template slot-scope="props">
                <span>{{handleStatus(props.row.status)}}</span>
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
                            <template v-if="props.row.status === 1">
                                <p v-if="props.row.mark" class="entry-link" @click.stop="handleMark(0, false, props.row.entityId)">
                                    {{$t('取消标记')}}
                                </p>
                                <p v-else class="entry-link" @click.stop="handleMark(1, false, props.row.entityId)">
                                    {{$t('标记处理')}}
                                </p>
                            </template>
                            <!-- 已忽略问题的操作 -->
                            <p v-if="props.row.status & 4" class="entry-link" @click.stop="handleIgnore('RevertIgnore', false, props.row.entityId)">
                                {{$t('恢复忽略')}}
                            </p>
                            <p v-else class="entry-link" @click.stop="handleIgnore('IgnoreDefect', false, props.row.entityId)">
                                {{$t('忽略问题')}}
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
                .color-major, .color-minor, .color-info {
                    color: #c3cdd7;
                }
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
    .icon-mark {
        color: #53cad1;
        &.re-mark {
            color: #facc48;
        }
    }
    >>>.bk-table {
        .mark-row {
            .cell {
                padding: 0;
            }
        }
    }
</style>

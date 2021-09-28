<template>
    <div v-bkloading="{ isLoading: !mainContentLoading && contentLoading, opacity: 0.3 }">
        <div class="ccn-list" v-if="taskDetail.enableToolList.find(item => item.toolName === 'CCN')">
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
                <div>
                    <bk-button style="border: none" v-if="exportLoading" icon="loading" :disabled="true" :title="$t('导出Excel')"></bk-button>
                    <span v-else class="codecc-icon icon-export-excel excel-download" @click="downloadExcel" v-bk-tooltips="$t('导出Excel')"></span>
                </div>
            </div>
            <div class="main-container" ref="mainContainer">
                <div class="main-content-inner main-content-list">
                    <bk-form :label-width="60" class="search-form main-form">
                        <container :class="['cc-container', { 'fold': !isSearchDropdown }]">
                            <div class="cc-col">
                                <bk-form-item :label="$t('处理人')">
                                    <bk-select v-model="searchParams.author" searchable>
                                        <bk-option
                                            v-for="(author, index) in searchFormData.authorList"
                                            :key="index"
                                            :id="author"
                                            :name="author"
                                        >
                                        </bk-option>
                                    </bk-select>
                                </bk-form-item>
                            </div>
                            <div class="cc-col">
                                <bk-form-item :label="$t('文件路径')" class="fixed-width">
                                    <bk-dropdown-menu @show="isFilePathDropdownShow = true" @hide="isFilePathDropdownShow = false" align="right" trigger="click" ref="filePathDropdown">
                                        <bk-button type="primary" slot="dropdown-trigger">
                                            <div class="filepath-name" :class="{ 'unselect': !searchFormData.filePathShow }" :title="searchFormData.filePathShow">{{searchFormData.filePathShow ? searchFormData.filePathShow : $t('请选择')}}</div>
                                            <i :class="['bk-icon icon-angle-down', { 'icon-flip': isFilePathDropdownShow }]"></i>
                                        </bk-button>
                                        <div class="filepath-dropdown-content" slot="dropdown-content" @click="e => e.stopPropagation()">
                                            <bk-tab type="unborder-card" class="create-tab" @tab-change="changeTab">
                                                <bk-tab-panel
                                                    v-for="(panel, index) in pathPanels"
                                                    v-bind="panel"
                                                    :key="index">
                                                </bk-tab-panel>
                                                <div v-show="tabSelect === 'choose'" class="create-tab-1">
                                                    <div>
                                                        <div class="content-hd">
                                                            <bk-input v-model="searchInput" class="search-input" :clearable="true" :placeholder="$t('搜索文件夹、问题路径名称')" @input="handleFilePathSearch"></bk-input>
                                                        </div>
                                                        <div class="content-bd" v-if="treeList.length">
                                                            <bk-big-tree
                                                                ref="filePathTree"
                                                                height="340"
                                                                :options="{ 'idKey': 'treeId' }"
                                                                :show-checkbox="true"
                                                                :data="treeList"
                                                                :filter-method="filterMethod"
                                                                :expand-icon="'bk-icon icon-folder-open'"
                                                                :collapse-icon="'bk-icon icon-folder'"
                                                                :has-border="true"
                                                                :node-key="'name'">
                                                            </bk-big-tree>
                                                        </div>
                                                        <div class="content-empty" v-if="!treeList.length">
                                                            <empty size="small" :title="$t('无问题文件')" />
                                                        </div>
                                                    </div>
                                                </div>
                                                <div v-show="tabSelect === 'input'" class="create-tab-2">
                                                    <div class="input-info">
                                                        <div class="input-info-left"><i class="bk-icon icon-info-circle-shape"></i></div>
                                                        <div class="input-info-right"></div>
                                                        搜索文件夹如P2PLive，可以输入.*/P2PLive/.*<br />
                                                        搜索某类文件如P2PLive下*.c，可以输入.*/P2PLive/.*\.c
                                                    </div>
                                                    <div class="input-paths">
                                                        <div class="input-paths-item" v-for="(path, index) in inputFileList" :key="index">
                                                            <bk-input :placeholder="$t('请输入')" class="input-style" v-model="inputFileList[index]"></bk-input>
                                                            <span class="input-paths-icon">
                                                                <i class="bk-icon icon-plus-circle-shape" @click="addPath(index)"></i>
                                                                <i class="bk-icon icon-minus-circle-shape" v-if="inputFileList.length > 1" @click="cutPath(index)"></i>
                                                            </span>
                                                        </div>
                                                    </div>
                                                </div>
                                            </bk-tab>
                                            <div class="content-ft">
                                                <bk-button theme="primary" @click="handleFilePathConfirmClick">{{$t('确定')}}</bk-button>
                                                <bk-button @click="handleFilePathCancelClick">{{$t('取消')}}</bk-button>
                                                <bk-button class="clear-btn" @click="handleFilePathClearClick">{{$t('清空选择')}}</bk-button>
                                            </div>
                                        </div>
                                    </bk-dropdown-menu>
                                </bk-form-item>
                            </div>
                            <div class="cc-col" v-show="isSearchDropdown || lineAverageOpt >= 3">
                                <bk-form-item :label="$t('日期')">
                                    <bk-date-picker v-model="searchParams.daterange" :type="'daterange'"></bk-date-picker>
                                </bk-form-item>
                            </div>
                            <div class="cc-col" v-if="isSearchDropdown || lineAverageOpt >= 4">
                                <bk-form-item :label="$t('快照')">
                                    <bk-select v-model="searchParams.buildId" :clearable="true" searchable>
                                        <bk-option
                                            v-for="item in buildList"
                                            :key="item.buildId"
                                            :id="item.buildId"
                                            :name="`#${item.buildNum}构建 ${formatDate(item.buildTime) || ''} ${item.buildUser || ''}`"
                                        >
                                        </bk-option>
                                    </bk-select>
                                </bk-form-item>
                            </div>
                            <div class="cc-col" v-if="isSearchDropdown || lineAverageOpt >= 5">
                                <bk-form-item :label="$t('状态')">
                                    <bk-select multiple v-model="searchParams.status" :clearable="false" searchable>
                                        <bk-option
                                            v-for="(value, key) in statusTypeMap"
                                            :key="Number(key)"
                                            :id="Number(key)"
                                            :name="value"
                                        >
                                        </bk-option>
                                    </bk-select>
                                </bk-form-item>
                            </div>
                            <!-- <div class="cc-col" v-if="isSearchDropdown || lineAverageOpt >= 6">
                                <bk-form-item :label="$t('函数类型')">
                                    <bk-checkbox-group v-model="searchParams.defectType" class="checkbox-group">
                                        <bk-checkbox :value="1">{{$t('新函数')}}(<em class="count">{{newDefectCount}}</em>)</bk-checkbox>
                                        <bk-checkbox :value="2">
                                            {{$t('历史函数')}}(<em class="count">{{historyDefectCount}}</em>)
                                        </bk-checkbox>
                                        <bk-popover placement="top" width="220" class="popover">
                                            <i class="codecc-icon icon-tips"></i>
                                            <div slot="content">
                                                {{typeTips}}
                                                <a href="javascript:;" @click="toLogs">{{$t('前往设置')}}>></a>
                                            </div>
                                        </bk-popover>
                                    </bk-checkbox-group>
                                </bk-form-item>
                            </div> -->
                            <div class="cc-col-2" v-if="isSearchDropdown || lineAverageOpt >= 7">
                                <bk-form-item :label="$t('风险级别')">
                                    <bk-checkbox-group v-model="searchParams.severity" class="checkbox-group">
                                        <bk-checkbox
                                            v-for="(name, value, index) in defectSeverityMap"
                                            :value="Number(value)"
                                            :key="index"
                                        >
                                            {{name}}(<em :class="['count', `count-${['major', 'minor', 'info'][index]}`]">{{getDefectCountBySeverity(value)}}</em>)
                                        </bk-checkbox>
                                        <bk-popover placement="top" width="220" class="popover">
                                            <i class="codecc-icon icon-tips"></i>
                                            <div slot="content">
                                                <p>{{$t('极高风险：复杂度>=60')}}</p>
                                                <p>{{$t('高风险：复杂度40-59')}}</p>
                                                <p>{{$t('中风险：复杂度20-39')}}</p>
                                                <p>{{$t('低风险：复杂度1-19')}}</p>
                                                <p>{{$t('阈值被设置为20，列表中仅展示大于等于该阈值的函数', { ccnThreshold: ccnThreshold })}}</p>
                                            </div>
                                        </bk-popover>
                                    </bk-checkbox-group>
                                </bk-form-item>
                            </div>
                        </container>
                    </bk-form>

                    <div class="cc-table">
                        <div class="cc-selected">
                            <span v-show="isSelectAll === 'Y'">{{$t('已选择x条,共y条', { x: totalCount, y: totalCount })}}</span>
                            <span v-show="isSelectAll !== 'Y'">{{$t('已选择x条,共y条', { x: selectedLen, y: totalCount })}}</span>
                        </div>
                        <p class="search-more-option">
                            <i :class="['bk-icon codecc-icon icon-codecc-arrow', { 'icon-flip': isSearchDropdown }]"
                                @click.stop="toggleSearch">
                            </i>
                        </p>
                        <div v-if="isBatchOperationShow" class="cc-operate pb10">
                            <div class="cc-operate-buttons">
                                <bk-dropdown-menu @show="isDropdownShow = true" @hide="isDropdownShow = false">
                                <bk-button size="small" slot="dropdown-trigger">
                                    <span>{{$t('标记')}}</span>
                                        <i :class="['bk-icon icon-angle-down', { 'icon-flip': isDropdownShow }]"></i>
                                    </bk-button>
                                    <div class="handle-menu-tips" slot="dropdown-content">
                                        <p class="entry-link" @click.stop="handleMark(1, true)">
                                            {{$t('标记处理')}}
                                        </p>
                                        <p class="entry-link" @click.stop="handleMark(0, true)">
                                            {{$t('取消标记')}}
                                        </p>
                                    </div>
                                </bk-dropdown-menu>
                                <bk-button size="small" ext-cls="cc-operate-button" @click="handleAuthor(2)" theme="primary">{{$t('分配')}}</bk-button>
                                <bk-button size="small" ext-cls="cc-operate-button" @click="handleIgnore('IgnoreDefect', true)" theme="primary">{{$t('忽略')}}</bk-button>
                                <bk-button size="small" ext-cls="cc-operate-button" @click="handleIgnore('RevertIgnore', true)" v-if="!searchParams.status.length || searchParams.status.includes(4)" theme="primary">
                                    {{$t('恢复忽略')}}
                                </bk-button>
                            </div>
                            
                        </div>
                    
                        <div class="cc-keyboard">
                            <span>{{$t('当前已支持键盘操作')}}</span>
                            <bk-button text ext-cls="cc-button" @click="operateDialogVisiable = true">{{$t('如何操作？')}}</bk-button>
                        </div>

                        <bk-table
                            v-show="isFetched"
                            class="file-list-table"
                            ref="fileListTable"
                            v-bkloading="{ isLoading: tableLoading, opacity: 0.6 }"
                            :data="defectList"
                            :row-class-name="handleRowClassName"
                            :height="screenHeight"
                            @row-click="handleFileListRowClick"
                            @sort-change="handleSortChange"
                            @selection-change="handleSelectionChange"
                            @select-all="toSelectAll">
                            <bk-table-column :selectable="handleSelectable" type="selection" width="50" align="center"></bk-table-column>
                            <bk-table-column width="15" class-name="mark-row">
                                <template slot-scope="props">
                                    <span v-if="props.row.mark === 1" v-bk-tooltips="$t('已标记处理')" class="codecc-icon icon-mark"></span>
                                    <span v-if="props.row.mark === 2" v-bk-tooltips="$t('标记处理后重新扫描仍为问题')" class="codecc-icon icon-mark re-mark"></span>
                                </template>
                            </bk-table-column>
                            <bk-table-column :label="$t('位置')" min-width="100" prop="filePath">
                                <template slot-scope="props">
                                    <span :title="`${props.row.filePath}:${props.row.startLines}`">{{`${getFileName(props.row.filePath)}:${props.row.startLines}`}}</span>
                                </template>
                            </bk-table-column>
                            <bk-table-column :label="$t('函数名')" min-width="100" prop="functionName">
                                <template slot-scope="props"> <span :title="props.row.functionName">{{ props.row.functionName }}</span></template>
                            </bk-table-column>
                            <bk-table-column :label="$t('圈复杂度')" prop="ccn" sortable="custom"></bk-table-column>
                            <bk-table-column :label="$t('处理人')" prop="author">
                                <template slot-scope="props">
                                    <div v-if="true"
                                        @mouseenter="handleAuthorIndex(props.$index)"
                                        @mouseleave="handleAuthorIndex(-1)"
                                        @click.stop="handleAuthor(1, props.row.entityId, [props.row.author], props.row.defectId)">
                                        <!-- <span>{{props.row.authorList && props.row.authorList.join(';')}}</span> -->
                                        <span>{{props.row.author}}</span>
                                        <span v-if="hoverAuthorIndex === props.$index" class="bk-icon icon-edit2 fs18"></span>
                                    </div>
                                    <div v-else>
                                        <span>
                                            {{props.row.author}}
                                        </span>
                                    </div>
                                </template>
                            </bk-table-column>
                            <bk-table-column :label="$t('风险')" width="70" prop="riskFactor">
                                <template slot-scope="props">
                                    <span :class="`color-${{ 1: 'major', 2: 'minor', 4: 'info' }[props.row.riskFactor]}`">{{defectSeverityMap[props.row.riskFactor]}}</span>
                                </template>
                            </bk-table-column>
                            <bk-table-column
                                prop="latestDateTime"
                                sortable="custom"
                                :label="$t('提交日期')">
                                <template slot-scope="props">
                                    <span>{{props.row.latestDateTime | formatDate('date')}}</span>
                                </template>
                            </bk-table-column>
                            <bk-table-column :label="$t('首次发现')" prop="createBuildNumber" sortable="custom" width="100">
                                <template slot-scope="props">
                                    <span>{{props.row.createBuildNumber ? '#' + props.row.createBuildNumber : '--'}}</span>
                                </template>
                            </bk-table-column>
                            <bk-table-column :label="$t('最新状态')" prop="status" width="90">
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

                    </div>

                    <bk-dialog
                        v-model="defectDetailDialogVisiable"
                        ext-cls="file-detail-dialog"
                        :fullscreen="isFullScreen"
                        :position="{ top: `${isFullScreen ? 0 : 50}` }"
                        :draggable="false"
                        :mask-close="false"
                        :show-footer="false"
                        :close-icon="true"
                        width="80%">
                        <div :class="['code-fullscreen', { 'full-active': isFullScreen }]">
                            <i class="bk-icon toggle-full-icon" :class="isFullScreen ? 'icon-un-full-screen' : 'icon-full-screen'" @click="isFullScreen = !isFullScreen"></i>
                            <div class="col-main">
                                <div class="file-bar">
                                    <div class="filemeta" v-if="currentLintFile">
                                        <b class="filename" :title="currentLintFile.filePath">{{lintDetail.fileName}}</b>
                                        <!-- <div class="filepath" :title="currentLintFile.filePath">{{$t('文件路径')}}：{{currentLintFile.filePath}}</div> -->
                                        <bk-button class="fr mr10" theme="primary" @click="scrollIntoView()">{{$t('函数位置')}}</bk-button>
                                    </div>
                                </div>
                                <div id="codeViewerInDialog" :class="isFullScreen ? 'full-code-viewer' : 'un-full-code-viewer'" @click="handleCodeViewerInDialogClick"></div>
                            </div>
                            <div class="col-aside">
                                <div class="operate-section">
                                    <dl class="basic-info" :class="{ 'full-screen-info': isFullScreen }" v-if="currentLintFile">
                                        <div class="block">
                                            <div class="item">
                                                <span class="fail" v-if="currentLintFile.status === 1"><span class="cc-dot"></span>{{$t('待修复')}}</span>
                                                <span class="success" v-else-if="currentLintFile.status & 2"><span class="cc-dot"></span>{{$t('已修复')}}</span>
                                                <span class="warn" v-else-if="currentLintFile.status & 4"><span class="cc-dot"></span>{{$t('已忽略')}}</span>
                                                <span v-if="currentLintFile.status === 1 && currentLintFile.mark" class="cc-mark">
                                                    <template v-if="currentLintFile.mark === 1">
                                                        <span v-bk-tooltips="$t('已标记处理')" class="codecc-icon icon-mark"></span>
                                                        <span>{{$t('已标记处理')}}</span>
                                                    </template>
                                                    <template v-if="currentLintFile.mark === 2">
                                                        <span v-bk-tooltips="$t('标记处理后重新扫描仍为问题')" class="codecc-icon icon-mark re-mark"></span>
                                                        <span>{{$t('已标记处理')}}</span>
                                                    </template>
                                                </span>
                                            </div>
                                            <div v-if="currentLintFile.status === 1" class="item">
                                                <bk-button v-if="currentLintFile.mark" class="item-button" @click="handleMark(0, false, currentLintFile.entityId)">
                                                    {{$t('取消标记')}}
                                                </bk-button>
                                                <bk-button v-else theme="primary" class="item-button" @click="handleMark(1, false, currentLintFile.entityId)">
                                                    {{$t('标记处理')}}
                                                </bk-button>
                                            </div>
                                            <div class="item">
                                                <bk-button class="item-button" @click="handleComent(currentLintFile.entityId)">
                                                    {{$t('评论')}}
                                                </bk-button>
                                            </div>
                                            <div class="item">
                                                <bk-button v-if="currentLintFile.status & 4" class="item-button" @click="handleIgnore('RevertIgnore', false, currentLintFile.entityId)">
                                                    {{$t('恢复忽略')}}
                                                </bk-button>
                                                <bk-button v-else-if="!(currentLintFile.status & 2)" class="item-button" @click="handleIgnore('IgnoreDefect', false, currentLintFile.entityId)">
                                                    {{$t('忽略告警')}}
                                                </bk-button>
                                            </div>
                                        </div>
                                        <div class="block">
                                            <div class="item">
                                                <dt>ID</dt>
                                                <dd>{{currentLintFile.entityId}}</dd>
                                            </div>
                                            <div class="item">
                                                <dt>{{$t('级别')}}</dt>
                                                <dd>{{defectSeverityMap[currentLintFile.riskFactor]}}</dd>
                                            </div>
                                        </div>
                                        <div class="block">
                                            <div class="item">
                                                <dt>{{$t('创建时间')}}</dt>
                                                <dd class="small">{{currentLintFile.createTime | formatDate}}</dd>
                                            </div>
                                            <div class="item" v-if="currentLintFile.status & 2">
                                                <dt>{{$t('修复时间')}}</dt>
                                                <dd class="small">{{currentLintFile.fixedTime | formatDate}}</dd>
                                            </div>
                                            <div class="item">
                                                <dt>{{$t('首次发现')}}</dt>
                                                <dd>{{currentLintFile.createBuildNumber ? '#' + currentLintFile.createBuildNumber : '--'}}</dd>
                                            </div>
                                            <div class="item">
                                                <dt>{{$t('提交时间')}}</dt>
                                                <dd class="small">{{currentLintFile.latestDateTime | formatDate}}</dd>
                                            </div>
                                        </div>
                                        <div class="block" v-if="currentLintFile.status & 4">
                                            <div class="item">
                                                <dt>{{$t('忽略时间')}}</dt>
                                                <dd class="small">{{currentLintFile.ignoreTime | formatDate}}</dd>
                                            </div>
                                            <div class="item">
                                                <dt>{{$t('忽略人')}}</dt>
                                                <dd>{{currentLintFile.ignoreAuthor}}</dd>
                                            </div>
                                            <div class="item disb">
                                                <dt>{{$t('忽略原因')}}</dt>
                                                <dd>{{getIgnoreReasonByType(currentLintFile.ignoreReasonType)}}
                                                    {{currentLintFile.ignoreReason ? '：' + currentLintFile.ignoreReason : ''}}
                                                </dd>
                                            </div>
                                        </div>
                                        <div class="block">
                                            <div class="item">
                                                <dt v-if="currentLintFile.status === 1" class="curpt" @click.stop="handleAuthor(1, currentLintFile.entityId, [currentLintFile.author], currentLintFile.defectId)">
                                                    {{$t('处理人')}}
                                                    <span class="bk-icon icon-edit2 fs20"></span>
                                                </dt>
                                                <dt v-else>
                                                    {{$t('处理人')}}
                                                </dt>
                                                <dd>{{currentLintFile.author}}</dd>
                                            </div>
                                        </div>
                                        <div class="block">
                                            <div class="item">
                                                <dt>{{$t('函数名')}}</dt>
                                                <dd>{{currentLintFile.functionName}}</dd>
                                            </div>
                                            <div class="item">
                                                <dt>{{$t('圈复杂度')}}</dt>
                                                <dd>{{currentLintFile.ccn}}</dd>
                                            </div>
                                            <div class="item">
                                                <dt>{{$t('函数总行数')}}</dt>
                                                <dd>{{currentLintFile.totalLines}}</dd>
                                            </div>
                                            <div class="item">
                                                <dt>{{$t('函数起始行')}}</dt>
                                                <dd>{{currentLintFile.startLines}}</dd>
                                            </div>
                                        </div>
                                        <div class="block">
                                            <div class="item ignore">
                                                <dt>{{$t('代码库路径')}}</dt>
                                                <a target="_blank" :href="lintDetail.filePath">{{lintDetail.filePath}}</a>
                                            </div>
                                        </div>
                                        <!-- <div class="block">
                                            <div class="item ignore">
                                                <dt>{{$t('忽略问题')}}</dt>
                                                <dd>{{$t('在函数头或函数内添加')}} // #lizard forgives</dd>
                                            </div>
                                        </div> -->
                                    </dl>
                                    <!-- <div class="toggle-file">
                                        <bk-button theme="primary" style="width:200px;" @click="scrollIntoView">{{$t('回到函数位置')}}</bk-button>
                                    </div> -->
                                    <div class="toggle-file">
                                        <bk-button :disabled="fileIndex - 1 < 0" @click="handleFileListRowClick(lintFileList[--fileIndex])">{{$t('上一函数')}}</bk-button>
                                        <bk-button :disabled="fileIndex + 1 >= totalCount" @click="handleFileListRowClick(lintFileList[++fileIndex])">{{$t('下一函数')}}</bk-button>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </bk-dialog>

                </div>
            </div>
            <bk-dialog
                v-model="authorEditDialogVisiable"
                width="560"
                theme="primary"
                :mask-close="false"
                header-position="left"
                :title="operateParams.changeAuthorType === 1 ? $t('修改问题处理人') : $t('批量修改问题处理人')"
            >
                <div class="author-edit">
                    <div class="tips" v-if="operateParams.changeAuthorType === 3"><i class="bk-icon icon-info-circle"></i>{{$t('原处理人所有函数都将转给新处理人')}}</div>
                    <bk-form :model="operateParams" :label-width="130" class="search-form">
                        <bk-form-item v-if="operateParams.changeAuthorType !== 2"
                            property="sourceAuthor"
                            :label="$t('原处理人')">
                            <bk-input v-model="operateParams.sourceAuthor" :disabled="operateParams.changeAuthorType === 1" style="width: 290px;"></bk-input>
                            <!-- <bk-member-selector v-model="operateParams.sourceAuthor" :disabled="operateParams.changeAuthorType === 1" style="width: 290px;"></bk-member-selector> -->
                        </bk-form-item>
                        <bk-form-item :label="$t('新处理人')">
                            <bk-input v-model="operateParams.targetAuthor" style="width: 290px;"></bk-input>
                            <!-- <bk-member-selector :max-data="1" v-model="operateParams.targetAuthor" style="width: 290px;"></bk-member-selector> -->
                        </bk-form-item>
                    </bk-form>
                </div>
                <div slot="footer">
                    <bk-button
                        type="button"
                        theme="primary"
                        :disabled="(operateParams.changeAuthorType === 3 && !operateParams.sourceAuthor) || !operateParams.targetAuthor"
                        :loading="authorEditDialogLoading"
                        @click.native="handleAuthorEditConfirm"
                    >
                        {{operateParams.changeAuthorType === 1 ? $t('确定') : $t('批量修改')}}
                    </bk-button>
                    <bk-button
                        theme="primary"
                        type="button"
                        :disabled="authorEditDialogLoading"
                        @click.native="authorEditDialogVisiable = false"
                    >
                        {{$t('取消')}}
                    </bk-button>
                </div>
            </bk-dialog>
            <bk-dialog
                v-model="ignoreReasonDialogVisiable"
                width="560"
                theme="primary"
                :mask-close="false"
                header-position="left"
                :title="operateParams.batchFlag ? $t('选择问题忽略原因，共x个问题', { num: isSelectAll === 'Y' ? totalCount : selectedLen }) : $t('选择问题忽略原因')">
                <div class="pd10 pr50">
                    <bk-form :model="operateParams" :label-width="30" class="search-form">
                        <bk-form-item property="ignoreReason">
                            <bk-radio-group v-model="operateParams.ignoreReasonType">
                                <bk-radio class="cc-radio" :value="1">{{$t('检查工具误报')}}</bk-radio>
                                <bk-radio class="cc-radio" :value="2">{{$t('设计如此')}}</bk-radio>
                                <bk-radio class="cc-radio" :value="4">{{$t('其他')}}</bk-radio>
                            </bk-radio-group>
                        </bk-form-item>
                        <bk-form-item property="ignoreReason" :required="ignoreReasonRequired">
                            <bk-input :type="'textarea'" :maxlength="255" v-model="operateParams.ignoreReason"></bk-input>
                        </bk-form-item>
                    </bk-form>
                </div>
                <div slot="footer">
                    <bk-button
                        theme="primary"
                        :disabled="ignoreReasonAble"
                        @click.native="handleIgnoreConfirm"
                    >
                        {{operateParams.batchFlag ? $t('批量忽略') : $t('确定')}}
                    </bk-button>
                    <bk-button
                        theme="primary"
                        @click.native="ignoreReasonDialogVisiable = false"
                    >
                        {{$t('取消')}}
                    </bk-button>
                </div>
            </bk-dialog>
            <bk-dialog
                v-model="commentDialogVisiable"
                width="560"
                theme="primary"
                :mask-close="false"
                header-position="left"
                :title="$t('告警评论')"
            >
                <div class="pd10 pr50">
                    <bk-form :model="commentParams" :label-width="30" class="search-form">
                        <bk-form-item property="comment" :required="true">
                            <bk-input placeholder="请输入你的评论内容" :type="'textarea'" :maxlength="200" v-model="commentParams.comment"></bk-input>
                        </bk-form-item>
                    </bk-form>
                </div>
                <div slot="footer">
                    <bk-button
                        theme="primary"
                        :disabled="!commentParams.comment"
                        @click.native="handleCommentConfirm"
                    >
                        {{$t('确定')}}
                    </bk-button>
                    <bk-button
                        theme="primary"
                        @click.native="commentDialogVisiable = false"
                    >
                        {{$t('取消')}}
                    </bk-button>
                </div>
            </bk-dialog>
            <bk-dialog
                v-model="operateDialogVisiable"
                width="605"
                theme="primary"
                :position="{ top: 50, left: 5 }"
                :title="$t('现已支持键盘操作，提升操作效率')">
                <div>
                    <img src="../../images/operate-cov.svg">
                </div>
                <div class="operate-footer" slot="footer">
                    <bk-button
                        theme="primary"
                        @click.native="operateDialogVisiable = false">
                        {{$t('关闭')}}
                    </bk-button>
                </div>
            </bk-dialog>
        </div>
        <div class="ccn-list" v-else>
            <div class="main-container large boder-none">
                <div class="no-task">
                    <empty title="" :desc="$t('CodeCC集成了圈复杂度工具，可以检测过于复杂的代码，复杂度越高代码存在缺陷的风险越大')">
                        <template v-slot:action>
                            <bk-button size="large" theme="primary" @click="addTool({ from: 'ccn' })">{{$t('配置规则集')}}</bk-button>
                        </template>
                    </empty>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
    import { mapState } from 'vuex'
    import { bus } from '@/common/bus'
    import { getClosest, toggleClass, formatDiff } from '@/common/util'
    import util from '@/mixins/defect-list'
    import CodeMirror from '@/common/codemirror'
    import Empty from '@/components/empty'
    import { format } from 'date-fns'
    // eslint-disable-next-line
    import { export_json_to_excel } from 'vendor/export2Excel'

    export default {
        components: {
            Empty
        },
        mixins: [util],
        data () {
            const query = this.$route.query

            return {
                contentLoading: false,
                panels: [
                    { name: 'defect', label: this.$t('风险函数') },
                    { name: 'report', label: this.$t('数据报表') }
                ],
                defectSeverityMap: {
                    1: this.$t('极高'),
                    2: this.$t('高'),
                    4: this.$t('中'),
                    8: this.$t('低')
                },
                toolId: 'CCN',
                lintListData: {
                    defectList: {
                        content: [],
                        totalElements: 0
                    }
                },
                lintDetail: {},
                searchFormData: {
                    checkerList: [],
                    authorList: [],
                    filePathTree: {},
                    filePathShow: ''
                },
                operateParams: {
                    toolName: 'CCN',
                    ignoreReasonType: '',
                    ignoreReason: '',
                    changeAuthorType: 1, // 1:单个修改处理人，2:批量修改处理人，3:固定修改处理人
                    sourceAuthor: [],
                    targetAuthor: []
                },
                searchParams: {
                    taskId: this.$route.params.taskId,
                    toolName: 'CCN',
                    checker: query.checker || '',
                    author: query.author,
                    severity: this.numToArray(query.severity),
                    defectType: this.numToArray(query.defectType, [1, 2]),
                    status: query.status ? this.numToArray(query.status) : [1],
                    buildId: query.buildId ? query.buildId : '',
                    fileList: [],
                    daterange: [query.startTime, query.endTime],
                    sortField: query.sortField || 'ccn',
                    sortType: 'DESC',
                    pageNum: 1,
                    pageSize: 50
                },
                defectDetailSearchParams: {
                    sortField: '',
                    sortType: '',
                    pattern: '',
                    filePath: '',
                    entityId: undefined
                },
                codeViewerInDialog: null,
                isFilePathDropdownShow: false,
                isFileListLoadMore: false,
                isDefectListLoadMore: false,
                defectDetailDialogVisiable: false,
                authorEditDialogVisiable: false,
                ignoreReasonDialogVisiable: false,
                commentDialogVisiable: false,
                isBatchOperationShow: false,
                isFullScreen: false,
                defectComment: '',
                pagination: {
                    current: 1,
                    count: 1,
                    limit: 50
                },
                totalCount: 0,
                fileIndex: 0,
                codeMirrorDefaultCfg: {
                    lineNumbers: true,
                    scrollbarStyle: 'simple',
                    theme: 'summerfruit',
                    lineWrapping: true,
                    placeholder: this.emptyText,
                    firstLineNumber: 1,
                    readOnly: true
                },
                show: false,
                searchInput: '',
                emptyText: this.$t('未选择文件'),
                dialogAnalyseVisible: false,
                neverShow: false,
                newDefectJudgeTime: '',
                buildList: [],
                tableLoading: false,
                isSearchDropdown: false,
                lineAverageOpt: 10,
                screenHeight: 336,
                selectedLen: 0,
                isSelectAll: '',
                isSearch: false,
                hoverAuthorIndex: -1,
                commentList: [],
                commentParams: {
                    fileId: '',
                    toolName: 'CCN',
                    defectId: '',
                    commentId: '',
                    singleCommentId: '',
                    userName: this.$store.state.user.username,
                    comment: ''
                },
                operateDialogVisiable: false,
                isFetched: false,
                currentLintFile: {},
                exportLoading: false,
                ccnThreshold: 20
            }
        },
        computed: {
            ...mapState('tool', {
                toolMap: 'mapList'
            }),
            ...mapState('task', {
                taskDetail: 'detail'
            }),
            projectId () {
                return this.$route.params.projectId
            },
            taskId () {
                return this.$route.params.taskId
            },
            userName () {
                return this.$store.state.user.username
            },
            typeTips () {
                return this.$t('起始时间x之后产生的函数为新函数', { accessTime: this.newDefectJudgeTime })
            },
            breadcrumb () {
                const toolId = this.toolId
                let toolDisplayName = (this.toolMap[toolId] || {}).displayName || ''
                const names = [this.$route.meta.title || this.$t('风险函数')]
                if (toolDisplayName) {
                    toolDisplayName = this.$t(`${toolDisplayName}`)
                    names.unshift(toolDisplayName)
                }

                return { name: names.join(' / ') }
            },
            lintFileList () {
                return this.lintListData.defectList.content
            },
            // currentLintFile () {
            //     return this.lintFileList[this.fileIndex]
            // },
            defectList () {
                this.setScreenHeight()
                return this.lintListData.defectList.content
            },
            newDefectCount () {
                const newDefectCount = this.lintListData.newDefectCount
                return newDefectCount > 100000 ? this.$t('10万+') : newDefectCount
            },
            historyDefectCount () {
                const historyDefectCount = this.lintListData.historyDefectCount
                return historyDefectCount > 100000 ? this.$t('10万+') : historyDefectCount
            },
            statusTypeMap () {
                const { existCount, fixCount, ignoreCount } = this.lintListData
                return {
                    1: `${this.$t('待修复')}（${existCount || 0}）`,
                    2: `${this.$t('已修复')}（${fixCount || 0}）`,
                    4: `${this.$t('已忽略')}（${ignoreCount || 0}）`
                }
            },
            nextPageStartNum () {
                return (this.searchParams.pageNum - 1) * this.searchParams.pageSize + 1
            },
            nextPageEndNum () {
                let nextPageEndNum = this.nextPageStartNum + this.searchParams.pageSize - 1
                nextPageEndNum = this.totalCount < nextPageEndNum ? this.totalCount : nextPageEndNum
                return nextPageEndNum
            },
            ignoreReasonRequired () {
                return this.operateParams.ignoreReasonType === 4
            },
            ignoreReasonAble () {
                if (this.operateParams.ignoreReasonType === 4 && this.operateParams.ignoreReason === '') {
                    return true
                } else {
                    return !this.operateParams.ignoreReasonType
                }
            }
        },
        watch: {
            // 监听查询参数变化，则获取列表
            searchParams: {
                handler () {
                    if (this.isSearch) {
                        this.tableLoading = true
                        this.fetchLintList(!this.pageChange).then(list => {
                            if (this.pageChange) {
                                // 将一页的数据追加到列表
                                this.lintListData.defectList.content = this.lintListData.defectList.content.concat(list.defectList.content)

                                // 隐藏加载条
                                this.isFileListLoadMore = false

                                // 重置页码变更标记
                                this.pageChange = false
                            } else {
                                this.lintListData = { ...this.lintListData, ...list }
                                this.totalCount = this.pagination.count = this.lintListData.defectList.totalElements

                                // 重置文件下的问题详情
                                this.lintDetail = {}
                            }
                        }).finally(() => {
                            this.addTableScrollEvent()
                            this.tableLoading = false
                        })
                    }
                },
                deep: true
            },
            defectDetailSearchParams: {
                handler () {
                    this.emptyText = this.$t('未选择文件')
                    this.defectDetailDialogVisiable = true
                    this.fetchLintDetail()
                },
                deep: true
            },
            searchInput: {
                handler () {
                    if (this.searchFormData.filePathTree.children) {
                        if (this.searchInput) {
                            // this.searchFormData.filePathTree.expanded = true
                            this.openTree(this.searchFormData.filePathTree)
                        } else {
                            this.searchFormData.filePathTree.expanded = false
                        }
                    }
                },
                deep: true
            },
            defectDetailDialogVisiable: {
                handler () {
                    if (!this.defectDetailDialogVisiable) {
                        this.codeViewerInDialog.setValue('')
                        this.codeViewerInDialog.setOption('firstLineNumber', 1)
                    }
                },
                deep: true
            },
            taskDetail: {
                handler (newVal) {
                    if (newVal.enableToolList.find(item => item.toolName === 'CCN')) {
                        this.$nextTick(() => {
                            this.getQueryPreLineNum()
                        })
                    }
                },
                deep: true
            },
            isSearchDropdown () {
                this.setScreenHeight()
            }
        },
        created () {
            if (!this.taskDetail.nameEn || this.taskDetail.enableToolList.find(item => item.toolName === 'CCN')) {
                this.init(true)
                this.getBuildList()
            }
        },
        mounted () {
            // this.$nextTick(() => {
            //     this.getQueryPreLineNum()
            // })
            // 读取缓存中是否展示首次分析弹窗
            const neverShow = JSON.parse(window.localStorage.getItem('neverShow'))
            neverShow === null ? this.neverShow = false : this.neverShow = neverShow
            // 读取缓存中搜索项首次展示或收起
            const ccnSearchExpend = JSON.parse(window.localStorage.getItem('ccnSearchExpend'))
            ccnSearchExpend === null
                ? this.isSearchDropdown = true
                : this.isSearchDropdown = ccnSearchExpend
            window.addEventListener('resize', this.getQueryPreLineNum)
            this.openDetail()
            this.keyOperate()
        },
        beforeDestroy () {
            window.removeEventListener('resize', this.getQueryPreLineNum)
            document.onkeydown = null
        },
        methods: {
            async init (isInit) {
                isInit ? this.contentLoading = true : this.fileLoading = true
                await Promise.all([
                    this.fetchLintList(),
                    this.fetchLintParams()
                ]).then(([list, params]) => {
                    this.ccnThreshold = list.ccnThreshold
                    this.isSearch = true
                    if (isInit) {
                        this.contentLoading = false
                        this.isFetched = true
                    } else {
                        this.tableLoading = false
                    }
                    this.lintListData = { ...this.lintListData, ...list }
                    this.totalCount = this.pagination.count = this.lintListData.defectList.totalElements
                    this.newDefectJudgeTime = list.newDefectJudgeTime ? this.formatTime(list.newDefectJudgeTime, 'YYYY-MM-DD') : ''
                    this.addTableScrollEvent()

                    // todo 给文件路径树加上icon
                    function formatFilePath (filepath = {}) {
                        if (filepath && filepath.children && filepath.children.length) {
                            filepath.openedIcon = 'icon-folder-open'
                            filepath.closedIcon = 'icon-folder'
                            filepath.children.forEach(formatFilePath)
                        } else {
                            filepath.icon = 'icon-file'
                        }
                    }
                    formatFilePath(params.filePathTree)

                    this.searchFormData = Object.assign({}, this.searchFormData, params)
                }).catch(e => e)
                // 判断是否为切换到v2环境
                if (this.taskDetail.nameEn.indexOf('LD_') === 0 || this.taskDetail.nameEn.indexOf('DEVOPS_') === 0) {
                    this.dialogAnalyseVisible = !this.neverShow
                }
            },
            fetchLintList (isReset = false) {
                // 非分页条件变化重置分页
                if (isReset) {
                    this.searchParams.pageNum = 1
                    this.$refs.fileListTable.$refs.bodyWrapper.scrollTop = 0
                }
                const params = { ...this.searchParams }
                return this.$store.dispatch('defect/lintList', params)
            },
            async getBuildList () {
                this.buildList = await this.$store.dispatch('defect/getBuildList', { taskId: this.$route.params.taskId })
            },
            fetchLintParams () {
                const params = this.$route.params
                params.toolId = 'CCN'
                return this.$store.dispatch('defect/lintParams', params)
            },
            fetchLintDetail () {
                const pattern = this.toolMap[this.$route.params.toolId]['pattern']
                const params = { ...this.searchParams, ...this.defectDetailSearchParams, pattern }
                bus.$emit('show-app-loading')
                this.$store.dispatch('defect/lintDetail', params).then(detail => {
                    if (detail.code === '2300005') {
                        this.defectDetailDialogVisiable = false
                        setTimeout(() => {
                        this.$bkInfo({
                            subHeader: this.$createElement('p', {
                                style: {
                                    fontSize: '20px',
                                    lineHeight: '40px'
                                }
                            }, this.$t('无法获取问题的代码片段。请先将工蜂OAuth授权给蓝盾。')),
                            confirmFn: () => {
                                this.$store.dispatch('defect/oauthUrl', { toolName: this.toolId }).then(res => {
                                    window.open(res, '_blank')
                                })
                            }
                        })
                        }, 500)
                    } else {
                        this.lintDetail = { ...this.lintDetail, ...detail, codeComment: detail.codeComment }

                        this.currentLintFile = detail.defectVO || {}
                        // 查询详情后，全屏显示问题
                        this.handleCodeFullScreen()
                    }
                }).finally(() => {
                    bus.$emit('hide-app-loading')
                })
            },
            getDefectCountBySeverity (severity) {
                const severityFieldMap = {
                    1: 'superHighCount',
                    2: 'highCount',
                    4: 'mediumCount',
                    8: 'lowCount'
                }
                const count = this.lintListData[severityFieldMap[severity]]
                return count > 100000 ? this.$t('10万+') : count
            },
            handleSortChange ({ column, prop, order }) {
                const orders = { ascending: 'ASC', descending: 'DESC' }
                this.searchParams = { ...this.searchParams, ...{ pageNum: 1, sortField: prop, sortType: orders[order] } }
            },
            handleSelectionChange (selection) {
                this.selectedLen = selection.length || 0
                this.isBatchOperationShow = Boolean(selection.length)
                // 如果长度是最长，那么就是Y，否则是N
                this.isSelectAll = this.selectedLen === this.defectList.length ? 'Y' : 'N'
            },
            toSelectAll () {
                this.isSelectAll = this.selectedLen === this.defectList.length ? 'Y' : 'N'
            },
            handleSelectable (row, index) {
                // return !(row.status & 2)
                return true
            },
            handlePageChange (page) {
                this.pagination.current = page
                this.searchParams = { ...this.searchParams, ...{ pageNum: page } }
            },
            handlePageLimitChange (currentLimit) {
                this.pagination.current = 1 // 切换分页大小时要回到第一页
                this.searchParams = { ...this.searchParams, ...{ pageNum: 1, pageSize: currentLimit } }
            },
            handleStatus (status) {
                let key = 1
                if (status === 1) {
                    key = 1
                } else if (status & 2) {
                    key = 2
                } else if (status & 4) {
                    key = 4
                }
                const statusMap = {
                    1: this.$t('待修复'),
                    2: this.$t('已修复'),
                    4: this.$t('已忽略')
                }
                return statusMap[key]
            },
            handleMark (markFlag, batchFlag, entityId, defectId) {
                // markFlag 0: 取消标记, 1: 标记修改
                // batchFlag true: 批量操作
                let defectKeySet = []
                if (batchFlag) {
                    this.$refs.fileListTable.selection.map(item => {
                        defectKeySet.push(item.entityId)
                    })
                } else {
                    defectKeySet = [entityId]
                }
                const bizType = 'MarkDefect'
                let data = { ...this.operateParams, bizType, defectKeySet, markFlag }
                if (this.isSelectAll === 'Y') {
                    data = { ...data, isSelectAll: 'Y', queryDefectCondition: JSON.stringify(this.searchParams) }
                }
                this.tableLoading = true
                this.$store.dispatch('defect/batchEdit', data).then(res => {
                    if (res.code === '0') {
                        this.$bkMessage({
                            theme: 'success',
                            message: this.$t('修改成功')
                        })
                        if (batchFlag) {
                            this.init()
                        } else {
                            this.lintListData.defectList.content.forEach(item => {
                                if (item.entityId === entityId) {
                                    item.mark = markFlag
                                }
                            })
                            this.lintListData.defectList.content = this.lintListData.defectList.content.slice()
                        }
                        if (this.defectDetailDialogVisiable) this.fetchLintDetail()
                    }
                }).catch(e => {
                    console.error(e)
                }).finally(() => {
                    this.tableLoading = false
                })
            },
            handleIgnore (ignoreType, batchFlag, entityId, defectId) {
                this.operateParams.bizType = ignoreType
                this.operateParams.batchFlag = batchFlag
                if (batchFlag) {
                    const defectKeySet = []
                    this.$refs.fileListTable.selection.map(item => {
                        defectKeySet.push(item.entityId)
                    })
                    this.operateParams.defectKeySet = defectKeySet
                } else {
                    this.operateParams.defectKeySet = [entityId]
                }
                if (ignoreType === 'RevertIgnore') {
                    this.handleIgnoreConfirm()
                } else {
                    this.ignoreReasonDialogVisiable = true
                }
            },
            handleIgnoreConfirm () {
                let data = this.operateParams
                this.tableLoading = true
                this.ignoreReasonDialogVisiable = false
                if (this.isSelectAll === 'Y') {
                    data = { ...data, isSelectAll: 'Y', queryDefectCondition: JSON.stringify(this.searchParams) }
                }
                this.$store.dispatch('defect/batchEdit', data).then(res => {
                    if (res.code === '0') {
                        this.$bkMessage({
                            theme: 'success',
                            message: this.$t('修改成功')
                        })
                        if (data.batchFlag) {
                            this.init()
                        } else {
                            const index = this.lintListData.defectList.content.findIndex(item => item.entityId === data.defectKeySet[0])
                            this.lintListData.defectList.content.splice(index, 1)
                        }
                        if (this.defectDetailDialogVisiable) this.fetchLintDetail()
                        this.operateParams.ignoreReason = ''
                        this.defectDetailDialogVisiable = false
                    }
                }).catch(e => {
                    console.error(e)
                }).finally(() => {
                    this.tableLoading = false
                })
            },
            handleComent (id) {
                // 暂不做评论修
                // const hasComment = this.commentList.find(val => val.userName === this.userName)
                // this.commentParams.comment = hasComment ? hasComment.comment : ''
                // this.commentParams.singleCommentId = hasComment ? hasComment.singleCommentId : ''

                this.commentParams.comment = ''
                this.commentParams.defectId = id
                this.commentParams.commentId = this.lintDetail.codeComment ? this.lintDetail.codeComment.entityId : ''
                this.commentDialogVisiable = true
            },
            handleFileListRowClick (row, event, column) {
                this.fileIndex = this.defectList.findIndex(file => file.entityId === row.entityId)
                // 筛选后，问题详情为空，此时要把参数强制置空，不然点击文件不能触发请求
                if (!this.lintDetail.lintDefectList) {
                    this.defectDetailSearchParams.entityId = ''
                }
                this.defectDetailSearchParams.entityId = row.entityId
                this.defectDetailSearchParams.filePath = row.filePath
                this.screenScroll()
            },
            handleAuthorIndex (index) {
                this.hoverAuthorIndex = index
            },
            handleAuthor (changeAuthorType, entityId, author, defectId) {
                this.authorEditDialogVisiable = true
                this.operateParams.changeAuthorType = changeAuthorType
                this.operateParams.sourceAuthor = author
                this.operateParams.defectKeySet = [entityId]
            },
            handleCodeFullScreen () {
                // setTimeout(() => {
                //     const width = 700 - document.getElementsByClassName('filename')[0].offsetWidth
                //     if (document.getElementsByClassName('filepath')[0]) {
                //         document.getElementsByClassName('filepath')[0].style.width = width + 'px'
                //     }
                // }, 0)

                if (!this.codeViewerInDialog) {
                    const codeMirrorConfig = {
                        ...this.codeMirrorDefaultCfg,
                        ...{ autoRefresh: true }
                    }
                    this.codeViewerInDialog = CodeMirror(document.getElementById('codeViewerInDialog'), codeMirrorConfig)

                    this.codeViewerInDialog.on('update', () => {})
                }
                this.updateCodeViewer(this.codeViewerInDialog)
                this.codeViewerInDialog.refresh()
                setTimeout(this.scrollIntoView, 250)
            },
            // 代码展示相关
            updateCodeViewer (codeViewer) {
                if (!this.lintDetail.fileContent) {
                    this.emptyText = this.$t('文件内容为空')
                    return
                }
                const { fileName, fileContent, trimBeginLine, codeComment } = this.lintDetail
                const { mode } = CodeMirror.findModeByFileName(fileName)
                this.commentList = codeComment ? codeComment.commentList : []
                import(`codemirror/mode/${mode}/${mode}.js`).then(m => {
                    codeViewer.setOption('mode', mode)
                })
                codeViewer.setValue(fileContent)
                codeViewer.setOption('firstLineNumber', trimBeginLine === 0 ? 1 : trimBeginLine)

                this.buildLintHints(codeViewer)
            },
            // 创建问题提示块
            buildLintHints (codeViewer) {
                let checkerComment = ''
                const { trimBeginLine } = this.lintDetail
                const { ccn, startLines, endLines } = this.currentLintFile
                const ccnThreshold = this.ccnThreshold
                const hints = document.createElement('div')
                const checkerDetail = `
                    <div>
                        <p>${this.$t('如果多个函数存在相同代码路径片段，可以尝试以下技巧：')}</p>
                        <p>${this.$t('技巧名称：提炼函数')}</p>
                        <p>${this.$t('具体方法：将相同的代码片段独立成函数，并在之前的提取位置上条用该函数')}</p>
                        <p>${this.$t('示例代码：')}</p>
                        <pre>void Example(int val){
    if(val &lt; MAX_VAL){
        val = MAX_VAL;
    }
    for(int i = 0; i &lt; val; i++){
        doSomething(i);
    }
}</pre>
            <p>${this.$t('可以提炼成两个函数')}</p>
            <pre>int getValidVal(int val){
    if(val &lt; MAX_VAL){
        return MAX_VAL;
    }
    return val;
    }

    void Example(int val){
    val = getValidVal(val);
    for(int i = 0; i &lt; val; i++){
        doSomething(i);
    }
}</pre>
                    </div>`

                if (this.commentList.length) {
                    for (let i = 0; i < this.commentList.length; i++) {
                        checkerComment += `
                            <p class="comment-item">
                                <span class="info">
                                    <i class="codecc-icon icon-user-fill"></i>
                                    <span>${this.commentList[i]['userName']}</span>
                                    <span title="${this.commentList[i]['comment']}">${this.commentList[i]['comment']}</span>
                                </span>
                                <span class="handle">
                                    <span>${this.formatCommentTime(this.commentList[i]['commentTime'])}</span>
                                    <i class="bk-icon icon-delete" data-type="comment-${this.commentList[i]['singleCommentId']}"></i>
                                </span>
                            </p>`
                    }
                } else checkerComment = ''
                
                hints.innerHTML = `
                    <i class="lint-icon bk-icon icon-right-shape"></i>
                    <div class="lint-info">
                        <p>${this.$t('圈复杂度为，超过圈复杂度规则的阈值xx，请进行函数功能拆分降低代码复杂度。', { ccn: ccn, ccnThreshold: ccnThreshold })}</p>
                        <div class="checker-detail">${checkerDetail}</div>
                        ${checkerComment ? `<div class="checker-comment">${checkerComment}</div>` : ''}
                    </div>
                `

                hints.className = `lint-hints`
                codeViewer.addLineWidget(startLines - trimBeginLine, hints, {
                    coverGutter: false,
                    noHScroll: false,
                    above: true
                })
                for (let i = startLines - trimBeginLine; i <= endLines - trimBeginLine; i++) {
                    codeViewer.addLineClass(i, 'wrap', 'lint-hints-wrap main ccn')
                }
                setTimeout(this.scrollIntoView, 1)
            },
            // 默认滚动到问题位置
            scrollIntoView () {
                const { trimBeginLine } = this.lintDetail
                const codeViewer = this.codeViewerInDialog
                const startLines = this.currentLintFile.startLines - 1
                const top = codeViewer.charCoords({ line: startLines - trimBeginLine, ch: 0 }, 'local').top
                const lineHeight = codeViewer.defaultTextHeight()
                codeViewer.scrollTo(0, top - 5 * lineHeight)
            },
            handleCodeViewerInDialogClick (event, eventSource) {
                this.codeViewerClick(event, 'dialog-code')
            },
            codeViewerClick (event, eventSource) {
                const lintHints = getClosest(event.target, '.lint-hints')
                const commentCon = getClosest(event.target, '.checker-comment')
                const delHandle = getClosest(event.target, '.icon-delete')
                // 如果点击的是lint问题区域，展开修复建议
                if (lintHints && !commentCon) {
                    toggleClass(lintHints, 'active')
                }
                // 如果点击的是删除评论
                if (delHandle) {
                    const that = this
                    this.$bkInfo({
                        title: this.$t('删除评论'),
                        subTitle: this.$t('确定要删除该条评论吗？'),
                        maskClose: true,
                        confirmFn () {
                            const delData = delHandle.getAttribute('data-type')
                            const singleCommentId = delData.split('-').pop()
                            that.deleteComment(singleCommentId)
                        }
                    })
                }
            },
            // 删除评论
            deleteComment (id) {
                const params = {
                    commentId: this.lintDetail.codeComment.entityId,
                    singleCommentId: id,
                    toolName: 'CCN'
                }
                this.$store.dispatch('defect/deleteComment', params).then(res => {
                    if (res.code === '0') {
                        this.$bkMessage({
                            theme: 'success',
                            message: this.$t('删除成功')
                        })
                        this.commentParams.comment = ''
                        this.fetchLintDetail()
                    }
                })
            },
            // 处理人修改
            handleAuthorEdit () {
                this.$router.push({
                    name: 'task-settings-trigger'
                })
            },
            handleFilePathCancelClick () {
                const filePathDropdown = this.$refs.filePathDropdown
                filePathDropdown.hide()
            },
            openSlider () {
                this.show = true
            },
            numToArray (num, arr = [1, 2, 4, 8]) {
                let filterArr = arr.filter(x => x & num)
                filterArr = filterArr.length ? filterArr : arr
                return filterArr
            },
            openTree (arr) {
                if (arr.children) {
                    arr.expanded = true
                    arr.children.forEach(item => {
                        this.openTree(item)
                    })
                }
            },
            toLogs () {
                this.$router.push({
                    name: 'task-settings-trigger'
                })
            },
            changeItem (data) {
                this.neverShow = data
            },
            newAnalyse () {
                const routeParams = { ...this.$route.params, ...{ dialogAnalyseVisible: false } }
                this.dialogAnalyseVisible = false
                this.$router.push({
                    name: 'task-detail',
                    params: routeParams
                })
            },
            keyOperate () {
                const vm = this
                document.onkeydown = keyDown
                function keyDown (event) {
                    const e = event || window.event
                    if (e.target.nodeName !== 'BODY') return
                    switch (e.keyCode) {
                        case 13: // enter
                            // e.path.length < 5 防止规则等搜索条件里面的回车触发打开详情
                            if (!vm.defectDetailDialogVisiable && !vm.authorEditDialogVisiable && e.path.length < 5) vm.keyEnter()
                            break
                        case 27: // esc
                            if (vm.defectDetailDialogVisiable) vm.defectDetailDialogVisiable = false
                            break
                        case 37: // left
                            if (vm.defectDetailDialogVisiable && vm.fileIndex > 0) {
                                vm.handleFileListRowClick(vm.defectList[--vm.fileIndex])
                            } else if (!vm.defectDetailDialogVisiable && vm.fileIndex > 0) {
                                --vm.fileIndex
                            }
                            break
                        case 38: // up
                            if (!vm.defectDetailDialogVisiable && vm.fileIndex > 0) {
                                --vm.fileIndex
                            } else if (vm.defectDetailDialogVisiable && vm.fileIndex > 0) {
                                vm.handleFileListRowClick(vm.defectList[--vm.fileIndex])
                                return false
                            }
                            break
                        case 39: // right
                            if (vm.defectDetailDialogVisiable && (vm.fileIndex < vm.defectList.length - 1)) {
                                vm.handleFileListRowClick(vm.defectList[++vm.fileIndex])
                            } else if (!vm.defectDetailDialogVisiable && vm.fileIndex < vm.defectList.length - 1) {
                                ++vm.fileIndex
                            }
                            break
                        case 40: // down
                            if (!vm.defectDetailDialogVisiable && vm.fileIndex < vm.defectList.length - 1) {
                                ++vm.fileIndex
                            } else if (vm.defectDetailDialogVisiable && (vm.fileIndex < vm.defectList.length - 1)) {
                                vm.handleFileListRowClick(vm.defectList[++vm.fileIndex])
                                return false
                            }
                            break
                    }
                }
            },
            addTableScrollEvent () {
                this.$nextTick(() => {
                    // 滚动加载
                    if (this.$refs.fileListTable) {
                        const tableBodyWrapper = this.$refs.fileListTable.$refs.bodyWrapper

                        // 列表滚动加载
                        tableBodyWrapper.addEventListener('scroll', (event) => {
                            const dom = event.target
                            // 总页数
                            const totalPages = this.lintListData.defectList.totalPages
                            // 当前页码
                            const currentPageNum = this.searchParams.pageNum
                            // 是否滚动到底部
                            const hasScrolledToBottom = dom.scrollTop + dom.offsetHeight + 100 > dom.scrollHeight

                            // 触发翻页加载
                            if (hasScrolledToBottom && currentPageNum + 1 <= totalPages && this.isFileListLoadMore === false) {
                                // 显示加载条
                                this.isFileListLoadMore = true
                                // 变更页码触发查询
                                this.searchParams.pageNum += 1
                                // 标记为页面变更查询
                                this.pageChange = true
                            }
                        })
                    }
                })
            },
            keyEnter () {
                const row = this.defectList[this.fileIndex]
                if (!this.lintDetail.lintDefectList) {
                    this.defectDetailSearchParams.entityId = ''
                }
                this.defectDetailSearchParams.entityId = row.entityId
                this.defectDetailSearchParams.filePath = row.filePath
            },
            formatDate (dateNum, time) {
                return time ? format(dateNum, 'HH:mm:ss') : format(dateNum, 'YYYY-MM-DD HH:mm:ss')
            },
            screenScroll () {
                this.$nextTick(() => {
                    if (this.$refs.fileListTable.$refs.bodyWrapper) {
                        const childrens = this.$refs.fileListTable.$refs.bodyWrapper
                        const height = this.fileIndex > 3 ? (this.fileIndex - 3) * 42 : 0
                        childrens.scrollTo({
                            top: height,
                            behavior: 'smooth'
                        })
                    }
                }, 0)
            },
            setScreenHeight () {
                setTimeout(() => {
                    let smallHeight = 0
                    let largeHeight = 0
                    let tableHeight = 0
                    const i = this.lintListData.defectList.content.length || 0
                    if (this.$refs.fileListTable) {
                        const $main = document.getElementsByClassName('main-form')
                        smallHeight = $main.length > 0 ? $main[0].clientHeight : 0
                        largeHeight = this.$refs.mainContainer ? this.$refs.mainContainer.clientHeight : 0
                        tableHeight = this.$refs.fileListTable.$el.scrollHeight
                    }
                    this.screenHeight = i * 42 > tableHeight ? largeHeight - smallHeight - 62 : i * 42 + 43
                    this.screenHeight = this.screenHeight === 43 ? 336 : this.screenHeight
                }, 100)
            },
            toggleSearch () {
                this.isSearchDropdown = !this.isSearchDropdown
                window.localStorage.setItem('ccnSearchExpend', JSON.stringify(this.isSearchDropdown))
                this.getQueryPreLineNum()
            },
            getQueryPreLineNum () {
                const containerW = document.getElementsByClassName('search-form')[0].offsetWidth
                const childW = document.getElementsByClassName('cc-col')[0].offsetWidth
                const average = Math.floor(containerW / childW)
                this.lineAverageOpt = average
            },
            getFileName (path) {
                return path.split('/').pop()
            },
            handleAuthorEditConfirm () {
                let data = this.operateParams
                if (data.changeAuthorType === 2) {
                    const defectKeySet = []
                    this.$refs.fileListTable.selection.map(item => {
                        defectKeySet.push(item.entityId)
                    })
                    this.operateParams.defectKeySet = defectKeySet
                    data.defectKeySet = defectKeySet
                }
                data.bizType = 'AssignDefect'
                // data.sourceAuthor = data.sourceAuthor
                data.newAuthor = data.targetAuthor.split(',')
                if (this.isSelectAll === 'Y') {
                    data = { ...data, isSelectAll: 'Y', queryDefectCondition: JSON.stringify(this.searchParams) }
                }
                const dispatchUrl = data.changeAuthorType === 3 ? 'defect/authorEdit' : 'defect/batchEdit'
                this.authorEditDialogVisiable = false
                this.tableLoading = true
                this.$store.dispatch(dispatchUrl, data).then(res => {
                    if (res.code === '0') {
                        this.$bkMessage({
                            theme: 'success',
                            message: this.$t('修改成功')
                        })
                        if (data.changeAuthorType === 1) {
                            this.lintListData.defectList.content.forEach(item => {
                                if (item.entityId === data.defectKeySet[0]) {
                                    item.author = data.newAuthor.join()
                                }
                            })
                            this.lintListData.defectList.content = this.lintListData.defectList.content.slice()
                        } else {
                            this.init()
                        }
                        if (this.defectDetailDialogVisiable) this.fetchLintDetail()
                        this.operateParams.targetAuthor = []
                    }
                }).catch(e => {
                    console.error(e)
                }).finally(() => {
                    this.tableLoading = false
                })
            },
            handleCommentConfirm () {
                this.commentDialogVisiable = false
                // 暂不做修改评论
                // const url = this.commentParams.singleCommentId ? 'defect/updateComment' : 'defect/commentDefect'

                const url = 'defect/commentDefect'
                bus.$emit('show-app-loading')
                this.$store.dispatch(url, this.commentParams).then(res => {
                    if (res.code === '0') {
                        this.$bkMessage({
                            theme: 'success',
                            message: this.$t('评论成功')
                        })
                        this.commentParams.comment = ''
                        this.fetchLintDetail()
                    }
                })
            },
            formatCommentTime (time) {
                return formatDiff(time)
            },
            handleRowClassName ({ row, rowIndex }) {
                let rowClass = 'list-row'
                if (this.fileIndex === rowIndex) rowClass += ' current-row'
                return rowClass
            },
            getIgnoreReasonByType (type) {
                const typeMap = {
                    1: this.$t('检查工具误报'),
                    2: this.$t('设计如此'),
                    4: this.$t('其他')
                }
                return typeMap[type]
            },
            openDetail () {
                const id = this.$route.query.entityId
                if (id) {
                    setTimeout(() => {
                        if (!this.toolMap[this.toolId]) {
                            this.openDetail()
                        } else {
                            this.defectDetailSearchParams.entityId = id
                            this.defectDetailSearchParams.filePath = this.$route.query.filePath
                        }
                    }, 500)
                }
            },
            getSearchParams () {
                const params = { ...this.searchParams }
                return params
            },
            downloadExcel () {
                const params = this.getSearchParams()
                params.pageSize = 300000
                if (this.totalCount > 300000) {
                    this.$bkMessage({
                        message: this.$t('当前问题数已超过30万个，无法直接导出excel，请筛选后再尝试导出。')
                    })
                    return
                }
                this.exportLoading = true
                this.$store.dispatch('defect/lintList', params).then(res => {
                    const list = res && res.defectList && res.defectList.content
                    this.generateExcel(list)
                }).finally(() => {
                    this.exportLoading = false
                })
            },
            generateExcel (list = []) {
                const tHeader = [this.$t('位置'), this.$t('路径'), this.$t('函数名'), this.$t('圈复杂度'), this.$t('处理人'), this.$t('风险'), this.$t('提交日期'), this.$t('首次发现'), this.$t('最新状态')]
                const filterVal = ['fileName', 'filePath', 'functionName', 'ccn', 'author', 'riskFactor', 'latestDateTime', 'createBuildNumber', 'status', 'startLines']
                const data = this.formatJson(filterVal, list)
                const title = `${this.taskDetail.nameCn}-${this.taskDetail.taskId}-${this.toolId}-${this.$t('风险函数')}-${new Date().toISOString()}`
                export_json_to_excel(tHeader, data, title)
            },
            formatJson (filterVal, list) {
                return list.map(item => filterVal.map(j => {
                    if (j === 'fileName') {
                        return `${this.getFileName(item.filePath)}:${item.startLines}`
                    } else if (j === 'riskFactor') {
                        return this.defectSeverityMap[item.riskFactor]
                    } else if (j === 'latestDateTime') {
                        return this.formatTime(item.latestDateTime, 'YYYY-MM-DD')
                    } else if (j === 'createBuildNumber') {
                        return `#${item.createBuildNumber}`
                    } else if (j === 'status') {
                        return this.handleStatus(item.status)
                    } else if (j === 'startLines') {
                        return ''
                    } else {
                        return item[j]
                    }
                }))
            }
        }
    }
</script>

<style>
    @import './codemirror.css';
</style>

<style lang="postcss" scoped>
    @import '../../css/variable.css';
    @import '../../css/mixins.css';
    @import './defect-list.css';

    .ccn-list {
        padding: 16px 20px 0px 16px;
        .search-form {
            position: relative;
        }
        .cc-container {
            display: inline-block;
        }
        .cc-operate-more {
            >>>.icon-more {
                font-size: 20px;
            }
        }
    }
    .breadcrumb {
        padding: 0px!important;
        .breadcrumb-name {
            background: white;
        }
    }
    .main-container {
        /* padding: 20px 33px 0!important;
        margin: 0 -13px!important; */
        border-top: 1px solid #dcdee5;
        margin: 0px!important;
        background: white;
    }
    .cc-selected {
        float: left;
        height: 42px;
        line-height: 32px;
        font-size: 12px;
        color: #333;
        padding-right: 10px;
    }
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
    >>>.checkbox-group {
        font-size: 14px;
        .popover {
            position: relative;
            top: 1px;
            font-size: 16px;
        }
        .bk-checkbox-text {
            font-size: 12px;
        }
    }
    >>>.bk-date-picker {
        width: 345px;
    }
    .filepath-dropdown-content {
        color: #737987;

        .content-hd {
            margin: 0 16px 16px;
        }
        .content-bd {
            width: 480px;
            height: 360px;
            margin: 16px;
            overflow: auto;
        }
        .content-ft {
            border-top: 1px solid #ded8d8;
            text-align: center;
            padding: 12px 0;
            position: relative;

            .clear-btn {
                position: absolute;
                right: 8px;
            }
        }

        >>> .bk-tree .node-icon {
            margin: 0 4px;
        }
        >>> .bk-tree .tree-drag-node .tree-expanded-icon {
            margin: 0 4px;
        }
    }
    .filepath-name {
        width: 200px;
        text-align: left;
        display: inline-block;
        float: left;
        @mixin ellipsis;

        &+.bk-icon {
            right: 10px;
        }
    }
    .icon-mark {
        color: #53cad1;
        &.re-mark {
            color: #facc48;
        }
    }
    .code-fullscreen {
        display: flex;
        &.full-active {
            padding-top: 30px;
        }
        .toggle-full-icon {
            position: absolute;
            top: 11px;
            right: 38px;
            color: #979ba5;
            cursor: pointer;
        }
        .col-main {
            flex: 1;
            max-width: calc(100% - 250px);
        }
        .col-aside {
            flex: none;
            width: 240px;
            background: #f0f1f5;
            padding: 4px 20px;
            margin-left: 16px;
        }

        .file-bar {
            height: 36px;
            margin-top: 4px;

            .filemeta {
                position: relative;
                top: -4px;
                white-space: nowrap;
                font-size: 12px;
                border-left: 4px solid #3a84ff;
                padding-left: 8px;
                .filename {
                    font-size: 16px;
                }
                .filepath {
                    width: 700px;
                    display: inline-block;
                    vertical-align: bottom;
                    margin-left: 8px;
                    line-height: 24px;
                    @mixin ellipsis;
                }
            }
        }

        .toggle-file {
            text-align: center;
            display: flex;
            justify-content: space-between;
            margin: 10px 0;
        }

        .operate-section {
            height: 100%;
        }

        .basic-info {
            height: calc(100% - 60px);
            max-height: calc(100vh - 190px);
            overflow-y: scroll;
            margin-right: -29px;
            padding-right: 20px;
            &.full-screen-info {
                max-height: calc(100vh - 120px);
            }
            .title {
                font-size: 14px;
                color: #313238;
            }
            .block {
                padding: 5px 0;
                border-bottom: 1px dashed #c4c6cc;
                &:last-of-type {
                    border-bottom: none;
                }
                .item {
                    display: flex;
                    padding: 5px 0;

                    dt {
                        width: 90px;
                        flex: none;
                    }
                    dd {
                        color: #313238;
                        word-break: break-all;
                        &.small {
                            width: 80px;
                        }
                    }
                    a {
                        color: #313238;
                        word-break: break-all;
                    }

                    &.ignore {
                        display: block;
                    }

                    .item-button {
                        width: 200px;
                    }
                }
                .cc-mark {
                    width: 114px;
                    background: white;
                    border-radius: 12px;
                    padding: 0 8px;
                    line-height: 23px;
                    margin-left: 27px;
                }
            }
        }
    }
    #codeViewerInDialog {
        font-size: 14px;
        width: 100%;
        border: 1px solid #eee;
        border-left: none;
        border-right: none;
    }
    .un-full-code-viewer {
        height: calc(100vh - 164px);
    }
    .full-code-viewer {
        height: calc(100vh - 100px);
    }
    >>>.bk-label {
        font-size: 12px;
    }
    .table-append-loading {
        text-align: center;
        padding: 12px 0;
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
    .bk-date-picker {
        width: 304px;
    }
    >>>.bk-table {
        .mark-row {
            .cell {
                padding: 0;
            }
        }
    }
    .cc-radio {
        display:block;
        padding-bottom: 15px;
    }
    .cc-keyboard {
        float: right;
        height: 42px;
        font-size: 12px;
        line-height: 30px;
        color: #333;
        .cc-button {
            font-size: 12px;
            color: #699df4;
        }
    }
    .cc-table {
        position: relative;
        .cc-operate {
            display: inline-block;
            .cc-operate-buttons {
                display: flex;
                .cc-operate-button {
                    margin-left: 10px;
                }
            }
        }
    }
    .operate-footer {
        text-align: center;
    }
    .main-container::-webkit-scrollbar {
        width: 0;
    }
    .excel-download {
        line-height: 32px;
        cursor: pointer;
        padding-right: 10px;
        &:hover {
            color: #3a84ff;
        }
    }
    >>>.bk-button .bk-icon {
        .loading {
            color: #3a92ff;
        }
    }
</style>
<style lang="postcss">
    .file-detail-dialog {
        .bk-dialog {
            min-width: 960px;
        }
    }
</style>

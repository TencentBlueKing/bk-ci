<template>
    <div v-bkloading="{ isLoading: !mainContentLoading && contentLoading, opacity: 0.3 }">
        <section class="coverity-list"
            v-if="taskDetail.enableToolList.find(item => item.toolName === 'COVERITY' || item.toolName === 'KLOCWORK' || item.toolName === 'PINPOINT')">
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
                <!-- <div class="breadcrumb-extra">
                    <a @click="openSlider"><i class="bk-icon icon-order"></i>{{$t('操作记录')}}</a>
                </div> -->
            </div>

            <div class="main-container" ref="mainContainer">
                <div class="main-content-inner main-content-list">
                    <bk-form ref="bkForm" :label-width="60" class="search-form main-form">
                        <container class="cc-container">
                            <div class="cc-col">
                                <bk-form-item :label="$t('工具')">
                                    <bk-select v-model="toolId" @selected="handleSelectTool" :clearable="false" searchable>
                                        <bk-option-group
                                            v-for="group in toolList"
                                            :name="group.name"
                                            :show-count="false"
                                            :key="group.key">
                                            <bk-option v-for="option in group.toolList"
                                                :key="option.toolName"
                                                :id="option.toolName"
                                                :name="option.toolDisplayName">
                                            </bk-option>
                                        </bk-option-group>
                                    </bk-select>
                                </bk-form-item>
                            </div>
                            <div class="cc-col">
                                <bk-form-item :label="$t('规则')">
                                    <bk-select v-model="searchParams.checker" searchable :loading="selectLoading.otherParamsLoading">
                                        <bk-option
                                            v-for="(value, key) in listData.checkerMap"
                                            :key="key"
                                            :id="key"
                                            :name="`${key}（${value}）`"
                                        >
                                        </bk-option>
                                    </bk-select>
                                </bk-form-item>
                            </div>
                            <div class="cc-col" v-show="lineAverageOpt >= 3 || isSearchDropdown">
                                <bk-form-item :label="$t('处理人')">
                                    <bk-select v-model="searchParams.author" searchable :loading="selectLoading.otherParamsLoading">
                                        <bk-option
                                            v-for="(value, key) in listData.authorMap"
                                            :key="key"
                                            :id="key"
                                            :name="`${key}（${value}）`"
                                        >
                                        </bk-option>
                                    </bk-select>
                                    <bk-button @click="toChangeMember" :title="$t('批量修改问题处理人')" :text="true" class="change-handler">
                                        <i class="codecc-icon icon-handler-2"></i>
                                    </bk-button>
                                </bk-form-item>
                            </div>
                            <div class="cc-col" v-show="lineAverageOpt >= 4 || isSearchDropdown">
                                <bk-form-item :label="$t('日期')">
                                    <date-picker :date-range="searchParams.daterange" :handle-change="handleDateChange" :selected="dateType"></date-picker>
                                </bk-form-item>
                            </div>
                            <div class="cc-col" v-show="lineAverageOpt >= 5 || isSearchDropdown">
                                <bk-form-item :label="$t('路径')">
                                    <bk-select v-if="selectLoading.otherParamsLoading" :loading="true"></bk-select>
                                    <bk-dropdown-menu
                                        v-else
                                        trigger="click"
                                        ref="filePathDropdown"
                                        @show="isFilePathDropdownShow = true"
                                        @hide="isFilePathDropdownShow = false"
                                        :align="left"
                                    >
                                        <bk-button type="primary" slot="dropdown-trigger">
                                            <div class="filepath-name"
                                                :class="{ 'unselect': !searchFormData.filePathShow }"
                                                :title="searchFormData.filePathShow"
                                            >
                                                {{searchFormData.filePathShow ? searchFormData.filePathShow : $t('请选择')}}
                                            </div>
                                            <i :class="['bk-icon icon-angle-down', { 'icon-flip': isFilePathDropdownShow }]" style="color: #979ba5; position: absolute; right: -2px"></i>
                                        </bk-button>
                                        <div class="filepath-dropdown-content" slot="dropdown-content" @click.stop>
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
                            <div class="cc-col" v-show="lineAverageOpt >= 6 || isSearchDropdown">
                                <bk-form-item :label="$t('快照')">
                                    <bk-select v-model="searchParams.buildId" :clearable="true" searchable :loading="selectLoading.buildListLoading">
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
                            <div class="cc-col" v-show="lineAverageOpt >= 7 || isSearchDropdown">
                                <bk-form-item :label="$t('状态')">
                                    <bk-select multiple v-model="searchParams.status" :clearable="false" searchable :loading="selectLoading.otherParamsLoading">
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
                            <div class="cc-col" v-show="lineAverageOpt >= 8 || isSearchDropdown">
                                <bk-form-item :label="$t('级别')">
                                    <bk-checkbox-group v-model="searchParams.severity" class="checkbox-group">
                                        <bk-checkbox
                                            v-for="(value, key, index) in defectSeverityMap"
                                            :value="Number(key)"
                                            :key="index"
                                        >
                                            {{value}}(<em :class="['count', `count-${['major', 'minor', 'info'][index]}`]">{{getDefectCountBySeverity(key)}}</em>)
                                        </bk-checkbox>
                                    </bk-checkbox-group>
                                </bk-form-item>
                            </div>
                            <!-- <div class="cc-col" v-show="lineAverageOpt >= 9 || isSearchDropdown">
                                <bk-form-item :label="$t('时期')">
                                    <bk-checkbox-group v-model="searchParams.defectType" class="checkbox-group">
                                        <bk-checkbox
                                            v-for="(value, key, index) in defectTypeMap"
                                            :value="Number(key)"
                                            :key="index"
                                        >
                                            {{value}}({{getDefectCountByType(key)}})
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
                        </container>
                    </bk-form>

                    <div class="cc-table" ref="ccTable" v-bkloading="{ isLoading: tableLoading, opacity: 0.6 }">
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
                            <bk-button text ext-cls="cc-button" @click="operateDialog.visiable = true">{{$t('如何操作？')}}</bk-button>
                        </div>
                        <bk-table
                            v-show="isFetched"
                            class="file-list-table list-row"
                            ref="fileListTable"
                            v-bkloading="{ isLoading: fileLoading, opacity: 0.6 }"
                            :height="screenHeight"
                            :data="defectList"
                            @row-click="handleFileListRowClick"
                            @sort-change="handleSortChange"
                            @selection-change="handleSelectionChange"
                            @select-all="toSelectAll"
                        >
                            <bk-table-column :selectable="handleSelectable" type="selection" width="60" align="center">
                            </bk-table-column>
                            <bk-table-column width="15" class-name="mark-row">
                                <template slot-scope="props">
                                    <span v-if="props.row.status === 1 && props.row.mark" class="cc-icon-mark"></span>
                                </template>
                            </bk-table-column>
                            <bk-table-column :label="$t('ID')" prop="id" sortable="custom" width="70"></bk-table-column>
                            <bk-table-column :label="$t('文件名称')" prop="fileName" sortable="custom">
                                <template slot-scope="props">
                                    <span v-bk-tooltips="props.row.filePathname">{{props.row.fileName}}</span>
                                </template>
                            </bk-table-column>
                            <bk-table-column :label="$t('规则')" prop="checkerName"></bk-table-column>
                            <bk-table-column :label="$t('规则类型')" prop="displayCategory"></bk-table-column>
                            <bk-table-column :label="$t('类型子类')" prop="displayType"></bk-table-column>
                            <bk-table-column :label="$t('处理人')" prop="authorList" min-width="70">
                                <template slot-scope="props">
                                    <div
                                        v-if="props.row.status === 1"
                                        @mouseenter="handleAuthorIndex(props.$index)"
                                        @mouseleave="handleAuthorIndex(-1)"
                                        @click.stop="handleAuthor(1, props.row.entityId, props.row.authorList && props.row.authorList)">
                                        <span>{{props.row.authorList && props.row.authorList.join(';')}}</span>
                                        <span v-if="hoverAuthorIndex === props.$index" class="bk-icon icon-edit2 fs18"></span>
                                    </div>
                                    <div v-else>
                                        <span>
                                            {{props.row.authorList && props.row.authorList.join(';')}}
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
                                prop="createTime"
                                sortable="custom"
                                width="110"
                                :label="$t('创建日期')"
                                :formatter="(row, column, cellValue, index) => formatTime(cellValue, 'YYYY-MM-DD')">
                            </bk-table-column>
                            <bk-table-column :label="$t('最新状态')" prop="status" width="80">
                                <template slot-scope="props">
                                    <span>{{handleStatus(props.row.status)}}</span>
                                </template>
                            </bk-table-column>
                            <bk-table-column :label="$t('首次发现')" prop="createBuildNumber" sortable="custom" width="100">
                                <template slot-scope="props">
                                    <span>{{props.row.createBuildNumber ? '#' + props.row.createBuildNumber : '--'}}</span>
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
                                    <div>{{$t('暂无数据')}}</div>
                                </div>
                            </div>
                        </bk-table>
                    </div>

                    <bk-dialog
                        v-model="defectDetailDialogVisiable"
                        :fullscreen="isFullScreen"
                        :position="{ top: `${isFullScreen ? 0 : 50}` }"
                        :draggable="false"
                        :mask-close="false"
                        :show-footer="false"
                        :close-icon="true"
                        width="80%"
                    >
                        <div :class="['code-fullscreen', { 'full-active': isFullScreen }]">
                            <i class="bk-icon toggle-full-icon" :class="isFullScreen ? 'icon-un-full-screen' : 'icon-full-screen'" @click="setFullScreen"></i>
                            <div class="col-main">
                                <div class="file-bar">
                                    <div class="filemeta" v-if="currentFile">
                                        <strong class="filename">{{currentFile.fileName}}</strong>
                                    </div>
                                    <bk-button class="fr" @click="scrollTrace">{{$t('问题上下文')}}</bk-button>
                                    <bk-button class="fr mr10" theme="primary" @click="scrollIntoView()">{{$t('问题位置')}}</bk-button>
                                </div>
                                <div id="codeViewerInDialog" :class="isFullScreen ? 'full-code-viewer' : 'un-full-code-viewer'" @click="handleCodeViewerInDialogClick"></div>
                            </div>
                            <div class="col-aside">
                                <div class="operate-section">
                                    <div class="basic-info" v-if="currentFile">
                                        <div class="block">
                                            <div class="item">
                                                <span class="fail" v-if="currentFile.status === 1"><span class="cc-dot"></span>{{$t('待修复')}}</span>
                                                <span class="success" v-else-if="currentFile.status & 2"><span class="cc-dot"></span>{{$t('已修复')}}</span>
                                                <span class="warn" v-else-if="currentFile.status & 4"><span class="cc-dot"></span>{{$t('已忽略')}}</span>
                                                <span v-if="currentFile.status === 1 && currentFile.mark" class="cc-mark">
                                                    <span class="cc-icon-mark"></span>
                                                    <span>{{$t('已标记处理')}}</span>
                                                </span>
                                            </div>
                                            <div v-if="currentFile.status === 1" class="item">
                                                <bk-button v-if="currentFile.mark" class="item-button" @click="handleMark(0, false, currentFile.entityId)">
                                                    {{$t('取消标记')}}
                                                </bk-button>
                                                <bk-button v-else theme="primary" class="item-button" @click="handleMark(1, false, currentFile.entityId)">
                                                    {{$t('标记处理')}}
                                                </bk-button>
                                            </div>
                                            <div class="item">
                                                <bk-button v-if="currentFile.status & 4" class="item-button" @click="handleIgnore('RevertIgnore', false, currentFile.entityId)">
                                                    {{$t('恢复忽略')}}
                                                </bk-button>
                                                <bk-button v-else-if="!(currentFile.status & 2)" class="item-button" @click="handleIgnore('IgnoreDefect', false, currentFile.entityId)">
                                                    {{$t('忽略问题')}}
                                                </bk-button>
                                            </div>
                                        </div>
                                        <div class="block">
                                            <div class="item">
                                                <dt>{{$t('ID')}}</dt>
                                                <dd>{{currentFile.id}}</dd>
                                            </div>
                                            <div class="item">
                                                <dt>{{$t('级别')}}</dt>
                                                <dd>{{defectSeverityMap[currentFile.severity]}}</dd>
                                            </div>
                                        </div>
                                        <div class="block">
                                            <div class="item">
                                                <dt>{{$t('创建时间')}}</dt>
                                                <dd>{{formatTime(currentFile.createTime, 'YYYY-MM-DD')}}</dd>
                                            </div>
                                            <div class="item" v-if="currentFile.status & 2">
                                                <dt>{{$t('修复时间')}}</dt>
                                                <dd>{{formatTime(currentFile.repairTime, 'YYYY-MM-DD')}}</dd>
                                            </div>
                                            <div class="item">
                                                <dt v-if="currentFile.status === 1" class="curpt" @click.stop="handleAuthor(1, currentFile.entityId, currentFile.authorList)">
                                                    {{$t('处理人')}}
                                                    <span class="bk-icon icon-edit2 fs20"></span>
                                                </dt>
                                                <dt v-else>
                                                    {{$t('处理人')}}
                                                </dt>
                                                <dd>{{currentFile.authorList && currentFile.authorList.join(';')}}</dd>
                                            </div>
                                        </div>
                                        <div class="block" v-if="currentFile.status & 4">
                                            <div class="item">
                                                <dt>{{$t('忽略时间')}}</dt>
                                                <dd>{{formatTime(currentFile.ignoreTime, 'YYYY-MM-DD')}}</dd>
                                            </div>
                                            <div class="item">
                                                <dt>{{$t('忽略人')}}</dt>
                                                <dd>{{currentFile.ignoreAuthor}}</dd>
                                            </div>
                                            <div class="item disb">
                                                <dt>{{$t('忽略原因')}}</dt>
                                                <dd>{{getIgnoreReasonByType(currentFile.ignoreReasonType)}}
                                                    {{currentFile.ignoreReason ? '：' + currentFile.ignoreReason : ''}}
                                                </dd>
                                            </div>
                                        </div>
                                        <div class="block">
                                            <div class="item disb">
                                                <dt>{{$t('规则')}}</dt>
                                                <dd>{{currentFile.checkerName}}</dd>
                                            </div>
                                        </div>
                                        <div class="block">
                                            <div class="item disb">
                                                <dt>{{$t('代码库路径')}}</dt>
                                                <a target="_blank" :href="currentFile.filePathname">{{currentFile.filePathname}}</a>
                                            </div>
                                        </div>
                                    </div>
                                    <div class="toggle-file">
                                        <bk-button :disabled="fileIndex - 1 < 0" @click="handleFileListRowClick(defectList[--fileIndex])">{{$t('上一问题')}}</bk-button>
                                        <bk-button :disabled="fileIndex + 1 >= totalCount" @click="handleFileListRowClick(defectList[++fileIndex])">{{$t('下一问题')}}</bk-button>
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
                            <!-- <bk-member-selector v-model="operateParams.targetAuthor" style="width: 290px;"></bk-member-selector> -->
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
                        {{$t('确定')}}
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
                :title="$t('选择问题忽略原因')"
            >
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
                        {{$t('确定')}}
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
                v-model="operateDialog.visiable"
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
                        @click.native="operateDialog.visiable = false"
                    >
                        {{$t('关闭')}}
                    </bk-button>
                </div>
            </bk-dialog>
            <Record :visiable.sync="show" :data="this.$route.name" />
            <bk-dialog
                v-model="changeHandlerVisiable"
                theme="primary"
                :mask-close="false"
                :title="$t('跳转提示')">
                <span>{{$t('配置处理人转换，可将原处理人当前和未来的问题都分配给新处理人。')}}</span>
                <div style="padding-bottom: 19px;"></div>
                <bk-checkbox
                    :true-value="true"
                    :false-value="false"
                    v-model="memberNeverShow"
                    :value="true">
                    {{this.$t('不再提示')}}
                </bk-checkbox>
                <template slot="footer">
                    <bk-button theme="primary" @click="toLogs()">{{$t('去配置')}}</bk-button>
                    <bk-button @click="changeHandlerVisiable = false">{{$t('取消')}}</bk-button>
                </template>
            </bk-dialog>
        </section>
        <div class="coverity-list" v-else>
            <div class="main-container large boder-none">
                <div class="no-task">
                    <empty title="" :desc="$t('CodeCC集成了十余款工具，支持检查代码缺陷、安全漏洞、代码规范等问题')">
                        <template v-slot:action>
                            <bk-button size="large" theme="primary" @click="addTool({ from: 'cov' })">{{$t('配置规则集')}}</bk-button>
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
    import { getClosest, addClass, toggleClass } from '@/common/util'
    import util from '@/mixins/defect-list'
    import defectCache from '@/mixins/defect-cache'
    import CodeMirror from '@/common/codemirror'
    import Record from '@/components/operate-record/index'
    import Empty from '@/components/empty'
    import DatePicker from '@/components/date-picker/index'
    import { format } from 'date-fns'
    import { export_json_to_excel } from 'vendor/export2Excel'

    export default {
        components: {
            Record,
            Empty,
            DatePicker
        },
        mixins: [util, defectCache],
        data () {
            const query = this.$route.query
            const toolId = this.$route.params.toolId

            return {
                contentLoading: false,
                panels: [
                    { name: 'defect', label: this.$t('问题管理') },
                    { name: 'report', label: this.$t('数据报表') }
                ],
                tableLoading: false,
                fileLoading: false,
                isSearch: false,
                defectSeverityMap: {
                    1: this.$t('严重'),
                    2: this.$t('一般'),
                    4: this.$t('提示')
                },
                defectTypeMap: {
                    1: this.$t('新问题'),
                    2: this.$t('历史问题')
                },
                toolId: toolId,
                listData: {
                    defectList: {
                        content: [],
                        totalElements: 0
                    }
                },
                lintDetail: {},
                searchFormData: {
                    filePathTree: {},
                    filePathShow: ''
                },
                searchParams: {
                    taskId: this.$route.params.taskId,
                    toolName: toolId,
                    checker: query.checker || '',
                    author: query.author,
                    severity: this.numToArray(query.severity),
                    defectType: this.numToArray(query.defectType, [1, 2]),
                    status: query.status ? this.numToArray(query.status) : [1],
                    buildId: query.buildId ? query.buildId : '',
                    fileList: [],
                    daterange: [query.startTime, query.endTime],
                    sortField: query.sortField || 'severity',
                    sortType: 'DESC',
                    pageNum: 1,
                    pageSize: 100
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
                isBatchOperationShow: false,
                searchInput: '',
                emptyText: this.$t('未选择文件'),
                hoverAuthorIndex: -1,
                operateParams: {
                    toolName: toolId,
                    ignoreReasonType: '',
                    ignoreReason: '',
                    changeAuthorType: 1, // 1:单个修改处理人，2:批量修改处理人，3:固定修改处理人
                    sourceAuthor: [],
                    targetAuthor: []
                },
                isDropdownShow: false,
                operateDialog: {
                    visiable: false
                },
                changeHandlerVisiable: false,
                screenHeight: 336,
                selectedLen: 0,
                detailContent: this.$t('待补充...'),
                dialogAnalyseVisible: false,
                neverShow: false,
                memberNeverShow: false,
                newDefectJudgeTime: '',
                buildList: [],
                isSelectAll: '',
                dateType: query.dateType || 'createTime',
                cacheConfig: {
                    length: 0
                },
                isFetched: false,
                lineAverageOpt: 10,
                isSearchDropdown: false,
                isFullScreen: false,
                exportLoading: false,
                selectLoading: {
                    otherParamsLoading: false,
                    buildListLoading: false
                }
            }
        },
        computed: {
            ...mapState('tool', {
                toolMap: 'mapList'
            }),
            ...mapState('task', {
                taskDetail: 'detail'
            }),
            typeTips () {
                return this.$t('起始时间x之后产生的问题为新问题', { accessTime: this.newDefectJudgeTime })
            },
            breadcrumb () {
                const toolId = this.toolId
                let toolDisplayName = (this.toolMap[toolId] || {}).displayName || ''
                const names = [this.$route.meta.title || this.$t('代码问题')]
                if (toolDisplayName) {
                    toolDisplayName = this.$t(`${toolDisplayName}`)
                    names.unshift(toolDisplayName)
                }

                return { name: names.join(' / ') }
            },
            defectList (val) {
                return this.listData.defectList.content
            },
            currentFile () {
                return { ...this.lintDetail.defectDetailVO, fileName: this.lintDetail.fileName }
                // return this.defectList[this.fileIndex]
            },
            mainDefect () {
                const tracesList = this.lintDetail.defectDetailVO.defectInstances[0].traces || [{}]
                const mainTrace = tracesList.find(item => item.main) || tracesList[0]
                return mainTrace
            },
            statusTypeMap () {
                const { existCount, fixCount, ignoreCount } = this.listData
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
                let nextPageEndNum = this.nextPageStartNum + this.searchParams.pageSize
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
            },
            searchParamsWatch () {
                return JSON.parse(JSON.stringify(this.searchParams))
            }
        },
        watch: {
            // 监听查询参数变化，则获取列表
            searchParamsWatch: {
                handler (newVal, oldVal) {
                    // 比如在第二页，筛选条件发生变化，要回到第一页
                    if (newVal.pageNum !== 1 && newVal.pageNum === oldVal.pageNum) {
                        this.searchParams.pageNum = 1
                        this.$refs.fileListTable.$refs.bodyWrapper.scrollTo(0, 0)
                        return
                    }
                    if (this.isSearch) {
                        this.tableLoading = true
                        this.fetchLintList().then(list => {
                            if (this.pageChange) {
                                // 将一页的数据追加到列表
                                this.listData.defectList.content = this.listData.defectList.content.concat(list.defectList.content)

                                // 隐藏加载条
                                this.isFileListLoadMore = false

                                // 重置页码变更标记
                                this.pageChange = false
                            } else {
                                this.listData = { ...this.listData, ...list }
                                this.totalCount = this.pagination.count = this.listData.defectList.totalElements
                                // 重置文件下的问题详情
                                this.lintDetail = {}
                            }
                        }).finally(() => {
                            // this.fileIndex = 0
                            this.addTableScrollEvent()
                            this.tableLoading = false
                        })
                    }
                },
                deep: true
            },
            defectDetailSearchParams: {
                handler (val) {
                    this.emptyText = this.$t('未选择文件')
                    bus.$emit('show-app-loading')
                    this.defectDetailDialogVisiable = true
                    const entityId = val.entityId
                    if (this.defectCache[entityId]) {
                        this.detailContent = this.checkerContentCache && this.checkerContentCache[this.checkerKey] && this.checkerContentCache[this.checkerKey].codeExample
                        this.lintDetail = this.defectCache[entityId]
                        this.handleCodeFullScreen()
                        this.fetchLintDetail()
                    } else {
                        this.fetchLintDetail('first')
                    }
                    this.preloadCache(this.defectList, this.cacheConfig)
                },
                deep: true
            },
            searchInput: {
                handler (val) {
                    if (this.searchFormData.filePathTree.children) {
                        if (val) {
                            // this.searchFormData.filePathTree.expanded = true
                            this.openTree(this.searchFormData.filePathTree)
                        } else {
                            this.searchFormData.filePathTree.expanded = false
                        }
                    }
                },
                deep: true
            },
            defectDetailDialogVisiable (val) {
                if (!val) {
                    this.codeViewerInDialog.setValue('')
                    this.codeViewerInDialog.setOption('firstLineNumber', 1)
                }
            },
            changeHandlerVisiable: {
                handler () {
                    if (!this.changeHandlerVisiable) {
                        window.localStorage.setItem('memberNeverShow', JSON.stringify(this.memberNeverShow))
                    }
                },
                deep: true
            },
            taskDetail: {
                handler (newVal) {
                    this.$nextTick(() => {
                        this.getQueryPreLineNum()
                    })
                },
                deep: true
            },
            defectList (val, oldVal) {
                this.preloadCache(val, this.cacheConfig)
                this.setTableHeight()
            }
        },
        created () {
            if (!this.taskDetail.nameEn || this.taskDetail.enableToolList.find(item => item.toolName === 'COVERITY' || item.toolName === 'KLOCWORK' || item.toolName === 'PINPOINT')) {
                this.init(true)
            }
        },
        async mounted () {
            // 读取缓存中是否展示首次分析弹窗
            const neverShow = JSON.parse(window.localStorage.getItem('neverShow'))
            this.neverShow = neverShow === null ? false : neverShow
            const memberNeverShow = JSON.parse(window.localStorage.getItem('memberNeverShow'))
            memberNeverShow === null
                ? this.memberNeverShow = false
                : this.memberNeverShow = memberNeverShow
            // 读取缓存中搜索项首次展示或收起
            const lintSearchExpend = JSON.parse(window.localStorage.getItem('lintSearchExpend'))
            lintSearchExpend === null
                ? this.isSearchDropdown = true
                : this.isSearchDropdown = lintSearchExpend
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
                this.selectLoading.otherParamsLoading = true
                const list = await this.fetchLintList()

                this.selectLoading.buildListLoading = true
                this.buildList = await this.$store.dispatch('defect/getBuildList', { taskId: this.$route.params.taskId })
                this.selectLoading.buildListLoading = false

                this.newDefectJudgeTime = list.newDefectJudgeTime ? this.formatTime(list.newDefectJudgeTime, 'YYYY-MM-DD') : ''
                this.listData = { ...this.listData, ...list }
                this.totalCount = this.pagination.count = this.listData.defectList.totalElements
                if (isInit) {
                    this.contentLoading = false
                    this.isFetched = true
                } else {
                    this.tableLoading = false
                }
                this.isSearch = true
                this.addTableScrollEvent()
                const { filePathTree } = list

                // 判断是否为切换到v2环境
                if (this.taskDetail.nameEn.indexOf('LD_') === 0 || this.taskDetail.nameEn.indexOf('DEVOPS_') === 0) {
                    this.dialogAnalyseVisible = !this.neverShow
                }

                this.searchFormData = { ...this.searchFormData, filePathTree }
                this.selectLoading.otherParamsLoading = false
            },
            fetchLintList () {
                const params = this.getSearchParams()

                return this.$store.dispatch('defect/lintList', params)
            },
            async fetchLintDetail (type, extraParams = {}) {
                const pattern = this.toolMap[this.toolId]['pattern']
                const params = { ...this.searchParams, ...this.defectDetailSearchParams, pattern, ...extraParams }
                const checkerContent = await this.getWarnContent()
                if (!this.checkerContentCache) {
                    this.checkerContentCache = {}
                }
                this.checkerContentCache[this.checkerKey] = checkerContent
                this.detailContent = checkerContent.codeExample
                this.$store.dispatch('defect/lintDetail', params).then(detail => {
                    if (detail.defectDetailVO) {
                        if (!extraParams.entityId) {
                            this.lintDetail = detail

                            // 查询详情后，全屏显示问题
                            if (type === 'first') {
                                this.handleCodeFullScreen(type)
                            }
                            const cacheConfig = { ...this.cacheConfig, index: this.fileIndex }
                            this.preloadCache(this.defectList, cacheConfig)
                        }
                        const updateCacheKey = params['entityId']
                        this.updateCache(updateCacheKey, detail)
                    } else if (detail.code === '2300005') {
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
                    } else if (detail.response) {
                        this.cacheConfig.length = 0
                        this.preloadCache(this.defectList, this.cacheConfig)
                        this.clear()
                    }
                }).finally(() => {
                    bus.$emit('hide-app-loading')
                })
            },
            getWarnContent () {
                if (this.checkerContentCache && this.checkerContentCache[this.checkerKey]) return this.checkerContentCache[this.checkerKey]
                return this.$store.dispatch('defect/getWarnContent', { toolName: this.toolId, checkerKey: this.checkerKey })
            },
            keyOperate () {
                const vm = this
                document.onkeydown = keyDown
                function keyDown (event) {
                    const e = event || window.event
                    if (e.target.nodeName !== 'BODY') return
                    switch (e.code) {
                        case 'Enter': // enter
                            // e.path.length < 5 防止规则等搜索条件里面的回车触发打开详情
                            if (!vm.defectDetailDialogVisiable && !vm.authorEditDialogVisiable && e.path.length < 5) vm.keyEnter()
                            break
                        case 'Escape': // esc
                            if (vm.defectDetailDialogVisiable) vm.defectDetailDialogVisiable = false
                            break
                        case 'ArrowLeft': // left
                            if (vm.fileIndex > 0) {
                                if (vm.defectDetailDialogVisiable) {
                                    vm.handleFileListRowClick(vm.defectList[--vm.fileIndex])
                                } else {
                                    --vm.fileIndex
                                }
                                vm.addCurrentRowClass()
                                vm.screenScroll()
                            }
                            break
                        case 'ArrowUp': // up
                            if (vm.fileIndex > 0) {
                                if (vm.defectDetailDialogVisiable) {
                                    vm.handleFileListRowClick(vm.defectList[--vm.fileIndex])
                                } else {
                                    --vm.fileIndex
                                }
                                vm.addCurrentRowClass()
                                vm.screenScroll()
                            }
                            break
                        case 'ArrowRight': // right
                            if (vm.fileIndex < vm.defectList.length - 1) {
                                if (vm.defectDetailDialogVisiable) {
                                    vm.handleFileListRowClick(vm.defectList[++vm.fileIndex])
                                } else {
                                    ++vm.fileIndex
                                }
                                vm.addCurrentRowClass()
                                vm.screenScroll()
                            }
                            break
                        case 'ArrowDown': // down
                            if (vm.fileIndex < vm.defectList.length - 1) {
                                if (vm.defectDetailDialogVisiable) {
                                    vm.handleFileListRowClick(vm.defectList[++vm.fileIndex])
                                } else {
                                    ++vm.fileIndex
                                }
                                vm.addCurrentRowClass()
                                vm.screenScroll()
                            }
                            break
                    }
                }
            },
            addTableScrollEvent () {
                this.$nextTick(() => {
                    // 滚动加载
                    this.fileLoading = false
                    this.addCurrentRowClass()
                    if (this.$refs.fileListTable) {
                        const tableBodyWrapper = this.$refs.fileListTable.$refs.bodyWrapper

                        // 问题文件列表滚动加载
                        tableBodyWrapper.addEventListener('scroll', (event) => {
                            const dom = event.target
                            // 总页数
                            const totalPages = this.listData.defectList.totalPages
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
            getDefectCountBySeverity (severity) {
                const severityFieldMap = {
                    1: 'seriousCount',
                    2: 'normalCount',
                    4: 'promptCount'
                }
                const count = this.listData[severityFieldMap[severity]] || 0
                return count > 100000 ? this.$t('10万+') : count
            },
            getDefectCountByType (type) {
                const tpyeFieldMap = {
                    1: 'newCount',
                    2: 'historyCount'
                }
                const count = this.listData[tpyeFieldMap[type]] || 0
                return count > 100000 ? this.$t('10万+') : count
            },
            getIgnoreReasonByType (type) {
                const typeMap = {
                    1: this.$t('检查工具误报'),
                    2: this.$t('设计如此'),
                    4: this.$t('其他')
                }
                return typeMap[type]
            },
            handleSortChange ({ column, prop, order }) {
                const orders = { ascending: 'ASC', descending: 'DESC' }
                this.searchParams = { ...this.searchParams, pageNum: 1, sortField: prop, sortType: orders[order] }
            },
            handlePageChange (page) {
                this.pagination.current = page
                this.searchParams = { ...this.searchParams, pageNum: page }
            },
            handlePageLimitChange (currentLimit) {
                this.pagination.current = 1 // 切换分页大小时要回到第一页
                this.searchParams = { ...this.searchParams, pageNum: 1, pageSize: currentLimit }
            },
            handleSelectionChange (selection) {
                this.selectedLen = selection.length || 0
                this.isBatchOperationShow = Boolean(selection.length)
                // 如果长度是最长，那么就是Y，否则是N
                this.isSelectAll = this.selectedLen === this.defectList.length ? 'Y' : 'N'
            },
            handleFileListRowClick (row, event, column) {
                this.checkerKey = row.checkerName
                // this.$store.dispatch('defect/getWarnContent', { toolName: this.toolId, checkerKey: row.checkerName }).then(res => {
                //     this.detailContent = res.codeExample
                // })
                this.fileIndex = this.defectList.findIndex(file => file.entityId === row.entityId)
                // 筛选后，问题详情为空，此时要把参数强制置空，不然点击文件不能触发请求
                if (!this.lintDetail.lintDefectList) {
                    this.defectDetailSearchParams.entityId = ''
                }
                this.defectDetailSearchParams.entityId = row.entityId
                this.defectDetailSearchParams.filePath = row.filePathname
                this.addCurrentRowClass()
                this.screenScroll()
            },
            keyEnter () {
                this.checkerKey = this.defectList[this.fileIndex].checkerName
                // this.checkerContentCache
                // this.$store.dispatch('defect/getWarnContent', { toolName: this.toolId, checkerKey: this.defectList[this.fileIndex].checkerName }).then(res => {
                //     this.detailContent = res.codeExample
                // })
                const row = this.defectList[this.fileIndex]
                if (!this.lintDetail.lintDefectList) {
                    this.defectDetailSearchParams.entityId = ''
                }
                this.defectDetailSearchParams.entityId = row.entityId
                this.defectDetailSearchParams.filePath = row.filePathname
            },
            // 表格行加当前高亮样式
            addCurrentRowClass () {
                const defectListTable = this.$refs.fileListTable
                if (defectListTable) {
                    const defectListTableBodyWrapper = defectListTable.$refs.bodyWrapper
                    const rows = defectListTableBodyWrapper.querySelectorAll('tr')
                    const currentRow = rows[this.fileIndex]
                    if (rows.length && currentRow) {
                        rows.forEach(el => el.classList.remove('current-row'))
                        addClass(currentRow, 'current-row')
                    }
                }
            },
            handleCodeFullScreen () {
                // setTimeout(() => {
                //     const width = 700 - document.getElementsByClassName('filename')[0].offsetWidth
                //     document.getElementsByClassName('filepath')[0].style.width = width + 'px'
                // }, 0)

                if (!this.codeViewerInDialog) {
                    setTimeout(() => {
                        const codeMirrorConfig = {
                            ...this.codeMirrorDefaultCfg,
                            autoRefresh: true
                        }
                        this.codeViewerInDialog = CodeMirror(document.getElementById('codeViewerInDialog'), codeMirrorConfig)

                        this.codeViewerInDialog.on('update', () => {})
                        this.updateCodeViewer(this.codeViewerInDialog)
                        this.codeViewerInDialog.refresh()
                        setTimeout(this.scrollIntoView, 10)
                        return false
                    }, 250)
                }
                this.updateCodeViewer(this.codeViewerInDialog)
                setTimeout(this.scrollIntoView, 10)
            },
            // 代码展示相关
            updateCodeViewer (codeViewer) {
                if (!codeViewer) return
                const fileMD5 = this.mainDefect.fileMD5
                const { contents, startLine, filePathname } = this.lintDetail.defectDetailVO.fileInfoMap[fileMD5]
                if (!contents) {
                    this.emptyText = this.$t('文件内容为空')
                    return
                }
                this.currentFile.startLine = startLine
                const codeMirrorMode = CodeMirror.findModeByFileName(filePathname)
                if (codeMirrorMode && codeMirrorMode.mode) {
                    const mode = codeMirrorMode.mode
                    import(`codemirror/mode/${mode}/${mode}.js`).then(m => {
                        codeViewer.setOption('mode', mode)
                    })
                }
                if (codeViewer) {
                    codeViewer.setValue(contents)
                    codeViewer.setOption('firstLineNumber', startLine === 0 ? 1 : startLine)
                    this.buildLintHints(codeViewer, startLine)
                }
            },
            // 创建问题提示块
            buildLintHints (codeViewer, startLine) {
                const defectList = this.lintDetail.defectDetailVO.defectInstances[0].traces
                const { detailContent } = this
                this.currentFile.locatedIndex = 0
                this.currentFile.locatedArr = []
                defectList.forEach(defect => {
                    const { traceNumber, lineNumber, message, main } = defect
                    const { displayType, checkerName } = this.currentFile
                    const messageDom = document.createElement('div')
                    messageDom.className = 'checker-detail'
                    messageDom.style.maxHeight = '300px'
                    messageDom.style.overflow = 'auto'
                    messageDom.innerHTML = detailContent || this.$t('待补充...')

                    const checkerDom = document.createElement('p')
                    checkerDom.innerText = `${traceNumber ? traceNumber + '.' : ''}${message || ''}`
                    this.currentFile.locatedArr.push(lineNumber)
                    const hints = document.createElement('div')
                    if (main) {
                        hints.innerHTML = `
                            <i class="lint-icon bk-icon icon-right-shape"></i>
                            <div class="lint-info">
                                <p>${checkerDom.outerHTML}</p>
                                <p>${displayType}(${checkerName})</p>
                                ${messageDom.outerHTML}
                            </div>
                        `
                        codeViewer.addLineClass(lineNumber - startLine, 'wrap', 'lint-hints-wrap main')
                    } else {
                        hints.innerHTML = `
                            <div class="lint-info">
                                <p>${checkerDom.outerHTML}</p>
                            </div>
                        `
                        codeViewer.addLineClass(lineNumber - startLine, 'wrap', 'lint-hints-wrap')
                    }
                    hints.className = `lint-hints`
                    codeViewer.addLineWidget(lineNumber - startLine, hints, {
                        coverGutter: false,
                        noHScroll: false,
                        above: true
                    })
                })
                this.scrollIntoView()
            },
            // 问题上下文
            scrollTrace () {
                let locatedIndex = this.currentFile.locatedIndex
                const { locatedArr, startLine } = this.currentFile
                const locatedArrLen = locatedArr.length
                const codeViewer = this.codeViewerInDialog
                // 先清除上个tarce的class
                const prevIndex = locatedIndex < 1 ? locatedArrLen - 1 : locatedIndex - 1
                codeViewer.removeLineClass(locatedArr[prevIndex] - startLine, 'wrap', 'defect-trace')
                codeViewer.addLineClass(locatedArr[locatedIndex] - startLine, 'wrap', 'defect-trace')
                this.scrollIntoView(locatedArr[locatedIndex])
                this.currentFile.locatedIndex = (++locatedIndex < locatedArrLen ? locatedIndex : 0)
            },
            // 默认滚动到问题位置
            scrollIntoView (number) {
                const codeViewer = this.codeViewerInDialog
                if (!codeViewer) return false
                const startLine = this.currentFile.startLine
                const middleHeight = codeViewer.getScrollerElement().offsetHeight / 2
                const lineHeight = codeViewer.defaultTextHeight()
                let lineNumber = number
                if (!number) {
                    lineNumber = this.mainDefect.lineNumber
                    codeViewer.removeLineClass(lineNumber - startLine, 'wrap', 'defect-trace')
                }
                setTimeout(() => {
                    codeViewer.scrollIntoView({ line: lineNumber - startLine, ch: 0 }, middleHeight - lineHeight)
                    bus.$emit('hide-app-loading')
                }, 1)
            },
            handleCodeViewerInDialogClick (event, eventSource) {
                this.codeViewerClick(event, 'dialog-code')
            },
            codeViewerClick (event, eventSource) {
                const lintHints = getClosest(event.target, '.lint-hints')

                // 如果点击的是lint问题区域，展开修复建议
                if (lintHints) {
                    toggleClass(lintHints, 'active')
                }
            },
            handleFilePathCancelClick () {
                const filePathDropdown = this.$refs.filePathDropdown
                filePathDropdown.hide()
            },
            openSlider () {
                this.show = true
            },
            numToArray (num, arr = [1, 2, 4]) {
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
            handleSelectable (row, index) {
                return !(row.status & 2)
            },
            handleMark (markFlag, batchFlag, entityId) {
                // markFlag 0: 取消标记, 1: 标记修改
                // batchFlag true: 批量操作
                let defectKeySet = []
                if (batchFlag) {
                    defectKeySet = this.$refs.fileListTable.selection.map(item => item.entityId)
                } else {
                    defectKeySet = [entityId]
                }
                const bizType = 'MarkDefect'
                const data = { ...this.operateParams, bizType, defectKeySet, markFlag }
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
                            this.listData.defectList.content.forEach(item => {
                                if (item.entityId === entityId) {
                                    item.mark = markFlag
                                }
                            })
                            this.listData.defectList.content = this.listData.defectList.content.slice()
                        }
                        if (this.defectDetailDialogVisiable) this.fetchLintDetail()
                    }
                }).catch(e => {
                    console.error(e)
                }).finally(() => {
                    this.tableLoading = false
                })
            },
            handleAuthorIndex (index) {
                this.hoverAuthorIndex = index
            },
            handleAuthor (changeAuthorType, id, author) {
                this.authorEditDialogVisiable = true
                this.operateParams.changeAuthorType = changeAuthorType
                this.operateParams.sourceAuthor = author
                this.operateParams.defectKeySet = [id]
            },
            // 处理人修改
            handleAuthorEditConfirm () {
                const data = this.operateParams
                if (data.changeAuthorType === 2) {
                    data.defectKeySet = this.$refs.fileListTable.selection.map(item => item.entityId)
                }
                data.bizType = 'AssignDefect'
                // data.sourceAuthor = data.sourceAuthor
                data.newAuthor = data.targetAuthor.split(',')
                const dispatchUrl = data.changeAuthorType === 3 ? 'defect/authorEdit' : 'defect/batchEdit'
                this.authorEditDialogVisiable = false
                this.tableLoading = true
                this.$store.dispatch(dispatchUrl, data).then(res => {
                    if (res.code === '0') {
                        this.$bkMessage({
                            theme: 'success',
                            message: this.$t('修改成功')
                        })
                        this.operateParams.targetAuthor = []
                        if (data.changeAuthorType === 1) {
                            this.listData.defectList.content.forEach(item => {
                                if (item.entityId === data.defectKeySet[0]) {
                                    item.authorList = data.newAuthor
                                }
                            })
                            this.listData.defectList.content = this.listData.defectList.content.slice()
                        } else {
                            this.init()
                        }
                        if (this.defectDetailDialogVisiable) this.fetchLintDetail()
                    }
                }).catch(e => {
                    console.error(e)
                }).finally(() => {
                    this.tableLoading = false
                })
            },
            handleIgnore (ignoreType, batchFlag, id) {
                this.operateParams.bizType = ignoreType
                if (batchFlag) {
                    this.operateParams.defectKeySet = this.$refs.fileListTable.selection.map(item => item.entityId)
                } else {
                    this.operateParams.defectKeySet = [id]
                }
                if (ignoreType === 'RevertIgnore') {
                    this.handleIgnoreConfirm()
                } else {
                    this.ignoreReasonDialogVisiable = true
                }
            },
            handleIgnoreConfirm () {
                this.tableLoading = true
                this.ignoreReasonDialogVisiable = false
                this.$store.dispatch('defect/batchEdit', this.operateParams).then(res => {
                    if (res.code === '0') {
                        this.$bkMessage({
                            theme: 'success',
                            message: this.$t('修改成功')
                        })
                        if (this.operateParams.batchFlag) {
                            this.init()
                        } else {
                            const index = this.listData.defectList.content.findIndex(item => item.entityId === this.operateParams.defectKeySet[0])
                            this.listData.defectList.content.splice(index, 1)
                        }
                        this.operateParams.ignoreReason = ''
                        this.defectDetailDialogVisiable = false
                    }
                }).catch(e => {
                    console.error(e)
                }).finally(() => {
                    this.tableLoading = false
                })
            },
            toLogs () {
                this.changeHandlerVisiable = false
                this.$router.push({
                    name: 'task-settings-trigger'
                })
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
                })
            },
            handleAuthorEdit () {
                this.$router.push({
                    name: 'task-settings-trigger'
                })
            },
            changeItem (data) {
                this.neverShow = data
            },
            newAnalyse () {
                const routeParams = { ...this.$route.params, dialogAnalyseVisible: false }
                this.dialogAnalyseVisible = false
                this.$router.push({
                    name: 'task-detail',
                    params: routeParams
                })
            },
            toChangeMember () {
                if (this.memberNeverShow) {
                    this.$router.push({
                        name: 'task-settings-trigger'
                    })
                } else {
                    this.changeHandlerVisiable = true
                }
            },
            formatDate (dateNum, time) {
                return time ? format(dateNum, 'HH:mm:ss') : format(dateNum, 'YYYY-MM-DD HH:mm:ss')
            },
            toSelectAll () {
                this.isSelectAll = this.selectedLen === this.defectList.length ? 'Y' : 'N'
            },
            openDetail () {
                const id = this.$route.query.entityId
                if (id) {
                    setTimeout(() => {
                        if (!this.toolMap[this.toolId]) {
                            this.openDetail()
                        } else {
                            this.defectDetailSearchParams.entityId = id
                        }
                    }, 500)
                }
            },
            toggleSearch () {
                this.isSearchDropdown = !this.isSearchDropdown
                window.localStorage.setItem('lintSearchExpend', JSON.stringify(this.isSearchDropdown))
                this.getQueryPreLineNum()
                this.setTableHeight()
            },
            getQueryPreLineNum () {
                const containerW = document.getElementsByClassName('search-form')[0].offsetWidth
                const childW = document.getElementsByClassName('cc-col')[0].offsetWidth
                const average = Math.floor(containerW / childW)
                this.lineAverageOpt = average
            },
            setTableHeight () {
                setTimeout(() => {
                    let smallHeight = 0
                    let largeHeight = 0
                    let tableHeight = 0
                    const i = this.listData.defectList.content.length || 0
                    if (this.$refs.fileListTable) {
                        const $main = document.getElementsByClassName('main-form')
                        smallHeight = $main.length > 0 ? $main[0].clientHeight : 0
                        largeHeight = this.$refs.mainContainer ? this.$refs.mainContainer.clientHeight : 0
                        tableHeight = this.$refs.fileListTable.$el.scrollHeight
                    }
                    this.screenHeight = i * 42 > tableHeight ? largeHeight - smallHeight - 73 : i * 42 + 43
                    this.screenHeight = this.screenHeight === 43 ? 336 : this.screenHeight
                }, 100)
            },
            setFullScreen () {
                this.isFullScreen = !this.isFullScreen
            },
            getSearchParams () {
                const daterange = this.searchParams.daterange
                const isSelectAll = this.isSelectAll
                const params = { ...this.searchParams, isSelectAll }
                const startTime = this.dateType === 'createTime' ? 'startCreateTime' : 'startFixTime'
                const endTime = this.dateType === 'createTime' ? 'endCreateTime' : 'endFixTime'

                params[startTime] = daterange[0] || ''
                params[endTime] = daterange[1] || ''

                return params
            },
            downloadExcel () {
                const params = this.getSearchParams()
                params.pageSize = 300000
                if (this.totalCount > 300000) {
                    this.$bkMessage({
                        message: this.$t('当前问题数已超过30万个，暂时不支持导出excel，请筛选后再尝试导出。')
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
                const tHeader = [this.$t('序号'), this.$t('entityId'), this.$t('ID'), this.$t('文件名称'), this.$t('路径'), this.$t('规则'), this.$t('规则类型'), this.$t('类型子类'), this.$t('处理人'), this.$t('级别'), this.$t('创建日期'), this.$t('最新状态'), this.$t('首次发现')]
                const filterVal = ['index', 'entityId', 'id', 'fileName', 'filePathname', 'checkerName', 'displayCategory', 'displayType', 'authorList', 'severity', 'createTime', 'status', 'createBuildNumber']
                const data = this.formatJson(filterVal, list)
                const title = `${this.taskDetail.nameCn}-${this.taskDetail.taskId}-${this.toolId}-${this.$t('问题')}-${new Date().toISOString()}`
                export_json_to_excel(tHeader, data, title)
            },
            // 处理表格数据
            formatJson (filterVal, list) {
                let index = 1
                return list.map(item => filterVal.map(j => {
                    if (j === 'index') {
                        return index++
                    } else if (j === 'severity') {
                        return this.defectSeverityMap[item.severity]
                    } else if (j === 'authorList') {
                        return item.authorList.toString()
                    } else if (j === 'createTime') {
                        return this.formatTime(item.createTime, 'YYYY-MM-DD HH:mm:ss')
                    } else if (j === 'createBuildNumber') {
                        return `#${item.createBuildNumber}`
                    } else if (j === 'status') {
                        return this.handleStatus(item.status)
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

    .coverity-list {
        padding: 16px 20px 0px 16px;
    }
    .breadcrumb {
        padding: 0px !important;
        .breadcrumb-name {
            background: white;
        }
    }
    .main-container {
        /* padding: 20px 33px 0!important;
        margin: 0 -13px!important; */
        border-top: 1px solid #dcdee5;
        margin: 0px !important;
        background: white;
        .change-handler {
            position: relative;
            top: -32px;
            left: 310px;
        }
        .codecc-icon {
            font-size: 14px;
        }
        .icon-empty {
            font-size: 50px;
        }
        .bk-button-text {
            color: #63656e;
            :hover {
                color: #699df4;
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
    .cc-table {
        padding: 0px 15px 15px 15px;
        background: #fff;
        .cc-operate {
            display: inline-block;
            .cc-operate-buttons {
                display: flex;
                .cc-operate-button {
                    margin-left: 10px;
                }
            }
        }
        .cc-selected {
            float: left;
            height: 42px;
            font-size: 12px;
            line-height: 26px;
            color: #333;
            padding-right: 10px;
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
        .cc-operate-more {
            >>>.icon-more {
                font-size: 20px;
            }
        }
    }
    .file-list-table {
        >>> .list-row {
            cursor: pointer;
            &.grey-row {
                color: #c3cdd7;
            }
        }
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
    .code-fullscreen {
        display: flex;
        &.full-active {
            padding-top: 29px;
        }
        .col-main {
            flex: 1;
            max-width: calc(100% - 250px);
        }
        .col-aside {
            flex: none;
            width: 240px;
            background: #f0f1f5;
            padding: 12px 20px;
            margin-left: 16px;
        }

        .file-bar {
            height: 42px;

            .filemeta {
                display: inline-block;
                margin-top: -2px;
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
            .title {
                font-size: 14px;
                color: #313238;
            }
            .block {
                padding: 5px 0;
                border-bottom: 1px dashed #c4c6cc;
                &:last-of-type {
                    border-bottom: none;
                    padding-bottom: 50px;
                }
                .item {
                    display: flex;
                    padding: 5px 0;

                    dt {
                        width: 90px;
                        flex: none;
                    }
                    dd {
                        flex: 1;
                        color: #313238;
                        word-break: break-all;
                    }
                    a {
                        color: #313238;
                        word-break: break-all;
                    }
                    .item-button {
                        width: 200px;
                    }
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
        height: calc(100vh - 200px);
    }
    .full-code-viewer {
        height: calc(100vh - 100px);
    }
    .author-edit {
        padding: 34px 18px 11px;
        .tips {
            position: absolute;
            top: 66px;
            left: 23px;
            .bk-icon {
                margin-right: 2px;
                color: #ffd695;
            }
            color: #979ba5;
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
    >>>.bk-date-picker {
        width: 304px;
    }
    .cc-radio {
        display:block;
        padding-bottom: 15px;
    }
    .cc-icon-mark {
        display: inline-block;
        background: url(../../images/mark.svg) no-repeat;
        height: 14px;
        width: 14px;
        margin-bottom: -2px;
    }
    .cc-mark {
        width: 114px;
        background: white;
        border-radius: 12px;
        padding: 0 8px;
        line-height: 23px;
        margin-left: 27px;
    }
    .defect-type-tips {
        top: 5px;
    }
    >>>.bk-label {
        font-size: 12px;
    }
    .operate-footer {
        text-align: center;
    }
    .table-append-loading {
        text-align: center;
        padding: 12px 0;
    }
    >>>.bk-table {
        .mark-row {
            .cell {
                padding: 0;
            }
        }
    }
    >>>.bk-option-content-default {
        overflow: hidden;
        white-space: nowrap;
        text-overflow: ellipsis;
    }
    .main-container::-webkit-scrollbar {
        width: 0;
    }
    .toggle-full-icon {
        position: absolute;
        top: 10px;
        right: 35px;
        color: #979ba5;
        cursor: pointer;
        &.icon-un-full-screen {
            top: 10px;
        }
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

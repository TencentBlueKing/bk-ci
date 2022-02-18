<template>
    <div v-bkloading="{ isLoading: !mainContentLoading && contentLoading, opacity: 0.3 }">
        <section class="coverity-list"
            v-if="taskDetail.enableToolList.find(item => item.toolName !== 'CCN' && item.toolName !== 'DUPC')">
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
                                        <bk-option-group
                                            v-for="group in searchFormData.checkerList"
                                            :name="group.typeName"
                                            :show-count="false"
                                            :key="group">
                                            <bk-option
                                                v-for="checker in group.checkers"
                                                :key="checker"
                                                :id="checker"
                                                :name="checker">
                                            </bk-option>
                                        </bk-option-group>
                                    </bk-select>
                                </bk-form-item>
                            </div>
                            <div class="cc-col" v-show="lineAverageOpt >= 5 || isSearchDropdown">
                                <bk-form-item :label="$t('处理人')">
                                    <bk-select v-model="searchParams.author" searchable :loading="selectLoading.otherParamsLoading">
                                        <bk-option
                                            v-for="author in searchFormData.authorList"
                                            :key="author"
                                            :id="author"
                                            :name="author">
                                        </bk-option>
                                    </bk-select>
                                    <bk-button @click="toChangeMember" :title="$t('批量修改问题处理人')" :text="true" class="change-handler">
                                        <i class="codecc-icon icon-handler-2"></i>
                                    </bk-button>
                                </bk-form-item>
                            </div>
                            <div class="cc-col" v-show="lineAverageOpt >= 8 || isSearchDropdown">
                                <bk-form-item :label="$t('日期')">
                                    <bk-date-picker v-model="searchParams.daterange" type="daterange"></bk-date-picker>
                                </bk-form-item>
                            </div>
                            <div class="cc-col" v-show="lineAverageOpt >= 7 || isSearchDropdown">
                                <bk-form-item :label="$t('路径')" class="fixed-width">
                                    <bk-select v-if="selectLoading.otherParamsLoading" :loading="true"></bk-select>
                                    <bk-dropdown-menu v-else @show="isFilePathDropdownShow = true" @hide="isFilePathDropdownShow = false" align="left" trigger="click" ref="filePathDropdown">
                                        <bk-button type="primary" slot="dropdown-trigger">
                                            <div style="font-size: 12px" class="filepath-name" :class="{ 'unselect': !searchFormData.filePathShow }" :title="searchFormData.filePathShow">{{searchFormData.filePathShow ? searchFormData.filePathShow : $t('请选择')}}</div>
                                            <i :class="['bk-icon icon-angle-down', { 'icon-flip': isFilePathDropdownShow }]" style="color: #979ba5; position: absolute; right: -2px"></i>
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
                            <div class="cc-col" v-show="lineAverageOpt >= 9 || isSearchDropdown">
                                <bk-form-item :label="$t('快照')">
                                    <bk-select v-model="searchParams.buildId" :clearable="true" searchable :loading="selectLoading.buildListLoading">
                                        <bk-option
                                            v-for="item in buildList"
                                            :key="item.buildId"
                                            :id="item.buildId"
                                            :name="`#${item.buildNum}构建 ${formatDate(item.buildTime) || ''} ${item.buildUser || ''}`">
                                        </bk-option>
                                    </bk-select>
                                </bk-form-item>
                            </div>
                            <div class="cc-col" v-show="lineAverageOpt >= 6 || isSearchDropdown">
                                <bk-form-item :label="$t('状态')">
                                    <bk-select multiple v-model="searchParams.status" :clearable="false" searchable :loading="selectLoading.statusLoading">
                                        <bk-option
                                            v-for="(value, key) in statusTypeMap"
                                            :key="Number(key)"
                                            :id="Number(key)"
                                            :disabled="searchParams.clusterType === 'file' && Number(key) !== 1"
                                            :name="value">
                                            <span v-bk-tooltips="searchParams.clusterType === 'file' && Number(key) !== 1 ? '仅支持按问题聚类方式查看' : ''">{{value}}</span>
                                        </bk-option>
                                    </bk-select>
                                </bk-form-item>
                            </div>
                            <div class="cc-col" v-show="lineAverageOpt >= 3 || isSearchDropdown">
                                <bk-form-item :label="$t('级别')">
                                    <bk-checkbox-group v-model="searchParams.severity" class="checkbox-group">
                                        <bk-checkbox
                                            v-for="(value, key, index) in defectSeverityMap"
                                            :value="Number(key)"
                                            :key="index">
                                            {{value}}(<em :class="['count', `count-${['major', 'minor', 'info'][index]}`]">{{getDefectCountBySeverity(key)}}</em>)
                                        </bk-checkbox>
                                    </bk-checkbox-group>
                                </bk-form-item>
                            </div>
                            <!-- <div class="cc-col" v-show="lineAverageOpt >= 4 || isSearchDropdown">
                                <bk-form-item :label="$t('时期')">
                                    <bk-checkbox-group v-model="searchParams.defectType" class="checkbox-group">
                                        <bk-checkbox
                                            v-for="(value, key, index) in defectTypeMap"
                                            :value="Number(key)"
                                            :key="index">
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
                            <div class="cc-col" v-show="lineAverageOpt >= 10 || isSearchDropdown">
                                <bk-form-item :label="$t('聚类')">
                                    <bk-radio-group v-model="searchParams.clusterType">
                                        <bk-radio :value="'defect'">按问题</bk-radio>
                                        <bk-radio :value="'file'">按文件</bk-radio>
                                    </bk-radio-group>
                                </bk-form-item>
                            </div>
                        </container>
                    </bk-form>

                    <div class="cc-table" ref="ccTable" v-bkloading="{ isLoading: tableLoading, opacity: 0.6 }">
                        <div class="cc-selected">
                            <span v-if="searchParams.clusterType === 'file'">
                                {{$t('已选择w文件(x问题)，共y文件(z问题)', {
                                    w: isSelectAll === 'Y' ? totalCount : selectedLen,
                                    x: selectedDefectCount,
                                    y: totalCount,
                                    z: totalDefectCount
                                })}}
                            </span>
                            <span v-else>{{$t('已选择x问题，共y问题', { x: isSelectAll === 'Y' ? totalCount : selectedLen, y: totalCount })}}</span>
                            <span v-if="gatherFile.fileCount" class="extra-file" @click="goToLog">
                                {{$t('另有x个大文件y问题', { x: gatherFile.fileCount, y: gatherFile.defectCount })}}
                                <bk-popover placement="top" width="300" class="popover">
                                    <i class="codecc-icon icon-tips"></i>
                                    <div slot="content">
                                        {{$t('对于部分大文件产生的海量告警，CodeCC已将其归档。可前往工具分析记录下载文件查看详情')}}
                                    </div>
                                </bk-popover>
                            </span>
                        </div>
                        <p class="search-more-option">
                            <i :class="['bk-icon codecc-icon icon-codecc-arrow', { 'icon-flip': isSearchDropdown }]"
                                @click.stop="toggleSearch">
                            </i>
                        </p>
                        <div v-if="isBatchOperationShow" class="cc-operate pb10">
                            <div class="cc-operate-buttons">
                                <bk-dropdown-menu v-if="searchParams.clusterType === 'defect'" @show="isDropdownShow = true" @hide="isDropdownShow = false">
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
                                <bk-button size="small" ext-cls="cc-operate-button" v-if="searchParams.clusterType === 'defect'" @click="handleAuthor(2)" theme="primary">{{$t('分配')}}</bk-button>
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
                        <table-file
                            v-if="searchParams.clusterType === 'file'"
                            v-show="isFetched"
                            ref="table"
                            :list="defectList"
                            :screen-height="screenHeight"
                            :file-index="fileIndex"
                            :handle-mark="handleMark"
                            :handle-ignore="handleIgnore"
                            :handle-sort-change="handleSortChange"
                            :handle-selection-change="handleSelectionChange"
                            :to-select-all="toSelectAll"
                            :handle-file-list-row-click="handleFileListRowClick">
                        </table-file>
                        <table-defect
                            v-else
                            v-show="isFetched"
                            ref="table"
                            :list="defectList"
                            :screen-height="screenHeight"
                            :file-index="fileIndex"
                            :handle-mark="handleMark"
                            :handle-ignore="handleIgnore"
                            :handle-author="handleAuthor"
                            :handle-sort-change="handleSortChange"
                            :handle-selection-change="handleSelectionChange"
                            :to-select-all="toSelectAll"
                            :handle-file-list-row-click="handleFileListRowClick">
                        </table-defect>
                    </div>

                    <bk-dialog
                        v-model="defectDetailDialogVisiable"
                        :ext-cls="'file-detail-dialog'"
                        :fullscreen="isFullScreen"
                        :position="{ top: `${isFullScreen ? 0 : 50}` }"
                        :draggable="false"
                        :mask-close="false"
                        :show-footer="false"
                        :close-icon="true"
                        width="80%">
                        <detail
                            ref="detail"
                            :type="searchParams.clusterType"
                            :is-loading.sync="detailLoading"
                            :is-full-screen.sync="isFullScreen"
                            :visiable="defectDetailDialogVisiable"
                            :file-index="fileIndex"
                            :entity-id="defectDetailSearchParams.entityId"
                            :total-count="totalCount"
                            :current-file="currentFile"
                            :handle-mark="handleMark"
                            :handle-coment="handleComent"
                            :delete-comment="deleteComment"
                            :handle-ignore="handleIgnore"
                            :handle-author="handleAuthor"
                            :trigger-row-click="triggerRowClick"
                            :lint-detail="lintDetail">
                        </detail>
                    </bk-dialog>

                </div>
            </div>
            <bk-dialog
                v-model="authorEditDialogVisiable"
                width="560"
                theme="primary"
                :mask-close="false"
                header-position="left"
                :title="operateParams.changeAuthorType === 1 ? $t('修改问题处理人') : $t('批量修改问题处理人')">
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
                        :disabled="(operateParams.changeAuthorType === 3) || !operateParams.targetAuthor"
                        :loading="authorEditDialogLoading"
                        @click.native="handleAuthorEditConfirm">
                        {{operateParams.changeAuthorType === 1 ? $t('确定') : $t('批量修改')}}
                    </bk-button>
                    <bk-button
                        theme="primary"
                        type="button"
                        :disabled="authorEditDialogLoading"
                        @click.native="authorEditDialogVisiable = false">
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
                :title="operateParams.batchFlag ? $t('选择问题忽略原因，共x个问题', { num: selectedDefectCount || (isSelectAll === 'Y' ? totalCount : selectedLen) }) : $t('选择问题忽略原因')">
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
                        @click.native="handleIgnoreConfirm">
                        {{operateParams.batchFlag ? $t('批量忽略') : $t('确定')}}
                    </bk-button>
                    <bk-button
                        theme="primary"
                        @click.native="ignoreReasonDialogVisiable = false">
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
            <bk-dialog
                v-model="commentDialogVisiable"
                width="560"
                theme="primary"
                :mask-close="false"
                header-position="left"
                :title="$t('问题评论')">
                <div class="pd10 pr50">
                    <bk-form :model="commentParams" :label-width="30" class="search-form">
                        <bk-form-item property="comment" :required="true">
                            <bk-input :placeholder="$t('请输入你的评论内容')" :type="'textarea'" :maxlength="200" v-model="commentParams.comment"></bk-input>
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
        </section>
        <div class="coverity-list" v-else>
            <div class="main-container large boder-none">
                <div class="no-task">
                    <empty title="" :desc="$t('CodeCC集成了十余款工具，支持检查代码缺陷、安全漏洞、代码规范等问题')">
                        <template v-slot:action>
                            <bk-button size="large" theme="primary" @click="addTool({ from: 'lint' })">{{$t('配置规则集')}}</bk-button>
                        </template>
                    </empty>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
    import { mapGetters, mapState } from 'vuex'
    import { getClosest, toggleClass } from '@/common/util'
    import { format } from 'date-fns'
    import util from '@/mixins/defect-list'
    import defectCache from '@/mixins/defect-cache'
    // import CodeMirror from '@/common/codemirror'
    import Record from '@/components/operate-record/index'
    import Empty from '@/components/empty'
    import tableFile from './table-file'
    import tableDefect from './table-defect'
    import detail from './detail'
    // eslint-disable-next-line
    import { export_json_to_excel } from 'vendor/export2Excel'

    export default {
        components: {
            Record,
            Empty,
            tableFile,
            tableDefect,
            detail
        },
        mixins: [util, defectCache],
        data () {
            const query = this.$route.query
            const toolId = this.$route.params.toolId

            return {
                contentLoading: false,
                detailLoading: false,
                panels: [
                    { name: 'defect', label: this.$t('问题管理') },
                    { name: 'report', label: this.$t('数据报表') }
                ],
                tableLoading: false,
                isSearch: false,
                defectSeverityMap: {
                    1: this.$t('严重'),
                    2: this.$t('一般'),
                    4: this.$t('提示')
                },
                defectSeverityDetailMap: {
                    1: this.$t('严重'),
                    2: this.$t('一般'),
                    3: this.$t('提示')
                },
                defectCountMap: {
                    1: 'seriousCount',
                    2: 'normalCount',
                    4: 'promptCount'
                },
                defectTypeMap: {
                    1: this.$t('新问题'),
                    2: this.$t('历史问题')
                },
                toolId: toolId,
                listData: {
                    defectList: {
                        records: [],
                        count: 0
                    }
                },
                lintDetail: {
                    lintDefectList: []
                },
                searchFormData: {
                    authorList: [],
                    checkerList: [],
                    filePathTree: {},
                    existCount: 0, // 待修复
                    fixCount: 0, // 已修复
                    ignoreCount: 0, // 已忽略
                    newCount: 0,
                    historyCount: 0,
                    seriousCount: 0,
                    normalCount: 0,
                    promptCount: 0
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
                    clusterType: query.clusterType || 'defect',
                    sortField: query.sortField || 'fileName',
                    sortType: 'ASC',
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
                isFilePathDropdownShow: false,
                isFileListLoadMore: false,
                isDefectListLoadMore: false,
                defectDetailDialogVisiable: false,
                authorEditDialogVisiable: false,
                ignoreReasonDialogVisiable: false,
                commentDialogVisiable: false,
                pagination: {
                    current: 1,
                    count: 1,
                    limit: 50
                },
                totalCount: 0,
                fileIndex: 0,
                selectedDefectCount: 0,
                totalDefectCount: 0,
                show: false,
                isBatchOperationShow: false,
                searchInput: '',
                emptyText: this.$t('未选择文件'),
                operateParams: {
                    toolName: toolId,
                    ignoreReasonType: '',
                    ignoreReason: '',
                    changeAuthorType: 1, // 1:单个修改处理人，2:批量修改处理人，3:固定修改处理人
                    sourceAuthor: '',
                    targetAuthor: []
                },
                isDropdownShow: false,
                operateDialogVisiable: false,
                changeHandlerVisiable: false,
                screenHeight: 336,
                selectedLen: 0,
                dialogAnalyseVisible: false,
                neverShow: false,
                memberNeverShow: false,
                newDefectJudgeTime: '',
                buildList: [],
                isSelectAll: '',
                lineAverageOpt: 10,
                isSearchDropdown: false,
                isFullScreen: false,
                isFetched: false,
                commentParams: {
                    toolName: toolId,
                    defectId: '',
                    commentId: '',
                    singleCommentId: '',
                    userName: this.$store.state.user.username,
                    comment: ''
                },
                gatherFile: {},
                exportLoading: false,
                selectLoading: {
                    otherParamsLoading: false,
                    statusLoading: false,
                    buildListLoading: false
                }
            }
        },
        computed: {
            ...mapGetters(['mainContentLoading']),
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
                const names = [this.$route.meta.title || this.$t('问题管理')]
                if (toolDisplayName) {
                    toolDisplayName = this.$t(`${toolDisplayName}`)
                    names.unshift(toolDisplayName)
                }

                return { name: names.join(' / ') }
            },
            defectList () {
                return this.listData.defectList.records
            },
            currentFile () {
                return this.lintDetail.lintDefectList && this.lintDetail.lintDefectList[0]
            },
            statusTypeMap () {
                const { existCount, fixCount, ignoreCount } = this.searchFormData
                return {
                    1: `${this.$t('待修复')}（${existCount || 0}）`,
                    2: `${this.$t('已修复')}（${fixCount || 0}）`,
                    4: `${this.$t('已忽略')}（${ignoreCount || 0}）`
                }
            },
            nextPageStartNum () {
                return this.searchParams.pageNum * this.searchParams.pageSize + 1
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
            },
            cacheConfig () {
                const cacheKey = this.searchParams.clusterType === 'file' ? 'entityId' : 'defectId'
                return { cacheKey }
            },
            statusRelevantParams () {
                const { checker, author, fileList, buildId, daterange } = this.searchParams
                return { checker, author, fileList, buildId, daterange }
            },
            severityRelevantParams () {
                const { status } = this.searchParams
                return { ...this.statusRelevantParams, status }
            }
        },
        watch: {
            // 监听查询参数变化，则获取列表
            searchParamsWatch: {
                handler (newVal, oldVal) {
                    // 比如在第二页，筛选条件发生变化，要回到第一页
                    if (newVal.pageNum !== 1 && newVal.pageNum === oldVal.pageNum) {
                        this.searchParams.pageNum = 1
                        this.$refs.table.$refs.fileListTable.$refs.bodyWrapper.scrollTo(0, 0)
                        return
                    }
                    // 切换文件后，如果状态还没修改为只选择待修复，不请求后台
                    if (newVal.clusterType === 'file' && !(newVal.status.length === 1 && newVal.status[0] === 1)) {
                        return
                    }
                    if (this.isSearch) {
                        this.tableLoading = true
                        // this.fetchSearchList()
                        this.fetchLintList().then(list => {
                            if (this.pageChange) {
                                // 将一页的数据追加到列表
                                this.listData.defectList.records = this.listData.defectList.records.concat(list.defectList.records)

                                // 隐藏加载条
                                this.isFileListLoadMore = false

                                // 重置页码变更标记
                                this.pageChange = false
                            } else {
                                this.listData = { ...this.listData, ...list }
                                this.totalCount = this.pagination.count = this.listData.defectList.count
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
                    this.clearAllInterval()
                    const cacheId = val[this.cacheConfig.cacheKey]
                    this.defectDetailDialogVisiable = true
                    this.emptyText = this.$t('未选择文件')
                    this.detailLoading = true
                    if (this.defectCache[cacheId]) {
                        this.lintDetail = this.defectCache[cacheId]
                        this.handleCodeFullScreen()
                    }
                    this.fetchLintDetail()
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
                    if (newVal.enableToolList.find(item => item.toolName !== 'CCN' && item.toolName !== 'DUPC')) {
                        this.$nextTick(() => {
                            this.getQueryPreLineNum()
                        })
                    }
                },
                deep: true
            },
            'searchParams.clusterType' (newVal) {
                this.isSelectAll = 'N'
                this.isBatchOperationShow = false
                if (newVal === 'file' && !(this.searchParams.status.length === 1 && this.searchParams.status[0] === 1)) {
                    this.searchParams.status = [1]
                }
                if (newVal === 'file') {
                    this.searchParams.sortField = 'fileName'
                }
                this.fileIndex = 0
                this.selectedLen = 0
                this.selectedDefectCount = 0
            },
            defectList (val, oldVal) {
                this.preloadCache(val, this.cacheConfig)
                this.setTableHeight()
                if (val.length < this.fileIndex) {
                    this.fileIndex = 0
                }
            },
            'searchParams.status' (val, oldVal) {
                this.fetchOtherParams()
            },
            statusRelevantParams (val, oldVal) {
                this.fetchStatusParams()
            },
            severityRelevantParams (val, oldVal) {
                this.fetchSeverityParams()
                this.fetchDefectTypeParams()
            }
        },
        created () {
            if (!this.taskDetail.nameEn || this.taskDetail.enableToolList.find(item => item.toolName !== 'CCN' && item.toolName !== 'DUPC')) {
                this.init(true)
                this.fetchBuildList()
                this.fetchOtherParams()
            }
        },
        mounted () {
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
                    const list = res && res.defectList && res.defectList.records
                    this.generateExcel(list)
                }).finally(() => {
                    this.exportLoading = false
                })
            },
            generateExcel (list = []) {
                const tHeader = [this.$t('序号'), this.$t('entityId'), this.$t('位置'), this.$t('路径'), this.$t('规则'), this.$t('规则描述'), this.$t('处理人'), this.$t('级别'), this.$t('提交日期'), this.$t('首次发现'), this.$t('最新状态')]
                const filterVal = ['index', 'entityId', 'fileName', 'filePath', 'checker', 'message', 'author', 'severity', 'lineUpdateTime', 'createBuildNumber', 'status']
                const data = this.formatJson(filterVal, list)
                const title = `${this.taskDetail.nameCn}-${this.taskDetail.taskId}-${this.toolId}-${this.$t('问题')}-${new Date().toISOString()}`
                export_json_to_excel(tHeader, data, title)
            },
            // 处理状态
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
            // 处理表格数据
            formatJson (filterVal, list) {
                let index = 1
                return list.map(item => filterVal.map(j => {
                    if (j === 'index') {
                        return index++
                    } else if (j === 'fileName') {
                        return `${item.fileName}:${item.lineNum}`
                    } else if (j === 'severity') {
                        return this.defectSeverityMap[item.severity]
                    } else if (j === 'lineUpdateTime') {
                        return this.formatTime(item.lineUpdateTime, 'YYYY-MM-DD HH:mm:ss')
                    } else if (j === 'createBuildNumber') {
                        return `#${item.createBuildNumber}`
                    } else if (j === 'status') {
                        return this.handleStatus(item.status)
                    } else {
                        return item[j]
                    }
                }))
            },
            async init (isInit) {
                isInit ? this.contentLoading = true : this.fileLoading = true
                const list = await this.fetchLintList()
                this.handleGatherFile()
                this.listData = { ...this.listData, ...list }
                this.totalCount = this.pagination.count = this.listData.defectList.count
                this.isSearch = true
                this.addTableScrollEvent()
                if (isInit) {
                    this.contentLoading = false
                    this.isFetched = true
                } else {
                    this.tableLoading = false
                }

                // 判断是否为切换到v2环境
                if (this.taskDetail.nameEn.indexOf('LD_') === 0 || this.taskDetail.nameEn.indexOf('DEVOPS_') === 0) {
                    this.dialogAnalyseVisible = !this.neverShow
                }
            },
            initParams () {
                this.fetchSeverityParams()
                this.fetchDefectTypeParams()
                this.fetchStatusParams()
                this.fetchOtherParams()
            },
            async fetchBuildList () {
                this.selectLoading.buildListLoading = true
                this.buildList = await this.$store.dispatch('defect/getBuildList', { taskId: this.$route.params.taskId })
                this.selectLoading.buildListLoading = false
            },
            getSearchParams () {
                const daterange = this.searchParams.daterange
                const startCreateTime = this.formatTime(daterange[0], 'YYYY-MM-DD')
                const endCreateTime = this.formatTime(daterange[1], 'YYYY-MM-DD')
                const isSelectAll = this.isSelectAll
                const params = { ...this.searchParams, startCreateTime, endCreateTime, isSelectAll }
                return params
            },
            async fetchLintList () {
                const params = this.getSearchParams()
                const res = await this.$store.dispatch('defect/lintList', params)
                if (!res) return []
                if (res.fileList) {
                    res.defectList = res.fileList
                    delete res.fileList
                }
                return res
            },
            // async fetchSearchList () {
            //     const params = this.getSearchParams()
            //     const res = await this.$store.dispatch('defect/lintSearchParams', params)
            //     this.newDefectJudgeTime = res.newDefectJudgeTime ? this.formatTime(res.newDefectJudgeTime, 'YYYY-MM-DD') : ''
            //     this.searchFormData = Object.assign(this.searchFormData, res)
            //     this.getDefectCount(res)
            // },
            async fetchSeverityParams () {
                const params = this.getSearchParams()
                params.statisticType = 'SEVERITY'
                const res = await this.$store.dispatch('defect/lintSearchParams', params)
                const { newDefectJudgeTime, seriousCount, normalCount, promptCount } = res
                this.newDefectJudgeTime = newDefectJudgeTime ? this.formatTime(newDefectJudgeTime, 'YYYY-MM-DD') : ''
                this.searchFormData = Object.assign(this.searchFormData, { seriousCount, normalCount, promptCount })
                this.getDefectCount(res)
            },
            async fetchDefectTypeParams () {
                const params = this.getSearchParams()
                params.statisticType = 'DEFECT_TYPE'
                const res = await this.$store.dispatch('defect/lintSearchParams', params)
                const { newCount, historyCount } = res
                this.searchFormData = Object.assign(this.searchFormData, { newCount, historyCount })
            },
            async fetchStatusParams () {
                this.selectLoading.statusLoading = true
                const params = this.getSearchParams()
                params.statisticType = 'STATUS'
                const res = await this.$store.dispatch('defect/lintSearchParams', params)
                const { existCount, fixCount, ignoreCount } = res
                this.searchFormData = Object.assign(this.searchFormData, { existCount, fixCount, ignoreCount })
                this.selectLoading.statusLoading = false
            },
            async fetchOtherParams () {
                this.selectLoading.otherParamsLoading = true
                const status = this.searchParams.status
                const params = { toolId: this.toolId, status }
                const res = await this.$store.dispatch('defect/lintOtherParams', params)
                const { authorList, checkerList, filePathTree } = res
                this.searchFormData = Object.assign(this.searchFormData, { authorList, checkerList, filePathTree })
                this.selectLoading.otherParamsLoading = false
            },
            async handleGatherFile () {
                const taskId = this.$route.params.taskId
                const toolName = this.toolId
                this.gatherFile = await this.$store.dispatch('defect/gatherFile', { taskId, toolName }) || {}
            },
            fetchLintDetail (type, extraParams = {}) {
                const pattern = this.toolMap[this.toolId]['pattern']
                const params = { ...this.searchParams, ...this.defectDetailSearchParams, pattern, ...extraParams }
                params.fileList = [params.filePath]
                this.$store.dispatch('defect/lintDetail', params).then(detail => {
                    if (detail.fileName) {
                        if (!extraParams.entityId) {
                            this.lintDetail = detail

                            // 查询详情后，全屏显示问题
                            this.handleCodeFullScreen(type)
                            const cacheConfig = { ...this.cacheConfig, index: this.fileIndex }
                            this.preloadCache(this.defectList, cacheConfig)
                        }
                        const updateCacheKey = params[this.cacheConfig.cacheKey]
                        this.updateCache(updateCacheKey, detail)
                    } else if (detail.response) {
                        this.cacheConfig.length = 0
                        this.preloadCache(this.defectList, this.cacheConfig)
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
                    }
                }).finally(() => {
                    this.detailLoading = false
                })
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
                                vm.screenScroll()
                            }
                            break
                    }
                }
            },
            addTableScrollEvent () {
                this.$nextTick(() => {
                    // 滚动加载
                    if (this.$refs.table && this.$refs.table.$refs.fileListTable) {
                        const tableBodyWrapper = this.$refs.table.$refs.fileListTable.$refs.bodyWrapper

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
                const count = this.searchFormData[severityFieldMap[severity]] || 0
                return count > 100000 ? this.$t('10万+') : count
            },
            getDefectCountByType (type) {
                const tpyeFieldMap = {
                    1: 'newCount',
                    2: 'historyCount'
                }
                const count = this.searchFormData[tpyeFieldMap[type]] || 0
                return count > 100000 ? this.$t('10万+') : count
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
                this.selectedDefectCount = 0
                selection.forEach(val => {
                    if (this.selectedLen === this.defectList.length) {
                        this.selectedDefectCount = this.totalDefectCount
                    } else this.selectedDefectCount += val.defectCount
                })
                this.isBatchOperationShow = Boolean(selection.length)
                // 如果长度是最长，那么就是Y，否则是N
                this.isSelectAll = this.selectedLen === this.defectList.length ? 'Y' : 'N'
            },
            handleFileListRowClick (row, event, column) {
                if (row.defectId) {
                    this.fileIndex = this.defectList.findIndex(file => file.defectId === row.defectId && file.entityId === row.entityId)
                } else {
                    this.fileIndex = this.defectList.findIndex(file => file.entityId === row.entityId)
                }
                // 要把参数强制先置空，不然不能触发请求
                this.defectDetailSearchParams.entityId = ''
                this.defectDetailSearchParams.entityId = row.entityId
                this.defectDetailSearchParams.filePath = row.filePath || row.fileName
                this.defectDetailSearchParams.defectId = row.defectId
            },
            triggerRowClick (position) {
                if (position === 'prev') {
                    this.handleFileListRowClick(this.defectList[--this.fileIndex])
                } else {
                    this.handleFileListRowClick(this.defectList[++this.fileIndex])
                }
                this.screenScroll()
            },
            keyEnter () {
                const row = this.defectList[this.fileIndex]
                this.defectDetailSearchParams.entityId = ''
                this.defectDetailSearchParams.entityId = row.entityId
                this.defectDetailSearchParams.defectId = row.defectId
                this.defectDetailSearchParams.filePath = row.filePath
            },
            handleCodeFullScreen (type) {
                this.$nextTick(() => {
                    this.$refs['detail'].handleCodeFullScreen(type)
                    if (!type) this.$refs['detail'].locateFirst()
                })
            },
            codeViewerClick (event, eventSource) {
                const lintHints = getClosest(event.target, '.lint-hints')

                // 如果点击的是lint问题区域，展开修复建议
                if (lintHints) {
                    toggleClass(lintHints, 'active')
                }
            },

            // 文件路径相关交互
            handleFilePathSearch (val) {
                this.$refs.filePathTree.filter(val)
            },
            handleFilePathCancelClick () {
                this.$refs.filePathDropdown.hide()
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
            handleSelectable (row, index) {
                return !(row.status & 2)
            },
            handleMark (markFlag, batchFlag, entityId) {
                // markFlag 0: 取消标记, 1: 标记修改
                // batchFlag true: 批量操作
                let defectKeySet = []
                if (batchFlag) {
                    this.$refs.table.$refs.fileListTable.selection.map(item => {
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
                            message: markFlag
                                ? this.$t('标记为已处理成功。若下次检查仍为问题将突出显示。') : this.$t('取消标记成功')
                        })
                        if (batchFlag) {
                            this.init()
                        } else {
                            this.listData.defectList.records.forEach(item => {
                                if (item.entityId === entityId) {
                                    item.mark = markFlag
                                }
                            })
                            this.listData.defectList.records = this.listData.defectList.records.slice()
                        }
                        if (this.defectDetailDialogVisiable) {
                            this.fetchLintDetail('scroll')
                        }
                    }
                }).catch(e => {
                    console.error(e)
                }).finally(() => {
                    this.tableLoading = false
                })
            },
            handleComent (entityId, commentId) {
                this.commentParams = { ...this.commentParams, defectId: entityId, comment: '' }
                if (commentId) {
                    this.commentParams.commentId = commentId
                } else if (this.searchParams.clusterType === 'defect') {
                    this.commentParams.commentId = this.lintDetail.lintDefectList[0].codeComment ? this.lintDetail.lintDefectList[0].codeComment.entityId : ''
                } else {
                    this.commentParams.commentId = ''
                }
                this.commentDialogVisiable = true
            },
            handleCommentConfirm () {
                this.commentDialogVisiable = false
                // 暂不做修改评论
                // const url = this.commentParams.singleCommentId ? 'defect/updateComment' : 'defect/commentDefect'

                const url = 'defect/commentDefect'
                this.detailLoading = true
                this.$store.dispatch(url, this.commentParams).then(res => {
                    if (res.code === '0') {
                        this.$bkMessage({
                            theme: 'success',
                            message: this.$t('评论问题成功')
                        })
                        this.fetchLintDetail('scroll')
                    }
                }).finally(() => {
                    this.detailLoading = false
                })
            },
            deleteComment (commentId, singleCommentId) {
                const params = {
                    commentId: commentId,
                    singleCommentId: singleCommentId,
                    toolName: this.toolId
                }
                this.$store.dispatch('defect/deleteComment', params).then(res => {
                    if (res.code === '0') {
                        this.$bkMessage({
                            theme: 'success',
                            message: this.$t('删除成功')
                        })
                        this.fetchLintDetail('scroll')
                    }
                })
            },
            handleAuthor (changeAuthorType, entityId, author) {
                this.authorEditDialogVisiable = true
                this.operateParams.changeAuthorType = changeAuthorType
                this.operateParams.sourceAuthor = author
                this.operateParams.defectKeySet = [entityId]
            },
            // 处理人修改
            handleAuthorEditConfirm () {
                let data = this.operateParams
                if (data.changeAuthorType === 2) {
                    const defectKeySet = []
                    this.$refs.table.$refs.fileListTable.selection.map(item => {
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
                            message: this.$t('修改处理人成功')
                        })
                        this.operateParams.targetAuthor = []
                        if (data.changeAuthorType === 1) {
                            this.listData.defectList.records.forEach(item => {
                                if (item.entityId === data.defectKeySet[0]) {
                                    item.author = data.newAuthor.join()
                                }
                            })
                            this.listData.defectList.records = this.listData.defectList.records.slice()
                        } else {
                            this.init()
                        }
                        this.initParams()
                        if (this.defectDetailDialogVisiable) {
                            this.fetchLintDetail('scroll')
                        }
                    }
                }).catch(e => {
                    console.error(e)
                }).finally(() => {
                    this.tableLoading = false
                })
            },
            handleIgnore (ignoreType, batchFlag, entityId, filePath) {
                this.operateParams.fileList = [filePath]
                this.operateParams.bizType = ignoreType
                this.operateParams.batchFlag = batchFlag
                if (batchFlag) {
                    const defectKeySet = []
                    const fileList = []
                    this.$refs.table.$refs.fileListTable.selection.map(item => {
                        defectKeySet.push(item.entityId)
                        fileList.push(item.filePath)
                    })
                    this.operateParams.defectKeySet = defectKeySet
                    this.operateParams.fileList = fileList
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
                } else if (this.searchParams.clusterType === 'file' && !this.operateParams.defectKeySet[0]) {
                    const searchParams = JSON.parse(JSON.stringify(this.searchParams))
                    searchParams.clusterType = 'defect'
                    searchParams.pattern = 'LINT'
                    searchParams.fileList = this.operateParams.fileList
                    data = { ...data, isSelectAll: 'Y', queryDefectCondition: JSON.stringify(searchParams) }
                }
                this.$store.dispatch('defect/batchEdit', data).then(res => {
                    if (res.code === '0') {
                        this.$bkMessage({
                            theme: 'success',
                            message: this.operateParams.bizType === 'IgnoreDefect'
                                ? this.$t('忽略问题成功。该问题将不会在待修复列表中显示。') : this.$t('恢复问题成功。该问题将重新在待修复列表中显示。')
                        })
                        if (data.batchFlag) {
                            this.init()
                        } else {
                            const index = this.listData.defectList.records.findIndex(item => item.entityId === data.defectKeySet[0])
                            this.listData.defectList.records.splice(index, 1)
                            this.totalCount -= 1
                        }
                        this.initParams()

                        this.operateParams.ignoreReason = ''
                        if (this.defectDetailDialogVisiable) {
                            if (this.searchParams.clusterType === 'file' && this.lintDetail.lintDefectList.length > 1) {
                                this.fetchLintDetail('scroll')
                            } else {
                                this.defectDetailDialogVisiable = false
                            }
                        }
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
                    if (this.$refs.table && this.$refs.table.$refs.fileListTable && this.$refs.table.$refs.fileListTable.$refs.bodyWrapper) {
                        const childrens = this.$refs.table.$refs.fileListTable.$refs.bodyWrapper
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
            // 根据ID打开详情
            openDetail () {
                const entityId = this.$route.query.entityId
                const filePath = this.$route.query.filePath
                if (entityId || filePath) {
                    setTimeout(() => {
                        if (!this.toolMap[this.toolId]) {
                            this.openDetail()
                        } else if (filePath) {
                            this.searchParams.clusterType = 'file'
                            this.defectDetailSearchParams.clusterType = 'file'
                            this.defectDetailSearchParams.filePath = filePath
                        } else {
                            this.defectDetailSearchParams.entityId = entityId
                        }
                    }, 500)
                }
            },
            setTableHeight () {
                setTimeout(() => {
                    let smallHeight = 0
                    let largeHeight = 0
                    let tableHeight = 0
                    const i = this.listData.defectList.records.length || 0
                    if (this.$refs.table && this.$refs.table.$refs.fileListTable) {
                        const $main = document.getElementsByClassName('main-form')
                        smallHeight = $main.length > 0 ? $main[0].clientHeight : 0
                        largeHeight = this.$refs.mainContainer ? this.$refs.mainContainer.clientHeight : 0
                        tableHeight = this.$refs.table.$refs.fileListTable.$el.scrollHeight
                        this.screenHeight = i * 42 > tableHeight ? largeHeight - smallHeight - 73 : i * 42 + 43
                        this.screenHeight = this.screenHeight === 43 ? 336 : this.screenHeight
                    }
                }, 0)
            },
            getDefectCount (list) {
                this.totalDefectCount = 0
                this.searchParams.severity.forEach(item => {
                    this.totalDefectCount += list[this.defectCountMap[item]]
                })
            },
            goToLog () {
                this.$router.push({ name: 'task-detail-logs' })
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
    .bk-form-radio {
        margin-right: 15px;
        >>>.bk-radio-text {
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
    
    .filepath-dropdown-content {
        color: #737987;

        .content-hd {
            margin: 0 16px 16px;
        }
        .content-bd {
            width: 480px;
            height: 340px;
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
            min-width: 1010px;
        }
    }
</style>

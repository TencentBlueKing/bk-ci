<template>
    <div
        class="node-detail-wrapper"
        v-bkloading="{
            isLoading: loading.isLoading,
            title: loading.title
        }"
    >
        <content-header class="info-header">
            <div slot="left">
                <i
                    class="devops-icon icon-arrows-left"
                    @click="toNodeList"
                ></i>
                <span class="header-text">{{ $t('environment.batchSetTag') }}</span>
            </div>
            <div
                slot="right"
                class="node-handle"
            >
                <bk-popconfirm
                    trigger="click"
                    width="422"
                    placement="bottom"
                    @confirm="confirmAddColumn"
                    @cancel="resetTagSelection"
                >
                    <div slot="content">
                        <div class="add-column-content">
                            <p class="add-title">添加/展示标签</p>
                            <div class="existing-columns">
                                <p class="tag-title">
                                    <span>已设置标签展示</span>
                                    <bk-checkbox
                                        v-model="checkAllExisting"
                                        @change="handleCheckAllExisting"
                                    >
                                        全选
                                    </bk-checkbox>
                                </p>
                                <div class="existing-column-item">
                                    <p
                                        v-for="(col, index) in tableColumns"
                                        :key="col.key"
                                        v-if="col && col.key && index !== 0"
                                    >
                                        <bk-checkbox
                                            v-model="selectedExistingTags[col.key]"
                                            @change="handleTagChange"
                                        >
                                            <span :class="{ 'disabled-cell': col.removedRecently }">
                                                {{ col.label }}
                                            </span>
                                        </bk-checkbox>
                                        <span
                                            v-if="col.removedRecently"
                                            class="tag-flag removed"
                                        >本次移除</span>
                                        <span
                                            v-else-if="col.addedRecently"
                                            class="tag-flag added"
                                        >本次添加</span>
                                    </p>
                                </div>
                            </div>

                            <div class="new-column-input">
                                <p class="tag-title">添加标签</p>
                                <div class="existing-column-item">
                                    <p
                                        v-for="(tag) in otherTags"
                                        :key="tag.key"
                                        v-if="tag && tag.key"
                                    >
                                        <bk-checkbox
                                            v-model="selectedNewTags[tag.key]"
                                            class="new-tag-checkbox"
                                            @change="handleNewTagChange"
                                        >
                                            {{ tag.label }}
                                        </bk-checkbox>
                                    </p>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div>
                        <i class="devops-icon icon-cog"></i>
                        <span class="copy-btn">{{ $t('environment.addOrShowTag') }}</span>
                    </div>
                </bk-popconfirm>
            </div>
        </content-header>
        <div
            class="sub-view-port"
            :style="{ width: `${mainWidth}px` }"
            v-show="!showContent"
        >
            <div
                class="table-content"
                ref="scrollContainer"
            >
                <table class="custom-table">
                    <thead>
                        <tr>
                            <th
                                v-for="(col, colIndex) in tableColumns"
                                :key="col.key"
                                :class="[
                                    { 'new-column': col.isNew },
                                    { 'disabled-column': col.disabled }
                                ]"
                                v-if="!col.hidden"
                            >
                                <div class="header-content">
                                    <span :class="{ 'strikethrough': col.disabled }">{{ col.label }}</span>

                                    <div
                                        class="header-actions"
                                        v-if="colIndex !== 0"
                                    >
                                        <template v-if="!col.disabled">
                                            <bk-popover
                                                :ref="`editPopover_${colIndex}`"
                                                trigger="click"
                                                width="240"
                                                theme="light"
                                                ext-cls="edit-popover"
                                                placement="bottom"
                                            >
                                                <div slot="content">
                                                    <div class="edit-content">
                                                        <p class="content-tit">批量设置 {{ col.label }}</p>
                                                        <bk-input
                                                            :clearable="true"
                                                            v-model="editColumnValue"
                                                        ></bk-input>
                                                    </div>
                                                    <div class="edit-btn">
                                                        <span @click="confirmEditColData(colIndex)">确定</span>
                                                        <span @click="cancleEditColData(colIndex)">取消</span>
                                                    </div>
                                                </div>
                                                <i
                                                    v-bk-tooltips="{ content: '批量编辑' }"
                                                    class="devops-icon icon-edit header-icon"
                                                ></i>
                                            </bk-popover>
                                            <i
                                                v-bk-tooltips="{ content: '批量移除' }"
                                                class="devops-icon icon-delete header-icon"
                                                @click.stop="handleDeleteCol(colIndex)"
                                            ></i>
                                        </template>
                                        <img
                                            v-else
                                            v-bk-tooltips="{ content: '批量恢复' }"
                                            src="../../scss/svg/revocation.svg"
                                            class="restore-btn"
                                            @click.stop="handleRestoreCol(colIndex)"
                                        >
                                    </div>
                                </div>
                            </th>
                        </tr>
                    </thead>

                    <tbody>
                        <tr
                            v-for="(row, rowIndex) in tableData"
                            :key="rowIndex"
                        >
                            <td
                                v-for="(col, colIndex) in tableColumns"
                                :key="col.key"
                                :class="[
                                    { 'content-td': colIndex !== 0 },
                                    getCellStatusClass(rowIndex, col)
                                ]"
                                @mouseenter="showCellActionsFn(rowIndex, col.key, true)"
                                @mouseleave="showCellActionsFn(rowIndex, col.key, false)"
                                v-if="!col.hidden"
                            >
                                <template v-if="colIndex === 0">
                                    {{ row[col.key] }}
                                </template>
                                <template v-else>
                                    <input
                                        v-model="row[col.key]"
                                        type="text"
                                        :class="[
                                            'cell-input',
                                            getCellStatusClass(rowIndex, col)
                                        ]"
                                        placeholder="请输入"
                                        :disabled="isPendingRemove(rowIndex, col.key) || col.disabled"
                                        @change="handleCellChange(rowIndex, col.key, row[col.key])"
                                    >
                                    <div
                                        class="cell-actions"
                                        v-if="cellActionVisibility[rowIndex] && cellActionVisibility[rowIndex][col.key] && colIndex !== 0 && !col.disabled"
                                    >
                                        <i
                                            v-if="!(cellDisabled[rowIndex] && cellDisabled[rowIndex][col.key]) && row[col.key]"
                                            v-bk-tooltips="{ content: '移除' }"
                                            class="devops-icon icon-delete cell-action-btn"
                                            @click.stop="handleCellDelete(rowIndex, col.key)"
                                        ></i>
                                        <img
                                            v-if="cellDisabled[rowIndex] && cellDisabled[rowIndex][col.key]"
                                            v-bk-tooltips="{ content: '恢复' }"
                                            class="cell-action-btn restore"
                                            src="../../scss/svg/revocation.svg"
                                            @click.stop="handleCellRestore(rowIndex, col.key)"
                                        >
                                    </div>
                                </template>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
            <div :class="['content-btn', { 'has-scroll': hasVerticalScrollbar }]">
                <bk-button
                    theme="primary"
                    @click="handlePreviewAndSave"
                >
                    预览并保存
                </bk-button>
                <bk-button @click="handleCancel">取消</bk-button>
            </div>
        </div>
    </div>
</template>

<script>
    import { mapState } from 'vuex'

    export default {
        props: {
            mainWidth: Number
        },
        data () {
            return {
                showContent: false,
                loading: {
                    isLoading: false,
                    title: this.$t('environment.loadingTitle')
                },
                tableData: [
                    { tagname: '节点1', id: 1, name: '张三', age: 20, address: '北京' },
                    { tagname: '节点2', id: 1, name: '张三', age: 20, address: '北京' },
                    { tagname: '节点3', id: 2, name: '李四', age: 25, address: '上海' },
                    { tagname: '节点4', id: 3, name: '', age: 30, address: '广州' },
                ],
                otherTags: [
                    { key: 'name1', label: 'wwwww', disabled: false },
                    { key: 'age2', label: 'aaaa', disabled: false },
                    { key: 'address3', label: 'bbb', disabled: false }
                ],
                tableColumns: [
                    { key: 'tagname', label: '已选节点', disabled: false, hidden: false },
                    { key: 'name', label: 'os', disabled: false, hidden: false },
                    { key: 'age', label: 'architecture', disabled: false, hidden: false },
                    { key: 'address', label: 'service', disabled: false, hidden: false },
                ],
                // 初始化状态相关
                initialTableColumns: [], // 初始表格列配置（用于对比）
                initialOtherTags: [], // 初始可用标签（用于对比）
                cellDisabled: {},   // 单元格禁用状态 - 用于标记待移除的数据
                cellActionVisibility: {},   // 单元格操作显示状态
                cellDataStatus: {}, // 单元格数据状态: 新增(new)、修改(modified)、原始(无状态)
                confirmedRemoved: {}, // 记录已确认移除的单元格
                originalData: {}, // 原始数据备份
                // 标签选择相关
                selectedExistingTags: {},
                selectedNewTags: {},
                checkAllExisting: false,
                editColumnValue: '',
                hasVerticalScrollbar: false
            }
        },
        computed: {
            ...mapState('environment', ['nodeTagList', 'selectionTagList']),
            projectId () {
                return this.$route.params.projectId
            },
            // 检查是否有未确认的移除操作
            hasPendingRemovals () {
                return this.hasColumnPendingRemovals() || this.hasCellPendingRemovals()
            }
        },
        watch: {
            projectId () {
                this.$router.push({ name: 'envList' })
            },
            tableColumns: {
                deep: true,
                handler () {
                    this.syncSelectedTags()
                }
            }
        },
        mounted () {
            this.initCellStates()
            this.saveOriginalData()
            this.initialTableColumns = JSON.parse(JSON.stringify(this.tableColumns))
            this.initialOtherTags = JSON.parse(JSON.stringify(this.otherTags))
            this.syncSelectedTags()
            this.$nextTick(() => this.checkScrollbar())
        },
        methods: {
            toNodeList () {
                this.$router.push({ name: 'nodeList' })
            },

            checkScrollbar () {
                const container = this.$refs.scrollContainer
                this.hasVerticalScrollbar = container?.scrollHeight > container?.clientHeight || false
            },
            // 同步已选中的标签
            syncSelectedTags () {
                // 清空现有状态
                Object.keys(this.selectedExistingTags).forEach(key => {
                    this.$delete(this.selectedExistingTags, key)
                })
            
                // 为每个列设置初始选中状态（可见列默认选中），跳过第一列
                this.tableColumns.forEach((col, index) => {
                    if (index !== 0 && col?.key) {
                        this.$set(this.selectedExistingTags, col.key, !col.disabled)
                    }
                })
            
                this.checkAllStatus()
            },
        
            // 检查全选状态（排除第一列）
            checkAllStatus () {
                const selectableColumns = this.tableColumns.filter((col, index) => index !== 0 && col?.key)
                this.checkAllExisting = selectableColumns.length
                    ? selectableColumns.every(col => this.selectedExistingTags[col.key])
                    : false
            },

            handleTagChange () {
                this.checkAllStatus()
                this.updateChangeFlags()

                // 同步列的禁用状态与选择状态，跳过第一列
                this.tableColumns.forEach((col, index) => {
                    if (index !== 0 && col?.key) {
                        this.$set(col, 'disabled', !this.selectedExistingTags[col.key])
                    }
                })
            },
            // 处理新增标签状态变化
            handleNewTagChange () {
                this.updateChangeFlags()
            },

            // 更新变更标记（本次添加/本次移除）
            updateChangeFlags () {
                // 处理列的移除/添加标记
                this.tableColumns.forEach((col, index) => {
                    if (index === 0 || !col?.key) return
                    const initialCol = this.initialTableColumns.find(item => item?.key === col.key)
                    this.$set(col, 'removedRecently', !!initialCol && !this.selectedExistingTags[col.key])
                })
                // 处理新增标签的添加标记
                Object.keys(this.selectedNewTags).forEach(tagKey => {
                    if (this.selectedNewTags[tagKey]) {
                        const existingCol = this.tableColumns.find(col => col?.key === tagKey)
                        if (existingCol && !this.initialTableColumns.some(col => col?.key === tagKey)) {
                            this.$set(existingCol, 'addedRecently', true)
                        }
                    }
                })
            },

            
            // 批量初始化单元格状态，减少重复$set调用
            initCellStates () {
                this.tableData.forEach((row, rowIndex) => {
                    const keys = Object.keys(row)
                    this.$set(this.cellDisabled, rowIndex, Object.fromEntries(keys.map(key => [key, false])))
                    this.$set(this.cellActionVisibility, rowIndex, Object.fromEntries(keys.map(key => [key, false])))
                    this.$set(this.confirmedRemoved, rowIndex, Object.fromEntries(keys.map(key => [key, false])))
                    this.$set(this.cellDataStatus, rowIndex, Object.fromEntries(keys.map(key => [key, ''])))
                })
            },
            saveOriginalData () {
                this.tableData.forEach((row, rowIndex) => {
                    this.$set(this.originalData, rowIndex, { ...row })
                })
            },
            // 全选/取消全选已设置标签（排除第一列）
            handleCheckAllExisting (checked) {
                this.tableColumns.forEach((col, index) => {
                    if (index !== 0 && col?.key) {
                        this.$set(this.selectedExistingTags, col.key, checked)
                        this.$set(col, 'disabled', !checked)
                    }
                })
                this.updateChangeFlags()
            },

            // 检查是否为未确认移除状态
            isPendingRemove (rowIndex, colKey) {
                return this.cellDisabled[rowIndex]?.[colKey] && !this.confirmedRemoved[rowIndex]?.[colKey]
            },
            // 检查是否为已确认移除状态
            isConfirmedRemove (rowIndex, colKey) {
                return this.confirmedRemoved[rowIndex]?.[colKey] && (this.tableData[rowIndex][colKey] || '') === ''
            },
            // 检查列级未确认移除
            hasColumnPendingRemovals () {
                return this.tableColumns.some((col, index) => index !== 0 && col?.key && col.disabled)
            },
            // 检查单元格级未确认移除
            hasCellPendingRemovals () {
                return Object.values(this.cellDisabled).some(row =>
                    Object.values(row).some(isDisabled => isDisabled)
                )
            },

            getCellStatusClass (rowIndex, col) {
                const { key: colKey, isNew, disabled } = col

                return {
                    'pending-remove': this.isPendingRemove(rowIndex, colKey) || disabled,
                    'confirmed-remove': this.isConfirmedRemove(rowIndex, colKey),
                    'cell-new': this.cellDataStatus[rowIndex]?.[colKey] === 'new' || isNew,
                    'cell-modified': this.cellDataStatus[rowIndex]?.[colKey] === 'modified',
                }
            },

            // 处理单元格内容变化
            handleCellChange (rowIndex, colKey, newValue) {
                const originalValue = this.originalData[rowIndex]?.[colKey]
                if ((originalValue || '') === '' && (newValue || '') !== '') {
                    this.$set(this.cellDataStatus[rowIndex], colKey, 'new')
                } else if ((originalValue || '') !== (newValue || '')) {
                    this.$set(this.cellDataStatus[rowIndex], colKey, 'modified')
                } else {
                    this.$set(this.cellDataStatus[rowIndex], colKey, '')
                }
            },
            // 确认编辑列数据
            confirmEditColData (colIndex) {
                const col = this.tableColumns[colIndex]
                if (!col?.key) return
                // 设置整列的值并更新状态
                this.tableData.forEach((row, rowIndex) => {
                    row[col.key] = this.editColumnValue
                    this.handleCellChange(rowIndex, col.key, this.editColumnValue)
                })
                this.editColumnValue = ''
                this.$refs[`editPopover_${colIndex}`]?.[0]?.hideHandler()
            },
            cancleEditColData (colIndex) {
                this.editColumnValue = ''
                this.$refs[`editPopover_${colIndex}`]?.[0]?.hideHandler()
            },
            // 处理列删除 - 整列置灰并同步选择状态
            handleDeleteCol (colIndex) {
                if (colIndex === 0) return
                const col = this.tableColumns[colIndex]
                if (col?.key) {
                    // 设置禁用状态并更新选择状态
                    this.$set(col, 'disabled', true)
                    this.$set(this.selectedExistingTags, col.key, false)
                    this.checkAllStatus()
                    this.updateChangeFlags()
                }
            },
            // 处理列恢复
            handleRestoreCol (colIndex) {
                if (colIndex === 0) return
                const col = this.tableColumns[colIndex]
                if (col?.key) {
                    this.$set(col, 'disabled', false)
                    this.$set(this.selectedExistingTags, col.key, true)
                    this.checkAllStatus()
                    this.updateChangeFlags()
                }
            },
            // 显示/隐藏单元格操作按钮
            showCellActionsFn (rowIndex, colKey, show) {
                if (!this.cellActionVisibility[rowIndex]) {
                    this.$set(this.cellActionVisibility, rowIndex, {})
                }
                this.$set(this.cellActionVisibility[rowIndex], colKey, show)
            },

            // 处理单元格删除（标记为待移除）
            handleCellDelete (rowIndex, colKey) {
                this.$set(this.cellDisabled[rowIndex], colKey, true)
                this.$set(this.confirmedRemoved[rowIndex], colKey, false)
            },
            // 处理单元格恢复
            handleCellRestore (rowIndex, colKey) {
                this.$set(this.cellDisabled[rowIndex], colKey, false)
                this.$set(this.confirmedRemoved[rowIndex], colKey, false)
                // 恢复原始值
                if (this.originalData[rowIndex]?.[colKey]) {
                    this.tableData[rowIndex][colKey] = this.originalData[rowIndex][colKey]
                }
            },

            // 确认添加列和标签 - 此时才真正执行添加/移除操作
            confirmAddColumn () {
                this.handleColumnRemovals() // 处理列移除
                this.handleCellRemovals() // 处理单元格移除
                this.handleNewTags() // 处理新增标签
                this.resetTagSelection()
                // 更新初始状态
                this.initialTableColumns = JSON.parse(JSON.stringify(this.tableColumns))
                this.initialOtherTags = JSON.parse(JSON.stringify(this.otherTags))
                this.syncSelectedTags()
            },
            // 处理列级移除
            handleColumnRemovals () {
                const tagsToRemove = this.tableColumns
                    .filter((col, index) => index !== 0 && col?.key && !this.selectedExistingTags[col.key])
                if (!tagsToRemove.length) return

                // 从表格列中移除
                this.tableColumns = this.tableColumns.filter(col =>
                    !tagsToRemove.some(t => t.key === col.key) || col.key === 'tagname' // 保留第一列
                )
                // 移除的标签放回otherTags
                tagsToRemove.forEach(tag => {
                    if (!this.otherTags.some(t => t.key === tag.key)) {
                        this.otherTags.push({ key: tag.key, label: tag.label, disabled: false })
                    }
                })
                // 从数据中删除列
                this.tableData.forEach((row, rowIndex) => {
                    tagsToRemove.forEach(tag => {
                        this.$delete(row, tag.key)
                        this.$delete(this.cellDisabled[rowIndex], tag.key)
                        this.$delete(this.originalData[rowIndex], tag.key)
                    })
                })
            },
            // 处理单元格级移除
            handleCellRemovals () {
                this.tableData.forEach((row, rowIndex) => {
                    Object.entries(this.cellDisabled[rowIndex] || {}).forEach(([colKey, isDisabled]) => {
                        if (isDisabled) {
                            row[colKey] = '' // 清空值
                            this.$set(this.confirmedRemoved[rowIndex], colKey, true) // 标记已确认
                            this.$set(this.cellDisabled[rowIndex], colKey, false) // 清除待移除标记
                            this.$set(this.cellDataStatus[rowIndex], colKey, '') // 清除状态
                        }
                    })
                })
            },
            // 处理新增标签
            handleNewTags () {
                const newTagKeys = Object.keys(this.selectedNewTags).filter(key => this.selectedNewTags[key])
                if (!newTagKeys.length) return

                const newTagsToAdd = this.otherTags.filter(tag => newTagKeys.includes(tag.key))
                newTagsToAdd.forEach(tag => {
                    if (this.tableColumns.some(col => col.key === tag.key)) return

                    // 添加到表格列（第一列后）
                    this.tableColumns.splice(1, 0, {
                        ...tag, hidden: false, disabled: false, isNew: true, addedRecently: true
                    })
                    // 为每行添加新列
                    this.tableData.forEach((row, rowIndex) => {
                        this.$set(row, tag.key, '')
                        this.$set(this.cellDisabled[rowIndex], tag.key, false)
                        this.$set(this.originalData[rowIndex], tag.key, '')
                    })
                    this.$set(this.selectedExistingTags, tag.key, true)
                })
                // 从otherTags移除已添加的
                this.otherTags = this.otherTags.filter(tag => !newTagKeys.includes(tag.key))
            },
            // 重置标签选择状态
            resetTagSelection () {
                // 重置新增标签选择
                Object.keys(this.selectedNewTags).forEach(key => {
                    this.$delete(this.selectedNewTags, key)
                })
                // 保留添加标记以便下次打开弹窗时显示, 只清除移除标记
                this.tableColumns.forEach(col => col && this.$set(col, 'removedRecently', false))
            },
            // 处理预览并保存
            handlePreviewAndSave () {
                if (this.hasPendingRemovals) {
                    this.$bkInfo({
                        theme: 'warning',
                        type: 'warning',
                        title: '是否确定移除数据',
                        confirmFn: () => {
                            this.confirmAddColumn() // 确认移除
                            console.log('最新表格数据:', JSON.parse(JSON.stringify(this.tableData)))
                        }
                    })
                } else {
                    console.log('最新表格数据:', JSON.parse(JSON.stringify(this.tableData)))
                }
            },

            handleCancel () {
                this.toNodeList()
            }
        }
    }
</script>

<style lang="scss" scoped>
@import '@/scss/conf';
.node-detail-wrapper {
    max-width: 100vw;
    height: 100%;
    overflow: hidden;

    .info-header {
        height: 54px;
        line-height: 54px;
        background-color: #FAFBFD;
        border-bottom: none;
        box-shadow: none;
        .icon-arrows-left {
            margin-right: 4px;
            cursor: pointer;
            color: $iconPrimaryColor;
            font-size: 14px;
            vertical-align: middle;
        }
        .header-text {
            font-size: 14px;
            color: #313238;
        }
        .icon-cog {
            vertical-align: middle;
        }
        .node-handle {
            color: #3A84FF;
            cursor: pointer;
            .copy-btn {
                margin-left: 0;
            }
        }
    }
    .sub-view-port {
        height: calc(100% - 54px);
        overflow: hidden;
        padding: 0 20px;
        .table-content {
            width: 100%;
            max-height: calc(100% - 48px);
            overflow: auto;
            border: 1px solid #DCDEE5;
            border-bottom: none;
            padding-bottom: 1px;
            &::-webkit-scrollbar {
                background-color: #fff;
                height: 9px;
                width: 9px;
                border-bottom: 1px solid #DCDEE5;
                border-top: 1px solid #DCDEE5;
                border-left: 1px solid #DCDEE5;
            }
        }
        .content-btn {
            height: 48px;
            line-height: 48px;
            margin: 0 -20px;
            padding: 0 20px;
            background: #FAFBFD;
            &.has-scroll {
                box-shadow: 0 -1px 0 0 #DCDEE5;

            }
        }
        .custom-table {
            width: 100%;
            border-collapse: collapse;
            th, td {
                padding: 10px 16px;
                text-align: left;
                border: 1px solid #DCDEE5;
                position: relative;
                background-color: #fff;
            }
            .content-td {
                padding: 0;
            }
            th {
                background-color: #F0F1F5;
                min-width: 280px;
            }
            th:first-child, td:first-child {
                width: 200px;
                min-width: 200px;
                max-width: 200px;
                border-left: none;
                tbody td:first-child {
                    background-color: #FAFBFD;
                }
            }
            th:last-child, td:last-child {
                border-right: none;
            }
            thead {
                position: sticky;
                top: -1px;
                background: white;
                z-index: 20;
                tr:first-child th {
                    border-top: none;
                }
            }
            .header-content {
                display: flex;
                justify-content: space-between;
                align-items: center;
                .header-actions {
                    display: flex;
                    .header-icon {
                        color: #3A84FF; font-size: 12px;
                    }
                    .icon-delete {
                        margin: 0 8px; font-size: 14px;
                    }
                    .restore-btn {
                        width: 14px; height: 14px;
                    }
                }
            }
            .cell-input {
                width: 100%;
                height: 100%;
                font-size: 12px;
                border: none;
                padding: 10px 16px;
                outline: none;
                &::placeholder {
                    color: #C4C6CC;
                    font-size: 12px;
                }
                &:focus {
                    outline: none;
                }
            }
            .content-td:not(.pending-remove):hover .cell-input {
                cursor: pointer;
                background-color: #fafbfd;
            }
            .content-td:not(.pending-remove):hover {
                z-index: 11;
                background-color: #fafbfd;
                outline: 1px solid #a3c5fd;
            }
            .content-td .cell-input:focus {
                outline: none;
            }
            .content-td:focus-within:hover {
                outline: 1px solid #3A84FF !important;
            }
            .content-td:focus-within {
                z-index: 11;
                outline: 1px solid #3A84FF;
            }
            // 新增
            .cell-new {
                background-color: #F2FFF4;
            }
            // 修改
            .cell-modified {
                background-color: #FFF3E1;
            }
            // 新增列
            .new-column {
                background-color: #D6F7DB;
            }
            // 列禁用
            .disabled-column {
                color: #C4C6CC;
                background-color: #F0F1F5;
            }
            // 待移除（灰线+中划线）
            .pending-remove {
                background-color: #FAFBFD;
                color: #C4C6CC;
            }
            .pending-remove .cell-input:not(:placeholder-shown) {
                text-decoration: line-through;
            }
            // 已移除
            .confirmed-remove {
                background-color: #fff0f0;
            }
            .strikethrough {
                text-decoration: line-through;
                color: #C4C6CC;
            }
        }
    }
}
.add-column-content {
    .add-title {
        font-size: 20px;
        color: #313238;
        margin-bottom: 20px;
    }
    .disabled-cell {
        text-decoration: line-through;
        color: #C4C6CC;
    }
    .existing-columns {
        padding-bottom: 16px;
        margin-bottom: 8px;
        border-bottom: 1px solid #DCDEE5;
    }
    .tag-title {
        margin-bottom: 6px;
        font-size: 14px;
        color: #63656E;
        display: flex;
        justify-content: space-between;
        align-items: center;
    }
    .existing-column-item {
        display: flex;
        flex-wrap: wrap;
        p {
            flex: 0 0 50%;
            margin: 6px 0;
            display: flex;
            align-items: center;
            gap: 6px;
            ::v-deep .bk-checkbox-text {
                max-width: 90px;
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
            }
        }
    }
}
.tag-flag {
    font-size: 12px;
    padding: 1px 4px;
    border-radius: 3px;
    &.added {
        background-color: #e6f4ea;
        color: #34a853;
    }
    &.removed {
        background-color: #fee;
        color: #ea4335;
    }
}
.edit-popover {
    .edit-content {
        margin-bottom: 12px;
        .content-tit {
            font-size: 14px;
            color: #63656E;
            margin-bottom: 8px;
        }
    }
    .edit-btn {
        margin-bottom: 8px;
        color: #3A84FF;
        font-size: 12px;
        text-align: right;
        span {
            display: inline-block;
            margin: 0 5px;
            cursor: pointer;
        }
    }
}
.cell-actions {
    position: absolute;
    right: 5px;
    top: 50%;
    transform: translateY(-50%);
    display: flex;
    gap: 5px;
    z-index: 12;
    .cell-action-btn {
        margin: 0 8px;
        font-size: 14px;
        color: #3A84FF;
        vertical-align: middle;
        &.restore {
            width: 14px;
            height: 14px;
        }
    }
}
</style>
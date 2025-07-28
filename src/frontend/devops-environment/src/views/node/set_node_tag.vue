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
                                        :key="tag.tagKeyId"
                                        v-if="tag && tag.tagKeyId"
                                    >
                                        <bk-checkbox
                                            v-model="selectedNewTags[tag.tagKeyId]"
                                            class="new-tag-checkbox"
                                            @change="handleNewTagChange"
                                        >
                                            {{ tag.tagKeyName }}
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
                                    <span
                                        :class="{ 'strikethrough': col.disabled, 'content-label': true }"
                                        v-bk-overflow-tips
                                    >{{ col.label }}</span>

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
                                                transfer
                                                placement="bottom"
                                                :tippy-options="{
                                                    hideOnClick: 'toggle',
                                                    interactive: true,
                                                    arrow: true,
                                                    onShow: () => handlePopoverShow(colIndex),
                                                    onHide: () => handlePopoverHide(colIndex)
                                                }"
                                            >
                                                <div slot="content">
                                                    <div class="edit-content">
                                                        <p class="content-tit">批量设置 {{ col.label }}</p>
                                                        <bk-select
                                                            v-model="editColumnValue"
                                                            placeholder="请选择"
                                                            :clearable="true"
                                                        >
                                                            <bk-option
                                                                v-for="option in getColumnTagValues(col.tagKeyId)"
                                                                :key="option.tagValueId"
                                                                :id="option.tagValueId"
                                                                :name="option.tagValueName"
                                                            ></bk-option>
                                                        </bk-select>
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
                                    <span :title="row[col.key]">{{ row[col.key] }}</span>
                                </template>
                                <template v-else>
                                    <bk-select
                                        v-model="row.tagDetails[col.key].tagValueId"
                                        :class="[
                                            'cell-select',
                                            getCellStatusClass(rowIndex, col)
                                        ]"
                                        placeholder="请选择"
                                        :clearable="false"
                                        :popover-width="260"
                                        :popover-options="{ boundary: 'viewport' }"
                                        :disabled="isPendingRemove(rowIndex, col.key) || col.disabled"
                                        @change="handleCellChange(rowIndex, col.key, $event, col.tagKeyId)"
                                    >
                                        <bk-option
                                            v-for="option in getColumnTagValues(col.tagKeyId)"
                                            :key="option.tagValueId"
                                            :id="option.tagValueId"
                                            :name="option.tagValueName"
                                        ></bk-option>
                                    </bk-select>
                                    <div
                                        class="cell-actions"
                                        v-if="cellActionVisibility[rowIndex] && cellActionVisibility[rowIndex][col.key] && colIndex !== 0 && !col.disabled"
                                    >
                                        <i
                                            v-if="!(cellDisabled[rowIndex] && cellDisabled[rowIndex][col.key]) && row.tagDetails[col.key].tagValueId"
                                            v-bk-tooltips="{ content: '移除' }"
                                            class="devops-icon icon-delete cell-action-btn"
                                            @click.stop="handleCellDelete(rowIndex, col.key)"
                                        ></i>
                                        <img
                                            v-if="cellDisabled[rowIndex] && cellDisabled[rowIndex][col.key]"
                                            v-bk-tooltips="{ content: '恢复' }"
                                            class="cell-action-btn restore"
                                            src="../../scss/svg/revocation.svg"
                                            @click.stop="handleCellRestore(rowIndex, col.key, col.tagKeyId)"
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
                tableData: [],
                otherTags: [],
                tableColumns: [],
                initialTableColumns: [],
                initialOtherTags: [],
                cellDisabled: {},
                cellActionVisibility: {},
                cellDataStatus: {},
                confirmedRemoved: {},
                originalData: {},
                selectedExistingTags: {},
                selectedNewTags: {},
                checkAllExisting: false,
                editColumnValue: '',
                hasVerticalScrollbar: false,
                currentOpenPopover: null,
            }
        },
        computed: {
            ...mapState('environment', ['nodeTagList', 'selectionTagList']),
            projectId () {
                return this.$route.params.projectId
            },
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
            this.getInitData()
            this.initCellStates()  // 初始化单元格状态
            this.saveOriginalData() // 保存原始数据
            this.syncSelectedTags()
            this.$nextTick(() => this.checkScrollbar())

            document.addEventListener('click', this.handleDocumentClick)
        },
        beforeDestroy () {
            document.removeEventListener('click', this.handleDocumentClick)
        },
        methods: {
            getInitData (){
                const tableList = this.selectionTagList.map(item => ({
                    nodeId: item.nodeId,
                    tags: item.tags,
                    displayName: item.displayName
                }))
                const { tableColumns, tableData, otherTags } = this.transformNodeData(tableList, this.nodeTagList)
                this.tableColumns = tableColumns
                this.tableData = tableData
                this.otherTags = otherTags

                this.initialTableColumns = JSON.parse(JSON.stringify(this.tableColumns))
                this.initialOtherTags = JSON.parse(JSON.stringify(this.otherTags))
            },
            
            saveOriginalData () {
                this.tableData.forEach((row, rowIndex) => {
                    this.$set(this.originalData, rowIndex, {
                        ...row,
                        tagDetails: JSON.parse(JSON.stringify(row.tagDetails))
                    })
                })
            },

            handlePopoverShow (colIndex) {
                if (this.currentOpenPopover !== null && this.currentOpenPopover !== colIndex) {
                    const prevPopoverRef = this.$refs[`editPopover_${this.currentOpenPopover}`]?.[0]
                    if (prevPopoverRef) {
                        prevPopoverRef.hideHandler()
                    }
                }
                this.currentOpenPopover = colIndex
            },

            handlePopoverHide (colIndex) {
                if (this.currentOpenPopover === colIndex) {
                    this.currentOpenPopover = null
                }
            },
            // 监听全局点击，判断是否应该关闭 Popover
            handleDocumentClick (event) {
                if (!this.currentOpenPopover) return

                const popoverRef = this.$refs[`editPopover_${this.currentOpenPopover}`]?.[0]
                if (!popoverRef) return

                const isInsidePopover = event.target.closest('.edit-popover') || event.target.closest('.tippy-content')
                const isTrigger = event.target.closest('.header-icon')

                if (!isInsidePopover && !isTrigger) {
                    popoverRef.hideHandler()
                    this.currentOpenPopover = null
                }
            },

            transformNodeData (originalData, allTags) {
                const tagKeyMap = new Map()
                originalData.forEach(item => {
                    item.tags.forEach(tag => {
                        if (!tagKeyMap.has(tag.tagKeyName)) {
                            tagKeyMap.set(tag.tagKeyName, tag.tagKeyId)
                        }
                    })
                })
    
                const tableColumns = [
                    {
                        key: 'displayName',
                        label: '已选节点',
                        disabled: false,
                        hidden: false
                    }
                ]

                tagKeyMap.forEach((tagKeyId, key) => {
                    tableColumns.push({
                        key: key,
                        label: key,
                        tagKeyId: tagKeyId,
                        disabled: false,
                        hidden: false,
                        isNew: false
                    })
                })

                const tableData = originalData.map(item => {
                    const row = {
                        displayName: item.displayName,
                        nodeId: item.nodeId,
                        tagDetails: {}
                    }

                    const tagMap = {}
                    item.tags.forEach(tag => {
                        const tagValue = tag.tagValues.length > 0
                            ? tag.tagValues[0].tagValueName
                            : ''
                        const tagValueId = tag.tagValues.length > 0
                            ? tag.tagValues[0].tagValueId
                            : ''
                
                        tagMap[tag.tagKeyName] = {
                            value: tagValue,
                            tagValueId: tagValueId,
                            tagKeyId: tag.tagKeyId
                        }
                    })

                    tagKeyMap.forEach((tagKeyId, key) => {
                        const tagInfo = tagMap[key] || { value: '', tagValueId: '', tagKeyId: tagKeyId }
                        row[key] = tagInfo.value
                        row.tagDetails[key] = {
                            tagValueId: tagInfo.tagValueId,
                            tagKeyId: tagInfo.tagKeyId || tagKeyId,
                            originalValue: tagInfo.value,
                            isNew: false
                        }
                    })
        
                    return row
                })

                const tableColumnKeys = new Set(tableColumns.map(col => col.key))
                const otherTags = allTags.filter(tag => !tableColumnKeys.has(tag.tagKeyName))
                    .map(tag => ({
                        tagKeyId: tag.tagKeyId,
                        tagKeyName: tag.tagKeyName,
                        disabled: false
                    }))
    
                return {
                    tableColumns,
                    tableData,
                    otherTags
                }
            },

            getColumnTagValues (tagKeyId) {
                const tagInfo = this.nodeTagList.find(tag => tag.tagKeyId === tagKeyId)
                return tagInfo?.tagValues || []
            },
            
            toNodeList () {
                this.$router.push({ name: 'nodeList' })
            },

            checkScrollbar () {
                const container = this.$refs.scrollContainer
                this.hasVerticalScrollbar = container?.scrollHeight > container?.clientHeight || false
            },

            syncSelectedTags () {
                Object.keys(this.selectedExistingTags).forEach(key => {
                    this.$delete(this.selectedExistingTags, key)
                })
            
                this.tableColumns.forEach((col, index) => {
                    if (index !== 0 && col?.key) {
                        this.$set(this.selectedExistingTags, col.key, !col.disabled)
                    }
                })
            
                this.checkAllStatus()
            },

            checkAllStatus () {
                const selectableColumns = this.tableColumns.filter((col, index) => index !== 0 && col?.key)
                this.checkAllExisting = selectableColumns.length
                    ? selectableColumns.every(col => this.selectedExistingTags[col.key])
                    : false
            },

            handleTagChange () {
                this.checkAllStatus()
                this.updateChangeFlags()

                this.tableColumns.forEach((col, index) => {
                    if (index !== 0 && col?.key) {
                        this.$set(col, 'disabled', !this.selectedExistingTags[col.key])
                    }
                })
            },

            handleNewTagChange () {
                this.updateChangeFlags()
            },

            updateChangeFlags () {
                this.tableColumns.forEach((col, index) => {
                    if (index === 0 || !col?.key) return
                    const initialCol = this.initialTableColumns.find(item => item?.key === col.key)
                    this.$set(col, 'removedRecently', !!initialCol && !this.selectedExistingTags[col.key])
                })
                
                Object.keys(this.selectedNewTags).forEach(tagKey => {
                    if (this.selectedNewTags[tagKey]) {
                        const existingCol = this.tableColumns.find(col => col?.key === tagKey)
                        if (existingCol && !this.initialTableColumns.some(col => col?.key === tagKey)) {
                            this.$set(existingCol, 'addedRecently', true)
                        }
                    }
                })
            },

            initCellStates () {
                this.tableData.forEach((row, rowIndex) => {
                    if (!this.cellDisabled[rowIndex]) {
                        this.$set(this.cellDisabled, rowIndex, {})
                    }

                    if (!this.cellActionVisibility[rowIndex]) {
                        this.$set(this.cellActionVisibility, rowIndex, {})
                    }

                    if (!this.cellDataStatus[rowIndex]) {
                        this.$set(this.cellDataStatus, rowIndex, {})
                    }

                    if (!this.confirmedRemoved[rowIndex]) {
                        this.$set(this.confirmedRemoved, rowIndex, {})
                    }

                    Object.keys(row.tagDetails).forEach(key => {
                        this.$set(this.cellDisabled[rowIndex], key, false)
                        this.$set(this.cellActionVisibility[rowIndex], key, false)
                        this.$set(this.confirmedRemoved[rowIndex], key, false)
                        this.$set(this.cellDataStatus[rowIndex], key, '')
                    })
                })
            },

            handleCheckAllExisting (checked) {
                this.tableColumns.forEach((col, index) => {
                    if (index !== 0 && col?.key) {
                        this.$set(this.selectedExistingTags, col.key, checked)
                        this.$set(col, 'disabled', !checked)
                    }
                })
                this.updateChangeFlags()
            },

            isPendingRemove (rowIndex, colKey) {
                return this.cellDisabled[rowIndex]?.[colKey] && !this.confirmedRemoved[rowIndex]?.[colKey]
            },

            isConfirmedRemove (rowIndex, colKey) {
                return this.confirmedRemoved[rowIndex]?.[colKey]
                       && (this.tableData[rowIndex].tagDetails[colKey].tagValueId === ''
                        || this.tableData[rowIndex].tagDetails[colKey].tagValueId === null)
            },

            hasColumnPendingRemovals () {
                return this.tableColumns.some((col, index) => index !== 0 && col?.key && col.disabled)
            },

            hasCellPendingRemovals () {
                return Object.values(this.cellDisabled).some(row =>
                    Object.values(row).some(isDisabled => isDisabled)
                )
            },

            getCellStatusClass (rowIndex, col) {
                const { key: colKey, isNew, disabled } = col
                const cellStatus = this.cellDataStatus[rowIndex]?.[colKey]

                return {
                    'pending-remove': this.isPendingRemove(rowIndex, colKey) || disabled,
                    'confirmed-remove': this.isConfirmedRemove(rowIndex, colKey),
                    'cell-new': cellStatus === 'new' || isNew,
                    'cell-modified': cellStatus === 'modified'
                }
            },

            handleCellChange (rowIndex, colKey, newValue, colTagKeyId) {
                const originalValue = this.originalData[rowIndex]?.tagDetails?.[colKey]?.tagValueId
                const row = this.tableData[rowIndex]

                this.$set(row.tagDetails[colKey], 'tagKeyId', colTagKeyId)
                this.$set(row.tagDetails[colKey], 'tagValueId', newValue)

                const tagValues = this.getColumnTagValues(colTagKeyId)
                const selectedOption = tagValues.find(option => option.tagValueId === newValue)
                if (selectedOption) {
                    row[colKey] = selectedOption.tagValueName
                } else {
                    row[colKey] = ''
                }

                if ((originalValue === undefined || originalValue === '') && (newValue !== '' && newValue !== null)) {
                    this.$set(this.cellDataStatus[rowIndex], colKey, 'new')
                } else if (originalValue !== newValue) {
                    this.$set(this.cellDataStatus[rowIndex], colKey, 'modified')
                } else {
                    this.$set(this.cellDataStatus[rowIndex], colKey, '')
                }
            },

            confirmEditColData (colIndex) {
                const col = this.tableColumns[colIndex]
                if (!col?.key || !col.tagKeyId) return
                
                // 获取选中的值对应的显示文本
                const tagValues = this.getColumnTagValues(col.tagKeyId)
                const selectedOption = tagValues.find(option => option.tagValueId === this.editColumnValue)
                const displayValue = selectedOption ? selectedOption.tagValueName : ''

                this.tableData.forEach((row, rowIndex) => {
                    this.$set(row.tagDetails[col.key], 'tagKeyId', col.tagKeyId)
                    this.$set(row.tagDetails[col.key], 'tagValueId', this.editColumnValue)
                    
                    row[col.key] = displayValue
                    this.handleCellChange(rowIndex, col.key, this.editColumnValue, col.tagKeyId)
                })

                this.cancleEditColData(colIndex)
            },

            cancleEditColData (colIndex) {
                this.$refs[`editPopover_${colIndex}`]?.[0]?.hideHandler()
                this.editColumnValue = ''
            },

            handleDeleteCol (colIndex) {
                if (colIndex === 0) return
                const col = this.tableColumns[colIndex]
                if (col?.key) {
                    this.$set(col, 'disabled', true)
                    this.$set(this.selectedExistingTags, col.key, false)
                    this.checkAllStatus()
                    this.updateChangeFlags()
                }
            },

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

            showCellActionsFn (rowIndex, colKey, show) {
                if (!this.cellActionVisibility[rowIndex]) {
                    this.$set(this.cellActionVisibility, rowIndex, {})
                }
                this.$set(this.cellActionVisibility[rowIndex], colKey, show)
            },

            handleCellDelete (rowIndex, colKey) {
                this.$set(this.cellDisabled[rowIndex], colKey, true)
                this.$set(this.confirmedRemoved[rowIndex], colKey, false)
            },

            handleCellRestore (rowIndex, colKey, colTagKeyId) {
                this.$set(this.cellDisabled[rowIndex], colKey, false)
                this.$set(this.confirmedRemoved[rowIndex], colKey, false)

                if (this.originalData[rowIndex]?.tagDetails?.[colKey]) {
                    this.$set(this.tableData[rowIndex].tagDetails[colKey], 'tagKeyId', colTagKeyId)
                    this.$set(this.tableData[rowIndex].tagDetails[colKey], 'tagValueId',this.originalData[rowIndex].tagDetails[colKey].tagValueId)
                    
                    this.tableData[rowIndex][colKey] = this.originalData[rowIndex].tagDetails[colKey].originalValue
                    this.handleCellChange(rowIndex, colKey, this.originalData[rowIndex].tagDetails[colKey].tagValueId, colTagKeyId)
                }
            },

            // 确认添加/展示标签 - 此时才真正执行添加/移除操作
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

                this.tableColumns = this.tableColumns.filter(col =>
                    !tagsToRemove.some(t => t.key === col.key) || col.key === 'displayName'
                )

                tagsToRemove.forEach(tag => {
                    if (!this.otherTags.some(t => t.tagKeyName === tag.key)) {
                        this.otherTags.push({ tagKeyId: tag.tagKeyId, tagKeyName: tag.label, disabled: false })
                    }
                })

                this.tableData.forEach((row, rowIndex) => {
                    tagsToRemove.forEach(tag => {
                        this.$delete(row, tag.key)
                        this.$delete(this.cellDisabled[rowIndex], tag.key)
                        this.$delete(this.originalData[rowIndex], tag.key)
                        this.$delete(row.tagDetails, tag.key)
                    })
                })
            },
            // 处理单元格级移除
            handleCellRemovals () {
                this.tableData.forEach((row, rowIndex) => {
                    Object.entries(this.cellDisabled[rowIndex] || {}).forEach(([colKey, isDisabled]) => {
                        if (isDisabled) {
                            this.$set(row.tagDetails[colKey], 'tagValueId', '')
                            row[colKey] = ''
                            this.$set(this.confirmedRemoved[rowIndex], colKey, true)
                            this.$set(this.cellDisabled[rowIndex], colKey, false)
                            this.$set(this.cellDataStatus[rowIndex], colKey, '')
                        }
                    })
                })
            },
            // 处理新增标签
            handleNewTags () {
                const newTagKeys = Object.keys(this.selectedNewTags).filter(key => this.selectedNewTags[key])
                if (!newTagKeys.length) return

                const newTagsToAdd = this.otherTags.filter(tag => newTagKeys.includes(String(tag.tagKeyId)))

                newTagsToAdd.forEach(tag => {
                    if (this.tableColumns.some(col => col.key === tag.tagKeyName)) return

                    const newCol = {
                        key: tag.tagKeyName,
                        label: tag.tagKeyName,
                        tagKeyId: tag.tagKeyId,
                        hidden: false,
                        disabled: false,
                        isNew: true,
                        addedRecently: true
                    }
                    
                    this.tableColumns.splice(1, 0, newCol)

                    this.tableData.forEach((row, rowIndex) => {
                        this.$set(row, tag.tagKeyName, '')
                        this.$set(this.cellDisabled[rowIndex], tag.tagKeyName, false)
                        this.$set(this.originalData[rowIndex], tag.tagKeyName, '')
                        this.$set(row.tagDetails, tag.tagKeyName, {
                            tagValueId: '',
                            tagKeyId: tag.tagKeyId,
                            originalValue: '',
                            isNew: true
                        })
                    })
                    this.$set(this.selectedExistingTags, tag.tagKeyName, true)
                })
                this.otherTags = this.otherTags.filter(tag => !newTagKeys.includes(String(tag.tagKeyId)))
            },

            resetTagSelection () {
                Object.keys(this.selectedNewTags).forEach(key => {
                    this.$delete(this.selectedNewTags, key)
                })

                this.tableColumns.forEach(col => col && this.$set(col, 'removedRecently', false))
            },

            handlePreviewAndSave () {
                if (this.hasPendingRemovals) {
                    this.$bkInfo({
                        theme: 'warning',
                        type: 'warning',
                        title: '是否确定移除数据',
                        confirmFn: () => {
                            this.confirmAddColumn()
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
            padding-right: 1px;
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
                padding: 10px;
                text-align: left;
                border: 1px solid #DCDEE5;
                position: relative;
                background-color: #fff;
            }
            .content-td {
                padding: 2px;
            }
            th {
                background-color: #F0F1F5;
                min-width: 280px;
                max-width: 280px;
            }
            th:first-child, td:first-child {
                width: 200px;
                min-width: 200px;
                max-width: 200px;
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
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
                .content-label {
                    max-width: 80%;
                    overflow: hidden;
                    text-overflow: ellipsis;
                    white-space: nowrap;
                }
                .header-actions {
                    display: flex;
                    .header-icon {
                        color: #3A84FF;
                        font-size: 12px;
                    }
                    .icon-delete {
                        margin-left: 8px;
                        font-size: 14px;
                    }
                    .restore-btn {
                        width: 14px;
                        height: 14px;
                    }
                }
            }
            .cell-select {
                width: 100%;
                height: 100%;
                font-size: 12px;
                border: none;
                outline: none;
                background-color: transparent;
                ::v-deep .icon-angle-down:before {
                    content: '';
                }
            }
            .content-td:not(.pending-remove):hover .cell-select {
                cursor: pointer;
                background-color: #fafbfd;
            }
            .content-td:not(.pending-remove):hover {
                z-index: 11;
                background-color: #fafbfd;
                outline: 1px solid #a3c5fd;
            }
            .content-td .cell-select:focus {
                outline: none;
            }
            ::v-deep .content-td .bk-select.is-focus {
                box-shadow: none !important;
            }
            .content-td:focus-within {
                z-index: 11;
                outline: 1px solid #a3c5fd;
            }
            // 新增 - 浅绿色背景
            .cell-new {
                background-color: #F2FFF4;
            }
            // 修改 - 浅黄色背景
            .cell-modified {
                background-color: #FFF3E1;
            }
            // 新增列标题背景色
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
            .pending-remove .cell-select:not(:placeholder-shown) {
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
    right: 10px;
    top: 50%;
    transform: translateY(-50%);
    display: flex;
    gap: 5px;
    z-index: 12;
    .cell-action-btn {
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
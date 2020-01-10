<template>
    <div class="card" :class="isSelected ? 'active' : ''">
        <p :class="isSelected ? 'active' : ''" class="title">{{title}}</p>
        <p :class="isSelected ? 'active' : ''">{{isSelected}}/{{total}}</p>
        <bk-switcher
            v-if="index !== 0"
            class="bk-switcher-xsmall"
            v-model="switcher"
            size="small"
            :title="switcher ? $t('checkers.关闭') : $t('suspend.启用')"
            @click.native="comfirm(switcher)"
        >
        </bk-switcher>
        <div class="status" :style="index === 0 ? 'padding-top: 20px;' : ''" v-if="!realOpen">
            <p @click="toggle">{{$t('checkers.展开')}}</p>
            <i class="bk-icon icon-angle-right" @click="toggle"></i>
        </div>
        <div class="status" :style="index === 0 ? 'padding-top: 20px;' : ''" v-else-if="realOpen">
            <p @click="toggle">{{$t('checkers.收起')}}</p>
            <i class="bk-icon icon-angle-down" @click="toggle"></i>
        </div>
        <div
            class="card-content"
            v-if="realOpen"
            :class="index === 0 ? 'is-first' : 'is-others'"
            :style="'margin-left:' + cardPosition + 'px;width:' + realWidth + 'px;'"
        >
            <bk-button class="add-button" theme="primary" @click="openChangeRules">{{$t('checkers.配置规则')}}</bk-button>
            <i class="bk-icon icon-close" @click="toggle"></i>
            <bk-table
                ref="rulesTable"
                class="rules-table"
                :data="tableData"
                :height="elHeight"
                :size="size"
                :empty-text="$t('checkers.暂无规则')"
                :cell-class-name="cellStyle"
                :toggle-row-selection="select"
                @select="select"
                @select-all="select"
                @selection-change="select"
            >
                <bk-table-column type="selection" width="60" align="center"></bk-table-column>
                <bk-table-column type="index" :label="$t('st.序号')" align="center" width="60"></bk-table-column>
                <bk-table-column :label="$t('defect.规则')" prop="checkerKey" width="200">
                    <template slot-scope="props"> <span :title="props.row.checkerKey">{{ props.row.checkerKey }}</span></template>
                </bk-table-column>
                <bk-table-column :label="$t('st.规则类型')" prop="checkerType" width="120"></bk-table-column>
                <bk-table-column :sort-orders="['ascending', 'descending', null]" :sortable="true" :label="$t('defect.级别')" prop="severity" width="80"></bk-table-column>
                <bk-table-column :label="$t('st.详细说明')" prop="checkerDesc">
                    <template slot-scope="props"> <span :title="props.row.checkerDesc">{{ props.row.checkerDesc }}</span></template>
                </bk-table-column>
            </bk-table>
        </div>
        <bk-dialog v-model="singleVisiable"
            :theme="'primary'"
            :mask-close="false"
            :title="editTitle"
            @confirm="changeRules"
            :footer-position="'center'"
        >
            <div class="center">{{operateText}}</div>
        </bk-dialog>
        <bk-dialog v-model="mutiVisiable"
            :theme="'primary'"
            :mask-close="false"
            :title="editTitle"
            @confirm="changeAllRules"
            @after-leave="cancel"
            :footer-position="'center'"
        >
            <div class="center">{{operateText}}</div>
        </bk-dialog>
    </div>
</template>
<script>
    export default {
        props: {
            data: {
                type: Object,
                default: {}
            },
            index: {
                type: Number,
                default: 0
            }
        },
        data () {
            return {
                comfirmVisible: false,
                selectedRules: [],
                singleVisiable: false,
                mutiVisiable: false,
                open: 0,
                close: 0,
                switcher: false,
                onFocusItem: '',
                editTitle: '',
                realWidth: 1080,
                selected: false,
                elHeight: 292,
                operateText: ''
            }
        },
        computed: {
            tableData () {
                const tableData = []
                if (this.data) {
                    Object.assign(tableData, this.data.checkerList)
                }
                tableData.map(item => {
                    if (item.severity === 1) {
                        item.severity = this.$t('defect.严重')
                    } else if (item.severity === 2) {
                        item.severity = this.$t('defect.一般')
                    } else if (item.severity === 3) {
                        item.severity = this.$t('defect.提示')
                    }
                })
                this.$nextTick(() => {
                    for (const i in tableData) {
                        if (tableData[i].checkerStatus && this.$refs.rulesTable) {
                            this.$refs.rulesTable.toggleRowSelection(tableData[i], true)
                        }
                    }
                })
                window.scrollTo(0, 0)
                return tableData
            },
            title () {
                let title = ''
                if (this.data.pkgDesc) {
                    title = this.data.pkgDesc
                }
                return title
            },
            total () {
                let total = 0
                if (this.data.totalCheckerNum) {
                    total = this.data.totalCheckerNum
                }
                return total
            },
            isSelected () {
                let isSelected = 0
                isSelected = this.data.openCheckerNum
                this.switcher = isSelected !== 0
                return isSelected
            },
            cardPosition () {
                let cardPosition = 0
                if (this.index !== 0) {
                    cardPosition = this.index - 1
                    while (cardPosition > (this.realWidth / 210 - 1)) {
                        cardPosition = cardPosition - (this.realWidth / 210 - 1)
                    }
                }
                cardPosition = parseInt(cardPosition) * (-210)
                return cardPosition
            },
            realOpen () {
                let realOpen = false
                let height = 292
                let width = 1085
                if (this.data) {
                    realOpen = this.data.open
                }
                this.$nextTick(() => {
                    if (this.$refs.rulesTable) {
                        if (this.$refs.rulesTable.$el.parentNode.clientHeight > 292) {
                            height = this.$refs.rulesTable.$children[7].$children.length > 7 ? 292 : (this.$refs.rulesTable.$children[7].$children.length + 1) * 42.1
                        } else {
                            height = this.$refs.rulesTable.$el.parentNode.clientHeight - 78
                        }
                        this.elHeight = height
                    }
                })
                this.$nextTick(() => {
                    if (this.$refs.rulesTable) {
                        width = this.$refs.rulesTable.$el.parentNode.parentNode.parentNode.parentNode.clientWidth
                        this.realWidth = width
                    }
                })
                return realOpen
            }
        },
        watch: {
            tableData: {
                handler (val) {
                    this.select()
                },
                deep: true
            },
            switcher: {
                handler (val) {
                    this.select()
                },
                deep: true
            }
            
        },
        mounted () {
        },
        created () {
            this.switcher = this.isSelected !== 0
        },
        methods: {
            cancel () {
                this.switcher = this.isSelected !== 0
            },
            toggle () {
                this.$emit('change', this.index)
            },
            comfirm (switcher) {
                this.$emit('close', this.index)
                this.open = 0
                this.close = 0
                for (const i in this.tableData) {
                    if (!this.tableData[i].checkerStatus) {
                        this.open++
                    } else if (this.tableData[i].checkerStatus) {
                        this.close++
                    }
                }
                if (!this.switcher) {
                    this.editTitle = this.$t('checkers.关闭x条规则', { num: this.close })
                    this.operateText = this.$t('checkers.规则减少可能会让扫描错过一些缺陷，增大代码质量风险。')
                } else {
                    this.editTitle = this.$t('checkers.启用x条规则', { num: this.open })
                    this.operateText = this.$t('checkers.你的代码将接受更细致的扫描，告警误报率也会有所上升。')
                }
                this.mutiVisiable = true
            },
            changeAllRules () {
                const openedCheckers = []
                const closedCheckers = []
                for (const i in this.tableData) {
                    if (!this.tableData[i].checkerStatus && !this.switcher) {
                        openedCheckers.push(this.tableData[i].checkerKey)
                    } else if (this.tableData[i].checkerStatus && this.switcher) {
                        closedCheckers.push(this.tableData[i].checkerKey)
                    }
                }
                const postData = {
                    pkgId: this.data.pkgId,
                    toolName: this.$route.params.toolId,
                    openedCheckers: openedCheckers,
                    closedCheckers: closedCheckers
                }
                this.$store.dispatch('tool/changeRules', postData).then(res => {
                    if (res === true) {
                        this.$bkMessage({ theme: 'success', message: this.$t('op.配置成功') })
                        this.$store.dispatch('tool/rules', this.$route.params.toolId)
                    }
                }).catch(e => {
                    this.$bkMessage({ theme: 'error', message: this.$t('op.配置失败') })
                }).finally(() => {
                    this.operateText = ''
                    this.switcher = this.isSelected !== 0
                })
            },
            cellStyle ({ row, column, rowIndex, columnIndex }) {
                if (column.property === 'severity') {
                    if (row.severity === this.$t('defect.严重')) {
                        return 'majorColor'
                    } else if (row.severity === this.$t('defect.一般')) {
                        return 'minorColor'
                    } else if (row.severity === this.$t('defect.提示')) {
                        return 'infoColor'
                    }
                }
            },
            select (row) {
                let arr = []
                this.selected = true
                arr = Object.assign(arr, row)
                this.selectedRules = arr
            },
            changeRules () {
                const openedCheckers = []
                const closedCheckers = []
                if (this.selected) {
                    for (const i in this.tableData) {
                        if (!this.tableData[i].checkerStatus && this.selectedRules.includes(this.tableData[i])) {
                            openedCheckers.push(this.tableData[i].checkerKey)
                        } else if (this.tableData[i].checkerStatus && !this.selectedRules.includes(this.tableData[i])) {
                            closedCheckers.push(this.tableData[i].checkerKey)
                        }
                    }
                } else {
                    this.$bkMessage({ theme: 'error', message: this.$t('op.规则不能为空') })
                    return
                }
                const postData = {
                    pkgId: this.data.pkgId,
                    toolName: this.$route.params.toolId,
                    openedCheckers: openedCheckers,
                    closedCheckers: closedCheckers
                }
                this.$store.dispatch('tool/changeRules', postData).then(res => {
                    if (res === true) {
                        this.$bkMessage({ theme: 'success', message: this.$t('op.配置成功') })
                        this.$store.dispatch('tool/rules', this.$route.params.toolId)
                    }
                }).catch(e => {
                    this.$bkMessage({ theme: 'error', message: this.$t('op.配置失败') })
                })
            },
            position (i) {
                setTimeout(() => {
                    if (this.$refs.rulesTable) {
                        if (this.onFocusItem) {
                            this.$refs.rulesTable.$children[7].$children[this.onFocusItem].$el.parentNode.parentNode.parentNode.style.background = ''
                        }
                        this.onFocusItem = i
                        this.$refs.rulesTable.$children[7].$el.parentNode.scrollTop = i * 42
                        this.$refs.rulesTable.$children[7].$children[i].$el.parentNode.parentNode.parentNode.style.background = '#f2f2f2'
                    }
                }, 0)
            },
            openChangeRules () {
                this.open = 0
                this.close = 0
                if (this.selected) {
                    for (const i in this.tableData) {
                        if (!this.tableData[i].checkerStatus && this.selectedRules.includes(this.tableData[i])) {
                            this.open++
                        } else if (this.tableData[i].checkerStatus && !this.selectedRules.includes(this.tableData[i])) {
                            this.close++
                        }
                    }
                    if (this.open === 0 && this.close !== 0) {
                        this.editTitle = this.$t('checkers.关闭x条规则', { num: this.close })
                        this.operateText = this.$t('checkers.规则减少可能会让扫描错过一些缺陷，增大代码质量风险。')
                    } else if (this.close === 0 && this.open !== 0) {
                        this.editTitle = this.$t('checkers.启用x条规则', { num: this.open })
                        this.operateText = this.$t('checkers.你的代码将接受更细致的扫描，告警误报率也会有所上升。')
                    } else if (this.open !== 0 && this.close !== 0) {
                        this.editTitle = this.$t('checkers.启用x条规则', { num: this.open }) + ',' + this.$t('checkers.关闭x条规则', { num: this.close })
                        this.operateText = this.$t('checkers.你的代码将接受更细致的扫描，告警误报率也会有所上升。') + this.$t('checkers.加油，对代码质量永远追求卓越！')
                    } else {
                        this.editTitle = this.$t('checkers.没有改动规则')
                    }
                } else {
                    this.editTitle = this.$t('checkers.没有改动规则')
                }
                this.singleVisiable = true
            }
        }
    }
</script>
<style lang="postcss" scoped>
    @import '../../css/variable.css';
    .card {
        position: relative;
        /* display: block; */
        width: 200px;
        height: 120px;
        background-color: #ffffff;
        border-radius: 2px;
        border: 1px solid $itemBorderColor;
        color: #979ba5;
        .bk-switcher-xsmall {
            top: 16px;
            left: 156px;
        }
        >>>.is-checked {
            background-color: $goingColor;
        }
        .status {
            padding-left: 15px;
            margin-top: -2px;
            font-size: 12px;
            p {
                padding-right: 2px;
                display: inline;
                font-size: 12px;
            }
            i, p {
                cursor: pointer;
            }
            i, p:hover {
                color: $goingColor;
            }
        }
        p {
            font-size: 14px;
            text-align: center;
            padding-top: 6px;
        }
        .title {
            font-size: 16px;
            padding-top: 30px;
            font-weight:bold;
        }
        .active {
            color: $goingColor;
        }
        .card-content {
            margin-top: 30px;
            background-color: #ffffff;
            padding: 15px;
            border: 1px solid $borderColor;
            float: left;
            .rules-table {
                margin-top: 15px;
                max-height: 380px;
                /* overflow: auto; */
                >>>.majorColor {
                    color: $majorColor;
                }
                >>>.infoColor {
                    color: $infoColor;
                }
                >>>.minorColor {
                    color: $minorColor;
                }
            }
            .bk-icon.icon-close {
                float: right;
                cursor: pointer;
            }
        }
        .is-others:before {
            content: '';
            width: 0;
            height: 0;
            border: 10px solid transparent;
            border-bottom-color: $borderColor;
            position: absolute;
            left: 88px;
            top: 120px;
        }
        .is-others:after {
            content: "";
            width: 0;
            height: 0;
            border: 9px solid transparent;
            border-bottom-color: #ffffff;
            position: absolute;
            left: 89px;
            top: 141px;
            margin-top: -18px;
        }
        .is-first:before {
            content: '';
            width: 0;
            height: 0;
            border: 10px solid transparent;
            border-bottom-color: $borderColor;
            position: absolute;
            left: 88px;
            top: 121px;
        }
        .is-first:after {
            content: "";
            width: 0;
            height: 0;
            border: 9px solid transparent;
            border-bottom-color: #ffffff;
            position: absolute;
            left: 89px;
            top: 142px;
            margin-top: -18px;
        }
    }
    .card.active {
        border: 1px solid $goingColor;
    }
    .center {
        text-align: center;
    }
</style>

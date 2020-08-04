<template>
    <div class="card" :class="isSelected ? 'active' : ''">
        <p :class="isSelected ? 'active' : ''" class="title"><i class="icon codecc-icon tool-icon" :class="isSelected ? `icon-${iconName}-selected` : `icon-${iconName}`"></i>{{title}}</p>
        <p :class="isSelected ? 'active' : ''">{{isSelected}}/{{total}}</p>
        <bk-switcher
            v-if="index !== 0"
            class="bk-switcher-xsmall"
            v-model="switcher"
            size="small"
            :title="switcher ? $t('关闭') : $t('启用')"
            @click.native="comfirm(switcher)"
        >
        </bk-switcher>
        <div class="status" :style="index === 0 ? 'padding-top: 20px;' : ''" v-if="!realOpen">
            <p @click="toggle">{{$t('展开')}}</p>
            <i class="bk-icon icon-angle-right" @click="toggle"></i>
        </div>
        <div class="status" :style="index === 0 ? 'padding-top: 20px;' : ''" v-else-if="realOpen">
            <p @click="toggle">{{$t('收起')}}</p>
            <i class="bk-icon icon-angle-down" @click="toggle"></i>
        </div>
        <div
            class="card-content"
            v-if="realOpen"
            :class="index === 0 ? 'is-first' : 'is-others'"
            :style="'margin-left:' + cardPosition + 'px;width:' + realWidth + 'px;'"
        >
            <bk-button class="add-button" theme="primary" @click="openChangeRules">{{$t('配置规则')}}</bk-button>
            <i class="bk-icon icon-close" @click="toggle"></i>
            <bk-table
                ref="rulesTable"
                class="rules-table"
                :data="tableData"
                :height="elHeight"
                :size="size"
                :cell-class-name="cellStyle"
                :toggle-row-selection="select"
                @select="select"
                @select-all="select"
                @selection-change="select"
            >
                <bk-table-column type="selection" width="60" align="center"></bk-table-column>
                <bk-table-column type="index" :label="$t('序号')" align="center" width="60"></bk-table-column>
                <bk-table-column :sortable="true" :label="$t('规则')" prop="checkerKey" width="200">
                    <template slot-scope="props"> <span :title="props.row.checkerKey">{{ props.row.checkerKey }}</span></template>
                </bk-table-column>
                <bk-table-column :label="$t('规则类型')" prop="checkerType" width="120">
                    <template slot-scope="props"> <span :title="props.row.checkerType">{{ props.row.checkerType }}</span></template>
                </bk-table-column>
                <bk-table-column :label="$t('语言')" prop="language" width="120">
                    <template slot-scope="props"> <span :title="formatLang(props.row.language)">{{ formatLang(props.row.language) || '--' }}</span></template>
                </bk-table-column>
                <bk-table-column :sort-method="sortSeverity" :sortable="true" :label="$t('级别')" prop="severity" width="80"></bk-table-column>
                <bk-table-column :label="$t('详细说明')" prop="checkerDesc">
                    <template slot-scope="props">
                        <span :title="props.row.checkerDesc" :class="{ 'edit-checker-true': props.row.editable }">{{ props.row.checkerDesc }}</span>
                        <bk-button :text="true" v-if="props.row.editable" @click="showUpdateParameter(props.row.checkerKey)">{{ $t('修改参数') }}</bk-button>
                    </template>
                </bk-table-column>
                <bk-table-column width="90px">
                    <template slot-scope="props">
                        <bk-button :text="true" v-if="hasDetail" @click="showDetail(props.row)">{{ $t('详情') }}</bk-button>
                    </template>
                </bk-table-column>
                <div slot="empty">
                    <div class="codecc-table-empty-text">
                        <img src="../../images/empty.png" class="empty-img">
                        <div>{{$t('暂无规则')}}</div>
                    </div>
                </div>
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
        <bk-dialog
            v-model="detailVisiable"
            width="800"
            :theme="'primary'"
            :mask-close="false"
            :title="detailTitle"
            @after-leave="cancel"
        >
            <div class="detail-content"></div>
            <div slot="footer" class="detail-footer">
                <bk-button
                    theme="primary"
                    type="button"
                    @click.native="detailVisiable = false"
                >
                    {{$t('关闭')}}
                </bk-button>
            </div>
        </bk-dialog>
        <bk-dialog v-model="updateVisiable"
            width="533px"
            :mask-close="false"
            @confirm="updateParameter"
            theme="primary">
            <div class="update-parameter-title">
                <span>{{$t('修改规则x参数', { name: ruleData.checkerName })}}</span>
            </div>
            <div class="update-parameter-body">
                <bk-form :label-width="120">
                    <bk-form-item :label="$t('语言')">{{formatLang(ruleData.language)}}</bk-form-item>
                    <bk-form-item :label="$t('工具')">{{ruleData.toolName}}</bk-form-item>
                    <bk-form-item :label="$t('类型')">{{ruleData.checkerType}}</bk-form-item>
                    <bk-form-item :label="$t('描述')">{{ruleData.checkerDesc}}</bk-form-item>
                    <bk-form-item :label="$t('参数修改')">n=
                        <bk-input style="width: 69px" v-model="parameter"></bk-input>
                    </bk-form-item>
                </bk-form>
            </div>
        </bk-dialog>
    </div>
</template>
<script>
    import { mapState } from 'vuex'
    import pako from 'pako'
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
                detailVisiable: false,
                open: 0,
                close: 0,
                switcher: false,
                onFocusItem: '',
                editTitle: '',
                detailTitle: '',
                realWidth: 1080,
                selected: false,
                elHeight: 292,
                operateText: '',
                updateVisiable: false,
                ruleData: {},
                parameter: '',
                iconMap: {
                    LOGICAL: 'logical',
                    BEST_PRACTICES: 'bestpractices',
                    DEFAULT: 'default',
                    STYLISTIC: 'stylistic',
                    ES6: 'es6',
                    NODE: 'node',
                    VARIABLE: 'variable',
                    STRICT_MODE: 'strictmode',
                    SECURITY: 'security',
                    MEMORY: 'memory',
                    PERFORMANCE: 'performance',
                    SYS_API: 'sys_api',
                    EXPRESSION: 'expression',
                    KING_KONG: 'security'
                },
                detailContent: '',
                isloading: false
            }
        },
        computed: {
            ...mapState([
                'toolMeta'
            ]),
            tableData () {
                const tableData = []
                if (this.data) {
                    Object.assign(tableData, this.data.checkerList)
                }
                tableData.map(item => {
                    if (item.severity === 1) {
                        item.severity = this.$t('严重')
                    } else if (item.severity === 2) {
                        item.severity = this.$t('一般')
                    } else if (item.severity === 3) {
                        item.severity = this.$t('提示')
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
                return this.data.pkgDesc || ''
            },
            iconName () {
                return this.iconMap[this.data.pkgName] || 'default'
            },
            total () {
                return this.data.totalCheckerNum || 0
            },
            isSelected () {
                const isSelected = this.data.openCheckerNum
                this.switcher = !!isSelected
                return isSelected
            },
            cardPosition () {
                let cardPosition = 0
                if (this.index) {
                    cardPosition = (this.index - 1) % Math.floor(this.realWidth / 210)
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
                        const childrens = this.$refs.rulesTable.$children
                        if (this.$refs.rulesTable.$el.parentNode.clientHeight > 292) {
                            height = childrens[childrens.length - 1].$children.length > 7 ? 292 : (childrens[childrens.length - 1].$children.length + 1) * 42.1
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
            },
            hasDetail () {
                return ['COVERITY', 'KLOCWORK', 'PINPOINT'].includes(this.$route.params.toolId)
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
                    this.editTitle = this.$t('关闭x条规则', { num: this.close })
                    this.operateText = this.$t('规则减少可能会让扫描错过一些缺陷，增大代码质量风险。')
                } else {
                    this.editTitle = this.$t('启用x条规则', { num: this.open })
                    this.operateText = this.$t('你的代码将接受更细致的扫描，问题误报率也会有所上升。')
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
                const { taskId, toolId } = this.$route.params
                const postData = {
                    pkgId: this.data.pkgId,
                    toolName: toolId,
                    taskId,
                    openedCheckers,
                    closedCheckers
                }
                this.$store.dispatch('tool/changeRules', postData).then(res => {
                    if (res === true) {
                        this.$bkMessage({ theme: 'success', message: this.$t('配置成功') })
                        
                        this.$store.dispatch('tool/rules', { taskId, toolName: toolId })
                    }
                }).catch(e => {
                    this.$bkMessage({ theme: 'error', message: this.$t('配置失败') })
                }).finally(() => {
                    this.operateText = ''
                    this.switcher = this.isSelected !== 0
                })
            },
            cellStyle ({ row, column, rowIndex, columnIndex }) {
                if (column.property === 'severity') {
                    if (row.severity === this.$t('严重')) {
                        return 'majorColor'
                    } else if (row.severity === this.$t('一般')) {
                        return 'minorColor'
                    } else if (row.severity === this.$t('提示')) {
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
                    this.$bkMessage({ theme: 'error', message: this.$t('规则不能为空') })
                    return
                }
                const { taskId, toolId } = this.$route.params
                const postData = {
                    pkgId: this.data.pkgId,
                    toolName: toolId,
                    taskId,
                    openedCheckers,
                    closedCheckers
                }
                this.$store.dispatch('tool/changeRules', postData).then(res => {
                    if (res === true) {
                        this.$bkMessage({ theme: 'success', message: this.$t('配置成功') })
                        this.$store.dispatch('tool/rules', { taskId, toolName: toolId })
                    }
                }).catch(e => {
                    this.$bkMessage({ theme: 'error', message: this.$t('配置失败') })
                })
            },
            position (i) {
                setTimeout(() => {
                    if (this.$refs.rulesTable) {
                        const childrens = this.$refs.rulesTable.$children
                        const validChildrenlList = childrens[childrens.length - 1].$children.filter(child => child.$el.classList[0] === 'bk-form-checkbox')
                        if (this.onFocusItem) {
                            validChildrenlList[this.onFocusItem].$el.parentNode.parentNode.parentNode.style.background = ''
                        }
                        this.onFocusItem = i
                        childrens[childrens.length - 1].$el.parentNode.scrollTop = i * 42
                        validChildrenlList[i].$el.parentNode.parentNode.parentNode.style.background = '#f2f2f2'
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
                        this.editTitle = this.$t('关闭x条规则', { num: this.close })
                        this.operateText = this.$t('规则减少可能会让扫描错过一些缺陷，增大代码质量风险。')
                    } else if (this.close === 0 && this.open !== 0) {
                        this.editTitle = this.$t('启用x条规则', { num: this.open })
                        this.operateText = this.$t('你的代码将接受更细致的扫描，问题误报率也会有所上升。')
                    } else if (this.open !== 0 && this.close !== 0) {
                        this.editTitle = this.$t('启用x条规则', { num: this.open }) + ',' + this.$t('关闭x条规则', { num: this.close })
                        this.operateText = this.$t('你的代码将接受更细致的扫描，问题误报率也会有所上升。') + this.$t('加油，对代码质量永远追求卓越！')
                    } else {
                        this.editTitle = this.$t('没有改动规则')
                    }
                } else {
                    this.editTitle = this.$t('没有改动规则')
                }
                this.singleVisiable = true
            },
            formatLang (num) {
                return this.toolMeta.LANG.map(lang => lang.key & num ? lang.name : '').filter(name => name).join('; ')
            },
            showUpdateParameter (row) {
                this.updateVisiable = true
                this.ruleData = this.tableData.find(rule => rule.checkerKey === row)
                this.parameter = this.ruleData.paramValue
            },
            updateParameter () {
                const { taskId, toolId } = this.$route.params
                const params = {
                    taskId,
                    toolName: toolId,
                    checkerKey: this.ruleData.checkerName,
                    paramValue: this.parameter
                }
                this.$store.dispatch('tool/updateCheckerParam', params).then(res => {
                    if (res === true) {
                        this.$bkMessage({
                            theme: 'success',
                            message: this.$t('修改成功')
                        })
                    }
                }).catch(e => {
                    this.$bkMessage({ theme: 'error', message: this.$t('配置失败') })
                    console.error(e)
                }).finally(() => {
                    const { taskId, toolId } = this.$route.params
                    this.$store.dispatch('tool/rules', { taskId, toolName: toolId })
                })
            },
            showDetail (row) {
                this.detailTitle = row.checkerKey
                if (row.codeExample) {
                    const data = pako.ungzip(row.codeExample, { to: 'String' })
                    document.getElementsByClassName('detail-content')[this.index].innerHTML = this.Utf8ArrayToStr(data)
                }
                
                this.detailVisiable = true
            },
            Utf8ArrayToStr (array) {
                let out = ''
                let c = ''
                let char2 = ''
                let char3 = ''
                const len = array.length
                let i = 0
                while (i < len) {
                    c = array[i++]
                    switch (c >> 4) {
                        case 0: case 1: case 2: case 3: case 4: case 5: case 6: case 7:
                            // 0xxxxxxx
                            out += String.fromCharCode(c)
                            break
                        case 12: case 13:
                            // 110x xxxx   10xx xxxx
                            char2 = array[i++]
                            out += String.fromCharCode(((c & 0x1F) << 6) | (char2 & 0x3F))
                            break
                        case 14:
                            // 1110 xxxx  10xx xxxx  10xx xxxx
                            char2 = array[i++]
                            char3 = array[i++]
                            out += String.fromCharCode(((c & 0x0F) << 12) | ((char2 & 0x3F) << 6) | ((char3 & 0x3F) << 0))
                            break
                    }
                }
                return out
            },
            sortSeverity (a, b) {
                const obj = {
                    '提示': 3,
                    '一般': 2,
                    '严重': 1
                }
                return obj[b.severity] - obj[a.severity]
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
        border-radius: 10px;
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
                .edit-checker-true {
                    display: inline-block;
                    max-width: calc(100% - 70px);
                    overflow: hidden;
                    white-space: nowrap;
                    text-overflow: ellipsis;
                    line-height: 14px;
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
        .tool-icon {
            padding-right: 10px;
        }
    }
    .card.active {
        border: 1px solid $goingColor;
    }
    .center {
        text-align: center;
    }
    .update-parameter-title {
        width: 470px;
        overflow: hidden;
        position: relative;
        top: -20px;
        font-size: 20px;
    }
    .detail-footer {
        text-align: center;
    }
    .detail-content {
        background-color: #f6f9fa;
        padding: 0 10px;
        overflow: auto;
        height: 350px;
    }
</style>

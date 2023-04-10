<template>
    <bk-dialog v-model="nodeSelectConf.isShow"
        :width="'900'"
        :ext-cls="'node-select-wrapper'"
        :close-icon="false">
        <div
            v-bkloading="{
                isLoading: loading.isLoading,
                title: loading.title
            }">
            <div class="node-list-header">
                <div class="title">{{ $t('environment.nodeInfo.selectNodeTip') }}
                    <span class="selected-node-prompt">
                        {{ $t('environment.nodeInfo.selectNodeTip') }}<span class="node-count"> {{ selectHandlercConf.curTotalCount }} </span>{{ $t('environment.nodes') }}
                    </span>
                    <span class="selected-node-prompt">
                        {{ $t('environment.selected') }}<span class="node-count"> {{ selectHandlercConf.selectedNodeCount }} </span>{{ $t('environment.nodes') }}
                    </span>
                </div>
                <div class="search-input-row">
                    <div class="biz-search-input">
                        <div class="biz-ip-searcher-wrapper">
                            <div class="biz-searcher" @click="focusSearch" ref="bizSearcher">
                                <ul class="search-key" ref="searchKey">
                                    <li class="key-node" v-for="(entry, index) in searchKeyList" :key="index">
                                        <span>{{ entry }}</span>
                                        <i class="devops-icon icon-close" @click="deleteKey(index)"></i>
                                    </li>
                                    <li class="input-item">
                                        <input type="text" class="search-input" ref="searchInput"
                                            v-model="inputValue"
                                            :style="inputStyle"
                                            @blur="handleBlur"
                                            @paste="paste"
                                            @keyup="keyupHandler">
                                    </li>
                                </ul>
                            </div>
                            <div class="actions">
                                <i class="devops-icon icon-close" @click="deleteAllKey" v-if="searchKeyList.length"></i>
                                <i class="devops-icon icon-search" @click="searchNode"></i>
                            </div>
                            <div class="ip-searcher-footer" v-if="isSearchFooter">
                                <p>{{ $t('environment.nodeInfo.searchNodePlaceholder') }}</p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            <div class="node-table">
                <div class="node-table-message" v-if="!selectHandlercConf.searchEmpty && rowList.length">
                    <div class="table-node-head">
                        <bk-checkbox
                            :true-value="true"
                            :false-value="false"
                            v-model="selectHandlercConf.allNodeSelected"
                            @change="toggleAllSelect"
                        ></bk-checkbox>
                        <div class="table-node-item node-item-ip">IP</div>
                        <div class="table-node-item node-item-displayname">{{ $t('environment.nodeInfo.displayName') }}</div>
                        <div class="table-node-item node-item-name">{{ $t('environment.nodeInfo.cpuName') }}</div>
                        <div class="table-node-item node-item-type">{{ `${$t('environment.nodeInfo.source')}/${$t('environment.nodeInfo.importer')}` }}</div>
                        <div class="table-node-item node-item-status">{{ $t('environment.nodeInfo.cpuStatus') }}</div>
                        <div class="table-node-item node-item-agstatus">
                            <span>{{ $t('environment.nodeInfo.gateway') }}</span>
                        </div>
                    </div>
                    <div class="table-node-body">
                        <div class="table-node-row" v-for="(col, index) of rowList" :key="index" v-if="col.isDisplay">
                            <div class="table-node-item node-item-checkbox">
                                <bk-checkbox
                                    :true-value="true"
                                    :false-value="false"
                                    :disabled="col.isEixtEnvNode"
                                    v-model="col.isChecked"
                                ></bk-checkbox>
                            </div>
                            <div class="table-node-item node-item-ip">
                                <span class="node-ip">{{ col.ip }}</span>
                            </div>
                            <div class="table-node-item node-item-name" :class="{ 'over-content': selectHandlercConf.curDisplayCount > 6 }">
                                <span class="node-name">{{ col.name }}</span>
                            </div>
                            <div class="table-node-item node-item-displayname">
                                <span class="node-displayname">{{ col.displayName }}</span>
                            </div>
                            <div class="table-node-item node-item-type" :class="{ 'over-content': selectHandlercConf.curDisplayCount > 6 }">
                                <div>
                                    <span class="node-name">{{ $t('environment.nodeTypeMap')[col.nodeType] }}</span>
                                    <span>({{ col.createdUser }})</span>
                                </div>
                            </div>
                            <div class="table-node-item node-item-status">
                                <span class="node-status" :class="{ 'over-content': selectHandlercConf.curDisplayCount > 6 }">{{ $t('environment.nodeStatusMap')[col.nodeStatus] }}</span>
                            </div>
                            <div class="table-node-item node-item-agstatus" :class="{ 'over-content': selectHandlercConf.curDisplayCount > 6 }">
                                <span>{{ col.gateway }}</span>
                            </div>
                        </div>
                    </div>
                </div>
                <bk-exception
                    v-if="selectHandlercConf.searchEmpty || !rowList.length"
                    class="exception-wrap-item exception-part"
                    search-empty
                    :type="selectHandlercConf.searchEmpty ? 'search-empty' : 'empty'"
                    scene="part"
                />
            </div>
        </div>
        <div slot="footer">
            <div class="footer-handler">
                <bk-button theme="primary" @click="confirmFn" :disabled="nodeSelectConf.unselected">{{ nodeSelectConf.importText }}</bk-button>
                <bk-button theme="default" @click="cancelFn">{{ $t('environment.cancel') }}</bk-button>
            </div>
        </div>
    </bk-dialog>
</template>

<script>
    export default {
        props: {
            nodeSelectConf: Object,
            loading: Object,
            curUserInfo: Object,
            selectHandlercConf: Object,
            rowList: Array,
            confirmFn: Function,
            toggleAllSelect: Function,
            cancelFn: Function,
            query: Function,
            searchInfo: {
                type: Object,
                default: {
                    data: [],
                    onChange: () => {}
                }
            }
        },
        data () {
            return {
                isSearchFooter: false,
                inputValue: '',
                searchKeyList: []
            }
        },
        computed: {
            // 动态计算输入长度
            inputStyle () {
                const tag = this.inputValue
                const charLen = this.getCharLength(tag) + 1
                return { width: charLen * 8 + 'px' }
            },
            hasConstruct () {
                return this.rowList.some(row => {
                    return row.nodeType === 'THIRDPARTY' && row.isDisplay
                })
            }
        },
        watch: {
            'nodeSelectConf.isShow' (val) {
                if (!val) {
                    this.inputValue = ''
                    this.searchKeyList.splice(0, this.searchKeyList.length)
                }
            }
        },
        methods: {
            // 获取字符长度，汉字两个字节
            getCharLength (str) {
                const len = str.length
                let bitLen = 0
                for (let i = 0; i < len; i++) {
                    if ((str.charCodeAt(i) & 0xff00) !== 0) {
                        bitLen++
                    }
                    bitLen++
                }
                return bitLen
            },
            handleFocus () {
                this.isSearchFooter = !this.isSearchFooter
            },
            handleBlur () {
                this.isSearchFooter = false
            },
            focusSearch () {
                if (!this.isSearchFooter) {
                    this.isSearchFooter = true
                }
                this.$refs.searchInput.focus()
            },
            deleteAllKey () {
                this.searchKeyList.splice(0, this.searchKeyList.length)
                this.inputValue = ''
            },
            paste (event) {
                const value = event.clipboardData.getData('text')
                let valParams = value.replace(/[\r\n]/g, ' ')
                valParams = valParams.replace(/\t/g, ' ')
                let target = valParams.split(' ')
                target = target.filter(item => {
                    return item
                })
                if (target.length) {
                    this.searchKeyList.push(...target)
                    this.inputValue = ''
                    setTimeout(() => {
                        this.inputValue = ''
                        this.$nextTick(() => {
                            this.$refs.bizSearcher.scrollTo(this.$refs.bizSearcher.offsetWidth + this.$refs.bizSearcher.scrollWidth, 0)
                        })
                    }, 10)
                }
            },
            selectedKey (val) {
                if (val && /\s*\S+/.test(val)) {
                    const target = val.replace(/(^\s*)|(\s*$)/g, '')
                    this.searchKeyList.push(target)
                    this.inputValue = ''

                    this.$nextTick(() => {
                        this.$refs.bizSearcher.scrollTo(this.$refs.bizSearcher.offsetWidth + this.$refs.bizSearcher.scrollWidth, 0)
                    })
                }
            },
            searchNode () {
                this.selectedKey(this.inputValue)
                this.query(this.searchKeyList)
                this.isSearchFooter = false
            },
            keyupHandler (event) {
                switch (event.code) {
                    case 'Space':
                        this.selectedKey(this.inputValue)

                        break
                    case 'Enter':
                    case 'NumpadEnter':
                        this.searchNode()
                        break
                    case 'Backspace':
                        if (!this.inputValue) {
                            this.searchKeyList.pop()
                        }
                        break
                    default:
                        break
                }
            },
            deleteKey (index) {
                this.searchKeyList.splice(index, 1)
            }
        }
    }
</script>

<style lang="scss">
    @import './../../../scss/conf';

    %flex {
        display: flex;
        align-items: center;
    }

    .node-select-wrapper {

        .bk-dialog-tool {
            display: none;
        }

        .bk-dialog-body {
            padding-left: 0;
            padding-right: 0;
        }

        .node-list-header {
            padding: 20px;
            height: 58px;
            display: flex;
            justify-content: space-between;

            .title {
                line-height: 16px;
                color: $fontWeightColor;

                .icon-info-circle {
                    position: relative;
                    top: 2px;
                    margin-left: 4px;
                }
            }

            .selected-node-prompt {
                margin-left: 6px;
                font-size: 12px;
            }

            .node-count {
                color: $failColor;
            }

            .search-input-row {
                position: absolute;
                top: 10px;
                right: 20px;
            }

            .search-tool-row {
                position: relative;
                top: -8px;
            }

            .biz-search-input {
                position: relative;
                display: inline-block;
                width: 320px;
            }

            .biz-ip-searcher-wrapper {
                position: relative;
                width: 100%;
                border: 1px solid #dde4eb;
                background-color: #fff;
                border-radius: 2px;

                .search-key {
                    height: 100%;
                    line-height: 1;

                    li {
                        display: inline-block;
                        margin: -1px 3px;
                        cursor: pointer;
                        position: relative;
                        padding: 2px;
                        border-radius: 2px;
                        // height: 27px;
                        line-height: 1;

                        input {
                            width: 20px;
                            padding: 0;
                            border: 0;
                            -webkit-box-shadow: border-box;
                            box-shadow: border-box;
                            outline: none;
                            max-width: 150px;
                            height: 36px;
                            margin: -3px 3px;
                            margin-left: 0;
                        }
                    }

                    .key-node {
                        background: #ebf4ff;

                        span {
                            display: inline-block;
                            background-color: #ebf4ff;
                            color: #7b7d8a;
                            font-size: 12px;
                            border: none;
                            vertical-align: middle;
                            -webkit-box-sizing: border-box;
                            box-sizing: border-box;
                            overflow: hidden;
                            border-radius: 2px;
                            padding: 0 9px;
                            min-height: 21px;
                            line-height: 22px;
                            word-break: break-all;
                        }

                        i {
                            position: relative;
                            top: 2px;
                            right: 6px;
                        }
                    }
                }

                .biz-searcher::-webkit-scrollbar {
                    display: none;
                }

                .actions {
                    position: absolute;
                    right: 10px;
                    top: 2px;
                    color: #c4ced8;
                    z-index: 10;
                    height: 36px;
                    line-height: 36px;

                    .devops-icon {
                        cursor: pointer;
                    }

                    .icon-close {
                        margin-right: 4px;
                        font-size: 14px;
                    }
                }
            }

            .biz-searcher {
                width: 80%;
                height: 36px;
                border-radius: 2px;
                font-size: 12px;
                position: relative;
                z-index: 1;
                background: #fff;
                cursor: pointer;
                white-space: nowrap;
                margin: 0 5px;
                overflow: hidden;
                overflow-x: scroll;
            }

            .ip-searcher-footer {
                padding-left: 8px;
                position: relative;
                left: -1px;
                top: 2px;
                width: 320px;
                height: 36px;
                line-height: 36px;
                border: 1px solid #dde4eb;
                border-top: none;
                background-color: #fff;
                color: #c3cdd7;
                font-size: 12px;

                p {
                    text-align: left;
                }
            }
        }

        .node-table {
            height: 294px;
            margin: 0;
            border: none;
            .table-node-item {
                flex-shrink: 0;
            }
        }

        .table-node-body {
            height: 252px;
            overflow: auto;
        }

        .table-node-head,
        .table-node-row {
            padding: 0 20px;
            @extend %flex;
            height: 42px;
            border-top: 1px solid $borderWeightColor;
            color: #333C48;
            font-size: 12px;
        }

        .table-node-row {
            color: $fontWeightColor;
            font-weight: normal;
            cursor: pointer;
        }

        .node-item-name,
        .node-item-displayname {
            flex: 5;
        }

        .node-item-ip,
        .node-item-status,
        .node-item-type,
        .node-item-agstatus,
        .node-item-operator {
            flex: 2;
        }

        .node-item-type {
            flex: 4;
            width: 200px;
        }
        .node-item-agstatus {
            flex: 1;
            min-width: 82px;
        }

        .prompt-operator,
        .edit-operator {
            padding-right: 10px;
            color: #ffbf00;

            .devops-icon {
                margin-right: 6px;
            }
        }

        .edit-operator {
            cursor: pointer;
        }

        .over-content {
            padding-left: 6px;
        }

        .bk-form-checkbox {
            margin-right: 12px;
        }

        .node-bkOperator {
            max-width: 130px;
            text-align: left;
            overflow: hidden;
            text-overflow: ellipsis;
        }

        .checkbox-all {
            padding-top: 0;

            &:before {
                content: '';
                display: block;
                width: 8px;
                height: 8px;
                position: relative;
                top: 14px;
                left: 5px;
                background-color: $lineColor;
            }
        }

        .all-checked {
            &:before {
                display: none;
            }
        }

        .normal-status-node {
            color: #30D878;
        }

        .abnormal-status-node {
            color: $failColor;
        }

        .refresh-status-node {
            color: $primaryColor;
        }

        .footer-handler {
            // text-align: right;

            .bk-button {
                height: 32px;
                line-height: 32px;
            }
        }
    }
</style>

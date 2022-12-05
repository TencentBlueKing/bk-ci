<template>
    <bk-dialog v-model="shareSelectConf.isShow"
        :width="'1000'"
        :ext-cls="'node-select-wrapper'"
        :position="{ top: 150 }"
        :close-icon="false">
        <div
            v-bkloading="{
                isLoading: loading.isLoading,
                title: loading.title
            }">
            <div class="node-list-header">
                <div class="title">{{$t('select')}}{{$t(selectType.toLowerCase())}}
                </div>
                <div class="search-input-row" v-if="selectType === 'PROJECT'">
                    <div class="biz-search-input">
                        <bk-input type="text" class="search-input" ref="searchInput"
                            right-icon="bk-icon icon-search"
                            v-model="inputValue"
                            @enter="search" />
                    </div>
                </div>
            </div>
            <div class="node-table">
                <section v-if="!shareHandlerConf.searchEmpty && rowList.length">
                    <div class="node-table-message">
                        <div class="table-node-head">
                            <div class="table-node-item node-item-status">
                                <bk-checkbox
                                    :true-value="true"
                                    :false-value="false"
                                    v-model="shareHandlerConf.allSelected"
                                    @change="toggleAllSelect"
                                ></bk-checkbox>
                            </div>
                            <div class="table-node-item node-item-ip">{{ selectType === 'PROJECT' ? 'Id' : 'Group Id'}}</div>
                            <div class="table-node-item node-item-name">{{ selectType === 'PROJECT' ? $t('projectName') : $t('groupName')}}</div>
                        </div>
                        <div class="table-node-body">
                            <template v-for="(col, index) of rowList">
                                <div class="table-node-row" :key="index">
                                    <div class="table-node-item node-item-status">
                                        <bk-checkbox
                                            :true-value="true"
                                            :false-value="false"
                                            :disabled="col.isEixt"
                                            v-model="col.isChecked"
                                            @change="val => toggleItemSelect(val, col.id)"
                                        ></bk-checkbox>
                                    </div>
                                    <div class="table-node-item node-item-ip">
                                        <span class="node-ip">{{ col.id }}</span>
                                    </div>
                                    <div class="table-node-item node-item-name" :class="{ 'over-content': shareHandlerConf.curDisplayCount > 6 }">
                                        <span class="node-name">{{ col.name }}</span>
                                    </div>
                                </div>
                            </template>
                        </div>
                    </div>
                </section>
                <div class="no-data-row" v-if="shareHandlerConf.searchEmpty || !rowList.length">
                    <span>No {{selectType.toLowerCase()}}</span>
                </div>
                <ul class="bk-page-list">
                    <!-- 上一页 -->
                    <li class="page-item" :class="{ disabled: pageConfig.page === 1 }" @click="changePage('pre')">
                        <a href="javascript:void(0);" class="page-button">
                            <i class="bk-icon icon-angle-left"></i>
                        </a>
                    </li>
                    <!-- 第一页 -->
                    <li class="page-item">
                        <a href="javascript:void(0);" class="page-button">{{ pageConfig.page }}</a>
                    </li>
                    <li class="page-item" :class="{ disabled: !pageConfig.hasNext }" @click="changePage('next')">
                        <a href="javascript:void(0);" class="page-button">
                            <i class="bk-icon icon-angle-right"></i>
                        </a>
                    </li>
                </ul>
            </div>
        </div>
        <div slot="footer">
            <div class="footer-handler">
                <bk-button theme="primary" @click="confirmFn" :disabled="shareSelectConf.unselected">{{$t('confirm')}}</bk-button>
                <bk-button theme="default" @click="cancelFn">{{$t('cancel')}}</bk-button>
            </div>
        </div>
    </bk-dialog>
</template>

<script>
    export default {
        props: {
            selectType: String,
            shareSelectConf: Object,
            pageConfig: Object,
            loading: Object,
            shareHandlerConf: Object,
            rowList: Array,
            totalList: Array,
            updatePage: Function,
            confirmFn: Function,
            toggleAllSelect: Function,
            cancelFn: Function,
            query: Function
        },
        data () {
            return {
                inputValue: ''
            }
        },
        watch: {
            'shareSelectConf.isShow' (val) {
                if (!val) {
                    this.inputValue = ''
                }
            }
        },
        methods: {
            search () {
                this.query(this.inputValue)
            },
            changePage (type) {
                if (type === 'pre') {
                    if (this.pageConfig.page !== 1) {
                        this.updatePage(this.pageConfig.page - 1)
                    }
                } else {
                    if (this.pageConfig.hasNext) {
                        this.updatePage(this.pageConfig.page + 1)
                    }
                }
            },
            toggleItemSelect (val, id) {
                const selectItem = this.totalList.find(item => item.id === id)
                selectItem.isChecked = val
            }
        }
    }
</script>

<style lang="postcss">
    @import '@/css/conf';

    .bk-page-list {
        position: absolute;
        bottom: 10px;
        right: 20px;
        margin: 0;
        padding: 0;
        overflow: hidden;
        font-size: 0;
        display: inline-block;
        vertical-align: middle;
    }
    .page-item {
        text-align: center;
        display: inline-block;
        vertical-align: middle;
        font-size: 12px;
        margin-right: 4px;
        box-sizing: border-box;
        border-radius: 2px;
        overflow: hidden;
        &.page-omit {
            border: none;
            min-width: auto;
            > span {
                display: inline-block;
            }
        }
        .page-button {
            display: block;
            min-width: 30px;
            height: 30px;
            padding: 0 4px;
            line-height: 28px;
            border: 1px solid #c4c6cc;
            color: #63656e;
            background: #fff;
            cursor: pointer;
            &:hover {
                color: $primaryColor;
                border-color: $primaryColor;
            }
        }

        &.cur-page {
            .page-button {
                border-color: $primaryColor;
                color: $primaryColor;
            }
        }
        &.disabled {
            .page-button {
                border-color: #dcdee5;
                cursor: not-allowed;
                color: #dcdee5;
                background-color: #fafbfd;
                &:hover {
                    color: #dcdee5;
                }
            }
        }
        &:last-child {
            margin-right: 0;
        }
    }

    %flex {
        display: flex;
        align-items: center;
    }

    .node-select-wrapper {

        .bk-dialog-tool {
            display: none;
        }

        .bk-dialog-body {
            padding: 0;
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

            .biz-search-input {
                position: relative;
                display: inline-block;
                width: 320px;
            }
        }

        .node-table {
            height: 410px;
            margin: 0;
            border: none;
        }

        .no-data-row {
            padding-top: 40px;
            text-align: center;
            border-top: 1px solid $borderWeightColor;
        }

        .table-node-body {
            height: 317px;
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

        .node-item-name {
            flex: 3;
        }

        .node-item-ip
        {
            flex: 2;
        }

        .node-item-status {
            flex: 1;
            min-width: 82px;
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

        .footer-handler {
            .bk-button {
                height: 32px;
                line-height: 32px;
            }
        }
    }
</style>

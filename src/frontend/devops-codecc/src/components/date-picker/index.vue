<template>
    <div class="date-picker-dropdown" v-bk-clickoutside="hiddenDropdown"
    >
        <bk-button type="primary" class="select-button" @click="toggleShow">
            <div class="data-range-input"
                :class="{ 'unselect': !localVal }"
                :title="localVal"
            >
                {{localVal ? localVal : $t('请选择')}}
            </div>
            <i :class="['bk-icon icon-angle-down', { 'icon-flip': visable }]"></i>
        </bk-button>
        <div class="picker-dropdown-content" v-if="visable">
            <div class="bk-button-group">
                <bk-button v-for="(item, index) in filterTypeList" :key="index"
                    :class="selected === item.key ? 'is-selected' : ''"
                    @click="selectType(item.key)"
                >{{ item.name }}</bk-button>
            </div>
            <bk-date-picker class="" v-model="localDaterange" type="daterange"></bk-date-picker>
            <div class="content-ft">
                <bk-button theme="primary" @click="handleConfirm">{{$t('确定')}}</bk-button>
                <bk-button @click="hiddenDropdown">{{$t('取消')}}</bk-button>
                <bk-button class="clear-btn" @click="handleClear">{{$t('清空选择')}}</bk-button>
            </div>
        </div>
    </div>
</template>

<script>
    import { format } from 'date-fns'

    export default {
        props: {
            dateRange: {
                type: String,
                default: ''
            },
            selected: {
                type: String,
                default: 'createTime'
            },
            handleChange: Function
        },
        data () {
            return {
                localDaterange: [],
                visable: false,
                filterTypeList: [
                    { key: 'createTime', name: '创建日期' },
                    { key: 'fixTime', name: '修复日期' }
                ]
            }
        },
        computed: {
            localVal () {
                let result = ''
                if (this.dateRange.length && Object.keys(this.dateRange).every(item => this.dateRange[item])) {
                    result = `${this.selected === 'createTime' ? '创建日期' : '修复日期'}：${this.dateRange.join(' - ')}`
                }
                return result
            }
        },
        methods: {
            toggleShow () {
                this.visable = !this.visable
            },
            hiddenDropdown () {
                this.visable = false
            },
            selectType (type) {
                this.selected = type
                this.localDaterange = []
            },
            handleConfirm () {
                if (Object.keys(this.localDaterange).every(item => this.localDaterange[item])) {
                    const startCreateTime = this.formatTime(this.localDaterange[0], 'YYYY-MM-DD')
                    const endCreateTime = this.formatTime(this.localDaterange[1], 'YYYY-MM-DD')
                    const target = [startCreateTime, endCreateTime]
                    this.handleChange(target, this.selected)
                } else {
                    this.handleChange([], this.selected)
                }
                this.hiddenDropdown()
            },
            handleClear () {
                this.localDaterange = []
                this.handleChange([], this.selected)
                this.hiddenDropdown()
            },
            formatTime (date, token, options = {}) {
                return date ? format(Number(date), token, options) : ''
            }
        }
    }
</script>

<style lang="postcss">
    @import '../../css/mixins.css';

    .date-picker-dropdown {
        position: absolute;
        width: 100%;
        z-index: 99;
        border-radius: 2px;
        .picker-dropdown-content {
            margin-top: 3px;
            width: 360px;
            border-radius: 2px;
            border: 1px solid #dcdee5;
            box-shadow: 0 2px 6px rgba(51,60,72,.1);
            background-color: #fff;
        }
        .select-button {
            width: 100%;
        }
        .data-range-input {
            text-align: left;
            display: inline-block;
            float: left;
            font-size: 12px;
            @mixin ellipsis;
            &+.bk-icon {
                position: absolute;
                top: 7px;
                right: 0;
            }
            &.unselect {
                color: #c3cdd7;
                font-size: 12px;
                margin-left: -4px;
            }
        }
        .icon-flip {
            transform: rotate(180deg);
            transition: all .2s ease;
        }
        .bk-date-picker {
            margin: 0 20px 20px;
        }
        .bk-button-group {
            margin: 20px 20px;
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
    }
</style>

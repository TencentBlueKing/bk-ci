<template>
    <div class="view-manage-wrapper">
        <div class="commonly-view-list">
            <i class="bk-icon icon-close close-view-manage" @click='closeViewManage()'></i>
            <p>我的常用视图<span>（<span>{{ commonlyViews.length }}</span>/7）</span></p>
            <ul class="view-card-list" ref='viewList'>
                <draggable v-model='commonlyViews' :options='dragOptions'>
                    <li class="view-card" v-for='(entry, index) in commonlyViews' :key='index'>
                        <span>{{ entry.name }}</span>
                        <i class="delete-btn" v-if='commonlyViews.length > 1' @click.stop='cancelHandler(index)'></i>
                    </li>
                </draggable>
            </ul>
        </div>
        <div class="manage-view-content">
            <ul class="manage-view-list">
                <li class="view-item" v-for="(entry, index) in viewList" :key="index">
                    <h3>{{ entry.label }}<span class="add-view-btn" v-if="index === 2">添加新视图</span></h3>
                    <div class="view-box" v-for='(view, viewIndex) in entry.group' :key='viewIndex'>
                        <span class="view-title">{{ view.name }}</span>
                        <i class="bk-icon icon-plus-square" :class="{ 'is-selected': isIncludeCommonly(view.name) || commonlyViews.length === 7 }"
                            @click='addHandler(view)'></i>
                    </div>
                </li>
            </ul>
        </div>
    </div>
</template>

<script>
    import draggable from 'vuedraggable'

    export default {
        components: {
            draggable
        },
        data () {
            return {
                viewList: [
                    // {
                    //     label: '系统视图',
                    //     group: [
                    //         { name: '我的收藏' },
                    //         { name: '我的流水线' },
                    //         { name: '所有流水线' }
                    //     ]
                    // },
                    // {
                    //     label: '项目视图',
                    //     group: [
                    //         { name: '视图AAA' },
                    //         { name: '视图BBB' },
                    //         { name: '视图CCC' },
                    //         { name: '视图DDD' }
                    //     ]
                    // },
                    // {
                    //     label: '个人视图',
                    //     group: [
                    //         { name: '蓝鲸蓝盾流水线' },
                    //         { name: '蓝盾流水线' },
                    //         { name: '测试流水线' },
                    //         { name: '流水线视图' },
                    //         { name: '流水线WWW' },
                    //         { name: '流水线XXX' },
                    //         { name: '流水线ZZZ' },
                    //         { name: '流水线YYY' }
                    //     ]
                    // }
                ],
                commonlyViewList: [
                    // { name: '我的收藏' },
                    // { name: '我的流水线' },
                    // { name: '所有流水线' },
                    // { name: '蓝鲸蓝盾流水线' },
                    // { name: '视图AAA' }
                ],
                changeSortViews: []
            }
        },
        computed: {
            ...mapState('pipelines', [
                'currentViewId',
                'currentViewList',
                'viewSettingList'
            ]),
            commonlyViews: {
                get () {
                    return this.commonlyViewList
                },
                set (value) {
                    this.commonlyViewList = value
                }
            },
            dragOptions () {
                return {
                    ghostClass: 'sortable-ghost-view',
                    chosenClass: 'sortable-chosen-view',
                    animation: 200
                }
            }
        },
        methods: {
            isIncludeCommonly (view) {
                return this.commonlyViews.some(item => { return item.name === view })
            },
            addHandler (value) {
                let isExistItem = this.commonlyViews.some(view => { return view.name === value.name })
                if (!isExistItem && this.commonlyViews.length !== 7) {
                    this.commonlyViews.push(value)
                }
            },
            cancelHandler (index) {
                this.commonlyViews.splice(index, 1)
            },
            closeViewManage () {
                this.$store.commit('pipelines/toggleShowViewManage', false)
            }
        },
        created () {
            this.commonlyViewList = this.currentViewList
            this.viewList = this.viewSettingList
        }
    }
</script>

<style lang="scss" scoped>
    @import './../../scss/conf';

    .view-manage-wrapper {
        position: absolute;
        top: 70px;
        right: 29px;
        padding: 20px 30px;
        width: 995px;
        z-index: 1600;
        background-color: #fff;
        box-shadow:0px 3px 6px 0px rgba(0,0,0,0.1);
        cursor: default;
        .close-view-manage {
            position: absolute;
            top: 18px;
            right: 32px;
            font-size: 12px;
            cursor: pointer;
        }
        .commonly-view-list{
            text-align: left;
            p {
                margin-top: 10px;
                color: #333C48;
            }
        }
        .view-card-list {
            display: inline-block;
            margin-top: 20px;
            li {
                margin-right: 12px;
                margin-bottom: 12px;
                padding: 8px;
                line-height: 14px;
                text-align: center;
                border: 1px solid $borderWeightColor;
                border-radius: 2px;
                background-color: #f9fbfc;
                font-size: 14px;
                cursor: pointer;
            }
            .delete-btn {
                display: inline-block;
                width: 10px;
                height: 10px;
                overflow: hidden;
                position: relative;
            }
            .delete-btn::before,
            .delete-btn::after {
                content: "";
                position: absolute;
                top: 50%;
                left: 0;
                margin-top: -1px;
                background-color: #c4c4c4;
                width: 100%;
                height: 2px;
            }
            .delete-btn::before {
                transform: rotate(45deg);
            }
            .delete-btn::after {
                transform: rotate(-45deg);
            }
            .view-card:hover {
                border-color: $primaryColor;
                .delete-btn::before,
                .delete-btn::after {
                    background-color: $primaryColor;
                }
            }
        }
        .manage-view-content {
            display: inline-block;
            text-align: center;
            ul {
                display: inline-block;
            }
            li {
                margin-right: 68px;
                padding: 0 0 10px;
                width: 186px;
            }
            h3 {
                display: flex;
                justify-content: space-between;
                margin-bottom: 10px;
                padding-bottom: 10px;
                border-bottom: 1px solid $borderWeightColor;;
                text-align: left;
                color: #333C48;
                font-weight: normal;
                font-size: 16px;
            }
            .view-box {
                display: flex;
                justify-content: space-between;
                margin-bottom: 6px;
                font-size: 14px;
            }
            .view-title {
                text-align: left;
            }
            .icon-plus-square {
                line-height: 24px;
                font-size: 14px;
                font-weight: bold;
                cursor: pointer;
            }
            .is-selected {
                color: $lineColor;
            }
            .add-view-btn {
                line-height: 21px;
                color: $primaryColor;
                font-size: 14px;
                cursor: pointer;
            }
            .view-item:nth-child(3) {
                margin-right: 0;
                width: 422px;
                .view-box {
                    float: left;
                    margin-right: 50px;
                    width: 186px;
                }
                .view-box:nth-child(odd) {
                    margin-right: 0;
                }
            }
        }
    }

    .sortable-ghost-view {
        opacity: 0.5;
    }
    .sortable-chosen-view {
        transform: scale(1.1);
    }
</style>

<template>
    <div class="view-manage-wrapper">
        <div class="commonly-view-list">
            <p>{{ $t('newlist.defaultViews') }}
                <span>（<span>{{ commonlyViews.length }}</span>/30）</span>
                <bk-button size="small" class="cancen-btn" @click.stop="closeViewManage()">{{ $t('cancel') }}</bk-button>
                <bk-button theme="primary" size="small" class="save-btn" @click.stop="saveViewManage()">{{ $t('save') }}</bk-button>
            </p>
            <ul class="view-card-list" ref="viewList">
                <draggable v-model="commonlyViews" :options="dragOptions">
                    <li class="view-card" v-for="(entry, index) in commonlyViews" :key="index">
                        <span>{{ entry.name }}</span>
                        <i class="delete-btn" v-if="commonlyViews.length > 1" @click.stop="cancelHandler(index)"></i>
                    </li>
                </draggable>
            </ul>
        </div>
        <div class="manage-view-content">
            <ul class="manage-view-list">
                <li class="view-item" v-for="(entry, index) in viewSettingList" :key="index">
                    <h3>{{ entry.label }}<span class="add-view-btn" v-if="index === 2" @click="routeTocreateView()">{{ $t('view.addView') }}</span></h3>
                    <div class="view-box" v-for="(view, viewIndex) in entry.viewList" :key="viewIndex">
                        <span class="view-title">{{ view.name }}</span>
                        <i class="devops-icon icon-plus-square" :class="{ 'is-selected': isIncludeCommonly(view.id) || commonlyViews.length === 30 }"
                            @click="addHandler(view)"></i>
                    </div>
                </li>
            </ul>
        </div>
    </div>
</template>

<script>
    import { mapState } from 'vuex'
    import draggable from 'vuedraggable'

    export default {
        components: {
            draggable
        },
        data () {
            return {
                commonlyViewList: []
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
                    animation: 200
                }
            }
        },
        created () {
            this.commonlyViewList = this.currentViewList.concat()
            this.viewList = this.viewSettingList
        },
        methods: {
            isIncludeCommonly (view) {
                return this.commonlyViews.some(item => {
                    return item.id === view
                })
            },
            addHandler (value) {
                const isExistItem = this.commonlyViews.some(view => {
                    return view.id === value.id
                })
                if (!isExistItem && this.commonlyViews.length !== 30) {
                    this.commonlyViews.push(value)
                }
            },
            routeTocreateView () {
                this.$store.commit('pipelines/toggleShowViewManage', false)
                this.$router.push({ name: 'pipelinesView' })
            },
            cancelHandler (index) {
                this.commonlyViews.splice(index, 1)
            },
            closeViewManage () {
                this.$store.commit('pipelines/toggleShowViewManage', false)
            },
            async saveViewManage () {
                const ids = this.commonlyViews.map(item => item.id)
                try {
                    await this.$store.dispatch('pipelines/updateCurrentViewList', {
                        projectId: this.$route.params.projectId,
                        ids
                    })
                    this.$store.commit('pipelines/updateCurrentViewList', this.commonlyViews)
                    // 如果当前currentViewId被修改，则默认选中第一个
                    if (!this.commonlyViews.filter(item => item.id === this.currentViewId).length) {
                        this.$store.commit('pipelines/updateCurrentViewId', ids[0])
                        this.$router.push({
                            name: 'PipelineManageList',
                            params: {
                                viewId: this.currentViewId
                            }
                        })
                    }
                    this.closeViewManage()
                } catch (err) {
                    this.$showTips({
                        theme: 'error',
                        message: err.message || err
                    })
                }
            }
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
        li {
            float: left;
        }
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
                .save-btn {
                    float: right;
                    margin-right: 10px;
                }
                .cancen-btn {
                    float: right;
                }
            }
        }
        .view-card-list {
            display: inline-block;
            margin-top: 20px;
            li {
                margin-right: 12px;
                margin-bottom: 12px;
                padding: 8px 22px 8px 8px;
                position: relative;
                line-height: 14px;
                text-align: center;
                border: 1px solid $borderWeightColor;
                border-radius: 2px;
                background-color: #f9fbfc;
                font-size: 14px;
                cursor: move;
            }
            .delete-btn {
                display: inline-block;
                width: 10px;
                height: 10px;
                overflow: hidden;
                position: absolute;
                top: 10px;
                right: 8px;
                cursor: pointer;
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
                max-width: 169px;
                word-break: break-all;
                margin-right: 3px;
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
</style>

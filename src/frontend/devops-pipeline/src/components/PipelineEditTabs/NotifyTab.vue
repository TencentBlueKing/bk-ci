<template>
    <section>
        <template v-for="card in notifyList">
            <bk-card :key="card.type" :is-collapse="true" :collapse-icons="icons" :border="false" class="notify-item">
                <div slot="header" class="item-header">
                    <span class="notify-title">{{card.name}}</span>
                    <bk-link theme="primary" icon="bk-icon icon-plus" @click.stop="handleEdit(card.type, -1)">
                        新增通知
                    </bk-link>
                </div>
                <div class="item-content-area">
                    <template v-for="(item, index) in getRenderInfo(card.type)">
                        <div :key="index" class="item-content">
                            <div class="operate-icons">
                                <i class="devops-icon icon-edit2" @click="handleEdit(card.type, index)"></i>
                                <i class="devops-icon icon-more" style="font-size: 16px"></i>
                            </div>
                            <template v-for="field in renderFields">
                                <div class="item-info" :key="field.col">
                                    <div class="info-label">
                                        {{field.label}}
                                    </div>
                                    <div class="info-content">
                                        {{getShowContent(field.col, item[field.col])}}
                                    </div>
                                </div>
                            </template>
                            <div class="item-info" v-if="item.wechatGroupFlag && item.wechatGroup && item.types && item.types.includes('WEWORK')">
                                <div class="info-label">
                                    企业微信群ID
                                </div>
                                <div class="info-content">
                                    {{item.wechatGroup}}
                                </div>
                            </div>
                        </div>
                    </template>
                </div>
            </bk-card>
        </template>

        <bk-sideslider
            quick-close
            :width="640"
            :title="slideTitle"
            :is-show.sync="showSlider"
            ext-cls="edit-notify-container"
            @hidden="closeSlider"
        >
            <div class="edit-notify-content" slot="content">
                <notify-setting :subscription="sliderEditItem" :update-subscription="updateEditItem" />
            </div>
            <div class="edit-notify-footer" slot="footer">
                <bk-button theme="primary" @click="handleSaveNotify">
                    新增
                </bk-button>
                <bk-button style="margin-left: 4px;" @click="hideSlider">
                    取消
                </bk-button>
            </div>
        </bk-sideslider>
    </section>
</template>

<script>
    import NotifySetting from '@/components/pipelineSetting/NotifySetting'

    const defaultSuc = {
        types: [],
        groups: [],
        users: '${BK_CI_START_USER_NAME}',
        wechatGroupFlag: false,
        wechatGroup: '',
        wechatGroupMarkdownFlag: false,
        detailFlag: false,
        content: '【${BK_CI_PROJECT_NAME_CN}】- 【${BK_CI_PIPELINE_NAME}】#${BK_CI_BUILD_NUM} 执行成功，耗时${BK_CI_BUILD_TOTAL_TIME}, 触发人: ${BK_CI_START_USER_NAME}。'
    }

    const defaultFail = {
        types: [],
        groups: [],
        users: '${BK_CI_START_USER_NAME}',
        wechatGroupFlag: false,
        wechatGroup: '',
        wechatGroupMarkdownFlag: false,
        detailFlag: false,
        content: '【${BK_CI_PROJECT_NAME_CN}】- 【${BK_CI_PIPELINE_NAME}】#${BK_CI_BUILD_NUM} 执行成功，耗时${BK_CI_BUILD_TOTAL_TIME}, 触发人: ${BK_CI_START_USER_NAME}。'
    }

    export default {
        name: 'notify-tab',
        components: {
            NotifySetting
        },
        props: {
            successSubscriptionList: Array,
            failSubscriptionList: Array,
            updateSubscription: Function
        },
        data () {
            return {
                showSlider: false,
                sliderEditItem: {},
                editType: '', // 当前编辑通知类型，成功或失败
                editIndex: -1, // 当前编辑哪一项通知， -1表示新增
                icons: ['icon-right-shape', 'icon-down-shape'],
                notifyList: [
                    {
                        type: 'successSubscriptionList',
                        name: this.$t('settings.whenSuc')
                    },
                    {
                        type: 'failSubscriptionList',
                        name: this.$t('settings.whenFail')
                    }
                ],
                renderFields: [
                    {
                        col: 'types',
                        label: '通知方式'
                    },
                    {
                        col: 'groups',
                        label: '通知组'
                    },
                    {
                        col: 'users',
                        label: '通知人'
                    },
                    {
                        col: 'content',
                        label: '通知内容'
                    }
                ]
            }
        },
        computed: {
            slideTitle () {
                const actionType = this.editIndex > -1 ? this.$t('settings.编辑通知') : this.$t('settings.新增通知')
                const targetType = this.editType === 'failSubscriptionList' ? this.$t('settings.whenFail') : this.$t('settings.whenSuc')
                return actionType + ' - ' + targetType
            }
        },
        methods: {
            getRenderInfo (type) {
                console.log(type)
                return this[type]
            },
            getShowContent (col, val) {
                console.log(col, val)
                let res = ''
                if (col === 'types') {
                    res = val.join(',')
                } else if (col === 'groups') {
                    res = val.join(',')
                } else {
                    res = val
                }
                return res
            },
            handleEdit (type, index) {
                this.showSlider = true
                this.editType = type
                this.editIndex = index
                if (index > -1 && this[type][index]) {
                    this.sliderEditItem = this[type][index]
                } else {
                    this.sliderEditItem = type === 'failSubscriptionList' ? Object.assign({}, defaultFail) : Object.assign({}, defaultSuc)
                }
                console.log(this.successSubscriptionList, this.failSubscriptionList, 'list')
                console.log(this[type], 'type')
            },
            handleSaveNotify () {
                console.log(this.sliderEditItem, this.editIndex, this.editType)
                if (this.editIndex > -1) {
                    this[this.editType][this.editIndex] = this.sliderEditItem
                } else {
                    this[this.editType].push(this.sliderEditItem)
                }
                console.log(this[this.editType], 'aftersave')
                this.hideSlider()
            },
            updateEditItem (name, value) {
                Object.assign(this.sliderEditItem, { [name]: value })
                console.log(this.sliderEditItem, 'editing')
            },
            hideSlider () {
                this.showSlider = false
                this.editType = ''
                this.editIndex = -1
                this.sliderEditItem = {}
            }
        }
    }
</script>

<style lang="scss">
    .notify-item {
        box-shadow: none;
        &:hover {
            box-shadow: none;
        }
        .bk-card-head {
            background-color: #F5F7FA;
            height: 40px;
        }
        .bk-card-body {
            padding: 16px 24px 0;
        }
        .item-header {
            height: 40px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            font-size: 14px;
        }
        .item-content-area {
            display: flex;
            flex-wrap: wrap;
            .item-content {
                position: relative;
                width: 600px;
                border: 1px solid #DCDEE5;
                padding: 24px 24px 8px;
                margin-bottom: 24px;
                .operate-icons {
                    position: absolute;
                    top: 10px;
                    right: 12px;
                    .devops-icon {
                        font-size: 12px;
                        padding-left: 4px;
                    }
                }
                &:nth-child(odd) {
                    margin-right: 140px;
                }
                .item-info {
                    display: flex;
                    font-size: 12px;
                    margin-bottom: 16px;
                    .info-label {
                        width: 100px;
                        color: #979BA5;
                    }
                    .info-content {
                        flex: 1;
                        color: #63656E;
                    }
                }
            }
        }
    }
    .edit-notify-container {
        .edit-notify-content {
            padding: 20px 24px;
        }
        .bk-sideslider-footer {
            position: absolute;
            bottom: 0;
            .edit-notify-footer {
                margin-left: 24px;
            }
        }
        /* .bk-s */
    }

    @media screen and (max-width: 1496px) { /*当屏幕尺寸小于1496px时，应用下面的CSS样式*/
        .item-content {
            width: 520px !important;
            &:nth-child(odd) {
                margin-right: 80px !important;
            }
        }
    }
    
</style>

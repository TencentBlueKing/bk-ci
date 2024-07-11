<template>
    <section>
        <bk-card v-for="card in notifyList" :key="card.type" :is-collapse="true" :collapse-icons="icons" :border="false" class="notify-item">
            <div slot="header" class="item-header">
                <span class="notify-title">{{card.name}}</span>
                <bk-link v-if="editable" theme="primary" icon="bk-icon icon-plus" @click.stop="handleEdit(card.type, -1)">
                    {{$t('newui.addNotice')}}
                </bk-link>
            </div>
            <div class="item-content-area">
                <template v-for="(item, index) in getRenderInfo(card.type)">
                    <div :key="index" class="item-content">
                        <div v-if="editable" class="operate-icons">
                            <i class="devops-icon icon-edit" @click="handleEdit(card.type, index)"></i>
                            <bk-popover class="setting-more-dot-menu"
                                placement="bottom-start"
                                theme="project-manage-more-dot-menu light"
                                trigger="click"
                                :arrow="false"
                                :distance="0">
                                <span class="more-menu-trigger">
                                    <i class="devops-icon icon-more" style="display: inline-block;margin-top: 2px;font-size: 18px"></i>
                                </span>
                                <ul class="setting-menu-list" slot="content">
                                    <li @click="handleDelete(card.type, index)" style="padding: 0 2px;cursor: pointer;">{{$t('delete')}}</li>
                                </ul>
                            </bk-popover>
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
                                {{$t('weChatGroupID')}}
                            </div>
                            <div class="info-content">
                                {{item.wechatGroup}}
                            </div>
                        </div>
                    </div>
                </template>
            </div>
        </bk-card>

        <bk-sideslider
            quick-close
            :width="640"
            :title="slideTitle"
            :is-show.sync="showSlider"
            ext-cls="edit-notify-container"
        >
            <div class="edit-notify-content" slot="content">
                <notify-setting
                    ref="notifySettingTab"
                    :subscription="sliderEditItem"
                    :update-subscription="updateEditItem"
                />
            </div>
            <div class="edit-notify-footer" slot="footer">
                <bk-button theme="primary" @click="handleSaveNotify">
                    {{$t('confirm')}}
                </bk-button>
                <bk-button style="margin-left: 4px;" @click="hideSlider">
                    {{$t('cancel')}}
                </bk-button>
            </div>
        </bk-sideslider>
    </section>
</template>

<script>
    import { deepCopy } from '@/utils/util'
    import NotifySetting from '@/components/pipelineSetting/NotifySetting'

    const defaultSuc = {
        types: [],
        groups: [],
        users: '${{ci.actor}}',
        wechatGroupFlag: false,
        wechatGroup: '',
        wechatGroupMarkdownFlag: false,
        detailFlag: false,
        content: window.pipelineVue?.$i18n?.t('settings.defaultSuc')
    }

    const defaultFail = {
        types: [],
        groups: [],
        users: '${{ci.actor}}',
        wechatGroupFlag: false,
        wechatGroup: '',
        wechatGroupMarkdownFlag: false,
        detailFlag: false,
        content: window.pipelineVue?.$i18n?.t('settings.defaultFail')
    }

    export default {
        name: 'notify-tab',
        components: {
            NotifySetting
        },
        props: {
            editable: {
                type: Boolean,
                default: true
            },
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
                        label: this.$t('settings.noticeType')
                    },
                    {
                        col: 'groups',
                        label: this.$t('settings.noticeGroup')
                    },
                    {
                        col: 'users',
                        label: this.$t('settings.additionUser')
                    },
                    {
                        col: 'content',
                        label: this.$t('settings.noticeContent')
                    }
                ],
                notifyTypeMap: {
                    EMAIL: this.$t('settings.emailNotice'),
                    WEWORK: this.$t('settings.rtxNotice'),
                    RTX: this.$t('settings.rtxNotice'),
                    VOICE: this.$t('settings.voice'),
                    WECHAT: this.$t('settings.wechatNotice'),
                    SMS: this.$t('settings.smsNotice')
                }
            }
        },
        computed: {
            slideTitle () {
                const actionType = this.editIndex > -1 ? this.$t('newui.editNotice') : this.$t('newui.addNotice')
                const targetType = this.editType === 'failSubscriptionList' ? this.$t('settings.whenFail') : this.$t('settings.whenSuc')
                return actionType + ' - ' + targetType
            }
        },
        methods: {
            getRenderInfo (type) {
                return this[type]
            },
            getShowContent (col, val) {
                let res = ''
                if (col === 'types') {
                    const showTypes = val.map(item => this.notifyTypeMap[item] || item)
                    return showTypes.join(',')
                } else if (col === 'groups') {
                    res = val.join(',')
                } else {
                    res = val
                }
                res = res || '--'
                return res
            },
            handleDelete (type, index) {
                this[type].splice(index, 1)
                this.updateSubscription(type, this[type])
            },
            handleEdit (type, index) {
                this.showSlider = true
                this.editType = type
                this.editIndex = index
                if (index > -1 && this[type][index]) {
                    this.sliderEditItem = deepCopy(this[type][index])
                } else {
                    this.sliderEditItem = deepCopy(type === 'failSubscriptionList' ? defaultFail : defaultSuc)
                }
            },
            handleSaveNotify () {
                this.$refs?.notifySettingTab?.$refs?.notifyForm?.validate().then(() => {
                    if (this.editIndex > -1) {
                        this[this.editType][this.editIndex] = this.sliderEditItem
                    } else {
                        this[this.editType].push(this.sliderEditItem)
                    }
                    this.updateSubscription(this.editType, this[this.editType])
                    this.hideSlider()
                })
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
        margin-bottom: 8px;
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
                margin-bottom: 16px;
                .operate-icons {
                    position: absolute;
                    top: 10px;
                    right: 12px;
                    display: flex;
                    align-items: center;
                    grid-gap: 10px;
                    font-size: 16px;
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
                        word-break: break-all;
                    }
                }
            }
        }
    }
    .edit-notify-container {
        z-index: 2010;
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

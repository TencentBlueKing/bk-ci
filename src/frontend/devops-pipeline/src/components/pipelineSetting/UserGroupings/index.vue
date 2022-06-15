<template>
    <section>
        <div class="setting-wrapper bk-form">
            <div :class="['setting-header', { 'hasFold': isFold }]" @click="toggleFold">
                <span class="setting-title">{{title}}</span>
                <slot name="introduce"></slot>
                <div class="setting-expand" v-if="isFold">
                    <div v-if="isContentShow">
                        <span>{{ $t('settings.fold') }}</span>
                        <i class="devops-icon icon-angle-up"></i>
                    </div>
                    <div v-else>
                        <span>{{ $t('settings.open') }}</span>
                        <i class="devops-icon icon-angle-down"></i>
                    </div>
                </div>
            </div>
            <div class="setting-content" v-if="isContentShow">
                <bk-form :style="{ 'width': computeWidth, 'max-width': computeMaxWidth }">
                    <bk-form-item :label-width="90" :label="inputLabel">
                        <staff-input :handle-change="handleChange" name="users" :value="pipelineSettingUser"></staff-input>
                        <div class="setting-extra" v-if="extraUserList.length">
                            <bk-popover placement="bottom-start">
                                <p class="setting-extra-list">{{ $t('settings.attach') }}：{{extraUserList.join(',')}}</p>
                                <div class="setting-tip-content" slot="content">{{extraUserList.join(',')}}</div>
                            </bk-popover>
                            <p class="setting-extra-tip">
                                <slot name="setting-extra-tip"></slot>
                            </p>
                        </div>
                    </bk-form-item>
                </bk-form>
                <bk-form :style="{ 'width': computeWidth, 'max-width': computeMaxWidth }">
                    <bk-form-item :label-width="90" :label="selectLabel">
                        <bk-select :multiple="multiSelect" :value="selected" @change="selectedChange" searchable>
                            <bk-option v-for="item in list" :key="item[settingKey]" :id="item[settingKey]" :name="item[selectKeyText]">
                            </bk-option>
                        </bk-select>
                        <bk-popover v-if="extraGroupList.length" placement="bottom-start" class="setting-extra">
                            <p class="setting-extra-list">{{ $t('settings.attach') }}：{{ extraGroupStr() }}</p>
                            <div class="setting-tip-content" slot="content">{{ extraGroupStr() }}</div>
                        </bk-popover>
                    </bk-form-item>
                </bk-form>
            </div>
        </div>
    </section>
</template>

<script>
    import { mapState } from 'vuex'
    import StaffInput from '@/components/atomFormField/StaffInput/index.vue'
    export default {
        components: {
            StaffInput
        },
        props: {
            settingKey: {
                type: String,
                default: 'id'
            },
            selectKeyText: {
                type: String,
                default: 'name'
            },
            dataIndex: {
                type: Number
            },
            width: {
                type: [String, Number],
                default: '100%'
            },
            maxWidth: {
                type: String,
                default: '100%'
            },
            isFold: {
                type: Boolean,
                default: true
            },
            isContentShow: {
                type: Boolean,
                default: false
            },
            multiSelect: {
                type: Boolean,
                default: true
            },
            title: {
                type: String,
                default: 'title'
            },
            inputLabel: {
                type: String,
                default: ''
            },
            selectLabel: {
                type: String,
                default: ''
            },
            selected: {
                type: Array,
                default: []
            },
            pipelineSettingUser: {
                type: Array,
                default: []
            },
            extraUserList: {
                type: Array,
                default: []
            },
            extraGroupList: {
                type: Array,
                default: []
            },
            list: {
                type: Array,
                default: []
            }
        },
        computed: {
            ...mapState('common', [
                'pipelineSetting'
            ]),
            computeWidth () {
                let width = this.width
                if (!width) {
                    return false
                }
                width = width.toString()
                return width.indexOf('%') > -1 ? width : width + 'px'
            },
            computeMaxWidth () {
                let max = this.maxWidth
                if (!max) {
                    return false
                }
                max = max.toString()
                return max.indexOf('%') > -1 ? max : max + 'px'
            }
        },
        created () {
            if (this.dataIndex === 0) {
                this.isContentShow = true
            }
        },
        methods: {
            toggleFold () {
                if (this.isFold) {
                    this.isContentShow = !this.isContentShow
                }
            },
            handleChange (name, value) {
                this.$emit('groupingChange', {
                    selectedId: this.selected,
                    usersGroup: value,
                    dataIndex: this.dataIndex
                })
            },
            selectedChange (selected) {
                this.$emit('groupingChange', {
                    selectedId: selected,
                    usersGroup: this.pipelineSettingUser,
                    dataIndex: this.dataIndex
                })
            },
            extraGroupStr () {
                const arr = []
                this.extraGroupList.forEach(item => {
                    arr.push(item.group_name)
                })
                return arr.join(',')
            }
        }
    }
</script>

<style lang='scss'>
    @import './../../../scss/conf.scss';
    .setting-wrapper {
        margin-bottom: 10px;
    }
    .setting-header {
        position: relative;
        // width: 755px;
        height: 43px;
        line-height: 38px;
        padding: 0 11px;
        background-color: #fafbfd; // #fafbfd;
        border: 1px solid #dde4eb;
        border-radius: 2px 2px 0 0;
        font-size: 12px;
        font-weight: 700;
        color: #63656E;
        &.hasFold {
            cursor: pointer;
        }
        .setting-title {
            float: left;
            line-height: 41px;
            display: inline-block;
            max-width: 200px;
            font-size: 12px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }
        .setting-expand {
            position: absolute;
            right: 20px;
            top: 2px;
            font-size: 12px;
            font-weight: 400;
            color: #3c96ff;
        }
    }
    .setting-content {
        padding: 20px 15px 10px 0;
        background: #fff;
        border-left: 1px solid #dde4eb;
        border-right: 1px solid #dde4eb;
        border-bottom: 1px solid #dde4eb;
        border-radius: 0 0 2px 2px;
    }
    .setting-extra {
        position: relative;
        margin-bottom: 10px;
        padding-right: 170px;
        font-size: 12px;
        color: #63656E;
        .setting-extra-list {
            margin: 8px 0 0 0;
            display: inline-block;
            width: 100%;
            white-space: nowrap;
            text-overflow: ellipsis;
            overflow: hidden;
        }
        .setting-extra-tip {
            position: absolute;
            top: 8px;
            right: 0;
            margin: 0;
            color: #3c96ff;
            cursor: default;
        }
        .setting-tip-content {
            white-space: normal;
            word-wrap: break-word;
        }
    }
</style>

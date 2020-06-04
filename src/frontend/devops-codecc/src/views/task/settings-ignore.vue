<template>
    <div>
        <bk-tab class="settings-ignore-tab" :active.sync="active" type="card" @tab-change="changeTab">
            <template slot="setting">
                <div class="settings-ignore-header" v-if="customExist">
                    <div v-if="customExist" class="header-tab-middle">
                        <bk-button @click="newVisible" size="small" icon="plus" :theme="'primary'" :title="$t('filter.添加路径')">{{$t('filter.添加路径')}}</bk-button>
                    </div>
                    <div class="header-tab-right">{{$t('filter.添加后的代码路径将不会产生告警')}}</div>
                </div>
                <div class="settings-ignore-header" v-if="defaultExist">
                    <div v-if="defaultExist" class="header-tab-middle">
                        <bk-button @click="inputVisible" size="small" icon="plus" :theme="'primary'" :title="$t('filter.默认设置')">{{$t('filter.默认设置')}}</bk-button>
                    </div>
                    <div class="header-tab-right">{{$t('filter.添加后的代码路径将不会产生告警')}}</div>
                </div>
            </template>
            <bk-tab-panel
                v-for="(panel, index) in options"
                :key="index"
                v-bind="panel">
            </bk-tab-panel>
            <!-- 添加路径组件 -->
            <div class="path-list" v-if="customExist">
                <div
                    class="custom-path"
                    v-for="(customPath, index) in customList"
                    :key="index">
                    {{customPath}}
                    <div class="del-style" @click="delCustomPath(customPath)">{{$t('op.删除')}}</div>
                </div>
            </div>
            <!-- /添加路径组件 -->
            <!-- 系统默认组件 -->
            <div class="path-list" v-if="defaultExist">
                <div
                    class="default-path"
                    v-for="(defaultPath, index) in defaultSelectList"
                    :key="index">
                    {{defaultPath}}
                    <div class="del-style" @click="delDefaultPath(defaultPath)">{{$t('op.移除')}}</div>
                </div>
            </div>
            <!-- /系统默认组件 -->
            <div class="no-path" v-if="customNone">
                <empty size="small" :title="$t('filter.暂无任何路径')" :desc="$t('filter.添加后的代码路径将不再进行代码检查')">
                    <template v-slot:action>
                        <bk-button theme="primary" @click="newVisible">{{$t('filter.添加路径')}}</bk-button>
                    </template>
                </empty>
            </div>
            <div class="no-path" v-if="defaultNone">
                <empty size="small" :title="$t('filter.暂无任何路径')" :desc="$t('filter.添加后的代码路径将不再进行代码检查')">
                    <template v-slot:action>
                        <bk-button theme="primary" @click="inputVisible">{{$t('filter.添加路径')}}</bk-button>
                    </template>
                </empty>
            </div>
        </bk-tab>
        <bk-dialog
            v-model="delVisiable"
            :theme="'primary'"
            :mask-close="false"
            @cancel="delVisiable = false"
            @confirm="handleDelete(pathName)"
            :title="$t('op.确认')">
            {{$t('filter.确认要删除吗', { pathName })}}
        </bk-dialog>
        <SettingsIgnoreNew :visible="isCreateShow" @visibleChange="updateCreateVisible" />
        <SettingsIgnoreInput :selected="defaultSelectList" :list="defaultList" :visible="isInputShow" @visibleChange="updateInputVisible" />
    </div>
</template>

<script>
    import { mapState } from 'vuex'
    import SettingsIgnoreNew from '@/components/settings-ignore-new/index'
    import SettingsIgnoreInput from '@/components/settings-ignore-new/input'
    import Empty from '@/components/empty'

    export default {
        components: {
            SettingsIgnoreNew,
            SettingsIgnoreInput,
            Empty
        },
        data () {
            return {
                options: [
                    { name: 'custom', label: this.$t('nav.自定义') },
                    { name: 'default', label: this.$t('nav.系统默认') }
                ],
                select: 'custom',
                tabSelect: 'custom',
                isCreateShow: false,
                isInputShow: false,
                delVisiable: false,
                pathName: ''
            }
        },
        computed: {
            ...mapState('task', {
                taskIgnore: 'ignore'
            }),
            taskId () {
                return this.$route.params.taskId
            },
            customNone () {
                let customNone = false
                customNone = this.tabSelect === 'custom' && this.customList.length === 0
                return customNone
            },
            defaultNone () {
                let defaultNone = false
                defaultNone = this.tabSelect === 'default' && this.defaultSelectList.length === 0
                return defaultNone
            },
            customExist () {
                let customExist = false
                customExist = this.tabSelect === 'custom' && this.customList.length !== 0
                return customExist
            },
            defaultExist () {
                let defaultExist = false
                defaultExist = this.tabSelect === 'default' && this.defaultSelectList.length !== 0
                return defaultExist
            },
            customList () {
                const customList = []
                if (this.taskIgnore) {
                    Object.assign(customList, this.taskIgnore.filterPaths)
                    return customList
                } else return customList
            },
            defaultList () {
                const defaultList = []
                if (this.taskIgnore) {
                    Object.assign(defaultList, this.taskIgnore.defaultFilterPath)
                    return defaultList
                } else return defaultList
            },
            defaultSelectList () {
                const defaultSelectList = []
                if (this.taskIgnore) {
                    Object.assign(defaultSelectList, this.taskIgnore.defaultAddPaths)
                    return defaultSelectList
                } else return defaultSelectList
            }
        },
        created () {
            this.$store.dispatch('task/ignore', this.taskId)
        },
        methods: {
            // 切换tab页面
            changeTab (name) {
                this.tabSelect = name
            },
            // 打开添加路径组件
            newVisible () {
                this.isCreateShow = true
            },
            // 打开添加系统默认组件
            inputVisible () {
                this.isInputShow = true
            },
            delCustomPath (path) {
                this.pathName = path
                this.delVisiable = true
            },
            delDefaultPath (path) {
                this.pathName = path
                this.delVisiable = true
            },
            // 弹出组件的显示关闭状态与方法
            updateCreateVisible (visible) {
                this.isCreateShow = visible
            },
            updateInputVisible (visible) {
                this.isInputShow = visible
            },
            handleDelete (name) {
                const params = {
                    taskId: '',
                    path: '',
                    pathType: ''
                }
                if (this.tabSelect === 'default') {
                    params.taskId = this.taskId
                    params.path = name
                    params.pathType = 'DEFAULT'
                } else if (this.tabSelect === 'custom') {
                    params.taskId = this.taskId
                    params.path = name
                    params.pathType = 'CUSTOM'
                }
                this.$store.dispatch('task/deleteIgnore', { params: params }).then(res => {
                    if (res === true) {
                        this.$bkMessage({ theme: 'success', message: this.$t('op.删除成功') })
                        this.$store.dispatch('task/ignore', this.taskId)
                        this.$store.dispatch('task/ignoreTree')
                    }
                }).catch(e => {
                    this.$bkMessage({ theme: 'error', message: this.$t('op.删除失败') })
                    this.$store.dispatch('task/ignoreTree')
                })
                this.delVisiable = false
            }
        }
    }
</script>

<style lang="postcss" scoped>
    @import '../../css/variable.css';
    .settings-ignore-tab {
        margin: -17px -5px;
        border-top: none;
        /* 路径列表 start */
        .path-list {
            padding-top: 5px;
            .custom-path,.default-path {
                border-bottom: 1px solid $borderColor;
                padding: 8px 0;
                font-size: 14px;
                .del-style {
                    cursor: pointer;
                    color: $goingColor;
                    float: right;
                    font-size: 14px;
                    padding-right: 14px;
                }
            }
        }
        .no-path {
            margin-top: 48px;
        }
        /* 路径列表 end */
        >>>.bk-tab-section {
            padding: 0 6px;
        }
        /* 组件按钮 start */
        .settings-ignore-header {
            .header-tab-left {
                display: inline-block;
                cursor: pointer;
                font-size: 14px;
                padding: 0 5px;
            }
            .active {
                padding-bottom: 6px;
                border-bottom:2px solid $goingColor;
                color: $goingColor;
            }
            .header-tab-right {
                float: right;
                font-size: 12px;
                color: $fontLightColor;
                padding-top: 2.5px;
            }
            .header-tab-middle {
                float: right;
                font-size: 12px;
                color: $goingColor;
                padding-left: 18px;
                cursor: pointer;
                >>>.bk-button.bk-button-small {
                    padding:0 6px;
                }
            }
        }
        /* 组件按钮 end */
    }
    /* 覆盖组件样式 start */
    >>>.settings-ignore-tab {
        .bk-tab-header {
            background-color: #ffffff;
            border: none;
            background-image: linear-gradient(transparent 42px,#dcdee5 0);
            .bk-tab-label-wrapper {
                .bk-tab-label-list {
                    .bk-tab-label-item {
                        border: none;
                        min-width: 62px;
                    }
                    .bk-tab-label-item.active {
                        color: $goingColor;
                        font-weight: bold;
                    }
                    .bk-tab-label-item.active:after {
                        background-color: $goingColor;
                        height: 2px;
                        top: auto;
                        bottom: 0;
                    }
                }
            }
        }
    }
    >>>.bk-tab.bk-tab-card .bk-tab-label-list {
        border: none;
        .bk-tab-label-item.is-first {
            border: none;
        }
    }
    >>>.bk-tab-section {
        border: none;
    }
    /* 覆盖组件样式 end */

</style>

<template>
    <div>
        <bk-tab :active.sync="active" type="unborder-card">
            <bk-tab-panel
                v-for="(panel, index) in panels"
                v-bind="panel"
                :key="index"
            >
                <div class="tool-added" v-if="index === 0">
                    <div class="tool-list">
                        <div v-for="(tool, toolIndex) in toolAddedList" :key="toolIndex" class="tool-item">
                            <tool-card type="manage" :tool="tool" @status-change="toolStatusChange"></tool-card>
                        </div>
                    </div>
                </div>
                <div class="tool-rest" v-if="index === 1">
                    <div class="tool-list">
                        <div v-for="(tool, toolIndex) in toolRestList" :key="toolIndex" class="tool-item">
                            <tool-card :tool="tool" type="manage" @add-click="openToolAddSlider"></tool-card>
                        </div>
                    </div>
                </div>
            </bk-tab-panel>
        </bk-tab>
        <bk-dialog
            width="480"
            header-position="left"
            v-model="disableDialog.visiable"
            :title="$t('tools.停用x工具', { tool: currentToolDisplayName })"
            :mask-close="false"
            @confirm="confirm"
            @value-change="disableDialogVisiableChange"
        >
            <div class="disable-alert"><i class="bk-icon icon-info-circle alert-icon"></i>{{$t('tools.停用后该工具的数据将被保留，功能将被停止。您也可以重新启用该工具。')}}</div>
            <bk-input
                :placeholder="$t('tools.请输入停用原因，至少10个字符…')"
                :type="'textarea'"
                :rows="3"
                :maxlength="100"
                v-model="disableReason"
            >
            </bk-input>
            <div slot="footer">
                <bk-button type="button" :disabled="disableReason.length < 10" :loading="disableDialog.loading" theme="primary" @click.native="toolDisableConfirm">
                    {{$t('op.确定')}}
                </bk-button>
                <bk-button type="button" :disabled="disableDialog.loading" @click.native="toolDisableCancel">
                    {{$t('op.取消')}}
                </bk-button>
            </div>
        </bk-dialog>
        <bk-sideslider
            width="640"
            style="margin-top: 50px;z-index: 4"
            :key="+new Date()"
            :is-show.sync="isShowSideSlider"
            :quick-close="true"
            :title="$t('tools.x代码库信息', { tool: currentToolDisplayName })"
        >
            <div slot="content" class="sideslider-content">
                <div class="sideslider-header-extra">
                </div>
                <tool-config-form
                    v-if="isShowSideSlider"
                    class="form-add"
                    scenes="manage-add"
                    is-tool-manage="true"
                    :tools="[currentToolName]"
                    :code-message="codeMessage"
                    :success="toolAddSuccess"
                />
            </div>
        </bk-sideslider>
    </div>
</template>

<script>
    import { mapState } from 'vuex'
    import ToolCard from '@/components/tool-card'
    import ToolConfigForm from '@/components/tool-config-form'

    export default {
        components: {
            ToolCard,
            ToolConfigForm
        },
        data () {
            return {
                panels: [
                    { name: 'tool-added', label: this.$t('nav.已有工具') },
                    { name: 'tool-more', label: this.$t('nav.更多工具') }
                ],
                disableDialog: {
                    visiable: false,
                    loading: false
                },
                isShowSideSlider: false,
                currentToolName: undefined,
                disableReason: ''
            }
        },
        computed: {
            ...mapState('task', {
                taskDetail: 'detail',
                codeMessage: 'codes'
            }),
            ...mapState('tool', {
                toolMap: 'mapList'
            }),
            toolAddedList () {
                const { enableToolList, disableToolList } = this.taskDetail
                const toolAddedList = enableToolList.concat(disableToolList)

                return toolAddedList
            },
            toolRestList () {
                const toolMap = this.toolMap
                const toolRestList = []
                const toolAddedList = this.toolAddedList
                Object.keys(toolMap).forEach(key => {
                    const tool = toolMap[key]
                    if (!toolAddedList.find(item => item.toolName === tool.name)) {
                        toolRestList.push(tool)
                    }
                })

                return toolRestList
            },
            currentTool () {
                return this.toolMap[this.currentToolName] || {}
            },
            currentToolDisplayName () {
                const toolName = this.currentTool.displayName
                return this.$t(`toolName.${toolName}`)
            }
        },
        watch: {
            'isShowSideSlider': {
                handler (newVal, oldVal) {
                    document.body.style.overflow = this.isShowSideSlider ? 'hidden' : ''
                }
            }
        },
        created () {
            this.init()
        },
        methods: {
            async init () {
                await this.$store.dispatch('tool/updated', { showLoading: true })
                await this.$store.dispatch('task/getCodeMessage')
            },
            toolStatusChange (enabled, toolName) {
                this.currentToolName = toolName
                if (!enabled) {
                    this.disableDialog.visiable = true
                } else {
                    this.changeToolStatus(enabled)
                }
            },
            toolDisableConfirm () {
                this.changeToolStatus(false)
            },
            toolDisableCancel () {
                this.disableDialog.visiable = false
            },
            changeToolStatus (enabled) {
                const data = {
                    toolNameList: [this.currentToolName],
                    stopReason: enabled ? '' : this.disableReason,
                    manageType: enabled ? 'Y' : 'N'
                }
                this.$store.dispatch('task/changeToolStatus', data, { showLoading: true }).then(res => {
                    if (res.code === '0') {
                        this.disableDialog.visiable = false
                        this.updateTaskTool()
                    }
                }).catch(e => {
                    console.error(e)
                })
            },
            openToolAddSlider (name) {
                this.currentToolName = name
                this.isShowSideSlider = true
                window.scrollTo(0, 0)
            },
            disableDialogVisiableChange (visiable) {
                // 如果是关闭dialog则迫使拉取数据以恢复停用开关按钮状态
                if (!visiable) {
                    this.updateTaskTool()
                }
                this.disableReason = ''
            },
            toolAddSuccess () {
                this.isShowSideSlider = false
                this.updateTaskTool()
            },
            updateTaskTool () {
                // 重新获取任务详情，以更新视图，如左侧导航
                this.$store.dispatch('task/detail', { showLoading: true })
                this.$store.dispatch('task/getRepoList', { projCode: this.$route.params.projectId })
            }
        }
    }
</script>

<style lang="postcss" scoped>
    @import '../../css/mixins.css';

    .tool-list {
        @mixin clearfix;
        .tool-item {
            float: left;
            margin-right: 16px;
            margin-bottom: 16px;
        }
    }

    .sideslider-content {
        padding: 20px;
        .form-add {
            width: 80%;
        }

        .sideslider-header-extra {
            position: absolute;
            right: 16px;
            top: 0;
            height: 60px;
            line-height: 60px;
            font-size: 12px;
        }
    }

    .disable-alert {
        position: relative;
        background: #f0f1f5;
        border-radius:2px;
        border:1px solid #f0f1f5;
        padding: 12px 20px 12px 48px;
        font-size: 12px;
        color: #63656e;
        margin-bottom: 12px;

        .alert-icon {
            position: absolute;
            left: 16px;
            top: 22px;
            color: #a3c5fd;
            font-size: 18px;
        }
    }
    .bk-sideslider-wrapper {
        overflow: hidden;
    }
</style>

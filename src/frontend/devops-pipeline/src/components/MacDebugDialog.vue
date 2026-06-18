<template>
    <bk-dialog
        v-model="isShow"
        :close-icon="false"
        :show-footer="false"
        :width="642"
        class="debug-dialog"
    >
        <div slot="default">
            <div
                v-bkloading="{ isLoading: isLoading }"
                class="content"
            >
                <!-- 错误状态 -->
                <template v-if="errorMessage && !isLoading">
                    <div class="debug-error">
                        <bk-alert
                            type="error"
                            :title="errorMessage"
                        />
                        <bk-button
                            theme="primary"
                            @click="$emit('close')"
                        >
                            {{ $t('close') }}
                        </bk-button>
                    </div>
                </template>
                <!-- 正常状态 -->
                <template v-else-if="!errorMessage && hasDebugData">
                    <bk-alert
                        type="warning"
                    >
                        <i-18n
                            slot="title"
                            path="debugDisconnectNotice"
                        >
                            <span class="warning-text">{{ $t('tenMinutes') }}</span>
                            <span class="warning-text">{{ $t('autoExit') }}</span>
                        </i-18n>
                    </bk-alert>

                    <div class="debug-content">
                        <!-- 左侧工具列表 -->
                        <div class="debug-tools-list">
                            <p class="debug-title">{{ $t('debugConnectionMethod') }}</p>
                            <div
                                v-for="tool in toolList"
                                :key="tool.type"
                                class="tool-item"
                                :class="{ 'is-active': currentTool === tool.type }"
                                @click="currentTool = tool.type"
                            >
                                <div class="tool-item-content">
                                    <div class="tool-info">
                                        <div class="tool-name">{{ $t(tool.nameKey) }}</div>
                                        <div class="tool-desc">{{ $t(tool.descKey) }}</div>
                                    </div>
                                    <div
                                        class="tool-icon"
                                        :class="`tool-icon-${tool.type}`"
                                    ></div>
                                </div>
                            </div>

                            <p
                                class="more-guide"
                                @click="viewMoreGuide"
                            >
                                <Logo
                                    name="jump"
                                    size="12"
                                />
                                {{ $t('viewMoreGuidance') }}
                            </p>
                        </div>

                        <!-- 右侧详情内容 -->
                        <div class="debug-detail">
                            <span class="debugging">{{ $t('debugging') }}</span>

                            <div class="debug-info-box">
                                <div
                                    class="debug-info-item"
                                    v-if="currentTool === 'command'"
                                >
                                    <div class="info-label">{{ $t('loginCommand') }}</div>
                                    <div class="info-value">
                                        <code>{{ debugData.ssh }}</code>
                                        <i
                                            class="bk-icon icon-copy"
                                            @click="copy(debugData.ssh)"
                                        ></i>
                                    </div>
                                </div>
                                <div
                                    class="debug-info-item"
                                    v-else-if="currentTool === 'vnc'"
                                >
                                    <div class="info-label">{{ $t('vncAddress') }}</div>
                                    <div class="info-value">
                                        <code>{{ debugData.vnc }}</code>
                                        <i
                                            class="bk-icon icon-copy"
                                            @click="copy(debugData.vnc)"
                                        ></i>
                                    </div>
                                </div>

                                <div class="debug-info-item vnc-info">
                                    <div v-if="debugData.username">
                                        <div class="info-label">{{ $t('loginUsername') }}</div>
                                        <div class="info-value">
                                            <span class="password-dots">{{ debugData.username }}</span>
                                            <i
                                                class="bk-icon icon-copy"
                                                @click="copy(debugData.username)"
                                            ></i>
                                        </div>
                                    </div>
                                    <div>
                                        <div class="info-label">{{ $t('loginPassword') }}</div>
                                        <div class="info-value password-value">
                                            <span class="password-dots">{{ showPassword ? debugData.password : '●●●●●●●●' }}</span>
                                            <bk-icon
                                                :type="showPassword ? 'eye' : 'eye-slash'"
                                                @click="showPassword = !showPassword"
                                            />
                                            <i
                                                class="bk-icon icon-copy"
                                                @click="copy(debugData.password)"
                                            ></i>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <div
                                class="debug-footer"
                                @click="exitDebug"
                            >
                                <Logo
                                    name="shutdown"
                                    size="14"
                                />
                                {{ $t('exitDebug') }}
                            </div>
                        </div>
                    </div>
                </template>
            </div>
        </div>
        <div slot="tools">
            <p class="debug-tools">
                <span>{{ $t('editPage.docker.debugConsole') }}</span>
                <span
                    @click="$emit('close')"
                    v-bk-tooltips="$t('minimize')"
                >
                    <Logo
                        name="minus"
                        size="16"
                        class="debug-tool-close"
                    />
                </span>
            </p>
        </div>
    </bk-dialog>
</template>

<script>
    import Logo from '@/components/Logo'
    import { copyToClipboard } from '@/utils/util'
    import { mapActions } from 'vuex'
    export default {
        components: {
            Logo
        },
        props: {
            isShow: {
                type: Boolean,
                default: false
            },
            pipelineId: {
                type: String,
                required: true
            },
            buildId: {
                type: String,
                required: true
            },
            containerId: {
                type: String
            },
            executeCount: {
                type: Number
            }
        },
        data () {
            return {
                showPassword: false,
                currentTool: 'command', // 当前选中的工具：command 或 vnc
                isLoading: false,
                errorMessage: '', // 存储接口错误信息
                toolList: [
                    {
                        type: 'command',
                        nameKey: 'commandTool',
                        descKey: 'likeWeTerm'
                    },
                    {
                        type: 'vnc',
                        nameKey: 'vncTool',
                        descKey: 'likeMacScreenShare'
                    }
                ],
                // 调试数据，从接口获取
                debugData: {
                    ssh: '',
                    vnc: '',
                    username: '',
                    password: ''
                }
            }
        },
        computed: {
            // 判断是否有调试数据
            hasDebugData () {
                return this.debugData && (this.debugData.ssh || this.debugData.vnc)
            }
        },
        watch: {
            isShow (val) {
                if (!val) {
                    // 关闭弹窗时重置状态
                    this.showPassword = false
                } else {
                    // 打开弹窗时自动调用接口获取调试数据
                    this.fetchDebugData()
                }
            }
        },
        methods: {
            ...mapActions('atom',[
                'startVmSeqDebug',
                'stopVmSeqDebug'
            ]),
            // 获取调试数据
            async fetchDebugData () {
                this.isLoading = true
                this.errorMessage = ''
                // 重置 debugData
                this.debugData = {
                    ssh: '',
                    vnc: '',
                    username: '',
                    password: ''
                }
                
                try {
                    const response = await this.startVmSeqDebug({
                        pipelineId: this.pipelineId,
                        buildId: this.buildId,
                        containerId: this.containerId,
                        executeCount: this.executeCount
                    })
                    
                    if (response.data?.actionCode === 200 && response.data?.data) {
                        this.debugData = response.data.data
                    } else {
                        this.errorMessage = response.data?.actionMessage || this.$t('startDebugFailed')
                    }
                } catch (err) {
                    this.errorMessage = err.message || err || this.$t('startDebugFailed')
                } finally {
                    this.isLoading = false
                }
            },
            exitDebug () {
                this.$bkInfo({
                    subTitle: this.$t('confirmExitDebugSession'),
                    okText: this.$t('exit'),
                    confirmFn: async () => {
                        try {
                            await this.stopVmSeqDebug({
                                pipelineId: this.pipelineId,
                                buildId: this.buildId,
                                containerId: this.containerId,
                                executeCount: this.executeCount
                            })
                        } catch (err) {
                            this.$bkMessage({
                                theme: 'error',
                                message: err.message || err
                            })
                        } finally {
                            this.$emit('close')
                            this.showPassword = false
                        }
                    }
                })
            },
            copy (text) {
                try {
                    copyToClipboard(text)
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('copySuc')
                    })
                } catch (error) {
                    console.log(error)
                }
            },
            viewMoreGuide () {
                window.open('https://iwiki.woa.com/p/4019636391', '_blank')
            }
        }
    }
</script>

<style lang="scss">
.debug-dialog {
    .bk-dialog-tool {
        min-height: 32px;
        z-index: 1;
        box-shadow: 0 1px 1px 0 #DCDEE5;
        font-size: 14px;
        font-weight: 600;
        color: #313238;
    }
    .bk-dialog-body {
        padding: 0;
    }
    .content {
        min-height: 200px;
    }
    .debug-tools {
        display: flex;
        align-items: center;
        justify-content: space-between;
        line-height: 32px;
        justify-content: space-between;
        margin: 0 16px;
        .debug-tool-close {
            cursor: pointer;
            z-index: 1;
        }
    }
    .warning-text {
        color: #E38B02;
    }
    
    // 错误状态样式 - 在 loading 容器内部，debug-content 外部
    .debug-error {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        min-height: 200px;
        padding: 24px;
        gap: 16px;
        
        .bk-alert {
            width: 100%;
        }
    }
    
    // 警告提示样式
    .bk-alert-warning {
        margin-bottom: 0;
        
        .bk-alert-title {
            display: flex;
            align-items: center;
            gap: 4px;
        }
    }
    
    .debug-content {
        display: flex;
        justify-content: space-between;
        
        // 左侧工具列表
        .debug-tools-list {
            width: 262px;
            padding: 16px 6px 16px 16px;
            flex-shrink: 0;
            background: #FFFFFF;

            .debug-title {
                margin-bottom: 12px;
            }
            
            .tool-item {
                width: 230px;
                height: 56px;
                margin-bottom: 16px;
                padding: 12px 7px;
                background: #FFFFFF;
                border: 1px solid #DCDEE5;
                border-radius: 2px;
                
                &.is-active {
                    border: 1px solid #3A84FF;
                    position: relative;

                    &::before {
                        content: '';
                        position: absolute;
                        top: 50%;
                        right: -10px;
                        transform: translateY(-50%);
                        border-top: 8px solid transparent;
                        border-bottom: 8px solid transparent;
                        border-left: 10px solid #3A84FF;
                    }
                }
                
                .tool-item-content {
                    height: 100%;
                    display: flex;
                    justify-content: space-between;
                    position: relative;
                    align-items: center;
                    gap: 12px;
                    flex: 1;
                    
                    .tool-info {
                        .tool-name {
                            font-size: 12px;
                            margin-bottom: 3px;
                            color: #4D4F56;
                            margin-bottom: 4px;
                        }
                        
                        .tool-desc {
                            font-size: 12px;
                            color: #979BA5;
                        }
                    }

                    .tool-icon {
                        position: absolute;
                        top: -2px;
                        right: 0;
                        width: 63px;
                        height: 43px;
                    }

                    .tool-icon-command {
                        background: url('../images/command.jpg') no-repeat -6px -6px / auto;
                    }

                    .tool-icon-vnc {
                        background: url('../images/vnc.jpg') no-repeat -6px -16px / auto;
                    }
                }
            }

            .more-guide {
                display: flex;
                align-items: center;
                font-size: 12px;
                color: #3A84FF;
                cursor: pointer;

                svg {
                    vertical-align: middle;
                    margin-right: 6px;
                }
            }
        }
        
        // 右侧详情内容
        .debug-detail {
            flex: 1;
            display: flex;
            flex-direction: column;
            background-color: #F5F7FA;
            
            .debugging {
                width: 84px;
                height: 24px;
                margin-top: 14px;
                line-height: 24px;
                text-align: center;
                font-size: 12px;
                color: #299E56;
                background: #DAF6E5;
            }
            
            .debug-info-box {
                padding: 16px 24px;
                
                .debug-info-item {
                    margin-bottom: 24px;
                    
                    &.vnc-info {
                        display: flex;
                        gap: 40px;
                    }
                    
                    &:last-child {
                        margin-bottom: 0;
                    }
                    
                    .info-label {
                        margin-bottom: 8px;
                        font-size: 12px;
                        color: #4D4F56;
                    }
                    
                    .info-value {
                        display: flex;
                        align-items: center;
                        gap: 8px;
                        
                        code, .password-dots {
                            font-size: 16px;
                            font-weight: 700;
                            color: #4D4F56;
                        }
                        
                        .bk-icon {
                            cursor: pointer;
                            &:hover {
                                color: #699DF4;
                            }
                        }
                    }
                }
            }
        }
    }
    
    .debug-footer {
        display: flex;
        justify-content: center;
        align-items: center;
        padding: 4px 37px;
        margin: 24px 114px;
        cursor: pointer;
        border: 1px solid #EA3636;
        border-radius: 2px;
        background: #FFFFFF;
        font-size: 14px;
        color: #E71818;
        
        svg {
            margin-right: 6px;
            vertical-align: middle;
        }
    }
}
</style>
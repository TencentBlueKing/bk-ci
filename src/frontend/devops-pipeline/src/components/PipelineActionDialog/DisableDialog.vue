<template>
    <bk-dialog
        width="480"
        render-directive="if"
        v-model="value"
        footer-position="center"
        class="lock-dialog"
        @cancel="handleCancel"
    >
        <div class="disable-pipeline-dialog">
            <i
                :class="['bk-icon disable-pipeline-warning-icon', {
                    'icon-exclamation': !lock || pacEnabled,
                    'icon-check-1': lock && !pacEnabled
                }]"
            ></i>

            <!-- 非 PAC 流水线 -->
            <template v-if="!pacEnabled">
                <h3>{{ $t(lock ? 'enablePipelineConfirmTips' : 'disablePipelineConfirmTips') }}</h3>
                <p>{{ $t(lock ? 'enablePipelineConfirmDesc' : 'disablePipelineConfirmDesc') }}</p>
            </template>

            <!-- ↓↓↓ PAC 流水线：以下 4 种场景。场景判断条件（pacYamlDisabled）及禁用人/时间/原因等字段，
                 均为 data 中的占位数据，语义待后端接口确认对应真实字段后再对接 ↓↓↓ -->
            <template v-else-if="!lock && pacYamlDisabled">
                <!-- 场景三：仅代码库 YAML 禁用，无法通过页面启用 -->
                <h3>{{ $t('cannotEnablePipelineTips') }}</h3>
                <p class="disable-pipeline-name">{{ $t('pipeline') }}：{{ pipelineName }}</p>
                <div class="disable-pipeline-tip-box">
                    <i18n
                        tag="span"
                        path="yamlDisablePipelineEnableDesc"
                    >
                        <code>disable-pipeline: true</code>
                        <br />
                    </i18n>
                </div>
                <div class="pac-yaml-file-link">
                    <copy-icon :value="pacYamlFilePath" />
                    <span
                        class="pac-yaml-file-path"
                        :title="pacYamlFilePath"
                        @click="handleCopyYamlPath"
                    >{{ pacYamlFilePath }}</span>
                    <span @click="handleJumpToYaml">
                        <logo
                            name="tiaozhuan"
                            size="12"
                            class="jump-icon"
                        />
                    </span>
                </div>
            </template>
            <template v-else-if="!lock">
                <!-- 场景一：禁用 PAC 流水线，需填写禁用原因 -->
                <h3>{{ $t('disablePipelineConfirmTips') }}</h3>
                <p class="disable-pipeline-name">{{ $t('pipeline') }}：{{ pipelineName }}</p>
                <div class="disable-pipeline-tip-box">
                    {{ $t('disablePipelineConfirmDesc1') }}
                </div>
                <div class="pac-disable-reason">
                    <label class="pac-disable-reason-label">
                        {{ $t('disableReasonLabel') }}
                        <span class="pac-disable-reason-required">*</span>
                    </label>
                    <bk-input
                        v-model="pacDisableForm.reason"
                        :maxlength="120"
                        show-word-limit
                        :placeholder="$t('disableReasonPlaceholder')"
                        @input="pacReasonError = false"
                    />
                    <p
                        v-if="pacReasonError"
                        class="pac-disable-reason-error"
                    >
                        {{ $t('disableReasonRequired') }}
                    </p>
                </div>
            </template>
            <template v-else-if="pacYamlDisabled">
                <!-- 场景四：UI 与 YAML 均被禁用，启用时只解除页面禁用 -->
                <h3>{{ $t('enablePipelineConfirmTips') }}</h3>
                <p class="disable-pipeline-name">{{ $t('pipeline') }}：{{ pipelineName }}</p>
                <div class="disable-pipeline-tip-box">
                    <i18n
                        tag="span"
                        path="enableBothDisablePipelineConfirmDesc"
                    >
                        <code>disable-pipeline: true</code>
                        <br />
                    </i18n>
                </div>
                <div class="pac-yaml-file-link">
                    <copy-icon :value="pacYamlFilePath" />
                    <span
                        class="pac-yaml-file-path"
                        :title="pacYamlFilePath"
                        @click="handleCopyYamlPath"
                    >{{ pacYamlFilePath }}</span>
                    <span @click="handleJumpToYaml">
                        <logo
                            name="tiaozhuan"
                            size="12"
                            class="jump-icon"
                        />
                    </span>
                </div>
            </template>
            <template v-else>
                <!-- 场景二：仅 UI 禁用，启用时展示禁用人/禁用时间/禁用原因 -->
                <h3>{{ $t('enablePipelineConfirmTips') }}</h3>
                <p class="disable-pipeline-name">{{ $t('pipeline') }}：{{ pipelineName }}</p>
                <ul class="pac-disable-info-list">
                    <li>
                        <span class="pac-disable-info-label">{{ $t('disabledByLabel') }}</span>
                        <span class="pac-disable-info-value">{{ pacDisabledByUser }}</span>
                    </li>
                    <li>
                        <span class="pac-disable-info-label">{{ $t('disabledAtLabel') }}</span>
                        <span class="pac-disable-info-value">{{ pacDisabledAt }}</span>
                    </li>
                    <li>
                        <span class="pac-disable-info-label">{{ $t('disableReasonLabel') }}</span>
                        <span class="pac-disable-info-value">{{ pacDisabledReason }}</span>
                    </li>
                </ul>
            </template>
            <!-- ↑↑↑ PAC 流水线占位逻辑结束 ↑↑↑ -->
        </div>
        <footer slot="footer">
            <bk-button
                v-if="!(pacEnabled && !lock && pacYamlDisabled)"
                :loading="disabling"
                theme="primary"
                @click="handleConfirm"
            >
                {{ $t(lock ? 'enable' : 'disable') }}
            </bk-button>
            <bk-button @click="handleCancel">
                {{ $t((pacEnabled && !lock && pacYamlDisabled) ? 'close' : 'cancel') }}
            </bk-button>
        </footer>
    </bk-dialog>
</template>

<script>
    import CopyIcon from '@/components/CopyIcon'
    import { mapActions } from 'vuex'
    import { copyToClipboard } from '@/utils/util'
    import Logo from '@/components/Logo'

    export default {
        components: {
            CopyIcon,
            Logo
        },
        props: {
            pipelineId: String,
            pipelineName: String,
            value: Boolean,
            pacEnabled: Boolean,
            lock: Boolean
        },
        data () {
            return {
                disabling: false,
                // 禁用 PAC 流水线时填写的禁用原因（表单字段）
                pacDisableForm: {
                    reason: ''
                },
                // 提交禁用原因时的校验错误态
                pacReasonError: false,

                // ↓↓↓ TODO: 以下 4 个变量均为占位数据，代表 PAC 流水线 4 种场景判断/展示所需的真实字段，
                // 待后端接口确认对应字段名与取值后，替换为真实数据绑定（不要再使用这里的假数据）。
                // 代码库 YAML 中是否存在 disable-pipeline: true 导致的禁用，用于区分启用时的 3 种子场景
                pacYamlDisabled: false,
                // 已禁用（仅 UI 禁用场景）时展示的禁用人
                pacDisabledByUser: '--',
                // 已禁用（仅 UI 禁用场景）时展示的禁用时间
                pacDisabledAt: '--',
                // 已禁用（仅 UI 禁用场景）时展示的禁用原因
                pacDisabledReason: '--',
                // YAML 禁用场景下展示的文件路径（如 bk-ci/demo-pipeline / .ci/e2e-nightly.yml）
                pacYamlFilePath: 'bk-ci/demo-pipeline / .ci/e2e-nightly.yml',
                // 点击跳转图标时打开的代码库文件链接
                pacYamlFileUrl: ''
                // ↑↑↑ 占位数据结束 ↑↑↑
            }
        },
        methods: {
            ...mapActions('pipelines', ['lockPipeline']),
            handleConfirm () {
                if (this.pacEnabled) {
                    return this.handlePacConfirm()
                }
                return this.disablePipeline()
            },
            // TODO: PAC 流水线的禁用/启用接口后端暂未提供，可能与普通流水线接口不同，待接口确认后再补充真实调用逻辑
            handlePacConfirm () {
                if (!this.lock && !this.pacDisableForm.reason.trim()) {
                    this.pacReasonError = true
                    return
                }
                console.warn('[DisableDialog] PAC 流水线禁用/启用接口待后端确认，当前未执行任何操作', {
                    pipelineId: this.pipelineId,
                    enable: this.lock,
                    reason: this.pacDisableForm.reason
                })
            },
            // 复制 YAML 文件路径
            handleCopyYamlPath () {
                copyToClipboard(this.pacYamlFilePath)
                this.$bkMessage({
                    theme: 'success',
                    message: this.$t('copySuc')
                })
            },
            // 跳转到代码库对应的 YAML 文件
            handleJumpToYaml () {
                if (this.pacYamlFileUrl) {
                    window.open(this.pacYamlFileUrl, '_blank')
                }
            },
            async disablePipeline () {
                try {
                    this.disabling = true
                    await this.lockPipeline({
                        projectId: this.$route.params.projectId,
                        pipelineId: this.pipelineId,
                        enable: this.lock
                    })
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t(this.lock ? 'enableSuc' : 'disableSuc', [this.pipelineName]),
                        limit: 1
                    })
                    this.$nextTick(() => {
                        this.handleCancel()
                        this.$emit('done', this.lock)
                    })
                } catch (error) {
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message || error
                    })
                } finally {
                    this.disabling = false
                }
            },
            handleCancel () {
                this.$emit('input', false)
                this.$emit('close')
            }
        }
    }
</script>

<style lang="scss">
.disable-pipeline-dialog {
    text-align: center;
    .disable-pipeline-warning-icon {
        display: inline-flex;
        width: 42px;
        height: 42px;
        background: #ffa012;
        color: #FFFFFF;
        align-items: center;
        justify-content: center;
        border-radius: 50%;
        font-size: 26px;
        &.icon-check-1 {
            background: #e5f6ea;
            color: #3fc06d;
        }
    }
    .disable-pipeline-name {
        margin: 12px 0 0;
        color: #63656e;
        font-size: 14px;
        text-align: left;
    }
    .disable-pipeline-tip-box {
        margin: 16px 0 0;
        padding: 12px 16px;
        background: #F5F7FA;
        border-radius: 2px;
        color: #63656e;
        font-size: 12px;
        line-height: 20px;
        text-align: left;
        code {
            background: #EAEBF0;
            padding: 0 4px;
            border-radius: 2px;
        }
    }
    .pac-yaml-file-link {
        display: flex;
        align-items: center;
        margin: 12px 0 0;
        font-size: 12px;
        color: #63656e;
        .pac-yaml-file-path {
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            margin: 0 6px;
            cursor: pointer;
            color: #3a84ff;
            &:hover {
                text-decoration: underline;
            }
        }
        .jump-icon {
            fill: #3c96ff;
            vertical-align: bottom;
        }
    }
    .pac-disable-info-list {
        margin: 16px 0 0;
        text-align: left;
        li {
            display: flex;
            line-height: 32px;
            font-size: 12px;
        }
        .pac-disable-info-label {
            width: 70px;
            color: #979ba5;
            flex-shrink: 0;
        }
        .pac-disable-info-value {
            color: #313238;
            word-break: break-all;
        }
    }
    .pac-disable-reason {
        margin: 16px 0 0;
        text-align: left;
        .pac-disable-reason-label {
            display: block;
            margin-bottom: 6px;
            font-size: 12px;
            color: #63656e;
        }
        .pac-disable-reason-required {
            color: #ea3636;
        }
        .pac-disable-reason-error {
            margin: 4px 0 0;
            font-size: 12px;
            color: #ea3636;
            text-align: left;
        }
    }
}
.lock-dialog .bk-dialog-footer {
    border-top: none !important;
    background-color: #fff !important;
    padding: 7px 24px 33px !important;
}
</style>

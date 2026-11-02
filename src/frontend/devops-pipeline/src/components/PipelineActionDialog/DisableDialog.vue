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
            <i class="bk-icon disable-pipeline-warning-icon icon-exclamation"></i>

            <!-- 场景一：仅YAML 禁用，不能直接启用 -->
            <template v-if="!lock && isCurPipelineYamlLocked">
                <h3>{{ $t('cannotEnablePipelineTips') }}</h3>
                <p
                    class="disable-pipeline-name"
                    :title="pipelineName"
                >
                    {{ $t('pipeline') }}：{{ pipelineName }}
                </p>
                <div class="disable-pipeline-tip-box">
                    <i18n
                        tag="span"
                        path="yamlDisablePipelineEnableDesc"
                    >
                        <code>disable-pipeline: true</code>
                        <br />
                    </i18n>
                    <p
                        v-if="yamlInfo?.fileUrl"
                        v-bk-tooltips="yamlInfo?.filePath"
                        class="pac-yaml-modify-link"
                        @click="handleJumpToYaml"
                    >
                        {{ $t('goToModifyLabel') }}
                        <logo
                            name="tiaozhuan"
                            size="12"
                        />
                    </p>
                </div>
            </template>
            <!-- 禁用流水线弹窗展示 -->
            <template v-else-if="!lock">
                <h3>{{ $t('disablePipelineConfirmTips') }}</h3>
                <p
                    class="disable-pipeline-name"
                    :title="pipelineName"
                >
                    {{ $t('pipeline') }}：{{ pipelineName }}
                </p>
                <div class="disable-pipeline-tip-box">
                    {{ $t('disablePipelineConfirmDesc1') }}
                </div>
                <div class="pac-disable-reason">
                    <label class="pac-disable-reason-label">
                        {{ $t('disableReasonLabel') }}
                        <span class="pac-disable-reason-required">*</span>
                    </label>
                    <bk-input
                        v-model="disableReason"
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
            <!-- 场景二：UI 与 YAML 均被禁用，启用时只解除页面禁用 -->
            <template v-else-if="isCurPipelineYamlLocked">
                <h3>{{ $t('enablePipelineConfirmTips') }}</h3>
                <p
                    class="disable-pipeline-name"
                    :title="pipelineName"
                >
                    {{ $t('pipeline') }}：{{ pipelineName }}
                </p>
                <div class="disable-pipeline-tip-box">
                    <i18n
                        tag="span"
                        path="enableBothDisablePipelineConfirmDesc"
                    >
                        <code>disable-pipeline: true</code>
                        <br />
                    </i18n>
                    <p
                        v-if="yamlInfo?.fileUrl"
                        v-bk-tooltips="yamlInfo?.filePath"
                        class="pac-yaml-modify-link"
                        @click="handleJumpToYaml"
                    >
                        {{ $t('goToModifyLabel') }}
                        <logo
                            name="tiaozhuan"
                            size="12"
                        />
                    </p>
                </div>
            </template>
            <!-- 场景三：仅 UI 禁用，启用时展示禁用人/禁用时间/禁用原因 -->
            <template v-else>
                <h3>{{ $t('enablePipelineConfirmTips') }}</h3>
                <p
                    class="disable-pipeline-name"
                    :title="pipelineName"
                >
                    {{ $t('pipeline') }}：{{ pipelineName }}
                </p>
                <ul
                    v-if="hasDisabledInfo"
                    class="pac-disable-info-list"
                >
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
        </div>
        <footer slot="footer">
            <bk-button
                v-if="!onlyYamlDisabledScene"
                :loading="disabling"
                theme="primary"
                @click="handleConfirm"
            >
                {{ $t(lock ? 'enable' : 'disable') }}
            </bk-button>
            <bk-button @click="handleCancel">
                {{ $t(onlyYamlDisabledScene ? 'close' : 'cancel') }}
            </bk-button>
        </footer>
    </bk-dialog>
</template>

<script>
    import { mapActions } from 'vuex'
    import { convertTime } from '@/utils/util'
    import Logo from '@/components/Logo'

    export default {
        components: {
            Logo
        },
        props: {
            pipelineId: String,
            pipelineName: String,
            value: Boolean,
            yamlInfo: Object,
            // UI 禁用态
            lock: Boolean,
            // 代码库 YAML 中声明 disable-pipeline: true 导致的禁用
            yamlLocked: Boolean,
            // UI 禁用人
            lockUser: String,
            // UI 禁用时间
            lockTime: Number,
            // UI 禁用原因
            lockReason: String
        },
        data () {
            return {
                disabling: false,
                // 禁用 PAC 流水线时填写的禁用原因（表单字段）
                disableReason: '',
                pacReasonError: false
            }
        },
        computed: {
            // 代码库 YAML 中声明 disable-pipeline: true 导致的禁用
            isCurPipelineYamlLocked () {
                return this.yamlLocked
            },
            // 存量禁用数据没有禁用人/时间/原因
            hasDisabledInfo () {
                return !!(this.lockUser || this.lockTime || this.lockReason)
            },
            // 已禁用（仅 UI 禁用场景）时展示的禁用人
            pacDisabledByUser () {
                return this.lockUser || '--'
            },
            // 已禁用（仅 UI 禁用场景）时展示的禁用时间
            pacDisabledAt () {
                return convertTime(this.lockTime)
            },
            // 已禁用（仅 UI 禁用场景）时展示的禁用原因
            pacDisabledReason () {
                return this.lockReason || '--'
            },
            // 点击"前往修改"打开的代码库 YAML 文件链接
            pacYamlFileUrl () {
                return this.yamlInfo?.fileUrl || ''
            },
            // 流水线仅被代码库 YAML 禁用，无法在页面启用，只展示关闭按钮
            onlyYamlDisabledScene () {
                return !this.lock && this.isCurPipelineYamlLocked
            }
        },
        methods: {
            ...mapActions('pipelines', ['lockPipeline']),
            ...mapActions('atom', ['requestPipelineSummary']),
            handleConfirm () {
                // 禁用时需要填写禁用原因
                if (!this.lock && !this.disableReason.trim()) {
                    this.pacReasonError = true
                    return
                }
                return this.disablePipeline()
            },
            // 跳转到代码库对应的 YAML 文件
            handleJumpToYaml () {
                if (this.pacYamlFileUrl) {
                    window.open(this.pacYamlFileUrl, '_blank')
                }
            },
            async disablePipeline () {
                const lock = this.lock
                try {
                    this.disabling = true
                    await this.lockPipeline({
                        projectId: this.$route.params.projectId,
                        pipelineId: this.pipelineId,
                        enable: lock,
                        params: {
                            // 禁用时传禁用原因
                            ...(!lock ? { lockReason: this.disableReason } : {})
                        }
                    })
                    // 详情页需重新拉取详情，刷新禁用人/禁用时间/禁用原因等展示数据
                    if (this.$route.params.pipelineId) {
                        await this.requestPipelineSummary(this.$route.params)
                    }
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t(lock ? 'enableSuc' : 'disableSuc', [this.pipelineName]),
                        limit: 1
                    })
                    this.$nextTick(() => {
                        this.handleCancel()
                        this.$emit('done', lock)
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
                this.pacReasonError = false
                this.disableReason = ''
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
        padding-left: 16px;
        color: #63656e;
        font-size: 14px;
        text-align: left;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }
    .disable-pipeline-tip-box {
        text-align: left;
        margin-top: 12px;
        padding: 12px 16px;
        background: #f5f7fa;
        border-radius: 2px;
        code {
            padding: 0 4px;
            background: rgba(0, 0, 0, 0.06);
            border-radius: 2px;
            font-family: Consolas, Monaco, monospace;
            white-space: nowrap;
        }
    }
    .pac-yaml-modify-link {
        display: flex;
        align-items: center;
        gap: 4px;
        width: fit-content;
        margin-top: 8px;
        color: #3a84ff;
        text-decoration: none;
        white-space: nowrap;
        cursor: pointer;
    }
    .pac-disable-info-list {
        display: flex;
        flex-direction: column;
        gap: 8px;
        margin-top: 20px;
        padding: 8px 16px;
        border-radius: 2px;
        background: #f5f7fa;
        li {
            display: flex;
            align-items: flex-start;
            gap: 8px;
        }
        .pac-disable-info-label {
            flex: 0 0 64px;
            color: #979ba5;
        }
        .pac-disable-info-value {
            color: #313238;
            text-align: left;
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
.lock-dialog h3 {
    font-size: 20px;
    line-height: 32px;
    font-weight: 500;
    color: #3c3c43;
}
</style>

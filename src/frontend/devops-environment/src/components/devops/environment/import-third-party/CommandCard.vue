<template>
    <div class="command-section">
        <p class="form-block-title">
            {{ $t('environment.installSession.commandTitle') }}
            <bk-popover
                placement="top"
                :max-width="360"
                ext-cls="import-form-tip"
            >
                <i class="devops-icon icon-info-circle hint-icon"></i>
                <template #content>
                    <ol class="command-tip-list">
                        <li>{{ $t('environment.installSession.commandTip1') }}</li>
                        <li>{{ $t('environment.installSession.commandTip2') }}</li>
                    </ol>
                </template>
            </bk-popover>
            <span class="section-note">
                {{ expired ? $t('environment.installSession.commandExpired') : $t('environment.installSession.commandExpireNote', { time: expireTime }) }}
            </span>
            <span
                v-if="expired"
                class="section-regen"
            >
                <bk-button
                    text
                    theme="primary"
                    :loading="regenerating"
                    @click="$emit('regenerate')"
                >
                    {{ $t('environment.installSession.regenerate') }}
                </bk-button>
            </span>
        </p>

        <div
            class="command-card"
            :class="{ 'is-expired': expired }"
        >
            <!--
        「非系统路径」这个约束并进步骤一，不再单占一段概述——原先那段说的就是步骤 1 + 步骤 2，
        整段只有这个约束是步骤里没有的
      -->
            <template v-if="!isWindows">
                <i18n
                    path="environment.installSession.stepCreateDir"
                    tag="p"
                    class="step-line"
                >
                    <span
                        place="warn"
                        class="strong"
                    >{{ $t('environment.installSession.notSystemPath') }}</span>
                </i18n>
                <p class="step-line">{{ $t('environment.installSession.stepRunCommand') }}</p>
            </template>
            <template v-else>
                <p class="step-line">{{ $t('environment.installSession.stepAdminPowershell') }}</p>
                <i18n
                    path="environment.installSession.stepCreateDirWin"
                    tag="p"
                    class="step-line"
                >
                    <span
                        place="warn"
                        class="strong"
                    >{{ $t('environment.installSession.notSystemPath') }}</span>
                </i18n>
                <p class="step-line">{{ $t('environment.installSession.stepRunCommandWin') }}</p>
            </template>

            <div class="cmd-block">
                <code class="cmd-text">{{ command }}</code>
                <bk-button
                    v-if="!expired"
                    text
                    theme="primary"
                    class="copy-btn"
                    @click="$emit('copy')"
                >
                    {{ $t('environment.installSession.copy') }}
                </bk-button>
            </div>

            <!-- devops_agent_{agent_id} 是服务名格式（字面量），不走 i18n 插值，前后文案拆两个 key -->
            <p
                v-if="isWindows && installType === 'SERVICE'"
                class="step-line"
            >
                {{ $t('environment.installSession.stepServiceNotePre') }}devops_agent_&#123;agent_id&#125;{{ $t('environment.installSession.stepServiceNoteSuf') }}
            </p>
        </div>
    </div>
</template>

<script>
    /**
     * 导入第三方构建机弹窗 · 第二步：安装命令卡片（含步骤说明与复制按钮）。
     * 复制动作（clipboard API 兜底）由父组件处理，本组件只负责展示。
     */
    export default {
        name: 'CommandCard',
        props: {
            /** 安装命令全文 */
            command: {
                type: String,
                default: ''
            },
            isWindows: {
                type: Boolean,
                default: false
            },
            /** Windows 安装模式 SERVICE / TASK */
            installType: {
                type: String,
                default: 'SERVICE'
            },
            /** 命令失效时间（展示文案） */
            expireTime: {
                type: String,
                default: ''
            },
            /** 安装会话已过期：命令置灰、隐藏复制，提供「重新生成」 */
            expired: {
                type: Boolean,
                default: false
            },
            /** 重新生成请求进行中 */
            regenerating: {
                type: Boolean,
                default: false
            }
        }
    }
</script>

<style lang="scss" scoped>
    /* 区块标题栏（与 ImportConfigForm 的同款式样；间距规则改为组件内自管） */
    .form-block-title {
        display: flex;
        align-items: center;
        gap: 8px;
        box-sizing: border-box;
        /* 上方是配置摘要（16px），下方紧跟命令卡片（8px） */
        margin: 16px 0 8px;
        padding: 0 16px;
        border-radius: 2px;
        background: #f0f1f5;
        font-size: 14px;
        font-weight: 400;
        line-height: 30px;
        color: #313238;
    }
    .hint-icon {
        flex: none;
        font-size: 14px;
        color: #979ba5;
        cursor: pointer;
    }
    .section-note {
        flex: none;
        margin-left: auto;
        font-size: 12px;
        font-weight: 400;
        line-height: 20px;
        color: #979ba5;
    }
    .section-regen {
        flex: none;
        font-size: 12px;
        line-height: 20px;
    }
    .command-card {
        padding: 16px;
        background: #fff;
        border: 1px solid #dcdee5;
        border-radius: 2px;

        &.is-expired {
            /* 置灰但保持可读：用户需要对照旧命令确认是哪条过期了 */
            opacity: 0.5;
        }

        .step-line {
            margin: 0 0 8px;
            text-align: left;

            &:has(+ .cmd-block) {
                margin-bottom: 4px;
            }
            &:last-child {
                margin-bottom: 0;
            }
        }
        .cmd-block + .step-line {
            margin-top: 8px;
        }
    }
    .step-line {
        margin: 12px 0 8px;
        padding-left: 16px;
        text-indent: -16px;
        font-size: 12px;
        color: #313238;
        line-height: 20px;
    }
    .strong {
        font-weight: 700;
        color: #313238;
    }
    .cmd-block {
        position: relative;
        padding: 10px 60px 10px 12px;
        background: #f5f7fa;
        border-radius: 2px;
    }
    .cmd-text {
        font-family: Menlo, Monaco, Consolas, monospace;
        font-size: 12px;
        color: #313238;
        line-height: 20px;
        word-break: break-all;
    }
    .copy-btn {
        position: absolute;
        top: 50%;
        right: 12px;
        transform: translateY(-50%);
        font-size: 12px;
    }
</style>

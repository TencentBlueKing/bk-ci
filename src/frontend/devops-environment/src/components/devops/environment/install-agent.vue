<template>
    <bk-sideslider
        ext-cls="install-agent-side"
        :is-show.sync="isShow"
        :width="600"
    >
        <div class="sideslider-title" slot="header">
            {{ $t('environment.installGseAgent') }}
            <span class="node-title">{{ $t('environment.nodeTitle') }}: 10.20.65.48</span>
        </div>
        <!-- <div slot="content">
            <bk-alert type="info" :title="$t('environment.installAgentTips')" closable></bk-alert>
            <bk-form
                class="mt20"
                :label-width="labelWidth"
                :model="formData"
            >
                <bk-form-item
                    :label="$t('environment.nodeInfo.os')"
                    :required="true"
                    property="os"
                >
                    <bk-radio-group v-model="formData.os">
                        <bk-radio value="linux" class="mr20">Linux</bk-radio>
                        <bk-radio value="windows" class="mr20">Windows</bk-radio>
                        <bk-radio value="macOs">macOs</bk-radio>
                    </bk-radio-group>
                </bk-form-item>
                <bk-form-item
                    :label="$t('environment.loginAccount')"
                    :required="true"
                    property="loginAccount"
                >
                    <bk-input
                        v-model="formData.account"
                        :placeholder="$t('environment.accountPlaceholder')"
                    />
                </bk-form-item>
                <bk-form-item
                    :label="$t('environment.oauthMethod')"
                    :required="true"
                    property="oauthMethod"
                >
                    <bk-radio-group v-model="formData.oauthMethod">
                        <bk-radio value="password" class="mr20">{{ $t('environment.password') }}</bk-radio>
                        <bk-radio value="key" class="mr20">{{ $t('environment.key') }}</bk-radio>
                    </bk-radio-group>
                </bk-form-item>
                <bk-form-item
                    v-if="formData.oauthMethod === 'password'"
                    :label="$t('environment.loginPassword')"
                    :required="true"
                    property="loginPassword"
                >
                    <bk-input
                        v-model="formData.loginPassword"
                        :placeholder="$t('environment.passwordPlaceholder')"
                    />
                    <div class="password-tips">{{ $t('environment.passwordTips') }}</div>
                </bk-form-item>
                <bk-form-item
                    v-else
                    :label="$t('environment.keyFile')"
                    :required="true"
                    property="keyFile"
                >
                    <bk-upload
                        ext-cls="upload-file-btn"
                        theme="button"
                        :tip="$t('environment.uploadTips')"
                    >
                    </bk-upload>
                    <div class="keyFile-tips">{{ $t('environment.keyFileTips') }}</div>
                </bk-form-item>
            </bk-form>
        </div> -->
        <div slot="content">
            <div class="install-status">
                <!-- <template>
                    <Icon class="icon" name="loading" />
                    <span>{{ $t('environment.installingTips') }}</span>
                </template>
                <template>
                    <i class="bk-icon import-status-icon icon-check-1 success icon"></i>
                    <span>{{ $t('environment.installSuccessTips') }}</span>
                </template> -->
                <template>
                    <i class="bk-icon import-status-icon icon-close error icon"></i>
                    <span>{{ $t('environment.installFailTips') }}</span>
                    <bk-button text class="ml10">{{ $t('environment.retry') }}</bk-button>
                </template>
            </div>
            <div class="log-wrapper">
                <div v-once id="executeScriptLog" style="height: 100%;" />
            </div>
        </div>
        <div slot="footer">
            <bk-button class="ml20" theme="primary">
                {{ $t('environment.confirm') }}
            </bk-button>
            <bk-button>
                {{ $t('environment.cancel') }}
            </bk-button>
        </div>
    </bk-sideslider>
</template>

<script>
    import ace from 'ace-builds'
    import 'ace/mode-text'
    import 'ace/theme-monokai'
    import 'ace/ext-searchbox'
    export default {
        data () {
            return {
                isZH: true,
                isShow: false,
                labelWidth: 100,
                formData: {
                    oauthMethod: 'password'
                }
            }
        },
        created () {
            this.isZH = ['zh-CN', 'zh', 'zh_cn'].includes(document.documentElement.lang)
            this.labelWidth = this.isZH ? 100 : 130

            this.initEditor()
        },
        methods: {
            initEditor () {
                const editor = ace.edit('executeScriptLog')
                editor.getSession().setMode('ace/mode/text')
                editor.setTheme('ace/theme/monokai')
                editor.setOptions({
                    wrapBehavioursEnabled: true,
                    copyWithEmptySelection: true,
                    useElasticTabstops: true,
                    printMarginColumn: false,
                    printMargin: 80,
                    showPrintMargin: false,
                    scrollPastEnd: 0.05,
                    fixedWidthGutter: true
                })
                editor.$blockScrolling = Infinity
                editor.setReadOnly(true)
                const editorSession = editor.getSession()
                // 自动换行时不添加缩进
                editorSession.$indentedSoftWrap = false
                editorSession.on('changeScrollTop', (scrollTop) => {
                    const {
                        height,
                        maxHeight
                    } = editor.renderer.layerConfig
                    this.isWillAutoScroll = height + scrollTop + 30 >= maxHeight
                })
                this.editor = editor
                this.$once('hook:beforeDestroy', () => {
                    editor.destroy()
                    editor.container.remove()
                })
            }
        }
    }
</script>

<style lang="scss" scoped>
.install-agent-side {
    ::v-deep .bk-sideslider-content {
        height: calc(100% - 114px) !important;
        padding: 20px;
    }
    .sideslider-title {
        display: flex;
        align-items: center;
    }
    .node-title {
        font-size: 12px;
        padding-left: 15px;
    }
    .password-tips,
    .keyFile-tips {
        font-size: 12px;
        color: #c7c9d0;
    }
    .install-status {
        display: flex;
        align-items: center;
        height: 80px;
        padding: 0 20px;
        background-color: #eef8ff;
        border: 1px solid #c5deff;
        .icon {
            margin-right: 20px;
        }
    }
    
}
</style>

<style lang="scss">
    .import-status-icon {
        width: 38px;
        height: 38px;
        line-height: 38px;
        font-size: 36px;
        border-radius: 50%;
        &.success {
            background-color: #e5f6ea;
            color: #3fc06d;
        }
        &.error {
            background-color: #fdd;
            color: #ea3636;
        }
    }
</style>

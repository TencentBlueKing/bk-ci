<template>
    <section class="basic-setting">
        <!-- 授权 -->
        <div class="form-item">
            <div class="label">
                {{ $t('codelib.auth') }}
                <Icon name="helper" size="14" class="help-icon" />
            </div>
            <div class="content">
                <div class="auth">
                    <Icon name="check-circle" size="14" class="icon-success" />
                    OAUTH@fayewang
                    <a class="reset-bth">{{ $t('codelib.resetAuth') }}</a>
                </div>
            </div>
        </div>
        <!-- PAC 模式 -->
        <div class="form-item">
            <div class="label">
                {{ $t('codelib.PACmode') }}
            </div>
            <p class="pac-tips">{{ $t('codelib.pacTips') }}</p>
            <div class="content">
                <div class="pac-mode">
                    <bk-switcher v-model="isEnabled" theme="primary"></bk-switcher>
                    <div class="pac-enable">
                        {{ isEnabled ? $t('codelib.isOnPAC') : $t('codelib.isOffPAC') }}
                    </div>
                </div>
            </div>
        </div>
        <!-- 通用设置 -->
        <div class="form-item">
            <div class="label">
                {{ $t('codelib.common') }}
                <Icon name="helper" size="14" class="edit-icon" />
            </div>
            <div class="content">
                <div class="merge-request">
                    {{ $t('codelib.blockingMergeRequest') }}
                    <Icon name="helper" size="14" class="help-icon" />
                    <p class="request-result">{{ true ? $t('codelib.yes') : $t('codelib.no') }}</p>
                </div>
            </div>
        </div>
        <!-- 历史信息 -->
        <div class="form-item">
            <div class="label">
                {{ $t('codelib.historyInfo') }}
            </div>
            <div class="history-content">
                <div class="history-item">
                    <span class="label">{{ $t('codelib.creator') }}</span>
                    <span class="value">hwweng</span>
                </div>
                <div class="history-item">
                    <span class="label">{{ $t('codelib.creator') }}</span>
                    <span class="value">hwweng</span>
                </div>
                <div class="history-item">
                    <span class="label">{{ $t('codelib.creator') }}</span>
                    <span class="value">hwweng</span>
                </div>
                <div class="history-item">
                    <span class="label">{{ $t('codelib.creator') }}</span>
                    <span class="value">hwweng</span>
                </div>
            </div>
        </div>
    </section>
</template>
<script>
    export default {
        name: 'basicSetting',
        props: {
            
        },
        data () {
            return {
                isEnabled: false
            }
        },
        computed: {
           
        },
        watch: {
           
        },
        created () {
        },
        methods: {
           
        }
    }
</script>
<style lang='scss' scoped>
    .basic-setting {
        .form-item {
            margin-bottom: 40px;

            .label {
                display: flex;
                align-items: center;
                font-weight: 700;
                font-size: 14px;
                color: #63656E;
            }
            .content,
            .history-content {
                margin-top: 16px;
                font-size: 12px;
            }
            .history-content {
                max-width: 1000px;
            }
            .pac-tips {
                margin-top: 8px;
                font-size: 12px;
                color: #979BA5;
            }
            .pac-mode {
                display: flex;
                align-items: center;
            }
            .edit-icon {
                margin-left: 18px;
            }
            .pac-enable {
                margin: 0 24px 0 8px;
            }
            .help-icon {
                cursor: pointer;
                margin-left: 8px;
                color: #979BA5;
            }
            .auth {
                display: inline-block;
                height: 32px;
                line-height: 32px;
                padding: 0 16px;
                background: #F5F7FA;
                border-radius: 16px;
            }
            .icon-success {
                position: relative;
                top: 2px;
                color: #3FC06D;
            }
            .reset-bth {
                &::before {
                    content: "";
                    position: relative;
                    display: inline-block;
                    top: 2px;
                    width: 1px;
                    height: 16px;
                    margin: 0 16px;
                    background: #DCDEE5;
                }
            }
            .merge-request {
                display: flex;
                align-items: center;
                font-size: 12px;
                color: #979BA5;
            }
            .request-result {
                font-size: 12px;
                color: #63656E;
                margin-left: 18px;
            }
        }
        .history-item {
            display: inline-flex;
            min-width: 200px;
            max-width: 300px;
            margin-right: 200px;
            margin-bottom: 16px;
            .label {
                width: 120px;
                font-size: 12px;
                color: #979BA5;
            }
            .content {
                font-size: 12px;
                color: #63656E;
            }
        }
    }
</style>

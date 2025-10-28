<template>
    <bk-form
        ref="form"
        :label-width="120"
        v-bkloading="{ isLoading: fetchingCodelibDetail }"
        :model="codelib"
        :rules="formRules"
    >
        <bk-form-item
            :label="$t('codelib.authType')"
            :required="true"
            property="authType"
        >
            <bk-radio-group
                v-model="codelib.credentialType"
                @change="authTypeChangeAsCustom(codelib)"
            >
                <bk-radio
                    v-for="auth in providerConfig.credentialTypeList"
                    :key="auth.credentialType"
                    class="mr20"
                    :value="auth.credentialType"
                >
                    {{ auth.name }}
                </bk-radio>
            </bk-radio-group>

            <div
                class="codelib-oauth"
                v-if="!oAuth.hasPower && isOAUTH"
            >
                <bk-button
                    theme="primary"
                    @click="openValidate"
                >
                    {{ $t('codelib.oauthCert') }}
                </bk-button>
                <div class="oauth-tips">
                    <p>{{ $t('codelib.尚未授权，请先点击按钮授权。') }}</p>
                    <p>{{ $t('codelib.此授权用于平台和代码库进行交互，涉及如下功能：') }}</p>
                    <p>1.{{ $t('codelib.注册 Webhook 到代码库，用于事件触发场景') }}</p>
                    <p>2.{{ $t('codelib.回写提交检测状态到代码库，用于代码库支持 checker 拦截合并请求场景') }}</p>
                    <p>3.{{ $t('codelib.流水线中 Checkout 代码') }}</p>
                    <p>{{ $t('codelib.需拥有代码库注册 Webhook 权限') }}</p>
                </div>
            </div>
        </bk-form-item>
        <template v-if="oAuth.hasPower && isOAUTH">
            <bk-form-item
                :label="$t('codelib.authorizeAccount')"
                :required="true"
                property="userName"
            >
                <bk-select
                    v-model="codelib.userName"
                    :clearable="false"
                >
                    <bk-option
                        v-for="user in oauthUserList"
                        :key="user.username"
                        :id="user.username"
                        :name="user.username"
                    >
                    </bk-option>
                </bk-select>
            </bk-form-item>
            <bk-form-item
                :label="$t('codelib.address')"
                :required="true"
                property="url"
                error-display-type="normal"
            >
                <bk-select
                    v-model="codelib.url"
                    v-bind="selectComBindData"
                >
                    <bk-option
                        v-for="option in oAuth.project"
                        :key="option.httpUrl"
                        :id="option.httpUrl"
                        :name="option.httpUrl"
                    >
                    </bk-option>
                </bk-select>
            </bk-form-item>
            <bk-form-item
                :label="$t('codelib.aliasName')"
                :required="true"
                property="aliasName"
                error-display-type="normal"
            >
                <bk-input
                    v-model.trim="codelib.aliasName"
                    :maxlength="60"
                    :placeholder="$t('codelib.aliasNameEnter')"
                >
                </bk-input>
            </bk-form-item>
            
            <bk-form-item
                v-if="providerConfig.pacEnabled"
                :label="$t('codelib.PACmode')"
            >
                <div class="pac-item">
                    <bk-switcher
                        v-model="codelib.enablePac"
                        :disabled="!!pacProjectName || !codelib.url"
                        theme="primary"
                        size="large"
                    ></bk-switcher>
                    <span
                        v-if="pacProjectName"
                        class="tips"
                    >
                        <span class="pac-warning-icon">
                            <i class="devops-icon icon-exclamation" />
                        </span>
                        {{ $t('codelib.当前代码库已在【】项目中开启 PAC 模式', [pacProjectName]) }}
                    </span>
                </div>
                <div class="pac-tips">
                    <p>1. {{ $t('codelib.同一个代码库可以关联到多个蓝盾项目，但仅支持在一个蓝盾项目下开启 PAC (Pipeline As Code) 模式') }}</p>
                    <p>
                        2. {{ $t('codelib.PAC 模式下，使用代码库 ci 目录下的 YAML 文件编排流水线，且 YAML 文件变更将自动同步到对应的蓝盾流水线。') }}
                        <bk-popover
                            width="380"
                            placement="right-end"
                        >
                            <a>{{ $t('codelib.查看同步规则') }}</a>
                            <div slot="content">
                                <p>{{ $t('codelib.同步规则：') }}</p>
                                <p>- {{ $t('codelib.新增 YAML 时，当前项目下将新增一条对应的流水线') }}</p>
                                <p>- {{ $t('codelib.修改 YAML 后，新触发自动以最新的 YAML 配置为准') }}</p>
                            </div>
                        </bk-popover>
                    </p>
                </div>
            </bk-form-item>
        </template>
        <template v-else-if="!isOAUTH">
            <bk-form-item
                :label="$t('codelib.address')"
                :required="true"
                property="url"
                error-display-type="normal"
            >
                <bk-input
                    :placeholder="urlPlaceholder"
                    v-model.trim="codelib.url"
                >
                </bk-input>
                <span
                    class="error-tips"
                    v-if="urlErrMsg"
                >
                    {{ urlErrMsg }}
                </span>
            </bk-form-item>
            <bk-form-item
                :label="$t('codelib.aliasName')"
                :required="true"
                property="aliasName"
                error-display-type="normal"
            >
                <bk-input
                    v-model.trim="codelib.aliasName"
                    :maxlength="60"
                    :placeholder="$t('codelib.aliasNameEnter')"
                >
                </bk-input>
            </bk-form-item>
            <bk-form-item
                :label="$t('codelib.codelibCredential')"
                :required="true"
                property="credentialId"
                error-display-type="normal"
            >
                <bk-select
                    v-model="codelib.credentialId"
                    :loading="isLoadingTickets"
                    searchable
                    :clearable="false"
                    name="credentialId"
                    class="codelib-credential-selector"
                    :placeholder="$t('codelib.credentialPlaceholder')"
                    @toggle="refreshTicket"
                >
                    <bk-option
                        v-for="(option, index) in credentialList"
                        :key="option.credentialId"
                        :id="option.credentialId"
                        :name="option.credentialId"
                    >
                        <span
                            class="name"
                            :title="option.credentialId"
                        >
                            {{ option.credentialId }}
                        </span>
                        <i
                            class="devops-icon icon-edit2 cre-icon"
                            @click.stop="goToEditCre(index)"
                        >
                        </i>
                    </bk-option>
                </bk-select>
                <span
                    class="add-cred-btn"
                    @click="addCredential"
                >{{ $t('codelib.new') }}</span>
            </bk-form-item>
        </template>
    </bk-form>
</template>

<script>
    import dialogMixin from './mixin.js'
    export default {
        mixins: [dialogMixin]
    }
</script>

<style lang="scss" scoped>
    .codelib-oauth {
        margin-top: 20px;
        .refresh-oauth {
            color: #3A84FF;
            cursor: pointer;
        }
        .refresh-icon {
            margin-left: 20px;
        }
        .oauth-tips {
            margin-top: 16px;
            font-size: 12px;
            color: #979BA5;
        }
    }
    .pac-item {
        .tips {
            position: relative;
            top: 2px;
            margin-left: 15px;
            font-size: 12px;
        }
    }

    .pac-warning-icon {
        display: inline-flex;
            align-items: center;
            justify-content: center;
            background-color: #FF9C01;
            color: #fff;
            width: 16px;
            height: 16px;
            border-radius: 50%;
            flex-shrink: 0;
    }
    .pac-tips {
        margin-top: 10px;
        font-size: 12px;
        color: #979ba5ff;
    }
    
</style>

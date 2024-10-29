<template>
    <!-- v-bkloading="{ isLoading: fetchingCodelibDetail }" -->
    <bk-form
        ref="form"
        :label-width="120"
        :model="codelib"
        :rules="formRules"
    >
        <bk-form-item
            :label="$t('codelib.authType')"
            :required="true"
            property="authType"
        >
            <bk-radio-group
                v-model="codelib.authType"
            >
                <bk-radio
                    class="mr20"
                    value="HTTPS"
                >
                    {{ $t('codelib.用户名+密码') }}
                </bk-radio>
            </bk-radio-group>
        </bk-form-item>
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
        <!-- <bk-form-item
            :label="$t('codelib.authType')"
            :required="true"
            property="authType"
        >
            <bk-radio-group
                v-model="codelib.authType"
            >
                <bk-radio
                    class="mr20"
                    value="OAUTH"
                >
                    OAUTH
                </bk-radio>
            </bk-radio-group>

            <div class="codelib-oauth" v-if="!hasPower">
                <bk-button
                    theme="primary"
                    @click="openValidate"
                >
                    {{ $t('codelib.oauthCert') }}
                </bk-button>
                <div class="oauth-tips">
                    <p>{{ $t('codelib.尚未授权，请先点击按钮授权。') }}</p>
                    <p>{{ $t('codelib.此授权用于平台和工蜂进行交互，用于如下场景：') }}</p>
                    <p>1.{{ $t('codelib.注册 Webhook 到工蜂') }}</p>
                    <p>2.{{ $t('codelib.回写提交检测状态到工蜂') }}</p>
                    <p>3.{{ $t('codelib.流水线中 Checkout 代码') }}</p>
                    <p>{{ $t('codelib.需拥有代码库 Devloper 及以上权限，建议使用公共账号授权') }}</p>
                </div>
            </div>
        </bk-form-item>
        <template v-if="hasPower">
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
        </template> -->
    </bk-form>
</template>

<script>
    import dialogMixin from './mixin.js'
    export default {
        mixins: [dialogMixin]
    }
</script>

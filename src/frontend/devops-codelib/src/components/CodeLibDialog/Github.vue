<template>
    <bk-form
        ref="form"
        v-bkloading="{ isLoading: fetchingCodelibDetail }"
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
                    value="OAUTH"
                >
                    OAUTH
                </bk-radio>
            </bk-radio-group>
            <div
                class="codelib-oauth"
                v-if="!oAuth.hasPower"
            >
                <bk-button
                    theme="primary"
                    @click="openValidate"
                >
                    {{ $t('codelib.oauthCert') }}
                </bk-button>
                <div class="oauth-tips">
                    <p>{{ $t('codelib.尚未授权，请先点击按钮授权。') }}</p>
                    <p>{{ $t('codelib.此授权用于平台和 Github 进行交互，用于如下场景：') }}</p>
                    <p>1.{{ $t('codelib.回写 Commit statuses 到 Github') }}</p>
                    <p>2.{{ $t('codelib.流水线中 Checkout 代码') }}</p>
                    <p>{{ $t('codelib.需拥有代码库 Push 权限') }}</p>
                </div>
            </div>
        </bk-form-item>
        <template v-if="oAuth.hasPower">
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
    
</style>

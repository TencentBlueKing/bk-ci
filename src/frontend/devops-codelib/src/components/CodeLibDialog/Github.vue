<template>
    <bk-form
        ref="form"
        v-bkloading="{ isLoading: saving || fetchingCodelibDetail }"
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
                @change="authTypeChange(codelib)"
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
                <span
                    v-if="showRefreshBtn"
                    class="refresh-oauth"
                    @click="handleRefreshOAUTH"
                >
                    <Icon
                        name="refresh"
                        size="12"
                        class="refresh-icon"
                    />
                    {{ $t('codelib.refreshOAUTH') }}
                </span>
                <div class="oauth-tips">
                    <p>{{ $t('codelib.尚未授权，请先点击按钮授权。推荐使用公共账号授权。') }}</p>
                    <p>{{ $t('codelib.此授权用于平台和代码库进行交互：') }}</p>
                    <p>1.{{ $t('codelib.添加代码库触发器时往工蜂注册回调事件（需拥有代码库开发者及以上权限）') }}</p>
                    <p>2.{{ $t('codelib.代码库事件触发时，获取 YAML 配置文件（PAC 模式下）') }}</p>
                    <p>3.{{ $t('codelib.拉代码插件不指定凭据时，默认使用此授权同步代码到构建机') }}</p>
                    <p>4.{{ $t('codelib.流水线执行时，回写流水线执行状态到代码库') }}</p>
                </div>
            </div>
        </bk-form-item>
        <template v-if="hasPower">
            <bk-form-item
                :label="$t('codelib.codelibUrl')"
                :required="true"
                property="url"
                error-display-type="normal"
            >
                <bk-select
                    v-model="codelib.url"
                    v-bind="selectComBindData"
                    class="codelib-credential-selector"
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

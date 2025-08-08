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
    </bk-form>
</template>

<script>
    import dialogMixin from './mixin.js'
    export default {
        mixins: [dialogMixin]
    }
</script>

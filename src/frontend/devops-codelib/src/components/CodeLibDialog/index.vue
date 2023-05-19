<template>
    <bk-dialog
        class="codelib-operate-dialog"
        v-model="isShow"
        :width="width"
        :padding="padding"
        :quick-close="false"
        :loading="loading"
        @confirm="submitCodelib"
        @cancel="handleCancel"
    >
        <h3 slot="header" class="bk-dialog-title">{{ title }}</h3>
        <component ref="form" :is="comName"></component>
        <form style="display: none" class="bk-form" v-bkloading="{ isLoading: saving || fetchingCodelibDetail }">
            <div class="bk-form-item is-required" v-if="isGit || isGitLab">
                <label class="bk-label">{{ $t('codelib.codelibMode') }}:</label>
                <bk-radio-group v-model="codelib.authType" @change="authTypeChange(codelib)" class="bk-form-content form-radio">
                    <bk-radio value="OAUTH" v-if="isGit">OAUTH</bk-radio>
                    <bk-radio value="SSH" v-if="!isTGit">SSH</bk-radio>
                    <bk-radio value="HTTP">HTTP</bk-radio>
                    <bk-radio value="HTTPS" v-if="isTGit">HTTPS</bk-radio>
                </bk-radio-group>
            </div>
            <div class="bk-form-item" v-if="(isGit || isGithub) && codelib.authType === 'OAUTH' || (isTGit && codelib.authType === 'T_GIT_OAUTH')">
                <div class="bk-form-item is-required" v-if="hasPower">
                    <!-- 源代码地址 start -->
                    <div class="bk-form-item is-required">
                        <label class="bk-label">{{ $t('codelib.codelibUrl') }}:</label>
                        <div class="bk-form-content">
                            <bk-select
                                v-model="codelibUrl"
                                v-bind="selectComBindData"
                                v-validate="'required'"
                                name="name"
                                class="codelib-credential-selector"
                            >
                                <bk-option v-for="option in oAuth.project"
                                    :key="option.httpUrl"
                                    :id="option.httpUrl"
                                    :name="option.httpUrl">
                                </bk-option>
                            </bk-select>
                            <span class="error-tips" v-if="urlErrMsg || errors.has('name')">
                                {{ urlErrMsg || errors.first("name") }}
                            </span>
                        </div>
                    </div>
                    <!-- 源代码地址 end -->

                    <!-- 别名 start -->
                    <div class="bk-form-item is-required">
                        <label class="bk-label">{{ $t('codelib.aliasName') }}:</label>
                        <div class="bk-form-content" :class="{ 'is-danger': errors.has('aliasName') }">
                            <input type="text" class="bk-form-input" :placeholder="$t('codelib.aliasNameEnter')" name="codelibAliasName" v-model.trim="codelibAliasName" data-vv-validate-on="blur" v-validate="{ required: true, max: 60, aliasUnique: [projectId, repositoryHashId] }" :class="{ 'is-danger': errors.has('codelibAliasName') }">
                            <span class="error-tips" v-if="errors.has('codelibAliasName')">
                                {{ errors.first('codelibAliasName') }}
                            </span>
                        </div>
                    </div>
                    <!-- 别名 end -->
                </div>
                <div class="bk-form-item is-required" v-else>
                    <div class="bk-form-content" :class="{ 'is-danger': errors.has('powerValidate') }" :style="isGithub ? { textAlign: 'center', marginLeft: 0 } : {}">
                        <button class="bk-button bk-primary" type="button" @click="openValidate">{{ $t('codelib.oauthCert') }}</button>
                        <input type="text" value="" name="powerValidate" v-validate="{ required: true }" style="width: 0; height: 0; border: none; z-index: -20; opacity: 0;">
                        <span class="error-tips" v-if="errors.has('powerValidate')">
                            {{ errors.first('powerValidate') }}
                        </span>
                    </div>
                </div>
            </div>
            <div class="bk-form-item" v-else>
                <div class="bk-form-item is-required" v-if="codelibConfig.label === 'SVN'">
                    <label class="bk-label">{{ $t('codelib.codelibPullType') }}:</label>
                    <bk-radio-group v-model="codelib.svnType" @change="svnTypeChange(codelib)" class="bk-form-content form-radio">
                        <bk-radio value="ssh">SSH</bk-radio>
                        <bk-radio value="http">HTTP</bk-radio>
                    </bk-radio-group>
                </div>
                <!-- 源代码地址 start -->
                <div class="bk-form-item is-required" v-if="!isP4">
                    <label class="bk-label">{{ $t('codelib.codelibUrl') }}:</label>
                    <div class="bk-form-content">
                        <input type="text" class="bk-form-input" :placeholder="urlPlaceholder" name="codelibUrl" v-model.trim="codelibUrl" :v-validate="'required' ? !isP4 : false" :class="{ 'is-danger': urlErrMsg || errors.has('codelibUrl') }">
                        <span class="error-tips" v-if="(urlErrMsg || errors.has('codelibUrl') && !isP4)">
                            {{ urlErrMsg || errors.first("codelibUrl") }}
                        </span>
                    </div>
                </div>
                <!-- 源代码地址 end -->

                <!-- 服务器 start -->
                <div class="bk-form-item is-required" v-if="isP4">
                    <label class="bk-label">p4 port:</label>
                    <div class="bk-form-content">
                        <div class="flex-content">
                            <input type="text" class="bk-form-input" :placeholder="portPlaceholder" name="codelibPort" v-model.trim="codelibPort" v-validate="'required'" :class="{ 'is-danger': errors.has('codelibPort') }">
                            <i class="devops-icon icon-info-circle tip-icon" v-bk-tooltips="$t('codelib.portTips')"></i>
                        </div>
                        <span class="error-tips" v-if="errors.has('codelibPort')">
                            {{ errors.first("codelibPort") }}
                        </span>
                    </div>
                </div>
                <!-- 服务器 end -->
                
                <!-- 别名 start -->
                <div class="bk-form-item is-required">
                    <label class="bk-label">{{ $t('codelib.aliasName') }}:</label>
                    <div class="bk-form-content" :class="{ 'is-danger': errors.has('aliasName') }">
                        <input type="text" class="bk-form-input" :placeholder="$t('codelib.aliasNameEnter')" name="codelibAliasName" v-model.trim="codelibAliasName" data-vv-validate-on="blur" v-validate="{ required: true, max: 60, aliasUnique: [projectId, repositoryHashId] }" :class="{ 'is-danger': errors.has('codelibAliasName') }">
                        <span class="error-tips" v-if="errors.has('codelibAliasName')">
                            {{ errors.first('codelibAliasName') }}
                        </span>
                    </div>
                </div>
                <!-- 别名 end -->

                <!-- 访问凭据 start -->
                <div class="bk-form-item is-required" v-if="codelibConfig.label !== 'Github'">
                    <label class="bk-label">{{ $t('codelib.codelibCredential') }}:</label>
                    <div class="bk-form-content code-lib-credential" :class="{ 'is-danger': errors.has('credentialId') }">
                        <bk-select v-model="credentialId"
                            :loading="isLoadingTickets"
                            searchable
                            :clearable="false"
                            v-validate="'required'"
                            name="credentialId"
                            class="codelib-credential-selector"
                            :placeholder="$t('codelib.credentialPlaceholder')"
                            @toggle="refreshTicket"
                        >
                            <bk-option v-for="(option, index) in credentialList"
                                :key="index"
                                :id="option.credentialId"
                                :name="option.credentialId">
                                <span>{{option.credentialId}}</span>
                                <i class="devops-icon icon-edit2 cre-icon" @click.stop="goToEditCre(index)"></i>
                            </bk-option>
                        </bk-select>
                        <span class="text-link" @click="addCredential">{{ $t('codelib.new') }}</span>
                    </div>
                    <span class="error-tips" v-if="errors.has('credentialId')">{{ $t('codelib.credentialRequired') }}</span>
                </div>
                <!-- 访问凭据 end -->
            </div>
        </form>
    </bk-dialog>
</template>

<script>
    import dialogMixin from './mixin.js'
    import P4 from './P4'
    import SVN from './SVN'
    import Git from './Git'
    import TGit from './TGit'
    import Github from './Github'
    import Gitlab from './Gitlab'
    export default {
        name: 'codelib-dialog',
        components: {
            Github,
            Gitlab,
            SVN,
            TGit,
            Git,
            P4
        },
        mixins: [dialogMixin],

        computed: {
            comName () {
                const comMap = {
                    Git: 'Git',
                    TGit: 'TGit',
                    Github: 'Github',
                    SVN: 'SVN',
                    P4: 'P4',
                    Gitlab: 'Gitlab'
                }

                return comMap[this.codelibConfig.label]
            }
        }
    }
</script>

<style lang="scss">
    .bk-dialog-title {
        text-align: left;
        font-size: 14px;
        color: #313238;
        font-weight: 400;
    }
    .code-lib-credential {
        display: flex;
        align-items: center;
        > .codelib-credential-selector {
            width: 300px;
            display: inline-block;
            margin-right: 4px;
        }
        .error-tips {
            display: block;
        }
        .text-link {
            cursor: pointer;
            color: #3c96ff;
            line-height: 1.5;
            font-size: 12px;
        }
    }

    .form-radio {
        margin-top: 4px;
        >label {
            margin-right: 30px;
        }
    }
    .cre-icon {
        float: right;
        margin-top: 10px;
    }
    .flex-content {
        display: flex;
        justify-content: center;
        align-items: center;
        .tip-icon {
            margin-left: 5px;
        }
    }
    .bk-form:nth-child(1) {
        margin-top: -20px !important;
    }
    .bk-form-item {
        margin-top: 20px !important;
    }
</style>

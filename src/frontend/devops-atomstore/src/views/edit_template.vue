<template>
    <div class="edit-template-wrapper" v-bkloading="{ isLoading: loading.isLoading, title: loading.title }">
        <bread-crumbs :bread-crumbs="navList" type="template">
            <a class="g-title-work" target="_blank" :href="docsLink"> {{ $t('store.模板指引') }} </a>
        </bread-crumbs>
        <div class="edit-template-content" v-if="showContent">
            <form class="bk-form edit-template-form">
                <div class="bk-form-item name-form-item is-required">
                    <label class="bk-label"> {{ $t('store.名称') }} </label>
                    <div class="bk-form-content template-item-content is-tooltips">
                        <div style="min-width: 40%;">
                            <input type="text" class="bk-form-input template-name-input" :placeholder="$t('store.请输入中英文名称')"
                                ref="templateName"
                                name="templateName"
                                v-model="templateForm.templateName"
                                v-validate="{
                                    required: true,
                                    max: 20
                                }"
                                :class="{ 'is-danger': errors.has('templateName') }">
                            <p :class="errors.has('templateName') ? 'error-tips' : 'normal-tips'">{{ errors.first("templateName") }}</p>
                        </div>
                        <bk-popover placement="right">
                            <i class="devops-icon icon-info-circle"></i>
                            <template slot="content">
                                <p> {{ $t('store.模板名称不超过20个字符') }} </p>
                            </template>
                        </bk-popover>
                    </div>
                </div>
                <div class="bk-form-item is-required" ref="sortError">
                    <label class="bk-label"> {{ $t('store.分类') }} </label>
                    <div class="bk-form-content template-item-content template-category-content">
                        <bk-select v-model="templateForm.classifyCode" style="width: 40%;" searchable>
                            <bk-option v-for="(option, index) in sortList"
                                :key="index"
                                :id="option.classifyCode"
                                :name="option.classifyName"
                                @click.native="changeClassify"
                                :placeholder="$t('store.请选择分类')"
                            >
                            </bk-option>
                        </bk-select>
                        <div v-if="formErrors.sortError" class="error-tips"> {{ $t('store.分类不能为空') }} </div>
                    </div>
                </div>
                <div class="bk-form-item is-required" ref="categoryError">
                    <label class="bk-label env-label"> {{ $t('store.应用范畴') }} </label>
                    <div class="bk-form-content template-item-content category">
                        <bk-checkbox-group v-model="templateForm.categoryIdList">
                            <bk-checkbox :value="entry.id" v-for="entry in categoryList" :key="entry.id">
                                <img class="category-icon" :src="entry.iconUrl" v-if="entry.iconUrl">
                                <span class="bk-checkbox-text" :style="{ 'margin-left': entry.iconUrl ? '24px' : '0' }">{{ entry.categoryName }}</span>
                            </bk-checkbox>
                        </bk-checkbox-group>
                        <div v-if="formErrors.categoryError" class="error-tips"> {{ $t('store.应用范畴不能为空') }} </div>
                    </div>
                </div>
                <div class="bk-form-item">
                    <label class="bk-label"> {{ $t('store.功能标签') }} </label>
                    <div class="bk-form-content template-item-content">
                        <bk-select v-model="templateForm.labelIdList" searchable multiple show-select-all>
                            <bk-option v-for="(option, index) in labelList"
                                :key="index"
                                :id="option.id"
                                :name="option.labelName"
                                :placeholder="$t('store.请选择功能标签')"
                            >
                            </bk-option>
                        </bk-select>
                    </div>
                </div>
                <div class="bk-form-item introduction-form-item is-required">
                    <label class="bk-label"> {{ $t('store.简介') }} </label>
                    <div class="bk-form-content template-item-content is-tooltips">
                        <input type="text" class="bk-form-input template-introduction-input" :placeholder="$t('store.展示在模板市场上的文本简介，不超过256个字符。')"
                            name="introduction"
                            maxlength="256"
                            v-model="templateForm.summary"
                            v-validate="{
                                required: true,
                                max: 256
                            }"
                            :class="{ 'is-danger': errors.has('introduction') }">
                        <bk-popover placement="left">
                            <i class="devops-icon icon-info-circle"></i>
                            <template slot="content">
                                <p> {{ $t('store.模版一句话简介，不超过256个字符，展示在模版市场上') }} </p>
                            </template>
                        </bk-popover>
                    </div>
                    <p :class="errors.has('introduction') ? 'error-tips' : 'normal-tips'">{{ errors.first("introduction") }}</p>
                </div>
                <div class="bk-form-item remark-form-item is-required" ref="descError">
                    <label class="bk-label"> {{ $t('store.详细描述') }} </label>
                    <div class="bk-form-content template-item-content is-tooltips">
                        <mavon-editor class="template-remark-input"
                            :placeholder="descTemplate"
                            ref="mdHook"
                            v-model="templateForm.description"
                            :toolbars="toolbarOptions"
                            :external-link="false"
                            :box-shadow="false"
                            :language="mavenLang"
                            preview-background="#fff"
                            @imgAdd="addImage"
                            @imgDel="delImage"
                            @change="changeData" />
                        <bk-popover placement="left">
                            <i class="devops-icon icon-info-circle"></i>
                            <template slot="content">
                                <p> {{ $t('store.展示在模版市场查看模版详情页面，帮助用户快速了解模版功能和使用场景') }} </p>
                            </template>
                        </bk-popover>
                    </div>
                    <p v-if="formErrors.descError" class="error-tips" style="margin-left: 100px;margin-top:4px;"> {{ $t('store.详细描述不能为空') }} </p>
                </div>
                <div class="version-msg">
                    <p class="form-title"> {{ $t('store.发布信息') }} </p>
                    <hr class="cut-line">
                </div>
                <div class="bk-form-item name-form-item is-required">
                    <label class="bk-label"> {{ $t('store.发布者') }} </label>
                    <div class="bk-form-content template-item-content">
                        <div style="width: 40%;">
                            <input type="text" class="bk-form-input template-name-input" :placeholder="$t('store.请输入')"
                                name="publisher"
                                v-model="templateForm.publisher"
                                v-validate="{
                                    required: true,
                                    max: 20
                                }"
                                :class="{ 'is-danger': errors.has('publisher') }">
                            <p :class="errors.has('publisher') ? 'error-tips' : 'normal-tips'">{{ errors.first("publisher") }}</p>
                        </div>
                    </div>
                </div>
                <div class="bk-form-item versionlog-form-item">
                    <label class="bk-label"> {{ $t('store.发布描述') }} </label>
                    <div class="bk-form-content template-item-content">
                        <textarea type="text" class="bk-form-input template-versionlog-input"
                            placeholder=""
                            v-model="templateForm.pubDescription">
                        </textarea>
                    </div>
                </div>
                <div class="form-footer">
                    <button class="bk-button bk-primary" type="button" @click="submit()"> {{ $t('store.提交') }} </button>
                    <button class="bk-button bk-default" type="button" @click="$router.back()"> {{ $t('store.取消') }} </button>
                </div>
                <select-logo :form="templateForm" type="TEMPLATE" :is-err="formErrors.logoUrlError" ref="logoUrlError"></select-logo>
            </form>
        </div>
    </div>
</template>

<script>
    import selectLogo from '@/components/common/selectLogo'
    import breadCrumbs from '@/components/bread-crumbs.vue'
    import { toolbars } from '@/utils/editor-options'

    export default {
        components: {
            selectLogo,
            breadCrumbs
        },
        data () {
            return {
                showContent: false,
                descTemplate: '',
                docsLink: this.BKCI_DOCS.TEMPLATE_GUIDE_DOC,
                sortList: [],
                labelList: [],
                categoryList: [],
                loading: {
                    isLoading: false,
                    title: ''
                },
                formErrors: {
                    sortError: false,
                    categoryError: false,
                    descError: false,
                    logoUrlError: false
                },
                templateForm: {
                    templateName: '',
                    templateType: 'FREEDOM',
                    releaseFlag: false,
                    classifyCode: '',
                    categoryIdList: [],
                    labelIdList: [],
                    summary: '',
                    description: `#### ${this.$t('store.模板功能')}\n\n#### ${this.$t('store.适用场景')}\n\n#### ${this.$t('store["使用限制和受限解决方案[可选]"]')}\n\n#### ${this.$t('store.常见的失败原因和解决方案')}`,
                    publisher: '',
                    pubDescription: '',
                    logoUrl: '',
                    iconData: ''
                }
            }
        },
        computed: {
            templateId () {
                return this.$route.params.templateId
            },
            toolbarOptions () {
                return toolbars
            },
            navList () {
                return [
                    { name: this.$t('store.工作台'), to: { name: 'templateWork' } },
                    { name: this.$t('store.上架模板') }
                ]
            },
            mavenLang () {
                return this.$i18n.locale === 'en-US' ? 'en' : this.$i18n.locale
            }
        },
        watch: {
            'templateForm.categoryList' (val) {
                if (val.length) {
                    this.formErrors.categoryError = false
                }
            }
        },
        async created () {
            this.init()
        },
        methods: {
            async init () {
                await this.initOption()
                await this.requestTemplateDetail()
            },
            async requestTemplateDetail () {
                this.loading.isLoading = true

                try {
                    const res = await this.$store.dispatch('store/requestTempIdDetail', {
                        templateId: this.templateId
                    })
                    Object.assign(this.templateForm, res, {})
                    this.templateForm.categoryIdList = this.templateForm.categoryList.map(item => {
                        return item.id
                    })
                    this.templateForm.labelIdList = (this.templateForm.labelList || []).map(item => {
                        return item.id
                    })
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                } finally {
                    setTimeout(() => {
                        this.loading.isLoading = false
                        this.showContent = true
                        this.autoFocus()
                    }, 500)
                }
            },
            initOption () {
                this.requestTplClassify()
                this.requestTplCategorys()
                this.requestTplLabel()
            },
            async requestTplClassify () {
                try {
                    const res = await this.$store.dispatch('store/requestTplClassify')
                    this.sortList = res || []
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            async requestTplCategorys () {
                try {
                    const res = await this.$store.dispatch('store/requestTplCategorys')
                    this.categoryList = res || []
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            async requestTplLabel () {
                try {
                    const res = await this.$store.dispatch('store/requestTplLabel')
                    this.labelList = res || []
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            changeClassify () {
                this.formErrors.sortError = false
            },
            addImage (pos, file) {
                this.uploadimg(pos, file)
            },
            delImage (pos) {
                
            },
            changeData (value, render) {
                if (value) {
                    this.formErrors.descError = false
                }
            },
            async uploadimg (pos, file) {
                const formData = new FormData()
                const config = {
                    headers: {
                        'Content-Type': 'multipart/form-data'
                    }
                }
                let message, theme
                formData.append('file', file)

                try {
                    const res = await this.$store.dispatch('store/uploadFile', {
                        formData,
                        config
                    })

                    this.$refs.mdHook.$img2Url(pos, res)
                } catch (err) {
                    message = err.message ? err.message : err
                    theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                    this.$refs.mdHook.$refs.toolbar_left.$imgDel(pos)
                }
            },
            
            autoFocus () {
                this.$nextTick(() => {
                    this.$refs.templateName.focus()
                })
            },
            toAtomStore () {
                this.$router.push({
                    name: 'atomHome'
                })
            },
            toPublishProgress (id) {
                this.$router.push({
                    name: 'upgradeTemplate',
                    params: {
                        templateId: id
                    }
                })
            },
            checkValid () {
                let errorCount = 0
                let ref = ''
                if (!this.templateForm.logoUrl && !this.templateForm.iconData) {
                    this.formErrors.logoUrlError = true
                    ref = ref || 'logoUrlError'
                    errorCount++
                }

                if (!this.templateForm.classifyCode) {
                    this.formErrors.sortError = true
                    ref = ref || 'sortError'
                    errorCount++
                }

                if (!this.templateForm.categoryIdList.length) {
                    this.formErrors.categoryError = true
                    ref = ref || 'categoryError'
                    errorCount++
                }

                if (!this.templateForm.description) {
                    this.formErrors.descError = true
                    ref = ref || 'descError'
                    errorCount++
                }

                if (errorCount > 0) {
                    const errorEle = this.$refs[ref]
                    if (errorEle) errorEle.scrollIntoView()
                    return false
                }

                return true
            },
            async submit () {
                const isCheckValid = this.checkValid()
                const valid = await this.$validator.validate()
                if (isCheckValid && valid) {
                    let message, theme
                    
                    try {
                        this.loading.isLoading = true

                        const params = {
                            templateCode: this.templateForm.templateCode,
                            templateName: this.templateForm.templateName,
                            templateType: this.templateForm.templateType,
                            categoryIdList: this.templateForm.categoryIdList,
                            classifyCode: this.templateForm.classifyCode,
                            labelIdList: this.templateForm.labelIdList.filter(i => i !== 'null' && i !== ' ' && i),
                            publisher: this.templateForm.publisher,
                            logoUrl: this.templateForm.logoUrl || undefined,
                            iconData: this.templateForm.iconData || undefined,
                            summary: this.templateForm.summary || undefined,
                            description: this.templateForm.description || undefined,
                            pubDescription: this.templateForm.pubDescription || undefined
                        }

                        const res = await this.$store.dispatch('store/editTemplate', {
                            params: params
                        })

                        message = this.$t('store.提交成功')
                        theme = 'success'
                        if (res) {
                            this.toPublishProgress(res)
                        }
                    } catch (err) {
                        if (err.httpStatus === 200) {
                            const h = this.$createElement

                            this.$bkInfo({
                                type: 'error',
                                title: this.$t('store.提交失败'),
                                showFooter: false,
                                subHeader: h('p', {
                                    style: {
                                        textDecoration: 'none',
                                        cursor: 'pointer',
                                        whiteSpace: 'normal',
                                        textAlign: 'left'
                                    }
                                }, err.message ? err.message : err)
                            })
                        } else {
                            message = err.message ? err.message : err
                            theme = 'error'
                        }
                    } finally {
                        if (theme === 'error') {
                            this.$bkMessage({
                                message,
                                theme
                            })
                        }
                        
                        this.loading.isLoading = false
                    }
                }
            }
        }
    }
</script>

<style lang="scss">
    @import '@/assets/scss/conf.scss';

    .edit-template-wrapper {
        height: 100%;
        .edit-template-content {
            margin: 20px 0;
            height: calc(100% - 5.6vh - 40px);
            overflow: auto;
            display: flex;
            justify-content: center;
        }
        .edit-template-form {
            position: relative;
            margin: 0 auto;
            width: 1200px;
            .bk-label {
                width: 100px;
                font-weight: normal;
            }
            .bk-form-content {
                margin-left: 100px;
            }
            .bk-selector .bk-form-checkbox {
                display: block;
                padding: 12px 0;
            }
            .template-category-content {
                .bk-selector{
                    width: 40%;
                    .bk-form-checkbox {
                        display: block;
                        padding: 12px 0;
                    }
                }
            }
            .bk-form-checkbox {
                margin-right: 40px;
            }
            .form-tooltips {
                max-width: 210px;
                text-align: left;
                white-space: normal;
                word-break: break-all;
                font-weight: 400;
            }
            .introduction-form-item {
                .error-tips {
                    margin-left: 100px;
                }
            }
            .name-form-item,
            .introduction-form-item,
            .remark-form-item,
            .publish-form-item,
            .version-num-form-item {
                .bk-tooltip {
                    margin-top: 10px;
                    margin-left: 10px;
                    color: $fontLigtherColor;
                    p {
                        max-width: 400px;
                        text-align: left;
                        white-space: normal;
                        word-break: break-all;
                        font-weight: 400;
                    }
                }
            }
            .is-tooltips {
                display: flex;
            }
            .introduction-form-item,
            .remark-form-item,
            .publish-form-item,
            .version-num-form-item  {
                .bk-tooltip {
                    left: 101%;

                }
            }
            .template-introduction-input,
            .template-remark-input {
                min-width: 100%;
                border: 1px solid #c4c6cc;
            }
            .version-num-form-item {
                .version-num-content {
                    position: relative;
                    top: 8px;
                    color: #333C48;
                }
                .version-prompt {
                    margin-left: 20px;
                    color: $fontWeightColor;
                }
            }
            .bk-radio-text {
                color: #333C48;
            }
            .tips-text {
                padding-bottom: 3px;
                border-bottom: 1px dashed #c3cdd7;
            }
            .env-checkbox {
                .devops-icon {
                    position: relative;
                    top: 3px;
                    font-size: 18px;
                    color: #979BA5;
                }
                .bk-checkbox-text {
                    color: #333C48;
                }
                >svg {
                    position: relative;
                    top: 5px;
                }
            }
            .template-remark-input {
                height: 263px;
                &.fullscreen {
                    height: auto;
                }
            }
            .version-msg {
                padding: 12px 0 12px 26px;
            }
            .form-title {
                margin-top: 20px;
                font-weight: bold
            }
            .cut-line {
                margin-top: 8px;
                height: 1px;
                border: none;
                background-color: #C3CDD7
            }
            .template-versionlog-input {
                padding: 10px;
                height: 80px;
            }
            .form-footer {
                margin-top: 26px;
                margin-left: 100px;
            }
            .template-logo-box {
                position: absolute;
                top: 0;
                right: 0;
                width: 100px;
                height: 100px;
                background: #fff;
                border: 1px dashed $lineColor;
                text-align: center;
                cursor: pointer;
                .icon-plus {
                    display: inline-block;
                    margin-top: 30px;
                    font-size: 19px;
                    color: #979BA5;
                }
                p {
                    margin-top: 4px;
                    font-size: 12px;
                }
                img {
                    position: relative;
                    width: 100px;
                    height: 100px;
                    z-index: 99;
                    object-fit: cover;
                }
            }
            .no-img {
                border: none;
                background: transparent;
                cursor: pointer;
                &:hover {
                    &:after {
                        content: '\66F4\6362logo';
                        position: absolute;
                        bottom: 0;
                        left: 0;
                        right: 0;
                        z-index: 100;
                        line-height: 25px;
                        text-align: center;
                        color: #fff;
                        background: black;
                        opacity: 0.7;
                    }
                }
            }
            .img-Error {
                border: 1px dashed $dangerColor;
                .error-msg {
                    color: $dangerColor;
                }
            }
            .category {
                margin-top: 5px;
            }
            .category-icon {
                position: absolute;
                top: 0;
                width: 18px;
                height: 18px;
            }
        }
        .auto-textarea-wrapper {
            min-height: 200px;
        }
    }
    .error-commit {
        .bk-dialog-default-status {
            padding-top: 10px;
        }
    }
</style>

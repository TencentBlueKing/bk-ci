<template>
    <div class="edit-template-wrapper" v-bkloading="{ isLoading: loading.isLoading, title: loading.title }">
        <div class="info-header">
            <div class="title first-level" @click="toAtomStore()">
                <logo :name="&quot;store&quot;" size="30" class="nav-icon" />
                <div class="title first-level">研发商店</div>
            </div>
            <i class="right-arrow"></i>
            <div class="title secondary" @click="toAtomList()">工作台</div>
            <i class="right-arrow"></i>
            <div class="title third-level">上架模板</div>
            <a class="develop-guide-link" target="_blank"
                :href="docsLink">模板指引</a>
        </div>
        <div class="edit-template-content" v-if="showContent">
            <form class="bk-form edit-template-form">
                <div class="bk-form-item name-form-item is-required">
                    <label class="bk-label">名称</label>
                    <div class="bk-form-content template-item-content is-tooltips">
                        <div style="min-width: 40%;">
                            <input type="text" class="bk-form-input template-name-input" placeholder="请输入中英文名称"
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
                            <i class="bk-icon icon-info-circle"></i>
                            <template slot="content">
                                <p>模板名称不超过20个字符</p>
                            </template>
                        </bk-popover>
                    </div>
                </div>
                <div class="bk-form-item is-required">
                    <label class="bk-label">分类</label>
                    <div class="bk-form-content template-item-content template-category-content">
                        <bk-select v-model="templateForm.classifyCode" style="width: 40%;" searchable>
                            <bk-option v-for="(option, index) in sortList"
                                :key="index"
                                :id="option.classifyCode"
                                :name="option.classifyName"
                                @click.native="changeClassify"
                                :placeholder="'请选择分类'"
                            >
                            </bk-option>
                        </bk-select>
                        <div v-if="formErrors.sortError" class="error-tips">分类不能为空</div>
                    </div>
                </div>
                <div class="bk-form-item is-required">
                    <label class="bk-label env-label">应用范畴</label>
                    <div class="bk-form-content template-item-content category">
                        <bk-checkbox-group v-model="templateForm.categoryIdList">
                            <bk-checkbox :value="entry.id" v-for="entry in categoryList" :key="entry.id">
                                <img class="category-icon" :src="entry.iconUrl" v-if="entry.iconUrl">
                                <span class="bk-checkbox-text" :style="{ 'margin-left': entry.iconUrl ? '24px' : '0' }">{{ entry.categoryName }}</span>
                            </bk-checkbox>
                        </bk-checkbox-group>
                        <div v-if="formErrors.categoryError" class="error-tips">应用范畴不能为空</div>
                    </div>
                </div>
                <div class="bk-form-item">
                    <label class="bk-label">功能标签</label>
                    <div class="bk-form-content template-item-content">
                        <bk-select v-model="templateForm.labelIdList" searchable multiple show-select-all>
                            <bk-option v-for="(option, index) in labelList"
                                :key="index"
                                :id="option.id"
                                :name="option.labelName"
                                :placeholder="'请选择功能标签'"
                            >
                            </bk-option>
                        </bk-select>
                    </div>
                </div>
                <div class="bk-form-item introduction-form-item is-required">
                    <label class="bk-label">简介</label>
                    <div class="bk-form-content template-item-content is-tooltips">
                        <input type="text" class="bk-form-input template-introduction-input"
                            placeholder="展示在模板市场上的文本简介，不超过70个字符。"
                            name="introduction"
                            maxlength="70"
                            v-model="templateForm.summary"
                            v-validate="{
                                required: true,
                                max: 70
                            }"
                            :class="{ 'is-danger': errors.has('introduction') }">
                        <bk-popover placement="left">
                            <i class="bk-icon icon-info-circle"></i>
                            <template slot="content">
                                <p>模版一句话简介，不超过70个字符，展示在模版市场上</p>
                            </template>
                        </bk-popover>
                    </div>
                    <p :class="errors.has('introduction') ? 'error-tips' : 'normal-tips'">{{ errors.first("introduction") }}</p>
                </div>
                <div class="bk-form-item remark-form-item is-required">
                    <label class="bk-label">详细描述</label>
                    <div class="bk-form-content template-item-content is-tooltips">
                        <mavon-editor class="template-remark-input"
                            :placeholder="descTemplate"
                            ref="mdHook"
                            v-model="templateForm.description"
                            :toolbars="toolbarOptions"
                            :external-link="false"
                            @imgAdd="addImage"
                            @imgDel="delImage"
                            @change="changeData" />
                        <bk-popover placement="left">
                            <i class="bk-icon icon-info-circle"></i>
                            <template slot="content">
                                <p>展示在模版市场查看模版详情页面，帮助用户快速了解模版功能和使用场景</p>
                            </template>
                        </bk-popover>
                    </div>
                    <p v-if="formErrors.descError" class="error-tips" style="margin-left: 100px;margin-top:4px;">详细描述不能为空</p>
                </div>
                <div class="version-msg">
                    <p class="form-title">发布信息</p>
                    <hr class="cut-line">
                </div>
                <div class="bk-form-item name-form-item is-required">
                    <label class="bk-label">发布者</label>
                    <div class="bk-form-content template-item-content">
                        <div style="width: 40%;">
                            <input type="text" class="bk-form-input template-name-input"
                                placeholder="请输入"
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
                    <label class="bk-label">发布描述</label>
                    <div class="bk-form-content template-item-content">
                        <textarea type="text" class="bk-form-input template-versionlog-input"
                            placeholder=""
                            v-model="templateForm.pubDescription">
                        </textarea>
                    </div>
                </div>
                <div class="form-footer">
                    <button class="bk-button bk-primary" type="button" @click="submit()">提交</button>
                    <button class="bk-button bk-default" type="button" @click="toAtomList()">取消</button>
                </div>
                <div class="template-logo-box" :class="{ 'is-border': !templateForm.logoUrl }" @click="uploadLogo()">
                    <section v-if="templateForm.logoUrl">
                        <img :src="templateForm.logoUrl">
                    </section>
                    <section v-else>
                        <i class="bk-icon icon-plus"></i>
                        <p>上传LOGO</p>
                    </section>
                </div>
            </form>
        </div>
        <template-logo :show-dialog="showlogoDialog"
            :to-confirm-logo="toConfirmLogo"
            :to-close-dialog="toCloseDialog"
            :file-change="fileChange"
            :selected-url="selectedUrl"
            :is-uploading="isUploading">
        </template-logo>
    </div>
</template>

<script>
    import templateLogo from '@/components/atom-logo'
    import { toolbars } from '@/utils/editor-options'
    import mavonEditor from 'mavon-editor'
    import 'mavon-editor/dist/css/index.css'

    const Vue = window.Vue
    Vue.use(mavonEditor)

    export default {
        components: {
            templateLogo
        },
        data () {
            return {
                showContent: false,
                showlogoDialog: false,
                selectedUrl: '',
                descTemplate: '',
                docsLink: `${DOCS_URL_PREFIX}/所有服务/流水线模版/summary.html`,
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
                    descError: false
                },
                templateForm: {
                    templateName: '',
                    templateType: 'FREEDOM',
                    releaseFlag: false,
                    classifyCode: '',
                    categoryIdList: [],
                    labelIdList: [],
                    summary: '',
                    description: '- 模版功能\n\n- 适用场景\n\n- 注意事项',
                    publisher: '',
                    pubDescription: '',
                    logoUrl: ''
                }
            }
        },
        computed: {
            templateId () {
                return this.$route.params.templateId
            },
            toolbarOptions () {
                return toolbars
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
                    this.templateForm.labelIdList = this.templateForm.labelList.map(item => {
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
            fileChange (e) {
                const file = e.target.files[0]
                if (file) {
                    if (!(file.type === 'image/jpeg' || file.type === 'image/png')) {
                        this.$bkMessage({
                            theme: 'error',
                            message: '请上传png、jpg格式的图片'
                        })
                    } else if (file.size > (2 * 1024 * 1024)) {
                        this.$bkMessage({
                            theme: 'error',
                            message: '请上传大小不超过2M的图片'
                        })
                    } else {
                        const reader = new FileReader()
                        reader.readAsDataURL(file)
                        reader.onload = evts => {
                            const img = new Image()
                            img.src = evts.target.result
                            img.onload = evt => {
                                if (img.width === 512 && img.height === 512) {
                                    this.uploadHandle(file)
                                } else {
                                    this.$bkMessage({
                                        theme: 'error',
                                        message: '请上传尺寸为512*512的图片'
                                    })
                                }
                            }
                        }
                    }
                }
            },
            async uploadHandle (file) {
                const formData = new FormData()
                const config = {
                    headers: {
                        'Content-Type': 'multipart/form-data'
                    }
                }
                let message, theme
                formData.append('logo', file)

                try {
                    const res = await this.$store.dispatch('store/uploadLogo', {
                        formData,
                        config
                    })

                    this.selectedUrl = res
                } catch (err) {
                    message = err.message ? err.message : err
                    theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            uploadLogo () {
                this.showlogoDialog = true
                this.selectedUrl = this.templateForm.logoUrl
            },
            /**
             * 清空input file的值
             */
            resetUploadInput () {
                this.$nextTick(() => {
                    const inputElement = document.getElementById('inputfile')
                    inputElement.value = ''
                })
            },
            autoFocus () {
                this.$nextTick(() => {
                    this.$refs.templateName.focus()
                })
            },
            async toConfirmLogo () {
                if (this.selectedUrl) {
                    this.templateForm.logoUrl = this.selectedUrl
                    this.showlogoDialog = false
                } else if (!this.selectedUrl) {
                    this.$bkMessage({
                        message: '请选择要上传的图片',
                        theme: 'error'
                    })
                }
                this.resetUploadInput()
            },
            toCloseDialog () {
                this.showlogoDialog = false
                this.selectedFile = undefined
                this.resetUploadInput()
            },
            toAtomList () {
                this.$router.push({
                    name: 'atomList',
                    params: {
                        type: 'template'
                    }
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
                if (!this.templateForm.classifyCode) {
                    this.formErrors.sortError = true
                    errorCount++
                }

                if (!this.templateForm.categoryIdList.length) {
                    this.formErrors.categoryError = true
                    errorCount++
                }

                if (!this.templateForm.description) {
                    this.formErrors.descError = true
                    errorCount++
                }

                if (errorCount > 0) {
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
                            labelIdList: this.templateForm.labelIdList,
                            publisher: this.templateForm.publisher,
                            logoUrl: this.templateForm.logoUrl || undefined,
                            summary: this.templateForm.summary || undefined,
                            description: this.templateForm.description || undefined,
                            pubDescription: this.templateForm.pubDescription || undefined
                        }

                        const res = await this.$store.dispatch('store/editTemplate', {
                            params: params
                        })

                        message = '提交成功'
                        theme = 'success'
                        if (res) {
                            this.toPublishProgress(res)
                        }
                    } catch (err) {
                        if (err.httpStatus === 200) {
                            const h = this.$createElement

                            this.$bkInfo({
                                type: 'error',
                                title: '提交失败',
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
        .info-header {
            display: flex;
            padding: 14px 24px;
            width: 100%;
            height: 50px;
            border-bottom: 1px solid #DDE4EB;
            background-color: #fff;
            box-shadow:0px 2px 5px 0px rgba(51,60,72,0.03);
            .title {
                display: flex;
                align-items: center;
            }
            .first-level,
            .secondary {
                color: $primaryColor;
                cursor: pointer;
            }
            .third-leve {
                color: $fontWeightColor;
            }
            .nav-icon {
                width: 24px;
                height: 24px;
                margin-right: 10px;
            }
            .right-arrow {
                display :inline-block;
                position: relative;
                width: 19px;
                height: 36px;
                margin-right: 4px;
            }
            .right-arrow::after {
                display: inline-block;
                content: " ";
                height: 4px;
                width: 4px;
                border-width: 1px 1px 0 0;
                border-color: $lineColor;
                border-style: solid;
                transform: matrix(0.71, 0.71, -0.71, 0.71, 0, 0);
                position: absolute;
                top: 50%;
                right: 6px;
                margin-top: -9px;
            }
            .develop-guide-link {
                position: absolute;
                right: 36px;
                margin-top: 2px;
                color: $primaryColor;
                cursor: pointer;
            }
        }
        .edit-template-content {
            padding: 20px 0 40px;
            height: calc(100% - 50px);
            overflow: auto;
            display: flex;
            justify-content: center;
        }
        .edit-template-form {
            position: relative;
            margin: auto;
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
                .bk-icon {
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
                position: relative;
                z-index: 1;
                height: 178px;
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
            .is-border {
                border: 1px dashed $lineColor;
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

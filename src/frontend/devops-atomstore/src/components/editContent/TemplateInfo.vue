<template>
    <div class="container">
        <p class="form-title">{{ $t('store.模板信息') }}</p>
        <div class="form-template form-item-container">
            <div
                class="bk-form-item is-required"
                ref="projectCodeErrors"
            >
                <label class="bk-label"> {{ $t('store.所属项目') }} </label>
                <div
                    class="bk-form-content template-item-content"
                    v-if="!isShowInfo"
                >
                    <bk-select
                        v-model="templateForm.projectCode"
                        class="fixed-width"
                        searchable
                        :placeholder="$t('store.请选择项目')"
                        :enable-virtual-scroll="projectList && projectList.length > 3000"
                        :list="projectList"
                        id-key="projectCode"
                        display-key="projectName"
                        @change="handleChangeProject"
                    >
                        <bk-option
                            v-for="option in projectList"
                            :key="option.projectCode"
                            :id="option.projectCode"
                            :name="option.projectName"
                            @click.native="optionChange('projectCodeErrors')"
                        >
                        </bk-option>
                    </bk-select>
                    <div
                        v-if="formErrors.projectCodeErrors"
                        class="error-tips"
                    >
                        {{ $t('store.项目不能为空') }}
                    </div>
                </div>
                <div
                    v-else
                    class="bk-form-content template-item-content"
                >
                    {{ templateForm.projectCode }}
                </div>
            </div>
            <div
                class="bk-form-item is-required"
                ref="templateCodeErrors"
            >
                <label class="bk-label"> {{ $t('store.源模板名称') }} </label>
                <div
                    class="bk-form-content template-item-content"
                    v-if="!isShowInfo"
                >
                    <bk-select
                        v-model="templateForm.templateCode"
                        class="fixed-width"
                        searchable
                        :disabled="!templateForm.projectCode"
                        :placeholder="$t('store.请选择源模板')"
                        @change="handleChangeProjectCode"
                        enable-scroll-load
                        :scroll-loading="selectLoading"
                        :loading="codesSelectLoading"
                        @scroll-end="selectedTplProject()"
                    >
                        <bk-option
                            v-for="option in templateList"
                            :key="option.id"
                            :id="option.id"
                            :name="option.name"
                            @click.native="optionChange('templateCodeErrors')"
                        >
                        </bk-option>
                    </bk-select>
                    <div
                        v-if="formErrors.templateCodeErrors"
                        class="error-tips"
                    >
                        {{ $t('store.源模板名称不能为空') }}
                    </div>
                </div>
                <div
                    class="bk-form-content template-item-content"
                    v-else
                >
                    {{ templateOptionName || templateForm.templateName }}
                </div>
            </div>
            <div class="bk-form-item">
                <label class="bk-label"> {{ $t('store.模板类型') }} </label>
                <div class="bk-form-content template-item-content">
                    <div class="fixed-width">
                        <span>{{ $t('store.流水线模板') }}</span>
                    </div>
                </div>
            </div>
            <div class="bk-form-item">
                <label class="bk-label"> {{ $t('store.模板版本') }} </label>
                <div class="bk-form-content template-item-content template-category-content">
                    <bk-select
                        v-model="templateForm.templateVersion"
                        class="fixed-width version"
                        searchable
                        :disabled="!templateForm.templateCode || !versionList.length"
                        :placeholder="(templateForm.templateCode && !versionList.length) ? $t('store.当前模板的版本已全部上架') : $t('store.请选择模板版本')"
                        enable-scroll-load
                        :loading="versionsSelectLoading"
                        :scroll-loading="bottomLoading"
                        @scroll-end="getVersionList()"
                    >
                        <bk-option
                            v-for="option in versionList"
                            :key="option.version"
                            :id="option.version"
                            :name="option.versionName"
                        >
                        </bk-option>
                    </bk-select>
                </div>
            </div>
            <div class="bk-form-item is-required">
                <label class="bk-label"> {{ $t('store.发布策略') }} </label>
                <div class="bk-form-content template-item-content">
                    <bk-radio-group v-model="templateForm.publishStrategy">
                        <bk-radio
                            label="MANUAL"
                            class="mr20 strategy"
                            v-bk-tooltips="{ content: $t('store.当源模板有新版本时，按需手动发布新版本到商店'), placement: 'top' }"
                        >
                            {{ $t('store.按需手动发布') }}
                        </bk-radio>
                        <bk-radio
                            label="AUTO"
                            class="strategy"
                            v-bk-tooltips="{ content: $t('store.当源模板有新版本时，新版本自动发布到研发商店'), placement: 'top' }"
                        >
                            {{ $t('store.自动发布') }}
                        </bk-radio>
                    </bk-radio-group>
                </div>
            </div>
        </div>
        <div
            style="position: relative;"
            class="form-item-container"
        >
            <div class="bk-form-item name-form-item is-required">
                <label class="bk-label"> {{ $t('store.研发商店模板名称') }} </label>
                <div class="bk-form-content template-item-content is-tooltips">
                    <div class="fixed-width">
                        <input
                            type="text"
                            class="bk-form-input"
                            :placeholder="$t('store.请输入中英文名称')"
                            ref="templateName"
                            name="templateName"
                            v-model="templateForm.templateName"
                            v-validate="{
                                required: true,
                                max: 20
                            }"
                            :class="{ 'is-danger': errors.has('templateName') }"
                        >
                        <p
                            :class="errors.has('templateName') ? 'error-tips' : 'normal-tips'"
                            style="padding-top: 15px;"
                        >
                            {{ errors.first("templateName") }}
                        </p>
                    </div>
                    <bk-popover placement="right">
                        <i class="devops-icon icon-info-circle"></i>
                        <template slot="content">
                            <p> {{ $t('store.模板名称不超过20个字符') }} </p>
                        </template>
                    </bk-popover>
                </div>
            </div>
            <div
                class="bk-form-item is-required"
                ref="sortError"
            >
                <label class="bk-label"> {{ $t('store.分类') }} </label>
                <div class="bk-form-content template-item-content template-category-content">
                    <bk-select
                        v-model="templateForm.classifyCode"
                        class="fixed-width"
                        searchable
                    >
                        <bk-option
                            v-for="(option, index) in sortList"
                            :key="index"
                            :id="option.classifyCode"
                            :name="option.classifyName"
                            @click.native="optionChange('sortError')"
                            :placeholder="$t('store.请选择分类')"
                        >
                        </bk-option>
                    </bk-select>
                    <div
                        v-if="formErrors.sortError"
                        class="error-tips"
                    >
                        {{ $t('store.分类不能为空') }}
                    </div>
                </div>
            </div>
            <div
                class="bk-form-item is-required"
                ref="categoryError"
            >
                <label class="bk-label env-label"> {{ $t('store.应用范畴') }} </label>
                <div class="bk-form-content template-item-content category">
                    <bk-checkbox-group v-model="templateForm.categoryIdList">
                        <bk-checkbox
                            :value="entry.id"
                            v-for="entry in categoryList"
                            :key="entry.id"
                        >
                            <img
                                class="category-icon"
                                :src="entry.iconUrl"
                                v-if="entry.iconUrl"
                            >
                            <span
                                class="bk-checkbox-text"
                                :style="{ 'margin-left': entry.iconUrl ? '24px' : '0' }"
                            >{{ entry.categoryName }}</span>
                        </bk-checkbox>
                    </bk-checkbox-group>
                    <div
                        v-if="formErrors.categoryError"
                        class="error-tips"
                    >
                        {{ $t('store.应用范畴不能为空') }}
                    </div>
                </div>
            </div>
            <div class="bk-form-item">
                <label class="bk-label"> {{ $t('store.功能标签') }} </label>
                <div
                    class="bk-form-content template-item-content"
                >
                    <bk-select
                        v-model="templateForm.labelIdList"
                        searchable
                        multiple
                        show-select-all
                        class="fixed-width"
                    >
                        <bk-option
                            v-for="(option, index) in labelList"
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
                    <input
                        type="text"
                        class="bk-form-input template-introduction-input"
                        :placeholder="$t('store.展示在模板市场上的文本简介，不超过256个字符。')"
                        name="introduction"
                        maxlength="256"
                        v-model="templateForm.summary"
                        v-validate="{
                            required: true,
                            max: 256
                        }"
                        :class="{ 'is-danger': errors.has('introduction') }"
                    >
                    <bk-popover placement="left">
                        <i class="devops-icon icon-info-circle"></i>
                        <template slot="content">
                            <p> {{ $t('store.模版一句话简介，不超过256个字符，展示在模版市场上') }} </p>
                        </template>
                    </bk-popover>
                </div>
                <p :class="errors.has('introduction') ? 'error-tips' : 'normal-tips'">{{ errors.first("introduction") }}</p>
            </div>
            <div
                class="bk-form-item remark-form-item is-required"
                ref="descError"
            >
                <label class="bk-label"> {{ $t('store.详细描述') }} </label>
                <div class="bk-form-content template-item-content is-tooltips">
                    <mavon-editor
                        class="template-remark-input"
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
                        @change="changeData"
                    />
                    <bk-popover placement="left">
                        <i class="devops-icon icon-info-circle"></i>
                        <template slot="content">
                            <p> {{ $t('store.展示在模版市场查看模版详情页面，帮助用户快速了解模版功能和使用场景') }} </p>
                        </template>
                    </bk-popover>
                </div>
                <p
                    v-if="formErrors.descError"
                    class="error-tips"
                    style="margin-left: 100px;margin-top:4px;"
                >
                    {{ $t('store.详细描述不能为空') }}
                </p>
            </div>
            <div>
                <label class="logo-label"> {{ $t('store.模板图标') }} </label>
                <select-logo
                    :form="templateForm"
                    type="TEMPLATE"
                    :is-err="formErrors.logoUrlError"
                    ref="logoUrlError"
                ></select-logo>
            </div>
        </div>
    </div>
</template>

<script>
    import selectLogo from '@/components/common/selectLogo'
    import { toolbars } from '@/utils/editor-options'
    import { mapActions } from 'vuex'

    export default {
        name: 'TemplateInfo',
        components: {
            selectLogo
        },
        props: {
            templateForm: {
                type: Object,
                required: true,
                default: () => {}
            },
            isShowInfo: String
        },
        data () {
            return {
                sortList: [],
                labelList: [],
                categoryList: [],
                projectList: [],
                templateList: [],
                versionList: [],
                descTemplate: '',
                formErrors: {
                    sortError: false,
                    categoryError: false,
                    descError: false,
                    logoUrlError: false,
                    templateCodeErrors: false,
                    projectCodeErrors: false
                },
                bottomLoading: false,
                versionsSelectLoading: false,
                versionsHasNext: true,
                versionsPagination: {
                    page: 1,
                    count: 0,
                    limit: 10
                },
                codesSelectLoading: false,
                codesHasNext: false,
                codesPagination: {
                    page: 1,
                    count: 0,
                    limit: 10
                }
            }
        },
        computed: {
            toolbarOptions () {
                return toolbars
            },
            templateOptionName () {
                return this.templateList.find(item => item.templateId === this.templateForm.templateCode)?.name || this.$route.query.templateName
            }
        },
        watch: {
            'templateForm.categoryIdList' (newVal) {
                if (newVal.length) {
                    this.formErrors.categoryError = false
                }
            },
            'templateForm.projectCode': {
                handler (newVal) {
                    if (newVal) {
                        this.selectedTplProject(1)
                    }
                },
                immediate: true
            },
            'templateForm.templateCode': {
                async handler (newVal) {
                    if (newVal) {
                        await this.getVersionList(1)
                        this.$emit('updateTemplateForm', {
                            templateVersion: this.versionList[0]?.version
                        })
                    }
                },
                immediate: true
            }
        },
        async created () {
            this.initOption()
        },
        methods: {
            ...mapActions('store', [
                'requestPipelineTemplate',
                'requestTemplateVersionList'
            ]),
            initOption () {
                this.requestTplClassify()
                this.requestTplCategorys()
                this.requestTplLabel()
                this.toggleProjectList()
                setTimeout(() => {
                    this.autoFocus()
                }, 500)
            },
            handleError (err) {
                const message = err.message ? err.message : err
                const theme = 'error'

                this.$bkMessage({
                    message,
                    theme
                })
            },
            async requestTplClassify () {
                try {
                    const res = await this.$store.dispatch('store/requestTplClassify')
                    this.sortList = res || []
                } catch (err) {
                    this.handleError(err)
                }
            },
            async requestTplCategorys () {
                try {
                    const res = await this.$store.dispatch('store/requestTplCategorys')
                    this.categoryList = res || []
                } catch (err) {
                    this.handleError(err)
                }
            },
            async requestTplLabel () {
                try {
                    const res = await this.$store.dispatch('store/requestTplLabel')
                    this.labelList = res || []
                } catch (err) {
                    this.handleError(err)
                }
            },
            async toggleProjectList () {
                try {
                    const res = await this.$store.dispatch('store/requestProjectList')
                    this.projectList.splice(0, this.projectList.length, ...res)
                } catch (err) {
                    this.handleError(err)
                }
            },
            handleChangeProject () {
                this.$emit('updateTemplateForm', {
                    templateCode: '',
                    templateVersion: ''
                })
                this.templateList = []
                this.versionList = []
            },
            handleChangeProjectCode (val) {
                const templateName = this.templateList.find(item => item.id === val)?.name
                this.$emit('updateTemplateForm', {
                    templateVersion: '',
                    templateName
                })
                this.versionList = []
            },
            async selectedTplProject (page) {
                try {
                    const nextPage = page ?? this.codesPagination.page + 1
                    if (nextPage > 1 && !this.codesHasNext) return
                    if (nextPage === 1) {
                        this.codesSelectLoading = true
                    } else {
                        this.bottomLoading = true
                    }
                    const res = await this.requestPipelineTemplate({
                        projectId: this.templateForm.projectCode,
                        page: nextPage,
                        pageSize: this.codesPagination.limit,
                        mode: 'CUSTOMIZE',
                        latestVersionStatus: 'RELEASED',
                        storeStatus: 'NEVER_PUBLISHED'
                    })
                    const data = res.records
                    if (page === 1) {
                        this.templateList = data
                    } else {
                        this.templateList = [...this.templateList, ...data]
                    }
                    this.codesHasNext = res.count > this.templateList.length
                } catch (err) {
                    this.handleError(err)
                } finally {
                    this.codesSelectLoading = false
                    this.bottomLoading = false
                }
            },
            async getVersionList (page) {
                try {
                    const nextPage = page ?? this.versionsPagination.page + 1
                    if (nextPage > 1 && !this.versionsHasNext) return
                    if (nextPage === 1) {
                        this.versionsSelectLoading = true
                    } else {
                        this.bottomLoading = true
                    }
                    const res = await this.requestTemplateVersionList({
                        projectId: this.templateForm.projectCode,
                        templateId: this.templateForm.templateCode,
                        page: nextPage,
                        pageSize: this.versionsPagination.limit,
                        status: 'RELEASED'
                    })

                    const versions = res.records
                    if (page === 1) {
                        this.versionList = versions
                    } else {
                        this.versionList = [...this.versionList, ...versions]
                    }
                    this.versionsHasNext = res.count > this.versionList.length
                } catch (err) {
                    this.handleError(err)
                } finally {
                    this.bottomLoading = false
                    this.versionsSelectLoading = false
                }
            },
            selectorToggle (status) {
                if (status) {
                    this.versionsHasNext = true
                    this.getVersionList(1)
                }
            },
            autoFocus () {
                this.$nextTick(() => {
                    this.$refs.templateName.focus()
                })
            },
            optionChange (key) {
                this.formErrors[key] = false
            },
            checkValid () {
                let errorCount = 0
                let ref = ''
                const validationRules = [
                    {
                        condition: () => !this.templateForm.logoUrl && !this.templateForm.iconData,
                        errorKey: 'logoUrlError'
                    },
                    {
                        condition: () => !this.templateForm.classifyCode,
                        errorKey: 'sortError'
                    },
                    {
                        condition: () => !this.templateForm.projectCode,
                        errorKey: 'projectCodeErrors'
                    },
                    {
                        condition: () => !this.templateForm.templateCode,
                        errorKey: 'templateCodeErrors'
                    },
                    {
                        condition: () => !this.templateForm.categoryIdList.length,
                        errorKey: 'categoryError'
                    },
                    {
                        condition: () => !this.templateForm.description,
                        errorKey: 'descError'
                    }
                ]

                // 遍历校验规则对象
                validationRules.forEach((rule) => {
                    if (rule.condition()) {
                        this.formErrors[rule.errorKey] = true
                        ref = ref || rule.errorKey
                        errorCount++
                    }
                })

                if (errorCount > 0) {
                    const errorEle = this.$refs[ref]
                    if (errorEle) errorEle.scrollIntoView()
                    return false
                }

                return true
            },
            async validate () {
                return await this.$validator.validate()
            },
            addImage (pos, file) {
                this.uploadimg(pos, file)
            },
            delImage (pos) {
                
            },
            changeData (value) {
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
                formData.append('file', file)

                try {
                    const res = await this.$store.dispatch('store/uploadFile', {
                        formData,
                        config
                    })

                    this.$refs.mdHook.$img2Url(pos, res)
                } catch (err) {
                    this.handleError(err)
                    this.$refs.mdHook.$refs.toolbar_left.$imgDel(pos)
                }
            }
        }
    
    }
</script>

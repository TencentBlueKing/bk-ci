<template>
    <div class="edit-atom-wrapper"
        v-bkloading="{
            isLoading: loading.isLoading,
            title: loading.title
        }">

        <h3 class="market-home-title">
            <icon class="title-icon" name="color-logo-store" size="25" />
            <p class="title-name">
                <span class="back-home" @click="toAtomStore()">研发商店</span>
                <i class="right-arrow banner-arrow"></i>
                <span class="back-home" @click="toAtomList()">工作台</span>
                <i class="right-arrow banner-arrow"></i>
                <span class="">{{ curTitle }}（{{ atomForm.atomCode }}）</span>
            </p>
            <a class="title-work" target="_blank" :href="docsLink">插件指引</a>
        </h3>

        <div class="edit-atom-content" v-if="showContent">
            <form class="bk-form edit-atom-form g-form-radio">
                <div class="bk-form-item name-form-item is-required">
                    <label class="bk-label">名称</label>
                    <div class="bk-form-content atom-item-content is-tooltips">
                        <div style="width: 40%;">
                            <input type="text" class="bk-form-input atom-name-input" placeholder="请输入中英文名称"
                                ref="atomName"
                                name="atomName"
                                v-model="atomForm.name"
                                v-validate="{
                                    required: true,
                                    max: 20
                                }"
                                :class="{ 'is-danger': errors.has('atomName') }">
                            <p :class="errors.has('atomName') ? 'error-tips' : 'normal-tips'">{{ errors.first("atomName") }}</p>
                        </div>
                        <bk-popover placement="right">
                            <i class="bk-icon icon-info-circle"></i>
                            <template slot="content">
                                <p>插件名称不超过20个字符</p>
                            </template>
                        </bk-popover>
                    </div>
                </div>
                <div class="bk-form-item is-required">
                    <label class="bk-label category-label">范畴</label>
                    <div class="bk-form-content atom-item-content">
                        <bk-radio-group v-model="atomForm.category" class="radio-group">
                            <bk-radio :value="entry.value" v-for="(entry, key) in categoryList" :key="key" @click.native="formErrors.categoryError = false">{{entry.label}}</bk-radio>
                        </bk-radio-group>
                        <div v-if="formErrors.categoryError" class="error-tips">字段有误，请重新选择</div>
                    </div>
                </div>
                <div class="bk-form-item  is-required">
                    <label class="bk-label">分类</label>
                    <div class="bk-form-content atom-item-content atom-classify-content">
                        <bk-select v-model="atomForm.classifyCode" @selected="changeClassify" style="width: 40%;" searchable :clearable="false">
                            <bk-option v-for="(option, index) in sortList"
                                :key="index"
                                :id="option.classifyCode"
                                :name="option.classifyName">
                            </bk-option>
                        </bk-select>
                        <div v-if="formErrors.sortError" class="error-tips">分类不能为空</div>
                    </div>
                </div>
                <div class="bk-form-item is-required">
                    <label class="bk-label env-label">操作系统</label>
                    <div class="bk-form-content atom-item-content">
                        <bk-checkbox-group v-model="atomForm.os">
                            <bk-checkbox :value="entry.value" v-for="(entry, key) in envList" :key="key" @click.native="changeOs(entry.value)">
                                <i :class="{ &quot;bk-icon&quot;: true, [`icon-${entry.icon}`]: true }"></i><span class="bk-checkbox-text">{{ entry.label }}</span>
                            </bk-checkbox>
                        </bk-checkbox-group>
                        <div v-if="formErrors.envError" class="error-tips env-error">操作系统不能为空</div>
                    </div>
                </div>
                <div class="bk-form-item">
                    <label class="bk-label">功能标签</label>
                    <div class="bk-form-content template-item-content">
                        <bk-select placeholder="请选择功能标签"
                            v-model="atomForm.labelIdList"
                            @selected="changeClassify"
                            show-select-all
                            searchable
                            multiple
                        >
                            <bk-option v-for="(option, index) in labelList"
                                :key="index"
                                :id="option.id"
                                :name="option.labelName">
                            </bk-option>
                        </bk-select>
                    </div>
                </div>
                <div class="bk-form-item introduction-form-item is-required">
                    <label class="bk-label">简介</label>
                    <div class="bk-form-content atom-item-content is-tooltips">
                        <input type="text" class="bk-form-input atom-introduction-input" placeholder="插件一句话简介，不超过70个字符"
                            name="introduction"
                            maxlength="70"
                            v-model="atomForm.summary"
                            v-validate="{
                                required: true,
                                max: 70
                            }"
                            :class="{ 'is-danger': errors.has('introduction') }">
                        <bk-popover placement="left">
                            <i class="bk-icon icon-info-circle"></i>
                            <template slot="content">
                                <p>插件一句话简介，不超过70个字符。</p>
                                <p>展示在插件市场以及流水线选择插件页面。</p>
                            </template>
                        </bk-popover>
                    </div>
                    <p :class="errors.has('introduction') ? 'error-tips' : 'normal-tips'">{{ errors.first("introduction") }}</p>
                </div>
                <div class="bk-form-item remark-form-item">
                    <label class="bk-label">详细描述</label>
                    <div class="bk-form-content atom-item-content is-tooltips">
                        <mavon-editor class="atom-remark-input" :placeholder="descTemplate"
                            ref="mdHook"
                            v-model="atomForm.description"
                            :toolbars="toolbarOptions"
                            :external-link="false"
                            @imgAdd="addImage"
                            @imgDel="delImage"
                            @change="changeData"
                        />
                        <bk-popover placement="left">
                            <i class="bk-icon icon-info-circle"></i>
                            <template slot="content">
                                <p>插件详细介绍，请说明插件功能、使用场景、使用限制和受限解决方案[可选]、常见的失败原因和解决方案、以及接口人联系方式。</p>
                                <p>展示在插件市场查看插件详情界面，帮助用户快速了解插件和解决遇到的问题。</p>
                            </template>
                        </bk-popover>
                    </div>
                </div>
                <div class="version-msg">
                    <p class="form-title">版本信息</p>
                    <hr class="cut-line">
                </div>
                <div class="bk-form-item name-form-item is-required">
                    <label class="bk-label">发布者</label>
                    <div class="bk-form-content atom-item-content">
                        <input type="text" class="bk-form-input atom-name-input" placeholder="请输入"
                            name="publisher"
                            v-model="atomForm.publisher"
                            v-validate="{
                                required: true,
                                max: 20
                            }"
                            :class="{ 'is-danger': errors.has('publisher') }">
                        <p :class="errors.has('publisher') ? 'error-tips' : 'normal-tips'">{{ errors.first("publisher") }}</p>
                    </div>
                </div>
                <div class="bk-form-item publish-form-item is-required">
                    <label class="bk-label publish-type-label">发布类型</label>
                    <div class="bk-form-content atom-item-content is-tooltips radio-flex">
                        <section v-if="atomForm.version" style="min-width: 100%;">
                            <bk-radio-group v-model="atomForm.releaseType" class="radio-group">
                                <bk-radio :value="entry.value" v-for="(entry, key) in publishTypeList" :key="key" @click.native="formErrors.releaseTypeError = false">
                                    <bk-popover placement="top" :delay="800" style="margin-top:0;margin-left:0;">
                                        <span class="bk-radio-text">{{entry.label}}</span>
                                        <template slot="content">
                                            <p>{{ entry.desc }}</p>
                                        </template>
                                    </bk-popover>
                                </bk-radio>
                            </bk-radio-group>
                            <div v-if="formErrors.releaseTypeError" class="error-tips">发布类型不能为空</div>
                        </section>
                        <section v-else style="min-width: 100%;">
                            <bk-radio-group v-model="atomForm.releaseType" class="radio-group">
                                <bk-radio :value="entry.value" v-for="(entry, key) in publishShelf" :key="key" @click.native="formErrors.releaseTypeError = false">{{entry.label}}</bk-radio>
                            </bk-radio-group>
                            <div v-if="formErrors.releaseTypeError" class="error-tips">发布类型不能为空</div>
                        </section>
                    </div>
                </div>
                <div class="bk-form-item version-num-form-item is-required" style="margin-top: 10px">
                    <label class="bk-label">版本号</label>
                    <div class="bk-form-content atom-item-content is-tooltips">
                        <p class="version-num-content" style="min-width: 100%;">{{ curVersion }}
                            <span class="version-prompt">（主版本号.次版本号.修正号）</span>
                        </p>
                        <bk-popover placement="left">
                            <i class="bk-icon icon-info-circle"></i>
                            <template slot="content">
                                <p>根据发布类型自动生成</p>
                            </template>
                        </bk-popover>
                    </div>
                </div>
                <div class="bk-form-item release-package-form-item is-required" style="margin-top: 10px">
                    <label class="bk-label">发布包</label>
                    <div class="bk-form-content atom-item-content">
                        <bk-file-upload
                            :post-url="releasePackageUrl"
                            :os="atomForm.os"
                            :tip="'只允许上传 zip 格式的文件'"
                            accept="application/zip"
                            @uploadSuccess="uploadPackageSuccess"
                            @uploadFail="uploadPackageErr"
                        ></bk-file-upload>
                        <div v-if="formErrors.releasePackageError" class="error-tips">发布包不能为空</div>
                    </div>
                </div>
                <div class="bk-form-item versionlog-form-item is-required">
                    <label class="bk-label">版本日志</label>
                    <div class="bk-form-content atom-item-content">
                        <textarea type="text" class="bk-form-input atom-versionlog-input" placeholder=""
                            name="versionContent"
                            v-validate="{
                                required: true
                            }"
                            v-model="atomForm.versionContent"
                            :class="{ 'is-danger': errors.has('versionContent') }">
                        </textarea>
                        <p :class="errors.has('versionContent') ? 'error-tips' : 'normal-tips'">{{ errors.first("versionContent") }}</p>
                    </div>
                </div>
                <div class="form-footer">
                    <button class="bk-button bk-primary" type="button" @click="submit()">提交</button>
                    <button class="bk-button bk-default" type="button" @click="toAtomList()">取消</button>
                </div>
                <div class="atom-logo-box" :class="{ 'is-border': !atomForm.logoUrl }" @click="uploadLogo">
                    <section v-if="atomForm.logoUrl">
                        <img :src="atomForm.logoUrl">
                    </section>
                    <section v-else>
                        <i class="bk-icon icon-plus"></i>
                        <p>上传LOGO</p>
                    </section>
                </div>
            </form>
        </div>
        <atom-logo :show-dialog="showlogoDialog"
            :to-confirm-logo="toConfirmLogo"
            :to-close-dialog="toCloseDialog"
            :file-change="fileChange"
            :selected-url="selectedUrl"
            :is-uploading="isUploading">
        </atom-logo>
    </div>
</template>

<script>
    import atomLogo from '@/components/atom-logo'
    import { toolbars } from '@/utils/editor-options'
    import mavonEditor from 'mavon-editor'
    import bkFileUpload from '@/components/common/file-upload'
    import 'mavon-editor/dist/css/index.css'

    const Vue = window.Vue
    Vue.use(mavonEditor)

    export default {
        components: {
            atomLogo,
            bkFileUpload
        },
        data () {
            return {
                curVersion: '',
                atomName: 'landun-atom-codecc',
                selectedUrl: '',
                initReleaseType: '',
                docsLink: `${DOCS_URL_PREFIX}/所有服务/流水线插件Store/快速入门.html`,
                descTemplate: '',
                GW_URL_PREFIX: GW_URL_PREFIX,
                showContent: false,
                showlogoDialog: false,
                isUploading: false,
                initOs: [],
                categoryList: [
                    { label: '流水线插件', value: 'TASK' }
                    // { label: '流水线触发器', value: 'TRIGGER' }
                ],
                envList: [
                    { label: 'Linux', value: 'LINUX', icon: 'linux-view' },
                    { label: 'Windows', value: 'WINDOWS', icon: 'windows' },
                    { label: 'macOS', value: 'MACOS', icon: 'macos' }
                ],
                publishShelf: [
                    { label: '新上架', value: 'NEW' }
                ],
                publishTypeList: [
                    {
                        label: '非兼容式升级',
                        value: 'INCOMPATIBILITY_UPGRADE',
                        desc: '当新版本输入输出不兼容旧版本时，使用"非兼容式升级"方式，发布后用户需修改流水线中的插件版本号才能使用新版本。'
                    },
                    {
                        label: '兼容式功能更新',
                        value: 'COMPATIBILITY_UPGRADE',
                        desc: '当新版本输入输出兼容旧版本，仅更新功能时，使用"兼容式升级"方式，发布后用户无需修改流水线中的插件版本号，默认使用最新版本。'
                    },
                    {
                        label: '兼容式问题修正',
                        value: 'COMPATIBILITY_FIX',
                        desc: '当新版本为bug fix时，使用"兼容式问题修正"方式，发布后用户无需修改流水线中的插件版本号，默认使用最新版本。'
                    }
                ],
                sortList: [],
                labelList: [],
                img_file: {},
                loading: {
                    isLoading: false,
                    title: ''
                },
                atomForm: {
                    name: '',
                    logoUrl: '',
                    category: 'TASK',
                    classifyCode: '',
                    classifyName: '',
                    os: [],
                    labelIdList: [],
                    summary: '',
                    description: '- 插件功能\n\n- 适用场景\n\n- 使用限制和受限解决方案[可选]\n\n- 常见的失败原因和解决方案',
                    publisher: '',
                    version: '1.0.0',
                    releaseType: 'NEW',
                    versionContent: '',
                    jobType: 'AGENT',
                    visibilityLevel: 'LOGIN_PUBLIC'
                },
                formErrors: {
                    categoryError: false,
                    jobError: false,
                    openSourceError: false,
                    sortError: false,
                    envError: false,
                    releaseTypeError: false,
                    releasePackageError: false
                }
            }
        },
        computed: {
            atomId () {
                return this.$route.params.atomId
            },
            curTitle () {
                return this.$route.name === 'shelfAtom' ? '上架插件' : '升级插件'
            },
            toolbarOptions () {
                return toolbars
            },
            releasePackageUrl () {
                return `${GW_URL_PREFIX}/artifactory/api/user/artifactories/projects/${this.atomForm.projectCode}/atoms/${this.atomForm.atomCode}/versions/${this.curVersion || '1.0.0'}/types/${this.atomForm.releaseType}/archive`
            }
        },
        watch: {
            'atomForm.os' (val) {
                if (this.$route.name === 'upgradeAtom') {
                    const isEqualOs = this.initOs.every(item => this.atomForm.os.indexOf(item) > -1)
                    this.atomForm.releaseType = isEqualOs ? this.initReleaseType : 'INCOMPATIBILITY_UPGRADE'
                }
            },
            'atomForm.releaseType' (val) {
                const tpl = ['INCOMPATIBILITY_UPGRADE', 'COMPATIBILITY_UPGRADE', 'COMPATIBILITY_FIX']
                const temp = this.atomForm.version.split('.')
                
                for (let i = 0; i < temp.length; i++) {
                    if (tpl[i] === val) {
                        temp[i] = (parseInt(temp[i]) + 1).toString()
                    }
                }
                if (val === 'INCOMPATIBILITY_UPGRADE') {
                    temp[1] = '0'
                    temp[2] = '0'
                } else if (val === 'COMPATIBILITY_UPGRADE') {
                    temp[2] = '0'
                }
                this.curVersion = temp.join('.')
                this.formErrors.releaseTypeError = false
            }
        },
        async created () {
            await this.requestAtomlabels()
            await this.requestAtomDetail(this.$route.params.atomId)
            this.requestAtomClassify()
        },
        methods: {
            toAtomList () {
                this.$router.push({
                    name: 'atomList',
                    params: {
                        type: 'atom'
                    }
                })
            },
            toPublishProgress (type, id) {
                this.$router.push({
                    name: 'releaseProgress',
                    params: {
                        releaseType: type,
                        atomId: id
                    }
                })
            },
            toAtomStore () {
                this.$router.push({
                    name: 'atomHome'
                })
            },
            async requestAtomlabels () {
                try {
                    const res = await this.$store.dispatch('store/requestAtomLables')
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
            async requestAtomDetail (atomId) {
                this.loading.isLoading = true

                try {
                    const res = await this.$store.dispatch('store/requestAtomDetail', {
                        atomId: atomId
                    })
                    if (res) {
                        Object.assign(this.atomForm, res, {})
                        this.curVersion = res.version
                        this.atomForm.labelIdList = this.atomForm.labelList.map(item => {
                            return item.id
                        })
                        this.initOs = JSON.parse(JSON.stringify(this.atomForm.os))
                        
                        if (this.atomForm.version) {
                            this.atomForm.releaseType = this.atomForm.releaseType === 'NEW' ? 'INCOMPATIBILITY_UPGRADE' : this.atomForm.releaseType
                            const temp = this.atomForm.version.split('.')
                            temp[0] = (parseInt(temp[0]) + 1).toString()
                            this.curVersion = temp.join('.')
                        } else {
                            this.curVersion = '1.0.0'
                        }

                        this.initReleaseType = this.atomForm.releaseType
                    }
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
            async requestAtomClassify () {
                try {
                    const res = await this.$store.dispatch('store/requestAtomClassify')
                    this.sortList = res
                    this.sortList = this.sortList.filter(item => item.classifyCode !== 'trigger')
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
            changeOs (data) {
                this.formErrors.envError = false
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
                    this.$refs.atomName.focus()
                })
            },
            uploadLogo () {
                this.showlogoDialog = true
                this.selectedUrl = this.atomForm.logoUrl
            },
            async toConfirmLogo () {
                if (this.selectedUrl) {
                    this.atomForm.logoUrl = this.selectedUrl
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
            addImage (pos, file) {
                this.uploadimg(pos, file)
            },
            delImage (pos) {
                
            },
            changeData (value, render) {
                // console.log(value, render)
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
            uploadPackageSuccess (data) {
                if (data.atomEnvRequest) {
                    this.atomForm.packageShaContent = data.atomEnvRequest.shaContent
                    this.atomForm.pkgName = data.atomEnvRequest.pkgName
                    this.formErrors.releasePackageError = false
                }
            },
            uploadPackageErr (message) {
                if (message) {
                    this.$bkMessage({
                        message: message,
                        theme: 'error'
                    })
                }
            },
            checkValid () {
                let errorCount = 0
                if (!this.atomForm.classifyCode) {
                    this.formErrors.sortError = true
                    errorCount++
                }

                if (!this.atomForm.os.length) {
                    this.formErrors.envError = true
                    errorCount++
                }

                if (!this.atomForm.releaseType) {
                    this.formErrors.releaseTypeError = true
                    errorCount++
                }

                if (this.categoryList.find(x => x.value === this.atomForm.category) < 0) {
                    this.formErrors.categoryError = true
                    errorCount++
                }

                if (!this.atomForm.packageShaContent) {
                    this.formErrors.releasePackageError = true
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
                    const isEqualOs = this.initOs.every(item => this.atomForm.os.indexOf(item) > -1)
                    
                    try {
                        if (this.atomForm.releaseType !== 'INCOMPATIBILITY_UPGRADE' && this.$route.name === 'upgradeAtom' && !isEqualOs) {
                            message = '操作系统发生变更，发布类型请选择非兼容式升级，避免影响已有流水线的使用。'
                            theme = 'error'
                        } else {
                            this.loading.isLoading = true

                            const params = {
                                atomCode: this.atomForm.atomCode,
                                name: this.atomForm.name,
                                category: this.atomForm.category,
                                classifyCode: this.atomForm.classifyCode,
                                version: this.curVersion,
                                releaseType: this.atomForm.releaseType,
                                os: this.atomForm.os,
                                labelIdList: this.atomForm.labelIdList,
                                publisher: this.atomForm.publisher,
                                versionContent: this.atomForm.versionContent,
                                logoUrl: this.atomForm.logoUrl || undefined,
                                summary: this.atomForm.summary || undefined,
                                description: this.atomForm.description || undefined,
                                packageShaContent: this.atomForm.packageShaContent,
                                pkgName: this.atomForm.pkgName
                            }
                
                            const res = await this.$store.dispatch('store/editAtom', {
                                projectId: this.atomForm.projectCode,
                                params: params
                            })

                            message = '提交成功'
                            theme = 'success'
                            
                            if (res) {
                                this.toPublishProgress(this.$route.name === 'shelfAtom' ? 'shelf' : 'upgrade', res)
                            }
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

    .edit-atom-wrapper {
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
        .edit-atom-content {
            padding: 20px 0 40px;
            height: calc(100% - 50px);
            overflow: auto;
            display: flex;
            justify-content: center;
        }
        .edit-atom-form {
            position: relative;
            width: 1200px;
            .bk-label {
                width: 110px;
                font-weight: normal;
            }
            .bk-form-content {
                margin-left: 0;
                .bk-form-checkbox {
                    margin: 6px 21px 6px 0;
                }
            }
            .env-error {
                margin: -4px 0 0;
            }
            .introduction-form-item {
                display: block;
            }
            .bk-selector .bk-form-checkbox {
                display: block;
                padding: 12px 0;
            }
            .atom-classify-content {
                .bk-selector {
                    width: 40%;
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
            .version-num-form-item {
                .bk-tooltip {
                    left: 101%;
                }
            }
            .atom-introduction-input,
            .atom-remark-input {
                min-width: 100%;
            }
            .version-num-form-item {
                .version-num-content {
                    top: 8px;
                    color: #333C48;
                    display: flex;
                    align-items: center;
                }
                .version-prompt {
                    margin-left: 20px;
                    color: $fontWeightColor;
                }
            }
            .bk-radio-text {
                color: #333C48;
            }
            .env-checkbox {
                .bk-icon {
                    position: relative;
                    top: 3px;
                    font-size: 16px;
                    color: $fontWeightColor;
                }
                .bk-checkbox-text {
                    color: #333C48;
                }
            }
            .atom-remark-input {
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
            .atom-versionlog-input {
                padding: 10px;
                height: 80px;
            }
            .form-footer {
                margin: 26px 0 30px 107px;
            }
            .atom-logo-box {
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
                    // border-radius: 50%;
                    z-index: 99;
                    object-fit: cover;
                }
            }
            .is-border {
                border: 1px dashed $lineColor;
            }
        }
        .op-image {
            .dropdown-item:first-child,
            .dropdown-images {
                display: none;
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

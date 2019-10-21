<template>
    <article class="edit-atom-wrapper edit-detail" v-bkloading="{ isLoading }">
        <section class="inner-header">
            <div class="title">插件编辑</div>
        </section>

        <section class="edit-atom-content" v-if="!isLoading">
            <form class="bk-form edit-atom-form g-form-radio">
                <div class="bk-form-item name-form-item is-required">
                    <label class="bk-label">名称</label>
                    <div class="bk-form-content atom-item-content is-tooltips">
                        <div style="width: 70%;">
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
                <div class="bk-form-item  is-required" ref="sortError">
                    <label class="bk-label">分类</label>
                    <div class="bk-form-content atom-item-content atom-classify-content">
                        <bk-select v-model="atomForm.classifyCode" @selected="formErrors.sortError = false" style="width: 70%;" searchable :clearable="false">
                            <bk-option v-for="(option, index) in sortList"
                                :key="index"
                                :id="option.classifyCode"
                                :name="option.classifyName">
                            </bk-option>
                        </bk-select>
                        <div v-if="formErrors.sortError" class="error-tips">分类不能为空</div>
                    </div>
                </div>
                <div class="bk-form-item">
                    <label class="bk-label">功能标签</label>
                    <div class="bk-form-content template-item-content">
                        <bk-select placeholder="请选择功能标签"
                            style="width: 70%;"
                            v-model="atomForm.labelIdList"
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
                <template v-if="userInfo.isProjectAdmin">
                    <div class="bk-form-item is-required is-open" ref="openSourceError">
                        <label class="bk-label">是否开源</label>
                        <div class="bk-form-content atom-item-content">
                            <bk-radio-group v-model="atomForm.visibilityLevel" class="radio-group">
                                <bk-radio :value="entry.value" v-for="(entry, key) in isOpenSource" :key="key" @click.native="changeOpenSource">{{entry.label}}</bk-radio>
                            </bk-radio-group>
                            <div v-if="formErrors.openSourceError" class="error-tips">字段有误，请重新选择</div>
                        </div>
                    </div>
                    <div class="bk-form-item is-required is-open" v-if="atomForm.visibilityLevel === 'PRIVATE'">
                        <label class="bk-label">不开源原因</label>
                        <div class="bk-form-content atom-item-content">
                            <bk-input type="textarea" v-model="atomForm.privateReason" @input="formErrors.privateReasonError = false"></bk-input>
                            <div v-if="formErrors.privateReasonError" class="error-tips">不开源原因不能为空</div>
                        </div>
                    </div>
                </template>
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
                <div class="atom-logo-box" :class="{ 'no-img': atomForm.logoUrl, 'img-Error': formErrors.logoUrlError }" @click="uploadLogo" ref="logoUrlError">
                    <section v-if="atomForm.logoUrl">
                        <img :src="atomForm.logoUrl">
                    </section>
                    <section v-else>
                        <i class="bk-icon icon-plus"></i>
                        <p>上传LOGO</p>
                        <p v-if="formErrors.logoUrlError" class="error-msg">logo必填</p>
                    </section>
                </div>
            </form>
            <bk-button :loading="isSaving" theme="primary" class="edit-atom" @click="saveAtom">保存</bk-button>
        </section>
        <atom-logo :show-dialog="showlogoDialog"
            :to-confirm-logo="toConfirmLogo"
            :to-close-dialog="toCloseDialog"
            :file-change="fileChange"
            :selected-url="selectedUrl"
            type="ATOM"
        >
        </atom-logo>
    </article>
</template>

<script>
    import { mapGetters } from 'vuex'
    import atomLogo from '@/components/common/selectLogo'
    import mavonEditor from 'mavon-editor'
    import 'mavon-editor/dist/css/index.css'

    const Vue = window.Vue
    Vue.use(mavonEditor)

    export default {
        components: {
            atomLogo
        },

        data () {
            return {
                isLoading: false,
                isSaving: false,
                labelList: [],
                sortList: [],
                formErrors: {
                    sortError: false,
                    openSourceError: false,
                    privateReasonError: false,
                    logoUrlError: false
                },
                atomForm: JSON.parse(JSON.stringify(this.$store.state.store.currentAtom)),
                hasChange: false,
                showlogoDialog: false,
                selectedUrl: '',
                isOpenSource: [
                    { label: '是', value: 'LOGIN_PUBLIC' },
                    { label: '否', value: 'PRIVATE' }
                ]
            }
        },

        computed: {
            ...mapGetters('store', {
                'userInfo': 'getUserInfo'
            })
        },

        watch: {
            atomForm: {
                handler () {
                    this.hasChange = true
                },
                deep: true
            }
        },

        beforeRouteLeave (to, from, next) {
            if (this.hasChange) {
                this.$bkInfo({
                    title: '确定离开？',
                    subTitle: '有修改的数据未保存，是否离开当前页面',
                    confirmFn: next
                })
            } else {
                next()
            }
        },

        created () {
            this.initData()
            this.hackData()
        },

        methods: {
            hackData () {
                this.atomForm.labelIdList = this.atomForm.labelList.map(label => label.id)
                this.atomForm.description = this.atomForm.description || '#### 插件功能\n\n#### 适用场景\n\n#### 使用限制和受限解决方案[可选]\n\n#### 常见的失败原因和解决方案'
            },

            changeOpenSource () {
                this.atomForm.privateReason = ''
                this.formErrors.openSourceError = false
                this.formErrors.privateReasonError = false
            },

            async saveAtom () {
                const validate = await this.check()
                if (!validate) return

                this.isSaving = true
                const { name, classifyCode, summary, description, logoUrl, publisher, labelIdList, visibilityLevel, privateReason } = this.atomForm
                const putData = {
                    atomCode: this.$route.params.atomCode,
                    data: { name, classifyCode, summary, description, logoUrl, publisher, labelIdList, visibilityLevel, privateReason }
                }
                this.$store.dispatch('store/modifyAtomDetail', putData).then(() => {
                    this.$store.dispatch('store/updateCurrentaAtom', { res: this.atomForm })
                    this.hasChange = false
                    this.$router.push({ name: 'detail' })
                }).catch((err) => this.$bkMessage({ message: err.message || err, theme: 'error' })).finally(() => (this.isSaving = false))
            },

            async check () {
                let isGood = true

                if (!this.atomForm.classifyCode) {
                    this.formErrors.sortError = true
                    isGood = false
                }

                if (this.isOpenSource.findIndex((x) => x.value === this.atomForm.visibilityLevel) <= -1) {
                    this.formErrors.openSourceError = true
                    isGood = false
                }

                if (this.atomForm.visibilityLevel === 'PRIVATE' && !this.atomForm.privateReason) {
                    this.formErrors.privateReasonError = true
                    isGood = false
                }

                if (!this.atomForm.logoUrl) {
                    this.formErrors.logoUrlError = true
                    isGood = false
                }

                const veeValidate = await this.$validator.validateAll()
                if (!veeValidate) isGood = false

                return isGood
            },

            toCloseDialog () {
                this.showlogoDialog = false
                this.selectedFile = undefined
                this.resetUploadInput()
            },

            async toConfirmLogo () {
                if (this.selectedUrl) {
                    this.atomForm.logoUrl = this.selectedUrl
                    this.showlogoDialog = false
                    this.formErrors.logoUrlError = false
                } else if (!this.selectedUrl) {
                    this.$bkMessage({
                        message: '请选择要上传的图片',
                        theme: 'error'
                    })
                }
                this.resetUploadInput()
            },

            resetUploadInput () {
                this.$nextTick(() => {
                    const inputElement = document.getElementById('inputfile')
                    inputElement.value = ''
                })
            },

            uploadLogo () {
                this.showlogoDialog = true
                this.selectedUrl = this.atomForm.logoUrl
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

            addImage (pos, file) {
                this.uploadimg(pos, file)
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

            initData () {
                this.isLoading = true
                Promise.all([this.requestAtomlabels(), this.requestAtomClassify()]).catch((err) => {
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                }).finally(() => (this.isLoading = false))
            },

            requestAtomlabels () {
                return this.$store.dispatch('store/requestAtomLables').then((res) => (this.labelList = res || []))
            },

            requestAtomClassify () {
                return this.$store.dispatch('store/requestAtomClassify').then((res) => {
                    this.sortList = res
                    this.sortList = this.sortList.filter(item => item.classifyCode !== 'trigger')
                })
            }
        }
    }
</script>

<style lang="scss" scope>
    @import './../../assets/scss/conf';
    .edit-detail {
        .inner-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 18px 20px;
            width: 100%;
            height: 60px;
            border-bottom: 1px solid $borderWeightColor;
            background-color: #fff;
            box-shadow:0px 2px 5px 0px rgba(51,60,72,0.03);
            .title {
                font-size: 16px;
            }
            .header-edit {
                font-size: 16px;
                color: $primaryColor;
                cursor: pointer;
            }
        }

        .edit-atom-content {
            display: block;
            padding: 40px 30px;
            height: calc(100% - 60px);
            .edit-atom {
                margin: 20px 110px;
            }
        }
    }
</style>

<template>
    <article class="edit-image-home">
        <div class="info-header">
            <div class="title first-level" @click="toAtomStore">
                <logo :name="&quot;store&quot;" size="30" class="nav-icon" />
                <div class="title first-level">研发商店</div>
            </div>
            <i class="right-arrow"></i>
            <div class="title secondary" @click="toImageList">工作台</div>
            <i class="right-arrow"></i>
            <div class="title third-level">上架/升级镜像 ({{form.imageName}})</div>
            <a class="develop-guide-link" target="_blank" href="http://iwiki.oa.com/pages/viewpage.action?pageId=22118721">镜像指引</a>
        </div>
        <main v-bkloading="{ isLoading }" class="edit-content">
            <bk-form ref="imageForm" class="edit-image" label-width="125" :model="form" v-show="!isLoading">
                <bk-form-item class="wt660" label="镜像名称" :required="true" property="imageName" :rules="[requireRule]" ref="imageName">
                    <bk-input v-model="form.imageName" placeholder="请输入镜像名称"></bk-input>
                </bk-form-item>
                <bk-form-item class="wt660" label="分类" :required="true" property="classifyCode" :rules="[requireRule]" ref="classifyCode">
                    <bk-select v-model="form.classifyCode" searchable>
                        <bk-option v-for="(option, index) in classifys"
                            :key="index"
                            :id="option.classifyCode"
                            :name="option.classifyName"
                            :placeholder="'请选择分类'"
                        >
                        </bk-option>
                    </bk-select>
                </bk-form-item>
                <bk-form-item class="wt660" label="标签" property="labelIdList">
                    <bk-select v-model="form.labelIdList" searchable multiple show-select-all>
                        <bk-option v-for="(option, index) in labelList"
                            :key="index"
                            :id="option.id"
                            :name="option.labelName"
                            :placeholder="'请选择功能标签'"
                        >
                        </bk-option>
                    </bk-select>
                </bk-form-item>
                <bk-form-item label="简介" property="summary">
                    <bk-input v-model="form.summary" placeholder="请输入简介"></bk-input>
                </bk-form-item>
                <bk-form-item label="描述" property="description">
                    <mavon-editor class="image-remark-input"
                        ref="mdHook"
                        v-model="form.description"
                        :toolbars="toolbars"
                        :external-link="false"
                        :box-shadow="false"
                        @imgAdd="uploadimg"
                    />
                </bk-form-item>
                <div class="version-msg">
                    <p class="form-title">镜像信息</p>
                    <hr class="cut-line">
                </div>
                <bk-form-item label="镜像源" :required="true" property="imageSourceType" class="h32" :rules="[requireRule]" ref="imageSourceType">
                    <bk-radio-group v-model="form.imageSourceType" class="mt6" @change="clearRepo">
                        <bk-radio value="BKDEVOPS" class="mr12">蓝盾源</bk-radio>
                        <bk-radio value="THIRD">第三方源</bk-radio>
                    </bk-radio-group>
                </bk-form-item>
                <template v-if="form.imageSourceType === 'BKDEVOPS'">
                    <bk-form-item label="源镜像" :required="true" property="imageRepoName" :rules="[requireRule]" ref="imageRepoName">
                        <bk-select v-model="form.imageRepoName" searchable>
                            <bk-option v-for="(option, index) in imageList"
                                :key="index"
                                :id="option.repo"
                                :name="option.name"
                                :placeholder="'请选择源镜像'"
                                @click.native="getImageTagList(option)"
                            >
                            </bk-option>
                        </bk-select>
                    </bk-form-item>
                    <bk-form-item label="源镜像Tag" :required="true" property="imageTag" :rules="[requireRule]" ref="imageTag">
                        <bk-select v-model="form.imageTag" searchable :loading="isLoadingTag">
                            <bk-option v-for="(option, index) in imageVersionList"
                                :key="index"
                                :id="option.tag"
                                :name="option.tag"
                                :placeholder="'请选择源镜像Tag'"
                            >
                            </bk-option>
                        </bk-select>
                    </bk-form-item>
                </template>
                <template v-else>
                    <bk-form-item label="源镜像库地址" property="imageRepoUrl">
                        <bk-input v-model="form.imageRepoUrl" placeholder="请输入源镜像库地址"></bk-input>
                    </bk-form-item>
                    <bk-form-item label="源镜像名称" property="imageRepoName" :required="true" :rules="[requireRule]" ref="imageRepoName">
                        <bk-input v-model="form.imageRepoName" placeholder="请输入源镜像名称"></bk-input>
                    </bk-form-item>
                    <bk-form-item label="源镜像Tag" property="imageTag" :rules="[requireRule]" desc="请输入">
                        <bk-input v-model="form.imageTag" placeholder="请输入源镜像Tag"></bk-input>
                    </bk-form-item>
                    <bk-form-item label="凭证" property="ticketId" :rules="[requireRule]" desc="若为私有镜像，请提供凭证，用于流水线执行时拉取镜像">
                        <bk-select v-model="form.ticketId" searchable placeholder="请选择凭证">
                            <bk-option v-for="option in ticketList"
                                :key="option.credentialId"
                                :id="option.credentialId"
                                :name="option.credentialId">
                            </bk-option>
                        </bk-select>
                    </bk-form-item>
                </template>
                <div class="version-msg">
                    <p class="form-title">版本信息</p>
                    <hr class="cut-line">
                </div>
                <bk-form-item label="发布类型" :required="true" property="releaseType" class="h32" :rules="[requireRule]" ref="releaseType" v-if="form.releaseType !== 'CANCEL_RE_RELEASE'">
                    <bk-radio-group v-model="form.releaseType" class="mt6">
                        <bk-radio value="NEW" class="mr12" v-if="form.imageStatus === 'INIT'">新上架</bk-radio>
                        <template v-else>
                            <bk-radio value="INCOMPATIBILITY_UPGRADE" class="mr12">非兼容升级</bk-radio>
                            <bk-radio value="COMPATIBILITY_UPGRADE" class="mr12">兼容式功能更新</bk-radio>
                            <bk-radio value="COMPATIBILITY_FIX">兼容式问题修正</bk-radio>
                        </template>
                    </bk-radio-group>
                </bk-form-item>
                <bk-form-item label="版本号" property="version" class="lh30" :required="true">
                    <span>{{form.version}} (主版本号，次版本号，修正号，如1.0.0)</span>
                    <span class="version-modify" @click="form.releaseType = 'COMPATIBILITY_FIX'" v-if="form.releaseType === 'CANCEL_RE_RELEASE'">修改</span>
                </bk-form-item>
                <bk-form-item label="发布者" :required="true" property="publisher" :rules="[requireRule]" ref="publisher">
                    <bk-input v-model="form.publisher" placeholder="请输入发布者"></bk-input>
                </bk-form-item>
                <bk-form-item label="版本日志" :required="true" property="versionContent" :rules="[requireRule]" ref="versionContent">
                    <bk-input type="textarea" v-model="form.versionContent" placeholder="请输入版本日志"></bk-input>
                </bk-form-item>
                <bk-form-item>
                    <bk-button theme="primary" @click.native="submitImage">提交</bk-button>
                    <bk-button @click.native="toImageList">取消</bk-button>
                </bk-form-item>
                <select-logo ref="selectLogo" label="Logo" :form="form" type="IMAGE" :is-err="logoErr" right="25"></select-logo>
            </bk-form>
        </main>
    </article>
</template>

<script>
    import { mapActions } from 'vuex'
    import { toolbars } from '@/utils/editor-options'
    import selectLogo from '@/components/common/selectLogo'

    export default {
        components: {
            selectLogo
        },

        data () {
            return {
                form: {
                    imageName: '',
                    classifyCode: '',
                    labelIdList: [],
                    labelList: [],
                    summary: '',
                    description: '- 镜像功能\n\n- 适用场景\n\n- 注意事项',
                    logoUrl: '',
                    imageSourceType: 'BKDEVOPS',
                    imageRepoUrl: '',
                    imageRepoName: '',
                    imageTag: '',
                    releaseType: '',
                    version: '1.0.0',
                    publisher: '',
                    versionContent: '',
                    ticketId: ''
                },
                ticketList: [],
                classifys: [],
                labelList: [],
                imageList: [],
                imageVersionList: [],
                isLoading: false,
                isLoadingTag: false,
                originVersion: '',
                requireRule: {
                    required: true,
                    message: '必填项',
                    trigger: 'blur'
                },
                logoErr: false,
                toolbars
            }
        },

        watch: {
            'form.releaseType': {
                handler (val) {
                    switch (val) {
                        case 'NEW':
                            this.form.version = '1.0.0'
                            break
                        case 'INCOMPATIBILITY_UPGRADE':
                            this.form.version = this.originVersion.replace(/(.)\.(.)\.(.)/, (a, b, c, d) => (`${+b + 1}.0.0`))
                            break
                        case 'COMPATIBILITY_UPGRADE':
                            this.form.version = this.originVersion.replace(/(.)\.(.)\.(.)/, (a, b, c, d) => (`${b}.${+c + 1}.0`))
                            break
                        case 'COMPATIBILITY_FIX':
                            this.form.version = this.originVersion.replace(/(.)\.(.)\.(.)/, (a, b, c, d) => (`${b}.${c}.${+d + 1}`))
                            break
                        default:
                            break
                    }
                },
                immediate: true
            }
        },

        created () {
            this.getImageDetail()
        },

        methods: {
            ...mapActions('store', [
                'requestImageDetail',
                'requestImageClassifys',
                'requestImageLabel',
                'requestImageList',
                'requestImageTagList',
                'requestTicketList',
                'requestReleaseImage'
            ]),

            submitImage () {
                this.$refs.imageForm.validate().then(() => {
                    if (!this.form.logoUrl) {
                        this.logoErr = true
                        const err = { field: 'selectLogo' }
                        throw err
                    }
                    this.isLoading = true
                    this.requestReleaseImage(this.form).then((imageId) => {
                        this.$bkMessage({ message: '提交成功', theme: 'success' })
                        this.$router.push({ name: 'imageProgress', params: { imageId } })
                    }).catch((err) => this.$bkMessage({ message: err.message || err, theme: 'error' })).finally(() => {
                        this.isLoading = false
                    })
                }).catch((validate) => {
                    const field = validate.field
                    const label = this.$refs[field].label
                    this.$bkMessage({ message: `${label}是必填项，请填写以后重试`, theme: 'error' })
                })
            },

            clearRepo () {
                this.form.imageRepoUrl = ''
                this.form.imageRepoName = ''
                this.form.imageTag = ''
                this.form.ticketId = ''
            },

            getImageTagList (option) {
                this.form.imageRepoUrl = option.repoUrl
                this.form.imageTag = ''
                this.isLoadingTag = true
                this.requestImageTagList(option.repo).then((res) => {
                    this.imageVersionList = res.tags || []
                }).catch((err) => this.$bkMessage({ message: err.message || err, theme: 'error' })).finally(() => {
                    this.isLoadingTag = false
                })
            },

            getImageDetail () {
                const params = this.$route.params || {}
                const imageId = params.imageId || ''
                this.isLoading = true
                this.requestImageDetail(imageId).then((res) => {
                    this.form = res
                    this.originVersion = res.version
                    this.form.labelIdList = res.labelList.map(x => x.id)
                    switch (res.imageStatus) {
                        case 'INIT':
                            this.form.releaseType = 'NEW'
                            break
                        case 'GROUNDING_SUSPENSION':
                            this.form.releaseType = 'CANCEL_RE_RELEASE'
                            break
                        default:
                            this.form.releaseType = 'COMPATIBILITY_FIX'
                            break
                    }

                    return Promise.all([this.requestImageClassifys(), this.requestImageLabel(), this.requestImageList(res.projectCode), this.requestTicketList({ projectCode: res.projectCode })]).then(([classifys, labels, imageList, ticket]) => {
                        this.classifys = classifys
                        this.labelList = labels
                        this.imageList = imageList.imageList
                        this.ticketList = ticket.records || []
                        if (this.form.imageRepoName && this.form.imageSourceType === 'BKDEVOPS') {
                            return this.requestImageTagList(this.form.imageRepoName).then((res) => {
                                this.imageVersionList = res.tags || []
                            })
                        }
                    })
                }).catch((err) => this.$bkMessage({ message: err.message || err, theme: 'error' })).finally(() => {
                    this.isLoading = false
                })
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

            toImageList () {
                this.$router.push({
                    name: 'atomList',
                    params: {
                        type: 'image'
                    }
                })
            },

            toAtomStore () {
                this.$router.push({
                    name: 'atomHome'
                })
            }
        }
    }
</script>

<style lang="scss" scope>
    @import '@/assets/scss/conf.scss';
    .edit-image-home {
        height: 100%;
        overflow: hidden;
    }

    .version-msg {
        margin: 30px 0 20px;
    }

    .h32 {
        height: 32px;
    }

    .mt6 {
        margin-top: 6px;
    }

    .mr12 {
        margin-right: 12px;
    }

    .lh30 {
        line-height: 30px;
    }

    .edit-content {
        height: calc(100% - 50px);
        overflow: auto;
    }

    .edit-image {
        width: 1200px;
        margin: 20px auto;
        position: relative;
        .image-remark-input {
            height: 263px;
            &.fullscreen {
                height: auto;
            }
        }
    }

    .version-modify {
        cursor: pointer;
        color: $primaryColor;
        margin-left: 3px;
    }

    .bk-form-item {
        padding-right: 25px;
        &.is-error .bk-select {
            border-color: $dangerColor;
        }
    }

    .wt660 {
        width: 660px;
    }

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
</style>

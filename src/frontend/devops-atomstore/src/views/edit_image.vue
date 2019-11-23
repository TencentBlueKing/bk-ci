<template>
    <article class="edit-image-home">
        <div class="info-header">
            <div class="title first-level" @click="toAtomStore">
                <logo :name="&quot;store&quot;" size="30" class="nav-icon" />
                <div class="title first-level"> {{ $t('研发商店') }} </div>
            </div>
            <i class="right-arrow"></i>
            <div class="title secondary" @click="toImageList"> {{ $t('工作台') }} </div>
            <i class="right-arrow"></i>
            <div class="title third-level">({{$t('上架/升级镜像') + form.imageName}})</div>
            <a class="develop-guide-link" target="_blank" href="http://tempdocklink/pages/viewpage.action?pageId=22118721"> {{ $t('镜像指引') }} </a>
        </div>
        <main v-bkloading="{ isLoading }" class="edit-content">
            <bk-form ref="imageForm" class="edit-image" label-width="125" :model="form" v-show="!isLoading">
                <bk-form-item class="wt660" :label="$t('镜像名称')" :required="true" property="imageName" :rules="[requireRule]" ref="imageName">
                    <bk-input v-model="form.imageName" :placeholder="$t('请输入镜像名称')"></bk-input>
                </bk-form-item>
                <bk-form-item class="wt660" :label="$t('范畴')" property="category" :required="true" :rules="[requireRule]" ref="category">
                    <bk-select v-model="form.category" searchable>
                        <bk-option v-for="(option, index) in categoryList"
                            :key="index"
                            :id="option.categoryCode"
                            :name="option.categoryName"
                            :placeholder="$t('请选择范畴')"
                        >
                        </bk-option>
                    </bk-select>
                </bk-form-item>
                <bk-form-item class="wt660" :label="$t('分类')" :required="true" property="classifyCode" :rules="[requireRule]" ref="classifyCode">
                    <bk-select v-model="form.classifyCode" searchable>
                        <bk-option v-for="(option, index) in classifys"
                            :key="index"
                            :id="option.classifyCode"
                            :name="option.classifyName"
                            :placeholder="$t('请选择分类')"
                        >
                        </bk-option>
                    </bk-select>
                </bk-form-item>
                <bk-form-item :label="$t('标签')" property="labelIdList">
                    <bk-tag-input v-model="form.labelIdList" :list="labelList" display-key="labelName" search-key="labelName" trigger="focus" :placeholder="$t('请选择标签')"></bk-tag-input>
                </bk-form-item>
                <bk-form-item :label="$t('适用机器')" property="agentTypeScope" :required="true" :rules="[requireRule]" ref="agentTypeScope">
                    <bk-select v-model="form.agentTypeScope" searchable multiple show-select-all>
                        <bk-option v-for="(option, index) in agentTypes"
                            :key="index"
                            :id="option.id"
                            :name="option.name"
                            :placeholder="$t('请选择适用机器')"
                        >
                        </bk-option>
                    </bk-select>
                </bk-form-item>
                <bk-form-item :label="$t('简介')" property="summary" :required="true" :rules="[requireRule]" ref="summary">
                    <bk-input v-model="form.summary" :placeholder="$t('请输入简介')"></bk-input>
                </bk-form-item>
                <bk-form-item :label="$t('描述')" property="description">
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
                    <p class="form-title"> {{ $t('镜像信息') }} </p>
                    <hr class="cut-line">
                </div>
                <bk-form-item :label="$t('镜像源')" :required="true" property="imageSourceType" class="h32" :rules="[requireRule]" ref="imageSourceType" v-if="VERSION_TYPE !== 'ee'">
                    <bk-radio-group v-model="form.imageSourceType" @change="clearRepo">
                        <bk-radio value="BKDEVOPS" class="mr12"> {{ $t('蓝盾源') }} </bk-radio>
                        <bk-radio value="THIRD"> {{ $t('第三方源') }} </bk-radio>
                    </bk-radio-group>
                </bk-form-item>
                <template v-if="form.imageSourceType === 'BKDEVOPS'">
                    <bk-form-item :label="$t('源镜像')" :required="true" property="imageRepoName" :rules="[requireRule]" ref="imageRepoName">
                        <bk-select v-model="form.imageRepoName" searchable>
                            <bk-option v-for="(option, index) in imageList"
                                :key="index"
                                :id="option.repo"
                                :name="option.repo"
                                :placeholder="$t('请选择源镜像')"
                                @click.native="getImageTagList(option)"
                            >
                            </bk-option>
                        </bk-select>
                    </bk-form-item>
                    <bk-form-item :label="$t('源镜像Tag')" :desc="$t('请选择源镜像Tag，注意已发布过的Tag不能重复发布，请不要使用可变功能的Tag（如latest），避免镜像变更导致关联流水线不能正常执行')" :required="true" property="imageTag" :rules="[requireRule]" ref="imageTag">
                        <bk-select v-model="form.imageTag" searchable :loading="isLoadingTag">
                            <bk-option v-for="(option, index) in imageVersionList"
                                :key="index"
                                :id="option.tag"
                                :name="option.tag"
                                :disabled="option.storeFlag"
                                :placeholder="$t('请选择源镜像Tag')"
                            >
                            </bk-option>
                        </bk-select>
                    </bk-form-item>
                </template>
                <template v-else>
                    <bk-form-item :label="$t('源镜像库地址')" property="imageRepoUrl" :desc="$t('请输入源镜像库地址。若源为 docker hub，可留空不填')">
                        <bk-input v-model="form.imageRepoUrl" :placeholder="$t('请输入源镜像库地址，如 csighub.tencentyun.com')"></bk-input>
                    </bk-form-item>
                    <bk-form-item :label="$t('源镜像名称')" property="imageRepoName" :required="true" :rules="[requireRule]" ref="imageRepoName">
                        <bk-input v-model="form.imageRepoName" :placeholder="$t('请输入源镜像名称，如 XXX/XXXX')"></bk-input>
                    </bk-form-item>
                    <bk-form-item :label="$t('源镜像Tag')" property="imageTag" :desc="$t('请不要使用可变功能的Tag（如latest），避免镜像变更导致关联流水线不能正常执行')" :required="true" :rules="[requireRule, latestRule]" ref="imageTag">
                        <bk-input v-model="form.imageTag" :placeholder="$t('请输入源镜像Tag，如 enterprise-6.0.3')"></bk-input>
                    </bk-form-item>
                    <bk-form-item :label="$t('凭证')" property="ticketId" :desc="$t('若为私有镜像，请提供凭证，用于流水线执行时拉取镜像')">
                        <bk-select v-model="form.ticketId" searchable :placeholder="$t('请选择凭证')">
                            <bk-option v-for="option in ticketList"
                                :key="option.credentialId"
                                :id="option.credentialId"
                                :name="option.credentialId">
                            </bk-option>
                            <a v-if="form.projectCode" :href="`/console/ticket/${form.projectCode}/createCredential/USERNAME_PASSWORD/true`" slot="extension" target="_blank"> {{ $t('新增凭证') }} </a>
                        </bk-select>
                    </bk-form-item>
                </template>
                <div class="version-msg">
                    <p class="form-title"> {{ $t('版本信息') }} </p>
                    <hr class="cut-line">
                </div>
                <bk-form-item :label="$t('发布类型')" :required="true" property="releaseType" class="h32" :rules="[requireRule]" ref="releaseType" v-if="form.releaseType !== 'CANCEL_RE_RELEASE'">
                    <bk-radio-group v-model="form.releaseType">
                        <bk-radio value="NEW" class="mr12" v-if="form.imageStatus === 'INIT'"> {{ $t('新上架') }} </bk-radio>
                        <template v-else>
                            <bk-radio value="INCOMPATIBILITY_UPGRADE" class="mr12"> {{ $t('非兼容升级') }} </bk-radio>
                            <bk-radio value="COMPATIBILITY_UPGRADE" class="mr12"> {{ $t('兼容式功能更新') }} </bk-radio>
                            <bk-radio value="COMPATIBILITY_FIX"> {{ $t('兼容式问题修正') }} </bk-radio>
                        </template>
                    </bk-radio-group>
                </bk-form-item>
                <bk-form-item :label="$t('版本号')" property="version" class="lh30" :required="true">
                    <span>{{form.version + $t('（主版本号.次版本号.修正号）')}}</span>
                    <span class="version-modify" @click="form.releaseType = 'COMPATIBILITY_FIX'" v-if="form.releaseType === 'CANCEL_RE_RELEASE'"> {{ $t('修改') }} </span>
                </bk-form-item>
                <bk-form-item :label="$t('发布者')" :required="true" property="publisher" :rules="[requireRule]" ref="publisher">
                    <bk-input v-model="form.publisher" :placeholder="$t('请输入发布者')"></bk-input>
                </bk-form-item>
                <bk-form-item :label="$t('版本日志')" :required="true" property="versionContent" :rules="[requireRule]" ref="versionContent">
                    <bk-input type="textarea" v-model="form.versionContent" :placeholder="$t('请输入版本日志')"></bk-input>
                </bk-form-item>
                <select-logo ref="selectLogo" label="Logo" :form="form" type="IMAGE" :is-err="logoErr" right="25"></select-logo>
            </bk-form>
            <section class="edit-image button-padding" v-show="!isLoading">
                <bk-button theme="primary" @click="submitImage"> {{ $t('提交') }} </bk-button>
                <bk-button @click="toImageList"> {{ $t('取消') }} </bk-button>
            </section>
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
                    imageId: '',
                    imageName: '',
                    classifyCode: '',
                    labelIdList: [],
                    labelList: [],
                    summary: '',
                    description: '',
                    logoUrl: '',
                    imageSourceType: 'BKDEVOPS',
                    imageRepoUrl: '',
                    imageRepoName: '',
                    imageTag: '',
                    releaseType: '',
                    version: '1.0.0',
                    publisher: '',
                    versionContent: '',
                    ticketId: '',
                    projectCode: '',
                    category: '',
                    agentTypeScope: []
                },
                ticketList: [],
                classifys: [],
                labelList: [],
                categoryList: [],
                agentTypes: [
                    { name: this.$t('Devnet 物理机'), id: 'DOCKER' },
                    { name: 'IDC CVM', id: 'IDC' },
                    { name: 'DevCloud', id: 'PUBLIC_DEVCLOUD' }
                ],
                imageList: [],
                imageVersionList: [],
                isLoading: false,
                isLoadingTag: false,
                originVersion: '',
                requireRule: {
                    required: true,
                    message: this.$t('必填项'),
                    trigger: 'blur'
                },
                latestRule: {
                    validator (val) {
                        return val !== 'latest'
                    },
                    message: this.$t('镜像tag不能是latest'),
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
                'requestImageCategorys',
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
                        this.$bkMessage({ message: this.$t('提交成功'), theme: 'success' })
                        this.$router.push({ name: 'imageProgress', params: { imageId } })
                    }).catch((err) => this.$bkMessage({ message: err.message || err, theme: 'error' })).finally(() => {
                        this.isLoading = false
                    })
                }).catch((validate) => {
                    const field = validate.field
                    const label = this.$refs[field].label
                    this.$bkMessage({ message: `${label + this.$t('是必填项，请填写以后重试')}`, theme: 'error' })
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

                const imageRepo = option.repo
                const imageId = this.form.imageId
                this.requestImageTagList({ imageRepo, imageId }).then((res) => {
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
                    Object.assign(this.form, res)
                    this.form.imageTag = ''
                    this.form.description = this.form.description || this.$t('### 功能简介\n\n### 如何使用\n\n### 注意事项\n\n### License')
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

                    return Promise.all([
                        this.requestImageClassifys(),
                        this.requestImageLabel(),
                        this.requestImageList(res.projectCode),
                        this.requestTicketList({ projectCode: res.projectCode }),
                        this.requestImageCategorys()]).then(([classifys, labels, imageList, ticket, categorys]) => {
                            this.classifys = classifys
                            this.labelList = labels
                            this.categoryList = categorys
                            this.imageList = imageList.imageList
                            this.ticketList = ticket.records || []
                            if (this.form.imageRepoName && this.form.imageSourceType === 'BKDEVOPS') {
                                const imageRepo = this.form.imageRepoName
                                const imageId = this.form.imageId
                                return this.requestImageTagList({ imageRepo, imageId }).then((res) => {
                                    this.form.imageRepoUrl = res.repoUrl
                                    this.imageVersionList = res.tags || []
                                })
                            }
                        })
                }).catch((err) => this.$bkMessage({ message: err.message || err, theme: 'error' })).finally(() => {
                    this.isLoading = false
                    if (VERSION_TYPE === 'ee') this.form.imageSourceType = 'THIRD'
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

<style lang="scss" scoped>
    @import '@/assets/scss/conf.scss';
    .edit-image-home {
        height: 100%;
        overflow: hidden;
    }

    .button-padding {
        padding-left: 125px;
    }

    .version-msg {
        margin: 30px 0 20px;
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
        .bk-form-control {
            vertical-align: baseline;
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

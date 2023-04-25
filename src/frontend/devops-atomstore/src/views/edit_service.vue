<template>
    <article class="edit-service-home">
        <bread-crumbs :bread-crumbs="navList" type="service">
            <a class="g-title-work" target="_blank" href="https://iwiki.oa.tencent.com/pages/viewpage.action?pageId=103523086"> {{ $t('store.微扩展指引') }} </a>
        </bread-crumbs>

        <main v-bkloading="{ isLoading }" class="edit-content">
            <bk-form ref="serviceForm" class="edit-service" label-width="125" :model="form" v-show="!isLoading">
                <bk-form-item class="wt660"
                    :label="$t('store.名称')"
                    :required="true"
                    property="serviceName"
                    :rules="[requireRule, numMax, nameRule]"
                    ref="serviceName"
                    error-display-type="normal"
                >
                    <bk-input v-model="form.serviceName" :placeholder="$t('store.请输入微扩展名称，不超过20个字符')"></bk-input>
                </bk-form-item>
                <bk-form-item class="wt660"
                    :label="$t('store.扩展点')"
                    :required="true"
                    property="extensionItemList"
                    :rules="[requireRule]"
                    ref="extensionItemList"
                    error-display-type="normal"
                >
                    <bk-select :placeholder="$t('store.请选择扩展点')"
                        class="service-item"
                        :scroll-height="300"
                        :clearable="true"
                        @toggle="getServiceList"
                        :loading="isServiceListLoading"
                        searchable
                        multiple
                        display-tag
                        v-model="form.extensionItemList">
                        <bk-option-group
                            v-for="(group, index) in serviceList"
                            :name="group.name"
                            :key="index">
                            <bk-option v-for="(option, key) in group.children"
                                :key="key"
                                :id="option.id"
                                :name="option.name"
                            >
                            </bk-option>
                        </bk-option-group>
                    </bk-select>
                </bk-form-item>
                <bk-form-item :label="$t('store.标签')"
                    property="labelIdList"
                    class="wt660"
                >
                    <bk-tag-input v-model="form.labelIdList" :list="labelList" display-key="labelName" search-key="labelName" trigger="focus" :placeholder="$t('store.请选择标签')"></bk-tag-input>
                </bk-form-item>
                <bk-form-item :label="$t('store.简介')"
                    property="summary"
                    :required="true"
                    :rules="[requireRule]"
                    ref="summary"
                    error-display-type="normal"
                >
                    <bk-input v-model="form.summary" :placeholder="$t('store.请输入简介')"></bk-input>
                </bk-form-item>
                <bk-form-item :label="$t('store.描述')" property="description">
                    <bk-radio-group v-model="form.descInputType" class="service-input-type">
                        <bk-radio value="MANUAL" class="mr21"> {{ $t('store.手动录入') }} </bk-radio>
                        <bk-radio value="FILE"> {{ $t('store.fromReadme') }} </bk-radio>
                    </bk-radio-group>
                    <mavon-editor class="service-remark-input"
                        v-if="form.descInputType === 'MANUAL'"
                        ref="mdHook"
                        preview-background="#fff"
                        v-model="form.description"
                        :toolbars="toolbars"
                        :external-link="false"
                        :box-shadow="false"
                        @imgAdd="uploadimg('mdHook', ...arguments)"
                    />
                </bk-form-item>
                <div class="version-msg">
                    <p class="form-title"> {{ $t('store.版本信息') }} </p>
                    <hr class="cut-line">
                </div>
                <bk-form-item :label="$t('store.发布类型')"
                    :required="true"
                    property="releaseType"
                    class="h32"
                    :rules="[requireRule]"
                    ref="releaseType"
                    v-if="form.releaseType !== 'CANCEL_RE_RELEASE'"
                    error-display-type="normal"
                >
                    <bk-radio-group v-model="form.releaseType">
                        <bk-radio value="NEW" class="mr12" v-if="form.serviceStatus === 'INIT'"> {{ $t('store.新上架') }} </bk-radio>
                        <template v-else>
                            <bk-radio value="INCOMPATIBILITY_UPGRADE" class="mr12"> {{ $t('store.非兼容升级') }} </bk-radio>
                            <bk-radio value="COMPATIBILITY_UPGRADE" class="mr12"> {{ $t('store.兼容式功能更新') }} </bk-radio>
                            <bk-radio value="COMPATIBILITY_FIX"> {{ $t('store.兼容式问题修正') }} </bk-radio>
                        </template>
                    </bk-radio-group>
                </bk-form-item>
                <bk-form-item :label="$t('store.版本号')"
                    property="version"
                    class="lh30"
                    :required="true"
                    error-display-type="normal"
                >
                    <span>{{$t('store.semverType', [form.version])}}</span>
                    <span class="version-modify" @click="form.releaseType = 'COMPATIBILITY_FIX'" v-if="form.releaseType === 'CANCEL_RE_RELEASE'"> {{ $t('store.修改') }} </span>
                </bk-form-item>
                <bk-form-item :label="$t('store.发布者')"
                    :required="true"
                    property="publisher"
                    :rules="[requireRule]"
                    ref="publisher"
                    error-display-type="normal"
                >
                    <bk-input v-model="form.publisher" :placeholder="$t('store.请输入发布者')"></bk-input>
                </bk-form-item>
                <bk-form-item :label="$t('store.版本日志')"
                    :required="true"
                    property="versionContent"
                    :rules="[requireRule]"
                    ref="versionContent"
                    error-display-type="normal"
                >
                    <mavon-editor class="service-remark-input"
                        :placeholder="$t('store.请输入版本日志')"
                        ref="versionMd"
                        preview-background="#fff"
                        v-model="form.versionContent"
                        :toolbars="toolbars"
                        :external-link="false"
                        :box-shadow="false"
                        @imgAdd="uploadimg('versionMd', ...arguments)"
                    />
                </bk-form-item>
                <select-logo ref="selectLogo" label="Logo" :form="form" type="SERVICE" :is-err="logoErr" right="25"></select-logo>
            </bk-form>
            <section class="edit-service button-padding" v-show="!isLoading">
                <bk-button theme="primary" @click="submitService" :loading="isCommitLoading"> {{ $t('store.提交') }} </bk-button>
                <bk-button @click="$router.back()" :disabled="isCommitLoading"> {{ $t('store.取消') }} </bk-button>
            </section>
        </main>
    </article>
</template>

<script>
    import { mapActions } from 'vuex'
    import { toolbars } from '@/utils/editor-options'
    import selectLogo from '@/components/common/selectLogo'
    import breadCrumbs from '@/components/bread-crumbs.vue'

    export default {
        components: {
            selectLogo,
            breadCrumbs
        },

        data () {
            return {
                form: {
                    serviceId: '',
                    serviceName: '',
                    serviceCode: '',
                    classifyCode: '',
                    labelIdList: [],
                    labelList: [],
                    summary: '',
                    description: '',
                    logoUrl: '',
                    iconData: '',
                    releaseType: '',
                    version: '1.0.0',
                    publisher: '',
                    versionContent: '',
                    projectCode: '',
                    descInputType: 'MANUAL',
                    extensionItemList: []
                },
                classifys: [],
                labelList: [],
                serviceList: [],
                serviceVersionList: [],
                isLoading: false,
                isCommitLoading: false,
                isServiceListLoading: false,
                originVersion: '',
                requireRule: {
                    required: true,
                    message: this.$t('store.必填项'),
                    trigger: 'blur'
                },
                numMax: {
                    validator: (val = '') => (val.length <= 20),
                    message: this.$t('store.字段不超过20个字符'),
                    trigger: 'blur'
                },
                nameRule: {
                    validator: (val) => (/^[\u4e00-\u9fa5a-zA-Z0-9-]*$/.test(val)),
                    message: this.$t('store.由汉字、英文字母、数字、连字符(-)组成，长度小于20个字符'),
                    trigger: 'blur'
                },
                logoErr: false,
                toolbars
            }
        },

        computed: {
            navList () {
                return [
                    { name: this.$t('store.工作台'), to: { name: 'serviceWork' } },
                    { name: `${this.$t('store.上架/升级微扩展')}（${this.form.serviceCode}）` }
                ]
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
                            this.form.version = this.originVersion.replace(/(.+)\.(.+)\.(.+)/, (a, b, c, d) => (`${+b + 1}.0.0`))
                            break
                        case 'COMPATIBILITY_UPGRADE':
                            this.form.version = this.originVersion.replace(/(.+)\.(.+)\.(.+)/, (a, b, c, d) => (`${b}.${+c + 1}.0`))
                            break
                        case 'COMPATIBILITY_FIX':
                            this.form.version = this.originVersion.replace(/(.+)\.(.+)\.(.+)/, (a, b, c, d) => (`${b}.${c}.${+d + 1}`))
                            break
                        default:
                            break
                    }
                },
                immediate: true
            }
        },

        mounted () {
            this.getServiceDetail()
        },

        methods: {
            ...mapActions('store', [
                'requestServiceDetail',
                'requestServiceItemList',
                'requestReleaseService',
                'requestServiceLabel'
            ]),

            submitService () {
                this.$refs.serviceForm.validate().then(() => {
                    if (!this.form.logoUrl && !this.form.iconData) {
                        this.logoErr = true
                        const err = { field: 'selectLogo' }
                        throw err
                    }
                    this.isCommitLoading = true
                    this.requestReleaseService(this.form).then((serviceId) => {
                        this.$bkMessage({ message: this.$t('store.提交成功'), theme: 'success' })
                        this.$router.push({ name: 'serviceProgress', params: { serviceId } })
                    }).catch((err) => this.$bkMessage({ message: err.message || err, theme: 'error' })).finally(() => {
                        this.isCommitLoading = false
                    })
                }).catch((validate) => {
                    const field = validate.field
                    const label = this.$refs[field].label
                    this.$bkMessage({ message: `${label + this.$t('store.输入不正确，请确认修改后再试')}`, theme: 'error' })
                })
            },

            getServiceDetail () {
                const params = this.$route.params || {}
                const serviceId = params.serviceId || ''
                this.isLoading = true

                Promise.all([
                    this.requestServiceDetail(serviceId),
                    this.requestServiceLabel(),
                    this.getServiceList(true)
                ]).then(([res, labels]) => {
                    this.labelList = labels || []
                    Object.assign(this.form, res)
                    if (res.serviceStatus === 'RELEASED') this.form.serviceTag = ''
                    this.form.description = this.form.description || this.$t('store.serviceMdDesc')
                    this.originVersion = res.version

                    switch (res.serviceStatus) {
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
                }).catch((err) => this.$bkMessage({ message: err.message || err, theme: 'error' })).finally(() => {
                    this.isLoading = false
                })
            },

            getServiceList (isOpen) {
                if (!isOpen) return

                this.isServiceListLoading = true
                this.$store.dispatch('store/requestServiceItemList').then((res) => {
                    this.serviceList = (res || []).map((item) => {
                        const serviceItem = item.extServiceItem || {}
                        return {
                            name: serviceItem.name,
                            children: item.childItem || []
                        }
                    })
                }).catch((err) => {
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                }).finally(() => (this.isServiceListLoading = false))
            },

            async uploadimg (ref, pos, file) {
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

                    this.$refs[ref].$img2Url(pos, res)
                } catch (err) {
                    message = err.message ? err.message : err
                    theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                    this.$refs[ref].$refs.toolbar_left.$imgDel(pos)
                }
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '@/assets/scss/conf.scss';
    .tag-list {
        padding: 0 20px;
        line-height: 32px;
        font-size: 12px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        color: #63656e;
    }

    .service-remark-input {
        border: 1px solid #c4c6cc;
        height: 263px;
        &.fullscreen {
            height: auto;
        }
    }

    .select-tag {
        min-height: 30px;
        &::after {
            content: '';
            display: table;
            clear: both;
        }
        li {
            float: left;
            height: 24px;
            background: #f1f2f3;
            border: 1px solid #d9d9d9;
            border-radius: 2px;
            line-height: 24px;
            margin: 3px 5px;
            padding: 0 4px;
            .icon-close {
                margin-right: 3px;
            }
        }
    }

    .edit-service-home {
        height: 100%;
        overflow: hidden;
    }

    .dockerfile {
        height: 400px;
        overflow: auto;
        background: black;
        ::v-deep .CodeMirror {
            font-family: Consolas, "Courier New", monospace;
            line-height: 1.5;
            padding: 10px;
            height: auto;
        }
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

    .mr21 {
        margin-right: 21px;
    }

    .lh30 {
        line-height: 30px;
    }

    .mt10 {
        margin-top: 10px;
    }

    .edit-content {
        height: calc(100% - 5.6vh);
        overflow: auto;
    }

    .edit-service {
        width: 1200px;
        margin: 20px auto;
        position: relative;
        .service-input-type {
            margin-bottom: 10px;
        }
        .service-remark-input {
            border: 1px solid #c4c6cc;
            height: 263px;
            &.fullscreen {
                height: auto;
            }
        }
        .bk-form-control {
            vertical-align: baseline;
        }
        .service-item {
            background-color: #fff;
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
</style>

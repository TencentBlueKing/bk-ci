<template>
    <section>
        <section class="select-logo" ref="selectLogo" :style="`top: ${top}px; right: ${right}px`">
            <section v-if="form.logoUrl || form.iconData" @click="uploadLogo" class="has-upload">
                <img :src="form.logoUrl || form.iconData" :title="$t('store.选择logo')">
                <span>{{ $t('store.editLogo') }}</span>
            </section>
            <section v-else @click="uploadLogo" :class="[{ 'logo-error': isErr }, 'un-upload']">
                <i class="devops-icon icon-plus"></i>
                <p> {{ $t('store.上传LOGO') }} </p>
            </section>
            <p v-if="isErr" class="is-err"> {{ $t('store.Logo必填') }} </p>
        </section>
        <bk-dialog v-model="showDialog"
            header-position="left"
            width="615" :title="$t('store.修改Logo')"
            class="logo-dialog"
            @cancel="toCloseDialog"
        >
            <main class="logo-main" v-bkloading="{ isLoading: loading }">
                <figure class="logo-pic">
                    <img class="pic-img" :src="selectedUrl" v-if="selectedUrl">
                    <icon class="pic-img" v-else name="placeholder" size="88" style="fill:#dcdee5" />
                </figure>

                <section class="logo-choose">
                    <h3 class="choose-upload">
                        <input type="file" name="file" class="input-file" id="inputfile" accept="image/png, image/jpeg" @change="fileChange">
                        <bk-button theme="primary"> {{ $t('store.自定义') }} </bk-button>
                        <p class="upload-info" v-bk-tooltips="$t('store.只允许上传png、jpg，尺寸为大于200*200的正方形，大小不超过2M')">
                            {{ $t('store.只允许上传png、jpg，尺寸为大于200*200的正方形，大小不超过2M') }}
                        </p>
                    </h3>
                    <h3 class="sys-title"> {{ $t('store.系统自带') }} </h3>
                    <hgroup class="choose-sys">
                        <h3 v-for="img in imgs" :key="img.id" @click="chooseSysImg(img.logoUrl)" :class="[{ 'select-icon': selectedUrl === img.logoUrl }, 'sys-icon']">
                            <img :src="img.logoUrl" class="icon">
                        </h3>
                    </hgroup>
                </section>
            </main>
            <template slot="footer">
                <div class="bk-dialog-outer">
                    <bk-button theme="primary" class="bk-dialog-btn bk-dialog-btn-confirm bk-btn-primary"
                        @click="toConfirmLogo"> {{ $t('store.确定') }} </bk-button>
                    <bk-button type="button" class="bk-dialog-btn bk-dialog-btn-cancel" @click="toCloseDialog"> {{ $t('store.取消') }} </bk-button>
                </div>
            </template>
        </bk-dialog>
    </section>
</template>

<script>
    export default {
        props: {
            form: Object,
            type: String,
            isErr: Boolean,
            label: String,
            right: {
                type: Number,
                default: 0
            },
            top: {
                type: Number,
                default: 0
            }
        },

        data () {
            return {
                imgs: [],
                showDialog: false,
                selectedUrl: '',
                iconData: '',
                loading: false
            }
        },

        created () {
            this.getImgs()
        },

        methods: {
            scrollIntoView () {
                this.$refs.selectLogo.scrollIntoView()
            },

            getImgs () {
                this.loading = true
                this.$store.dispatch('store/getLogoUrl', { type: this.type }).then((res) => {
                    this.imgs = res || []
                }).catch(err => this.$bkMessage({ message: (err.message || err), theme: 'error' })).finally(() => {
                    this.loading = false
                })
            },

            chooseSysImg (url) {
                this.selectedUrl = url
                this.iconData = ''
                this.resetUploadInput()
            },

            fileChange (e) {
                const file = e.target.files[0]
                if (file) {
                    let message = ''

                    if (!['image/jpeg', 'image/png'].includes(file.type)) message = this.$t('store.请上传png、jpg格式的图片')

                    if (file.size > (2 * 1024 * 1024)) message = this.$t('store.请上传大小不超过2M的图片')

                    if (message) {
                        this.$bkMessage({ theme: 'error', message })
                    } else {
                        const reader = new FileReader()
                        reader.readAsDataURL(file)
                        reader.onload = evts => {
                            const img = new Image()
                            img.src = evts.target.result
                            img.onload = evt => {
                                if (img.width === img.height && img.height >= 200) {
                                    this.uploadHandle(file)
                                } else {
                                    this.$bkMessage({
                                        theme: 'error',
                                        message: this.$t('store.请上传宽高大于等于200的正方形图片')
                                    })
                                }
                            }
                        }
                    }
                }
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

                    this.selectedUrl = res.logoUrl
                    this.iconData = res.iconData
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
                this.showDialog = true
                this.selectedUrl = this.form.logoUrl
                this.iconData = this.form.iconData
            },

            async toConfirmLogo () {
                if (this.selectedUrl || this.iconData) {
                    this.form.logoUrl = this.selectedUrl
                    this.form.iconData = this.iconData
                    this.showDialog = false
                    this.isErr = false
                } else if (!this.selectedUrl) {
                    this.$bkMessage({
                        message: this.$t('store.请选择要上传的图片'),
                        theme: 'error'
                    })
                }
                this.resetUploadInput()
            },

            toCloseDialog () {
                this.showDialog = false
                this.resetUploadInput()
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '../../../assets/scss/conf';
    @import '../../../assets/scss/mixins/ellipsis';

    .select-logo {
        position: absolute;
        cursor: pointer;
        .un-upload {
            background: rgba(255, 255, 255, 1);
            height: 100px;
            width: 100px;
            border-radius: 2px;
            border: 1px dashed rgba(195, 205, 215, 1);
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            text-align: center;
            &.logo-error {
                border: 1px dashed #ff5656;
            }
        }
        .has-upload {
            position: relative;
            img {
                height: 100px;
                width: 100px;
            }
            > span {
                display: none;
            }
            &:hover {
                > span {
                    display: block;
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
        .is-err {
            color: #ff5656;
            text-align: center;
        }
    }

    .logo-dialog {
        .bk-dialog-tool {
            min-height: 19px;
        }
        .bk-dialog-content .bk-dialog-header .bk-dialog-header-inner {
            font-size: 22px;
        }
    }

    .logo-main {
        .logo-pic {
            margin: 0;
            float: left;
            height: 96px;
            width: 96px;
            background: $fontLightColor;
            border-radius: 2px;
            .pic-img {
                width: 88px;
                height: 88px;
                margin: 4px;
            }
        }
        .logo-choose {
            margin-left: 116px;
            .choose-upload {
                position: relative;
                height: 32px;
                padding-bottom: 19px;
                box-sizing: content-box;
                .input-file {
                    position: absolute;
                    left: 0;
                    height: 32px;
                    line-height: 32px;
                    width: 74px;
                    z-index: 2;
                    cursor: pointer;
                    opacity: 0;
                    &::-webkit-file-upload-button {
                        cursor: pointer;
                    }
                }
                .upload-info {
                    line-height: 32px;
                    font-size: 12px;
                    color: $fontWeightColor;
                    text-align: left;
                    width: 100%;
                    @include ellipsis();
                }
            }
            .sys-title {
                font-size: 14px;
                color: $fontDarkColor;
                line-height: 19px;
                margin-bottom: 8px;
                margin-top: 15px;
            }
            .choose-sys {
                border-top: 1px solid $borderLightColor;
                border-left: 1px solid $borderLightColor;
                display: inline-block;
                &:after {
                    content: '';
                    display: table;
                    clear: both;
                }
                .sys-icon {
                    height: 56px;
                    width: 56px;
                    padding: 9px;
                    background: #fff;
                    float: left;
                    border-right: 1px solid $borderLightColor;
                    border-bottom: 1px solid $borderLightColor;
                    cursor: pointer;
                    .icon {
                        height: 37px;
                        width: 37px;
                    }
                }
                .select-icon {
                    background: $tinyBlue;
                    border: 1px solid $darkerBlue;
                    transform: translate3d(-1px, -1px, 0);
                }
            }
        }
    }
</style>

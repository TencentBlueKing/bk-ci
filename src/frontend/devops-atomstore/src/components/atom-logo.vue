<template>
    <bk-dialog
        class="atom-logo-dialog"
        v-model="showDialog"
        :width="width"
        :has-header="atomLogoConf.hasHeader"
        :close-icon="atomLogoConf.closeIcon"
        :quick-close="atomLogoConf.quickClose">
        <main class="atom-logo-content">
            <i class="bk-icon icon-close" @click="toCloseDialog()"></i>
            <div class="info-title">修改LOGO</div>
            <div class="upload-content">
                <div class="upload-box">
                    <img :src="selectedUrl" v-if="selectedUrl">
                </div>
                <div class="upload-btn">
                    <input type="file" name="file" class="inputfile" id="inputfile"
                        accept="image/png, image/jpeg"
                        @change="fileChange">
                    <label for="file"><i class="bk-icon icon-bk"></i>选择LOGO</label>
                    <p class="logo-desc">只允许上传png、jpg</p>
                    <p class="logo-desc">尺寸为512*512，大小不超过2M</p>
                </div>
            </div>
        </main>
        <template slot="footer">
            <div class="bk-dialog-outer">
                <!-- <template v-if="isUploading">
                    <button type="button" class="bk-dialog-btn bk-dialog-btn-confirm bk-btn-primary disabled">
                        修改中...
                    </button>
                    <button type="button" class="bk-dialog-btn bk-dialog-btn-cancel disabled">
                        取消
                    </button>
                </template> -->
                <template>
                    <bk-button theme="primary" class="bk-dialog-btn bk-dialog-btn-confirm bk-btn-primary"
                        @click="toConfirmLogo">
                        确定
                    </bk-button>
                    <bk-button class="bk-dialog-btn bk-dialog-btn-cancel" @click="toCloseDialog">
                        取消
                    </bk-button>
                </template>
            </div>
        </template>
    </bk-dialog>
</template>

<script>
    export default {
        props: {
            showDialog: Boolean,
            isUploading: Boolean,
            selectedUrl: String,
            toConfirmLogo: Function,
            toCloseDialog: Function,
            fileChange: Function
        },
        data () {
            return {
                width: 507,
                curImage: ''
            }
        },
        computed: {
            atomLogoConf () {
                return {
                    hasHeader: false,
                    closeIcon: false,
                    quickClose: false
                }
            }
        }
    }
</script>

<style lang="scss">
    @import '../assets/scss/conf';

    .atom-logo-dialog {
        .atom-logo-content {
            .icon-close {
                position: absolute;
                top: 0;
                right: 0;
                margin-top: 14px;
                margin-right: 14px;
                font-size: 12px;
                color: $fontLigtherColor;
                cursor: pointer;
            }
            .info-title {
                padding-top: 6px;
                padding-left: 10px;
                font-size: 22px;
                color: #333C48;
            }
        }
        .upload-content {
            display: flex;
            padding: 20px 10px 10px;
        }
        .upload-box {
            margin-right: 16px;
            width: 128px;
            height: 128px;
            border: 1px solid $fontLigtherColor;
            // background: $borderColor;
            &:before {
                content: '';
                position: absolute;
                width: 124px;
                height: 124px;
                background: #fff;
                // border-radius: 50%;
                // border: 1px dashed $fontLigtherColor;
            }
            img {
                position: relative;
                width: 126px;
                height: 126px;
                // border-radius: 50%;
                z-index: 99;
                object-fit: cover;
            }
        }
        .upload-btn {
            color: $fontWeightColor;
            .logo-desc {
                line-height: 24px;
                font-size: 12px;
                color: $fontWeightColor;
                text-align: left;
            }
        }
        .inputfile {
            width: 120px;
            height: 36px;
            opacity: 0;
            overflow: hidden;
            position: absolute;
            cursor: pointer;
        }
        .inputfile + label {
            margin-bottom: 10px;
            width: 120px;
            height: 36px;
            line-height: 36px;
            display: inline-block;
            text-align: center;
            background-color: $primaryColor;
            color: white;
            border-radius: 2px;
            .icon-bk {
                position: relative;
                top: 2px;
                margin-right: 4px;
                font-size: 16px;
            }
        }
        .bk-dialog-footer {
            button {
                margin-top: 0;
                width: 70px;
                min-width: 70px;
                height: 32px;
                line-height: 30px;
            }
        }
        button.disabled {
            background-color: #fafafa;
            border-color: #e6e6e6;
            color: #ccc;
            cursor: not-allowed;
            &:hover {
                background-color: #fafafa;
                border-color: #e6e6e6;
            }
        }
    }
</style>

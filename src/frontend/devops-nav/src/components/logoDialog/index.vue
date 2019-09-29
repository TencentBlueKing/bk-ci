<template>
    <bk-dialog
        v-model="showDialog"
        class="devops-project-logo-dialog"
        :width="width"
        :close-icon="false"
        title="修改LOGO"
        header-position="left"
    >
        <main class="project-logo-content">
            <div class="upload-content">
                <div class="upload-box">
                    <img
                        v-if="selectedUrl"
                        :src="selectedUrl"
                    >
                </div>
                <div class="upload-btn">
                    <input
                        id="inputfile"
                        type="file"
                        name="file"
                        class="inputfile"
                        accept="image/png, image/jpeg"
                        @change="fileChange"
                    >
                    <label for="file"><i class="bk-icon icon-bk" />选择LOGO</label>
                    <p class="logo-desc">
                        只允许上传png、jpg
                    </p>
                    <p class="logo-desc">
                        大小不超过2M
                    </p>
                </div>
            </div>
        </main>
        <template slot="footer">
            <div class="bk-dialog-outer">
                <bk-button
                    theme="primary"
                    :disabled="isUploading"
                    :loading="isUploading"
                    @click="toConfirmLogo"
                >
                    确定
                </bk-button>
                <bk-button
                    theme="default"
                    :disabled="isUploading"
                    @click="toCloseDialog"
                >
                    取消
                </bk-button>
            </div>
        </template>
    </bk-dialog>
</template>

<script lang='ts'>
    import Vue from 'vue'
    import { Component, Prop } from 'vue-property-decorator'

    @Component
    export default class projectLogoDialog extends Vue {
        @Prop({ default: false })
        showDialog: boolean

        @Prop({ default: false })
        isUploading: boolean

        @Prop({ default: '' })
        selectedUrl: string

        @Prop()
        toConfirmLogo

        @Prop()
        toCloseDialog

        @Prop()
        fileChange

        width: number = 640
        curImage: string = ''

        get logoDialogConf (): object {
          return {
            hasHeader: false,
            quickClose: false
          }
        }
    }
</script>

<style lang="scss" scoped>
    @import '../../assets/scss/conf.scss';

    .devops-project-logo-dialog {
        .project-logo-content {
            .info-title {
                padding-top: 6px;
                padding-left: 10px;
                font-size: 22px;
                color: #333C48;
            }
        }
        .upload-content {
            display: flex;
            padding: 27px 10px;
        }
        .upload-box {
            margin-right: 16px;
            width: 128px;
            height: 128px;
            border: 1px solid $fontLigtherColor;
            background: $borderColor;
            &:before {
                content: '';
                position: absolute;
                width: 124px;
                height: 124px;
                background: #fff;
                border-radius: 50%;
                border: 1px dashed $fontLigtherColor;
            }
            img {
                position: relative;
                width: 126px;
                height: 126px;
                border-radius: 50%;
                z-index: 99;
            }
        }
        .upload-btn {
            color: $fontWeightColor;
            .logo-desc {
                line-height: 24px;
                font-size: 12px;
                color: $fontLigtherColor;
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

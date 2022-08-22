<template>
    <section>
        <upload ref="upload"
            action="/support/api/user/file/upload"
            list-type="picture-card"
            :accept="accept"
            :on-success="handleUpload"
            :before-upload="checkUpload"
            :file-list="fileList"
            :limit="limit"
        >
            <p slot="tip" class="upload-tip">
                {{tip}}
            </p>
            <p class="upload-title">
                <i class="bk-icon icon-plus"></i>
                <span>{{$t('store.点击上传')}}</span>
            </p>
            <div slot="file" slot-scope="{ file }" class="upload-list">
                <span v-if="type === 'PICTURE'"
                    class="media-item media-image"
                    @click="imgSrc = file.mediaUrl"
                    :style="`background-image: url(${file.mediaUrl})`"
                ></span>
                <video v-else
                    preload="auto"
                    webkit-playsinline="true"
                    playsinline="true"
                    x-webkit-airplay="allow"
                    x5-video-player-type="h5"
                    x5-video-player-fullscreen="true"
                    x5-video-orientation="portraint"
                    style="object-fit: fill; width: 100%; height: 100%"
                    :src="file.mediaUrl">
                </video>
                <bk-progress class="media-progress" v-if="file.status === 'uploading'" :show-text="false" :theme="file.theme || 'success'" :percent="+(file.percentage || 100) / 100"></bk-progress>
                <span class="media-tool-abort media-tool" v-if="file.status === 'uploading'">
                    <span
                        v-if="!disabled"
                        @click="abortFile(file)"
                    >
                        <i class="bk-icon icon-delete"></i>
                    </span>
                </span>
                <span class="media-tool" v-else>
                    <span
                        v-if="!disabled"
                        @click="deleteFile(file)"
                    >
                        <i class="bk-icon icon-delete"></i>
                    </span>
                </span>
            </div>
        </upload>

        <zoomImage :img-src.sync="imgSrc"></zoomImage>
    </section>
</template>

<script>
    import upload from './elUpload'
    import zoomImage from '../common/zoomImage'

    export default {
        components: {
            upload,
            zoomImage
        },

        props: {
            fileList: Array,
            type: String,
            disabled: Boolean,
            limit: {
                type: Number,
                default: 0
            },
            size: Number,
            tip: String
        },

        data () {
            return {
                imgSrc: ''
            }
        },

        computed: {
            accept () {
                let res = 'image/png,image/jpeg,image/jpg,image/gif,image/svg+xml'
                if (this.type === 'VIDEO') res = 'video/webm,video/ogg,video/mp4'
                return res
            }
        },

        methods: {
            abortFile (file) {
                this.$refs.upload.abort(file)
            },

            handleUpload (res, file) {
                if (res.status === 0) {
                    const curFileList = [...this.fileList]
                    curFileList.push({ mediaUrl: res.data, mediaType: this.type })
                    this.$emit('update:fileList', curFileList)
                } else {
                    file.theme = 'danger'
                    this.$bkMessage({ message: res.message, theme: 'error' })
                }
            },

            deleteFile (file) {
                const index = this.fileList.findIndex(x => x === file)
                const curFileList = [...this.fileList]
                curFileList.splice(index, 1)
                this.$emit('update:fileList', curFileList)
            },

            checkUpload (file) {
                console.log(file.type)
                if (+file.size / 1048576 > +this.size) {
                    this.$bkMessage({ message: this.$t('store.uploadSize', [this.size]), theme: 'error' })
                    return false
                }

                if (this.limit && this.fileList.length >= this.limit) {
                    this.$bkMessage({ message: this.$t('store.uploadLimit', [this.limit]), theme: 'error' })
                    return false
                }

                const fileType = file.type
                if (!this.accept.includes(fileType)) {
                    this.$bkMessage({ message: this.$t('store.uploadType', [this.accept]), theme: 'error' })
                    return false
                }

                return true
            },

            getPlayOption (mediaUrl) {
                return {
                    autoplay: false, // 如果为true,浏览器准备好时开始回放。
                    muted: false, // 默认情况下将会消除任何音频。
                    loop: false, // 是否视频一结束就重新开始。
                    preload: 'auto', // 建议浏览器在<video>加载元素后是否应该开始下载视频数据。auto浏览器选择最佳行为,立即开始加载视频（如果浏览器支持）
                    aspectRatio: '1:1', // 将播放器置于流畅模式，并在计算播放器的动态大小时使用该值。值应该代表一个比例 - 用冒号分隔的两个数字（例如"16:9"或"4:3"）
                    fluid: false, // 当true时，Video.js player将拥有流体大小。换句话说，它将按比例缩放以适应其容器。
                    sources: [{
                        src: mediaUrl // url地址
                    }],
                    controlBar: {
                        timeDivider: true, // 当前时间和持续时间的分隔符
                        durationDisplay: true, // 显示持续时间
                        remainingTimeDisplay: false, // 是否显示剩余时间功能
                        fullscreenToggle: true // 是否显示全屏按钮
                    }
                }
            }
        }
    }
</script>

<style lang="scss" scoped>
    .media-item {
        height: 146px;
        width: 146px;
    }
    .media-image {
        cursor: pointer;
        display: inline-block;
        background-color: #F6F7FA;
        background-position: center;
        background-repeat: no-repeat;
        background-size: contain;
    }
    .upload-title {
        display: flex;
        align-items: center;
        justify-content: center;
        flex-direction: column;
        height: 100%;
        span {
            margin-top: 6px;
            line-height: 12px;
        }
        &:hover .bk-icon.icon-plus{
            color: #409eff;
        }
    }
    .upload-tip {
        color: #b0b0b0;
    }
    .upload-list {
        position: relative;
        height: 146px;
        width: 146px;
        img {
            cursor: pointer;
        }
        .media-progress {
            position: absolute;
            bottom: 0;
            left: 0;
            right: 0;
        }
        .media-tool {
            position: absolute;
            z-index: 1;
            right: 2px;
            top: 2px;
            height: 24px;
            width: 24px;
            display: none;
            justify-content: center;
            align-items: center;
            background: rgba(0, 0, 0, 0.7);
            border-radius: 100%;
            span {
                margin: 0 5px;
                font-size: 18px;
                color: #fff;
                cursor: pointer;
            }
        }
        .media-tool-abort {
            top: 0;
            bottom: 0;
            left: 0;
            right: 0;
        }
    }
    .upload-list:hover {
        .media-tool {
            display: flex;
        }
    }
</style>

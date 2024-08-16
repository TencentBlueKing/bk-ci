<template>
    <section class="media-List" ref="mediaList">
        <ul class="detail-swiper" :style="{ width: maxTransferWidth + 'px', left: `${swiperTransfer}px` }">
            <li v-for="media in list" :key="media.id" class="media-item">
                <span v-if="media.mediaType === 'PICTURE'"
                    class="media-image"
                    :style="`background-image: url(${media.mediaUrl})`"
                    @click="imgSrc = media.mediaUrl"
                >
                </span>
                <video v-else
                    controls="true"
                    preload="auto"
                    webkit-playsinline="true"
                    playsinline="true"
                    x-webkit-airplay="allow"
                    x5-video-player-type="h5"
                    x5-video-player-fullscreen="true"
                    x5-video-orientation="portraint"
                    style="object-fit: fill; width: 100%; height: 100%"
                    :src="media.mediaUrl">
                </video>
            </li>
        </ul>
        <span :class="[{ disabled: swiperTransfer >= 0 }, 'nav-left', 'swiper-nav']"
            @click="() => {
                if (swiperTransfer < 0) changeIndex(1)
            }"
        >
            <i class="nav-icon"></i>
        </span>
        <span :class="[{ disabled: swiperTransfer <= -maxTransferWidth + containWidth }, 'nav-right', 'swiper-nav']"
            @click="() => {
                if (swiperTransfer > -maxTransferWidth + containWidth) changeIndex(-1)
            }"
        >
            <i class="nav-icon"></i>
        </span>

        <zoomImage :img-src.sync="imgSrc"></zoomImage>
    </section>
</template>

<script>
    import zoomImage from '../zoomImage'
    export default {
        components: {
            zoomImage
        },

        props: {
            list: {
                type: Array
            }
        },

        data () {
            return {
                swiperTransfer: 0,
                imgSrc: '',
                containWidth: 0
            }
        },

        computed: {
            maxTransferWidth () {
                const mediaList = this.list || []
                return mediaList.length * 395 + (mediaList.length - 1) * 37
            }
        },

        mounted () {
            const mediaList = this.$refs.mediaList
            this.containWidth = mediaList.offsetWidth
        },

        methods: {
            getPlayOption (media) {
                return {
                    playbackRates: [0.5, 1.0, 1.5, 2.0], // 可选的播放速度
                    autoplay: false, // 如果为true,浏览器准备好时开始回放。
                    muted: false, // 默认情况下将会消除任何音频。
                    loop: false, // 是否视频一结束就重新开始。
                    preload: 'auto', // 建议浏览器在<video>加载元素后是否应该开始下载视频数据。auto浏览器选择最佳行为,立即开始加载视频（如果浏览器支持）
                    aspectRatio: '16:9', // 将播放器置于流畅模式，并在计算播放器的动态大小时使用该值。值应该代表一个比例 - 用冒号分隔的两个数字（例如"16:9"或"4:3"）
                    fluid: false, // 当true时，Video.js player将拥有流体大小。换句话说，它将按比例缩放以适应其容器。
                    sources: [{
                        type: 'video/mp4', // 类型
                        src: media.mediaUrl // url地址
                    }],
                    controlBar: {
                        timeDivider: true, // 当前时间和持续时间的分隔符
                        durationDisplay: true, // 显示持续时间
                        remainingTimeDisplay: false, // 是否显示剩余时间功能
                        fullscreenToggle: true // 是否显示全屏按钮
                    }
                }
            },

            changeIndex (num) {
                let newTransferDis = this.swiperTransfer + num * 432
                const maxMove = this.maxTransferWidth - this.containWidth
                if (newTransferDis >= 0) newTransferDis = 0
                if (newTransferDis <= -maxMove) newTransferDis = -maxMove
                this.swiperTransfer = newTransferDis
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '@/assets/scss/conf.scss';

    .media-List {
        width: 100%;
        height: 222px;
        position: relative;
        overflow: hidden;
    }

    .detail-swiper {
        position: absolute;
        transition: 0.525s cubic-bezier(0.42, 0, 0.58, 1);
        &::after {
            content: '';
            display: table;
            clear: both;
        }
        .media-item {
            float: left;
            height: 222px;
            width: 395px;
            margin-right: 37px;
            cursor: pointer;
            .media-image {
                display: inline-block;
                background-color: #F6F7FA;
                background-position: center;
                background-repeat: no-repeat;
                background-size: contain;
                height: 222px;
                width: 395px;
            }
            &:last-child {
                margin-right: 0;
            }
        }
    }

    .swiper-nav {
        cursor: pointer;
        position: absolute;
        width: 15px;
        height: 20px;
        background: #000;
        opacity: .7;
        top: 101px;
        &.disabled {
            cursor: not-allowed;
            opacity: .2;
        }
        .nav-icon {
            position: absolute;
            top: 7px;
            left: 5px;
            width: 6px;
            height: 6px;
            border-left: 1px solid $white;
            border-bottom: 1px solid $white;
        }
        &.nav-left {
            left: 0;
            .nav-icon {
                transform: rotate(45deg);
            }
        }
        &.nav-right {
            right: 0;
            .nav-icon {
                transform: rotate(225deg);
            }
        }
        &:not(.disabled):hover {
            opacity: .9;
        }
        &:not(.disabled):active {
            opacity: .8;
        }
    }
</style>

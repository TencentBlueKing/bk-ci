<template>
    <section class="detail-title">
        <img class="detail-pic atom-logo" :src="detail.logoUrl || defaultUrl">
        <hgroup class="detail-info-group">
            <h3 class="title-with-img">
                {{detail.name}}
                <h5 :title="isPublicTitle" @click="goToCode" :class="{ 'not-public': !isPublic }">
                    <icon class="detail-img" name="gray-git-code" size="14" />
                    <span class="approve-msg">{{ isPublic ? $t('store.源码') : $t('store.未开源') }}</span>
                </h5>
            </h3>
            <h5 class="detail-info">
                <span> {{ $t('store.发布者：') }} </span><span>{{detail.publisher || '-'}}</span>
            </h5>
            <h5 class="detail-info">
                <span> {{ $t('store.版本：') }} </span><span>{{detail.version || '-'}}</span>
            </h5>
            <h5 class="detail-info detail-score" :title="$t('store.rateTips', [(detail.score || 0), (detail.totalNum || 0)])">
                <span> {{ $t('store.评分：') }} </span>
                <p class="score-group">
                    <comment-rate :rate="5" :width="14" :height="14" :style="{ width: starWidth }" class="score-real"></comment-rate>
                    <comment-rate :rate="0" :width="14" :height="14"></comment-rate>
                </p>
                <span class="rate-num">{{detail.totalNum || 0}}</span>
            </h5>
            <h5 class="detail-info">
                <span> {{ $t('store.适用IDE：') }} </span><span>{{detail.categoryList|templateCategory}}</span>
            </h5>
            <h5 class="detail-info">
                <span> {{ $t('store.分类：') }} </span><span>{{detail.classifyName || '-'}}</span>
            </h5>
            <h5 class="detail-info">
                <span> {{ $t('store.热度：') }} </span><span>{{detail.downloads || 0}}</span>
            </h5>
            <h5 class="detail-info detail-label">
                <span> {{ $t('store.功能标签：') }} </span>
                <span v-for="(label, index) in detail.labelList" :key="index" class="info-label">{{label.labelName}}</span>
                <span v-if="!detail.labelList || detail.labelList.length <= 0 ">-</span>
            </h5>
            <h5 class="detail-info detail-maxwidth" :title="detail.summary">
                <span> {{ $t('store.简介：') }} </span><span>{{detail.summary || '-'}}</span>
            </h5>
        </hgroup>
        <button :class="[{ 'opicity-hidden': (detail.categoryList || []).every(x => x.categoryCode !== 'VsCode') }, 'detail-install']" @click="installPlugin"> {{ $t('store.安装') }} </button>
        <bk-dialog v-model="showInstallTip"
            theme="primary"
            :close-icon="false"
            header-position="center"
            :ok-text="$t('store.已安装 T-extensions，继续')"
            :title="$t('store.安装提示')"
            width="700"
            class="install-tip"
            :draggable="false"
            @confirm="confirmInstall"
        >
            <h3 class="mb10"> {{ $t('store.VSCode 插件安装指引：') }} </h3>
            1. {{ $t('store.首先安装') }} <span class="text-tip" v-bk-tooltips="{ placements: ['top'], content: $t('store.T-extensions 管理公司内部的所有VSCode插件') }">T-extensions</span> {{ $t('store.到 VSCode。若已安装，则跳过此步') }} <br>
            <span class="ml10">1）<a class="down-link" href="http://bk.artifactory.oa.com/generic-public/ide-plugin/t-extension/0.0.4/t-extension-0.0.4.vsix"> {{ $t('store.点此下载') }} </a>  {{ $t('store.T-extensions 插件安装包') }} <br></span>
            <span class="ml10">2）{{ $t('store.在 VSCode 扩展 =》更多功能 =》从VSIX安装，安装上一步下载的VSIX包，入口如下图所示：') }}</span>
            <img :src="VSCODE_GUIDE_IMAGE_URL">
            <span class="mt10 inb">2. {{ $t('store.在 T-extensions 中安装目标插件，或在蓝盾研发商店中点击目标插件详情页面的安装按钮') }}</span>
        </bk-dialog>
    </section>
</template>

<script>
    import commentRate from '../comment-rate'
    import { DEFAULT_LOGO_URL, VSCODE_GUIDE_IMAGE_URL } from '@/utils/'

    export default {
        components: {
            commentRate
        },

        filters: {
            templateCategory (list = []) {
                const nameList = list.map(item => item.categoryName) || []
                const res = nameList.join('，') || '-'
                return res
            }
        },

        props: {
            detail: Object
        },

        data () {
            return {
                defaultUrl: DEFAULT_LOGO_URL,
                VSCODE_GUIDE_IMAGE_URL,
                user: JSON.parse(localStorage.getItem('_cache_userInfo')).username,
                isLoading: false,
                showInstallTip: false
            }
        },

        computed: {
            starWidth () {
                const integer = Math.floor(this.detail.score)
                const fixWidth = 17 * integer
                const rateWidth = 14 * (this.detail.score - integer)
                return `${fixWidth + rateWidth}px`
            },

            isPublic () {
                return this.detail.visibilityLevel === 'LOGIN_PUBLIC'
            },

            isPublicTitle () {
                if (this.isPublic) return this.$t('store.点击查看源码')
                else return this.$t('store.未开源')
            }
        },

        methods: {
            goToCode () {
                if (this.isPublic) window.open(this.detail.codeSrc, '_blank')
            },

            installPlugin () {
                this.showInstallTip = true
            },

            confirmInstall () {
                window.open(`vscode://bkdevops.t-extension/selected?id=${this.detail.atomCode}`, '_self')
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '@/assets/scss/conf.scss';

    .install-tip {
        .inb {
            display: inline-block;
        }
        .mt10 {
            margin-top: 10px;
        }
        .mb10 {
            margin-bottom: 10px;
        }
        .down-link {
            cursor: pointer;
            color: $primaryColor;
        }
        ::v-deep .bk-dialog {
            top: 100px;
        }
        img {
            width: 400px;
            display: block;
            margin: 15px auto 0;
        }
        .plugin-tip {
            vertical-align: middle;
            margin: 0 5px;
        }
        .text-tip {
            border-bottom: 1px dashed $fontGray;
        }
    }

    .detail-title {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin: 26px auto 0;
        width: 95vw;
        background: $white;
        box-shadow: 1px 2px 3px 0px rgba(0,0,0,0.05);
        padding: 32px;
        .detail-pic {
            width: 130px;
        }
        .atom-icon {
            height: 160px;
            width: 160px;
        }
        button {
            border-radius: 4px;
            width: 120px;
            height: 40px;
        }
        .detail-install {
            background: $primaryColor;
            border: none;
            font-size: 14px;
            color: $white;
            line-height: 40px;
            text-align: center;
            &.opicity-hidden {
                opacity: 0;
                user-select: none;
            }
            &:active {
                transform: scale(.97)
            }
        }
        .bk-tooltip button {
            width: 89px;
        }
    }

    .detail-info-group {
        flex: 1;
        margin: 0 32px;
        max-width: calc(100% - 314px);
        h3 {
            font-size: 22px;
            line-height: 29px;
            color: $fontBlack;
        }
        .detail-score {
            display: flex;
            align-items: center;
            .score-group {
                position: relative;
                margin-top: -2px;
                .score-real {
                    position: absolute;
                    overflow: hidden;
                    left: 0;
                    top: 0;
                    height: 14px;
                    display: flex;
                    .yellow {
                        min-width: 14px;
                    }
                }
            }
            .rate-num {
                margin-left: 6px;
                color: $fontWeightColor;
            }
        }
        .detail-info {
            float: left;
            display: flex;
            padding-top: 7px;
            width: 33.33%;
            font-size: 14px;
            font-weight: normal;
            line-height: 20px;
            color: $fontBlack;
            span:nth-child(1) {
                color: $fontWeightColor;
                display: inline-block;
                width: 90px;
                padding-right: 10px;
                text-align: right;
            }
            span:nth-child(2) {
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
                display: inline-block;
                width: calc(100% - 90px);
            }
        }
        .title-with-img {
            display: flex;
            align-items: center;
            >h5 {
                margin-left: 12px;
                line-height: 14px;
                padding: 2px 5px;
                cursor: pointer;
                background: rgba(21, 146, 255, 0.08);
                color: #1592ff;
                .detail-img {
                    fill: #1592ff;
                }
                span {
                    font-weight: normal;
                    font-size: 12px;
                    line-height: 14px;
                }
            }
            >span {
                font-size: 20px;
                color: $fontLightGray;
                line-height: 20px;
                font-weight: normal;
            }
            .detail-img {
                vertical-align: middle;
            }
            h5.not-public {
                cursor: auto;
                background: none;
                color: #9e9e9e;
                .detail-img {
                    fill: #9e9e9e;
                }
            }
        }
        .detail-info.detail-label {
            width: 100%;
            padding-left: 90px;
            display: inline-block;
            position: relative;
            span {
                overflow: inherit;
                margin-bottom: 7px;
            }
            span:first-child {
                position: absolute;
                left: 0;
            }
            span.info-label {
                display: inline-block;
                width: auto;
                height: 19px;
                padding: 0 7px;
                border: 1px solid $laberColor;
                border-radius: 20px;
                margin-right: 8px;
                line-height: 17px;
                text-align: center;
                font-size: 12px;
                color: $laberColor;
                background-color: $laberBackColor;
            }
        }
        .detail-maxwidth {
            max-width: 100%;
            width: auto;
            padding-top: 0;
        }
    }
</style>

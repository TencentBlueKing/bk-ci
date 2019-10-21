<template>
    <section class="detail-title">
        <img class="detail-pic atom-logo" :src="detail.logoUrl || defaultUrl">
        <hgroup class="detail-info-group">
            <h3 class="title-with-img">
                {{detail.name}}
                <h5 :title="isPublicTitle" @click="goToCode" :class="{ 'not-public': !isPublic }">
                    <icon v-if="isPublic" class="detail-img" name="color-git-code" size="16" />
                    <icon v-else class="detail-img" name="gray-git-code" size="16" style="fill:#9E9E9E" />
                    <span>工蜂</span>
                </h5>
            </h3>
            <h5 class="detail-info">
                <span>发布者：</span><span>{{detail.publisher || '-'}}</span>
            </h5>
            <h5 class="detail-info">
                <span>版本：</span><span>{{detail.version || '-'}}</span>
            </h5>
            <h5 class="detail-info detail-score" :title="`平均评分为${detail.score || 0}星（总分为5星），${detail.totalNum || 0}位用户评价了此项内容`">
                <span>评分：</span>
                <p class="score-group">
                    <comment-rate :rate="5" :width="14" :height="14" :style="{ width: starWidth }" class="score-real"></comment-rate>
                    <comment-rate :rate="0" :width="14" :height="14"></comment-rate>
                </p>
                <span class="rate-num">{{detail.totalNum || 0}}</span>
            </h5>
            <h5 class="detail-info">
                <span>适用IDE：</span><span>{{detail.categoryList|templateCategory}}</span>
            </h5>
            <h5 class="detail-info">
                <span>分类：</span><span>{{detail.classifyName || '-'}}</span>
            </h5>
            <h5 class="detail-info">
                <span>热度：</span><span>{{detail.downloads || 0}}</span>
            </h5>
            <h5 class="detail-info detail-label">
                <span>功能标签：</span>
                <span v-for="(label, index) in detail.labelList" :key="index" class="info-label">{{label.labelName}}</span>
                <span v-if="!detail.labelList || detail.labelList.length <= 0 ">-</span>
            </h5>
            <h5 class="detail-info detail-maxwidth" :title="detail.summary">
                <span>简介：</span><span>{{detail.summary || '-'}}</span>
            </h5>
        </hgroup>
        <button :class="[{ 'opicity-hidden': detail.categoryList.every(x => x.categoryCode !== 'VsCode') }, 'detail-install']" @click="installPlugin">安装</button>
        <bk-dialog v-model="showInstallTip"
            theme="primary"
            :close-icon="false"
            header-position="center"
            ok-text="已安装 T-extensions，继续"
            title="安装提示"
            width="700"
            class="install-tip"
            @confirm="confirmInstall"
        >
            <h3 class="mb10">VSCode 插件安装指引：</h3>
            Step1：首先安装 T-extensions 插件<bk-popover content="T-extensions 管理公司内部的所有VSCode插件" placement="top"><i class="plugin-tip bk-icon icon-info-circle"></i></bk-popover>到VSCode<br>
            Step2：在 T-extensions 中安装目标插件，或在蓝盾研发商店中点击目标插件详情页面的安装按钮

            <h3 class="mb10 mt10">如何安装T-extensions？</h3>
            Step1：<a class="down-link" href="http://bk.artifactory.oa.com/generic-public/ide-plugin/t-extension/0.0.3/t-extension-0.0.3.vsix">点此下载</a> T-extensions 插件安装包<br>
            Step2：在 VSCode 扩展 =》更多功能 =》从VSIX安装，安装上一步下载的VSIX包，入口如下图所示：
            <img src="http://radosgw.open.oa.com/paas_backend/ieod/prod/file/png/random_15705911044808382165408406563094.png?v=1570591104">
        </bk-dialog>
    </section>
</template>

<script>
    import commentRate from '../comment-rate'

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
                defaultUrl: 'http://radosgw.open.oa.com/paas_backend/ieod/dev/file/png/random_15647373141529070794466428255950.png?v=1564737314',
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
                if (this.isPublic) return '查看源码'
                else return '未开源'
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

<style lang="scss" scope>
    @import '@/assets/scss/conf.scss';

    .install-tip {
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
        /deep/ .bk-dialog {
            top: 50%;
            .bk-dialog-content {
                transform: translateY(-50%);
            }
        }
        img {
            width: 500px;
            display: block;
            margin: 15px auto 0;
        }
        .plugin-tip {
            vertical-align: middle;
            margin: 0 5px;
        }
    }

    .detail-title {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin: 47px auto 30px;
        width: 1200px;
        .detail-pic {
            width: 130px;
        }
        .atom-icon {
            height: 160px;
            width: 160px;
        }
        .detail-install {
            width: 89px;
            height: 36px;
            background: $primaryColor;
            border-radius: 2px;
            border: none;
            font-size: 14px;
            color: $white;
            line-height: 36px;
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
        width: 829px;
        margin: 0 76px;
        
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
                margin-top: 2px;
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
            line-height: 19px;
            color: $fontBlack;
            span:nth-child(1) {
                color: $fontWeightColor;
                display: inline-block;
                width: 70px;
                text-align: right;
            }
            span:nth-child(2) {
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
                display: inline-block;
                width: calc(100% - 70px);
            }
        }
        .title-with-img {
            display: flex;
            align-items: center;
            h5 {
                cursor: pointer;
            }
            span {
                margin-left: -2px;
                font-size: 14px;
                color: $fontLightGray;
                line-height: 19px;
                font-weight: normal;
            }
            .detail-img {
                margin-left: 12px;
                vertical-align: middle;
            }
            .not-public {
                cursor: auto;
            }
        }
        .detail-info.detail-label {
            width: 829px;
            padding-left: 70px;
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

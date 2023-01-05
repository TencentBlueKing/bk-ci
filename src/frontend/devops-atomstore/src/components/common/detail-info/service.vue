<template>
    <section class="detail-title">
        <img class="detail-pic atom-logo" :src="detail.logoUrl">
        <hgroup class="detail-info-group">
            <h3 class="title-with-img">
                <span :class="[{ 'not-recommend': detail.recommendFlag === false }, 'title-with-img']" :title="detail.recommendFlag === false ? $t('store.该微扩展不推荐使用') : ''">
                    {{detail.name}}
                </span>
                <h5 :title="isPublicTitle" @click="goToCode" :class="{ 'not-public': !isPublic }" v-if="!isEnterprise">
                    <icon class="detail-img" name="gray-git-code" size="14" />
                    <span class="approve-msg">{{ isPublic ? $t('store.源码') : $t('store.未开源') }}</span>
                </h5>
            </h3>
            <h5 class="install-info">
                <span>{{detail.publisher || '-'}}</span><span class="install-title"> {{ $t('store.发布') }} </span>
                <span>{{detail.downloads || 0}}</span><span class="install-title"> {{ $t('store.次安装') }} </span>
                <h6 class="detail-score" :title="$t('store.rateTips', [(detail.score || 0), (detail.totalNum || 0)])">
                    <span>{{detail.totalNum || 0}}</span>
                    <p class="score-group">
                        <comment-rate :rate="5" :width="14" :height="14" :style="{ width: starWidth }" class="score-real"></comment-rate>
                        <comment-rate :rate="0" :width="14" :height="14"></comment-rate>
                    </p>
                </h6>
                <span>V{{detail.version || 'init'}}</span><span class="install-title"> {{ $t('store.版本') }} </span>
            </h5>
            <h5 class="detail-info detail-label">
                <span> {{ $t('store.功能标签：') }} </span>
                <span v-for="(label, index) in detail.labelList" :key="index" class="info-label">{{label.labelName}}</span>
                <span v-if="!detail.labelList || detail.labelList.length <= 0 ">-</span>
            </h5>
            <h5 class="detail-info">
                <span> {{ $t('store.扩展点') }}： </span>
                <span class="info-over-hidden" :title="detail.itemName">{{detail.itemName || '-'}}</span>
            </h5>
            <h5 class="detail-info" :title="detail.summary">
                <span> {{ $t('store.简介：') }} </span>
                <span class="info-over-hidden" :title="detail.summary">{{detail.summary || '-'}}</span>
            </h5>
        </hgroup>
        <bk-popover placement="top" v-if="buttonInfo.disable">
            <button class="bk-button bk-primary" type="button" disabled> {{ $t('store.安装') }} </button>
            <template slot="content">
                <p>{{buttonInfo.des}}</p>
            </template>
        </bk-popover>
        <button class="detail-install" @click="goToInstall" v-else> {{ $t('store.安装') }} </button>
    </section>
</template>

<script>
    import commentRate from '../comment-rate'

    export default {
        components: {
            commentRate
        },

        props: {
            detail: Object
        },

        data () {
            return {
                user: JSON.parse(localStorage.getItem('_cache_userInfo')).username,
                isLoading: false
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
            },

            isEnterprise () {
                return VERSION_TYPE === 'ee'
            },

            buttonInfo () {
                const info = {}
                info.disable = this.detail.publicFlag || !this.detail.flag
                if (this.detail.publicFlag) info.des = `${this.$t('store.通用微扩展，所有项目默认可用，无需安装')}`
                if (!this.detail.flag) info.des = `${this.$t('store.你没有该微扩展的安装权限，请联系微扩展发布者')}`
                return info
            }
        },

        methods: {
            goToInstall () {
                this.$router.push({
                    name: 'install',
                    query: {
                        code: this.detail.serviceCode,
                        type: 'service',
                        from: 'details'
                    }
                })
            },

            goToCode () {
                if (this.isPublic) window.open(this.detail.codeSrc, '_blank')
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '@/assets/scss/conf.scss';

    .detail-title {
        display: flex;
        align-items: flex-start;
        margin: 26px auto 0;
        width: 95vw;
        background: $white;
        box-shadow: 1px 2px 3px 0px rgba(0,0,0,0.05);
        padding: 32px;
        .detail-pic {
            width: 100px;
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
    }
    .detail-info-group {
        flex: 1;
        margin: 0 32px;
        max-width: calc(100% - 284px);
        h3 {
            font-size: 20px;
            line-height: 23px;
            color: $fontDarkBlack;
        }
        .detail-score {
            display: flex;
            align-items: center;
            font-size: 14px;
            line-height: 17px;
            font-weight: normal;
            margin-right: 26px;
            .score-group {
                position: relative;
                margin-left: 8px;
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
                margin-top: 2px;
                margin-left: 6px;
                color: $fontWeightColor;
            }
        }
        .install-info {
            margin-top: 18px;
            display: flex;
            align-items: center;
            color: $fontDarkBlack;
            font-size: 14px;
            font-weight: normal;
            line-height: 17px;
            .install-title {
                color: #999999;
                display: inline-block;
                margin: 0 26px 0 8px;
            }
        }
        .detail-info {
            display: flex;
            margin-top: 18px;
            font-size: 14px;
            font-weight: normal;
            line-height: 17px;
            padding-left: 70px;
            position: relative;
            span:first-child {
                position: absolute;
                left: 0;
            }
            span:nth-child(2) {
                color: $fontDarkBlack;
            }
            .info-over-hidden {
                display: inline-block;
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
            }
        }
        .not-recommend {
            text-decoration: line-through;
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
            padding-left: 70px;
            display: inline-block;
            position: relative;
            span:first-child {
                position: absolute;
                left: 0;
            }
            span.info-label {
                display: inline-block;
                width: auto;
                height: 18px;
                padding: 0 12px;
                border: 1px solid $lightBorder;
                border-radius: 20px;
                margin-right: 8px;
                line-height: 16px;
                text-align: center;
                font-size: 12px;
                color: $fontDarkBlack;
                background-color: $white;
            }
        }
    }
</style>

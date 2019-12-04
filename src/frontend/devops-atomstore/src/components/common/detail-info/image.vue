<template>
    <section class="detail-title">
        <img class="detail-pic atom-logo" :src="detail.logoUrl">
        <hgroup class="detail-info-group">
            <h3 class="title-with-img">
                {{detail.name}}
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
                <span> {{ $t('store.镜像源：') }} </span><span>{{detail.imageSourceType | imageTypeFilter}}</span>
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

        filters: {
            imageTypeFilter (val) {
                const local = window.devops || {}
                let res = ''
                switch (val) {
                    case 'THIRD':
                        res = local.$t('store.第三方源')
                        break
                    case 'BKDEVOPS':
                        res = local.$t('store.蓝盾源')
                        break
                }
                return res
            }
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

            buttonInfo () {
                const info = {}
                info.disable = this.detail.publicFlag || !this.detail.flag
                if (this.detail.publicFlag) info.des = `${this.$t('store.通用镜像，所有项目默认可用，无需安装')}`
                if (!this.detail.flag) info.des = `${this.$t('store.你没有该镜像的安装权限，请联系镜像发布者')}`
                return info
            }
        },

        methods: {
            goToInstall () {
                this.$router.push({
                    name: 'install',
                    query: {
                        code: this.detail.imageCode,
                        type: 'image',
                        from: 'details'
                    }
                })
            }
        }
    }
</script>

<style lang="scss" scope>
    @import '@/assets/scss/conf.scss';

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

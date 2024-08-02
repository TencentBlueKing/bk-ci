<template>
    <section class="atom-card">
        <honer-img class="card-pic atom-logo" :detail="atom" />
        <p v-bk-overflow-tips :class="[{ 'not-recommend': atom.recommendFlag === false }, 'card-name', 'text-overflow']">{{ atom.name }}</p>
        <h5 class="card-detail">
            <honer-tag :detail="atom" :max-num="1"></honer-tag>
            <img
                v-for="indexInfo in atom.indexInfos"
                v-bk-tooltips="{
                    allowHTML: true,
                    content: indexInfo.hover
                }"
                :key="indexInfo.indexCode"
                :src="indexInfo.iconUrl"
                :style="{
                    color: indexInfo.iconColor,
                    height: '16px',
                    width: '16px',
                    marginRight: '8px',
                    cursor: 'pointer'
                }"
            >
        </h5>
        <p v-if="hasSummary" class="card-summary">{{atom.summary || $t('store.暂无描述')}}</p>
        <section class="card-rate">
            <section class="rate-left">
                <p class="score-group">
                    <comment-rate
                        :max-stars="1"
                        :rate="1"
                        :width="14"
                        :height="14"
                        :style="{ width: starWidth }"
                        class="score-real"
                    />
                    <comment-rate
                        :max-stars="1"
                        :rate="0"
                        :width="14"
                        :height="14"
                    />
                </p>
                <span class="ml4">{{ atom.score }}</span>
                <img v-if="atom.hotFlag" class="hot-icon" src="../../../images/hot-red.png">
                <img v-else class="hot-icon" src="../../../images/hot.png">
                <span class="ml4">{{ getShowNum(atom.recentExecuteNum) }}</span>
            </section>
            <span class="text-overflow ml5">{{ atom.publisher }}</span>
        </section>
        <i class="devops-icon icon-lock-shape" v-if="!atom.flag"></i>
    </section>
</template>

<script>
    import CommentRate from '../comment-rate'
    import HonerImg from '../../honer-img.vue'
    import HonerTag from '../../honer-tag.vue'

    export default {
        components: {
            CommentRate,
            HonerImg,
            HonerTag
        },

        props: {
            atom: Object,
            hasSummary: Boolean
        },

        computed: {
            starWidth () {
                if (this.atom.score >= 5) {
                    return '14px'
                } else if (this.atom.score <= 0) {
                    return '0px'
                } else {
                    return '7px'
                }
            }
        },

        methods: {
            getShowNum (num) {
                if (+num > 10000) {
                    return Math.floor(+num / 10000) + 'W+'
                } else {
                    return num
                }
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '@/assets/scss/conf.scss';

    .atom-card {
        display: block;
        width: 226px;
        position: relative;
        .not-recommend {
            text-decoration: line-through;
        }
        .card-pic {
            margin: 17px 73px 4px;
            &.atom-logo {
                height: 85px;
                width: 85px;
                margin: 10px 70px 5px;
            }
        }
        .card-name {
            text-align: center;
            padding: 0 12px;
            height: 21px;
            line-height: 21px;
            font-size: 17px;
            font-weight: normal;
            color: $fontBlack;
        }
        .card-detail {
            margin: 16px 13px 4px;
            height: 16px;
            line-height: 16px;
            font-size: 12px;
            font-weight: normal;
            color: $fontGray;
            display: flex;
            span:nth-child(1) {
                display: inline-block;
                width: 60%;
            }
            span:nth-child(2) {
                float: right;
            }
        }
        .card-rate {
            padding: 5px 13px 20px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            line-height: 16px;
            color: #979BA5;
            .score-group {
                position: relative;
                .score-real {
                    position: absolute;
                    overflow: hidden;
                    left: 0;
                    top: 0;
                    height: 16px;
                    display: flex;
                }
            }
            .hot-icon {
                color: $fontWeightColor;
                margin-left: 12px;
                width: 14px;
                height: 14px;
            }
            .rate-left {
                display: flex;
                align-items: center;
            }
        }
        .card-summary {
            font-weight: normal;
            height: 32px;
            margin: 0px 13px 6px;
            box-sizing: content-box;
            font-size: 12px;
            line-height: 16px;
            color: $fontGray;
            overflow : hidden;
            text-overflow: ellipsis;
            display: -webkit-box;
            -webkit-line-clamp: 2;
            -webkit-box-orient: vertical;
        }
        .icon-lock-shape {
            position: absolute;
            top: 12px;
            right: 12px;
            color: #737987;
        }
        .ml4 {
            margin-left: 4px;
        }
    }
</style>

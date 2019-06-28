<template>
    <router-link :to="{ name: 'details', params: { code: atom.code, type: $route.query.pipeType || 'atom' } }" class="card-home">
        <img class="card-pic atom-logo" :src="atom.logoUrl" v-if="atom.logoUrl">
        <icon class="card-pic" v-else :name="getAtomIcon(atom.code)" size="78" style="fill:#C3CDD7" />
        <p class="card-name text-overflow">{{ atom.name }}</p>
        <h5 class="card-detail">
            <span class="text-overflow">{{ atom.publisher }}</span>
            <span>{{ atom.downloads }} <i class="bk-icon icon-heat-2"></i></span>
        </h5>
        <p v-if="hasSummary" class="card-summary">{{atom.summary || '暂无描述'}}</p>
        <section class="card-rate">
            <p class="score-group">
                <comment-rate :rate="5" :width="15" :height="16" :style="{ width: starWidth }" class="score-real"></comment-rate>
                <comment-rate :rate="0" :width="15" :height="16"></comment-rate>
            </p>
        </section>
    </router-link>
</template>

<script>
    import commentRate from '../comment-rate'

    export default {
        components: {
            commentRate
        },

        props: {
            atom: Object,
            hasSummary: Boolean
        },

        computed: {
            starWidth () {
                const integer = Math.floor(this.atom.score)
                const fixWidth = 18 * integer
                const rateWidth = 15 * (this.atom.score - integer)
                return `${fixWidth + rateWidth}px`
            }
        },

        methods: {
            getAtomIcon (atomCode) {
                return document.getElementById(atomCode) ? atomCode : 'placeholder'
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '@/assets/scss/conf.scss';

    .card-home {
        display: block;
        width: 226px;
        background: $white;
        border-radius: 2px;
        border:1px solid $lightGray;
        cursor: pointer;
        &:hover {
            box-shadow: 0 3px 8px 0 rgba(60, 150, 255, 0.2), 0 0 0 1px rgba(60, 150, 255, 0.08);
        }
        .card-pic {
            margin: 17px 73px 4px;
            &.atom-logo {
                height: 64px;
                width: 64px;
                margin: 24px 80px 11px;
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
            margin: 10px 13px 4px;
            height: 16px;
            line-height: 16px;
            font-size: 12px;
            font-weight: normal;
            color: $fontGray;
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
            .bk-icon {
                color: $fontWeightColor;
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
    }
</style>

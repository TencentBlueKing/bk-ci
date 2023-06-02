<template>
    <section class="honer-tags">
        <span
            v-for="renderHoner in renderHoners"
            :key="renderHoner.honorId"
            class="honer-tag"
        >
            <img class="tag-image" src="../../images/honer-left.png">
            <span class="tag-txt">
                <span class="tag-txt-main text-overflow" v-bk-tooltips="getHonorTips(renderHoner)">{{ renderHoner.honorTitle }}</span>
                <img class="tag-txt-image" src="../../images/honer-center.png">
            </span>
            <img class="tag-image" src="../../images/honer-right.png">
        </span>
        <span
            v-if="honorInfos.length > +maxNum"
            v-bk-tooltips="honorTooltips"
            class="honer-num"
        >
            +{{ honorInfos.length - maxNum }}
        </span>
        <span class="honer-gap" v-if="renderHoners.length && indexInfos.length"></span>
    </section>
</template>

<script>
    export default {
        props: {
            detail: Object,
            maxNum: Number
        },

        computed: {
            honorInfos () {
                return this.detail?.honorInfos || []
            },

            indexInfos () {
                return this.detail?.indexInfos || []
            },

            renderHoners () {
                return this.honorInfos.slice(0, this.maxNum)
            },

            honorTooltips () {
                return {
                    theme: 'light',
                    allowHTML: true,
                    zIndex: 10000,
                    content: this.honorInfos.reduce((acc, cur) => {
                        acc += `<section class="honor-gaps"><span class="honor-title text-overflow" title=${cur.honorTitle}>${cur.honorTitle}</span><span class="honor-name">${cur.honorName}</span></section>`
                        return acc
                    }, '')
                }
            }
        },

        methods: {
            getHonorTips (honner) {
                return {
                    theme: 'light',
                    allowHTML: true,
                    zIndex: 10000,
                    content: `<section class="honor-gaps"><span class="honor-title text-overflow" title=${honner.honorTitle}>${honner.honorTitle}</span><span class="honor-name">${honner.honorName}</span></section>`
                }
            }
        }
    }
</script>

<style lang="scss" scoped>
    .honer-tags {
        display: flex;
        align-items: center;
        .honer-tag {
            display: flex;
            align-items: center;
            margin-right: 8px;
        }
        .tag-txt {
            line-height: 16px;
            height: 16px;
            display: inline-flex;
            position: relative;
        }
        .tag-txt-main {
            position: relative;
            z-index: 11;
            font-size: 12px;
            color: #fff;
            max-width: 70px;
        }
        .tag-txt-image {
            position: absolute;
            top: 0;
            left: 0;
            bottom: 0;
            right: 0;
            width: 100%;
            height: 16px;
            z-index: 10;
        }
        .tag-image {
            height: 19px;
        }
        .honer-num {
            color: #FF9C01;
            margin-right: 8px;
            font-size: 12px;
            font-weight: normal;
            cursor: pointer;
        }
        .honer-gap {
            display: inline-block;
            width: 1px;
            height: 16px;
            margin-right: 16px;
            background: #DCDEE5;
        }
    }
</style>
<style lang="scss">
    .honor-title {
        display: inline-block;
        padding: 1px 4px;
        background: #F0F1F5;
        margin-right: 4px;
        width: 60px;
        text-align: center;
    }
    .honor-gaps {
        display: flex;
        align-items: center;
        margin-top: 4px;
        &:last-child {
            margin-bottom: 4px;
        }
    }
</style>

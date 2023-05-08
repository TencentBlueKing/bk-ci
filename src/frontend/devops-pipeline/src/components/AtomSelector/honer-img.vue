<template>
    <section class="honer-img">
        <section class="img-wrapper">
            <img class="img-self" :src="detail.logoUrl">
            <template v-if="recentHoner">
                <img class="honer-self" src="../../images/honer.png">
                <span
                    v-bk-tooltips="honorTips"
                    :class="{
                        'text-overflow': true,
                        'honer-txt': true,
                        'is-big': isBig
                    }"
                >{{ recentHoner.honorTitle }}</span>
            </template>
        </section>
    </section>
</template>

<script>
    export default {
        props: {
            detail: Object,
            isBig: Boolean
        },

        computed: {
            recentHoner () {
                return this.detail?.honorInfos?.[0]
            },

            honorTips () {
                return {
                    theme: 'light',
                    allowHTML: true,
                    zIndex: 10000,
                    content: `<section class="honor-gaps"><span class="honor-title text-overflow" title=${this.recentHoner.honorTitle}>${this.recentHoner.honorTitle}</span><span class="honor-name">${this.recentHoner.honorName}</span></section>`
                }
            }
        }
    }
</script>

<style lang="scss">
    .honer-img {
        width: 100%;
        height: 100%;
        position: relative;
        min-width: 68px;
        .img-wrapper {
            position: absolute;
            top: 2%;
            left: 7%;
            right: 7%;
            bottom: 11%;
            background: #F5F7FA;
            border-radius: 100%;
            display: flex;
            align-items: center;
            justify-content: center;
        }
        .img-self {
            width: 69%;
            height: 69%;
        }
        .honer-self {
            position: absolute;
            width: 130%;
            height: 130%;
            left: -16%;
            bottom: -13px;
        }
        .honer-txt {
            position: absolute;
            bottom: -3px;
            color: #FFFFFF;
            font-size: 12px;
            scale: 83%;
            line-height: 12px;
            width: 69%;
            text-align: center;
        }
    }
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

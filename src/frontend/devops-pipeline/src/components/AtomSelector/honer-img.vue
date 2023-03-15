<template>
    <section class="honer-img">
        <section class="img-wrapper">
            <img class="img-self" :src="detail.logoUrl">
            <template v-if="recentHoner">
                <img class="honer-self" src="../../images/honer.png">
                <span
                    v-bk-overflow-tips="{ extCls: 'tippy-padding', zIndex: 10000 }"
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
            }
        }
    }
</script>

<style lang="scss" scoped>
    .honer-img {
        width: 100%;
        height: 100%;
        position: relative;
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
        }
    }
</style>

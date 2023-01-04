<template>
    <section class="honer-tags">
        <span
            v-for="renderHoner in renderHoners"
            :key="renderHoner.honorId"
            class="honer-tag"
        >
            <img class="tag-image" :src="HonerLeftImage">
            <span class="tag-txt">
                <span class="tag-txt-main">{{ renderHoner.honorTitle }}</span>
                <img class="tag-txt-image" :src="HonerCenterImage">
            </span>
            <img class="tag-image" :src="HonerRightImage">
        </span>
        <span v-if="honorInfos.length > maxNum" class="honer-num">
            +{{ honorInfos.length - maxNum }}
        </span>
        <span class="honer-gap" v-if="renderHoners.length && indexInfos.length"></span>
    </section>
</template>

<script>
    import HonerLeftImage from '../images/honer-left.png'
    import HonerCenterImage from '../images/honer-center.png'
    import HonerRightImage from '../images/honer-right.png'

    export default {
        props: {
            detail: Object,
            maxNum: Number
        },

        data () {
            return {
                HonerLeftImage,
                HonerCenterImage,
                HonerRightImage
            }
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
            position: relative;
        }
        .tag-txt-main {
            position: relative;
            z-index: 11;
            font-size: 12px;
            color: #fff;
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

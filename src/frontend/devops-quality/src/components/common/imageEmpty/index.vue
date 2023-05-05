<template>
    <section class="image-empty-tips">
        <img v-if="imgType !== 'noCollect'" :src="noDataSrc" alt="" class="no-data-pic">
        <img v-if="imgType === 'noCollect'" src="./../../../images/box.png" alt="" class="no-collect-pic">
        <p class="title">{{title}}</p>
        <p class="desc">{{desc}}</p>
        <p class="btns-row">
            <slot name="btns">
                <template v-if="btns.length">
                    <button class="bk-button"
                        v-for="(btn, index) in btns"
                        :key="index"
                        :class="[`bk-${btn.type}`, `bk-button-${btn.size}`]"
                        @click="btn.handler">
                        {{btn.text}}
                    </button>
                </template>
            </slot>
        </p>
    </section>
</template>

<script>
    import noData from '@/images/box.png'
    export default {
        props: {
            imgType: {
                type: String,
                default: 'noData'
            },
            title: {
                type: String,
                default: ''
            },
            desc: {
                type: String,
                default: ''
            },
            btns: {
                type: Array,
                default () {
                    return []
                }
            }
        },
        data () {
            return {
                noDataSrc: ''
            }
        },
        created () {
            this.noDataSrc = noData
        }
    }
</script>

<style lang="scss">
    @import './../../../scss/conf';

    .image-empty-tips {
        width: 913px;
        margin: 0 auto;
        text-align: center;
        .title {
            color: #333C48;
            font-size: 18px;
            line-height: 26px;
        }
        .desc {
            margin-top: 10px;
            margin-bottom: 20px;
            color: $fontWeightColor;
            font-size: 14px;
        }
        .btns-row {
            font-size: 0;
            .bk-button {
                & + .bk-button {
                    margin-left: 10px;
                }
            }
        }
        .no-data-pic {
            margin-top: 90px;
            margin-bottom: 14px;
            max-width: 320px;
            max-height: 320px;
        }
        .no-collect-pic {
            margin-top: 40px;
            margin-bottom: 24px;
            width: 320px;
            height: 320px;
        }
    }
</style>

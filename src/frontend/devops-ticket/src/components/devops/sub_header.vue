<template id="subHeader">
    <section class="sub-header clearfix">
        <div class="sub-header-left clearfix">
            <slot name="left">
                <span class="default-logo fl"
                    v-if="$slots.logo">
                    <slot name="logo"></slot>
                </span>

                <span class="default-title fl"
                    v-if="title">
                    {{ title }}
                </span>
            </slot>
        </div>
        <div class="sub-header-center">
            <slot name="center"></slot>
        </div>
        <div class="sub-header-right clearfix">
            <slot name="right">
                <ul class="default-link-list">
                    <li v-if="links.length > 0" v-for="item of links">
                        <a href="javascript:;" class="text-link"
                            @click="item.handler">
                            {{ item.title }}
                        </a>
                    </li>
                </ul>
            </slot>
        </div>
    </section>
</template>

<script>
    export default {
        props: {
            title: {
                type: String,
                default: ''
            },
            desc: {
                type: String
            },
            links: {
                type: Array,
                default () {
                    return []
                }
            }
        }
    }
</script>

<style lang="scss">
    @import './../../scss/conf';

    .sub-header {
        width: 100%;
        height: 60px;
        padding: 0 30px;
        border-bottom: 1px solid $borderWeightColor;
        box-shadow: 0 2px 5px rgba(0, 0, 0, .03);
        display: flex;
        background: #fff;
        &-left {
            display: flex;
            flex: 1;
            align-items: center;
            height: 100%;
            &.sub-logo {
                width: 260px;
                font-size: 0;
            }
        }
        &-right {
            flex: 1;
            height: 100%;
            align-items: center;
            text-align: right;
            .text-link {
                font-size: 14px;
            }
        }
        &-center {
            flex: 1;
            align-items: center;
            text-align: center;
        }
        .default-logo {
            margin-right: 11px;
        }
        .default-title {
            letter-spacing: .5px;
            font-size: 16px;
            color: #333948;
        }
        .default-link-list {
            height: 100%;
            font-size: 0;
            li {
                display: inline-block;
                height: 100%;
                line-height: 60px;
                font-size: 14px;
                & + li {
                    margin-left: 20px;
                }
            }
        }
        .default-subheader-icon {
            display: block;
            width: 32px;
            height: 32px;
            line-height: 32px;
            border-radius: 50%;
            // background-color: $fontWeightColor;
            color: #fff;
            text-align: center;
            font-size: 20px;
        }
    }

    @media screen and (max-width: 1500px) {
        .sub-header-left.sub-logo {
            width: 240px;
        }
    }
</style>

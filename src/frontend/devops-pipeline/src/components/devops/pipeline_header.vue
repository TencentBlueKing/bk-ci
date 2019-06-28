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
                <i v-if="links.length" class="bk-icon icon-question-circle" @click="links[0].handler"></i>
            </slot>
        </div>
        <div class="sub-header-center">
            <slot name="center"></slot>
        </div>
        <div class="sub-header-right clearfix">
            <slot name="right">

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
            max-width: 124px;
            height: 100%;
            &.sub-logo {
                width: 260px;
                font-size: 0;
            }
            i {
                padding-left: 10px;
                color: #c4cdd6;
                cursor: pointer;
            }
        }
        &-right {
            flex: 1;
            max-width: 80px;
            height: 100%;
            align-items: center;
            text-align: right;
            vertical-align: middle;

        }
        &-center {
            display: flex;
            flex: 5;
            align-items: center;
            justify-content: center;
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
            line-height: 60px;
            i {
                font-size: 18px;
                color: #63656E;
                margin: 21px 0 21px 10px;
            }
            .order-icon {
                padding-bottom: 23px;
                &:hover {
                    color: $iconPrimaryColor;
                }
            }
            .layout-icon {
                cursor: pointer;
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
        .list-method:hover .feedback-menu {
            display: block;
        }
        .feedback-menu {
            z-index: 2;
            display: none;
            position: absolute;
            background-color: white;
            border: 1px solid $borderWeightColor;
            border-radius: 2px;
            top: 52px;
            right: 15px;
            box-shadow: 0 3px 6px rgba(51, 60, 72, 0.12);
            &:before {
                position: absolute;
                content: '';
                width: 8px;
                height: 8px;
                border: 1px solid $borderWeightColor;
                border-bottom: 0;
                border-right: 0;
                right: 18px;
                top: -5px;
                transform: rotate(45deg);
                background: white;
            }
            li {
                border-bottom: 1px solid $borderWeightColor;
                &:last-child {
                    border: 0;
                }
                line-height: 32px;
                a {
                    cursor: pointer;
                    line-height: 32px;
                    white-space: nowrap;
                    padding: 0 14px;
                    color: $fontWeightColor;
                    &:hover {
                        color: $primaryColor;
                    }
                }
            }
        }
    }

    @media screen and (max-width: 1500px) {
        .sub-header-left.sub-logo {
            width: 240px;
        }
    }
</style>

<template id="subHeader">
    <section class="sub-header">
        <div class="sub-header-left">
            <slot name="left">
                <span
                    class="default-logo fl"
                    v-if="$slots.logo"
                >
                    <slot name="logo"></slot>
                </span>

                <span
                    class="default-title fl"
                    v-if="$slots.title || title"
                >
                    <slot name="title">
                        {{ title }}
                    </slot>
                </span>
            </slot>
        </div>
        <div class="sub-header-right">
            <slot name="right"></slot>
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
        justify-content: space-between;
        background: #fff;
        flex-shrink: 0;
        &-left {
            display: flex;
            flex: 1;
            align-items: center;
        }
        &-right {
            display: flex;
            flex: 1;
            max-width: fit-content;
            height: 100%;
            align-items: center;
        }
        .default-logo {
            margin-right: 11px;
        }
        .default-title {
            flex: 1;
            letter-spacing: .5px;
            font-size: 16px;
            color: #333948;
        }
    }
</style>

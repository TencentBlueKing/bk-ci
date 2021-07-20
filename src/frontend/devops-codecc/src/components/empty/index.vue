<template>
    <div :class="['empty', `empty-${size}`]">
        <img src="../../images/empty.png" class="empty-img">
        <div class="title">{{title}}</div>
        <div class="desc" v-if="desc">{{desc}}</div>
        <template v-if="$slots.action">
            <div class="action">
                <slot name="action"></slot>
            </div>
        </template>
        <template>
            <slot></slot>
        </template>
    </div>
</template>

<script>
    export default {
        name: 'empty',
        props: {
            title: {
                type: String,
                default: 'Empty'
            },
            desc: {
                type: String,
                default: ''
            },
            size: {
                type: String,
                default: 'normal',
                validator (value) {
                    if (['normal', 'small'].indexOf(value) === -1) {
                        console.error(`size property is not valid: '${value}'`)
                        return false
                    }
                    return true
                }
            }
        },
        data () {
            return {}
        }
    }
</script>

<style lang="postcss">
    .empty {
        text-align: center;

        .icon {
            background: url(../../images/empty.png) no-repeat 50% 0;
            background-size: contain;
            margin: 0 auto;
        }
        &.empty-normal {
            .icon {
                width: 91px;
                height: 57px;
            }
        }
        &.empty-small {
            .icon {
                width: 64px;
                height: 40px;
            }
            .title {
                font-size: 12px;
                margin-top: 0;
            }
        }

        .title {
            font-size: 20px;
            color: #333;
            margin-top: 16px;
        }
        .desc {
            font-size: 14px;
            margin: 8px 0;
        }
        .action {
            margin-top: 12px;
        }
        .empty-img {
            width: 60px;
            /* height: 56px; */
        }
    }
</style>

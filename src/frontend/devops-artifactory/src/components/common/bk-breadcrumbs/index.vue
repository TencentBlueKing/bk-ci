<template>
    <div class="bk-breadcrumbs">
        <ul v-if="list.length">
            <li class="bk-breadcrumbs-item"
                :key="item.text"
                v-for="(item, index) of list">
                <span class="bk-breadcrumbs-text"
                    :class="{ &quot;disabled&quot;: item.disabled }"
                    @click.stop="itemClickHandler(item)">
                    {{ item.text }}
                </span>
                <i class="bk-icon icon-angle-right bk-breadcrumbs-icon"
                    v-if="index < list.length - 1">
                </i>
            </li>
        </ul>
    </div>
</template>

<script>
    /**
     * @param list {Array} 用于渲染面包屑内容的数组，数组元素为对象，每个对象中可以传入以下内容：
        text：显示的文字内容
        disabled：当前节点能否点击
        handler：点击的回调函数
     */
    export default {
        props: {
            list: {
                type: Array,
                default () {
                    return []
                }
            }
        },
        methods: {
            itemClickHandler (item) {
                item.handler && typeof item.handler === 'function' && item.handler(item)
            }
        }
    }
</script>

<style lang="scss">
    @import './../../../scss/conf';

    .bk-breadcrumbs {
        font-size: 0;
        &-item {
            display: inline-block;
            font-size: 16px;
        }
        &-text {
            cursor: pointer;
            color: $fontWeightColor;
            &:hover {
                color: $primaryColor;
            }
            &.disabled {
                cursor: default;
                &:hover {
                    color: $fontWeightColor;
                }
            }
        }
        &-icon {
            margin-right: 5px;
            font-size: 12px;
        }
    }
</style>

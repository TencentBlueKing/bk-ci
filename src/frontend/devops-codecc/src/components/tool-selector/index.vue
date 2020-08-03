<template>
    <ul>
        <li
            v-for="item in list"
            :class="picked && picked.includes(item.id) ? 'active' : ''"
            @click="selectedItem(item.id)"
            class="settings-trigger-item"
            :key="item.id">
            <img class="item-pic" :src="item.logo">
            <p :title="item.name">{{item.name}}</p>
        </li>
    </ul>
</template>

<script>
    export default {
        name: 'week-selector',
        props: {
            picked: {
                type: Array,
                default: () => ([])
            },
            list: {
                type: Array,
                default: () => ([])
            }
        },
        data () {
            return {
            }
        },
        computed: {
        },
        methods: {
            selectedItem (id) {
                this.$emit('select', id)
            }
        }
    }
</script>

<style lang="postcss" scoped>
    @import '../../css/variable.css';

    .settings-trigger-item {
        position: relative;
        margin-right: 8px;
        margin-bottom: 8px;
        display: inline-block;
        width: 89px;
        height: 87px;
        border-radius: 2px;
        border: 1px solid #d9e2e8;
        background-color: #f7f9fb;
        cursor: pointer;
        line-height: 12px;
        text-align: center;
        font-size: 12px;

        .item-pic {
            height: 64px;
            width: 64px;
        }
        p {
            font-size: 14px;
            overflow: hidden;
            white-space:nowrap;
            text-overflow: ellipsis;
            height: 14px;
        }
    }
    .active {
        border: 1px solid $goingColor;
        color: $goingColor;

        &::before {
            content: "";
            width: 0;
            height: 0;
            border-style: solid;
            position: absolute;
            right: -1px;
            bottom: -1px;
            border-width: 0 0 14px 20px;
            border-color: transparent transparent #3a84ff transparent;
        }
        &::after {
            content: "\e977";
            color: #fff;
            position: absolute;
            right: 0;
            bottom: -2px;
            font-size: 12px;
            font-weight: bold;
            font-family: bk-icons-linear!important;
        }
    }
</style>

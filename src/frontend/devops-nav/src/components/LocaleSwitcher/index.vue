<template>
    <div
        v-clickoutside="hideList"
        class="devops-locale-switcher"
    >
        <div
            class="devops-locale-switcher-entry"
            @click="toggleList"
        >
            <icon
                :name="$i18n.locale"
                size="20"
            />
            <span>{{ $i18n.locale }}</span>
            <i class="bk-icon icon-down-shape" />
        </div>

        <ul
            v-show="showLocaleList"
            class="devops-locale-switcher-list"
        >
            <li
                v-for="item in $localeList"
                :key="item"
                @click="$setLocale(item)"
            >
                <icon
                    :name="item"
                    size="20"
                />
                {{ item }}
            </li>
        </ul>
    </div>
</template>

<script lang="ts">
    import Vue from 'vue'
    import { Component } from 'vue-property-decorator'
    import { clickoutside } from '../../directives/index'

    @Component({
        directives: {
            clickoutside
        }
    })
    export default class LocaleSwitcher extends Vue {
        showLocaleList: boolean = false
        hideList () {
            this.showLocaleList = false
        }

        toggleList (show) {
            this.showLocaleList = !this.showLocaleList
        }
    }
</script>

<style lang="scss">
    @import '../../assets/scss/conf';
    .devops-locale-switcher {
        color: $fontLigtherColor;
        height: 100%;
        &-entry {
            cursor: pointer;
            display: flex;
            height: 100%;
            padding:0 12px;
            align-items: center;
            &:hover {
                color: white;
                background-color: black;
            }
            > span {
                padding-left: 6px;
            }
        }
        &-list {
            position: absolute;
            background: white;
            width: 120px;
            position: absolute;
            background: white;
            border: 1px solid $borderWeightColor;
            border-radius: 2px;
            box-shadow: 0 3px 6px rgba(51, 60, 72, 0.12);
            top: 53px;
            right: 130px;
            cursor: default;
            &:after {
                position: absolute;
                content: '';
                width: 8px;
                height: 8px;
                border: 1px solid $borderWeightColor;
                border-bottom: 0;
                border-right: 0;
                transform: rotate(45deg);
                background: white;
                top: -5px;
                right: 36px;
            }
            > li {
                display: flex;
                align-items: center;
                justify-content: space-around;
                padding: 10px 20px;
                color: $fontColor;
                cursor: pointer;
            }
        }
    }
</style>

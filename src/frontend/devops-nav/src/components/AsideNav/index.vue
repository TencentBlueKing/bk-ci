<template>
    <bk-navigation
        class="bkdevops-aside-nav"
        v-bind="navTheme"
        :default-open="true"
        :header-title="headerTitle"
    >
        <template slot="side-header">
            <logo
                :name="nav.icon"
                size="32"
                class="title-icon"
            />
            <span class="title-desc">{{ nav.title }}</span>
        </template>
        <div
            slot="header-set"
            class="header-right-set"
        >
            <slot name="header-right" />
        </div>
        
        <div slot="menu">
            <bk-navigation-menu
                ref="menu"
                :default-active="$route.name"
                v-bind="navTheme"
                :toggle-active="nav.toggle"
            >
                <bk-navigation-menu-item
                    v-for="item in nav.menu"
                    :key="item.id"
                    v-bind="item"
                    @click="menuClick"
                >
                    <span>{{ item.name }}</span>
                </bk-navigation-menu-item>
            </bk-navigation-menu>
        </div>
        <slot name="content" />
    </bk-navigation>
</template>

<script lang="ts">
    import Vue from 'vue'
    import { Prop, Component } from 'vue-property-decorator'
    
    @Component
    export default class AsideNav extends Vue {
        @Prop({ default: '' })
        headerTitle: string

        @Prop({ required: true })
        nav: Object

        @Prop({ required: true })
        menuClick
        
        get navTheme () {
            return {
                themeColor: 'white',
                itemDefaultBgColor: 'white',
                itemActiveBgColor: '#ebf4ff',
                itemHoverBgColor: '#ebf4ff',
                itemHoverColor: '#3c96ff',
                itemHoverIconColor: '#3c96ff',
                itemActiveColor: '#3c96ff',
                itemDefaultColor: '#7b7d8a',
                itemActiveIconColor: '#3c96ff'
            }
        }
    }
</script>

<style lang="scss">
    @import './../../assets/scss/conf';
    .bkdevops-aside-nav.bk-navigation {
        height: calc(100vh - 50px);
        .navigation-nav {
            .nav-slider-title {
                border-bottom-color: #dcdee5;
                box-shadow: 0 3px 6px 0 rgba(99,101,110,.06);
                .title-desc {
                    color: $fontWeightColor;
                }
                .devops-icon {
                    margin-left: 10px;
                }
            }
            .nav-slider {
                border-right: 1px solid #dcdee5 !important;
            }
            .nav-slider-list {
                height: 100% !important;
            }

            .nav-slider-footer {
                .footer-icon {
                    color: $fontWeightColor;
                    &:hover {
                        background-color: #ebf4ff;
                        color: $primaryColor;
                    }
                }
            }
            .navigation-menu-item .devops-icon {
                font-family: 'bk-icons-linear' !important;
            }
            .navigation-menu-item[group]:after {
                content: " ";
                width: 100%;
                height: 1px;
                position: absolute;
                bottom: -6px;
                background: $borderColor;
                left: 0;
                z-index: 100;
            }
        }
        .navigation-container {
            height: calc(100vh - 50px);
            .container-header {
                display: none;
            }
            .header-right-set {
                padding-right: 20px;
            }
            .container-content {
                max-height: 100%;
                padding: 0;
            }
        }

    }
</style>

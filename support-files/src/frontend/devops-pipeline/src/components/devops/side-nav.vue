<template>
    <div class="side-nav">
        <div class="side-menu-nav clearfix">
            <div @click.stop="backUrl(nav.backUrl)" v-if="nav.backUrl" class="back-icon"><i class="bk-icon icon-angle-left"></i></div>
            <logo :name="nav.icon" size="32" class="nav-icon" />
            <span class="side-menu-title">{{ nav.title }}</span>
        </div>
        <section class="side-menu-list">
            <div class="side-menu-item"
                v-for="(menu, index) of sideMenuList" :key="index">
                <p class="title" v-if="menu.title">{{ menu.title }}</p>
                <bk-menu
                    v-if="sideMenuList.length"
                    :key="`devopsSideMenu${index}`"
                    :list="menu.list"
                    @item-selected="menuSelected">
                </bk-menu>
            </div>
        </section>
        <p class="biz-copyright">Copyright © 2012-<span>{{ currentYear }}</span> Tencent BlueKing. All Rights Reserved</p>
    </div>
</template>
<script>
    import bkMenu from '@/components/common/menu'
    import Logo from '@/components/Logo'

    export default {
        name: 'side-nav',
        components: {
            bkMenu,
            Logo
        },
        props: {
            sideMenuList: {
                type: Array,
                default () {
                    return [
                        {
                            list: [],
                            title: ''
                        }
                    ]
                }
            },
            dropdownConfig: {
                type: Object,
                default () {
                    return {}
                }
            },
            nav: {
                type: Object,
                default () {
                    return {
                        icon: '',
                        title: ''
                    }
                }
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            currentYear () {
                return new Date().getFullYear()
            }
        },
        mounted () {
            if (!this.projectId) {
                return
            }
            this.$store.commit('updateCurSelectedOnlineProject', this.projectId)
        },
        methods: {
            backUrl (name) {
                this.$router.push({
                    name
                })
            },
            /**
             * 左侧导航 menu 选择事件
             *
             * @param {Object} data menu 数据
             */
            menuSelected (data) {
                data.item.isSelected = true

                this.$router.push({
                    name: data.isChild ? data.child.id : data.item.id,
                    params: data.item.params
                })
            }
        }
    }
</script>

<style lang="scss">
    @import './../../scss/conf';

    .side-nav {
        position: relative;
        height: 100%;
        padding-bottom: 100px;

        .biz-pm-dropdown .bk-selector-list {
            top: 60px !important;
        }
    }

    .main-menu {
        padding-bottom: 15px;
        margin: 20px 0 15px 0;
        border-bottom: 1px solid #dde4eb;
    }
    .sub-menu {
        .title {
            font-size: 14px;
            font-weight: bold;
            text-align: left;
            color: #c3cdd7;
            padding-left: 27px;
            margin: 15px 0 15px 0;
            display: inline-block;
        }
    }
    .side-menu-list {
        padding-top: 20px;
        .title {
            margin-left: 25px;
            margin-bottom: 15px;
            font: {
                size: 14px;
                weight: bold;
            }
            color: #c3cdd7;
        }
        .side-menu-item:first-child {
            margin-bottom: 30px;
            & + .side-menu-item {
                border-top: 1px solid $borderColor;
            }
        }
        .side-menu-item + .side-menu-item {
            padding-top: 34px;
        }
    }
    .side-menu-nav {
        height: 60px;
        padding: 0 25px;
        border-bottom: 1px solid $borderWeightColor;
        font-size: 16px;
        color: #333948;
        display: flex;
        align-items: center;
        .nav-icon {
            margin-right: 10px;
        }
        .back-icon {
            color: #fff;
            cursor: pointer;
            text-align: center;
            margin-left: -25px;
            height: 100%;
            background: $primaryColor;
            width: 30px;
            line-height: 60px;
        }
    }
    .default-sidemenu-icon {
        display: inline-block;
        width: 32px;
        height: 32px;
        line-height: 32px;
        border-radius: 50%;
        color: #fff;
        text-align: center;
        font-size: 20px;
        background-color: #63656E;
    }
    .biz-copyright {
        font-size: 12px;
        color: #b7c0ca;
        width: 100%;
        position: absolute;
        bottom: 25px;
        text-align: center;
        line-height: 20px;
        padding: 0 15px;
    }
</style>

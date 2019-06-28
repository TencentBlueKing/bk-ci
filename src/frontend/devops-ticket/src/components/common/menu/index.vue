<template>
    <div class="bkc-menu">
        <ul v-if="menuList.length">
            <li class="bkc-menu-item" v-for="(item, itemIndex) in menuList" :key="itemIndex">
                <div v-if="(!item.children || !item.children.length || item.showChildren === false) && !item.childrenType" class="bkc-menu-title-wrapper"
                    :class="[item.hide, item.disable, item.id === $route.name || (item.children && item.children.some(child => child.id === $route.name)) ? 'selected' : '']"
                    @click="(!item.disable && !item.hide) ? handleClick(item, itemIndex, $event) : () => {}">
                    <i class="bk-icon left-icon" :class="[item.disable, item.icon]"></i>
                    <div class="bkc-menu-title">{{item.name}}</div>
                    <i class="biz-badge" v-if="item.badge !== undefined">{{item.badge}}</i>
                </div>

                <div v-else class="bkc-menu-title-wrapper"
                    :class="[item.hide, item.disable, item.isChildSelected ? 'child-selected' : '', item.isOpen ? 'open' : '']"
                    @click="(!item.disable && !item.hide) ? openChildren(item, itemIndex, $event) : () => {}">
                    <i class="bk-icon left-icon" :class="[item.disable, item.icon]"></i>
                    <div class="bkc-menu-title">{{item.name}}</div>
                    <i class="bk-icon right-icon icon-angle-down" :class="{ 'open': item.isOpen }"></i>
                </div>
                <collapse-transition>
                    <ul v-show="item.isOpen">
                        <li class="bkc-menu-child-item" v-for="(child, childIndex) in item.children" :key="childIndex">
                            <div class="bkc-menu-child-title-wrapper" :class="child.id === $route.name ? 'selected' : ''"
                                @click="handleChildClick(item, itemIndex, child, childIndex, $event)">
                                {{child.name}}
                            </div>
                        </li>
                    </ul>
                </collapse-transition>
            </li>
        </ul>
        <template v-else>
            <div class="biz-no-data" style="margin-top: 100px;">
                <i class="bk-icon icon-empty"></i>
                <p>暂时没数据!</p>
            </div>
        </template>
    </div>
</template>

<script>
    import CollapseTransition from '@/utils/collapse-transition.js'

    export default {
        name: 'bkc-menu',
        components: {
            CollapseTransition
        },
        props: {
            list: {
                type: Array,
                required: true
            },
            icon: {
                type: String,
                default: () => {
                    return 'icon-id'
                }
            },
            menuChangeHandler: {
                type: Function,
                default: null
            }
        },
        data () {
            return {
                menuList: this.list
            }
        },
        watch: {
            list: function (newVal, oldVal) {
                this.menuList = newVal
            }
        },
        methods: {
            clearSelectCls (menuList = this.menuList) {
                menuList.forEach(item => {
                    item.isSelected = false
                    item.isOpen = false
                    if (item.children) {
                        item.isChildSelected = false
                        item.children.forEach(childItem => {
                            childItem.isSelected = false
                        })
                    }
                })
            },
            openChildren (item, itemIndex, e) {
                item.isOpen = !item.isOpen
                this.menuList.splice(itemIndex, 1, item)
                if (item.isOpen && item.childrenType === 'bk-tree') {
                    this.$emit('item-selected', {
                        isChild: false,
                        item,
                        itemIndex
                    })
                }
            },
            handleClick (item, itemIndex) {
                item.isSelected = !item.isSelected
                this.menuList.splice(itemIndex, 1, item)
                this.$emit('item-selected', {
                    isChild: false,
                    item,
                    itemIndex
                })
            },
            handleChildClick (item, itemIndex, child, childIndex, e) {
                if (this.menuChangeHandler && typeof this.menuChangeHandler === 'function') {
                    const ret = this.menuChangeHandler({
                        isChild: true,
                        item,
                        itemIndex,
                        child,
                        childIndex
                    })
                    if (ret) {
                        this.clearSelectCls()
                        child.isSelected = !child.isSelected
                        item.children.splice(childIndex, 1, child)
                        if (child.isSelected) {
                            item.isChildSelected = true
                            item.isOpen = true
                        }

                        this.menuList.splice(itemIndex, 1, item)
                    }
                    return
                }

                this.clearSelectCls()
                child.isSelected = !child.isSelected
                item.children.splice(childIndex, 1, child)
                if (child.isSelected) {
                    item.isChildSelected = true
                    item.isOpen = true
                }

                this.menuList.splice(itemIndex, 1, item)
                this.$emit('item-selected', {
                    isChild: true,
                    item,
                    itemIndex,
                    child,
                    childIndex
                })
            }
        }
    }
</script>

<style lang="scss">
    @import '../../../scss/conf.scss';

    .collapse-transition {
        -webkit-transition: .2s height ease-in-out, .2s padding-top ease-in-out, .2s padding-bottom ease-in-out;
        -moz-transition: .2s height ease-in-out, .2s padding-top ease-in-out, .2s padding-bottom ease-in-out;
        transition: .2s height ease-in-out, .2s padding-top ease-in-out, .2s padding-bottom ease-in-out;
    }
    .bk-menu {
        position: relative;
    }
    .bkc-menu-item {
        cursor: pointer;
    }
    .bkc-menu-child-item {
        &:hover {
            color: $primaryColor;
        }
    }
    .bkc-menu-title-wrapper {
        height: 48px;
        line-height: 48px;
        font-size: 14px;
        padding: 0 50px 0 25px;
        position: relative;
        &:hover {
            color: $primaryColor;
            .left-icon {
                color: $primaryColor;
            }
        }

        &.hide {
            display: none;
        }
        &.disable {
            cursor: not-allowed;
            color: #c3cdd7;
        }

        &.selected {
            background-color: $primaryLightColor;
            color: $primaryColor;
            .left-icon {
                color: $primaryColor;
            }
        }
        &.open {
            color: $primaryColor;
            .left-icon {
                color: $primaryColor;
            }
        }
        &.child-selected {
            font-weight: 700;
        }
        .biz-badge {
            position: absolute;
            right: 20px;
            top: 17px;
        }
        .left-icon {
            vertical-align: middle;
            font-size: 20px;
            position: absolute;
            top: 14px;
            color: #c4cdd6;
            &.disable {
                cursor: not-allowed;
                color: #c3cdd7;
            }
        }
        .right-icon {
            position: absolute;
            right: 20px;
            top: 17px;
            font-size: 12px;
            -webkit-transition: transform linear .2s;
            transition: transform linear .2s;
            &.open {
                color: $primaryColor;
                -webkit-transform: rotate(180deg);
                transform: rotate(180deg);
            }
        }
        .bkc-menu-title {
            margin-left: 40px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }
    }
    .bkc-menu-child-title-wrapper {
        font-size: 14px;
        height: 36px;
        line-height: 36px;
        padding-left: 65px;
        position: relative;
        &.selected {
            background-color: $primaryLightColor;
            color: $primaryColor;
        }
    }
</style>

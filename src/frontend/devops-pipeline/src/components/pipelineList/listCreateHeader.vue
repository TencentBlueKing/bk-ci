<template>
    <section>
        <create-header
            :create-text="'新建流水线'"
            @createPipeline="toggleTemplatePopup(true)">
            <div slot="addon" class="create-header-right">
                <span class="pipeline-num">（共{{num}}条流水线）</span>
                <i @click.stop="showSlide" class="layout-icon bk-icon icon-filter-shape" :class="{ 'active-icon': hasFilter }" title="过滤"></i>
                <span class="seperate-line">|</span>
                <i @click.stop="changeLayoutType('table')" class="layout-icon bk-icon icon-grid-view" v-if="layout === 'card'" title="卡片视图"></i>
                <i @click.stop="changeLayoutType('card')" class="layout-icon bk-icon icon-list-view" v-if="layout === 'table'" title="列表视图"></i>
                <div v-bk-clickoutside="hideFeedBackMenu" class="list-method" style="display:inline">
                    <i class="order-icon bk-icon icon-new-order" @click.stop="toggleFeedBackMenu" :class="{ 'active-icon': showOrderType }" title="排序"></i>
                    <ul class="feedback-menu" v-show="showOrderType">
                        <li v-for="(order, index) in orderList" :key="`order${index}`">
                            <a @click.stop="changeOrderType(order.id)">{{ order.name }}</a>
                        </li>
                    </ul>
                </div>
            </div>
        </create-header>
    </section>
</template>

<script>
    import createHeader from '@/components/devops/create_header'
    export default {
        components: {
            createHeader
        },
        props: {
            layout: {
                type: String
            },
            hasFilter: {
                type: Boolean
            },
            num: {
                type: String,
                default: 0
            }
        },
        data () {
            return {
                showOrderType: false,
                orderList: [
                    {
                        'id': 'NAME',
                        'name': '按名称A-Z'
                    }, {
                        'id': 'CREATE_TIME',
                        'name': '按创建时间'
                    }, {
                        'id': 'UPDATE_TIME',
                        'name': '按修改时间'
                    }
                ]
            }
        },
        methods: {
            hideFeedBackMenu () {
                this.showOrderType = false
            },
            toggleFeedBackMenu () {
                this.showOrderType = !this.showOrderType
            },
            changeLayoutType (val) {
                this.$emit('changeLayout', val)
            },
            changeOrderType (val) {
                this.showOrderType = false
                this.$emit('changeOrder', val)
            },
            toggleTemplatePopup (val) {
                this.$emit('showCreate', val)
            },
            showSlide () {
                this.$emit('showSlide', true)
            }
        }
    }
</script>

<style lang='scss'>
    @import './../../scss/conf';
    .create-header-right {
        line-height: 36px;
        position: relative;
        i {
            font-size: 21px;
            color: #63656E;
            vertical-align: middle;
            margin-left: 13px;
        }
        .layout-icon, .order-icon {
            cursor: pointer;
            &:hover {
                color: $iconPrimaryColor;
            }
        }
        .active-icon {
           color: $iconPrimaryColor;
        }
        .seperate-line {
            width:1px;
            height:18px;
            color: #DDE4EB;
            margin-left: 10px;
        }
        .pipeline-num {
            font-size: 12px;
            color: #c3cdd7;
        }
    }
    .feedback-menu {
        z-index: 3;
        position: absolute;
        background-color: white;
        border: 1px solid $borderWeightColor;
        border-radius: 2px;
        top: 40px;
        right: -15px;
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
</style>

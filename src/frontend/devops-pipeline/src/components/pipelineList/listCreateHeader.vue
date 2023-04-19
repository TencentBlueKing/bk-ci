<template>
    <section>
        <create-header
            :create-text="$t('newlist.addPipeline')"
            button-type="menu"
            trigger-type="click"
            :dropdown-menu-config="createPipelineDropMenuConf">
            <div slot="addon">
                <span class="pipeline-num">（{{ $t('newlist.sumPipelinesTips', [num]) }}）</span>
                <i @click.stop="showSlide" class="layout-icon devops-icon icon-filter-shape" :class="{ 'active-icon': hasFilter }" :title="$t('newlist.filter')"></i>
                <span class="seperate-line">|</span>
                <i @click.stop="changeLayoutType('table')" class="layout-icon devops-icon icon-grid-view" v-if="layout === 'card'" :title="$t('newlist.cardLayout')"></i>
                <i @click.stop="changeLayoutType('card')" class="layout-icon devops-icon icon-list-view" v-if="layout === 'table'" :title="$t('newlist.tableLayout')"></i>
                <div v-bk-clickoutside="hideFeedBackMenu" class="list-method" style="display:inline">
                    <i class="order-icon devops-icon icon-new-order" @click.stop="toggleFeedBackMenu" :class="{ 'active-icon': showOrderType }" :title="$t('newlist.order')"></i>
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
                        id: 'NAME',
                        name: this.$t('newlist.orderByAlpha')
                    }, {
                        id: 'CREATE_TIME',
                        name: this.$t('newlist.orderByCreateTime')
                    }, {
                        id: 'UPDATE_TIME',
                        name: this.$t('newlist.orderByUpdateTime')
                    }, {
                        id: 'LAST_EXEC_TIME',
                        name: this.$t('newlist.orderByExecuteTime')
                    }
                ]
            }
        },
        computed: {
            createPipelineDropMenuConf () {
                return {
                    showHandler: () => {},
                    hideHandler: () => {},
                    align: 'center',
                    list: [{
                        text: this.$t('newPipelineFromTemplateLabel'),
                        handler: () => {
                            this.$emit('showCreate', true)
                        }
                    }, {
                        text: this.$t('newPipelineFromJSONLabel'),
                        handler: () => {
                            this.$emit('showImport', true)
                        }
                    }]
                }
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
        right: -13px;
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

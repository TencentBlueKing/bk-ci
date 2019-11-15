<template>
    <div :class="{ &quot;soda-accordion&quot;: true, &quot;is-danger&quot;: isError, &quot;showCheckbox&quot;: showCheckbox }">
        <header :active="isShow" @click="toggleContent" class="header">
            <i class="bk-icon icon-angle-down" />
            <slot name="header"></slot>
        </header>
        <transition name="slideLeft">
            <section v-if="condition">
                <section v-if="isShow" class="content">
                    <slot name="content"></slot>
                </section>
            </section>
            <section v-else>
                <section v-show="isShow" class="content">
                    <slot name="content"></slot>
                </section>
            </section>
        </transition>
    </div>
</template>

<script>
    export default {
        name: 'accordion',
        props: {
            afterToggle: Function,
            showContent: {
                type: Boolean,
                default: false
            },
            showCheckbox: {
                type: Boolean,
                default: false
            },
            isError: {
                type: Boolean,
                default: false
            },
            isVersion: {
                type: Boolean,
                default: false
            },
            condition: {
                type: Boolean,
                default: false
            }
        },
        data () {
            return {
                isShow: this.showContent
            }
        },
        watch: {
            showContent (val) {
                this.isShow = val
            }
        },
        methods: {
            toggleContent: function () {
                if (this.isVersion) {
                    this.isShow = this.showContent ? !this.isShow : this.isShow
                } else {
                    this.isShow = !this.isShow
                }
                if (typeof this.afterToggle === 'function') {
                    this.afterToggle(this.$el, this.isShow)
                }
            }
        }
    }
</script>

<style lang="scss">
    @import '../../../scss/conf.scss';
    .soda-accordion {
        border: 1px solid $borderColor;
        border-radius: 3px;
        margin: 12px 0;
        font-size: 12px;

        &.is-danger {
            color: $dangerColor;
            border-color: $dangerColor;

            .header {
                color: $dangerColor;
            }
        }
        .header {
            display: flex;
            color: $fontColor;
            background-color: white;
            padding: 10px 15px;
            align-items: center;
            cursor: pointer;

            .icon-angle-down {
                display: block;
                margin: 2px 12px 0 0;
                transition: all 0.3s ease;
            }
            &[active] {
                .icon-angle-down {
                    transform: rotate(-180deg)
                }
            }
        }
        .content {
            padding: 10px 15px;
        }

        &.showCheckbox {
            > .header {
                background-color: $fontWeightColor;
                color: white;
                .bk-icon {
                    display: none;
                }
                .var-header {
                    width: 100%;
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                }

            }
        }
    }

</style>

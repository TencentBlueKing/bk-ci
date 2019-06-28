<template>
    <div class="step-item"
        :class="[extCls, {
            'has-error': hasError && extCls !== 'create'
        }]"
        @mouseenter="itemEnterHandler"
        @mouseleave="itemLeaveHandler"
        @click="itemClickHandler">
        <div class="step-item__wrapper">
            <div class="base-left">
                <slot name='icon'></slot>
                <slot name='title'></slot>
            </div>
            <div class="base-right">
                <slot name='right'></slot>
            </div>
        </div>
    </div>
</template>

<script>
    export default {
        name: 'step-item_base',
        props: {
            extCls: String,
            // step在列表中的索引值
            stepIndex: {
                type: Number,
                default: 0
            },
            // step所在的stage在列表中的索引值
            stageIndex: {
                type: Number,
                default: 0
            },
            // step是否有错误
            hasError: {
                type: Boolean,
                default: false
            }
        },
        methods: {
            itemEnterHandler () {
                this.$emit('item-enter', {
                    stageIndex: this.stageIndex,
                    stepIndex: this.stepIndex
                })
            },
            itemLeaveHandler () {
                this.$emit('item-leave', {
                    stageIndex: this.stageIndex,
                    stepIndex: this.stepIndex
                })
            },
            itemClickHandler () {
                this.$emit('item-click', {
                    stageIndex: this.stageIndex,
                    stepIndex: this.stepIndex
                })
            }
        }
    }
</script>

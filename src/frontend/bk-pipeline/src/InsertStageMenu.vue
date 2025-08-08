<template>
    <span class="insert-stage direction">
        <div
            class="click-item"
            @click.stop="handleClick(false)"
        >
            <span>
                {{ t('insertStage') }}
            </span>
        </div>
        <div
            :class="{
                'disabled-item': disableFinally,
                'click-item': true
            }"
            @click.stop="!disableFinally && handleClick(true)"
        >
            <span>
                {{ t('insertFinallyStage') }}
            </span>
        </div>
    </span>
</template>

<script>
    import { localeMixins } from './locale'
    
    export default {
        name: 'insert-stage-menu',
        mixins: [localeMixins],
        props: {
            editStage: {
                type: Function,
                required: true
            },
            isLast: Boolean,
            disableFinally: Boolean
        },
        methods: {
            handleClick (isFinally = false) {
                this.editStage(false, isFinally, this.isLast)
            }
        }
    }
</script>

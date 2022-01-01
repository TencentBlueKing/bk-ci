<template>
    <span class="insert-stage direction">
        <div class="click-item" @click.stop="handleClick">
            <span>
                {{ $t('editPage.insertStage') }}
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
                {{ $t('editPage.insertFinallyStage') }}
            </span>
        </div>
    </span>
</template>

<script>
    export default {
        name: 'insert-stage-menu',
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

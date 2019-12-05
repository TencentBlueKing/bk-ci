<template>
    <bk-form-item
        v-bind="$attrs"
        :class="{ 'is-error': isError }"
    >
        <slot />
        <slot
            v-if="isError"
            name="error-tips"
        >
            <div class="error-tips">
                {{ errorMsg }}
            </div>
        </slot>
    </bk-form-item>
</template>

<script lang="ts">
    import Vue from 'vue'
    import { Prop, Component } from 'vue-property-decorator'
    
    @Component
    export default class DevopsFormItem extends Vue {
        @Prop()
        errorMsg: string = ''
        @Prop()
        isError: boolean = false
    }
</script>

<style lang="scss">
    @import '../../assets/scss/conf';
    .error-tips {
        color: $dangerColor;
        font-size: 12px;
        .text-link {
            color: $primaryColor;
            padding: 0 5px;
        }
    }
</style>

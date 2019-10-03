<template>
    <div class="bk-option-content">
        <slot>
            <div class="bk-option-content-default" :title="name">
                <span class="bk-option-name">{{name}}</span>
                <i class="bk-option-icon bk-icon icon-check-1" v-if="multiple && isSelected"></i>
            </div>
        </slot>
    </div>
</template>

<script lang="ts">
    import Vue from 'vue'
    import { Prop, Component } from 'vue-property-decorator'
    @Component
    export default class DevopsOption extends Vue {
        @Prop({ required: true })
        id: string | number
        @Prop({ required: true })
        name: string | number
        @Prop()
        multiple: boolean
        @Prop()
        isSelected: boolean
        @Prop()
        disabled: boolean
    }
</script>

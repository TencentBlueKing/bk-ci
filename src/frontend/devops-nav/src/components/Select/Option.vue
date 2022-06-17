<template>
    <div class="bk-option-content">
        <slot>
            <div
                class="bk-option-content-default"
                :title="name"
            >
                <span class="bk-option-name">{{ name }}</span>
                <i
                    v-if="multiple && isSelected"
                    class="bk-option-icon devops-icon icon-check-1"
                />
            </div>
        </slot>
    </div>
</template>

<script lang="ts">
    import Vue from 'vue'
    import { Prop, Component } from 'vue-property-decorator'
    @Component({
        name: 'devops-option'
    })
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

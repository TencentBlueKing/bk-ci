<template>
    <div
        class="we-dev-logo-area"
    >
        <Icon
            name="we"
            :size="26"
        />
        <Icon
            class="we-dev-logo"
            :name="weName"
            :size="size"
        />
    </div>
</template>

<script>
    import Icon from '@/components/Icon/index.vue'
    import { defineComponent, ref } from 'vue'
    export default defineComponent({
        components: {
            Icon
        },
        setup () {
            const weName = ref('wedev')
            const size = ref(90)
            
            return {
                weName,
                size
            }
        }
    })
</script>

<style lang="scss">
    .we-dev-logo-area {
        position: relative;
        transition: all .3s ease;
        background-color: #30304E;
        display: flex;
        height: 100%;
        align-items: center;
        padding: 0 12px;
        overflow: hidden;
        width: 50px;
        
        &:hover {
            .we-dev-logo {
                transform: scaleX(1);
                opacity: 1;
            }
            width: 114px;
        }
        > svg {
            transition: all .3s ease;
            display: block;
            overflow: visible;
        }
        .we-dev-logo {
            position: absolute;
            overflow: hidden;
            left: 12px;
            opacity: 0;
            transform: scaleX(0);
            transform-origin: left center;
        }
    }
</style>

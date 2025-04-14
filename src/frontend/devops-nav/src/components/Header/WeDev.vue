<template>
    <a
        class="we-dev-logo-area"
        target="_blank"
        href="https://wedev.woa.com/"
    >
        <Icon
            name="w"
            :size="26"
        />
        <Icon
            class="we-dev-logo"
            name="edev"
            :size="50"
        />
    </a>
</template>

<script>
    import Icon from '@/components/Icon/index.vue'
    import { defineComponent } from 'vue'
    export default defineComponent({
        components: {
            Icon
        }
    })
</script>

<style lang="scss">
    .we-dev-logo-area {
        position: relative;
        transition: all .25s ease-in-out;
        background-color: #30304E;
        display: flex;
        height: 100%;
        align-items: center;
        padding: 0 12px;
        overflow: hidden;
        width: 50px;
        cursor: pointer;
        
        &:hover {
            .we-dev-logo {
                transform: translateX(28px);
                opacity: 1;
            }
            width: 102px;
        }
        &:not(:hover) {
            svg {
                &:first-child {
                   opacity: .4;
                }
            }
        }
        > svg {
            transition: all .25s ease-in-out;
            display: block;
            overflow: visible;
        }
        .we-dev-logo {
            position: absolute;
            overflow: hidden;
            left: 12px;
            transform-origin: left center;
            transform: translateX(0);
            opacity: 0;
        }
    }
</style>

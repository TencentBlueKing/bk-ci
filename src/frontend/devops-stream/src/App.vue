<template>
    <article class="stream-main">
        <stream-header></stream-header>
        <router-view class="stream-content" :name="childRouteName"></router-view>
    </article>
</template>

<script>
    import streamHeader from '@/components/header'
    import { mapState } from 'vuex'

    export default {
        name: 'App',

        components: {
            streamHeader
        },

        computed: {
            ...mapState(['exceptionInfo']),
            pathName () {
                return window.location.pathname || ''
            },
            childRouteName () {
                return this.exceptionInfo.type === 200 ? 'default' : 'exception'
            }
        },

        created () {
            if (this.pathName === '/') {
                let routeName = 'dashboard'
                if (!localStorage.getItem('visited-stream-home')) {
                    localStorage.setItem('visited-stream-home', true)
                    routeName = 'home'
                }
                this.$router.push({
                    name: routeName
                })
            }
        }
    }
</script>

<style lang="postcss">
    .stream-main {
        width: 100%;
        height: 100vh;
        overflow: auto;
        font-size: 14px;
        color: #7b7d8a;
        background: #f5f5f5;
        font-family: -apple-system,PingFang SC,BlinkMacSystemFont,Microsoft YaHei,Helvetica Neue,Arial;
        ::-webkit-scrollbar-thumb {
            background-color: #c4c6cc !important;
            border-radius: 3px !important;
            &:hover {
                background-color: #979ba5 !important;
            }
        }
        ::-webkit-scrollbar {
            width: 6px !important;
            height: 6px !important;
        }
    }

    .stream-content {
        height: calc(100% - 61px);
        overflow: auto;
    }
</style>

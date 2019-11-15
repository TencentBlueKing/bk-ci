<template>
    <div class="layou-outer">
        <!-- <nav-top /> -->
        <header class="page-header">
            <div class="app-logo">
                <!-- <img :src="logo" :alt="this.$t('st.代码检查中心')"> -->
            </div>
            <div class="breadcrumb">{{title}}</div>
        </header>
        <main class="page-main">
            <div class="page-content">
                <div class="main-container">
                    <slot />
                </div>
            </div>
        </main>
    </div>
</template>

<script>
    import logo from '@/images/logo.png'
    // import NavTop from './nav-top'

    export default {
        components: {
            // NavTop
        },
        data () {
            return {
                logo
            }
        },
        computed: {
            title () {
                const title = this.$route.meta.title
                return this.$t(`nav.${title}`)
            }
        }
    }
</script>

<style lang="postcss">
    .layou-outer {
        --headerHeight: 50px;
        .page-header {
            display: flex;
            height: var(--headerHeight);
            align-items: center;
            padding: 0 20px;
            border-bottom: 1px solid #d1d1d1;
            .app-logo {}
            .breadcrumb {
                margin-left: 8px;
                color: #63656e;
            }
        }
        .page-main {
            height: calc(100vh - var(--navTopHeight) - var(--headerHeight));
            overflow: auto;

            .page-content,
            .main-container,
            .main-content {
                height: 100%;
            }

            .main-container {
                padding: 20px 0;
            }
        }
    }
</style>

<template>
    <header class="store-nav">
        <section class="nav-main">
            <icon class="title-icon" name="color-logo-store" size="22" />
            <span class="back-home quick-route" @click="changePage({ name: 'atomHome', query: { pipeType: type } })"> {{ $t('store.研发商店') }} </span>
            <template v-for="breadCrumb in breadCrumbs">
                <i class="right-arrow banner-arrow" :key="breadCrumb.name"></i>
                <span class="banner-des quick-route" :key="breadCrumb.name" v-if="breadCrumb.to" @click="changePage(breadCrumb.to)"> {{ breadCrumb.name }} </span>
                <span class="banner-des" :key="breadCrumb.name" v-else> {{ breadCrumb.name }} </span>
            </template>
        </section>
        <slot></slot>
    </header>
</template>

<script>
    export default {
        props: {
            breadCrumbs: Array,
            type: String
        },

        methods: {
            changePage (to) {
                this.$router.push(to)
            }
        }
    }
</script>

<style lang="scss" scoped>
    .store-nav {
        height: 5.6vh;
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 0 .32rem;
        background-color: #fff;
        color: #999;
        box-shadow: 0 1px 0 0 #ebedf0;
        .nav-main {
            display: flex;
            align-items: center;
        }
        .title-icon {
            margin-right: 6px;
        }
        .right-arrow {
            height: 5.6vh;
            &::after {
                margin-top: -2.5px;
                border-color: #252935;
            }
        }
        .quick-route {
            color: #222;
            cursor: pointer;
        }
    }
</style>

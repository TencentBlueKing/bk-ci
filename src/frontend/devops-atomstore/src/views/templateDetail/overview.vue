<template>
    <div class="template-overview-wrapper">
        <div class="inner-header">
            <div class="title">模板概览</div>
        </div>

        <section
            class="sub-view-port"
            v-bkloading="{
                isLoading: loading.isLoading,
                title: loading.title
            }">
            <div class="template-overview-container" v-if="showContent">
                <div class="building-content">
                    <img :src="image">
                    <p>功能正在建设中···</p>
                </div>
            </div>
        </section>
    </div>
</template>

<script>
    import imgBuilding from '@/images/building.png'

    export default {
        data () {
            return {
                showContent: true,
                image: imgBuilding,
                loading: {
                    isLoading: false,
                    title: ''
                }
            }
        },
        computed: {
            templateCode () {
                return this.$route.params.templateCode
            }
        },
        async mounted () {
            await this.requestTemplate()
        },
        methods: {
            async requestTemplate () {
                try {
                    const res = await this.$store.dispatch('store/requestTemplate', {
                        templateCode: this.templateCode
                    })
                    this.$store.dispatch('store/updateCurrentaTemplate', { res })
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            }
        }
    }
</script>

<style lang="scss">
    @import './../../assets/scss/conf';
    .template-overview-wrapper {
        overflow: auto;
        .inner-header {
            display: flex;
            justify-content: space-between;
            padding: 18px 20px;
            width: 100%;
            height: 60px;
            border-bottom: 1px solid $borderWeightColor;
            background-color: #fff;
            box-shadow:0px 2px 5px 0px rgba(51,60,72,0.03);
            .title {
                font-size: 16px;
            }
        }
        .template-overview-container {
            display: flex;
            height: 100%;
            padding: 20px;
            overflow: auto;
        }
        .building-content {
            margin: 150px auto;
            text-align: center;
        }
    }
</style>

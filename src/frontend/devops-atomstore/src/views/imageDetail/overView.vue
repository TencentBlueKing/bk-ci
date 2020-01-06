<template>
    <div class="image-overview-wrapper">
        <div class="inner-header">
            <div class="title"> {{ $t('store.镜像概览') }} </div>
        </div>

        <section class="sub-view-port">
            <div class="image-overview-container">
                <div class="building-content">
                    <img :src="image">
                    <p> {{ $t('store.功能正在建设中') }}···</p>
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
                image: imgBuilding
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import './../../assets/scss/conf';
    .image-overview-wrapper {
        overflow: auto;
        .inner-header {
            display: flex;
            justify-content: space-between;
            padding: 0 20px;
            width: 100%;
            height: 60px;
            border-bottom: 1px solid $borderWeightColor;
            background-color: #fff;
            box-shadow:0px 2px 5px 0px rgba(51,60,72,0.03);
            .title {
                font-size: 16px;
                line-height: 59px;
            }
        }
        .image-overview-container {
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

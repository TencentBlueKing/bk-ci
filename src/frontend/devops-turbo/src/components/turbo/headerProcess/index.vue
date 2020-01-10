<template>
    <div class="wrapper-header">
        <div class="title">{{ processHead.title }}</div>
        <div class="process-wrapper" v-if="processHead.list.length">
            <div v-for="(item, index) in processHead.list" :key="index"
                :class="['process-section', { 'process-completed': index < processHead.current, 'process-current': index === processHead.current }]">
                <!-- <span class="process-icon" v-if="index < processHead.current">
                    <i class="bk-icon check-circle-shape"></i>
                </span> -->
                <i class="bk-icon icon-check-circle process-state-icon" v-if="index < processHead.current"></i>
                <span v-else class="process-icon">{{ index + 1 }}</span>
                <span class="process-text">{{ item }}</span>
                <i v-if="index < processHead.list.length - 1" class="bk-icon icon-ellipsis process-ellipsis"></i>
            </div>
        </div>
        <div class="head-link" v-if="processHead.hasLink"><a target="_blank" :href="processHead.linkHref">{{ processHead.linkTitle }}</a></div>
    </div>
</template>

<script>
    export default {
        props: {
            processHead: {
                type: Object
            }
        }
    }
</script>

<style lang="scss">
    @import '../../../assets/scss/conf.scss';
    
    .process-wrapper {
        line-height: 32px;
        font-size: 0;
    }
    .process-section {
        display: inline-block;
        height: 32px;
        font-size: 0;
        vertical-align: top;
        .process-icon {
            display: inline-block;
            margin-right: 9px;
            width: 32px;
            height: 32px;
            line-height: 30px;
            border: 1px solid $borderWeightColor;
            border-radius: 16px;
            font-size: 14px;
            text-align: center;
            background: #fff
        }
        .process-text, .process-ellipsis {
            display: inline-block;
            line-height: 32px;
            font-size: 14px;
            vertical-align: top;
        }
        .process-ellipsis {
            margin: 0 18px;
        }
        &.process-completed {
            .process-state-icon {
                margin-right: 9px;
                font-size: 32px;
                color:  #0082ff;
            }
        }
        &.process-current {
            .process-icon {
                border: 1px solid #0082FF;
                color: #fff;
                background: #0082ff
            }
            .process-text {
                color: #0082ff;
            }
        }
    }
</style>

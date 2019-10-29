<template>
    <div :class="progressClass">
        <div class="progress-line-head">
            <div class="progress-line-title" v-if="currentState === 'primary'">正在安装必要软件 ··· {{ progressWidth }}</div>
            <div class="progress-line-title" v-else-if="currentState === 'error'">安装失败 ··· {{ progressWidth }}</div>
            <div class="progress-line-title" v-else>安装成功 </div>
            <div class="progress-line-tips" v-if="currentState !== 'error'">
                剩余时间 {{ restDate }}
            </div>
            <div class="progress-line-tips" v-else>
                <a href="wxwork://message/?username=DevOps">联系蓝盾人工客服</a>
                <a href="javascript: void(0);" @click.stop="resInstall">重新安装</a>
            </div>
        </div>
        <div class="progress">
            <div class="progress-bar" :style="{ width: progressWidth }"></div>
        </div>
        <p class="progress-error-info" v-if="currentState === 'error'">{{ errorMessage }}</p>
    </div>
</template>

<script>
    export default {
        name: 'buildProgress',
        props: {
            currentState: {
                type: String,
                default: 'primary'
            },
            progressWidth: {
                type: String,
                default: '0%'
            },
            restDate: {
                type: String,
                default: ''
            },
            errorMessage: {
                type: String,
                default: 'Message of failed because of Network error'
            }
        },
        computed: {
            progressClass () {
                return 'progress-line-wrapper ' + (this.currentState === 'primary' ? '' : ('progress-' + this.currentState))
            }
        },
        methods: {
            resInstall () {
                this.$emit('resInstall')
            }
        }
    }
</script>

<style lang="scss">
    @import '../../../assets/scss/conf.scss';
    
    .progress-line-wrapper {
        .progress {
            position: relative;
            margin-bottom: 7px;
            height: 10px;
            overflow: hidden;
            border: 1px solid $borderWeightColor;
            border-radius: 5px;
            box-shadow: inset 0 1px 2px rgba(0,0,0,.1);
            background-color: $bgColor;
        }
        .progress-bar {
            position: absolute;
            top: -1px;
            left: 0;
            width: 20%;
            height: 10px;
            border:1px solid #0082FF;
            border-radius:5px;
            background-color: $primaryColor;
            transition: width .6s ease;
        }
        &.progress-error {
            .progress-bar {
                border:1px solid #F72239;
                background-color: #FF5656;
            }
        }
        &.progress-success {
            .progress-bar {
                border:1px solid #00C873;
                background-color: #30D878;
            }
        }
    }
    .progress-line-head {
        display: flex;
        align-items: baseline;
        width: 100%;
        margin-bottom: 20px;
        .progress-line-title {
            flex: 1;
            line-height: 29px;
            font-size: 22px;
            text-align: left;
            color: $fontColorLabel;
        }
        .progress-line-tips {
            & a{
                color: #0082FF;
            }
        }
    }
    .progress-error-info {
        margin: 0;
        width: 100%;
        height: 16px;
        line-height: 16px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        font-size: 12px;
        text-align: left;
        color: #F72239;
    }
    
</style>

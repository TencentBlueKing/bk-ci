<template>
    <div v-if="visible" class="route-tips" :style="!pathTips ? 'height:48px' : ''">
        <div class="tips-icon">
            <i class="bk-icon icon-info-circle-shape" :style="!pathTips ? 'margin: 15px 13px;' : ''"></i>
        </div>
        <div class="tips-content">
            <p v-if="srcTips" class="tips-row" :style="!pathTips ? 'margin-top: 15px' : ''">
                <span class="tips-label">源 ：</span>
                <span class="tips-route" :title="srcTips">{{ srcTips }}</span>
            </p>
            <p v-if="pathTips" class="tips-row" style="margin-top: 0px">
                <span class="tips-label">目标 ：</span>
                <span class="tips-route" :title="pathTips">{{ pathTips }}</span>
            </p>
            <p v-if="keystoreTips" class="tips-row keystore-row" :style="!pathTips ? 'margin-top: 15px' : ''">
                <span class="tips-label" style="width: 78px">证书文件 ：</span>
                <span class="tips-route" :title="keystoreTips">{{ keystoreTips }}</span>
            </p>
            <p v-if="githubAppUrl" class="tips-row" style="margin-top: 15px">
                <span class="tips-route normal-route" style="margin-left: 15px">启用本插件需安装对应
                    <a class="link" :href="githubAppUrl" target="_blank">GitHub APP</a>（Tencent DevOps）
                </span>
            </p>
        </div>
    </div>
</template>

<script>
    import atomFieldMixin from '../atomFieldMixin'

    export default {
        mixins: [atomFieldMixin],
        props: {
            visible: {
                type: Boolean,
                default: false
            },
            srcTips: {
                type: String,
                default: ''
            },
            pathTips: {
                type: String,
                default: ''
            },
            githubAppUrl: {
                type: String,
                default: ''
            },
            keystoreTips: {
                type: String,
                default: ''
            }
        }
    }
</script>

<style lang="scss">
    @import '../../../scss/conf.scss';
    .route-tips {
        height: 60px;
        margin-top: 8px;
        border: 1px solid #e6e6e6;
        .tips-icon {
            float: left;
            width: 44px;
            height: 100%;
            background-color: #ffb400;
            i {
                display: inline-block;
                font-size: 18px;
                color: #fff;
                margin: 21px 13px;
            }
        }
        .tips-content {
            .tips-row {
                margin: 7px;
                .tips-label {
                    display: inline-block;
                    font-weight: bold;
                    font-size: 14px;
                    width: 65px;
                    text-align: right;
                }
                .tips-route {
                    display: inline-block;
                    font-size: 12px;
                    width: 430px;
                    overflow: hidden;
                    text-overflow:ellipsis;
                    white-space: nowrap;
                    vertical-align: middle;
                    padding-bottom: 3px;
                }
                .normal-route {
                    white-space:normal;
                    .link {
                        color: $primaryColor;
                    }
                }
            }
            .keystore-row {
                .tips-label {
                    width: 80px;
                    margin-left: 10px;
                }
                .tips-route {
                    width: 420px;
                }
            }
        }
    }
</style>

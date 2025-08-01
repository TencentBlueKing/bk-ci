<template>
    <div
        v-if="visible"
        class="route-tips"
        :style="!pathTips ? 'height:48px' : ''"
    >
        <div class="tips-icon">
            <i
                class="bk-icon icon-info-circle-shape"
                :style="!pathTips ? 'margin: 15px 13px;' : ''"
            ></i>
        </div>
        <div class="tips-content">
            <p
                v-if="srcTips"
                class="tips-row"
                :style="!pathTips ? 'margin-top: 15px' : ''"
            >
                <span class="tips-label">{{ $t('editPage.source') }} ：</span>
                <span
                    class="tips-route"
                    :title="srcTips"
                >{{ srcTips }}</span>
            </p>
            <p
                v-if="pathTips"
                class="tips-row"
                style="margin-top: 0px"
            >
                <span class="tips-label">{{ $t('editPage.target') }}  ：</span>
                <span
                    class="tips-route"
                    :title="pathTips"
                >{{ pathTips }}</span>
            </p>
            <p
                v-if="keystoreTips"
                class="tips-row keystore-row"
                :style="!pathTips ? 'margin-top: 15px' : ''"
            >
                <span
                    class="tips-label"
                    style="width: 78px"
                >{{ $t('editPage.certFile') }}  ：</span>
                <span
                    class="tips-route"
                    :title="keystoreTips"
                >{{ keystoreTips }}</span>
            </p>
            <p
                v-if="githubAppUrl"
                class="tips-row"
                style="margin-top: 10px"
            >
                <span
                    class="tips-route"
                    style="margin-left: 15px"
                >{{ $t('editPage.githubAppTips') }}
                    <a
                        class="link"
                        :href="githubAppUrl"
                        target="_blank"
                    >GitHub APP</a>（Tencent DevOps）
                </span>
            </p>
            <p
                v-if="tips"
                class="tips-row"
            >
                <span
                    class="tips-route"
                    style="margin-left: 15px"
                    :title="tips"
                >{{ tips }}</span>
            </p>
        </div>
    </div>
</template>

<script>
    import atomFieldMixin from '../atomFieldMixin'

    export default {
        name: 'route-tips',
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
            },
            tips: {
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
                line-height: 26px;
                .tips-label {
                    display: inline-block;
                    font-weight: bold;
                    font-size: 14px;
                    width: 90px;
                    text-align: right;
                }
                .tips-route {
                    display: inline-block;
                    font-size: 12px;
                    width: calc(100% - 148px);
                    overflow: hidden;
                    text-overflow: ellipsis;
                    white-space: nowrap;
                    vertical-align: middle;
                    padding-bottom: 3px;
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

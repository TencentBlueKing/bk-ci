<template>
    <div
        :class="['devops-undeploy', {
            'is-in-popup': isInPopup
        }]">
        <img :src="imgUndeploy">
        <p class="undeploy-title">
            {{serviceName}}
        </p>
        <p class="undeploy-desc">
            {{serviceDesc}}
        </p>
        <footer v-if="!isInPopup">
            <a :href="guideUrl" target="_blank">
                <bk-button theme="primary">
                    {{ $t('learnMore') }}
                </bk-button>
            </a>
            <bk-button @click="back">
                {{ $t('exception.goBack') }}
            </bk-button>
        </footer>
    </div>
</template>

<script lang="ts">
    import imgUndeploy from '@/assets/images/undeploy.svg'
    import Vue from 'vue'
    import { Component, Prop } from 'vue-property-decorator'

    @Component()
    export default class Undeploy extends Vue {
        @Prop({ default: false })
        isInPopup: boolean

        @Prop()
        serviceName: string

        @Prop()
        serviceDesc: string

        @Prop({ default: '' })
        guideUrl: string

        get imgUndeploy (): string {
            return imgUndeploy
        }

        back () {
            this.$router.back()
        }
    }
</script>

<style lang="scss">
    @import '../../assets/scss/conf';
    .devops-undeploy {
        display: flex;
        flex-direction: column;
        align-items: center;
        align-self: center;
        margin: auto;
        min-width: 220px;
        max-width: 420px;
        font-size: 14px;
        color: #63656e;

        &.is-in-popup {
            font-size: 12px;
            >img {
                height:  100px;
            }
            .undeploy-title {
                font-size: 16px;
                line-height: 24px;
            }
        }
        > img {
            height: 200px;
        }
        .undeploy-title {
            margin: 8px 0 16px;
            font-size: 24px;
            line-height: 32px;
            color: #313238;
        }
        .undeploy-desc {
            display: flex;
            flex-wrap: wrap;
            justify-content: center;
            width: 100%;
            margin-bottom: 16px;
            line-height: 22px;
            letter-spacing: 0;
            white-space: wrap;
        }

        > footer {
            margin-top: 24px;
        }
    }
</style>

<template>
    <div
        v-clickoutside="hideOrcode"
        :class="{ 'devops-qrcode': true, 'active': show }"
    >
        <div
            @click="toggleOrcode(show)"
        >
            <div class="flag-box">
                <Icon name="phone" size="20" />
            </div>
        </div>
        <div
            v-if="show"
            class="qrcode-div"
        >
            <div class="content">
                <img
                    width="120"
                    height="120"
                    src="./../../assets/images/devopsapp-qrcode.png"
                >
                <p style="text-align: center">
                    {{ $t('scanToDownload') }}
                </p>
            </div>
        </div>
    </div>
</template>

<script lang="ts">
    import Vue from 'vue'
    import { Component, Watch } from 'vue-property-decorator'
    import { clickoutside } from '../../directives/index'
    import { State, Action } from 'vuex-class'

    @Component({
        directives: {
            clickoutside
        }
    })
    export default class FeedBack extends Vue {
        @State user
        @Action togglePopupShow
        show: boolean = false

        @Watch('show')
        handleShow (show, oldVal) {
            if (show !== oldVal) {
                this.togglePopupShow(show)
            }
        }

        toggleOrcode (): void {
            this.show = !this.show
        }

        hideOrcode (): void {
            this.show = false
        }
    }
</script>

<style lang="scss">
    @import '../../assets/scss/conf';
    @import '../../assets/scss/mixins/triangle';
    .devops-qrcode {
        position: relative;
        height: 100%;
        display: flex;
        align-items: center;
        color: $fontLighterColor;

        > .qrcode-tips {
            margin: 0 10px;
            cursor: pointer;
            line-height: 50px;
            margin-top: 2px;
            vertical-align: middle;

            > div {
                display: inline;
            }

            > .qrcode-icon {
                vertical-align: middle;
                font-size: 20px;
                margin-top: 10px;
            }
        }

        .qrcode-div {
            position: absolute;
            background-color: white;
            border: 1px solid $borderWeightColor;
            border-radius: 2px;
            top: 52px;
            left: 6px;
            width: 140px;
            box-shadow: 0 3px 6px rgba(51, 60, 72, 0.12);
            font-size: 12px;
            color: #7b7d8a;
            > .content {
                margin: 10px;
                > p {
                    text-align: center;
                }
            }
        }
    }
</style>

<template>
    <bk-dialog
        :value="isShow"
        :title="$t('disclosureAgreement')"
        header-position="left"
        :width="720"
        :mask-close="false"
        @cancel="toggleSignDialog(false)"
        @confirm="refresh"
        :ok-text="$t(okText, [sec])"
    >
        <bk-alert
            type="warning"
            class="disclosure-alert"
        >
            <p
                slot="title"
                class="disclosure-title-alert"
                v-html="$t('disclosureTitle', [projectName])"
            />
        </bk-alert>
        <section class="qrcode-box-area">
            <div class="qrcode-box">
                <div
                    :class="['signed-mask', {
                        'signed-mask-show': isShow && signed
                    }]"
                >
                    <i class="bk-icon icon-check-circle-shape"></i>
                    {{ $t('signed') }}
                </div>
                <img
                    v-if="schemeQrcodeUrl"
                    class="qrcode-img"
                    :src="schemeQrcodeUrl"
                />
            </div>
            <span>{{ $t(signed ? 'signed' : 'scanSign') }}</span>
        </section>
        <footer>
            <p class="disclosure-tips-header">
                {{ $t('noticeTitle') }}
            </p>
            <ul class="disclosure-tips">
                <li
                    v-for="(tip, index) in agreementTips"
                    v-html="tip"
                    :key="index"
                />
            </ul>
        </footer>
    </bk-dialog>
</template>

<script>
    import { useStore } from '@/store'
    import { computed, defineComponent, getCurrentInstance, onBeforeUnmount, ref, watch } from 'vue'
    
    export default defineComponent({
        
        setup () {
            const store = useStore()
            const signed = computed(() => store.state.nonDisclosureAgreementConfig?.signed ?? false)
            const projectName = computed(() => store.state.nonDisclosureAgreementConfig?.projectInformation ?? '')
            const schemeQrcodeUrl = computed(() => store.state.nonDisclosureAgreementConfig?.schemeQrcodeUrl ?? '')
            const agreementTips = computed(() => store.state.nonDisclosureAgreementConfig?.agreementTips?.split('\n') ?? [])
            const okText = computed(() => signed.value ? 'countdownFresh' : '刷新')
            const isShow = computed(() => store.state.isShowNonDisclosureAgreement)
            const sec = ref(3)
            const vm = getCurrentInstance()
            let timer = null

            watch(() => signed.value, (val) => {
                // Clear previous timer if exists
                if (timer) {
                    clearInterval(timer)
                    timer = null
                }
                
                if (val) {
                    timer = setInterval(() => {
                        sec.value--
                        if (sec.value <= 0) {
                            clearInterval(timer)
                            timer = null
                            store.dispatch('toggleSignatureDialog', false)
                        }
                    }, 1000)
                } else {
                    sec.value = 3 // Reset counter when not signed
                }
            })

            onBeforeUnmount(() => {
                if (timer) {
                    clearInterval(timer)
                    sec.value = 3
                    timer = null
                }
            })

            return {
                isShow,
                schemeQrcodeUrl,
                signed,
                okText,
                sec,
                projectName,
                agreementTips,
                toggleSignDialog (show) {
                    store.dispatch('toggleSignatureDialog', show)
                    store.state.cancelDisclosureHandler?.()
                },
                refresh () {
                    store.dispatch('fetchSignatureStatus', { projectId: vm.proxy.$route.params.projectId })
                }
            }
        }
    })
</script>

<style lang="scss">
    .disclosure-title-alert {
        font-size: 14px;
        color: #4D4F56;
        font-family: MicrosoftYaHei;
        line-height: 24px;
    }
    .disclosure-alert.bk-alert-warning .icon-info {
        line-height: 24px;
    }
    .qrcode-box-area {
        position: relative;
        display: flex;
        justify-content: center;
        align-items: center;
        padding: 24px 0;
        border: 1px solid #DCDEE5;
        margin: 12px 0  16px 0;
        flex-direction: column;

        .qrcode-box {
            position: relative;
            width: 140px;
            height: 140px;
            padding: 10px;
            border: 1px solid #DCDEE5;
            margin-bottom: 12px;
            .qrcode-img {
                width: 120px;
                height: 120px;
            }
            .signed-mask {
                transition: all 0.5s;
                opacity: 0;
                position: absolute;
                width: 120px;
                height: 120px;
                top: 10px;
                left: 10px;
                background-color: rgba(255, 255, 255, 0.9);
                display: flex;
                align-items: center;
                justify-content: center;
                font-weight: 700;
                font-size: 14px;
                color: #2CAF5E;
                grid-gap: 5px;
                > .bk-icon {
                    color: #299E56;
                }
                &.signed-mask-show {
                    opacity: 1;
                }
            }
        }
    }
    .disclosure-tips-header {
        font-weight: 700;
        font-size: 14px;
        color: #4D4F56;
        line-height: 22px;
    }
    .disclosure-tips {
        color: #4D4F56;
        line-height: 20px;
        font-size: 14px;
        margin: 12px 0 0 14px;
        > li {
            list-style: disc;
            margin-bottom: 16px;
            &:last-child {
                margin-bottom: 0;
            }
            > em {
                font-style: normal;
                color: #F59500;
            }
        }
    }
</style>

<template>
    <div
        class="qrcode-element-div"
        ref="qrcodeWrapper"
    ></div>
</template>

<script>
    import { defineComponent, onMounted, ref } from 'vue'
    export default defineComponent({
        props: {
            url: {
                type: String,
                required: true
            },
            size: {
                type: Number,
                required: false,
                default: 128
            }
        },
        setup (props) {
            const qrcodeWrapper = ref(null)
            onMounted(async () => {
                const { default: QRCode } = await import('../../assets/static/qrcode.min.js')
                console.log(qrcodeWrapper.value)
                const qrcode = new QRCode(qrcodeWrapper.value, {
                    text: props.url,
                    width: props.size,
                    height: props.size
                })
                console.log(qrcode)
            })
            return {
                qrcodeWrapper
            }
        }
    })
</script>

<style lang="scss" scoped>
    .qrcode-element-div {
        padding: 8px 8px 2px 8px;
        border: 1px solid #DCDEE5;
        border-radius: 2px;
    }
</style>


<template>
    <div class="qrcode-content" ref="qrcode"></div>
</template>

<script>
    import QRCode from './qrcode.min'

    export default {
        props: {
            text: {
                type: String,
                required: true
            },
            size: {
                type: Number,
                required: false,
                default: 128
            },
            color: {
                type: String,
                required: false,
                default: '#000'
            },
            bgColor: {
                type: String,
                required: false,
                default: '#FFF'
            }
        },
        data () {
            return {
                qrCode: {}
            }
        },
        watch: {
            text: function (val) {
                this.clear()
                val && this.makeCode(val)
            }
        },
        mounted () {
            this.qrCode = new QRCode(this.$refs.qrcode, {
                text: this.text,
                width: this.size,
                height: this.size,
                colorDark: this.color,
                colorLight: this.bgColor
            })
        },
        methods: {
            clear: function () {
                this.qrCode.clear()
            },
            makeCode: function (text) {
                this.qrCode.makeCode(text)
            }
        }
    }
</script>


<script>
    export default {
        name: 'bread-crumb',
        props: {
            separator: {
                type: String,
                default: '>'
            }
        },
        render (h) {
            return h('div', {
                class: 'bkdevops-bread-crumb'
            }, this.$slots.default)
        }
    }
</script>

<style lang="scss">
    .bkdevops-bread-crumb {
        display: flex;
        align-items: center;
        height: 100%;
        padding: 0 10px;
        font-size: 14px;
        &-separator {
            display: inline-block;
            align-self: center;
            font-weight: bold;
            color: #C3CDD7;
        }
    }
</style>

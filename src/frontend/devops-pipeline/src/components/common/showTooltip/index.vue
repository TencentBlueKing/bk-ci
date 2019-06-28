<template>
    <bk-popover v-if="isShow" ref="showPopover" class="show-tooltip" theme="light" :placement="placement" always="isShow">
        <slot></slot>
        <div slot="content" class="show-tooltip-content">
            {{ content }}
            <div v-if="content" class="show-tooltip-footer">
                <span class="close-tooltip-btn" @click="confirmBtn">{{ footer }}</span>
            </div>
        </div>
    </bk-popover>
    <div v-else>
        <slot></slot>
    </div>
</template>
<script>

    export default {
        name: 'show-tooltip',
        props: {
            name: {
                type: String,
                required: true
            },
            placement: {
                validator (value) {
                    return [
                        'top', 'top-start', 'top-end', 'bottom', 'bottom-start', 'bottom-end',
                        'left', 'left-start', 'left-end', 'right', 'right-start', 'right-end'
                    ].indexOf(value) > -1
                },
                default: 'bottom'
            },
            content: {
                type: [String, Number],
                default: ''
            },
            footer: {
                type: [String, Number],
                default: '知道了'
            },
            width: {
                type: [String, Number],
                default: '230'
            },
            always: {
                type: Boolean,
                default: false
            }
        },
        data () {
            const tooltipEventList = this.getTooltipEventList()
            return {
                isShow: this.name ? !tooltipEventList.includes(this.name) : false
            }
        },
        computed: {
            instance () {
                return this.$refs.showPopover && this.$refs.showPopover.instance && this.$refs.showPopover.instance.instances && this.$refs.showPopover.instance.instances[0]
            }
        },
        beforeDestroy () {
            if (this.instance && typeof this.instance.hide === 'function') {
                this.instance.hide()
            }
        },
        methods: {
            getTooltipEventList () {
                const tooltipEventList = localStorage.getItem('tooltipEventList')
                return tooltipEventList ? JSON.parse(tooltipEventList) : []
            },
            confirmBtn () {
                let tooltipEventList = localStorage.getItem('tooltipEventList')
                tooltipEventList = tooltipEventList ? JSON.parse(tooltipEventList) : []
                tooltipEventList.push(this.name)
                
                localStorage.setItem('tooltipEventList', JSON.stringify(tooltipEventList))
                this.isShow = false
                if (this.instance && typeof this.instance.hide === 'function') {
                    this.instance.hide()
                }
                this.$emit('confirm')
            }
        }
    }
</script>
<style lang="scss">
    .show-tooltip {
        height: 100%;
        display: flex;
        align-items: center;
        .bk-tooltip-ref {
            display: flex;
            align-items: center;
        }
    }
     .show-tooltip-content {
        max-width: 260px;
        font-size: 12px;
    }
    .show-tooltip-footer {
        margin-top: 10px;
        text-align: right;
        color: #3A84FF;
    }
    .close-tooltip-btn {
        cursor: pointer;
    }
</style>

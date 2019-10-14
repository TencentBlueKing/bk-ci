<template>
    <bk-popover
        v-if="isShow"
        ref="showPopover"
        class="show-tooltip"
        theme="light"
        :placement="placement"
        always="isShow"
    >
        <slot />
        <div
            slot="content"
            class="show-tooltip-content"
        >
            {{ content }}
            <div
                v-if="content"
                class="show-tooltip-footer"
            >
                <span
                    class="close-tooltip-btn"
                    @click="confirmBtn"
                >{{ footer }}</span>
            </div>
        </div>
    </bk-popover>
    <div
        v-else
        class="show-tooltip"
    >
        <slot />
    </div>
</template>
<script lang="ts">
    import Vue from 'vue'
    import { Prop, Component } from 'vue-property-decorator'
    import { BkPopover } from 'bk-magic-vue'

    @Component
    export default class ShowTooltip extends Vue {
        @Prop({ required: true })
        name: string
        @Prop({
          default: 'bottom',
          validator (value) {
            return ['top', 'top-start', 'top-end', 'bottom', 'bottom-start', 'bottom-end', 'left', 'left-start', 'left-end', 'right', 'right-start', 'right-end'].indexOf(value) !== -1
          }
        })
        placement: string

        @Prop({ default: '' })
        content: string

        @Prop({ default: '知道了' })
        footer: string

        @Prop({ default: '0' })
        margin: string

        @Prop({ default: 230 })
        width: number

        @Prop({ default: false })
        always: boolean

        isShow: boolean = false

        $refs = {
          showPopover: BkPopover
        }

        get instance () {
          return this.$refs.showPopover && this.$refs.showPopover.instance && this.$refs.showPopover.instance.instances && this.$refs.showPopover.instance.instances[0]
        }

        created () {
          const tooltipEventList = this.getTooltipEventList()
          this.isShow = tooltipEventList.indexOf(this.name) < 0
        }

        beforeDestroy () {
          if (this.instance && typeof this.instance.hide === 'function') {
            this.instance.hide()
          }
        }

        getTooltipEventList () {
          const tooltipEventList = localStorage.getItem('tooltipEventList')
          return tooltipEventList ? JSON.parse(tooltipEventList) : []
        }

        confirmBtn () {
          const tooltipEventListString: string = localStorage.getItem('tooltipEventList')
          const tooltipEventList: string[] = tooltipEventListString ? JSON.parse(tooltipEventListString) : []
          tooltipEventList.push(this.name)

          localStorage.setItem('tooltipEventList', JSON.stringify(tooltipEventList))
          this.isShow = false
          if (this.instance && typeof this.instance.hide === 'function') {
            this.instance.hide()
          }
          this.$emit('confirm')
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
    color: #3a84ff;
}
.close-tooltip-btn {
    cursor: pointer;
}
</style>

<style lang="scss">
.show-tooltip {
    display: inline-block;
    .show-tooltip-rel {
        display: inline-block;
        position: relative;
    }
    .show-tooltip-popper {
        display: block;
        visibility: visible;
        font-size: 12px;
        line-height: 1.5;
        position: absolute;
        z-index: 1060;
        &[x-placement^="top"] {
            padding: 5px 0 8px 0;
            .show-tooltip-arrows {
                margin-left: -4px;
                left: 50%;
                bottom: 5px;
                transform: rotate(225deg);
            }
        }
        &[x-placement^="right"] {
            padding: 0 5px 0 8px;
            .show-tooltip-arrows {
                margin-top: -5px;
                top: 50%;
                left: 5px;
                transform: rotate(315deg);
            }
        }
        &[x-placement^="bottom"] {
            padding: 8px 0 5px 0;
            .show-tooltip-arrows {
                margin-left: -4px;
                top: 5px;
                left: 50%;
            }
        }
        &[x-placement^="left"] {
            padding: 0 8px 0 5px;
            .show-tooltip-arrows {
                margin-top: -5px;
                top: 50%;
                right: 5px;
                transform: rotate(135deg);
            }
        }
        &[x-placement="top-start"] {
            .show-tooltip-arrows {
                left: 10%;
            }
        }
        &[x-placement="top-end"] {
            .show-tooltip-arrows {
                left: 90%;
            }
        }
        &[x-placement="right-start"] {
            .show-tooltip-arrows {
                top: 30%;
            }
        }
        &[x-placement="right-end"] {
            .show-tooltip-arrows {
                top: 70%;
            }
        }
        &[x-placement="left-start"] {
            .show-tooltip-arrows {
                top: 30%;
            }
        }
        &[x-placement="left-end"] {
            .show-tooltip-arrows {
                top: 70%;
            }
        }
        &[x-placement="bottom-start"] {
            .show-tooltip-arrows {
                left: 10%;
            }
        }
        &[x-placement="bottom-end"] {
            .show-tooltip-arrows {
                left: 90%;
            }
        }
    }
    .show-tooltip-inner {
        max-width: 500px;
        min-height: 34px;
        padding: 16px 18px;
        color: #63656e;
        text-align: left;
        text-decoration: none;
        background-color: #fff;
        border-radius: 2px;
        white-space: normal;
        border: 1px solid rgba(220, 222, 229, 1);
        box-shadow: 0px 2px 8px rgba(0, 0, 0, 0.1);
        cursor: default;
    }
    .show-tooltip-footer {
        margin-top: 10px;
        text-align: right;
        color: #3a84ff;
    }
    .close-tooltip-btn {
        cursor: pointer;
    }
    .show-tooltip-arrows {
        padding-top: 4px;
        position: absolute;
        width: 8px;
        height: 8px;
        border: 1px solid #dcdee5;
        border-right-color: transparent;
        border-bottom-color: transparent;
        background-color: #fff;
        transform: rotate(45deg);
    }
}
</style>

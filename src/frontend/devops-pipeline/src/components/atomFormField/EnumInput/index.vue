<template>
    <form class="enum-input-main">
        <bk-radio-group @change="handleSelect" :value="value" :name="name">
            <template v-for="item in list">
                <bk-popover
                    v-if="!item.hidden"
                    :style="`width: calc(${100 / lineNumber}% - 10px)`"
                    :disabled="!item.tips"
                    :key="item.id"
                    :content="item.tips"
                    placement="bottom"
                >
                    <bk-radio
                        :class="['bkdevops-radio', { mr15: lineNumber <= 0 }]"
                        :value="item.value"
                        :disabled="disabled || item.disabled"
                    >
                        <span class="overflow" v-bk-overflow-tips>
                            {{ item.label }}
                        </span>
                    </bk-radio>
                </bk-popover>
            </template>
        </bk-radio-group>
    </form>
</template>

<script>
    import atomFieldMixin from '../atomFieldMixin'
    export default {
        name: 'enum-input',
        mixins: [atomFieldMixin],
        props: {
            list: {
                type: Array,
                default: []
            },
            lineNumber: {
                type: Number,
                default: 0
            },
            value: [Number, Boolean, String]
        },
        computed: {
            enumList () {
                return this.list.map((item) => {
                    let { value, ...restProp } = item
                    if (value === 'true') {
                        value = true
                    } else if (value === 'false') {
                        value = false
                    }
                    return {
                        ...restProp,
                        value
                    }
                })
            }
        },
        methods: {
            handleSelect (value) {
                if (value === 'true') {
                    value = true
                } else if (value === 'false') {
                    value = false
                }
                this.handleChange(this.name, value)
            }
        }
    }
</script>

<style lang="scss" scoped>
.bkdevops-radio {
  width: 100%;
  ::v-deep .bk-radio-text {
    width: calc(100% - 21px);
    height: 20px;
    line-height: 20px;
  }
  .overflow {
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
    width: 100%;
    display: inline-block;
  }
}
.mr15 {
  margin-right: 15px;
}
.bkdevops-running-lock-setting-tab .bkdevops-radio {
  margin-right: 20px;
}
</style>

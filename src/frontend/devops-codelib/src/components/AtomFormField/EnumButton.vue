<template>
    <form class="enum-button-main">
        <div class="bk-button-group">
            <bk-button
                v-for="item in list"
                :key="item.value"
                :class="{
                    'is-selected': item.value === curVal
                }"
                :disabled="disabled"
                @click="updateVal(item.value)"
            >
                {{ item.label }}
            </bk-button>
        </div>
    </form>
</template>

<script>
    import atomFieldMixin from './atomFieldMixin'
    export default {
        name: 'enum-button',
        mixins: [atomFieldMixin],
        props: {
            list: {
                type: Array,
                default: []
            },
            value: String
        },
        data () {
            return {
                curVal: ''
            }
        },
        created () {
            this.curVal = this.value
        },
        methods: {
            updateVal (val) {
                this.curVal = val
                this.handleChange(this.name, val)
            }
        }
    }
</script>

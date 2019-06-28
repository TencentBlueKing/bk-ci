<template>
    <div class="sub-build-params">
        <ul>
            <template v-if="paramList.length">
                <li class="param-item" v-for="param in paramList" :key="param.key">
                    <div><vuex-input :disabled="true" name="key" :value="param.key" /></div>
                    <span>=</span>
                    <div><vuex-input :name="param.key" :value="param.value" :handle-change="handleParamChange" /></div>
                </li>
            </template>
            <li v-else class="param-item-empty"><span>参数为空</span></li>
        </ul>
    </div>
</template>

<script>
    import atomFieldMixin from '../atomFieldMixin'
    import VuexInput from '@/components/atomFormField/VuexInput'

    export default {
        components: {
            VuexInput
        },
        mixins: [atomFieldMixin],
        props: {
            value: {
                type: Object,
                default: []
            }
        },
        data () {
            return {
                paramList: []
            }
        },
        watch: {
            value (val) {
                this.paramList = val
            }
        },
        async created () {
            this.paramList = this.value
        },
        methods: {
            handleParamChange (name, value) {
                const newItem = { key: name, value: value }
                this.paramList.map((item, index) => {
                    if (item.key.toString() === name) {
                        // this.paramList.splice(index, 1, newItem)
                        Object.assign(item, newItem)
                    }
                })
                this.handleChange(this.name, this.paramList)
            }
        }
    }
</script>

<style lang="scss">
    @import '../../../scss/conf.scss';
    .sub-build-params {
        .param-item {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 10px;
            > span {
                margin: 0 10px;
            }
            > div {
                flex: 1;
            }
        }
        .param-item-empty {
            text-align: center;
            color: $fontLigtherColor;
        }
    }
</style>

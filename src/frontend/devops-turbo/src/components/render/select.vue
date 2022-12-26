<template>
    <bk-select
        class="g-turbo-single-width"
        v-bind="{ ...$props, ...paramProps }"
        :value="paramValue[paramKey]"
        :loading="isLoading"
        @change="changeParamValue"
    >
        <bk-option v-for="(param, index) in renderList"
            :key="index + param.paramValue"
            :id="param.paramValue"
            :name="param.paramName">
        </bk-option>
    </bk-select>
</template>

<script>
    import mixin from './mixins.js'
    import { http } from '@/api/index.js'

    export default {
        mixins: [mixin],

        data () {
            return {
                renderList: [],
                queryKey: {},
                isLoading: false
            }
        },

        watch: {
            paramValue: {
                handler (value) {
                    const index = Object.keys(this.queryKey).findIndex((key) => value[key] !== this.queryKey[key])
                    if (index > -1) {
                        this.initRenderList()
                        this.changeParamValue('')
                    }
                },
                deep: true
            }
        },

        mounted () {
            this.$nextTick(() => {
                this.initRenderList()
            })
        },

        methods: {
            initRenderList () {
                if (this.dataType === 'remote') {
                    this.isLoading = true
                    const url = this.handleQueryUrl(this.paramUrl)
                    http
                        .get(url)
                        .then((res) => {
                            this.renderList = res || []
                        })
                        .catch((err) => {
                            console.error(err.message || err)
                        })
                        .finally(() => {
                            this.isLoading = false
                        })
                } else {
                    this.renderList = this.paramEnum
                }
            },

            handleQueryUrl (url) {
                this.queryKey = []
                return url.replace(/\{(.+)\}/g, (all, key) => {
                    const val = this.paramValue[key]
                    this.queryKey[key] = val
                    return val
                })
            }
        }
    }
</script>

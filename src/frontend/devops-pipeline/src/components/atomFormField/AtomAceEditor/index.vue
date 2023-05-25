<template>
    <section style="pointer-events: all; position: relative" v-bkloading="{ isLoading }">
        <div :class="['ace-fullscreen', { 'active': isFullScreen }]" :alt="$t('editPage.isFullScreen')" :title="$t('editPage.isFullScreen')"
            @click="setFullScreen">
            <i class="devops-icon" :class="isFullScreen ? &quot;icon-un-full-screen&quot; : &quot;icon-full-screen&quot;"></i>
        </div>
        <ace
            class="ace-wrapper"
            :read-only="disabled"
            :value="value"
            :lang="lang"
            :name="name"
            :full-screen="isFullScreen"
            @input="handleScriptInput"
            :height="height"
            width="100%">
        </ace>
    </section>
</template>

<script>
    import Ace from '@/components/common/ace-editor'
    import atomFieldMixin from '../atomFieldMixin'
    import { getActualTop } from '@/utils/util'

    export default {
        name: 'atom-ace-editor',
        components: {
            Ace
        },
        mixins: [atomFieldMixin],
        props: {
            lang: {
                type: String,
                default: 'sh'
            },
            default: String,
            bashConf: {
                type: Object,
                default: () => ({})
            },
            defaultHeight: {
                type: Number,
                default: 360
            }
        },
        data () {
            return {
                height: 360,
                isLoading: false,
                isFullScreen: false
            }
        },
        watch: {
            isFullScreen (newVal) {
                const top = getActualTop(this.$el)
                const { clientHeight } = document.body
                if (newVal) {
                    this.height = Math.max(clientHeight - 10, this.defaultHeight)
                } else {
                    this.height = Math.max(clientHeight - top - 180, this.defaultHeight)
                }
            }
        },
        mounted () {
            const top = getActualTop(this.$el)
            const { clientHeight } = document.body
            if (this.defaultHeight !== 360) {
                this.height = this.defaultHeight
            } else {
                this.height = Math.max(clientHeight - top - 180, this.defaultHeight)
            }
            this.getInitValue()
        },
        methods: {
            handleScriptInput (content) {
                this.handleChange(this.name, content)
            },
            setFullScreen () {
                this.isFullScreen = !this.isFullScreen
            },
            getResponseData (response, dataPath = 'data.records') {
                try {
                    switch (true) {
                        case typeof response.data === 'string':
                            return response.data
                        case response.data && response.data.record && typeof response.data.record === 'string':
                            return response.data.record
                        default: {
                            const path = dataPath.split('.')
                            let result = response
                            let pos = 0
                            while (path[pos] && result) {
                                const key = path[pos]
                                result = result[key]
                                pos++
                            }
                            if (pos === path.length && typeof result === 'string') {
                                return result
                            } else {
                                throw Error(this.$t('editPage.failToGetData'))
                            }
                        }
                    }
                } catch (e) {
                    console.error(e)
                    return ''
                }
            },
            async getInitValue () {
                if (!this.bashConf) return
                const { bashConf: { url, dataPath }, default: defalutValue, element, value } = this
                if (url && typeof url === 'string' && defalutValue === value) {
                    const query = this.$route.params
                    const changeUrl = this.urlParse(url, {
                        bkPoolType: this?.container?.dispatchType?.buildType,
                        ...query,
                        ...element
                    })
                    try {
                        this.isLoading = true
                        const res = await this.$ajax.get(changeUrl)
                        const content = this.getResponseData(res, dataPath)
                        this.handleChange(this.name, content)
                    } catch (e) {
                        console.error(e)
                    } finally {
                        this.isLoading = false
                    }
                }
            }
        }
    }
</script>

<style lang="scss">
    .ace-fullscreen {
        top: 10px;
        right: 10px;
        position: absolute;
        z-index: 999;
        color: #fff;
        cursor: pointer;
        &.active {
            position: fixed;
            z-index: 10005;
        }
    }
    .ace_editor:fullscreen {
        height: 100% !important;
        font-size: 14px;
    }
</style>

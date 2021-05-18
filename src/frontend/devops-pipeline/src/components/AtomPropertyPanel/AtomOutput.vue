<template>
    <accordion v-if="outputProps && Object.keys(outputProps).length > 0" show-checkbox show-content>
        <header class="var-header" slot="header">
            <span>插件输出</span>
            <i class="devops-icon icon-angle-down" style="display: block"></i>
        </header>
        <div slot="content">
            <form-field class="output-namespace" :desc="outputNamespaceDesc" label="输出字段命名空间" :is-error="errors.has(&quot;namespace&quot;)" :error-msg="errors.first(&quot;namespace&quot;)">
                <vuex-input name="namespace" v-validate.initial="{ varRule: true }" :handle-change="handleUpdateAtomOutputNameSpace" :value="namespace" placeholder="" />
            </form-field>
            <div class="atom-output-var-list">
                <h4>输出字段列表：</h4>
                <p v-for="(output, key) in outputProps" :key="key">
                    {{ namespace ? `${namespace}_` : '' }}{{ key }}
                    <bk-popover placement="right">
                        <i class="bk-icon icon-info-circle" />
                        <div slot="content">
                            {{ output.description }}
                        </div>
                    </bk-popover>
                    <copy-icon :value="`\${${namespace ? `${namespace}_${key}` : key}}`"></copy-icon>
                </p>
            </div>
        </div>
    </accordion>
</template>

<script>
    import atomMixin from './atomMixin'
    import validMixins from '../validMixins'
    import copyIcon from '@/components/copyIcon'

    export default {
        name: 'atom-output',
        components: {
            copyIcon
        },
        mixins: [atomMixin, validMixins],
        computed: {
            outputNamespaceDesc () {
                return `用于解决流水线下，相同插件有多个实例时，输出字段使用冲突的问题。\n当没有冲突时，无需添加命名空间。\n当修改了命名空间后，后续使用到对应字段的地方也需要同步修改`
            },
            outputProps () {
                try {
                    return this.atomPropsModel.output
                } catch (e) {
                    console.warn('getAtomModalOpt error', e)
                    return null
                }
            },
            namespace () {
                try {
                    const ns = this.element.data.namespace || ''
                    return ns.trim().replace(/(.+)?\_$/, '$1')
                } catch (e) {
                    console.warn('getAtomOutput namespace error', e)
                    return ''
                }
            }
        },
        created () {
            console.log('create output', this.atomPropsModel.output, this.element)
        }
    }
</script>

<style lang="scss">
    .output-namespace {
        margin-bottom: 12px;
    }
    .atom-output-var-list {
        > h4,
        > p {
            margin: 0;
        }
        > p {
            line-height: 36px;
        }
    }
</style>

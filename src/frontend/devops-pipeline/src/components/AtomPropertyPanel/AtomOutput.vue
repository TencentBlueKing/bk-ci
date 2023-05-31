<template>
    <accordion v-if="outputProps && Object.keys(outputProps).length > 0" show-checkbox show-content>
        <header class="var-header" slot="header">
            <span>{{ $t('editPage.atomOutput') }}</span>
            <i class="devops-icon icon-angle-down" style="display: block"></i>
        </header>
        <div slot="content">
            <div :class="{ 'output-namespace': true, 'form-field': true, 'is-danger': errors.has('namespace') }">
                <label class="bk-label" style="line-height: 32px;">
                    {{ $t('editPage.outputNamespace') }}：
                    <bk-popover placement="top">
                        <i class="bk-icon icon-info-circle"></i>
                        <div slot="content" style="white-space: pre-wrap; font-size: 12px; max-width: 500px;">
                            <div>{{ outputNamespaceDesc }}</div>
                        </div>
                    </bk-popover>
                    <i class="bk-icon icon-edit edit-namespace" @click="editNamespace"></i>
                </label>
                <div class="bk-form-content output-namespace-tips">
                    <bk-alert type="warning" :closable="true" style="margin-bottom: 8px;">
                        <template slot="title">
                            {{ namespaceTips }}
                            <a
                                class="primary-link"
                                target="_blank"
                                :href="variableNamespaceDocs"
                            >
                                {{ $t('context') }}
                            </a>
                        </template>
                    </bk-alert>
                    <vuex-input v-if="showEditNamespace" name="namespace" v-validate.initial="{ varRule: true }" :handle-change="handleUpdateAtomOutputNameSpace" :value="namespace" />
                    <p v-if="errors.has('namespace')" class="bk-form-help is-danger">{{errors.first('namespace')}}</p>
                </div>
            </div>
            <div class="atom-output-var-list">
                <h4>{{ $t('editPage.outputItemList') }}</h4>
                <p v-for="(output, key) in outputProps" :key="key">
                    {{ namespace ? `${namespace}_` : '' }}{{ key }}
                    <bk-popover placement="right">
                        <i class="bk-icon icon-info-circle" />
                        <div slot="content">
                            {{ output.description }}
                        </div>
                    </bk-popover>
                    <copy-icon :value="bkVarWrapper(namespace ? `${namespace}_${key}` : key)"></copy-icon>
                </p>
            </div>
        </div>
    </accordion>
</template>

<script>
    import copyIcon from '@/components/copyIcon'
    import validMixins from '../validMixins'
    import atomMixin from './atomMixin'
    export default {
        name: 'atom-output',
        components: {
            copyIcon
        },
        mixins: [atomMixin, validMixins],
        data () {
            return {
                showEditNamespace: false,
                namespaceTips: this.$t('namespaceTips'),
                outputNamespaceDesc: this.$t('outputNameSpaceDescTips'),
                variableNamespaceDocs: this.$pipelineDocs.NAMESPACE_DOC
            }
        },
        computed: {
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
        },
        methods: {
            editNamespace () {
                this.showEditNamespace = true
            }
        }
    }
</script>

<style lang="scss">
    .output-namespace {
        margin-bottom: 12px;
    }
    .edit-namespace {
        cursor: pointer;
        margin-left: 2px;
        font-size: 12px;
    }
    .output-namespace-tips,
    .atom-output-var-list {
        pointer-events: auto;
    }
    .atom-output-var-list {
        pointer-events: auto;
        > h4,
        > p {
            margin: 0;
        }
        > p {
            line-height: 36px;
        }
    }
</style>

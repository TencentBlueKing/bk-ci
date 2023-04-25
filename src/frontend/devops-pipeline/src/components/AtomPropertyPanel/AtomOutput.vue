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
                            <div>{{ $t("editPage.namespaceTips") }}</div>
                        </div>
                    </bk-popover>
                    <i class="bk-icon icon-edit edit-namespace" @click="editNamespace"></i>
                </label>
                <div class="bk-form-content">
                    <bk-alert type="warning" :closable="true" style="margin-bottom: 8px;">
                        <div slot="title"><span v-html="namespaceTips"></span></div>
                    </bk-alert>
                    <vuex-input v-if="showEditNamespace" name="namespace" v-validate.initial="{ varRule: true }" :handle-change="handleUpdateAtomOutputNameSpace" :value="namespace" />
                    <p v-if="errors.has('namespace')" class="bk-form-help is-danger">{{errors.first('namespace')}}</p>
                </div>
            </div>
            <div class="atom-output-var-list">
                <h4>{{ $t('editPage.outputItemList') }}：</h4>
                <p v-for="(output, key) in outputProps" :key="key">
                    {{ namespace ? `${namespace}_` : '' }}{{ key }}
                    <bk-popover placement="right">
                        <i class="devops-icon icon-info-circle" />
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
    import atomMixin from './atomMixin'
    import validMixins from '../validMixins'
    import copyIcon from '@/components/copyIcon'
    export default {
        name: 'atom-output',
        components: {
            copyIcon
        },
        mixins: [atomMixin, validMixins],
        data () {
            return {
                showEditNamespace: false
            }
        },
        computed: {
            namespaceTips () {
                return `即将下线，请使用Step ID来设置插件字段的命名空间，通过<a style="color: #3c96ff" target="_blank" href="${IWIKI_DOCS_URL}/pages/viewpage.action?pageId=691153587#Stream%E8%A1%A8%E8%BE%BE%E5%BC%8F%E3%80%81%E4%B8%8A%E4%B8%8B%E6%96%87%E3%80%81%E5%87%BD%E6%95%B0-jobs">上下文</a>方式访问`
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

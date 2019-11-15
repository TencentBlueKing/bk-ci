<template>
    <div class="u3d-build-panel bk-form bk-form-vertical">
        <form-field v-for="(obj, key) in commonModel[&quot;row&quot;]" :key="key" :desc="obj.desc" :required="obj.required" :label="obj.label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
            <component :is="obj.component" v-validate.initial="Object.assign({}, obj.rule, { required: !!obj.required })" :name="key" :handle-change="handleUpdateElement" :value="element[key]" v-bind="obj"></component>
        </form-field>
        <accordion v-for="(prop, index) in getPlatformList" :key="index" show-checkbox :show-content="checkPlateform(prop.id)" :after-toggle="togglePlatform">
            <header class="var-header" slot="header">
                <span>{{ prop.name }}</span>
                <input class="accordion-checkbox" type="checkbox" :name="'platform'" :checked="checkPlateform(prop.id)" :value="prop.id" />
            </header>
            <div slot="content" class="bk-form bk-form-vertical">
                <form-field v-for="key of beloneKeys[prop.id]" :key="key" :desc="atomPropsModel[key].desc" :required="atomPropsModel[key].required" :label="atomPropsModel[key].label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
                    <component :is="atomPropsModel[key].component" v-validate.initial="Object.assign({}, atomPropsModel[key].rule, { required: !!atomPropsModel[key].required })" :name="key" :handle-change="handleUpdateElement" :value="element[key]" v-bind="atomPropsModel[key]"></component>
                </form-field>
            </div>
        </accordion>
    </div>
</template>

<script>
    import { mapGetters } from 'vuex'
    import atomMixin from './atomMixin'
    import validMixins from '../validMixins'
    import { deepCopy } from '@/utils/util'

    export default {
        name: 'u3d-build-code',
        mixins: [atomMixin, validMixins],
        computed: {
            ...mapGetters('atom', [
                'getPlatformList'
            ]),
            commonModel () {
                const { debug, rootDir, executeMethod } = this.atomPropsModel
                return {
                    'row': { debug, rootDir, executeMethod }
                }
            },
            beloneKeys () {
                return {
                    'ANDROID': [
                        'certId',
                        'apkPath',
                        'apkName'
                    ],
                    'IPHONE': [
                        'enableBitCode',
                        'xcodeProjectName'
                    ]
                }
            }
        },
        methods: {
            checkPlateform (key) {
                return this.element.platform && this.element.platform.includes(key)
            },
            updateProps (newParam) {
                this.updateAtom({
                    element: this.element,
                    newParam
                })
            },
            togglePlatform (element, show) {
                const value = element.querySelector('.accordion-checkbox').getAttribute('value')
                let platform = deepCopy(this.element.platform)

                if (show) {
                    platform.push(value)
                } else {
                    platform = platform.filter(item => {
                        return item !== value
                    })
                }
                this.handleUpdateElement('platform', platform)
            },
            filter (array1, array2) {
                return array1.filter(i => {
                    return array2.indexOf(i) < 0
                })
            },
            updatePlatform (platform) {
                const { element, atomPropsModel } = this
                const newParam = { platform }
                let key = ''
                const len = element.platform.length - platform.length || 0

                if (len > 0) {
                    const arr = this.filter(element.platform, platform)
                    arr && (key = arr[0])
                    key && this.beloneKeys[key].map(propKey => {
                        this.deletePropKey({
                            element,
                            propKey
                        })
                    })
                } else {
                    const arr = this.filter(platform, element.platform)
                    arr && (key = arr[0])
                    key && this.beloneKeys[key].reduce((params, propKey) => {
                        params[propKey] = atomPropsModel[propKey].default
                        return params
                    }, newParam)
                }
                this.updateProps(newParam)
            },
            handleUpdateElement (name, value) {
                if (name === 'platform') {
                    this.updatePlatform(value)
                } else {
                    this.updateProps({
                        [name]: value
                    })
                }
            }
        }
    }
</script>

/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

import DefineParam from '@/components/AtomFormComponent/DefineParam'
import DevopsSelect from '@/components/AtomFormComponent/DevopsSelect'
import SelectInput from '@/components/AtomFormComponent/SelectInput'
import NotifyType from '@/components/AtomFormComponent/notifyType'
import Accordion from '@/components/atomFormField/Accordion'
import AtomAceEditor from '@/components/atomFormField/AtomAceEditor'
import AtomCheckbox from '@/components/atomFormField/AtomCheckbox'
import AtomCheckboxList from '@/components/atomFormField/AtomCheckboxList'
import AtomDatePicker from '@/components/atomFormField/AtomDatePicker'
import AtomMarkdown from '@/components/atomFormField/AtomMarkdown'
import AutoComplete from '@/components/atomFormField/AutoComplete'
import CodeModeInput from '@/components/atomFormField/CodeModeInput'
import CodeModeSelector from '@/components/atomFormField/CodeModeSelector'
import CronTimer from '@/components/atomFormField/CronTimer/week'
import EnumInput from '@/components/atomFormField/EnumInput'
import KeyValue from '@/components/atomFormField/KeyValue'
import KeyValueNormal from '@/components/atomFormField/KeyValueNormal'
import NameSpaceVar from '@/components/atomFormField/NameSpaceVar'
import ParamsView from '@/components/atomFormField/ParamsView'
import QualitygateTips from '@/components/atomFormField/QualitygateTips'
import RemoteCurlUrl from '@/components/atomFormField/RemoteCurlUrl'
import RequestSelector from '@/components/atomFormField/RequestSelector'
import RouteTips from '@/components/atomFormField/RouteTips'
import Selector from '@/components/atomFormField/Selector'
import SvnpathInput from '@/components/atomFormField/SvnpathInput'
import UserInput from '@/components/atomFormField/UserInput'
import VuexInput from '@/components/atomFormField/VuexInput'
import VuexTextarea from '@/components/atomFormField/VuexTextarea'
import GroupIdSelector from '@/components/atomFormField/groupIdSelector'
import { mapActions, mapGetters, mapState } from 'vuex'
import { bkVarWrapper, rely, urlJoin } from '../../utils/util'
import FormField from './FormField'

const atomMixin = {
    props: {
        elementIndex: Number,
        containerIndex: Number,
        stageIndex: Number,
        element: Object,
        container: Object,
        stage: Object,
        atomPropsModel: Object,
        setAtomValidate: Function,
        atomValue: Object,
        disabled: Boolean
    },
    components: {
        Accordion,
        RemoteCurlUrl,
        VuexInput,
        VuexTextarea,
        EnumInput,
        Selector,
        SelectInput,
        AtomAceEditor,
        CronTimer,
        UserInput,
        RequestSelector,
        AtomCheckbox,
        AtomCheckboxList,
        FormField,
        AtomDatePicker,
        CodeModeSelector,
        CodeModeInput,
        ParamsView,
        SvnpathInput,
        KeyValue,
        KeyValueNormal,
        DefineParam,
        NotifyType,
        NameSpaceVar,
        RouteTips,
        GroupIdSelector,
        QualitygateTips,
        AutoComplete,
        DevopsSelect,
        AtomMarkdown
    },
    computed: {
        ...mapGetters('atom', [
            'isThirdPartyContainer',
            'atomVersionChangedKeys'
        ]),
        ...mapState('atom', [
            'pipelineCommonSetting'
        ]),
        isThirdParty () {
            return this.isThirdPartyContainer(this.container)
        },
        atomInputLimit () {
            try {
                return this.pipelineCommonSetting.stageCommonSetting.jobCommonSetting.taskCommonSetting.inputComponentCommonSettings || []
            } catch (error) {
                return []
            }
        }
    },
    methods: {
        ...mapActions('atom', [
            'updateAtomInput',
            'updateWholeAtomInput',
            'updateAtomOutput',
            'updateAtomOutputNameSpace',
            'updateAtom',
            'deleteAtomProps'
        ]),
        bkVarWrapper,
        deletePropKey (propKey) {
            this.deleteAtomProps({
                element: this.element,
                propKey
            })
        },
        handleUpdateElement (name, value) {
            this.updateAtom({
                element: this.element,
                newParam: {
                    [name]: value
                }
            })
        },
        handleUpdateAtomInput (name, value) {
            this.updateAtomInput({
                atom: this.element,
                newParam: {
                    [name]: value
                }
            })
        },
        handleUpdateWholeAtomInput (newInput) {
            this.updateWholeAtomInput({
                atom: this.element,
                newInput
            })
        },
        handleUpdateAtomOutput (name, value) {
            this.updateAtomOutput({
                atom: this.element,
                newParam: {
                    [name]: value
                }
            })
        },
        handleUpdateAtomOutputNameSpace (name, value) {
            this.updateAtomOutputNameSpace({
                atom: this.element,
                [name]: value
            })
        },
        handlePath (path = '', getFileName = false) {
            if (path.startsWith('./')) {
                path = path.slice(2)
            }
            if (path.endsWith('/')) {
                path = path.substring(0, path.length - 1)
            }
            if (getFileName && path) {
                const index = path.lastIndexOf('/')
                path = path.substring(index + 1, path.length) // 文件名
            }
            return path
        },
        isHidden (obj, element) {
            try {
                if (typeof obj.isHidden === 'function') {
                    return obj.isHidden(element)
                }
                const isHidden = eval(`(${obj.isHidden})`) // eslint-disable-line

                if (typeof isHidden === 'function') {
                    return isHidden(element)
                }

                if (typeof obj.hidden === 'boolean') {
                    return obj.hidden
                }

                return typeof isHidden === 'boolean' ? isHidden : false
            } catch (error) {
                console.log(element, obj, error)
                return false
            }
        },
        getPlaceholder (obj, element) {
            if (typeof obj.getPlaceholder === 'function') {
                return obj.getPlaceholder(element)
            } else if (typeof obj.getPlaceholder === 'string') {
                const getPlaceholder = eval(`(${obj.getPlaceholder})`)  // eslint-disable-line
                if (typeof getPlaceholder === 'function') {
                    return getPlaceholder(element)
                }

                return ''
            }
            return obj.placeholder || ''
        },
        getComponentTips (obj, element) {
            const tips = typeof obj.tips === 'string' ? eval(`(${obj.tips})`) : obj.tips // eslint-disable-line

            if (typeof tips === 'function') {
                return tips(element, urlJoin, this.handlePath)
            }

            return {
                visible: false
            }
        },
        rely (obj, element) {
            return rely(obj, element)
        },
        /**
         * 获取每种类型最大长度限制
         */
        getMaxLengthByType (type) {
            const defaultLength = 1024
            const componentItem = this.atomInputLimit.find(item => item.componentType === type) || {}
            return componentItem.maxSize || defaultLength
        }
    }
}

export default atomMixin

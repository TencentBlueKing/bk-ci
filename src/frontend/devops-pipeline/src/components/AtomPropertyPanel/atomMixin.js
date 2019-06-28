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

import { mapActions, mapGetters } from 'vuex'
import Accordion from '@/components/atomFormField/Accordion'
import EnumInput from '@/components/atomFormField/EnumInput'
import VuexInput from '@/components/atomFormField/VuexInput'
import ExperienceInput from '@/components/atomFormField/ExperienceInput'
import VuexTextarea from '@/components/atomFormField/VuexTextarea'
import Selector from '@/components/atomFormField/Selector'
import AtomAceEditor from '@/components/atomFormField/AtomAceEditor'
import CronTimer from '@/components/atomFormField/CronTimer/week'
import StaffInput from '@/components/atomFormField/StaffInput'
import UserInput from '@/components/atomFormField/UserInput'
import RequestSelector from '@/components/atomFormField/RequestSelector'
import GitRequestSelector from '@/components/atomFormField/GitRequestSelector'
import AtomCheckbox from '@/components/atomFormField/AtomCheckbox'
import AtomCheckboxList from '@/components/atomFormField/AtomCheckboxList'
import AtomDatePicker from '@/components/atomFormField/AtomDatePicker'
import CodeModeSelector from '@/components/atomFormField/CodeModeSelector'
import CodeModeInput from '@/components/atomFormField/CodeModeInput'
import ParamsView from '@/components/atomFormField/ParamsView'
import SvnpathInput from '@/components/atomFormField/SvnpathInput'
import KeyValue from '@/components/atomFormField/KeyValue'
import KeyValueNormal from '@/components/atomFormField/KeyValueNormal'
import NameSpaceVar from '@/components/atomFormField/NameSpaceVar'
import RouteTips from '@/components/atomFormField/RouteTips'
import FormField from './FormField'
import GroupIdSelector from '@/components/atomFormField/groupIdSelector'
import RemoteCurlUrl from '@/components/atomFormField/RemoteCurlUrl'
import AutoComplete from '@/components/atomFormField/AutoComplete'
import { urlJoin } from '../../utils/util'

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
        AtomAceEditor,
        CronTimer,
        StaffInput,
        UserInput,
        RequestSelector,
        GitRequestSelector,
        AtomCheckbox,
        AtomCheckboxList,
        FormField,
        AtomDatePicker,
        CodeModeSelector,
        CodeModeInput,
        ExperienceInput,
        ParamsView,
        SvnpathInput,
        KeyValue,
        KeyValueNormal,
        NameSpaceVar,
        RouteTips,
        GroupIdSelector,
        AutoComplete
    },
    computed: {
        ...mapGetters('atom', [
            'isThirdPartyContainer'
        ]),
        isThirdParty () {
            return this.isThirdPartyContainer(this.container)
        }
    },
    methods: {
        ...mapActions('atom', [
            'updateAtomInput',
            'updateAtomOutput',
            'updateAtomOutputNameSpace',
            'updateAtom',
            'deleteAtomProps'
        ]),
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
            const isHidden = eval(`(${obj.isHidden})`) // eslint-disable-line

            if (typeof isHidden === 'function') {
                return isHidden(element)
            }

            if (typeof obj.hidden === 'boolean') {
                return obj.hidden
            }

            return typeof isHidden === 'boolean' ? isHidden : false
        },
        getPlaceholder (obj, element) {
            if (obj.getPlaceholder) {
                const getPlaceholder = eval(`(${obj.getPlaceholder})`)  // eslint-disable-line
                if (typeof getPlaceholder === 'function') {
                    return getPlaceholder(element)
                }

                return ''
            }
            return obj.placeholder || ''
        },
        getComponentTips (obj, element) {
            const tips = eval(`(${obj.tips})`) // eslint-disable-line
            if (typeof tips === 'function') {
                return tips(element, urlJoin, this.handlePath)
            }

            return {
                visible: false
            }
        },
        rely (obj, element) {
            try {
                const { rely: { expression = [], operation = 'AND' } } = obj
                const cb = item => {
                    const { key, value } = item
                    if (Array.isArray(value)) {
                        return typeof element[key] !== 'undefined' && value.includes(element[key])
                    } else {
                        return element[key] === value
                    }
                }
                switch (operation) {
                    case 'AND':
                        return expression.every(cb)
                    case 'OR':
                        return expression.length > 0 ? expression.some(cb) : true
                    default:
                        return true
                }
            } catch (e) {
                return true
            }
        }
    }
}

export default atomMixin

import AtomCheckbox from '../AtomFormField/AtomCheckbox'
import EnumInput from '../AtomFormField/EnumInput'
import FormField from '../AtomFormField/FormField'
import Selector from '../AtomFormField/Selector'
import VuexInput from '../AtomFormField/VuexInput'
import RequestSelector from '../AtomFormField/RequestSelector'
import StaffInput from '../AtomFormField/StaffInput'
import Accordion from '../AtomFormField/Accordion'
import Tips from '../AtomFormField/Tips'
import TipsSimple from '../AtomFormField/TipsSimple'
import FormFieldGroup from '../AtomFormField/FormFieldGroup'
import EnumButton from '../AtomFormField/EnumButton'
import ConditionalInputSelector from '../AtomFormField/ConditionalInputSelector'
import CompositeInput from '../AtomFormField/CompositeInput'
import { rely } from './utils.js'

const atomMixin = {
    components: {
        FormField,
        VuexInput,
        EnumInput,
        Selector,
        RequestSelector,
        AtomCheckbox,
        StaffInput,
        Accordion,
        Tips,
        TipsSimple,
        FormFieldGroup,
        EnumButton,
        ConditionalInputSelector,
        CompositeInput
    },
    methods: {
        rely (obj, element) {
            return rely(obj, element)
        }
    }
}

export default atomMixin

import AtomCheckbox from '../AtomFormField/AtomCheckbox'
import EnumInput from '../AtomFormField/EnumInput'
import FormField from '../AtomFormField/FormField'
import Selector from '../AtomFormField/Selector'
import VuexInput from '../AtomFormField/VuexInput'
import RequestSelector from '../AtomFormField/RequestSelector'
import StaffInput from '../AtomFormField/StaffInput'
import Accordion from '../AtomFormField/Accordion'
import Tips from '../AtomFormField/Tips'
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
        Tips
    },
    methods: {
        rely (obj, element) {
            return rely(obj, element)
        }
    }
}

export default atomMixin

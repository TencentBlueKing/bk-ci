import { format } from 'date-fns'

export default {
    methods: {
        formatTime (date, token, options = {}) {
            return format(Number(date), token, options)
        }
    }
}

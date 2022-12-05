import { pipelines } from '@/http'

export default {
    checkYaml: {
        validator (yml) {
            return new Promise((resolve, reject) => {
                return pipelines.checkYaml(yml).then(() => {
                    this.message = ''
                    resolve(true)
                }).catch((err) => {
                    this.message = err.message || err
                    resolve(false)
                })
            })
        },
        message: '',
        trigger: 'blur'
    }
}

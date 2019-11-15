/**
 * @file development env
 * @author blueking
 */

import merge from 'webpack-merge'
import prodEnv from './prod.env'

const NODE_ENV = JSON.stringify(process.env.NODE_ENV)

export default merge(prodEnv, {
    'process.env': {
        'NODE_ENV': NODE_ENV
    },
    staticUrl: '/static',
    NODE_ENV: NODE_ENV
})

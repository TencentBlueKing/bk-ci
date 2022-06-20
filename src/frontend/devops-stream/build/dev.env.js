/**
 * @file development env
 * @author Blueking
 */

import merge from 'webpack-merge'
import prodEnv from './prod.env'

const NODE_ENV = JSON.stringify('development')

export default merge(prodEnv, {
    'process.env': {
        NODE_ENV: NODE_ENV
    },
    NODE_ENV: NODE_ENV
})

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
    NODE_ENV: NODE_ENV,
    LOGIN_URL: JSON.stringify(''),
    AJAX_URL_PREFIX: JSON.stringify('https://dev.stream.woa.com/'),
    WEBSOCKET_URL_PREFIX: JSON.stringify('https://dev.stream.woa.com/'),
    AJAX_MOCK_PARAM: JSON.stringify(''),
    USER_INFO_URL: JSON.stringify('')
})

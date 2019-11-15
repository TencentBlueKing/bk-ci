/**
 * @file util
 * @author blueking
 */

import os from 'os'
import path from 'path'

export function resolve (dir) {
    return path.join(__dirname, '..', dir)
}

export function assetsPath (_path) {
    const assetsSubDirectory = 'static'
    return path.posix.join(assetsSubDirectory, _path)
}

export function getIP () {
    const ifaces = os.networkInterfaces()
    const defultAddress = '127.0.0.1'
    let ip = defultAddress

    /* eslint-disable fecs-use-for-of, no-loop-func */
    for (const dev in ifaces) {
        if (ifaces.hasOwnProperty(dev)) {
            /* jshint loopfunc: true */
            ifaces[dev].forEach(details => {
                if (ip === defultAddress && details.family === 'IPv4') {
                    ip = details.address
                }
            })
        }
    }
    /* eslint-enable fecs-use-for-of, no-loop-func */
    return ip
}

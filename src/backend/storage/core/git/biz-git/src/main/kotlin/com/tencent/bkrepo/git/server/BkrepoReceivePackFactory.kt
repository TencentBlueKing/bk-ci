package com.tencent.bkrepo.git.server

import com.tencent.bkrepo.common.api.constant.ANONYMOUS_USER
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContext
import org.eclipse.jgit.lib.Config
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.transport.ReceivePack
import org.eclipse.jgit.transport.resolver.ServiceNotAuthorizedException
import org.eclipse.jgit.transport.resolver.ServiceNotEnabledException
import javax.servlet.http.HttpServletRequest

class BkrepoReceivePackFactory {

    private class ServiceConfig internal constructor(cfg: Config) {
        val set: Boolean = cfg.getString("http", null, "receivepack") != null
        val enabled: Boolean = cfg.getBoolean("http", "receivepack", false)
    }

    fun create(req: HttpServletRequest, db: Repository, artifactContext: ArtifactContext): ReceivePack {
        val cfg: ServiceConfig = db.config[
            { cfg: Config? -> ServiceConfig(cfg!!) }
        ]
        var user = artifactContext.userId
        if (cfg.set) {
            if (cfg.enabled) {
                if (user.isNullOrEmpty()) user = ANONYMOUS_USER
                return createFor(req, db, user)
            }
            throw ServiceNotEnabledException()
        }
        if (user.isNotEmpty()) return createFor(req, db, user)
        throw ServiceNotAuthorizedException()
    }

    private fun createFor(
        req: HttpServletRequest,
        db: Repository,
        user: String
    ): ReceivePack {
        val rp = ReceivePack(db)
        rp.refLogIdent = toPersonIdent(req, user)
        return rp
    }

    private fun toPersonIdent(req: HttpServletRequest, user: String): PersonIdent? {
        return PersonIdent(user, user + "@" + req.remoteHost)
    }
}

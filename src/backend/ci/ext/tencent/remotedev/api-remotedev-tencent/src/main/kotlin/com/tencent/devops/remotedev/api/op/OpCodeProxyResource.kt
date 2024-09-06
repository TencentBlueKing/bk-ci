package com.tencent.devops.remotedev.api.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.remotedev.pojo.gitproxy.CallbackLinktgitData
import com.tencent.devops.remotedev.pojo.gitproxy.DeleteTGitLinkData
import com.tencent.devops.remotedev.pojo.gitproxy.UpdateTgitAclIpData
import com.tencent.devops.remotedev.pojo.gitproxy.UpdateTgitAclUserData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Tag(name = "OP_CODE_PROY", description = "OP_CODE_PROY")
@Path("/op/codeproxy")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpCodeProxyResource {

    @Operation(summary = "回调关联工蜂库")
    @POST
    @Path("/callback/tgitlink")
    fun tgitlink(
        data: CallbackLinktgitData
    ): Result<Map<Long, Boolean>>

    @Operation(summary = "添加或者删除工蜂ACLIP")
    @POST
    @Path("/updateTgitAclIp")
    fun updateTgitAclIp(
        data: UpdateTgitAclIpData
    )

    @Operation(summary = "刷新工蜂ACL用户")
    @POST
    @Path("/updateTgitAclUser")
    fun updateTgitAclUser(
        data: UpdateTgitAclUserData
    )

    @Operation(summary = "删除工蜂链接")
    @DELETE
    @Path("/delete_tgit_link")
    fun deleteTGitLink(
        data: DeleteTGitLinkData
    ): Result<Boolean>
}

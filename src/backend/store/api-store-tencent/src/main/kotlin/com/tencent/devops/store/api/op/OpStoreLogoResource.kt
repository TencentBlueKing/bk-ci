package com.tencent.devops.store.api.op

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.Logo
import com.tencent.devops.store.pojo.StoreLogoReq
import com.tencent.devops.store.pojo.enums.LogoTypeEnum
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.glassfish.jersey.media.multipart.FormDataParam
import java.io.InputStream
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["OP_STORE_LOGO"], description = "store-logo")
@Path("/op/store/logo")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpStoreLogoResource {

    @ApiOperation("上传logo")
    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun uploadStoreLogo(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("logo", required = true)
        @FormDataParam("logo")
        inputStream: InputStream,
        @FormDataParam("logo")
        disposition: FormDataContentDisposition
    ): Result<String?>

    @ApiOperation("新增一条logo记录")
    @POST
    @Path("/type/{logoType}")
    fun add(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("logoType", required = true)
        @PathParam("logoType")
        logoType: LogoTypeEnum,
        @ApiParam("storeLogoReq", required = true)
        storeLogoReq: StoreLogoReq
    ): Result<Boolean>

    @ApiOperation("更新一条logo记录")
    @PUT
    @Path("/{id}")
    fun update(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("id", required = true)
        @PathParam("id")
        id: String,
        @ApiParam("storeLogoReq", required = true)
        storeLogoReq: StoreLogoReq
    ): Result<Boolean>

    @ApiOperation("获取一条logo记录")
    @GET
    @Path("/{id}")
    fun get(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("id", required = true)
        @PathParam("id")
        id: String
    ): Result<Logo?>

    @ApiOperation("list logo记录")
    @GET
    @Path("/type/{logoType}")
    fun list(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("logoType", required = true)
        @PathParam("logoType")
        logoType: LogoTypeEnum
    ): Result<List<Logo>?>

    @ApiOperation("删除一条logo记录")
    @DELETE
    @Path("/{id}")
    fun delete(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("id", required = true)
        @PathParam("id")
        id: String
    ): Result<Boolean>
}
package com.tencent.devops.plugin.api

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.plugin.pojo.stke.ConfigMapData
import com.tencent.devops.plugin.pojo.stke.StkeType
import com.tencent.devops.plugin.pojo.stke.StkeUpdateParam
import io.swagger.annotations.ApiParam
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.GET
import javax.ws.rs.PathParam
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["BUILD_STKE"], description = "构建--STKE插件相关")
@Path("/build/stke")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildStkeResource {

    @ApiOperation("更新Stke")
    @POST
    @Path("/{stkeType}/update")
    fun updateDeployment(
        @ApiParam("更新类型", required = true)
        @PathParam("stkeType")
        stkeType: StkeType,
        @ApiParam("集群名称", required = true)
        @QueryParam("clusterName")
        clusterName: String,
        @ApiParam("命名空间", required = true)
        @QueryParam("namespace")
        namespace: String,
        @ApiParam("实例名称", required = true)
        @QueryParam("appsName")
        appsName: String,
        @ApiParam("更新参数", required = true)
        updateParam: StkeUpdateParam
    ): Result<String>

    @ApiOperation("获取Pods状态")
    @GET
    @Path("/{stkeType}/pods/status")
    fun getPodsStatus(
        @ApiParam("更新类型", required = true)
        @PathParam("stkeType")
        stkeType: StkeType,
        @ApiParam("集群名称", required = true)
        @QueryParam("clusterName")
        clusterName: String,
        @ApiParam("命名空间", required = true)
        @QueryParam("namespace")
        namespace: String,
        @ApiParam("实例名称", required = true)
        @QueryParam("appsName")
        appsName: String
    ): Result<String>

    @ApiOperation("获取workload配置")
    @GET
    @Path("/{stkeType}/workload")
    fun getWorkload(
        @ApiParam("更新类型", required = true)
        @PathParam("stkeType")
        stkeType: StkeType,
        @ApiParam("集群名称", required = true)
        @QueryParam("clusterName")
        clusterName: String,
        @ApiParam("命名空间", required = true)
        @QueryParam("namespace")
        namespace: String,
        @ApiParam("实例名称", required = true)
        @QueryParam("appsName")
        appsName: String
    ): Result<String>

    @ApiOperation("获取业务负责人")
    @GET
    @Path("/managers")
    fun getManagers(
        @ApiParam("项目ID", required = true)
        @QueryParam("projectId")
        projectId: String
    ): Result<String>

    @ApiOperation("更新configMap")
    @POST
    @Path("/configMap/update")
    fun updateConfigMap(
        @ApiParam("集群名称", required = true)
        @QueryParam("clusterName")
        clusterName: String,
        @ApiParam("命名空间", required = true)
        @QueryParam("namespace")
        namespace: String,
        @ApiParam("configMap名称，带版本号", required = true)
        @QueryParam("configMapName")
        configMapName: String,
        @ApiParam("更新数据")
        configMapData: List<ConfigMapData>
    ): Result<String>

    @ApiOperation("更新workLoad")
    @POST
    @Path("/{stkeType}/workload/update")
    fun updateWorkLoad(
        @ApiParam("更新类型", required = true)
        @PathParam("stkeType")
        stkeType: StkeType,
        @ApiParam("集群名称", required = true)
        @QueryParam("clusterName")
        clusterName: String,
        @ApiParam("命名空间", required = true)
        @QueryParam("namespace")
        namespace: String,
        @ApiParam("实例名称", required = true)
        @QueryParam("appsName")
        appsName: String,
        @ApiParam("操作者", required = true)
        @QueryParam("operator")
        operator: String,
        @ApiParam("构建ID", required = true)
        @QueryParam("buildId")
        buildId: String
    ): Result<String>
}
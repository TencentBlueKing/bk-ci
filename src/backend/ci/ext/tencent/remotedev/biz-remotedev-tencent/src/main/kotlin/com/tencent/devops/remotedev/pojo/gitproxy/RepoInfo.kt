package com.tencent.devops.remotedev.pojo.gitproxy

/**
 * BkRepo 仓库信息
 * @param projectId    string 	项目id
 * @param name    string 	仓库名称
 * @param type    string 	仓库类型
 * @param category    string 	仓库类别
 * @param public    boolean 	是否公开项目
 * @param description    string 	仓库描述
 * @param configuration RepoConfig 仓库配置
 * @param createdBy    string 	创建者
 * @param createdDate    string 	创建时间
 * @param lastModifiedBy    string 	上次修改者
 * @param lastModifiedDate    string 	上次修改时间
 * @param quota    long 	仓库配额，单位字节，值为nul时表示未设置仓库配额
 * @param used    long 	仓库已使用容量，单位字节
 */
data class RepoInfo(
    val projectId: String,
    val name: String,
    val type: String,
    val category: String,
    val public: Boolean,
    val description: String,
    val configuration: RepoConfig,
    val createdBy: String,
    val createdDate: String,
    val lastModifiedBy: String,
    val lastModifiedDate: String,
    val quota: Int,
    val used: Int
)

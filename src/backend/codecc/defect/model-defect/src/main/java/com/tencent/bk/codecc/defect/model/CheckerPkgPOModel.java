package com.tencent.bk.codecc.defect.model;

/**
 * 规则包的持久化对象
 * 
 * @date 2017/10/11
 * @version V2.4
 */
public class CheckerPkgPOModel 
{
    private String pkgId;

    private String pkgName;

    private String pkgDesc;

    public String getPkgId()
    {
        return pkgId;
    }

    public void setPkgId(String pkgId)
    {
        this.pkgId = pkgId;
    }

    public String getPkgName()
    {
        return pkgName;
    }

    public void setPkgName(String pkgName)
    {
        this.pkgName = pkgName;
    }

    public String getPkgDesc()
    {
        return pkgDesc;
    }

    public void setPkgDesc(String pkgDesc)
    {
        this.pkgDesc = pkgDesc;
    }
}

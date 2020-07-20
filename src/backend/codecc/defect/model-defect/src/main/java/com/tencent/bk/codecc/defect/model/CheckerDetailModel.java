package com.tencent.bk.codecc.defect.model;

/**
 * 规则详情模型
 * 
 * @date 2017/11/20
 * @version V2.5
 */
public class CheckerDetailModel
{
    /**
     * 告警类型key，唯一标识，如：qoc_lua_UseVarIfNil
     */
    private String checkerKey;

    /**
     * 规则名称
     */
    private String checkerName;

    /**
     * 规则详细描述
     */
    private String checkerDesc;
    
    /**
     * 规则严重程度，1=>严重，2=>一般，3=>提示
     */
    private int severity;

    /**
     * 规则所属语言（针对KLOCKWORK）
     */
    private int language;
    
    /**
     * 规则状态 2=>打开 1=>关闭;
     */
    private int status;

    /**
     * 规则类型
     */
    private String checkerType;

    /**
     * 规则类型说明
     */
    private String checkerTypeDesc;
    
    /**
     * 规则类型排序序列号
     */
    private String checkerTypeSort;
    
    /**
     * 所属规则包
     */
    private String pkgKind;
    
    /**
     * 项目框架（针对Eslint工具,目前有vue,react,standard）
     */
    private String frameworkType;
    
    private String checkerMapped;

    /**
     * 规则配置
     */
    private String props;

    /**
     * 规则所属标准
     */
    private int standard;

    /**
     * 规则是否支持配置true：支持;空或false：不支持
     */
    private String editAble;

    /**
     * 示例代码
     */
//    private String codeExample;


    public String getEditAble()
    {
        return editAble;
    }

    public void setEditAble(String editAble)
    {
        this.editAble = editAble;
    }
    
    public int getStandard()
    {
        return standard;
    }

    public void setStandard(int standard)
    {
        this.standard = standard;
    }

    public String getProps()
    {
        return props;
    }

    public void setProps(String props)
    {
        this.props = props;
    }

    public String getCheckerMapped()
    {
        return checkerMapped;
    }

    public void setCheckerMapped(String checkerMapped)
    {
        this.checkerMapped = checkerMapped;
    }

    public String getCheckerKey()
    {
        return checkerKey;
    }

    public void setCheckerKey(String checkerKey)
    {
        this.checkerKey = checkerKey;
    }

    public String getCheckerName()
    {
        return checkerName;
    }

    public void setCheckerName(String checkerName)
    {
        this.checkerName = checkerName;
    }

    public String getCheckerDesc()
    {
        return checkerDesc;
    }

    public void setCheckerDesc(String checkerDesc)
    {
        this.checkerDesc = checkerDesc;
    }

    public int getSeverity()
    {
        return severity;
    }

    public void setSeverity(int severity)
    {
        this.severity = severity;
    }

    public int getLanguage()
	{
		return language;
	}

	public void setLanguage(int language)
	{
		this.language = language;
	}

	public int getStatus()
    {
        return status;
    }

    public void setStatus(int status)
    {
        this.status = status;
    }

    public String getCheckerType()
    {
        return checkerType;
    }

    public void setCheckerType(String checkerType)
    {
        this.checkerType = checkerType;
    }

    public String getCheckerTypeDesc()
    {
        return checkerTypeDesc;
    }

    public void setCheckerTypeDesc(String checkerTypeDesc)
    {
        this.checkerTypeDesc = checkerTypeDesc;
    }

	public String getCheckerTypeSort()
	{
		return checkerTypeSort;
	}

	public void setCheckerTypeSort(String checkerTypeSort)
	{
		this.checkerTypeSort = checkerTypeSort;
	}

	public String getPkgKind()
	{
		return pkgKind;
	}

	public void setPkgKind(String pkgKind)
	{
		this.pkgKind = pkgKind;
	}

	public String getFrameworkType()
	{
		return frameworkType;
	}

	public void setFrameworkType(String frameworkType)
	{
		this.frameworkType = frameworkType;
	}


//    public String getCodeExample()
//    {
//        return codeExample;
//    }
//
//    public void setCodeExample(String codeExample)
//    {
//        this.codeExample = codeExample;
//    }
}

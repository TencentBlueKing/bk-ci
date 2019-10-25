package com.omg.ons;

public class NameSvcApi
{

	static
	{
		System.loadLibrary("javanamesvc");
	}
	

	public static native NameEnt getHostByKey(String key);
	public static native NameEnt getHostByKeyEx(String key, String route_key);
	public static native DictEnt getValueByKey(String key);
}


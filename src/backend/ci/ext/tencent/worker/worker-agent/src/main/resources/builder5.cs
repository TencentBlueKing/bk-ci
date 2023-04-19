using UnityEngine;
using UnityEditor;
using System.Collections.Generic;
using UnityEditor.Callbacks;
using System.Collections;
using UnityEditor.iOS.Xcode;
using System.IO;

public class SODABuild
{
    private static bool ms_isDebugBuild = false;
    private static BuildTarget ms_buildTarget = BuildTarget.Android;

    private static string XCODE_PROJECT_NAME = "${xcodeProjectName}";
    private static string ANDROID_APK_PATH = "${androidAPKPath}";
    private static string ANDROID_APK_NAME = "${androidAPKName}";
    private static string ANDROID_KEY_STORE_NAME = "${androidKeyStoreName}";
    private static string ANDROID_KEY_STORE_PASS = "${androidKeyStorePass}";
    private static string ANDROID_KEY_ALIAS_NAME = "${androidKeyAliasName}";
    private static string ANDROID_KEY_ALIAS_PASS = "${androidKeyAliasPass}";
    private static string ENABLE_BITCODE = "${enableBitCode}";

    [PostProcessBuild]
    public static void OnPostprocessBuild(BuildTarget buildTarget, string path)
    {

        Debug.Log("On Post process build - " + ENABLE_BITCODE);

        if (ENABLE_BITCODE.Equals("false")) {
            Debug.Log("Disable bitcode");
            if (buildTarget == BuildTarget.iOS)
            {
                string projPath = path + "/Unity-iPhone.xcodeproj/project.pbxproj";

                PBXProject proj = new PBXProject();
                proj.ReadFromString(File.ReadAllText(projPath));

                string target = proj.TargetGuidByName("Unity-iPhone");

                proj.SetBuildProperty(target, "ENABLE_BITCODE", "NO");

                File.WriteAllText(projPath, proj.WriteToString());
            }
        }
    }

    private static string[] GetBuildScenes()
    {
        List<string> names = new List<string>();
        foreach (EditorBuildSettingsScene e in EditorBuildSettings.scenes)
        {
            if (e == null)
            {
                continue;
            }
            if (e.enabled)
            {
                names.Add(e.path);
            }
        }
        return names.ToArray();
    }

    private static void UpdateBuildFlag()
    {
        string[] args = System.Environment.GetCommandLineArgs();
        foreach (string oneArg in args)
        {
            if (oneArg != null && oneArg.Length > 0)
            {
                if (oneArg.ToLower().Contains("-debug"))
                {
                    Debug.Log("\"-debug\" is detected, switch to debug build.");
                    ms_isDebugBuild = true;
                    return;
                }
                else if (oneArg.ToLower().Contains("-release"))
                {
                    Debug.Log("\"-release\" is detected, switch to release build.");
                    ms_isDebugBuild = false;
                    return;
                }
            }
        }

        if (ms_isDebugBuild)
        {
            Debug.Log("neither \"-debug\" nor \"-release\" is detected, current is to debug build.");
        }
        else
        {
            Debug.Log("neither \"-debug\" nor \"-release\" is detected, current is to release build.");
        }
    }
    private static void UpdateBuildTarget()
    {
        string[] args = System.Environment.GetCommandLineArgs();
        foreach (string oneArg in args)
        {
            if (oneArg != null && oneArg.Length > 0)
            {
                if (oneArg.ToLower().Contains("-android"))
                {
                    Debug.Log("\"-android\" is detected, switch build target to android.");
                    ms_buildTarget = BuildTarget.Android;
                    return;
                }
                else if (oneArg.ToLower().Contains("-iphone"))
                {
                    Debug.Log("\"-iphone\" is detected, switch build target to iphone.");
                    ms_buildTarget = BuildTarget.iOS;
                    return;
                }
            }
        }
        EditorUserBuildSettings.enableHeadlessMode = true;
        Debug.Log("neither \"-android\", \"-iphone\" is detected, current build target is: " + ms_buildTarget);
    }

    public static void PreBuild()
    {
        UpdateBuildFlag();
    }

    public static void Build()
    {
        UpdateBuildTarget();

        BuildOptions buildOption = BuildOptions.None;
        if (ms_isDebugBuild)
        {
            buildOption |= BuildOptions.Development;
            buildOption |= BuildOptions.AllowDebugging;
            buildOption |= BuildOptions.ConnectWithProfiler;
        }
        else
        {
            buildOption |= BuildOptions.None;
        }

        string locationPathName;
        if (BuildTarget.iOS == ms_buildTarget)
        {
            locationPathName = XCODE_PROJECT_NAME;
        }
        else
        {
            locationPathName = ANDROID_APK_PATH;
            locationPathName += ANDROID_APK_NAME;

            if (!ms_isDebugBuild)
            {
                PlayerSettings.Android.keystorePass = ANDROID_KEY_STORE_PASS;
                PlayerSettings.Android.keystoreName = ANDROID_KEY_STORE_NAME;
                PlayerSettings.Android.keyaliasName = ANDROID_KEY_ALIAS_NAME;
                PlayerSettings.Android.keyaliasPass = ANDROID_KEY_ALIAS_PASS;
            }
        }
        BuildPipeline.BuildPlayer(GetBuildScenes(), locationPathName, ms_buildTarget, buildOption);
    }
}
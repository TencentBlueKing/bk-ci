#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
BK-CI Windows Session Mouse & Keyboard Test (Python Version)
测试 Windows session 模式下 Python 操作鼠标键盘的能力
"""

import sys
import time
import ctypes
import ctypes.wintypes

# ── 统一结构体定义 (必须放在最前面，SendInput 对 cbSize 极其敏感) ──
class MOUSEINPUT(ctypes.Structure):
    _fields_ = [("dx", ctypes.c_long),
                ("dy", ctypes.c_long),
                ("mouseData", ctypes.c_ulong),
                ("dwFlags", ctypes.c_ulong),
                ("time", ctypes.c_ulong),
                ("dwExtraInfo", ctypes.c_void_p)]

class KEYBDINPUT(ctypes.Structure):
    _fields_ = [("wVk", ctypes.c_ushort),
                ("wScan", ctypes.c_ushort),
                ("dwFlags", ctypes.c_ulong),
                ("time", ctypes.c_ulong),
                ("dwExtraInfo", ctypes.c_void_p)]

class HARDWAREINPUT(ctypes.Structure):
    _fields_ = [("uMsg", ctypes.c_ulong),
                ("wParamL", ctypes.c_ushort),
                ("wParamH", ctypes.c_ushort)]

class INPUT_UNION(ctypes.Union):
    _fields_ = [("mi", MOUSEINPUT),
                ("ki", KEYBDINPUT),
                ("hi", HARDWAREINPUT)]

class INPUT(ctypes.Structure):
    _fields_ = [("type", ctypes.c_ulong),
                ("u", INPUT_UNION)]

# POINT 也在文件头部定义，方便复用
class POINT(ctypes.Structure):
    _fields_ = [("x", ctypes.c_long), ("y", ctypes.c_long)]

# SendInput 函数原型 (避免 ctypes 默认调用约定导致 cbSize 解释错误)
user32 = ctypes.windll.user32
user32.SendInput.argtypes = [ctypes.c_uint, ctypes.POINTER(INPUT), ctypes.c_int]
user32.SendInput.restype = ctypes.c_uint

failed = False

def print_header(title):
    print("")
    print("=" * 60)
    print(f"  {title}")
    print("=" * 60)

def print_pass(msg):
    print(f"  [PASS] {msg}")

def print_fail(msg):
    global failed
    print(f"  [FAIL] {msg}")
    failed = True

def print_warn(msg):
    print(f"  [WARN] {msg}")

# ── 1. 鼠标移动测试 ──
print_header("1. Mouse Movement Test")
try:
    user32.SetProcessDPIAware()
    
    # 获取当前鼠标位置
    pt = POINT()
    if not user32.GetCursorPos(ctypes.byref(pt)):
        print_fail("GetCursorPos failed")
    else:
        original_x, original_y = pt.x, pt.y
        print(f"  Original position: ({original_x}, {original_y})")
        
        # 移动鼠标
        target_x = original_x + 50
        target_y = original_y + 50
        if not user32.SetCursorPos(target_x, target_y):
            print_fail("SetCursorPos failed")
        else:
            time.sleep(0.1)
            user32.GetCursorPos(ctypes.byref(pt))
            print(f"  Moved to: ({pt.x}, {pt.y})")
            
            # 验证移动是否成功
            if abs(pt.x - target_x) <= 5 and abs(pt.y - target_y) <= 5:
                print_pass("Mouse movement successful")
            else:
                print_fail(f"Position mismatch: expected ({target_x}, {target_y}), got ({pt.x}, {pt.y})")
            
            # 恢复鼠标位置
            user32.SetCursorPos(original_x, original_y)
            print(f"  Position restored to: ({original_x}, {original_y})")
except Exception as e:
    print_fail(f"Mouse movement test failed: {e}")

# ── 2. 鼠标点击测试 (SendInput) ──
print_header("2. Mouse Click Test (SendInput)")
try:
    INPUT_MOUSE = 0
    MOUSEEVENTF_LEFTDOWN = 0x0002
    MOUSEEVENTF_LEFTUP = 0x0004

    # 左键按下
    inp = INPUT()
    inp.type = INPUT_MOUSE
    inp.u.mi.dx = 0
    inp.u.mi.dy = 0
    inp.u.mi.mouseData = 0
    inp.u.mi.dwFlags = MOUSEEVENTF_LEFTDOWN
    inp.u.mi.time = 0
    inp.u.mi.dwExtraInfo = None

    result = user32.SendInput(1, ctypes.byref(inp), ctypes.sizeof(INPUT))
    if result != 1:
        print_fail(f"SendInput (LEFTDOWN) failed, return={result}, error={ctypes.GetLastError()}")
    else:
        # 左键释放
        inp.u.mi.dwFlags = MOUSEEVENTF_LEFTUP
        result = user32.SendInput(1, ctypes.byref(inp), ctypes.sizeof(INPUT))
        if result != 1:
            print_fail(f"SendInput (LEFTUP) failed, return={result}, error={ctypes.GetLastError()}")
        else:
            print_pass("Mouse click simulation successful")
except Exception as e:
    print_fail(f"Mouse click test failed: {e}")

# ── 3. 键盘按键测试 (SendInput) ──
print_header("3. Keyboard Input Test (SendInput)")
try:
    INPUT_KEYBOARD = 1
    KEYEVENTF_KEYUP = 0x0002
    VK_A = 0x41  # A 键的虚拟键码

    # 按下 A 键
    inp = INPUT()
    inp.type = INPUT_KEYBOARD
    inp.u.ki.wVk = VK_A
    inp.u.ki.wScan = 0
    inp.u.ki.dwFlags = 0
    inp.u.ki.time = 0
    inp.u.ki.dwExtraInfo = None

    result = user32.SendInput(1, ctypes.byref(inp), ctypes.sizeof(INPUT))
    if result != 1:
        print_fail(f"SendInput (KeyDown) failed, return={result}, error={ctypes.GetLastError()}")
    else:
        # 释放 A 键
        inp.u.ki.dwFlags = KEYEVENTF_KEYUP
        result = user32.SendInput(1, ctypes.byref(inp), ctypes.sizeof(INPUT))
        if result != 1:
            print_fail(f"SendInput (KeyUp) failed, return={result}, error={ctypes.GetLastError()}")
        else:
            print_pass("Keyboard input simulation successful")
except Exception as e:
    print_fail(f"Keyboard input test failed: {e}")

# ── 4. PyAutoGUI 测试 (如果可用) ──
print_header("4. PyAutoGUI Test (if available)")
try:
    import pyautogui
    print("  PyAutoGUI imported successfully")
    
    # 测试获取鼠标位置
    x, y = pyautogui.position()
    print(f"  Current position: ({x}, {y})")
    
    # 测试鼠标移动
    pyautogui.moveTo(x + 10, y + 10, duration=0.1)
    new_x, new_y = pyautogui.position()
    print(f"  Moved to: ({new_x}, {new_y})")
    
    # 恢复位置
    pyautogui.moveTo(x, y, duration=0.1)
    print_pass("PyAutoGUI mouse operation successful")
    
except ImportError:
    print_warn("pyautogui not installed, skipping")
except Exception as e:
    print_fail(f"PyAutoGUI test failed: {e}")

# ── 5. PyWin32 测试 (如果可用) ──
print_header("5. PyWin32 Test (if available)")
try:
    import win32api
    print("  PyWin32 imported successfully")
    
    # 测试获取鼠标位置
    x, y = win32api.GetCursorPos()
    print(f"  Current position: ({x}, {y})")
    
    # 测试鼠标移动
    win32api.SetCursorPos((x + 10, y + 10))
    new_x, new_y = win32api.GetCursorPos()
    print(f"  Moved to: ({new_x}, {new_y})")
    
    # 恢复位置
    win32api.SetCursorPos((x, y))
    print_pass("PyWin32 mouse operation successful")
    
except ImportError:
    print_warn("pywin32 not installed, skipping")
except Exception as e:
    print_fail(f"PyWin32 test failed: {e}")

# ── 汇总 ──
print("")
print("=" * 60)
if failed:
    print("  RESULT: FAILED - Mouse/keyboard operations not working")
    print("=" * 60)
    sys.exit(1)
else:
    print("  RESULT: ALL PASSED - Mouse/keyboard operations working")
    print("=" * 60)
    sys.exit(0)

mate [version 1]
# ========== 全命令测试脚本 ==========
# 覆盖所有 @AutoCmdRegister 命令的 key=val 格式示例
# 部分命令需运行环境，仅作解析/格式验证

# --- 无参数命令 ---
actionBack
actionHome
actionOpenNotifications
actionRecent
actionScreenLock
actionScreenShot
actionUnlock
actionWakeUp
playAudio
listInstalledApps

# --- 坐标点击 ---
click x=0.5 y=0.5 random=0
press x=0.5 y=0.5 duration=500
repeatTap x=0.5 y=0.5 random=0 count=5 gap=100
patternTap type=0 gap=300 hrz=30 ver=30 random=0

# --- 图像/文字/颜色/控件 ---
clickImage action=click accuracy=0.8 images=img.png random=0 fastCount=1 fastGap=200 pressDuration=500
clickText text=搜索 action=click findType=contains direction=0 random=0 fastCount=1 fastGap=200 pressDuration=500 ocrType=1
clickView id=- text=确定 tag=- action=click direction=0 random=0 fastCount=1 fastGap=200 pressDuration=500
clickColor action=click color=-16777216 threshold=10 findType=block random=0 fastCount=1 fastGap=200 pressDuration=500

# --- 滑动缩放 ---
scroll points="0.5,0.5,0.5,0.2" times="500,500"
pinchZoom action=out duration=500 from="0.2,0.5" to="0.8,0.5" center="0.5,0.5"
pinch fingers=2 gap=50 duration=1000 from="0.2,0.5" to="0.8,0.5"

# --- 流程控制 ---
delay start=500 end=1000
alignToSecond seconds=60
jumpPoint id=1
jump target=1

# --- 变量与输入 ---
set main.param0=${sys.clipboard}
set main.param1="hello"
input content=test target=- targetIndex=1 action=full anim=false
copyToClipboard content=clipboard
log content=log
toast text=tip

# --- 屏幕与摄像头 ---
captureScreen output=main.param0
captureCamera camera=0 output=main.param0
readScreenText output=main.param0
readScreenLayout output=main.param0
readViewText type=TEXT output=main.param0 target=- direction=0 scope=SINGLE

# --- 网络与 AI ---
curl url=https://api.com method=GET output=main.param0
download url=https://example.com/f path=/sdcard/ output=main.param0 gallery=false
aiRequest output=main.param0 prompt=sum failure=fail
python code="print(1)" output=main.param1

# --- 应用与脚本 ---
openApp package=com.example class=- name=Example action=reopen
openUrl url=https://example.com
callScript path=child name=child
requestPermission permission=android.permission.CAMERA

# --- 对话框 ---
waitForUser title=Wait message=Ready confirmBtn=OK cancelBtn=Cancel countDown=10
dialog title=Confirm message=Sure confirmBtn=OK cancelBtn=Cancel countDown=-1
dialog title=WithImage message=See below image=https://example.com/a.png confirmBtn=OK cancelBtn=Cancel
dialogUserInput title=Input inputs=Name|Age hints=Enter defaults=
dialogUserSelector title=Select items=A|B|C multiSelect=false

# --- 块结构：for ---
for count=2:
	click x=0.3 y=0.3 random=0
	delay start=100 end=200
end

# --- 块结构：if ---
scriptStart params=main.param0 title=Sub inputs=Arg
if checkParam(main.param0):
	click x=0.5 y=0.5 random=0
end
scriptEnd main.param0=result

# --- 流程结束（可选）---
# break
# exit
# 注：scrollMultiple 仍为旧格式，未包含

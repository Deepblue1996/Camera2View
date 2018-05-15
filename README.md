# Camera2View


```
<zou.dahua.cameralib.CameraView
        android:id="@+id/cameraView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"/>
```
照相机画布显示
```
cameraView.show(this);
```
**1.在AndroidManifest.xml申明所需要的权限：**

（注：请确保进入Camera2的时候已经拥有这三项权限了，Android6.0需要动态去申请权限）
```
   <uses-permission android:name="android.permission.CAMERA" />
   <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
   <uses-permission android:name="android.permission.RECORD_AUDIO" />
```

How to

To get a Git project into your build:

Step 1. Add the JitPack repository to your build file

gradle
maven
sbt
leiningen
Add it in your root build.gradle at the end of repositories:
```
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}
```
Step 2. Add the dependency
```
dependencies {
	implementation 'com.github.Deepblue1996:Camera2View:0.2.0'
}
```

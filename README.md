# CmoreDLFFaceDetection
CmoreDLFFaceDetection
人臉辨識

第一步驟環境設定
在自己專案 build:gradle(app)中加入    
  
~~~
dependencies { implementation 'com.github.cmorehub:CmoreDLFFaceDetection:c256ae2261' }  
~~~
在自己專案 build:gradle中加入  
~~~
buildscript {  
  repositories {  
    maven { url "https://oss.sonatype.org/content/repositories/snapshots" }  
  }  
}  
  
allprojects {  
  repositories {  
    maven { url 'https://jitpack.io' }  
    maven { url "https://dl.bintray.com/qualeams/Android-Face-Recognition-Deep-Learning-Library" }  
  }  
}  
~~~

第二步驟呼叫人臉辨識功能頁面  
~~~
startActivity(new Intent(this, com.example.rueychi.tensorflowface.activities.MainActivity.class));
~~~

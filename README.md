README: [English](https://github.com/YummyLau/ComponentPlugin/blob/master/README.md) | [中文](https://github.com/YummyLau/ComponentPlugin/blob/master/README-zh.md)

### Plugin original intention

> With the advent of android 9.0 and new versions, the system has become more stable and closed, and the road to plug-in has become more difficult. Development has gradually shifted towards modular development. The reason for regression componentization is because any module function is stably split or combined into components. The concept of components in android engineering is not obvious. I have tried many solutions in the industry, and each has its own merits, but there is no plug-in that supports code isolation, supports independent debugging, and does not change any existing code.

From the very beginning, I learned that "WeChat's Modular Architecture Reconstruction Practice" began to focus on componentization, and benefited from contact with the US-based take-out/51 credit card/cat eye. I understand the same componentization, the project must meet the "module independence of different functional granularity", the business must meet the "functional independence", and the development must meet the "dependency isolation, interface-oriented programming". This is why my solution abandoned the use of routing schemes to forward apis. The wheel is more inclined to solve the "convenient debugging / full code isolation", and it is also convenient to support the binding and unbinding of the module api.

### new version update
* 2019/10/10 1.0.3-beta
	* Support component circular dependency
	* Support for concurrent transform to speed up compilation
* 2019/10/22 1.0.4
	* Adjust the gradle plugin version to 3.1
	* Optimized debug log format
	* Solve the problem of compile compilation failure under multi-task task
* 2019/11/04 1.0.5
	* Support sub-project pin in the module
	* Provide a more user-friendly debug log format
* 2019/12/09
	* Optimization plugin gradle incremental compilation
* 2020/05/28
	* Adjust the sdk injection logic, optimize the sdk registration, to avoid the repeated registration of multiple modules in extreme cases, resulting in the loss of the bound implementation
* 2022/11/16
     * Support one-way dependency between sdk, see demo for details
     * Optimize the compilation cache of components and optimize the compilation speed
     * Support publishing sdk and impl to maven
     * Support component automatic switching maven dependencies, local file dependencies and source code compilation (depending on git to provide basic capabilities)
     * Support automatic version management of components (depending on git to provide basic capabilities)
     * Support automatic injection from maven product, local compilation cache and source code compilation when compiling and injecting impl
     - Temporarily cancel the component debug ability
     - Temporarily cancel the pin project support capability

### Why use it

**tip：** *The word module below refers to the native module created by android Studio. The term component means the module module processed by the plugin.*

#### Compare the advantages of plugins

* **Complete code isolation (marking)**

	Using "interface-oriented" programming, abolishing hard-coded programming exposes apis (such as routing), relies on the SDK in the build/sync process, and injects IMPL into the assemable process.
* **Support circular dependencies（marking）**

	This is very important! For modules, because of the inability to circular dependencies, the respective exposed content needs to sink to the next module, and the component solves this problem by separating the SDK/IMPL.
* **Convenient integrated debugging**

	Debug based on module dependencies, without modifying the native plugin for dynamically modifying the library. (For example, the debug module relies on component A for component A functional testing), and supports multiple directory debugging for multiple components/android libraries/custom configurations.
* **Very low access costs**

	In the root project to declare the plugin and add configuration scripts, the plugin will be automatically injected into each subproject according to the configuration and complete sdk packaging.

#### Suggestions for Android engineering structure

<img src="./doc/component_build_0.png"  alt="component_all" align=center />

* **Library layer** 

	The basic class library, which stores the streamlined code, is highly reusable, and can be directly referenced by other modules, such as Utils, BaseActivity, etc.
* **Service layer** 

	Independent modules that support a certain type of basic business functions, such as login services, skinning services, between the library layer and the component layer, can also be directly called by the app layer
	
* **Component layer** 

	Complex business modules that aggregate multiple basic business functions, such as friends, nearby people, may use multiple service services, or you can use library directly.
	
* **App layer**
 	Application portal to aggregate multiple business modules, such as the main terminal or debugger

A good architecture needs to be highly available and easy to debug. The **plugin supports debugging of any layer, and the Module of the service/component layer is turned into a component to break the limitations of traditional componentization.**

The plugin divides the source code into SDK and IMPL by intervening in the module's build process, where the SDK is compiled into a jar and IMPL is all resources except the SDK.

<img src="./doc/component_build_1.jpg"  alt="component_all" align=center />

Therefore, the dependencies of the module or component are converted to

<img src="./doc/component_build_2.jpg"  alt="component_all" align=center />

In fact, dependent scenarios will change dynamically based on different build processes.

<img src="./doc/component_build_3.jpg"  alt="component_all" align=center />

### how to use

* Add plugin dependencies and declare dependent libraries

```
buildscript {
    repositories {
        maven { url 'https://jitpack.io' }
    }
    dependencies {
        classpath "com.effective.plugins:component:1.0.10
    }
}
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}

```
* Write a plugin script (see component.gradle in the sample gradleScript directory) and use it in the root project

```
apply from: "./gradleScript/component.gradle"
```

* Add the sdk directory to the module that needs to be componentized to store the exposed source code (refer to sample library/src/sdk), it will be automatically compiled into jar

```
+ library/src/sdk/<packagePath>/xxx.java or xxx.kt
```


### Sample or Practice of the AndroidModularArchiteture project

<img src="./doc/sample.png"  width = "270" height = "500" /> <img src="./doc/android_modular_architeture.png"  width = "270" height = "500"/>

* Green is a stand-alone module
* Blue is a stand-alone service (component)
* Orange is a component that runs independently (component)
* Black is the main end, the default debugging is the running result of not configuring the debugging component, and the custom debugging is to support the debugging result of any module (the above green/blue/orange can be regarded as the custom debugging, but the debugging function is for the corresponding module/ Components only)

**link：**[AndroidModularArchiteture](https://github.com/YummyLau/AndroidModularArchiteture) 


### Reference / Special Acknowledgments
As early as 17 years, WeChat has been published [微信Android模块化架构重构实践](https://mp.weixin.qq.com/s?__biz=MzAwNDY1ODY2OQ==&mid=2649286672&idx=1&sn=4d9db00c496fcafd1d3e01d69af083f9&chksm=8334cc92b4434584e8bdb117274f41145fb49ba467ec0cd9ba5e3551a8abf92f1996bd6b147a&mpshare=1&scene=1&srcid=06309KcVegxww8kRannKXmkM&key=9965dca0b72a0a7428febd95a3bc61657924797129ae35d34f67f2cfc5c5ac09bec624714cd4662b978742d3424726f08b3ea1b9cb858cccf97dbb56bd5bfdd07a81917eedc452194d3c6b438d76dfac&ascene=0&uin=Mjg5NTY2MjM0MA==&devicetype=iMac%20MacBookPro11,4%20OSX%20OSX%2010.12.5%20build(16F73)&version=12020810&nettype=WIFI&fontScale=100&pass_ticket=X8yiKyEXbEsX7ouYBsjW0ddHl5Zc0CXaGzDaapnZidysc89C7Z257hmzlRaR3CQk) In the article, it is related to the function of separating the module through the interface protection form (.apiization) and generating the corresponding 'SDK' project. Other projects rely on the compilation of this generated project.
I used to separate the interface provided by the module into a project and package it into sdk, but this will cause sdk and impl to be divided into two modules, which is not easy to maintain and unsightly.The industry's excellent open source project Mis provides a code isolation idea, roughly:

* Put the interface file into the aidl file by modifying the sourceSet and packaging the interface into sdk
* Implementing engineering depends on packaged sdk

By studying the ideas written by the source code reference authors, it is indeed a good method, the details of the logic see the source code. It is worth mentioning that sdk is a jar, not only can contain business APIs, but also data interface beans. In the process of plugin development，Thanks to the Mis author [EastWoodYang] (https://github.com/EastWoodYang) for giving me a lot of help and guidance!

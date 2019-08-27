# ModulePlugin
android modular plugin.

> 初衷，android 9.0之后插件基本已经告别。随着android版本越来稳定封闭，开发逐步偏向模块化开发。
> 并有没有一个支持代码隔离，支持独立调试，且不对任何现有代码进行改动的插件。

本工程主要解决以下 4 个问题：
1. 如何解决接口api和实现impl代码隔？
2. 如何解决工程模块独立调试？
3. 如何解决动态依赖接口api和impl实现，在Sync场景下依赖接口api而在其他场景下动态添加impl实现？
4. 如何解决模块的绑定和卸载，接口api的暴露和回收？

现实场景中，有很多优秀的开源库如 [DDComponentForAndroid](https://github.com/luojilab/DDComponentForAndroid) 提供了独立的调试思路但其配置繁琐，不利于开发。[Mis](https://github.com/EastWoodYang/Mis) 提供了如何实现工程上代码隔离但是无法针对单一模块进行调试等等。
在希望解决上述 4 个核心问题的前提下，只需要在项目添加一份独立的gradle脚本且不需要修改任何已有的代码工程结构实现 "代码隔离，独立调试，api注入"。

> 如何解决接口api和实现impl代码隔

早在 17 年微信发布过 [微信Android模块化架构重构实践](https://mp.weixin.qq.com/s?__biz=MzAwNDY1ODY2OQ==&mid=2649286672&idx=1&sn=4d9db00c496fcafd1d3e01d69af083f9&chksm=8334cc92b4434584e8bdb117274f41145fb49ba467ec0cd9ba5e3551a8abf92f1996bd6b147a&mpshare=1&scene=1&srcid=06309KcVegxww8kRannKXmkM&key=9965dca0b72a0a7428febd95a3bc61657924797129ae35d34f67f2cfc5c5ac09bec624714cd4662b978742d3424726f08b3ea1b9cb858cccf97dbb56bd5bfdd07a81917eedc452194d3c6b438d76dfac&ascene=0&uin=Mjg5NTY2MjM0MA==&devicetype=iMac%20MacBookPro11,4%20OSX%20OSX%2010.12.5%20build(16F73)&version=12020810&nettype=WIFI&fontScale=100&pass_ticket=X8yiKyEXbEsX7ouYBsjW0ddHl5Zc0CXaGzDaapnZidysc89C7Z257hmzlRaR3CQk) 一文中涉及到通过接口保护形式（.api化）拉实现分离模块的功能并生成对应的 'SDK' 工程，其他工程依赖编译的只是这个生成的工程。
曾把模块提供的接口独立为一个工程并打包成sdk，但这样就会导致 sdk 和 impl 分为两个模块，不好维护和不雅观。业界优秀开源项目 Mis 提供了一种代码隔离的思路，大致为：

* 通过修改 sourceSet 把接口文件放到 aidl 文件中，并接口打包成 sdk
* 实现工程依赖打包的 sdk

通过研究源码参考作者编写的思路，确实是一种好用的方法，细节逻辑见源码。值得一提的是，sdk 是一个 jar，不仅仅可以包含业务 api，也可以包含数据接口 bean等。

> 如何解决工程模块独立调试

不仅仅是 DDComponent 提供的方案，很多现有的文章推荐都方法都是通过修改 sourceSet 的内容，在已有工程下新增一个独立的调试资源目录，然后配置代码和资源，指定 AndroidManifest 来达到提供 App 入口的手段。也就说，在 Debug 模式下我们只需要把我们的调试目录编进去即可。但是如何在对应的插件 Project 被加载之前读取提前读取到配置信息呢 ？很多方案都使用了 gradle.properties 来配置，实际上没有必要，也可以在 gradle 脚本中配置，但是要改变配置读取的时间。我们可以在 root Project 提前读取子 Project 的手段来解决。这样更聚合配置信息。

> 如何解决动态依赖接口api和impl实现，在Sync场景下依赖接口api而在其他场景下动态添加impl实现

在解决第一个问题的前提下，我们已经把接口api打成 sdk ， 把实现打成 impl，一般我们在 Sync阶段只需要编译 sdk 即可，但我们总不能一直修改 gradle 脚本。可以通过修改 Dependence 的 Scope 来自定义获取依赖的值。比如

```
    //只需要这个
    implementation component(':library')

    //等价于
    同步时
    implementation ':library-sdk'
    非同步时
    implementation ':library-impl'
    implementation ':library-sdk'

```
细节逻辑请参考源码

> 如何解决模块的绑定和卸载，接口api的暴露和回收

按照业务把工程划分为一下几层：

* Base层 - 基础类库,存放精简的代码，高复用性，一般其他模块直接引用即可，比如一些utils，一些baseActivity等
* Service层 - 支持某类基础业务功能的独立模块，比如登陆服务，换肤服务，一般介于基础类库Base层和业务组件Component层中间，也可以直接被App层调用
* Component层 - 聚合多中基础业务功能的复杂业务模块，比如朋友圈，附近的人，一般可能使用多个Service服务；
* App层 - App的入口，聚合多个业务模块；

从功能上看，Service层和Component最大的区别是Service功能更纯粹而Component耦合多功能多场景逻辑，但是从工程上看，两者都可进行代码隔离。比如登陆Service需要登陆相关的接口，朋友圈需要提供入口等等。
所以这两者都工程构建上，都需要支持代码隔离。

在解决代码隔离场景下，需要解决一个问题 "Service层和Component如何在合适的时机注入impl模块，并维护impl的生命周期。"
这里的接口指的是模块需要暴露出来的api，一般一个模块可对应多个api接口，所以接口注入，模块的绑定和卸载可以有一个清晰的生命周期：

 **模块绑定** -> **接口注入** -> **接口卸载** -> **模块卸载**

而在实现完全被隔离的情况下，如何实现解决这个问题呢 ？ Transform + annotation + asm 技术。通过自定义插件注册自定义的 transform 完成 Class 文件中 annotation 的扫描可收集目的信息（注解内容，注解类等）， 用 asm 封装好的字节码指令完成对目标类字节内容的修改达到注入代码的目的。




未来规划： 支持sdk及impl maven仓库存储


# ModulePlugin
android modular plugin.

> 初衷，android 9.0之后插件基本已经告别。随着android版本越来稳定封闭，开发逐步偏向模块化开发。
> 并有没有一个支持代码隔离，支持独立调试，且不对任何现有代码进行改动的插件。

早在 17年微信发布过 [微信Android模块化架构重构实践](https://mp.weixin.qq.com/s?__biz=MzAwNDY1ODY2OQ==&mid=2649286672&idx=1&sn=4d9db00c496fcafd1d3e01d69af083f9&chksm=8334cc92b4434584e8bdb117274f41145fb49ba467ec0cd9ba5e3551a8abf92f1996bd6b147a&mpshare=1&scene=1&srcid=06309KcVegxww8kRannKXmkM&key=9965dca0b72a0a7428febd95a3bc61657924797129ae35d34f67f2cfc5c5ac09bec624714cd4662b978742d3424726f08b3ea1b9cb858cccf97dbb56bd5bfdd07a81917eedc452194d3c6b438d76dfac&ascene=0&uin=Mjg5NTY2MjM0MA==&devicetype=iMac%20MacBookPro11,4%20OSX%20OSX%2010.12.5%20build(16F73)&version=12020810&nettype=WIFI&fontScale=100&pass_ticket=X8yiKyEXbEsX7ouYBsjW0ddHl5Zc0CXaGzDaapnZidysc89C7Z257hmzlRaR3CQk) 一文中涉及到通过接口保护形式（.api化）拉实现分离模块的功能并生成对应的 'SDK' 工程，其他工程依赖编译的只是这个生成的工程。
曾把模块提供的接口独立为一个工程并打包成sdk，但这样就会导致 sdk 和 impl 分为两个模块，不好维护和不雅观。 从业界有比较优秀的开源 [Mis](https://github.com/EastWoodYang/Mis) 提供了一种代码隔离的思路，大致为：

* 通过修改 sourceSet 把接口文件放到 aidl 文件中，并接口打包成 sdk
* 实现工程依赖打包的 sdk

通过研究源码参考作者编写的思路，确实是一种好用的方法。

按照业务把工程划分为一下几层：

* 基础类库Base层 - 精简的代码，高复用性，一般其他模块直接引用即可；
* Service层 - 支持某类基础业务功能的独立模块，比如登陆服务，换肤服务，一般介于基础类库Base层和业务组件Component层中间，也可以直接被App层调用
* Component层 - 聚合多中基础业务功能的复杂业务模块，比如朋友圈，附近的人，一般可能使用多个Service服务；
* App层 - App的入口，聚合多个业务模块；

从功能上看，Service层和Component最大的区别是Service更加纯粹而Component耦合多场景逻辑，但是从工程上看，两者都可进行代码隔离。比如登陆Service需要登陆相关的接口，朋友圈需要提供入口等等。
所以这两者都工程构建上，都需要支持代码隔离。

在解决代码隔离场景下，接口的注入，模块的绑定和卸载是面临的一大问题。

这里的模块指的是工程上的概念。也就是说，这里的接口指的是模块需要暴露出来的api，一般一个模块可对应多个api接口，所以接口注入，模块的绑定和卸载可以有一个清晰的生命周期：

模块绑定 -> 接口注入 -> 接口卸载 -> 模块卸载

而在实现完全被隔离的情况下，如何实现解决这个问题呢 ？ Transform + annotation + asm 技术。通过自定义插件注册自定义的 transform 完成 Class 文件中 annotation 的扫描可收集目的信息（注解内容，注解类等）， 用 asm 封装好的字节码指令完成对目标类字节内容的修改达到注入代码的目的。

未完待续。。。。




* 支持独立调试 (done)

* 支持代码隔离 (done)
* 动态替换依赖（done）
* 动态绑定和卸载 （done）
* 通讯
    * router （框架提供或者自行提供）
    * 跨进程支持 （未开发）
* kotlin/java/fluter模块兼容 （未开发）

\# Luban

[![](https://jitpack.io/v/mmm3w/Luban.svg)](https://jitpack.io/#mmm3w/Luban)

对原Luban按个人需求做了相关调整，以及适配Android的沙盒存储模式，部分实现也参考了[KLuban](https://github.com/forJrking/KLuban)

### [Raw README](https://github.com/Curzibn/Luban/blob/master/README.md)

### 相关改动

- [x] 删除了异步压缩<br>原项目使用了`AsyncTask`去作为多文件或单文件的异步压缩，但个人认为这部分可以由外部去实现，无论是使用协程也好、线程池也好，又或者是简单粗暴的new
  Thread。

- [x] 单实例不再支持列表数据<br>配合上一条件做出的相关调整

- [x] 
  整合部分KLuban中的逻辑，详见[KLuban](https://github.com/forJrking/KLuban/blob/master/README.md)，并搬取了Glide的数组池相关逻辑

- [x] Kotlin迁移<br>大部分逻辑迁移至kotlin，因功能调整删减了一些api，并且提供了kotlin的新的调用形式

- [x] 适配Android沙盒模式<br>若输出文件需要对外共享，则要么存储在沙盒中再依赖ContentProvider来共享，要么申请相关权限获取OutputStream实例来直接输出。<br>
  注：关于沙盒模式，十分建议在`AndroidManifest.xml`中配置`android:requestLegacyExternalStorage="true"`
  。在android10中因为存储模式调整用力过猛导致无法使用路径直接访问沙盒内部的文件，而在后续的版本中又开放了此权限，这项配置并不是为了逃避适配沙盒模式，而且为了保证在Android10中能够通过路径直接访问沙盒内部存储。对于沙盒外部的存储，在高版本中依然需要申请相关权限然后依赖流来读写

- [x] 支持Bitmap和Base64的输入

- [x] 迁移库至jitpack<br>原始的jcenter库已废弃

### 使用

Step 1. Add the JitPack repository to your build file

Add it in your root build.gradle at the end of repositories:

	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}

Step 2. Add the dependency

	dependencies {
	        implementation 'com.github.mmm3w:Luban:1.2.0-fix_build'
	}

~~吐了，留下了一个奇怪的版本号~~

Java Api:

请参照原来文档，存在部分api删减

Kotlin Api:

```kotlin
//使用 compress 方法时会自动调用get()
Luban.with(this)
    .compress {
        //请下面任选一种作为输入，必须
        input(inputStream = /*InputStream*/)
        input(uri = /*Uri*/, context = /*Context*/)
        input(file = /*File*/) //请不要使用沙盒以外的任何路径
        input(path = /*String*/) //请不要使用沙盒以外的任何路径
        input(base64 = /*String*/) //不建议使用 
        input(bitmap = /*Bitmap*/) //不建议使用

        //请下面任选一种作为输出，可选
        output(dir = /*File*/) //输出目录，请不要使用沙盒以外的任何路径，有默认输出目录
        output(path = /*String*/) //输出目录，请不要使用沙盒以外的任何路径，有默认输出目录
        output(outputStream = /*OutputStream*/) //输出流，可应对沙盒模式下对外部存储的写入（未测试）

        //压缩阈值，可选，默认100KB，例：ignoreBy(size = 200L, unit = MemoryUnit.KB)
        ignoreBy(size = /*Long*/, unit = /*MemoryUnit*/)

        //定义输出名字，可选，默认纳秒时间戳，例：rename( "test-$it" )
        rename { it }

        //压缩文件格式，可选，默认参照输入格式，支持 JPG,PNG,WEBP，例：format(Bitmap.CompressFormat.PNG)
        format(/*Bitmap.CompressFormat*/)

        //质量压缩系数，0-100，可选，默认60，例：quality(90)
        quality(/*Int*/)

        //最大大小，可选，不一定能达到，例：maxSize(size = 300L, unit = MemoryUnit.KB)
        maxSize(size = /*Long*/, unit = /*MemoryUnit*/)

        //保持原有的编码方式，可选，内存不够时自动降级策略会失效
        keepConfig(true)

        //开起双线性采样，可选，参照KLuban以及其提及文档，对纯文字内容效果较好
        bilinear(true)
    }

//使用 config 方法需要手动调用获取方法
val luban = Luban.with(this)
    .config {
        //...
    }
//无任何返回，建议在配置output为outputStream或配置rename后去指定目录下查找文件时使用
luban.get()

//返回压缩后生成的文件
luban.getFile()

//返回压缩后的Bitmap
luban.getBitmap()

//返回压缩后的Base64
luban.getBase64()

```

关于分区存储的说明：

- 请不要使用外部存储的路径直接读写文件
- 在没有读取权限的情况下，请不要直接通过路径读取从媒体数据库中获取的其他应用的媒体文件，因为此时即使能够获取路径，也只能访问自身应用创建的媒体文件。
- 在没有配置`android:requestLegacyExternalStorage="true"`的情况下在`Android10`
  中不要直接使用路径直接读写任意目录下的文件，官方对于`Android10`的存储适配建议是停用分区存储并使用`Android9`
  及以下的存储方案，即配置`android:requestLegacyExternalStorage="true"`。

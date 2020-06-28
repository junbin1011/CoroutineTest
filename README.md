# 协程

大家如果已经使用Kotlin语言进行开发，对协程这个概念应该不会很陌生。虽然在网上有很多Kotlin协程相关的文章，但当我开始准备使用的时候，还是有如下几个疑虑。

1. 协程到底能够解决什么样的问题？
2. 协程和我们常用的Executor、RxJava有什么区别？
3. 项目上使用有什么风险吗？

接下来就带着这几个问题一起来揭开协程神秘的面纱。


# 如何使用

关于协程，我在网上看到最多的说法是协程是轻量级的线程。那么协程首先应该解决的问题就是程序中我们常常遇到的 **“异步”** 的问题。我们看看官网介绍的几个使用例子。

## 依赖

```
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.3'
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.3.3'
```

## 入门

```
import kotlinx.coroutines.*

fun main() {
    GlobalScope.launch { // 在后台启动一个新的协程并继续
        delay(1000L)
        println("World!")
    }
    println("Hello,") // 主线程中的代码会立即执行
    runBlocking {     // 但是这个表达式阻塞了主线程
        delay(2000L)  // ……我们延迟 2 秒来保证 JVM 的存活
    } 
}
```

## 挂起函数

```

suspend fun doSomethingUsefulOne(): Int {
    delay(1000L) // 假设我们在这里做了一些有用的事
    return 13
}

suspend fun doSomethingUsefulTwo(): Int {
    delay(1000L) // 假设我们在这里也做了一些有用的事
    return 29
}

val time = measureTimeMillis {
    val one = doSomethingUsefulOne()
    val two = doSomethingUsefulTwo()
    println("The answer is ${one + two}")
}
println("Completed in $time ms")

```

结果:
```
The answer is 42
Completed in 2015 ms
```

## 使用 async 并发

```
val time = measureTimeMillis {
    val one = async { doSomethingUsefulOne() }
    val two = async { doSomethingUsefulTwo() }
    println("The answer is ${one.await() + two.await()}")
}
println("Completed in $time ms")
```

结果:
```
The answer is 42
Completed in 1017 ms
```
## 单元测试

```
class MyTest {
    @Test
    fun testMySuspendingFunction() = runBlocking<Unit> {
        // 这里我们可以使用任何喜欢的断言风格来使用挂起函数
    }
}
```

更新详细的使用可参考[官网示例](https://www.kotlincn.net/docs/reference/coroutines/coroutines-guide.html)

# 为何使用

既然已经有这么多异步处理的框架，那我们为何还要使用协程。这里举个例子，看看对同个需求，不同异步框架的处理方式。

> 现在有一个产品需求，生成一个二维码在页面展示给用户。我们来对比看看不同的做法。

# Thread

```
Thread(Runnable {
        try {
            val qrCode: Bitmap =
            CodeCreator.createQRCode(this@ShareActivity, SHARE_QR_CODE)
            runOnUiThread { 
                img_qr_code.setImageBitmap(qrCode)
                }
            } catch (e: WriterException) {
                e.printStackTrace()
            }
        }).start()
    }
```
# Executors
```
Executors.newSingleThreadExecutor().execute {
        try {
            val qrCode: Bitmap =
            CodeCreator.createQRCode(this@ShareActivity, SHARE_QR_CODE)
            runOnUiThread {
                img_qr_code.setImageBitmap(qrCode)
            }
        } catch (e: WriterException) {
            e.printStackTrace()
        }
    }
```
# RxJava
```
Observable.just(SHARE_QR_CODE)
        .map(new Function<String, Bitmap>() {
            @Override
            public Bitmap apply(String s) throws Exception {
                return CodeCreator.createQRCode(ShareActivity.this, s);
            }
        })
        .subscribe(new Consumer<Bitmap>() {
            @Override
            public void accept(Bitmap bitmap) throws Exception {
                img_qr_code.setImageBitmap(bitmap);
            }
        });
```

# Koroutine
```
 val job = GlobalScope.launch(Dispatchers.IO) {
            val bitmap = CodeCreator.createQRCode(ShareActivity.this, SHARE_QR_CODE)
            launch(Dispatchers.Main) {
                img_qr_code.setImageBitmap(bitmap)
            }
        }
}
```

通过这个例子，可以看出使用协程的非常方便解决 **"异步回调"** 问题。
相比传统的Thread及Excutors，RxJava将嵌套回调转换成链式调用的形式，提高了代码可读性。协程直接将链式调用转换成了协程内的顺序调用，**"代码更加精简"**。 



# 性能

官网上对于协程的有一句介绍。

> 本质上，协程是轻量级的线程

那么协程的执行效率到底怎么样呢？下面我们采用官网的示例在相同的环境和设备下做下对比。

>启动了 1000个协程，并且为每个协程都输出一个点

# Coroutine

```
  var startTime = System.currentTimeMillis()
            repeat(times) { i -> // 启动大量的协程
                GlobalScope.launch(Dispatchers.IO) {
                    Log.d(this@MainActivity.toString(), "$i=.")
                }

            }
            var endTime = System.currentTimeMillis() - startTime;
            Log.d(this@MainActivity.toString(), "endTime=$endTime")
            
```

**执行结果：endTime=239 ms**

# Thread

```
 var startTime = System.currentTimeMillis()
            repeat(times) { i ->// 启动大量的线程
                Thread(Runnable {
                    Log.d(this@MainActivity.toString(), "$i=.")
                }).start()
            }
            var endTime = System.currentTimeMillis() - startTime;
```

**执行结果：endTime=3161 ms**

# Excutors

```
 var startTime = System.currentTimeMillis()
            var executors = Executors.newCachedThreadPool()
            repeat(times) { i -> // 使用线程池
                executors.execute {
                    Log.d(this@MainActivity.toString(), "$i=.")
                }
            }
            var endTime = System.currentTimeMillis() - startTime;
            Log.d(this@MainActivity.toString(), "endTime=$endTime")
```

**执行结果：endTime=143 ms**

# rxjava

```
      var startTime = System.currentTimeMillis()
            repeat(times) { i -> // 启动Rxjava
                Observable.just("").subscribeOn(Schedulers.io())
                        .subscribe {
                            Log.d(this@MainActivity.toString(), "$i=.")
                        }
            }
            var endTime = System.currentTimeMillis() - startTime;
            Log.d(this@MainActivity.toString(), "endTime=$endTime")
```

**执行结果：endTime=241 ms**

源码工程：[CorountineTest](https://github.com/junbin1011/CoroutineTest)

## Profiler

利用AS自带的Profiler对运行时的CPU状态进行检测，我们可以看到Thread对CPU的消耗比较大，Koroutine、Executor、RxJava的消耗基本差不多。

![](https://note.youdao.com/yws/api/personal/file/WEB5f6a56d727f90a44b7b6429ce020d197?method=download&shareKey=234ec553594f4eb914dced4a79b678be)
## 总结

从执行时间和Profiler上看，Coroutine比使用Thread性能提升了一个量级，但与Excutor和RxJava性能是在一个量级上。

> 注意这里的例子为了简便，因为异步执行的时间基本和repeat的时间差不多，我们没有等所有异步执行完再打印时间，这里们不追求精确的时间，只为做量级上的对比。

# 实现机制

## 协程底层异步实现机制

我们先来看一段简单的Kotlin程序。

```
GlobalScope.launch(Dispatchers.IO) {
            print("hello world")
        }
```

我们接着看下launch的实现代码。

```
public fun CoroutineScope.launch(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
): Job {
    val newContext = newCoroutineContext(context)
    val coroutine = if (start.isLazy)
        LazyStandaloneCoroutine(newContext, block) else
        StandaloneCoroutine(newContext, active = true)
    coroutine.start(start, coroutine, block)
    return coroutine
}
```
这里注意，我们通过追踪最后的继承关系发现，DefaultScheduler.IO最后也是一个CoroutineContext。

接着发现继续看coroutine.start的实现，如下：
```
 public fun <R> start(start: CoroutineStart, receiver: R, block: suspend R.() -> T) {
        initParentJob()
        start(block, receiver, this)
    }
```

接着继续看CoroutineStart的start策略，如下：
```
  @InternalCoroutinesApi
    public operator fun <T> invoke(block: suspend () -> T, completion: Continuation<T>) =
        when (this) {
            CoroutineStart.DEFAULT -> block.startCoroutineCancellable(completion)
            CoroutineStart.ATOMIC -> block.startCoroutine(completion)
            CoroutineStart.UNDISPATCHED -> block.startCoroutineUndispatched(completion)
            CoroutineStart.LAZY -> Unit // will start lazily
        }
```
继续看startCoroutineCancellable方法，如下：
```
@InternalCoroutinesApi
public fun <T> (suspend () -> T).startCoroutineCancellable(completion: Continuation<T>) = runSafely(completion) {
    createCoroutineUnintercepted(completion).intercepted().resumeCancellableWith(Result.success(Unit))
}
```
继续看resumeCancellableWith方法实现：

```
@InternalCoroutinesApi
public fun <T> Continuation<T>.resumeCancellableWith(result: Result<T>) = when (this) {
    is DispatchedContinuation -> resumeCancellableWith(result)
    else -> resumeWith(result)
}
```

最后发现调用的resumeCancellableWith方法实现如下：

```
   inline fun resumeCancellableWith(result: Result<T>) {
        val state = result.toState()
        if (dispatcher.isDispatchNeeded(context)) {
            _state = state
            resumeMode = MODE_CANCELLABLE
            dispatcher.dispatch(context, this)
        } else {
            executeUnconfined(state, MODE_CANCELLABLE) {
                if (!resumeCancelled()) {
                    resumeUndispatchedWith(result)
                }
            }
        }
    }
```

这里关键的触发方法在这个地方

```
dispatcher.dispatch(context, this)

```

我们看 DefaultScheduler.IO最后的dispatch方法：

```
    override fun dispatch(context: CoroutineContext, block: Runnable): Unit =
        try {
            coroutineScheduler.dispatch(block)
        } catch (e: RejectedExecutionException) {
            DefaultExecutor.dispatch(context, block)
        }
```



这里我们最终发现是调用了CoroutineScheduler的dispatch方法，继续看CoroutineScheduler的实现发现，CoroutineScheduler继承了Executor。

通过dispatch的调用最后可以发现CoroutineScheduler其实就是对Worker的调度，我们看看Worker的定义。

```
internal inner class Worker private constructor() : Thread()
```
通过这里我们发现另外一个老朋友Thread，所以到这里也符合上面性能验证的测试结果。

**到这里我们也有结论了，协程异步实现机制本质也就是自定义的线程池。**


## 非阻塞式挂起 suspend

suspend有什么作用，如何做到异步不用回调？下面先定义一个最简单的suspend方法。
```
    suspend fun hello(){
        delay(100)
        print("hello world")
    }
```
通过Kotlin Bytecode转换为java 代码如下：
```
@Nullable
   public final Object hello(@NotNull Continuation $completion) {
      Object $continuation;
      label20: {
         if ($completion instanceof <undefinedtype>) {
            $continuation = (<undefinedtype>)$completion;
            if ((((<undefinedtype>)$continuation).label & Integer.MIN_VALUE) != 0) {
               ((<undefinedtype>)$continuation).label -= Integer.MIN_VALUE;
               break label20;
            }
         }

         $continuation = new ContinuationImpl($completion) {
            // $FF: synthetic field
            Object result;
            int label;
            Object L$0;

            @Nullable
            public final Object invokeSuspend(@NotNull Object $result) {
               this.result = $result;
               this.label |= Integer.MIN_VALUE;
               return Test.this.hello(this);
            }
         };
      }

      Object $result = ((<undefinedtype>)$continuation).result;
      Object var6 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
      switch(((<undefinedtype>)$continuation).label) {
      case 0:
         ResultKt.throwOnFailure($result);
         ((<undefinedtype>)$continuation).L$0 = this;
         ((<undefinedtype>)$continuation).label = 1;
         if (DelayKt.delay(100L, (Continuation)$continuation) == var6) {
            return var6;
         }
         break;
      case 1:
         Test var7 = (Test)((<undefinedtype>)$continuation).L$0;
         ResultKt.throwOnFailure($result);
         break;
      default:
         throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
      }

      String var2 = "hello world";
      boolean var3 = false;
      System.out.print(var2);
      return Unit.INSTANCE;
   }
```

这里首先我们发现方法的参数多了一个Continuation completion并且内部回定义一个 Object continuation，看看Continuation的定义。

```
@SinceKotlin("1.3")
public interface Continuation<in T> {
    /**
     * The context of the coroutine that corresponds to this continuation.
     */
    public val context: CoroutineContext

    /**
     * Resumes the execution of the corresponding coroutine passing a successful or failed [result] as the
     * return value of the last suspension point.
     */
    public fun resumeWith(result: Result<T>)
}
```
这是一个回调接口，里面有一个关键的方法为resumeWith。 这个方法的具体调用通过上面的协程调用流程可以知道 ，在DispatchedContinuation的resumeCancellableWith会触发。

```
public fun <T> Continuation<T>.resumeCancellableWith(result: Result<T>) = when (this) {
    is DispatchedContinuation -> resumeCancellableWith(result)
    else -> resumeWith(result)
}
```

那么resumeWith里面做了那些事情？我们看下具体的实现在ContinuationImpl的父类BaseContinuationImpl中。

```
 public final override fun resumeWith(result: Result<Any?>) {
        // This loop unrolls recursion in current.resumeWith(param) to make saner and shorter stack traces on resume
        var current = this
        var param = result
        while (true) {
            // Invoke "resume" debug probe on every resumed continuation, so that a debugging library infrastructure
            // can precisely track what part of suspended callstack was already resumed
            probeCoroutineResumed(current)
            with(current) {
                val completion = completion!! // fail fast when trying to resume continuation without completion
                val outcome: Result<Any?> =
                    try {
                        val outcome = invokeSuspend(param)
                        if (outcome === COROUTINE_SUSPENDED) return
                        Result.success(outcome)
                    } catch (exception: Throwable) {
                        Result.failure(exception)
                    }
                releaseIntercepted() // this state machine instance is terminating
                if (completion is BaseContinuationImpl) {
                    // unrolling recursion via loop
                    current = completion
                    param = outcome
                } else {
                    // top-level completion reached -- invoke and return
                    completion.resumeWith(outcome)
                    return
                }
            }
        }
    }

```
首先我们发现这里其实是一个递归的循环，并且会调用invokeSuspend方法触发实际的调用，等待返回结果。通过上面的分析可以看出2点。

1. 非阻塞是因为本身启动一个协程也是使用线程池异步执行，所以不会阻塞
2. 协程并不是没有回调，而是将回调的接口（Continuation）及调度代码在编译器生成，不用自己编写。
3. resumeWith是一个循环及递归，所以会将协程内定义的表达式顺序串联调用。达到挂起及恢复的链式调用。


## 总结

1. 协程到底能够解决什么样的问题？
- 解决异步回调嵌套
- 解决异步任务之间协作

2. 协程和我们常用的Executor、RxJava有什么区别？

- 从任务调度上看，本质都是线程池的封装

3. 项目上使用有什么风险吗？

- 从性能上看与线程池与RxJava在一个量级
- 目前已是稳定版本1.3.3，开源项目使用多
- 代码使用简便，可维护性高
- 开源生态支持良好，方便使用（Retrofit、Jitpack已支持）
- 团队学习及旧项目改造需要投入一定成本


# 参考资料

[www.kotlincn.net](https://www.kotlincn.net/docs/reference/coroutines/coroutines-guide.html)


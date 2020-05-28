package com.junbin.myapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val times = 1000
        btnCoroutine.setOnClickListener {
            var startTime = System.currentTimeMillis()
            repeat(times) { i -> // 启动大量的协程
                GlobalScope.launch(Dispatchers.IO) {
                    Log.d(this@MainActivity.toString(), "$i=.")
                }

            }
            var endTime = System.currentTimeMillis() - startTime;
            Log.d(this@MainActivity.toString(), "endTime=$endTime")

        }


        btnThread.setOnClickListener {
            var startTime = System.currentTimeMillis()
            repeat(times) { i ->// 启动大量的线程
                Thread(Runnable {
                    Log.d(this@MainActivity.toString(), "$i=.")
                }).start()
            }
            var endTime = System.currentTimeMillis() - startTime;
            Log.d(this@MainActivity.toString(), "endTime=$endTime")
        }

        btnExecutors.setOnClickListener {
            var startTime = System.currentTimeMillis()
            var executors = Executors.newCachedThreadPool()
            repeat(times) { i -> // 使用线程池
                executors.execute {
                    Log.d(this@MainActivity.toString(), "$i=.")
                }
            }
            var endTime = System.currentTimeMillis() - startTime;
            Log.d(this@MainActivity.toString(), "endTime=$endTime")
        }

        btnRxJava.setOnClickListener {
            var startTime = System.currentTimeMillis()
            repeat(times) { i -> // 启动Rxjava
                Observable.just("").subscribeOn(Schedulers.io())
                        .subscribe {
                            Log.d(this@MainActivity.toString(), "$i=.")
                        }
            }
            var endTime = System.currentTimeMillis() - startTime;
            Log.d(this@MainActivity.toString(), "endTime=$endTime")
        }

    }
}

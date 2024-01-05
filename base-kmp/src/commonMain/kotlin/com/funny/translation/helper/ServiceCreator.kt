package com.funny.translation.helper

import com.funny.translation.network.OkHttpUtils
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type


/** from https://github.com/zhujiang521/PlayAndroid
 * 版权：Zhujiang 个人版权
 * @author zhujiang
 * 版本：1.5
 * 创建日期：2021/4/30
 * 描述：ServiceCreator
 *
 * 有修改
 */

object ServiceCreator {
    var TRANS_PATH = "/trans/v1/"
    private const val DEFAULT_BASE_URL = "https://api.funnysaltyfish.fun/trans/v1/"

    var BASE_URL = readBaseURL()
        set(value) {
            field = value
//            if (!AppConfig.developerMode.value) return
//            field = value
//            DataSaverUtils.saveData("BASE_URL", value)
//            ApplicationUtil.restartApp(appCtx)
        }

    private val retrofit by lazy {
        val okHttpClient = OkHttpUtils.okHttpClient
        RetrofitBuild(
            url = BASE_URL,
            client = okHttpClient,
        ).retrofit
    }

    private fun readBaseURL(): String {
//        var url = if (AppConfig.developerMode.value) DataSaverUtils.readData("BASE_URL", DEFAULT_BASE_URL) else DEFAULT_BASE_URL
//        if (!url.endsWith("/")) url += "/"
//        return url
        return DEFAULT_BASE_URL
    }

    /**
     * get ServiceApi
     */
    fun <T> create(service: Class<T>): T = retrofit.create(service)
}

/**
 * 当相应体为空时直接返回null
 */
class NullOnEmptyConverterFactory : Converter.Factory() {
    override fun responseBodyConverter(
        type: Type,
        annotations: Array<Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *> {
        val delegate: Converter<ResponseBody, *> =
            retrofit.nextResponseBodyConverter<Any>(this, type, annotations)
        return Converter { body ->
            val contentLength = body.contentLength()
            if (contentLength == 0L) {
                null
            } else delegate.convert(body)
        }
    }
}

class RetrofitBuild(
    url: String, client: OkHttpClient,
) {
    val retrofit: Retrofit = Retrofit.Builder().apply {
        baseUrl(url)
        client(client)
        addConverterFactory(NullOnEmptyConverterFactory())
        addConverterFactory(JsonX.asConverterFactory("application/json".toMediaType()))
    }.build()
}

/**
 * save cookie string
 */
fun encodeCookie(cookies: List<String>): String {
    val sb = StringBuilder()
    val set = HashSet<String>()
    cookies
        .map { cookie ->
            cookie.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        }
        .forEach { it ->
            it.filterNot { set.contains(it) }.forEach { set.add(it) }
        }

    val ite = set.iterator()
    while (ite.hasNext()) {
        val cookie = ite.next()
        sb.append(cookie).append(";")
    }

    val last = sb.lastIndexOf(";")
    if (sb.length - 1 == last) {
        sb.deleteCharAt(last)
    }

    return sb.toString()
}

package com.example.kursovaya.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.kursovaya.api.RetrofitClient
import org.robolectric.RuntimeEnvironment
import retrofit2.Retrofit

object RepositoryTestHelper {
    fun applicationContext(): Context = ApplicationProvider.getApplicationContext()

    /** Fallback when ApplicationProvider is unavailable in plain JVM tests. */
    fun robolectricContext(): Context = RuntimeEnvironment.getApplication()

    fun setField(
        target: Any,
        name: String,
        value: Any?,
    ) {
        var clazz: Class<*>? = target.javaClass
        while (clazz != null) {
            try {
                val field = clazz.getDeclaredField(name)
                field.isAccessible = true
                field.set(target, value)
                return
            } catch (_: NoSuchFieldException) {
                clazz = clazz.superclass
            }
        }
        error("Field $name not found on ${target.javaClass.name}")
    }

    fun resetRetrofitClient() {
        setRetrofitField("context", null)
        setRetrofitField("retrofitInstance", null)
    }

    fun setRetrofitInstance(retrofit: Retrofit) {
        setRetrofitField("retrofitInstance", retrofit)
    }

    fun initRetrofitForTests(retrofit: Retrofit) {
        resetRetrofitClient()
        setRetrofitField("context", robolectricContext())
        setRetrofitInstance(retrofit)
    }

    private fun setRetrofitField(
        name: String,
        value: Any?,
    ) {
        val field = RetrofitClient::class.java.getDeclaredField(name)
        field.isAccessible = true
        field.set(null, value)
    }
}

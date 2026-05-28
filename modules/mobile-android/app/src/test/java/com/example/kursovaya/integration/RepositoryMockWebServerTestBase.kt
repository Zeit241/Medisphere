package com.example.kursovaya.integration

import android.content.Context
import com.example.kursovaya.repository.RepositoryTestHelper
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
abstract class RepositoryMockWebServerTestBase : MockWebServerTestBase() {
    protected val context: Context
        get() = RepositoryTestHelper.robolectricContext()

    protected fun testRetrofit(): Retrofit =
        Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    protected fun prepareRetrofit() {
        RepositoryTestHelper.initRetrofitForTests(testRetrofit())
    }

    protected inline fun <reified T> bindApi(
        repository: Any,
        fieldName: String,
    ): T {
        val api = testRetrofit().create(T::class.java)
        RepositoryTestHelper.setField(repository, fieldName, api)
        return api
    }

    protected fun bindRetrofitForGetters() {
        RepositoryTestHelper.setRetrofitInstance(testRetrofit())
    }
}

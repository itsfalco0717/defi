package com.tenisturnuva.app.data.network

import android.os.Build
import com.tenisturnuva.app.data.session.SessionManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    /**
     * Bilgisayarinin yerel ag (Wi-Fi) IP adresi — `ipconfig` ile bulunur.
     * SADECE burayi guncellemen yeterli, IP degistiginde.
     */
    private const val COMPUTER_LAN_IP = "192.168.1.52"

    /**
     * Emulator icinde "localhost" bilgisayarin kendisini degil emulatorun
     * kendisini isaret eder — bu yuzden emulator'den host'a ulasmak icin
     * ozel adres 10.0.2.2 kullanilir. Fiziksel bir cihazda (telefon/tablet)
     * ise bilgisayarin gercek yerel ag IP'si gerekir (COMPUTER_LAN_IP).
     *
     * isEmulator kontrolu sayesinde, hangi cihazda calistirdigini elle
     * degistirmene gerek kalmadan dogru adres otomatik secilir.
     */
    private val isEmulator: Boolean by lazy {
        Build.FINGERPRINT.startsWith("generic") ||
                Build.FINGERPRINT.startsWith("unknown") ||
                Build.MODEL.contains("google_sdk") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK built for") ||
                Build.MANUFACTURER.contains("Genymotion") ||
                Build.PRODUCT.contains("sdk") ||
                Build.PRODUCT.contains("emulator") ||
                Build.HARDWARE.contains("goldfish") ||
                Build.HARDWARE.contains("ranchu") ||
                (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
    }

    private val BASE_URL: String by lazy {
        if (isEmulator) "http://10.0.2.2:4000/" else "http://$COMPUTER_LAN_IP:4000/"
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    /** Oturum acikken SessionManager'daki token'i her istege otomatik ekler. */
    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val token = SessionManager.token
        val request = if (token != null) {
            original.newBuilder().addHeader("Authorization", "Bearer $token").build()
        } else {
            original
        }
        chain.proceed(request)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
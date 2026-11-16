package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.gson.JsonArray
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.File

interface YoloApiService {
    @Multipart
    @POST("detect")
    fun detectObject(
        @Part file: MultipartBody.Part
    ): Call<DetectionResponse>
}

data class DetectionResponse(
    val detections: JsonArray? = null
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LookUpScreen()
                }
            }
        }
    }
}

@Composable
fun LookUpScreen() {
    var statusText by remember { mutableStateOf("Pronto para testar a conexão!") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "LookUp App",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                isLoading = true
                statusText = "A enviar imagem de teste para o YOLO..."
                testarServidor { resultado ->
                    statusText = resultado
                    isLoading = false
                }
            },
            enabled = !isLoading
        ) {
            Text(if (isLoading) "A comunicar..." else "Testar Conexão YOLO")
        }
    }
}

fun testarServidor(onResult: (String) -> Unit) {
    val retrofit = Retrofit.Builder()
        .baseUrl("http://127.0.0.1:5000/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api = retrofit.create(YoloApiService::class.java)

    val tempFile = File.createTempFile("teste", ".jpg")
    val dummyJpgData = byteArrayOf(
        0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0x00.toByte(), 0x10.toByte(),
        0x4A.toByte(), 0x46.toByte(), 0x49.toByte(), 0x46.toByte(), 0x00.toByte(), 0x01.toByte(),
        0x01.toByte(), 0x01.toByte(), 0x00.toByte(), 0x48.toByte(), 0x00.toByte(), 0x48.toByte(),
        0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), 0xDB.toByte(), 0x00.toByte(), 0x43.toByte(),
        0x00.toByte(), 0xFF.toByte(), 0xD9.toByte()
    )
    tempFile.writeBytes(dummyJpgData)

    val requestFile = tempFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
    val body = MultipartBody.Part.createFormData("file", tempFile.name, requestFile)

    api.detectObject(body).enqueue(object : Callback<DetectionResponse> {
        override fun onResponse(call: Call<DetectionResponse>, response: Response<DetectionResponse>) {
            if (response.isSuccessful) {
                onResult("CONECTADO COM SUCESSO!\nResposta do YOLO: ${response.body()?.detections}")
            } else {
                onResult("O servidor respondeu, mas deu erro: Código ${response.code()}")
            }
        }

        override fun onFailure(call: Call<DetectionResponse>, t: Throwable) {
            onResult("Falha na conexão: ${t.localizedMessage}")
        }
    })
}
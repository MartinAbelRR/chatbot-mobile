package com.example.dolphinbotv2

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.consumiendoapi.ChatApiService
import com.example.consumiendoapi.Conversacion
import com.example.consumiendoapi.Mensaje
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class HomeActivity : AppCompatActivity() {
    private lateinit var tv1: TextView
    private lateinit var userImageView: ImageView
    private lateinit var editText: EditText
    private lateinit var sendButton: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private val chatMessages = mutableListOf<String>()
    private lateinit var service: ChatApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        init()
        loadConversation() // Cargar la conversación al iniciar
    }

    private fun init() {
        initView()
        configuracion()
        setupRecyclerView()
        setupListeners()
        setupRetrofit()
    }

    private fun initView() {
        tv1 = findViewById(R.id.tv1)
        userImageView = findViewById(R.id.lyimageView)
        editText = findViewById(R.id.editText)
        sendButton = findViewById(R.id.sendButton)
        recyclerView = findViewById(R.id.recyclerViewChat)
    }

    private fun configuracion() {
        val userName = intent.getStringExtra("USER_NAME") ?: "Usuario"
        val userPhotoUrl = intent.getStringExtra("USER_PHOTO")

        tv1.text = getString(R.string.text_bienvenida, userName)

        if (!userPhotoUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(userPhotoUrl)
                .circleCrop()
                .into(userImageView)
        }
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(chatMessages)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = chatAdapter
        recyclerView.visibility = View.GONE
    }

    private fun setupListeners() {
        sendButton.setOnClickListener {
            val message = editText.text.toString().trim()
            if (message.isNotEmpty()) {
                chatMessages.add(message)
                chatAdapter.notifyItemInserted(chatMessages.size - 1)
                recyclerView.scrollToPosition(chatMessages.size - 1)
                editText.text.clear()

                // Ocultar el TextView de bienvenida y mostrar el RecyclerView
                tv1.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE

                // Enviar mensaje a la API y esperar respuesta de la IA
                enviarMensaje(message)
            }
        }
    }

    private fun setupRetrofit() {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.1.60:8001/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        service = retrofit.create(ChatApiService::class.java)
    }

    private fun loadConversation() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = service.getConversacion("23") // Intentamos obtener la conversación

                runOnUiThread {
                    val conversacion = response?.conversacion ?: emptyList() // Evita NullPointerException

                    if (conversacion.isEmpty()) {
                        // ❌ Si no hay conversación, mostrar pantalla inicial
                        tv1.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                    } else if (conversacion.size > 1) {
                        // ✅ Si hay más de un mensaje, cargar mensajes (omitir el primero)
                        chatMessages.clear()
                        chatMessages.addAll(conversacion.drop(1).map { it.contenido })
                        chatAdapter.notifyDataSetChanged()

                        tv1.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                    } else {
                        println("⚠️ La conversación tiene menos de 2 mensajes")
                    }
                }
            } catch (e: retrofit2.HttpException) {
                // ❌ Error de respuesta HTTP (404, 500, etc.)
                runOnUiThread {
                    println("❌ Error HTTP: ${e.code()} - ${e.message()}")
                    tv1.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                }
            } catch (e: java.net.UnknownHostException) {
                // ❌ Error de conexión (sin internet o API caída)
                runOnUiThread {
                    println("❌ No hay conexión a internet o el servidor no responde.")
                    tv1.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                }
            } catch (e: Exception) {
                // ❌ Cualquier otro error inesperado
                runOnUiThread {
                    println("❌ Error inesperado: ${e.message}")
                    tv1.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                }
            }
        }
    }


    private fun enviarMensaje(mensajeUsuario: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val nuevaConversacion = Conversacion(
                    arrayListOf(Mensaje("humano", mensajeUsuario))
                )

                // Enviar mensaje a la API.
                val response = service.postConversacion("23", nuevaConversacion) // ID de la conversación

                // Tomar la última respuesta de la IA (si existe).
                val respuestaIA = response.conversacion.lastOrNull { it.rol == "ai" }?.contenido

                runOnUiThread {
                    if (!respuestaIA.isNullOrEmpty()) {
                        chatMessages.add(respuestaIA)
                        chatAdapter.notifyItemInserted(chatMessages.size - 1)
                        recyclerView.scrollToPosition(chatMessages.size - 1)
                    } else {
                        println("⚠️ La IA no respondió nada.")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread { println("❌ Error al enviar mensaje: ${e.message}") }
            }
        }
    }
}

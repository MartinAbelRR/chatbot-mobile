package com.example.consumiendoapi

import com.google.gson.annotations.SerializedName

data class Mensaje  (
    @SerializedName("rol")
    val rol: String,
    @SerializedName("contenido")
    val contenido: String
    )

data class Conversacion (
    @SerializedName("conversacion")
    val conversacion: ArrayList<Mensaje>
)
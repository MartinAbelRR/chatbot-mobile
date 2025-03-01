package com.example.consumiendoapi

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ChatApiService {
    @GET("redis/")
    suspend fun getConversacion(
        @Query("conversation_id") conversationId: String
    ): Conversacion

    @POST("redis/{conversation_id}")
    suspend fun postConversacion(
        @Path("conversation_id") conversationId: String,
        @Body conversacion: Conversacion
    ): Conversacion

}


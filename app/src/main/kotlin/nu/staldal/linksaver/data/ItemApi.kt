package nu.staldal.linksaver.data

import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ItemApi {
    @GET("./")
    @Headers("Accept: application/json")
    suspend fun getItems(@Query("s") searchTerm: String? = null): List<Item>

    @POST("./")
    @Headers("Accept: application/json")
    @FormUrlEncoded
    suspend fun addLink(@Field("url") url: String): List<Item>

    @POST("./")
    @Headers("Accept: application/json")
    @FormUrlEncoded
    suspend fun addNote(
        @Field("note-title") title: String,
        @Field("note-text") text: String,
    ): List<Item>

    @GET("{id}")
    @Headers("Accept: application/json")
    suspend fun getItem(@Path("id") id: String): Item

    @PATCH("{id}")
    @Headers("Accept: application/json")
    @FormUrlEncoded
    suspend fun updateItem(
        @Path("id") id: String,
        @Field("title") title: String,
        @Field("description") description: String,
    ): Item

    @DELETE("{id}")
    suspend fun deleteItem(@Path("id") id: String)
}

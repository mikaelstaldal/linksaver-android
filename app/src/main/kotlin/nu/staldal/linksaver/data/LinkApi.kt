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

interface LinkApi {
    @GET("./")
    @Headers("Accept: application/json")
    suspend fun getLinks(@Query("s") searchTerm: String? = null): List<Link>

    @POST("./")
    @Headers("Accept: application/json")
    @FormUrlEncoded
    suspend fun addLink(@Field("url") url: String): List<Link>

    @POST("./")
    @Headers("Accept: application/json")
    @FormUrlEncoded
    suspend fun addNote(
        @Field("note-title") title: String,
        @Field("note-text") text: String,
    ): List<Link>

    @GET("{id}")
    @Headers("Accept: application/json")
    suspend fun getLink(@Path("id") id: String): Link

    @PATCH("{id}")
    @Headers("Accept: application/json")
    @FormUrlEncoded
    suspend fun updateLink(
        @Path("id") id: String,
        @Field("title") title: String,
    ): Link

    @DELETE("{id}")
    suspend fun deleteLink(@Path("id") id: String)
}

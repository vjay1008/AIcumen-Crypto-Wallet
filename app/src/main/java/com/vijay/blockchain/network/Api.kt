package com.vijay.blockchain.network

import android.net.Uri
import com.vijay.blockchain.model.email.EmailResponse
import com.vijay.blockchain.model.login.ProfileResponse
import com.vijay.blockchain.model.posts.req.registerUpload.MainRequest
import com.vijay.blockchain.model.posts.req.ugcPosts.PostRequest
import com.vijay.blockchain.model.posts.res.PostResponse
import com.vijay.blockchain.model.posts.res.RegisterUploadResponse
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface Api {
    @GET("v2/me?projection=(id,firstName,lastName,profilePicture(displayImage~:playableStreams))")
    fun getProfile(): Call<ProfileResponse>

    @GET("v2/emailAddress?q=members&projection=(elements*(handle~))")
    fun getEmail(): Call<EmailResponse>

    @POST("v2/ugcPosts")
    fun postMessage(@Body req: PostRequest): Call<PostResponse>

    @POST("v2/assets?action=registerUpload")
    fun registerUpload(@Body req: MainRequest): Call<RegisterUploadResponse>

    @POST
    fun uploadMedia(
        @Url url: Uri,
        @Body file: RequestBody
    ): Call<Void>


}
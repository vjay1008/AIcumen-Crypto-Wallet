package com.vijay.blockchain.model.posts.res

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.vijay.blockchain.model.posts.res.MediaUploadHttpRequest
import java.io.Serializable

data class UploadMechanism(
    @Expose
    @SerializedName("com.linkedin.digitalmedia.uploading.MediaUploadHttpRequest")
    val mediaUploadHttpRequest: MediaUploadHttpRequest
) : Serializable
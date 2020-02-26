package com.vijay.blockchain.model.posts.req.ugcPosts

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.vijay.blockchain.model.posts.req.ugcPosts.ShareContent
import java.io.Serializable

data class SpecificContent(
    @Expose
    @SerializedName("com.linkedin.ugc.ShareContent")
    var shareContent: ShareContent?
):Serializable
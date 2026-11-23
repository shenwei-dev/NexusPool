package com.family.chat.api;

import com.family.chat.protobuf.MessageProto;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface MessageApi {

    @GET("/api/chat/sync/{friendId}")
    @Headers("Accept: application/x-protobuf")
    Call<MessageProto.QueryHistoryResponse> syncDeltaMessages(
            @Path("friendId") Long friendId,
            @Query("localMaxSeq") Long localMaxSeq
    );
}

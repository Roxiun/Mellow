package com.roxiun.mellow.support;

import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public final class HttpResponses {

    private HttpResponses() {}

    public static Response response(Request request, String body) {
        return response(request, 200, body);
    }

    public static Response response(Request request, int code, String body) {
        return response(request, code, "application/json", body);
    }

    public static Response response(Request request, int code, String contentType, String body) {
        return new Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(code == 200 ? "OK" : "Error")
            .body(ResponseBody.create(MediaType.parse(contentType), body))
            .build();
    }
}

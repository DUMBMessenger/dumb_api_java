package com.dumbmessenger.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import okhttp3.*;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class DClient {
    private final String baseUrl;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final GoogleAuthenticator authenticator;
    private String authToken;
    private MessengerWebSocketClient webSocketClient;
    private final Map<String, List<Consumer<Message>>> messageListeners;
    private final List<Consumer<WebSocketEvent>> eventListeners;

    public DClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        this.httpClient = new OkHttpClient.Builder().build();
        this.objectMapper = new ObjectMapper();
        this.authenticator = new GoogleAuthenticator();
        this.messageListeners = new ConcurrentHashMap<>();
        this.eventListeners = new ArrayList<>();
    }


    public AuthResponse register(String username, String password) throws IOException {
        Map<String, String> request = Map.of(
            "username", username,
            "password", password
        );

        RequestBody body = RequestBody.create(
            objectMapper.writeValueAsString(request),
            MediaType.parse("application/json")
        );

        Request httpRequest = new Request.Builder()
            .url(baseUrl + "api/register")
            .post(body)
            .build();

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            return objectMapper.readValue(response.body().string(), AuthResponse.class);
        }
    }

    public AuthResponse login(String username, String password, String twoFactorToken) throws IOException {
        Map<String, String> request = new HashMap<>();
        request.put("username", username);
        request.put("password", password);
        if (twoFactorToken != null) {
            request.put("twoFactorToken", twoFactorToken);
        }

        RequestBody body = RequestBody.create(
            objectMapper.writeValueAsString(request),
            MediaType.parse("application/json")
        );

        Request httpRequest = new Request.Builder()
            .url(baseUrl + "api/login")
            .post(body)
            .build();

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            AuthResponse authResponse = objectMapper.readValue(response.body().string(), AuthResponse.class);
            if (authResponse.success && authResponse.token != null) {
                this.authToken = authResponse.token;
            }
            return authResponse;
        }
    }

    public TwoFAResponse setup2FA() throws IOException {
        Request httpRequest = new Request.Builder()
            .url(baseUrl + "api/2fa/setup")
            .header("Authorization", "Bearer " + authToken)
            .post(RequestBody.create("", null))
            .build();

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            return objectMapper.readValue(response.body().string(), TwoFAResponse.class);
        }
    }

    public AuthResponse enable2FA(String token) throws IOException {
        Map<String, String> request = Map.of("token", token);

        RequestBody body = RequestBody.create(
            objectMapper.writeValueAsString(request),
            MediaType.parse("application/json")
        );

        Request httpRequest = new Request.Builder()
            .url(baseUrl + "api/2fa/enable")
            .header("Authorization", "Bearer " + authToken)
            .post(body)
            .build();

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            return objectMapper.readValue(response.body().string(), AuthResponse.class);
        }
    }


    public String generateTOTPSecret() {
        GoogleAuthenticatorKey key = authenticator.createCredentials();
        return key.getKey();
    }

    public boolean verifyTOTP(String secret, String token) {
        return authenticator.authorize(secret, Integer.parseInt(token));
    }


    public ChannelResponse createChannel(String name, String customId) throws IOException {
        Map<String, String> request = new HashMap<>();
        request.put("name", name);
        if (customId != null) {
            request.put("customId", customId);
        }

        RequestBody body = RequestBody.create(
            objectMapper.writeValueAsString(request),
            MediaType.parse("application/json")
        );

        Request httpRequest = new Request.Builder()
            .url(baseUrl + "api/channels/create")
            .header("Authorization", "Bearer " + authToken)
            .post(body)
            .build();

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            return objectMapper.readValue(response.body().string(), ChannelResponse.class);
        }
    }

    public ChannelListResponse getChannels() throws IOException {
        Request httpRequest = new Request.Builder()
            .url(baseUrl + "api/channels")
            .header("Authorization", "Bearer " + authToken)
            .get()
            .build();

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            return objectMapper.readValue(response.body().string(), ChannelListResponse.class);
        }
    }

    public MessageResponse sendMessage(String channel, String text, String replyTo, 
                                     boolean encrypt) throws IOException {
        Map<String, Object> request = new HashMap<>();
        request.put("channel", channel);
        request.put("text", text);
        if (replyTo != null) {
            request.put("replyTo", replyTo);
        }
        request.put("encrypt", encrypt);

        RequestBody body = RequestBody.create(
            objectMapper.writeValueAsString(request),
            MediaType.parse("application/json")
        );

        Request httpRequest = new Request.Builder()
            .url(baseUrl + "api/message")
            .header("Authorization", "Bearer " + authToken)
            .post(body)
            .build();

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            return objectMapper.readValue(response.body().string(), MessageResponse.class);
        }
    }

    public MessageListResponse getMessages(String channel, int limit, String before) throws IOException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl + "api/messages").newBuilder()
            .addQueryParameter("channel", channel)
            .addQueryParameter("limit", String.valueOf(limit));
        
        if (before != null) {
            urlBuilder.addQueryParameter("before", before);
        }

        Request httpRequest = new Request.Builder()
            .url(urlBuilder.build())
            .header("Authorization", "Bearer " + authToken)
            .get()
            .build();

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            return objectMapper.readValue(response.body().string(), MessageListResponse.class);
        }
    }


    public FileUploadResponse uploadFile(File file) throws IOException {
        RequestBody requestBody = new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.getName(),
                RequestBody.create(file, MediaType.parse("application/octet-stream")))
            .build();

        Request request = new Request.Builder()
            .url(baseUrl + "api/upload/file")
            .header("Authorization", "Bearer " + authToken)
            .post(requestBody)
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            return objectMapper.readValue(response.body().string(), FileUploadResponse.class);
        }
    }

    public FileUploadResponse uploadAvatar(File imageFile) throws IOException {
        RequestBody requestBody = new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("avatar", imageFile.getName(),
                RequestBody.create(imageFile, MediaType.parse("image/*")))
            .build();

        Request request = new Request.Builder()
            .url(baseUrl + "api/upload/avatar")
            .header("Authorization", "Bearer " + authToken)
            .post(requestBody)
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            return objectMapper.readValue(response.body().string(), FileUploadResponse.class);
        }
    }

    public CompletableFuture<Boolean> downloadFile(String filename, File destination) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Request request = new Request.Builder()
                    .url(baseUrl + "api/download/" + filename)
                    .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        byte[] fileBytes = response.body().bytes();
                        java.nio.file.Files.write(destination.toPath(), fileBytes);
                        return true;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        });
    }


    public AuthResponse sendVerificationEmail(String email) throws IOException {
        Map<String, String> request = Map.of("email", email);

        RequestBody body = RequestBody.create(
            objectMapper.writeValueAsString(request),
            MediaType.parse("application/json")
        );

        Request httpRequest = new Request.Builder()
            .url(baseUrl + "api/email/send-verification")
            .header("Authorization", "Bearer " + authToken)
            .post(body)
            .build();

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            return objectMapper.readValue(response.body().string(), AuthResponse.class);
        }
    }

    public AuthResponse verifyEmail(String email, String code) throws IOException {
        Map<String, String> request = Map.of("email", email, "code", code);

        RequestBody body = RequestBody.create(
            objectMapper.writeValueAsString(request),
            MediaType.parse("application/json")
        );

        Request httpRequest = new Request.Builder()
            .url(baseUrl + "api/email/verify")
            .header("Authorization", "Bearer " + authToken)
            .post(body)
            .build();

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            return objectMapper.readValue(response.body().string(), AuthResponse.class);
        }
    }

    public AuthResponse requestPasswordReset(String email) throws IOException {
        Map<String, String> request = Map.of("email", email);

        RequestBody body = RequestBody.create(
            objectMapper.writeValueAsString(request),
            MediaType.parse("application/json")
        );

        Request httpRequest = new Request.Builder()
            .url(baseUrl + "api/auth/reset-password")
            .post(body)
            .build();

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            return objectMapper.readValue(response.body().string(), AuthResponse.class);
        }
    }


    public void connectWebSocket() {
        if (webSocketClient != null) {
            webSocketClient.close();
        }

        String wsUrl = baseUrl.replace("http", "ws") + "api/events?token=" + authToken;
        webSocketClient = new MessengerWebSocketClient(URI.create(wsUrl));
        webSocketClient.connect();
    }

    public void disconnectWebSocket() {
        if (webSocketClient != null) {
            webSocketClient.close();
            webSocketClient = null;
        }
    }

    public void addMessageListener(String channel, Consumer<Message> listener) {
        messageListeners.computeIfAbsent(channel, k -> new ArrayList<>()).add(listener);
    }

    public void addEventListener(Consumer<WebSocketEvent> listener) {
        eventListeners.add(listener);
    }

    public boolean isWebSocketConnected() {
        return webSocketClient != null && webSocketClient.isOpen();
    }


    private class MessengerWebSocketClient extends WebSocketClient {
        public MessengerWebSocketClient(URI serverUri) {
            super(serverUri);
        }

        @Override
        public void onOpen(ServerHandshake handshake) {
            WebSocketEvent event = new WebSocketEvent("connected", "WebSocket connected successfully");
            eventListeners.forEach(listener -> listener.accept(event));
        }

        @Override
        public void onMessage(String message) {
            try {
                WebSocketEvent event = objectMapper.readValue(message, WebSocketEvent.class);
                
                if ("message".equals(event.type) && "new".equals(event.action)) {
                    Message msg = objectMapper.convertValue(event.data, Message.class);
                    List<Consumer<Message>> listeners = messageListeners.get(msg.channel);
                    if (listeners != null) {
                        listeners.forEach(listener -> listener.accept(msg));
                    }
                }
                
                eventListeners.forEach(listener -> listener.accept(event));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            WebSocketEvent event = new WebSocketEvent("disconnected", "WebSocket disconnected: " + reason);
            eventListeners.forEach(listener -> listener.accept(event));
        }

        @Override
        public void onError(Exception ex) {
            WebSocketEvent event = new WebSocketEvent("error", "WebSocket error: " + ex.getMessage());
            eventListeners.forEach(listener -> listener.accept(event));
        }
    }


    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }
}

package com.dumbmessenger.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
class BaseResponse {
    public boolean success;
    public String error;
    public String message;
}

class AuthResponse extends BaseResponse {
    public String token;
    public boolean requires2FA;
    public String sessionId;
    public boolean twoFactorEnabled;
}

class TwoFAResponse extends BaseResponse {
    public String secret;
    public String qrCodeUrl;
}

class ChannelResponse extends BaseResponse {
    public String channelId;
    public String channel;
}

class ChannelListResponse extends BaseResponse {
    public List<Channel> channels;
}

class Channel {
    public String id;
    public String name;
    public String creator;
    public long createdAt;
    public boolean customId;
}

class MessageResponse extends BaseResponse {
    public Message message;
}

class MessageListResponse extends BaseResponse {
    public List<Message> messages;
}

class Message {
    public String id;
    public String from;
    public String channel;
    public String text;
    public long ts;
    public String replyTo;
    public FileAttachment file;
    public VoiceAttachment voice;
    public boolean encrypted;
    public Message replyToMessage;
}

class FileAttachment {
    public String filename;
    public String originalName;
    public String mimetype;
    public long size;
    public String downloadUrl;
}

class VoiceAttachment {
    public String filename;
    public int duration;
    public String downloadUrl;
}

class FileUploadResponse extends BaseResponse {
    public UploadedFile file;
    public String filename;
    public String avatarUrl;
    public String mimeType;
}

class UploadedFile {
    public String id;
    public String filename;
    public String originalName;
    public String mimetype;
    public long size;
    public long uploadedAt;
    public String uploadedBy;
    public String downloadUrl;
}

class WebSocketEvent {
    public String type;
    public String action;
    public String clientId;
    public String message;
    public Map<String, Object> data;

    public WebSocketEvent() {}

    public WebSocketEvent(String type, String message) {
        this.type = type;
        this.message = message;
    }
}

class TOTPData {
    public String secret;
    public String qrCodeUrl;

    public TOTPData(String secret, String qrCodeUrl) {
        this.secret = secret;
        this.qrCodeUrl = qrCodeUrl;
    }
}

package com.dumbmessenger.Models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

public class Models {
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BaseResponse {
        public boolean success;
        public String error;
        public String message;
    }

    public static class AuthResponse extends BaseResponse {
        public String token;
        public boolean requires2FA;
        public String sessionId;
        public boolean twoFactorEnabled;
    }

    public static class TwoFAResponse extends BaseResponse {
        public String secret;
        public String qrCodeUrl;
    }

    public static class ChannelResponse extends BaseResponse {
        public String channelId;
        public String channel;
    }

    public static class ChannelListResponse extends BaseResponse {
        public List<Channel> channels;
    }

    public static class ChannelMembersResponse extends BaseResponse {
        public List<String> members;
    }

    public static class Channel {
        public String id;
        public String name;
        public String creator;
        public long createdAt;
        public boolean customId;
    }

    public static class MessageResponse extends BaseResponse {
        public Message message;
    }

    public static class MessageListResponse extends BaseResponse {
        public List<Message> messages;
    }

    public static class Message {
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

    public static class FileAttachment {
        public String filename;
        public String originalName;
        public String mimetype;
        public long size;
        public String downloadUrl;
    }

    public static class VoiceAttachment {
        public String filename;
        public int duration;
        public String downloadUrl;
    }

    public static class FileUploadResponse extends BaseResponse {
        public UploadedFile file;
        public String filename;
        public String avatarUrl;
        public String mimeType;
    }

    public static class VoiceUploadResponse extends BaseResponse {
        public String voiceId;
        public String uploadUrl;
    }

    public static class UploadedFile {
        public String id;
        public String filename;
        public String originalName;
        public String mimetype;
        public long size;
        public long uploadedAt;
        public String uploadedBy;
        public String downloadUrl;
    }

    public static class WebSocketEvent {
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

    public static class TOTPData {
        public String secret;
        public String qrCodeUrl;

        public TOTPData(String secret, String qrCodeUrl) {
            this.secret = secret;
            this.qrCodeUrl = qrCodeUrl;
        }
    }

    public static class User {
        public String username;
        public String avatar;
    }

    public static class UserListResponse extends BaseResponse {
        public List<User> users;
    }
}

Java API Lib
## How install?
1. Clone Repository
   ``` shell
   git clone https://github.com/dumbmessenger/dumb_api_java
   ```
2. Install it local with maven
   ``` shell
   mvn install
   ```
3. Add to gradle or maven

build.gradle
``` gradle
repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {                                                                  implementation 'com.dumbmessenger:java-api:1.0.0'
    implementation 'com.dumbmessenger:java-api:1.0.0'
}
                                                              
```

pom.xml
``` xml
<dependencies>
    <dependency>
        <groupId>com.dumbmessenger</groupId>
        <artifactId>java-api</artifactId>
        <version>1.0.0</version>
    </dependency>
    
</dependencies>
```
# Example
``` java
package com.dumbmessenger.example;

import com.dumbmessenger.Client;
import com.dumbmessenger.Models;
import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

public class ExampleUsage {
    
    public static void main(String[] args) {
        // Initialize client with server URL
        Client client = new Client("http://localhost:8080/");
        
        try {
            // Example 1: User Registration and Authentication
            authenticationExample(client);
            
            // Example 2: Channel Management
            channelManagementExample(client);
            
            // Example 3: Messaging
            messagingExample(client);
            
            // Example 4: File Upload
            fileUploadExample(client);
            
            // Example 5: Real-time WebSocket Communication
            websocketExample(client);
            
            // Example 6: Two-Factor Authentication
            twoFactorExample(client);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void authenticationExample(Client client) throws IOException {
        System.out.println("=== AUTHENTICATION EXAMPLE ===");
        
        // Register a new user
        Models.AuthResponse registerResponse = client.register("john_doe", "securepassword123");
        if (registerResponse.success) {
            System.out.println("Registration successful!");
        } else {
            System.out.println("Registration failed: " + registerResponse.error);
        }
        
        // Login with credentials
        Models.AuthResponse loginResponse = client.login("john_doe", "securepassword123", null);
        if (loginResponse.success) {
            System.out.println("Login successful! Token: " + client.getAuthToken());
        } else if (loginResponse.requires2FA) {
            System.out.println("2FA required. Session ID: " + loginResponse.sessionId);
            // Handle 2FA flow here
        } else {
            System.out.println("Login failed: " + loginResponse.error);
        }
        
        // Check if user is authenticated
        System.out.println("Is authenticated: " + client.isAuthenticated());
    }
    
    private static void channelManagementExample(Client client) throws IOException {
        System.out.println("\n=== CHANNEL MANAGEMENT EXAMPLE ===");
        
        // Create a new channel
        Models.ChannelResponse createResponse = client.createChannel("General Chat", "general");
        if (createResponse.success) {
            System.out.println("Channel created: " + createResponse.channelId);
        }
        
        // Get all channels
        Models.ChannelListResponse channelsResponse = client.getChannels();
        if (channelsResponse.success) {
            System.out.println("Available channels:");
            for (Models.Channel channel : channelsResponse.channels) {
                System.out.println(" - " + channel.name + " (ID: " + channel.id + ")");
            }
        }
        
        // Join a channel
        Models.AuthResponse joinResponse = client.joinChannel("general");
        if (joinResponse.success) {
            System.out.println("Successfully joined channel");
        }
        
        // Get channel members
        Models.ChannelMembersResponse membersResponse = client.getChannelMembers("general");
        if (membersResponse.success) {
            System.out.println("Channel members: " + membersResponse.members);
        }
    }
    
    private static void messagingExample(Client client) throws IOException {
        System.out.println("\n=== MESSAGING EXAMPLE ===");
        
        // Send a simple text message
        Models.MessageResponse messageResponse = client.sendMessage("general", "Hello everyone!", null, false);
        if (messageResponse.success) {
            System.out.println("Message sent with ID: " + messageResponse.message.id);
        }
        
        // Send a reply to previous message
        String replyToId = messageResponse.message.id;
        Models.MessageResponse replyResponse = client.sendMessage("general", "This is a reply!", replyToId, false);
        
        // Get recent messages from channel
        Models.MessageListResponse messagesResponse = client.getMessages("general", 10, null);
        if (messagesResponse.success) {
            System.out.println("Recent messages:");
            for (Models.Message msg : messagesResponse.messages) {
                System.out.println("[" + msg.from + "]: " + msg.text);
                if (msg.replyTo != null) {
                    System.out.println("  (Reply to: " + msg.replyTo + ")");
                }
            }
        }
    }
    
    private static void fileUploadExample(Client client) throws IOException {
        System.out.println("\n=== FILE UPLOAD EXAMPLE ===");
        
        // Upload a file
        File sampleFile = new File("document.pdf");
        if (sampleFile.exists()) {
            Models.FileUploadResponse uploadResponse = client.uploadFile(sampleFile);
            if (uploadResponse.success) {
                System.out.println("File uploaded: " + uploadResponse.file.downloadUrl);
                
                // The uploaded file can now be attached to messages
                client.sendMessage("general", "Check out this document!", null, false);
            }
        }
        
        // Upload avatar image
        File avatarFile = new File("avatar.jpg");
        if (avatarFile.exists()) {
            Models.FileUploadResponse avatarResponse = client.uploadAvatar(avatarFile);
            if (avatarResponse.success) {
                System.out.println("Avatar uploaded: " + avatarResponse.avatarUrl);
            }
        }
    }
    
    private static void websocketExample(Client client) {
        System.out.println("\n=== WEBSOCKET EXAMPLE ===");
        
        // Add event listener for WebSocket events
        client.addEventListener(new Consumer<Models.WebSocketEvent>() {
            @Override
            public void accept(Models.WebSocketEvent event) {
                System.out.println("WebSocket Event: " + event.type + " - " + event.message);
            }
        });
        
        // Add message listener for specific channel
        client.addMessageListener("general", new Consumer<Models.Message>() {
            @Override
            public void accept(Models.Message message) {
                System.out.println("New message in general: [" + message.from + "] " + message.text);
            }
        });
        
        // Connect to WebSocket for real-time updates
        client.connectWebSocket();
        System.out.println("WebSocket connected: " + client.isWebSocketConnected());
        
        // Keep the connection alive for a while to receive messages
        try {
            Thread.sleep(5000); // Wait 5 seconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Disconnect when done
        client.disconnectWebSocket();
    }
    
    private static void twoFactorExample(Client client) throws IOException {
        System.out.println("\n=== TWO-FACTOR AUTHENTICATION EXAMPLE ===");
        
        // Setup 2FA
        Models.TwoFAResponse setupResponse = client.setup2FA();
        if (setupResponse.success) {
            System.out.println("2FA Setup - Secret: " + setupResponse.secret);
            System.out.println("QR Code URL: " + setupResponse.qrCodeUrl);
            
            // Generate TOTP code using the secret
            String totpCode = client.getTOTPCode(setupResponse.secret);
            System.out.println("Current TOTP code: " + totpCode);
            
            // Enable 2FA with the generated code
            Models.AuthResponse enableResponse = client.enable2FA(totpCode);
            if (enableResponse.success) {
                System.out.println("2FA enabled successfully");
            }
        }
        
        // Check 2FA status
        Models.AuthResponse statusResponse = client.get2FAStatus();
        if (statusResponse.success && statusResponse.twoFactorEnabled) {
            System.out.println("2FA is currently enabled");
        }
    }
}
```

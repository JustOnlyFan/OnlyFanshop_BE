package com.example.onlyfanshop_be.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void init() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            // Try to load from classpath first, then from file system
            InputStream serviceAccount = null;
            
            try {
                // Try loading from classpath (resources folder)
                serviceAccount = getClass().getClassLoader().getResourceAsStream("onlyfan-f9406-firebase-adminsdk-fbsvc-80882fd511.json");
                if (serviceAccount == null) {
                    // If not found in classpath, try from file system (project root)
                    serviceAccount = new FileInputStream("onlyfan-f9406-firebase-adminsdk-fbsvc-80882fd511.json");
                }
            } catch (Exception e) {
                System.err.println("⚠️ Warning: Could not load Firebase credentials. Image upload will not work.");
                System.err.println("Please ensure the Firebase JSON file exists in classpath or project root.");
                return;
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setStorageBucket("onlyfan-f9406.firebasestorage.app")
                    .setDatabaseUrl("https://onlyfan-f9406-default-rtdb.asia-southeast1.firebasedatabase.app")
                    .build();

            FirebaseApp.initializeApp(options);
        }
    }

    @Bean
    public DatabaseReference firebaseDatabase() {
        return FirebaseDatabase.getInstance().getReference();
    }
}

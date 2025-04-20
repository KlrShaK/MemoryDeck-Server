package ch.uzh.ifi.hase.soprafs24.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import java.io.IOException;
import java.util.Base64;

@Configuration
public class GoogleCloudConfig {

    @Bean
    public Storage googleStorage() throws IOException {

        GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();

        System.out.println("✅ GCP_SERVICE_CREDENTIALS is set.");
        System.out.println("🔍 Checking GCP_SERVICE_CREDENTIALS: " + credentials.toString());

        Storage storage = StorageOptions.newBuilder().setCredentials(credentials).build().getService();
        // Load credentials and create the Storage client
        return storage;
    
    } 
}

// @Configuration
// public class GoogleCloudConfig {

//     @Bean
//     public Storage googleStorage() throws IOException {
//         String base64Credentials = System.getenv("GCP_SERVICE_CREDENTIALS");

//         GoogleCredentials credentials;
//         if (base64Credentials != null && !base64Credentials.isEmpty()) {
//             System.out.println("✅ Loading GCP credentials from GCP_SERVICE_CREDENTIALS env var");
//             byte[] decodedBytes = Base64.getDecoder().decode(base64Credentials);
//             try (InputStream credentialsStream = new ByteArrayInputStream(decodedBytes)) {
//                 credentials = GoogleCredentials.fromStream(credentialsStream);
//             }
//         } else {
//             System.out.println("⚠️ GCP_SERVICE_CREDENTIALS not set, using Application Default Credentials");
//             credentials = GoogleCredentials.getApplicationDefault();
//         }

//         return StorageOptions.newBuilder()
//                 .setCredentials(credentials)
//                 .build()
//                 .getService();
//     }
// }

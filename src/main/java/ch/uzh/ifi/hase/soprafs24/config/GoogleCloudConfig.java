package ch.uzh.ifi.hase.soprafs24.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import java.io.IOException;

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

package com.coze.jwt.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppConfig {
    @JsonProperty("client_id")
    private String clientId;

    @JsonProperty("client_secret")
    private String clientSecret;
    
    @JsonProperty("private_key")
    private String privateKey;
    
    @JsonProperty("public_key_id")
    private String publicKeyId;

    @JsonProperty("coze_api_base")
    private String cozeApiBase;

    private final String redirectUri = "http://localhost:8080/callback";

    private static volatile AppConfig instance;

    public static AppConfig load(String configFilePath) {
        if (instance == null) {
            synchronized (AppConfig.class) {
                if (instance == null) {
                    instance = _load(configFilePath);
                }
            }
        }
        return instance;
    }

    public static AppConfig load() {
        String configFilePath = "config.yaml";
        return load(configFilePath);
    }

    private static AppConfig _load(String configFilePath) {
        try (InputStream inputStream = new FileInputStream(configFilePath)) {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            mapper.findAndRegisterModules();

            // First read into Map
            Map<String, Object> config = mapper.readValue(inputStream, new TypeReference<Map<String, Object>>() {});

            // Get the content of the 'app' node
            Map<String, Object> appConfig = (Map<String, Object>) config.get("app");
            if (appConfig == null) {
                throw new IllegalArgumentException("Missing 'app' configuration node in config file");
            }

            // Convert the 'app' node content to AppConfig object
            return mapper.convertValue(appConfig, AppConfig.class);

        } catch (FileNotFoundException e) {
            throw new RuntimeException("Config file not found: " + configFilePath, e);
        } catch (StreamReadException e) {
            throw new RuntimeException("YAML file format error: " + e.getMessage(), e);
        } catch (DatabindException e) {
            throw new RuntimeException("Failed to map config file to object: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException("I/O error while reading config file: " + e.getMessage(), e);
        }
    }
}
package com.coze.device.config;

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
    @JsonProperty("client_type")
    private String clientType;

    @JsonProperty("client_id")
    private String clientId;

    @JsonProperty("coze_api_base")
    private String cozeApiBase;

    @JsonProperty("coze_www_base")
    private String cozeWwwBase;

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
        String configFilePath = "coze_oauth_config.json";
        return load(configFilePath);
    }

    private static AppConfig _load(String configFilePath) {
        try (InputStream inputStream = new FileInputStream(configFilePath)) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.findAndRegisterModules();

            // 直接读取 JSON 文件到 AppConfig 对象
            return mapper.readValue(inputStream, AppConfig.class);

        } catch (FileNotFoundException e) {
            throw new RuntimeException("Config file not found: " + configFilePath, e);
        } catch (StreamReadException e) {
            throw new RuntimeException("JSON file format error: " + e.getMessage(), e);
        } catch (DatabindException e) {
            throw new RuntimeException("Failed to map config file to object: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException("I/O error while reading config file: " + e.getMessage(), e);
        }
    }
}
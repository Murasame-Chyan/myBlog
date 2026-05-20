package com.murasame.config;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.region.Region;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "tencent.cos")
@Data
public class TencentCosConfig {
    private String secretId;
    private String secretKey;
    private String region;
    private String bucketName;
    private String baseUrl;
    private String folder;

    @Bean
    public COSClient cosClient() {
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        Region regionObj = new Region(region);
        ClientConfig clientConfig = new ClientConfig(regionObj);
        return new COSClient(cred, clientConfig);
    }
}

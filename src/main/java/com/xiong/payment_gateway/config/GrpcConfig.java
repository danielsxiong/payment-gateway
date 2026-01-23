package com.xiong.payment_gateway.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcConfig {
    // gRPC configuration is handled automatically by grpc-spring-boot-starter
    // The server will start on the configured port (default 9090)
    // All @GrpcService annotated beans will be registered automatically
}

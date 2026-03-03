package io.caudal.server;

import io.caudal.core.BucketClock;
import io.caudal.core.MemoryEngine;
import io.caudal.core.SpaceConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CaudalProperties.class)
public class CaudalConfig {

    @Bean
    public MemoryEngine memoryEngine() {
        return new MemoryEngine();
    }

    @Bean
    public BucketClock bucketClock(CaudalProperties props) {
        return new BucketClock(props.bucketSizeSeconds());
    }

    @Bean
    public SpaceConfig defaultSpaceConfig(CaudalProperties props) {
        return new SpaceConfig(
                props.maxNodes(),
                props.maxEdges(),
                props.minScoreToKeep(),
                props.decayPerBucket(),
                props.depositScale(),
                props.alpha()
        );
    }
}

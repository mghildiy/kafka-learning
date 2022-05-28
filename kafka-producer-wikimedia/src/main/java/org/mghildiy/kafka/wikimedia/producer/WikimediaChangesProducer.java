package org.mghildiy.kafka.wikimedia.producer;

import com.launchdarkly.eventsource.EventHandler;
import com.launchdarkly.eventsource.EventSource;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class WikimediaChangesProducer {
    private static final Logger LOGGER = LoggerFactory.getLogger(WikimediaChangesProducer.class.getSimpleName());
    private static final String SERVERS = "kafka-learning-ghildiyal-da9a.aivencloud.com:15905";
    private static final String TOPIC = "wikimedia.recentchange";

    public static void main(String[] args) throws InterruptedException {
        KafkaProducer<String, String> producer = new KafkaProducer(config());

        String source = "https://stream.wikimedia.org/v2/stream/recentchange";
        EventHandler eventHandler = new WikimediaChangeHandler(producer, TOPIC);
        EventSource eventSource = new EventSource.Builder(eventHandler, URI.create(source)).build();
        eventSource.start();

        TimeUnit.MINUTES.sleep(10);
    }

    private static Properties config() {
        Properties config = new Properties();
        config.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, SERVERS);
        config.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        config.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        config.setProperty(ProducerConfig.LINGER_MS_CONFIG, "20");
        config.setProperty(ProducerConfig.BATCH_SIZE_CONFIG, Integer.toString(32 * 1024));
        config.setProperty(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        // avien access details
        config.put("security.protocol", "SSL");
        config.put("ssl.truststore.location", "D:\\work\\learning\\kafka\\aiven\\client.truststore.jks");
        config.put("ssl.truststore.password", "aiven_truststore");
        config.put("ssl.keystore.type", "PKCS12");
        config.put("ssl.keystore.location", "D:\\work\\learning\\kafka\\aiven\\client.keystore.p12");
        config.put("ssl.keystore.password", "aiven_keystore");
        config.put("ssl.key.password", "aiven_keystore");

        return config;
    }
}

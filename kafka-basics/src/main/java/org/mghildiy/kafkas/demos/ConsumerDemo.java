package org.mghildiy.kafkas.demos;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ConsumerDemo<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumerDemo.class.getSimpleName());
    private static final String TOPIC = "kafka_basics_demo";
    private static final String GROUP = "kafka_basics_demo_consumer";
    private final KafkaConsumer<String, T> consumer;

    ConsumerDemo(KafkaConsumer<String, T> consumer) {
        this.consumer = consumer;
    }

    public static void main(String[] args) throws Exception{
        LOGGER.info("Consumer demo starts");

        KafkaConsumer<String, String> consumer = new KafkaConsumer(loadConfig(null));
        consumer.subscribe(Arrays.asList(TOPIC));
        Integer counter = 0;
        while(true) {
            LOGGER.info("Polling starts");
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
            counter = counter + records.count();
            records.forEach(record -> {
                LOGGER.info("KEY: "+ record.key() + ", VALUE: "+ record.value());
                LOGGER.info("Partition: " + record.partition());
                LOGGER.info("Offset: "+ record.offset());
            });
            LOGGER.info("Polling ends");
            LOGGER.info("Number of records received so far: "+ counter);
        }

        //LOGGER.info("Consumer demo ENDS");
    }

    private static Stream<String> messages() {
        return IntStream.rangeClosed(1, 100)
                .mapToObj(i -> "Test Message another:"+i);
    }

    private static Properties loadConfig(String configFile) {
        final Properties cfg = new Properties();
        if(configFile != null) {
            InputStream inputStream = ConsumerDemo.class.getClassLoader().getResourceAsStream(configFile);
            if (inputStream == null)
                throw new IllegalArgumentException("file not found! " + configFile);
            try {
                cfg.load(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            cfg.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        }
        cfg.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        cfg.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        cfg.setProperty(ConsumerConfig.GROUP_ID_CONFIG, GROUP);
        cfg.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return cfg;
    }
}

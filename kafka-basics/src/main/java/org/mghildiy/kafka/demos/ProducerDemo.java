package org.mghildiy.kafka.demos;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ProducerDemo<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProducerDemo.class.getSimpleName());
    private static final String TOPIC = "kafka_basics_demo";
    private final KafkaProducer<String, T> producer;

    ProducerDemo(KafkaProducer<String, T> producer) {
        this.producer = producer;
    }

    public static void main(String[] args) throws Exception{
        LOGGER.info("Producer demo starts");

        // String configFile = "kafka-cloud.properties"
        KafkaProducer<String, String> producer = new KafkaProducer(loadConfig(null));
        ProducerDemo pd = new ProducerDemo(producer);
        messages().forEach(pd::dispatch);
        producer.close();

        LOGGER.info("Demo ENDS");
    }

    private void dispatch(T data) {
        ProducerRecord<String, T> record = new ProducerRecord(TOPIC, data);
        LOGGER.info("Sending:"+ data);
        producer.send(record);
    }

    private static Stream<String> messages() {
        return IntStream.rangeClosed(1, 100)
                .mapToObj(i -> "Test Message another:"+i);
    }

    private static Properties loadConfig(String configFile) {
        final Properties cfg = new Properties();
        if(configFile != null) {
            InputStream inputStream = ProducerDemo.class.getClassLoader().getResourceAsStream(configFile);
            if (inputStream == null)
                throw new IllegalArgumentException("file not found! " + configFile);
            try {
                cfg.load(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            cfg.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        }
        cfg.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        cfg.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        return cfg;
    }
}

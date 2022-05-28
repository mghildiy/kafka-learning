package open.mghildiy.kafka.opensearch.consumer;

import com.google.gson.JsonParser;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.indices.CreateIndexRequest;
import org.opensearch.client.indices.GetIndexRequest;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Properties;

public class OpenSearchConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenSearchConsumer.class.getSimpleName());
    private static Properties props;

    public static void main(String[] args) throws IOException {
        RestHighLevelClient openSearchClient = createOpenSearchClient();
        KafkaConsumer<String, String> kafkaConsumer = createKafkaConsumer();
        kafkaConsumer.subscribe(List.of(props.getProperty("topic")));

        try (openSearchClient; kafkaConsumer) {
            if (openSearchClient.indices().exists(new GetIndexRequest("wikimedia"), RequestOptions.DEFAULT)) {
                LOGGER.info("Index exists");
            } else {
                CreateIndexRequest request = new CreateIndexRequest("wikimedia");
                request.settings(Settings.builder()
                        .put("index.mapping.total_fields.limit", 2500)
                );
                openSearchClient.indices().create(request, RequestOptions.DEFAULT);

                LOGGER.info("Index has been created");
            }

            // consume from topic
            while (true) {
                int recordsConsumed = 0;
                int indexCounter = 0;
                int failCounter = 0;

                ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofMillis(3000));
                //LOGGER.info("Received "+ records.count() + " records");
                recordsConsumed += records.count();
                for (ConsumerRecord<String, String> record : records) {
                    // idempotency strategy 1
                    //String id = record.topic() + "_" + record.partition() + "_" + record.offset();
                    // idempotency strategy 2
                    String id = extractId(record.value());
                    IndexRequest request = new IndexRequest("wikimedia")
                            .source(record.value(), XContentType.JSON)
                            .id(id);
                    try {
                        IndexResponse response = openSearchClient.index(request, RequestOptions.DEFAULT);
                        LOGGER.info("Indexed document:" + response.getId());
                        indexCounter++;
                    } catch (Exception e) {
                        LOGGER.error("Error while indexing", e);
                        failCounter++;
                    }
                }
                /*LOGGER.info("Records consumed from kafka topic:"+ recordsConsumed);
                LOGGER.info("Documents indexed into opensearch:"+ indexCounter);
                LOGGER.info("Documents failed to index:"+ failCounter);*/
            }
        }

    }

    private static String extractId(String json) {
        return JsonParser
                .parseString(json)
                .getAsJsonObject()
                .get("meta")
                .getAsJsonObject()
                .get("id")
                .getAsString();
    }

    private static KafkaConsumer<String, String> createKafkaConsumer() {
        return new KafkaConsumer<>(configure());
    }

    private static Properties configure() {
        final Properties cfg = new Properties();
        cfg.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, props.getProperty("servers"));
        cfg.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        cfg.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        cfg.setProperty(ConsumerConfig.GROUP_ID_CONFIG, props.getProperty("group"));
        cfg.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // aiven access details
        cfg.put("security.protocol", "SSL");
        cfg.put("ssl.truststore.location", props.getProperty("ssl.truststore.location"));
        cfg.put("ssl.truststore.password", props.getProperty("ssl.truststore.password"));
        cfg.put("ssl.keystore.type", props.getProperty("ssl.keystore.type"));
        cfg.put("ssl.keystore.location", props.getProperty("ssl.keystore.location"));
        cfg.put("ssl.keystore.password", props.getProperty("ssl.keystore.password"));
        cfg.put("ssl.key.password", props.getProperty("ssl.key.password"));

        return cfg;
    }

    static {
        try(InputStream inputStream  = OpenSearchConsumer.class.getClassLoader().getResourceAsStream("config.properties");) {
            props = new Properties();
            props.load(inputStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static RestHighLevelClient createOpenSearchClient() {
        // we build a URI from the connection string
        RestHighLevelClient restHighLevelClient;
        URI connUri = URI.create(props.getProperty("bonsaiconnurl"));
        // extract login information if it exists
        String userInfo = connUri.getUserInfo();

        if (userInfo == null) {
            // REST client without security
            restHighLevelClient = new RestHighLevelClient(RestClient.builder(new HttpHost(connUri.getHost(), connUri.getPort(), "http")));

        } else {
            // REST client with security
            String[] auth = userInfo.split(":");

            CredentialsProvider cp = new BasicCredentialsProvider();
            cp.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(auth[0], auth[1]));

            restHighLevelClient = new RestHighLevelClient(
                    RestClient.builder(new HttpHost(connUri.getHost(), connUri.getPort(), connUri.getScheme()))
                            .setHttpClientConfigCallback(
                                    httpAsyncClientBuilder -> httpAsyncClientBuilder.setDefaultCredentialsProvider(cp)
                                            .setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy())));


        }

        return restHighLevelClient;
    }
}

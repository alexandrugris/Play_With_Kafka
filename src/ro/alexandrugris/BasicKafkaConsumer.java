package ro.alexandrugris;

import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;


import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class BasicKafkaConsumer implements AutoCloseable {

    private KafkaConsumer<String, MyMessage> consumer = null;

    public BasicKafkaConsumer(Properties _props) {

        Properties props = new Properties();
        props.setProperty("bootstrap.servers", _props.getProperty("bootstrap.servers", "localhost:9092"));

        // make sure we read from the beginning everytime:
        // https://stackoverflow.com/questions/28561147/how-to-read-data-using-kafka-consumer-api-from-beginning
        // props.setProperty(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());
        // props.setProperty(ConsumerConfig.CLIENT_ID_CONFIG, "ro.alexandrugris.BasicKafkaConsumer");
        // props.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        props.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "ro.alexandrugris.my_topic_consumergroup");

        // no auto commit
        props.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        consumer = new KafkaConsumer<>(
                props,
                new org.apache.kafka.common.serialization.StringDeserializer(),
                new ro.alexandrugris.ObjectSerializer<>()
        );

    }

    private void consume(Consumer<MyMessage> callback, String... topicsAndPartitions) throws Exception{

        class TopicPattern{
            private String topic = null;
            private String pattern = null;

            private TopicPattern(String _topic, String _pattern){
                topic = _topic;
                pattern = _pattern;
            }

            public TopicPartition getPartition(){
                return new TopicPartition(topic, ByDestinationPartitioner.getPartitionForKey(pattern, consumer.partitionsFor(topic).size()));
            }

            private String getTopic(){
                return topic;
            }
        }

        List<TopicPattern> topicPatterns = Arrays.stream(topicsAndPartitions)
                .map((topic) -> {

                    String[] split = topic.split(":");

                    if(split.length != 2)
                        throw new RuntimeException("Syntax: topic:pattern");

                    // todo: handle *
                    return new TopicPattern(split[0], split[1]);

                }).collect(Collectors.toList());


        // subscribe to all partitions in a topic
        consumer.subscribe(topicPatterns.stream().map(TopicPattern::getTopic).collect(Collectors.toSet()), new ConsumerRebalanceListener() {
            @Override
            public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                // do something here
                // https://kafka.apache.org/0101/javadoc/org/apache/kafka/clients/consumer/ConsumerRebalanceListener.html
                // for instance, save restore offset from an external store
            }

            @Override
            public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                // do something here
            }
        });

        // subscribe to specific partitions
        // consumer.assign(topicPatterns.stream().map( TopicPattern::getPartition).collect(Collectors.toList()));

        while(true){

            ConsumerRecords<String, MyMessage> msg = consumer.poll(100);

            msg.forEach( (cr) -> {
                if(topicPatterns.stream().anyMatch( (tp) -> tp.pattern.compareTo(cr.key()) == 0)) {
                    System.out.print(cr.partition() + ": ");
                    callback.accept(cr.value());
                }
            } );

            consumer.commitSync(); // we do not consume any other message until this commit has been performed
        }

    }

    /**
     * Entry point
     * @param args
     * @throws Exception
     */
    public static void main(String... args) throws Exception {

        try(BasicKafkaConsumer bkc = new BasicKafkaConsumer(System.getProperties())){

            bkc.consume( msg -> {
                System.out.println(String.format("To: %s, Content: %s", msg.destination(), msg.content()));
            }, "alexandrugris.my_topic:alexandru.gris", "alexandrugris.my_topic:olga.muravska");

        }
    }

    @Override
    public void close(){
        if(consumer != null)  consumer.close();
    }
}

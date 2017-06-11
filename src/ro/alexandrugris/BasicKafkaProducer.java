package ro.alexandrugris;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import java.util.Arrays;
import java.util.Properties;
import java.util.List;


public class BasicKafkaProducer implements AutoCloseable{

    private final static String TOPIC_NAME = "alexandrugris.my_topic";

    private KafkaProducer<String, MyMessage> myProducer = null;

    public BasicKafkaProducer(Properties _props){

        Properties props = new Properties();
        props.setProperty("bootstrap.servers", _props.getProperty("bootstrap.servers", "localhost:9092"));
        props.setProperty("partitioner.class", "ro.alexandrugris.ByDestinationPartitioner");
        props.setProperty("compression.type", "gzip");

        myProducer = new KafkaProducer<>(
                props,
                new org.apache.kafka.common.serialization.StringSerializer(),
                new ro.alexandrugris.ObjectSerializer<>()
        );

    }

    public static void main(String[] args) {

        try (BasicKafkaProducer me = new BasicKafkaProducer(System.getProperties())) {

            me.send(Arrays.asList(
                    new MyMessage("alexandru.gris", "Hello World"),
                    new MyMessage("olga.muravska", "Hello World"),
                    new MyMessage("olga.muravska", "Hello World"),
                    new MyMessage("alexandru.gris", "Hello World"),
                    new MyMessage("alexandru.gris", "Hello World"),
                    new MyMessage("gris.laurian", "Hello World"),
                    new MyMessage("gris.laurian", "Hello World")
            ));

        }
        catch(Exception exx){
            System.out.println(exx.toString());
        }

    }

    private void send(List<MyMessage> msgs) {

        Object lock      = new Object();

        class Counter {
            private int cnt = msgs.size();
            private int decrement(){ return --cnt; }
        }

        Counter cnt = new Counter();

        for(MyMessage msg : msgs) {

            myProducer.send(new ProducerRecord<>(TOPIC_NAME, msg.destination(), msg), (RecordMetadata metadata, Exception exception) -> {

                    if (exception != null) {
                        System.out.println(exception.toString());
                    } else {
                        System.out.println(metadata.toString());
                    }

                    synchronized (lock) {
                        if (cnt.decrement() == 0) lock.notify();
                    }
            });

        }

        synchronized (lock) {
            try {
                lock.wait();
            }
            catch (InterruptedException exx){ return ;}
        }
    }

    @Override
    public void close() throws Exception {
        if(myProducer != null)
            myProducer.close();
    }
}

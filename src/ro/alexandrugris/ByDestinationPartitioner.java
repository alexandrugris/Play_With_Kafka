package ro.alexandrugris;

import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;

import java.util.Map;

/***
 * A basic encyclopedia-style fixed partitioner, just for play
 */
public class ByDestinationPartitioner implements Partitioner {

    @Override
    public int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes, Cluster cluster) {
        return getPartitionForKey(key.toString(), cluster.partitionCountForTopic(topic));
    }

    static int getPartitionForKey(String key, int partitions){

        if(partitions < getMinPartitionCount()){ // try to avoid this :)
            throw new RuntimeException("This partitioner can only work with at least 5 partitions");
        }

        if(key.length() > 0) {
            char k = key.toLowerCase().charAt(0);

            // [ ... Here some advanced processing should occur ... ]

            if (k >= 'a' && k <= 'c')
                return 0;
            else if (k <= 'f')
                return 1;
            else if (k <= 'm')
                return 2;
            else if (k <= 'z')
                return 3;

            return 4 + k % (partitions - 4);
        }
        return 4;

    }

    private static int getMinPartitionCount(){
        return 5;
    }

    @Override
    public void close() {

    }
    @Override
    public void configure(Map<String, ?> configs) {

    }
}

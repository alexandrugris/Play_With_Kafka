package ro.alexandrugris;

import java.io.*;
import java.util.Map;

public class ObjectSerializer<T> implements
        org.apache.kafka.common.serialization.Serializer<T>,
        org.apache.kafka.common.serialization.Deserializer<T>{

    private byte[] toByteArray(T msg){

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(msg);
        }
        catch(Exception exx){
            System.out.println(exx.getMessage());
            return null;
        }

        return baos.toByteArray();
    }

    /**
     * Creates a basic message from a byte array
     */
    private T fromByteArray(byte[] arr){

        ByteArrayInputStream bais = new ByteArrayInputStream(arr);

        try (ObjectInputStream ois = new ObjectInputStream(bais)) {
            return (T) ois.readObject();
        }
        catch(Exception exx){
            return null;
        }
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        System.out.println("configure");
    }

    @Override
    public byte[] serialize(String topic, T data) {
        return toByteArray(data);
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        return fromByteArray(data);
    }

    @Override
    public void close() {
        System.out.println("close");
    }
}

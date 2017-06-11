package ro.alexandrugris;

import java.io.*;

class MyMessage implements Serializable {

    MyMessage(String _destination, String _content){
        destination = _destination;
        content = _content;
    }

    private String destination = null;
    private String content = null;

    String destination(){
        return destination;
    }

    String content(){
        return content;
    }

}

module com.openelements.cardless {
    requires com.google.gson;
    requires java.net.http;
    requires static org.jspecify;
    requires org.slf4j;

    exports com.openelements.cardless;
    exports com.openelements.cardless.data;
}
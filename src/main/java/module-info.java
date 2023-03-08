module com.erik.satter {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.ow2.sat4j.core;


    opens com.erik.satter to javafx.fxml;
    exports com.erik.satter;
}
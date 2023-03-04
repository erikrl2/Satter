module com.erik.satter {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.erik.satter to javafx.fxml;
    exports com.erik.satter;
}
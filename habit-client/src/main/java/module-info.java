module habit.client {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.base;

    requires com.habit.domain;
    requires org.slf4j;
    requires com.fasterxml.jackson.databind;
    requires org.json;
    requires java.net.http;
    // requires org.mindrot.jbcrypt;
    requires java.sql;

    exports com.habit.client.gui; // ← Mainクラスのあるパッケージ
}

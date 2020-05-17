package nats;

public interface MessageHandler {
    void onMessage(String msg);
}

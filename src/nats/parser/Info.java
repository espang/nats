package nats.parser;

public class Info extends Message{
    private String message;

    public Info(String s) {
        this.message = s;
    }
    public String getMessage() {
        return this.message;
    }
}

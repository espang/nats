package nats.protocol;

public class Info {
    private String message;

    public Info(String s) {
        this.message = s;
    }
    public String getMessage() {
        return this.message;
    }
}

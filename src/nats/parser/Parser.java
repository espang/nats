package nats.parser;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufProcessor;
import java.nio.charset.StandardCharsets;

public class Parser {

    public static Message parseMessage(ByteBuf buf) {
        if (buf.readableBytes() < 3) return new None();
        int index;
        byte first = buf.getByte(buf.readerIndex());
        switch (first) {
        case 73:
        index = buf.forEachByte(ByteBufProcessor.FIND_CR);
        String infoMsg = buf.readCharSequence(index, StandardCharsets.UTF_8).toString();
        return new Info(infoMsg);
        case 80:
        index = buf.forEachByte(ByteBufProcessor.FIND_CR);
        String msg = buf.readCharSequence(index, StandardCharsets.UTF_8).toString();
        if (msg.equals("PING"))
            return new Ping();
        if (msg.equals("PONG"))
            return new Pong();
        }
        
        return new Info("hello");
    } 
}

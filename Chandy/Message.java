package chandy;

import java.io.Serializable;

public class Message implements Serializable {
    public enum Type { REQUEST, TOKEN }
    public final Type type;
    public final int senderId;
    public final int m;       // για REQUEST
    public final int[] f;     // για TOKEN

    public Message(Type type, int senderId, int m, int[] f) {
        this.type     = type;
        this.senderId = senderId;
        this.m        = m;
        this.f        = f;
    }
}

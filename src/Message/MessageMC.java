package Message;
import java.io.Serializable;

public class MessageMC implements Serializable {
    private static final long serialVersionUID = 1L;
	String msg;
    public MessageMC(String msg){
        this.msg = msg;
    }
}

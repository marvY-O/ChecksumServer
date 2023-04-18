package Message;
import java.util.*;
import java.io.Serializable;

public class MessageAC implements Serializable{
	private static final long serialVersionUID = 1L;
	private Vector<String> adjacent;
    String testmsg = "";
    public MessageAC(){
        adjacent = new Vector<>();
    }
    public Vector<String> getAdjacent(){
        return adjacent;
    }
    public void set(String testmsg){
        this.testmsg = testmsg;
    }
    public void print() {
    	System.out.print(testmsg);
    }
}

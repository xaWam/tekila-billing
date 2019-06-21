package reseller;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * Created by ShakirG on 16/04/2018.
 */
@XmlRootElement
class ListOfOnlineUsers{
    public List<OnlineSessionObject> listArray;
    public int size;

    public ListOfOnlineUsers(){}

    public ListOfOnlineUsers(List<OnlineSessionObject> listArray, int size) {
        this.listArray = listArray;
        this.size = size;
    }

    public List<OnlineSessionObject> getListArray() {
        return listArray;
    }

    public void setListArray(List<OnlineSessionObject> listArray) {
        this.listArray = listArray;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    @Override
    public String toString() {
        return "ListOfOnlineUsers{" +
                "listArray=" + listArray +
                ", size=" + size +
                '}';
    }
}

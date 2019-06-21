package reseller;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * Created by ShakirG on 16/04/2018.
 */
@XmlRootElement
class OnlineUser {
    public OnlineSessionObject onlineSessionObject;
    public int size;

    public OnlineUser() {
    }

    public OnlineUser(OnlineSessionObject onlineSessionObject, int size) {
        this.onlineSessionObject = onlineSessionObject;
        this.size = size;
    }

    public OnlineSessionObject getOnlineSessionObject() {
        return onlineSessionObject;
    }

    public void setOnlineSessionObject(OnlineSessionObject onlineSessionObject) {
        this.onlineSessionObject = onlineSessionObject;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    @Override
    public String toString() {
        return "OnlineUser{" +
                "onlineSessionObject=" + onlineSessionObject +
                ", size=" + size +
                '}';
    }
}

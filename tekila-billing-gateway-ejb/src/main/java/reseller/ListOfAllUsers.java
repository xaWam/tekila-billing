package reseller;

import java.util.ArrayList;

/**
 * Created by ShakirG on 16/04/2018.
 */
public class ListOfAllUsers {
    public ArrayList<EchoEndpoint.UserSubscriber> userSubscribers;
    public int size;

    public ListOfAllUsers(){}

    public ListOfAllUsers(ArrayList<EchoEndpoint.UserSubscriber> userSubscribers, int size) {
        this.userSubscribers = userSubscribers;
        this.size = size;
    }
}

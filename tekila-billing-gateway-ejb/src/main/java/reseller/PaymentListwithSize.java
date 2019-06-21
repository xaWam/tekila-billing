package reseller;

import java.util.ArrayList;

/**
 * Created by ShakirG on 16/04/2018.
 */
public class PaymentListwithSize {
    public ArrayList<EchoEndpoint.PaymentList> paymentLists;
    public int totalSize;

    public PaymentListwithSize(){}

    public PaymentListwithSize(ArrayList<EchoEndpoint.PaymentList> paymentLists, int totalSize) {
        this.paymentLists = paymentLists;
        this.totalSize = totalSize;
    }

    @Override
    public String toString() {
        return "PaymentListwithSize{" +
                "paymentLists=" + paymentLists +
                ", totalSize=" + totalSize +
                '}';
    }
}

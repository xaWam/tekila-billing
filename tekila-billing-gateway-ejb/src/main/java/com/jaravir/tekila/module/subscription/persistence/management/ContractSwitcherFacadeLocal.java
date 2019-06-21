package com.jaravir.tekila.module.subscription.persistence.management;

import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.tools.WrapperAgreementChangeBatch;

import javax.ejb.Local;
import java.util.Collection;

/**
 * @author ElmarMa on 5/15/2018
 */
@Local
public interface ContractSwitcherFacadeLocal {

    void switchToNewContract(WrapperAgreementChangeBatch wrapperAgreementChangeBatch) throws Exception;

}

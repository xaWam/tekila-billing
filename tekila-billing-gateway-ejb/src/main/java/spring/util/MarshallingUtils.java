package spring.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spring.model.Report;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.File;


/**
 * @author GurbanAz
 * @date 11/04/2019
 */
public class MarshallingUtils {


    private static final Logger log = LoggerFactory.getLogger(MarshallingUtils.class);

    public static File subscriptionToXML(String filename, Report report){
        File file = new File(filename);

        log.info("~~~File created with name: " + filename);

        try {
            log.info("~~~Marshalling starts...");
            JAXBContext jaxbContext = JAXBContext.newInstance(Report.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.marshal(report, file);
        } catch (JAXBException e) {
            log.error("JAXB exception: ", e);

        }

        log.info("~~~Marshalling ended successfully...");
        return file;

    }
}

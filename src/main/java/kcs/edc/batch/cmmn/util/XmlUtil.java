package kcs.edc.batch.cmmn.util;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

public class XmlUtil {

    public static Object convertXmlContentToObject(String xml, Class<?> cls) {
        Object ob = null;
        JAXBContext jaxbContext = null;

        try {
            ob = cls.newInstance();
            jaxbContext = JAXBContext.newInstance(new Class[]{ob.getClass()});
        } catch (IllegalAccessException | InstantiationException var5) {
            var5.printStackTrace();
        } catch (JAXBException var6) {
            var6.printStackTrace();
        }

        return convertXmlContentToObject(xml, cls, jaxbContext);
    }

    public static Object convertXmlContentToObject(String xml, Class<?> cls, JAXBContext jaxbContext) {
        if (xml != null && !xml.trim().equals("") && jaxbContext != null) {
            Object convObj = null;
            Unmarshaller jaxbUnmarshaller = null;

            try {
                jaxbUnmarshaller = jaxbContext.createUnmarshaller();
                convObj = cls.newInstance();
                convObj = jaxbUnmarshaller.unmarshal(new StringReader(xml));
            } catch (IllegalAccessException | InstantiationException var6) {
                System.out.println(var6.getClass());
            } catch (JAXBException var7) {
                System.out.println(var7.getClass());
            }

            return convObj;
        } else {
            return null;
        }
    }

    public static Object convertXmlFileToObject(String filePath, Class<?> cls) {
        Object ob = null;
        JAXBContext jaxbContext = null;

        try {
            ob = cls.newInstance();
            jaxbContext = JAXBContext.newInstance(new Class[]{ob.getClass()});
        } catch (IllegalAccessException | InstantiationException var5) {
            var5.printStackTrace();
        } catch (JAXBException var6) {
            var6.printStackTrace();
        }

        return convertXmlFileToObject(filePath, cls, jaxbContext);
    }

    public static Object convertXmlFileToObject(String filePath, Class<?> cls, JAXBContext jaxbContext) {
        if (filePath != null && jaxbContext != null) {
            File file = new File(filePath);
            Object convObj = null;
            Unmarshaller jaxbUnmarshaller = null;

            try {
                jaxbUnmarshaller = jaxbContext.createUnmarshaller();
                convObj = cls.newInstance();
                convObj = jaxbUnmarshaller.unmarshal(file);
            } catch (IllegalAccessException | InstantiationException var7) {
                var7.printStackTrace();
            } catch (JAXBException var8) {
                var8.printStackTrace();
            }

            return convObj;
        } else {
            return null;
        }
    }

    public static void convertObjectToXmlFile(Object obj, String filePath) {
        if (filePath != null && obj != null) {
            try {
                JAXBContext context = JAXBContext.newInstance(new Class[]{obj.getClass()});
                Marshaller marshaller = context.createMarshaller();
                marshaller.setProperty("jaxb.formatted.output", Boolean.TRUE);
                marshaller.marshal(obj, new File(filePath));
            } catch (JAXBException var4) {
                var4.printStackTrace();
            }

        }
    }

    public static String convertObjectToXmlContent(Object obj, JAXBContext context) {
        StringWriter sw = new StringWriter();

        try {
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty("jaxb.formatted.output", Boolean.TRUE);
            marshaller.marshal(obj, sw);
        } catch (JAXBException var4) {
            var4.printStackTrace();
        }

        return sw.toString();
    }

    public static String convertXmlFileToString(String filePath) {
        Document docs = convertXmlFileToDocument(filePath);
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = null;
        String xmlString = "";

        try {
            transformer = tf.newTransformer();
            StringWriter sw = new StringWriter();
            transformer.transform(new DOMSource(docs), new StreamResult(sw));
            xmlString = sw.getBuffer().toString();
        } catch (TransformerException var6) {
            var6.printStackTrace();
        } catch (NullPointerException var7) {
            var7.printStackTrace();
        }

        return xmlString;
    }

    private static Document convertXmlFileToDocument(String filePath) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        Document xmlDocument = null;

        try {
            builder = factory.newDocumentBuilder();
            xmlDocument = builder.parse(new File(filePath));
        } catch (NullPointerException var5) {
            var5.printStackTrace();
        } catch (ParserConfigurationException | SAXException var6) {
            var6.printStackTrace();
        } catch (IOException var7) {
            var7.printStackTrace();
        }

        return xmlDocument;
    }
}

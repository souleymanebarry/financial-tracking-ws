package com.barry.bank.document.fop;

import lombok.extern.log4j.Log4j2;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.xmlgraphics.util.MimeConstants;
import org.springframework.stereotype.Component;

import javax.xml.XMLConstants;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayOutputStream;
import java.io.Reader;
import java.util.Objects;

@Component
@Log4j2
public class FopRenderer {

    private final FopFactory fopFactory;
    private final TransformerFactory transformerFactory;

    public FopRenderer(FopFactory fopFactory) {
        this.fopFactory = fopFactory;
        this.transformerFactory = TransformerFactory.newInstance();
        this.transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        this.transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
    }

    public byte[] render(Reader xslFoReader) throws FOPException, TransformerException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(256 * 1024);
        Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, baos);

        Transformer transformer = transformerFactory.newTransformer();
        Source src = new StreamSource(xslFoReader);
        Result res = new SAXResult(Objects.requireNonNull(
                fop.getDefaultHandler(), "FOP default handler is null"));
        transformer.transform(src, res);

        log.debug("FOP — PDF généré, taille: {} octets", baos.size());
        return baos.toByteArray();
    }
}
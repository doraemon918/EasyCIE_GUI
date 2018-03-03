/*
 * Copyright  2017  Department of Biomedical Informatics, University of Utah
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.utah.bmi.nlp.easycie.writer;

import edu.utah.bmi.nlp.uima.common.UIMATypeFunctions;
import edu.utah.bmi.nlp.uima.writer.EhostConfigurator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.jcas.tcas.DocumentAnnotation;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;

/**
 * This XMIWriter is derived from the simple CAS consumer provided by apache
 * UIMA. It read the MetaData annotation, which maintains the original source
 * file name, and output the xmi starting with the original source file name.
 * run it through CPE by using desc/casconsumer/XmiWritterCasConsumerPoet.xml
 *
 * @author Jianlin Shi
 * <p> *
 * A simple CAS consumer that writes the CAS to XMI format.
 * </p>
 * <p>
 * This CAS Consumer takes one parameter:
 * </p>
 * <ul>
 * <li><code>OutputDirectory</code> - path to directory into which output files
 * will be written</li>
 * </ul>
 */
public class EhostWriter_AE extends edu.utah.bmi.nlp.easycie.writer.XMIWritter_AE {
    /**
     * Name of configuration parameter that must be set to the path of a
     * directory into which the output files will be written.
     */

    private boolean keepSubDir = false;

    private File xmlOutputDir, txtOutputDir, configDir;

    private String annotator = "uima";

    private int mDocNum, docCounter = 0, subCorpusCounter = 0;


    private int elementId = 0;

    protected static HashMap<Class, LinkedHashSet<Method>> typeMethods = new HashMap<>();

    private SimpleDateFormat dateFormat = new SimpleDateFormat(
//            "EEE MMM dd HH:mm:ss zzz yyyy");
            "MM/dd/yy");

    public void initialize(UimaContext cont) {
        String includeTypes = baseInit(cont, "data/output/ehost", "uima");

        typeMethods = UIMATypeFunctions.getTypeMethods(includeTypes);

        mDocNum = 0;
        System.out.println("Ehost annotations will be exported to: " + outputDirectory);

        outputDirectory = new File(outputDirectory, annotator);

        xmlOutputDir = new File(outputDirectory, "saved");
        txtOutputDir = new File(outputDirectory, "corpus");


        try {
            if (!xmlOutputDir.exists())
                Files.createDirectories(Paths.get(xmlOutputDir.getAbsolutePath()));
            if (!txtOutputDir.exists())
                Files.createDirectories(Paths.get(txtOutputDir.getAbsolutePath()));
            configDir = new File(outputDirectory, "config");
            if (!configDir.exists())
                Files.createDirectories(Paths.get(configDir.getAbsolutePath()));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public void process(JCas jcas) throws AnalysisEngineProcessException {
        Collection<Annotation> annotations = JCasUtil.select(jcas, Annotation.class);
        File[] files = initialOutputXml(jcas);
        File outputXml = files[1];
        File sourceFile = files[0];
        try {
            writeEhostXML(jcas, annotations, sourceFile, outputXml);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }
    }


    protected LinkedHashSet<Method> getMethods(Annotation annotation) {
        if (typeMethods.containsKey(annotation.getClass())) {
            return typeMethods.get(annotation.getClass());
        } else {
            LinkedHashSet<Method> attributes = new LinkedHashSet<>();
            getMethods(annotation.getClass(), attributes);
            typeMethods.put(annotation.getClass(), attributes);
            return attributes;
        }
    }


    public void getMethods(Class c, LinkedHashSet<Method> methods) {
//        System.out.println(c.getSimpleName());
        if (c.getSimpleName().equals("Annotation")) {
            return;
        }
        for (Method method : c.getDeclaredMethods()) {
            String methodName = method.getName();
            if (methodName.startsWith("get") && !methodName.equals("getTypeIndexID"))
                methods.add(method);
        }
        getMethods(c.getSuperclass(), methods);
    }

    protected void writeEhostXML(JCas jcas, Collection<Annotation> annotations,
                                 File sourceFile, File outputXml) throws IOException, XMLStreamException {
        try {
            FileUtils.writeStringToFile(sourceFile, jcas.getDocumentText());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        FileOutputStream outputXmlStream = new FileOutputStream(outputXml);
        XMLStreamWriter xtw = initiateWritter(outputXmlStream, sourceFile);
        elementId = 0;
        for (Annotation annotation : annotations) {
            if (annotation instanceof SourceDocumentInformation || annotation instanceof DocumentAnnotation)
                continue;
            if (typeMethods.size() == 0)
                writeEhostAnnotation(xtw, annotation);
            else if (typeMethods.containsKey(annotation.getClass())) {
                writeEhostAnnotation(xtw, annotation);
            }
        }
        //		finish writing
        xtw.writeEndElement();
        xtw.writeEndDocument();
        xtw.flush();
        outputXmlStream.close();
        xtw.close();
    }


    private void writeEhostAnnotation(XMLStreamWriter xtw, Annotation annotation) throws XMLStreamException {
        int begin = annotation.getBegin();
        int end = annotation.getEnd();
        String type = annotation.getType().getShortName();
        String coveredText = StringEscapeUtils.escapeXml(annotation.getCoveredText());

        xtw.writeStartElement("annotation");

        xtw.writeStartElement("mention");
        xtw.writeAttribute("id", "EHOST_Instance_" + elementId);

        xtw.writeEndElement();

        xtw.writeStartElement("annotator");
        xtw.writeAttribute("id", annotator);
        xtw.writeCharacters(annotator);
        xtw.writeEndElement();

        xtw.writeStartElement("span");
        xtw.writeAttribute("start", begin + "");
        xtw.writeAttribute("end", end + "");
        xtw.writeEndElement();

        xtw.writeStartElement("spannedText");
        xtw.writeCharacters(coveredText);
        xtw.writeEndElement();

        xtw.writeStartElement("creationDate");
        xtw.writeCharacters(dateFormat.format(new Date()));
        xtw.writeEndElement();

        xtw.writeEndElement();
        int attributeIds = 0;
//        System.out.println(annotation.getType().getName() + "\t" + annotation.getCoveredText());
        for (Method method : getMethods(annotation)) {
            xtw.writeStartElement("stringSlotMention");
            xtw.writeAttribute("id", "EHOST_Instance_" + (elementId + attributeIds));
            attributeIds++;
            xtw.writeStartElement("mentionSlot");
            xtw.writeAttribute("id", method.getName().substring(3));
            xtw.writeEndElement();
            xtw.writeStartElement("stringSlotMentionValue");
            String value = getMethodValue(method, annotation);
//            System.out.println("\t"+value);
            xtw.writeAttribute("value", value);
            xtw.writeEndElement();
            xtw.writeEndElement();
        }


        xtw.writeStartElement("classMention");
        xtw.writeAttribute("id", "EHOST_Instance_" + elementId);

        for (int i = 0; i < attributeIds; i++) {
            xtw.writeStartElement("hasSlotMention");
            xtw.writeAttribute("id", "EHOST_Instance_" + (elementId + i));
            xtw.writeEndElement();
        }
        xtw.writeStartElement("mentionClass");
        xtw.writeAttribute("id", type);
        xtw.writeCharacters(coveredText);
        xtw.writeEndElement();
        elementId += attributeIds + 1;

        xtw.writeEndElement();
    }

    protected String getMethodValue(Method method, Annotation annotation) {
        String value = "";
        try {
            Object valueObj = method.invoke(annotation, null);
            if (valueObj != null)
                value = valueObj + "";

        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return value;
    }


    protected XMLStreamWriter initiateWritter(FileOutputStream outputFileStream, File sourceFile)
            throws XMLStreamException, IOException {
        XMLOutputFactory xof = XMLOutputFactory.newInstance();
        XMLStreamWriter xtw = xof.createXMLStreamWriter(outputFileStream, "UTF-8");
//		System.out.println(outputPath			+ sourcefileName + ".knowtator.xml");
        xtw.writeStartDocument("UTF-8", "1.0");
        xtw.writeStartElement("annotations");
        xtw.writeAttribute("textSource", sourceFile.getAbsolutePath());
        return xtw;
    }

    protected File[] initialOutputXml(JCas jcas) {

        String originalFileName = readFileIDName(jcas);
        File outFile, sourceFile;
        if (originalFileName.length() == 0) {
            originalFileName = "doc" + mDocNum;
        }
        if (!originalFileName.endsWith("txt"))
            originalFileName += ".txt";

        outFile = new File(xmlOutputDir, originalFileName + ".knowtator.xml");
        sourceFile = new File(txtOutputDir, originalFileName);


        return new File[]{sourceFile, outFile};
    }

    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        // no default behavior
        EhostConfigurator.setUp(new File(configDir, "projectschema.xml"), typeMethods);

    }


}
package org.cy3sbml.archive;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.unirostock.sems.cbarchive.Utils;
import de.unirostock.sems.cbarchive.meta.DefaultMetaDataObject;
import de.unirostock.sems.cbarchive.meta.OmexMetaDataObject;

import org.apache.taverna.robundle.Bundle;
import org.apache.taverna.robundle.manifest.Manifest;
import org.apache.taverna.robundle.manifest.PathAnnotation;

import org.cy3sbml.util.XMLUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * Handle the different types of annotation data
 * for a given aggregate.
 *
 * The annotation can be RDF or JSON.
 * The content can vary depending on the annotation.
 *
 * OMEX: on file containing all the information
 * <rdf:Description rdf:about=".">
 *
 * ROBundle
 * RDF
 *   <jerm:Investigation rdf:about="http://fairdomhub.org/investigations/96">
 *   :generic JSON parsing
 *   dcterms:title
 *   dcterms:created
 *   dcterms:description
 *   dcterms:modified
 *   sioc:has_creator
 *   sioc:has_owner
 *   jerm:hasContributor
 *   jerm:hasCreator
 *   jerm:hasPart
 *   jerm:isPartOf
 *   jerm:itemProducedBy
 *
 * JSON
 */
public class BundleAnnotation{
    private static final Logger logger = LoggerFactory.getLogger(BundleAnnotation.class);

    private static final String TYPE_RDF = "rdf";
    private static final String TYPE_JSON = "json";

    private Bundle bundle;
    private Map<String, List<String>> pathAnnotations;


    public BundleAnnotation(Bundle bundle){
        this.bundle = bundle;
        pathAnnotations = getBundleAnnotations(bundle);
    }

    public Map<String, List<String>> getPathAnnotations(){
        return pathAnnotations;
    }


    /**
     * BundleAnnotations.
     *
     * Reads the annotation Strings for the bundle and creates the
     * map to the paths.
     */
    public static Map<String, List<String>> getBundleAnnotations(Bundle bundle){

        // Get all the annotation Strings for given paths
        Map<String, List<String>> annotationMap = new HashMap<>();

        try {
            Manifest manifest = bundle.getManifest();
            logger.debug("getBundleAnnotations");
            for (PathAnnotation a : manifest.getAnnotations()) {
                System.out.println(a);

                List<URI> aboutURIs = a.getAboutList();
                URI contentURI = a.getContent();

                String content = contentURI.toString();
                // wrong prefix in content
                if (content.startsWith("/.ro/")){
                    content = content.replace("/.ro", "");
                }

                // annotation type
                String annotationType = null;
                if (content.endsWith(TYPE_JSON)){
                    annotationType = TYPE_JSON;
                } else if (content.endsWith(TYPE_RDF)){
                    annotationType = TYPE_RDF;
                }
                logger.info(content  + " : Type=" + annotationType);

                // read annotation content in String
                Path annotationPath = bundle.getPath(content);
                String annotation = new String(Files.readAllBytes(annotationPath));
                // readOmexMetaData(path);

                // add to all the paths
                for (URI uri: aboutURIs){
                    String aboutPath = uri.toString();

                    // Filter the RDF subset which is about the given path
                    // TODO:
                    /*
                    if (annotationType == TYPE_RDF){
                        annotation = aboutSubstringRDF(annotation, aboutPath);
                        if (aboutPath.equals("/")){
                            // omex names the about for root different
                            String a2 = aboutSubstringRDF(annotation, ".");
                            if (a2.length() < annotation.length()){
                                annotation = a2;
                            }
                        }
                    }
                    */
                    if (annotationType == TYPE_RDF) {
                        Document doc = XMLUtil.readXMLString(annotation);
                        annotation = XMLUtil.writeNodeToTidyString(doc);
                    }


                    List<String> annotations = annotationMap.getOrDefault(aboutPath, new LinkedList<> ());
                    annotations.add(annotation);
                    annotationMap.put(aboutPath, annotations);
                }
            }
        } catch (IOException e){
            e.printStackTrace();
        }
        return annotationMap;

    }

    /**
     * Gets the substring of the RDF which is about the path.
     * Mainly for OMEX archives.
     */
    private static String aboutSubstringRDF(String annotation, String aboutPath){
        Document doc = XMLUtil.readXMLString(annotation);

        Element rdfElement = doc.getDocumentElement();
        System.out.println(rdfElement);

        // NodeList nodeList = doc.getElementsByTagNameNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "RDF");
        // Element rdfElement = (Element) nodeList.item(0);
        NodeList children = rdfElement.getChildNodes();
        for (int k=0; k<children.getLength(); k++){
            Node n = children.item(k);
            if (n.getNodeType() != Node.ELEMENT_NODE){
                continue;
            }
            Element e = (Element) children.item(k);
            System.out.println(e);
            System.out.println(e.getAttributes());
            //String aboutRDF = e.getAttribute("rdf:about");
            String aboutRDF = e.getAttributeNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "about");
            // String aboutRDF = e.getAttributeValue("about", Utils.rdfNS);
            System.out.println("aboutRDF: " + aboutRDF);
            if (aboutRDF.equals(aboutPath)){
                // convert to String
                String text = XMLUtil.writeNodeToTidyString(e);
                return text;
            }
        }
        return annotation;
    }


    /**
     * Reads the omex meta data.
     */
    /*
    private static void readOmexMetaData(Path file){
        Document doc = null;
        try {
            doc = Utils.readXmlDocument(file);
            // Combine archive files
            List<Element> nl = Utils.getElementsByTagName(doc.getRootElement (), "Description", Utils.rdfNS);
            for (int i = 0; i < nl.size (); i++) {
                Element subtree = nl.get(i);
                OmexMetaDataObject object = OmexMetaDataObject.tryToRead(subtree);
                if (object == null) {
                    // is it default?
                    DefaultMetaDataObject object2 = DefaultMetaDataObject.tryToRead(subtree);
                }
                System.out.println(object);
            }
        } catch (JDOMException e) {
            logger.error("cannot read manifest of archive", e);
        } catch (IOException e) {
            logger.error("cannot read manifest of archive.", e);
        }
    }
    */
    
    
    /** Parse the different annotation files. */
    public static void main(String[] args) throws IOException {

        Path annotationPath = Paths.get("/home/mkoenig/git/cy3sbml/src/main/resources/archives/metadata.rdf");
        String annotation = new String(Files.readAllBytes(annotationPath));
        System.out.println(annotation);

        aboutSubstringRDF(annotation, ".");

    }
}

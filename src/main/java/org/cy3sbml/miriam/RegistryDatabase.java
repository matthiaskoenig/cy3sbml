package org.cy3sbml.miriam;


//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.cy3sbml.util.IOUtil;
import org.identifiers.registry.data.Annotation;
import org.identifiers.registry.data.DataType;
import org.identifiers.registry.data.PhysicalLocation;
import org.identifiers.registry.data.Restriction;
import org.identifiers.registry.data.RestrictionType;
import org.identifiers.registry.data.Tag;



public class RegistryDatabase {
    public static RegistryDatabase instance = null;
    public static Set<DataType> dataTypesSet = new TreeSet();
    public TreeMap<String, DataType> idDataTypeMap = new TreeMap();
    public TreeMap<String, DataType> nameDataTypeMap = new TreeMap();
    public HashMap<String, DataType> urnMap = new HashMap();
    public HashMap<String, DataType> namespaceMap = new HashMap();
    public HashMap<String, DataType> uriMap = new HashMap();
    public HashMap<String, Set<DataType>> tagNameDataTypeMap = new HashMap();
    public HashMap<String, String> tagNameDefinitionMap = new HashMap();

    public RegistryDatabase() {
    }

    public static RegistryDatabase getInstance() {
        if (instance == null) {
            instance = defaultLoad();
        }

        return instance;
    }

    public static RegistryDatabase loadFromFile(File file) throws FileNotFoundException {
        if (instance != null) {
            instance.clear();
        }

        instance = read(new FileInputStream(file));
        return instance;
    }

    public RegistryDatabase loadFromInputStream(InputStream inputStream) {
        if (instance != null) {
            clear();
        }

        instance = read(inputStream);
        return instance;
    }

    private void clear() {
        this.dataTypesSet = new TreeSet();
        this.idDataTypeMap = new TreeMap();
        this.nameDataTypeMap = new TreeMap();
        this.urnMap = new HashMap();
        this.namespaceMap = new HashMap();
        this.uriMap = new HashMap();
        this.tagNameDataTypeMap = new HashMap();
        this.tagNameDefinitionMap = new HashMap();
    }

    private static RegistryDatabase defaultLoad() {
        String RESOURCES_FILE = System.getProperty("registry.xml.export", "Miriam.xml");
        System.out.println("RESOURCES_FILE = " + RESOURCES_FILE);
        File file = new File(RESOURCES_FILE);
        if (!file.exists() || file.isDirectory()) {
            RESOURCES_FILE = "miriam.xml";
            file = new File(RESOURCES_FILE);
        }

        if (file.exists() && !file.isDirectory()) {
            System.out.println("Loading the Identifiers.org registry database from  '" + file.getAbsolutePath() + "'.");

            try {
                return read(new FileInputStream(file));
            } catch (Exception e) {
                throw new ExceptionInInitializerError(e);
            }
        } else {
            System.out.println("Loading the default Identifiers.org registry database from the jar file.");
            InputStream stream = IOUtil.readResource("/miriam/" + "getResolverDataset.json");
            return read(stream);
        }
    }

    public DataType getDatatypeById(String datatypeId) {
        return (DataType)this.idDataTypeMap.get(datatypeId);
    }

    public DataType getDataTypeByURI(String uri) {
        return (DataType)this.uriMap.get(uri);
    }

    public DataType getDataTypeByName(String name) {
        return (DataType)this.nameDataTypeMap.get(name);
    }

    public Collection<DataType> getDataTypes() {
        return this.idDataTypeMap.values();
    }

    public String getURNByDatatypeId(String datatypeId) {
        return this.getDatatypeById(datatypeId).getURN();
    }

    public String getURLByDatatypeId(String datatypeId) {
        return this.getDatatypeById(datatypeId).getURL();
    }

    public String getURNByDataTypeName(String name) {
        return this.getDataTypeByName(name).getURN();
    }

    public String getURLByDataTypeName(String name) {
        return this.getDataTypeByName(name).getURL();
    }

    public DataType getDatatypeByURI(String uri) {
        return (DataType)this.uriMap.get(uri);
    }

    public Map<String, DataType> getURIMap() {
        return this.uriMap;
    }

    public Map<String, DataType> getNameMap() {
        return this.nameDataTypeMap;
    }

    public Map<String, DataType> getDataTypeMap() {
        return this.idDataTypeMap;
    }

    public Set<DataType> getDataTypesByTagName(String tagName) {
        Set<DataType> set = (Set)this.tagNameDataTypeMap.get(tagName);
        return set;
    }

    public Map<String, String> getTagNameDefinitionMap(String tagName) {
        return this.tagNameDefinitionMap;
    }

    private static RegistryDatabase read(InputStream inputStrem) {
        BufferedInputStream stream = new BufferedInputStream(inputStrem);
        JsonNode document = null;

        try {
            document = create(stream);
        } catch (RuntimeException e) {
            throw e;
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException var31) {
                }
            }

        }

        instance = new RegistryDatabase();

        List<JsonNode> datatypes_list = document.findValues("pattern");


        System.out.println("document size: "+ document.findValue("namespaces").size());

        for(int i=0;i<datatypes_list.size();i++) {
            JsonNode datatypeElement = document.findValue("namespaces").get(i);

            //System.out.println(document.findValue("namespaces").get(i));
            DataType dataType = new DataType();
            dataType.setId(datatypeElement.get("id").asText());
            dataTypesSet.add(dataType);
            dataType.setName(datatypeElement.findValues("name").get(0).asText().trim());
            String obsloleteString = datatypeElement.get("deprecated").asText();
            dataType.setObsolete(Boolean.parseBoolean(obsloleteString));
            //Not sure how replacement works in JSON
          /*  if (dataType.isObsolete()) {
                String replacementString = datatypeElement.get("replacement").asText();
                dataType.setReplacedBy(replacementString);
            }*/

           // String restrictedString = datatypeElement.get("restricted").asText();
           // dataType.setRestricted(Boolean.parseBoolean(restrictedString));
            dataType.setRegexp(datatypeElement.get("pattern").asText());


            List<JsonNode> definitionLists = datatypeElement.findValues("description");
            if (definitionLists.size() > 0) {
                JsonNode definitionElt = definitionLists.get(0);
                String definition = definitionElt.asText().trim();
                dataType.setDefinition(definition);
            }

            List<JsonNode> synonymList = datatypeElement.findValues("prefix");
            ;
                ArrayList<String> synoArrayList = new ArrayList();

                for(int j = 0; j < synonymList.size(); ++j) {
                    JsonNode synonym = synonymList.get(j);
                    String synoString = synonym.asText().trim();
                    instance.nameDataTypeMap.put(synoString, dataType);
                    synoArrayList.add(synoString);
                }

                dataType.setSynonyms(synoArrayList);


            List<JsonNode> uriLists = datatypeElement.findValues("sampleId");
            if (uriLists.size() > 0) {
                List<JsonNode> uriList = (uriLists.get(0)).findValues("resourceHomeUrl");
                ArrayList<String> deprecatedURIs = new ArrayList();

                for(int j = 0; j < uriList.size(); ++j) {
                    JsonNode uri = uriList.get(j);
                    instance.uriMap.put(uri.asText(), dataType);
                    String uriType = String.valueOf(uri.get("type"));
                    String uriIsDeprecated = String.valueOf(uri.get("deprecated"));
                    String uriStr = uri.asText();
                    if (uriType.equals("URN") && !uriIsDeprecated.equals("true")) {
                        dataType.setURN(uriStr);
                    } else if (uriType.equals("URL") && !uriIsDeprecated.equals("true")) {
                        dataType.setURL(uriStr);
                    } else {
                        deprecatedURIs.add(uriStr);
                    }
                }

                if (deprecatedURIs.size() > 0) {
                    dataType.setDeprecatedURIs(deprecatedURIs);
                }
            }

            List<JsonNode> namespaceLists = datatypeElement.findValues("prefix");
            if (namespaceLists.size() > 0) {
                JsonNode namespaceElt = namespaceLists.get(0);
                String namespace = namespaceElt.asText().trim();
                dataType.setNamespace(namespace);
            }

            instance.idDataTypeMap.put(dataType.getId(), dataType);
            instance.nameDataTypeMap.put(dataType.getName(), dataType);
            instance.urnMap.put(dataType.getURN(), dataType);
            instance.namespaceMap.put(dataType.getNamespace(), dataType);
            List<JsonNode> resourcesLists = datatypeElement.findValues("resources");
            if (resourcesLists.size() > 0) {
                List<JsonNode> locationList = (resourcesLists.get(0)).findValues("resource");

                for(int j = 0; j < locationList.size(); ++j) {
                    JsonNode location = locationList.get(j);
                    if (!location.get("deprecated").asText().equalsIgnoreCase("true")) {
                        PhysicalLocation physicalLocation = new PhysicalLocation();
                        physicalLocation.setId(String.valueOf(location.get("id")));
                        //physicalLocation.setState(String.valueOf(location.get("state")));
                        //physicalLocation.setReliability(String.valueOf(location.get("reliability")));
                        physicalLocation.setPreferred(Boolean.parseBoolean(String.valueOf(location.get("preferred"))));
                        physicalLocation.setPrimary(Boolean.parseBoolean(String.valueOf(location.get("primary"))));
                        physicalLocation.setUrlRoot((location.findValues("dataResource").get(0)).asText());
                        String action = location.findValues("dataEntry").get(0).asText();
                        if (action.indexOf("$id") > -1) {
                            physicalLocation.setUrlPrefix(action.substring(0, action.indexOf("$id")));
                            physicalLocation.setUrlSuffix(action.substring(action.indexOf("$id") + 3, action.length()));
                        }

                        String info = location.findValues("dataInfo").get(0).asText();
                        physicalLocation.setInfo(info);
                        List<JsonNode> dataEntityExampleNodes = location.findValues("dataEntityExample");
                        if (dataEntityExampleNodes.size() > 0) {
                            String dataEntityExample = dataEntityExampleNodes.get(0).asText();
                            physicalLocation.setExample(dataEntityExample);
                        }

                        String institution = location.findValues("dataInstitution").get(0).asText();
                        physicalLocation.setInstitution(institution);
                        String geographicalLocation = location.findValues("dataLocation").get(0).asText();
                        physicalLocation.setLocation(geographicalLocation);
                        dataType.getResources().add(physicalLocation);
                    }
                }
            }

            List<JsonNode> documentationLists = datatypeElement.findValues("documentations");
            if (documentationLists.size() > 0) {
                List<JsonNode> documentationList = documentationLists.get(0).findValues("documentation");

                for(int j = 0; j < documentationList.size(); ++j) {
                    JsonNode documentationElt = documentationList.get(j);
                    String type = String.valueOf(documentationElt.get("type"));
                    dataType.addDocumentationType(type);
                    String documentationUrl = documentationElt.asText();
                    dataType.addDocumentationUrl(documentationUrl);
                }
            }

            List<JsonNode> restrictionLists = datatypeElement.findValues("restrictions");
            if (restrictionLists.size() > 0) {
                List<JsonNode> restrictionList = restrictionLists.get(0).findValues("restriction");

                for(int j = 0; j < restrictionList.size(); ++j) {
                    JsonNode restrictionElt = restrictionList.get(j);
                    Restriction restriction = new Restriction();
                    RestrictionType restrictionType = new RestrictionType();
                    String typeStr = String.valueOf(restrictionElt.get("type"));
                    String description = String.valueOf(restrictionElt.get("desc"));
                    restrictionType.setDesc(description);
                    restrictionType.setId(Integer.valueOf(typeStr));
                    restrictionType.setCategory(description);
                    restriction.setType(restrictionType);
                    List<JsonNode> statementList = restrictionElt.findValues("statement");
                    List<JsonNode> linkList = restrictionElt.findValues("link");
                    if (statementList.size() > 0) {
                        String statement = statementList.get(0).asText().trim();
                        restriction.setInfo(statement);
                    }

                    if (linkList.size() > 0) {
                        String link = linkList.get(0).asText().trim();
                        restriction.setLink(link);
                        restriction.setLinkText(linkList.get(0).get("desc").asText());
                    }

                    dataType.addRestriction(restriction);
                }
            }

            List<JsonNode> annotationLists = datatypeElement.findValues("annotation");
            if (annotationLists.size() > 0) {
                List<JsonNode> formatList = (annotationLists.get(0)).findValues("format");

                for(int j = 0; j < formatList.size(); ++j) {
                    JsonNode formatElt = formatList.get(j);
                    String formatName = String.valueOf(formatElt.get("name"));
                    Annotation annotation = new Annotation(formatName);
                    List<JsonNode> elementList = (formatElt.findValues("elements").get(0)).findValues("element");

                    for(int k = 0; k < elementList.size(); ++k) {
                        String elementName = elementList.get(k).asText().trim();
                        annotation.addTag(new Tag(elementName, elementName, (String)null));
                    }

                    dataType.getAnnotations().add(annotation);
                }
            }

            List<JsonNode> tagsLists = datatypeElement.findValues("tags");
            if (tagsLists.size() > 0) {
                List<JsonNode> tagList = (tagsLists.get(0)).findValues("tag");

                for(int j = 0; j < tagList.size(); ++j) {
                    JsonNode tagElt = tagList.get(j);
                    String tagName = tagElt.asText();
                    dataType.getTags().add(tagName);
                    Set<DataType> datatypeSet = (Set)instance.tagNameDataTypeMap.get(tagName);
                    if (datatypeSet == null) {
                        datatypeSet = new TreeSet();
                        instance.tagNameDataTypeMap.put(tagName, datatypeSet);
                    }

                    datatypeSet.add(dataType);
                }
            }
        }

        List<JsonNode> tagsLists = document.findValues("listOfTags");
        if (tagsLists.size() > 0) {
            List<JsonNode> tagDefinitions = (tagsLists.get(0)).findValues("tagDefinition");
            if (tagDefinitions.size() > 0) {
                for(int j = 0; j < tagDefinitions.size(); ++j) {
                    JsonNode tagElt = tagDefinitions.get(j);
                    String tagName = tagElt.findValues("name").get(0).asText();
                    String tagDefinition = tagElt.findValues("definition").get(0).asText();
                    instance.tagNameDefinitionMap.put(tagName, tagDefinition);
                }
            }
        }

        return instance;
    }

    public void main(String[] args) throws FileNotFoundException {
        for(DataType dataType : getInstance().getDataTypes()) {
            System.out.println(dataType);

            for(PhysicalLocation p : dataType.getPhysicalLocations()) {
                if (p.isPrimary()) {
                    System.out.println(p.getId() + " is primary");
                }
            }
        }

        System.out.println("Full size after default load = " + getInstance().getDataTypes().size());
        System.out.println("Size of the list of tags = " + getInstance().tagNameDefinitionMap.size());
        System.out.println("Nb datatype tag with neuroscience = " + getInstance().getDataTypesByTagName("neuroscience").size());
        loadFromFile(new File("/home/rodrigue/download/IdentifiersOrg-Registry_2015-07-09.xml"));
        System.out.println("Full size after loadFromFile 1 = " + getInstance().getDataTypes().size());
        loadFromFile(new File("/home/rodrigue/download/IdentifiersOrg-Registry_2015-07-09-small.xml"));
        System.out.println("Full size after loadFromFile 2 = " + getInstance().getDataTypes().size());
        loadFromInputStream(new FileInputStream(new File("/home/rodrigue/download/IdentifiersOrg-Registry_2015-07-09-tiny.xml")));
        System.out.println("Full size after loadFromFile 3 = " + getInstance().getDataTypes().size());
    }

    public static JsonNode create(InputStream byteStream) {
        if (byteStream == null) {
            throw new NullPointerException("Input stream is null.");
        }

        try {
            ObjectMapper mapper = new ObjectMapper();

            return mapper.readTree(byteStream);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }
        }


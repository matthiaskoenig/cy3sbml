package org.cy3sbml.util;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.cy3sbml.SBML;
import org.cy3sbml.comp.CompTargets;
import org.cy3sbml.comp.ModelResolution;
import org.cy3sbml.comp.SBaseRefResolution;
import org.cy3sbml.comp.SBaseRefResolver;
import org.cy3sbml.gui.BrowserHyperlinkListener;
import org.cy3sbml.gui.GUIConstants;
import org.cy3sbml.gui.SBaseHTMLFactory;
import org.sbml.jsbml.*;
import org.sbml.jsbml.ext.SBasePlugin;
import org.sbml.jsbml.ext.comp.CompConstants;
import org.sbml.jsbml.ext.comp.CompSBMLDocumentPlugin;
import org.sbml.jsbml.ext.comp.ExternalModelDefinition;
import org.sbml.jsbml.ext.comp.ModelDefinition;
import org.sbml.jsbml.ext.comp.ReplacedBy;
import org.sbml.jsbml.ext.comp.ReplacedElement;
import org.sbml.jsbml.ext.comp.SBaseRef;
import org.sbml.jsbml.ext.comp.Submodel;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;
import org.sbml.jsbml.ext.fbc.FBCReactionPlugin;
import org.sbml.jsbml.ext.fbc.FBCSpeciesPlugin;
import org.sbml.jsbml.ext.fbc.FluxObjective;
import org.sbml.jsbml.ext.fbc.GeneProduct;
import org.sbml.jsbml.ext.fbc.Objective;
import org.sbml.jsbml.ext.fbc.UserDefinedConstraint;
import org.sbml.jsbml.ext.fbc.UserDefinedConstraintComponent;
import org.sbml.jsbml.ext.groups.Group;
import org.sbml.jsbml.ext.groups.ListOfMembers;
import org.sbml.jsbml.ext.groups.Member;
import org.sbml.jsbml.ext.qual.Input;
import org.sbml.jsbml.ext.qual.Output;
import org.sbml.jsbml.ext.qual.QualitativeSpecies;
import org.sbml.jsbml.ext.qual.Transition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Some utils to work with SBML and SBML naming.
 */
public class SBMLUtil {
    private static final Logger logger = LoggerFactory.getLogger(SBMLUtil.class);

    /** The start of the namespace of SBML core, followed by the level and version. */
    private static final String SBML_NAMESPACE_PREFIX = "http://www.sbml.org/sbml/level";

    /**
     * Checks if the stream is an SBML file: XML whose root element is an {@code sbml} element
     * in an SBML core namespace. Reads the stream up to the root element only, in the
     * encoding of the XML declaration, and skips comments, processing instructions and a
     * document type declaration before it (without loading the DTD). The stream belongs
     * to the caller and is not closed.
     *
     * @throws XMLStreamException if the stream is no XML up to the root element, e.g. it
     *     ends before the root element is complete
     */
    public static boolean isSBML(InputStream stream) throws XMLStreamException {
        XMLInputFactory factory = XMLInputFactory.newDefaultFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        // does not close the stream
        XMLStreamReader reader = factory.createXMLStreamReader(stream);
        try {
            while (reader.hasNext()) {
                if (reader.next() == XMLStreamConstants.START_ELEMENT) {
                    String namespace = reader.getNamespaceURI();
                    return "sbml".equals(reader.getLocalName())
                            && namespace != null
                            && namespace.startsWith(SBML_NAMESPACE_PREFIX);
                }
            }
            return false;
        } finally {
            reader.close();
        }
    }

    /**
     * Reads the SBMLDocument of a classpath resource.
     *
     * @param resource the resource path
     * @return the document, null if it cannot be read
     */
    public static SBMLDocument readSBMLDocument(String resource) {
        InputStream instream = SBMLUtil.class.getResourceAsStream(resource);
        SBMLDocument doc = null;
        try {
            doc = SBMLReader.read(instream);
        } catch (XMLStreamException e) {
            logger.error("SBMLDocument reading failed.", e);
        }
        return doc;
    }

    /**
     * The XHTML content of the {@code <notes>} of the SBase, without an enclosing
     * {@code <body>} element, one top level element per line, sanitized with
     * {@link HtmlSanitizer}.
     *
     * @param sbase the SBase
     * @return the notes, null if the SBase has no notes or they cannot be read
     */
    public static String parseNotes(SBase sbase) {
        if (!sbase.isSetNotes()) {
            return null;
        }
        try {
            return sanitizedXhtml(sbase.getNotesString());
        } catch (XMLStreamException e) {
            logger.error("Error parsing notes xml.", e);
            return null;
        }
    }

    /**
     * The XHTML content of the root element of the XML, e.g. {@code <notes>} or the
     * {@code <message>} of a constraint, sanitized with {@link HtmlSanitizer}: the
     * formatting markup, without an enclosing {@code <html>} or {@code <body>} element,
     * one top level element per line.
     *
     * @return the XHTML, null if the XML cannot be read
     */
    static String sanitizedXhtml(String xml) {
        Document doc = XMLUtil.readXMLString(xml);
        if (doc == null) {
            return null;
        }
        XMLUtil.cleanEmptyTextNodes(doc);
        Element root = doc.getDocumentElement();
        HtmlSanitizer.sanitizeChildren(root);
        StringBuilder text = new StringBuilder();
        NodeList nodes = root.getChildNodes();
        for (int k = 0; k < nodes.getLength(); k++) {
            String nodeText = XMLUtil.writeNodeToTidyString(nodes.item(k));
            if (nodeText != null && !nodeText.isBlank()) {
                text.append(nodeText.trim()).append('\n');
            }
        }
        return text.toString();
    }

    /**
     * The variable of an AssignmentRule or RateRule.
     *
     * @param rule the rule
     * @return the variable, null if it is not set or the rule is an AlgebraicRule
     */
    public static Variable getVariableFromRule(Rule rule) {
        if (rule.isAssignment()) {
            AssignmentRule r = (AssignmentRule) rule;
            if (r.isSetVariable()) {
                return r.getVariableInstance();
            }
        } else if (rule.isRate()) {
            RateRule r = (RateRule) rule;
            if (r.isSetVariable()) {
                return r.getVariableInstance();
            }
        }
        return null;
    }

    /**
     * The class name of the object without the package, with {@code .} for the {@code $} of
     * a nested class, e.g. {@code ListOf.Type}.
     *
     * @param obj the object
     * @return the unqualified class name
     */
    public static String getUnqualifiedClassName(Object obj) {
        String name = obj.getClass().getName();
        if (name.lastIndexOf('.') > 0) {
            name = name.substring(name.lastIndexOf('.') + 1);
        }
        // The $ can be converted to a .
        name = name.replace('$', '.');
        return name;
    }

    // ------------------------------------------------------------
    // Attribute maps
    // ------------------------------------------------------------
    // the math labels of rules without a variable (algebraic) and with one (assignment, rate)
    public static final String TEMPLATE_ALGEBRAIC_RULE = "<~>";
    public static final String TEMPLATE_ASSIGNMENT_RULE = "<%s>";
    public static final String TEMPLATE_RATE_RULE = "<d/dt %s>";

    private static final String ATTR_ID = "id";
    private static final String ATTR_NAME = "name";
    public static final String ATTR_COMPARTMENT = "compartment";
    public static final String ATTR_INITIAL_CONCENTRATION = "initialConcentration";
    public static final String ATTR_INITIAL_AMOUNT = "amount";
    public static final String ATTR_CHARGE = "charge";
    private static final String ATTR_CONSTRAINT = "constraint";
    private static final String ATTR_COMPONENT = "component";
    private static final String ATTR_COEFFICIENT = "coefficient";
    private static final String ATTR_VARIABLE = "variable";
    private static final String ATTR_VARIABLE2 = "variable2";

    private static final String LINK_ID_TEMPLATE = " <a href=\"" + BrowserHyperlinkListener.URL_SELECT_ID + "%s\">"
            + GUIConstants.ICON_LINK.replace("{title}", "Link to node.") + "</a>";
    private static final String LINK_METAID_TEMPLATE = " <a href=\"" + BrowserHyperlinkListener.URL_SELECT_METAID
            + "%s\">" + GUIConstants.ICON_LINK.replace("{title}", "Link to node.") + "</a>";

    /** The link to the node of the SId, the SId escaped. */
    private static String idLink(String sid) {
        return String.format(LINK_ID_TEMPLATE, HtmlUtil.escape(sid));
    }

    /** The link to the node of the metaId, the metaId escaped. */
    private static String metaIdLink(String metaId) {
        return String.format(LINK_METAID_TEMPLATE, HtmlUtil.escape(metaId));
    }

    /** HTML of an attribute that is not set: an empty cell. */
    private static final String UNSET = "";

    /**
     * Unit HTML, or {@link #UNSET} if there are no units.
     */
    private static String unitHtml(String units) {
        return units == null || units.isEmpty()
                ? UNSET
                : String.format("<span class=\"unit\">%s</span>", HtmlUtil.escape(units));
    }

    /**
     * Math HTML, or {@link #UNSET} if there is no math.
     *
     * @param math the math as HTML, i.e. the formula escaped
     */
    private static String mathHtml(String math) {
        return math == null || math.isEmpty() ? UNSET : String.format("<span class=\"math\">%s</span>", math);
    }

    /**
     * Map for SBase.
     */
    public static Map<String, String> createSBaseMap(SBase sbase) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put(SBML.ATTR_METAID, sbase.isSetMetaId() ? HtmlUtil.escape(sbase.getMetaId()) : UNSET);
        return map;
    }

    /**
     * Map for NamedSBase.
     */
    public static Map<String, String> createNamedSBaseMap(NamedSBase nsb) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put(ATTR_ID, nsb.isSetId() ? nsb.getId() : UNSET);
        map.put(ATTR_NAME, nsb.isSetName() ? HtmlUtil.escape(nsb.getName()) : UNSET);
        map.putAll(createSBaseMap(nsb));
        return map;
    }

    /**
     * Map for NamedSBaseWithDerivedUnit.
     */
    public static Map<String, String> createNamedSBaseWithDerivedUnitMap(NamedSBaseWithDerivedUnit nsbu) {
        Map<String, String> map = createNamedSBaseMap(nsbu);
        String units = getDerivedUnitHtml(nsbu);
        map.put(SBML.ATTR_DERIVED_UNITS, unitHtml(units));
        return map;
    }

    /**
     * Map for QuantityWithUnit.
     */
    public static Map<String, String> createQuantityWithUnitNodeMap(QuantityWithUnit quantity) {
        Map<String, String> map = createNamedSBaseWithDerivedUnitMap(quantity);
        String units = quantity.isSetUnits() ? quantity.getUnits() : UNSET;
        String value = quantity.isSetValue() ? ((Double) quantity.getValue()).toString() : UNSET;
        map.put(SBML.ATTR_VALUE, String.join(" ", value, unitHtml(units)).trim());
        return map;
    }

    /**
     * Map for Symbol.
     */
    public static Map<String, String> createSymbolMap(Symbol symbol) {
        Map<String, String> map = createQuantityWithUnitNodeMap(symbol);
        map.put(
                SBML.ATTR_CONSTANT,
                symbol.isSetConstant() ? SBaseHTMLFactory.booleanHTML(symbol.getConstant()) : UNSET);
        return map;
    }

    public static Map<String, String> createAbstractMathContainerNodeMap(AbstractMathContainer container) {
        return createAbstractMathContainerNodeMap(container, null);
    }

    /**
     * Map for AbstractMathContainer.
     */
    public static Map<String, String> createAbstractMathContainerNodeMap(
            AbstractMathContainer container, Variable variable) {
        Map<String, String> map = createSBaseMap(container);
        String math = container.isSetMath() ? HtmlUtil.escape(ASTNodeUtil.toFormula(container.getMath())) : UNSET;
        String units = getDerivedUnitHtml(container);
        if (variable != null) {
            String variableId = HtmlUtil.escape(variable.getId());
            map.put(SBML.ATTR_VARIABLE, variableId + metaIdLink(variable.getMetaId()));
            math = String.format("%s = %s", variableId, math);
        }
        map.put(SBML.ATTR_MATH, mathHtml(math));
        map.put(SBML.ATTR_UNITS, unitHtml(units));
        return map;
    }

    /**
     * SBMLDocument map.
     */
    public static Map<String, String> createSBMLDocumentMap(SBMLDocument doc) {
        return new LinkedHashMap<>();
    }

    /**
     * Model map.
     */
    public static Map<String, String> createModelMap(Model model) {
        // packages
        Map<String, SBasePlugin> packageMap = model.getExtensionPackages();
        String packages = "";
        if (packageMap != null) {
            packages = "";
            for (SBasePlugin plugin : packageMap.values()) {

                packages += String.format(
                        " <span class=\"collection\">%s-V%s</span>",
                        plugin.getPackageName(), plugin.getPackageVersion());
            }
        }

        LinkedHashMap<String, String> map = new LinkedHashMap<>();

        // default
        map.put(
                String.format(
                        "<span class=\"collection\">L%sV%s</span>%s", model.getLevel(), model.getVersion(), packages),
                String.format(
                        "<a href=\"%s\"><img src=\"./images/logos/sbml_icon.png\" height=\"20\" /></a>",
                        BrowserHyperlinkListener.URL_SBMLFILE));
        map.putAll(createNamedSBaseMap(model));

        // optional
        if (model.isSetSubstanceUnits()) {
            map.put(SBML.ATTR_SUBSTANCE_UNITS, unitHtml(model.getSubstanceUnits()));
        }
        if (model.isSetTimeUnits()) {
            map.put(SBML.ATTR_TIME_UNITS, unitHtml(model.getTimeUnits()));
        }
        if (model.isSetVolumeUnits()) {
            map.put(SBML.ATTR_VOLUME_UNITS, unitHtml(model.getVolumeUnits()));
        }
        if (model.isSetAreaUnits()) {
            map.put(SBML.ATTR_AREA_UNITS, unitHtml(model.getAreaUnits()));
        }
        if (model.isSetLengthUnits()) {
            map.put(SBML.ATTR_LENGTH_UNITS, unitHtml(model.getLengthUnits()));
        }
        if (model.isSetExtentUnits()) {
            map.put(SBML.ATTR_EXTENT_UNITS, unitHtml(model.getExtentUnits()));
        }
        if (model.isSetConversionFactor()) {
            map.put(SBML.ATTR_CONVERSION_FACTOR, HtmlUtil.escape(model.getConversionFactor()));
        }
        return map;
    }

    /**
     * FunctionDefinition map.
     */
    public static Map<String, String> createFunctionDefinitionMap(FunctionDefinition fd) {
        Map<String, String> map = createNamedSBaseMap(fd);
        map.putAll(createAbstractMathContainerNodeMap(fd));
        return map;
    }

    /**
     * Compartment map.
     */
    public static Map<String, String> createCompartmentMap(Compartment compartment) {
        Map<String, String> map = createSymbolMap(compartment);
        map.put(
                SBML.ATTR_SPATIAL_DIMENSIONS,
                compartment.isSetSpatialDimensions()
                        ? ((Double) compartment.getSpatialDimensions()).toString()
                        : UNSET);
        map.put(SBML.ATTR_SIZE, compartment.isSetSize() ? ((Double) compartment.getSize()).toString() : UNSET);
        return map;
    }

    /**
     * Parameter map.
     */
    public static Map<String, String> createParameterMap(Parameter p) {
        Map<String, String> map = createSymbolMap(p);
        return map;
    }

    /**
     * Species map.
     */
    public static Map<String, String> createSpeciesMap(Species s) {
        Map<String, String> map = createSymbolMap(s);

        String compartment = UNSET;
        if (s.isSetCompartment()) {
            compartment = HtmlUtil.escape(s.getCompartment()) + idLink(s.getCompartment());
        }
        map.put(ATTR_COMPARTMENT, compartment);
        String boundaryCondition =
                s.isSetBoundaryCondition() ? SBaseHTMLFactory.booleanHTML(s.getBoundaryCondition()) : UNSET;
        map.put(SBML.ATTR_BOUNDARY_CONDITION, boundaryCondition);
        String initialAmount = s.isSetInitialAmount() ? ((Double) s.getInitialAmount()).toString() : UNSET;
        map.put(ATTR_INITIAL_AMOUNT, initialAmount);
        String initialConcentration = UNSET;
        if (s.isSetInitialConcentration()) {
            initialConcentration = ((Double) s.getInitialConcentration()).toString();
        }
        map.put(ATTR_INITIAL_CONCENTRATION, initialConcentration);
        String hasOnlySubstanceUnits = UNSET;
        if (s.isSetHasOnlySubstanceUnits()) {
            hasOnlySubstanceUnits = SBaseHTMLFactory.booleanHTML(s.getHasOnlySubstanceUnits());
        }
        map.put(SBML.ATTR_HAS_ONLY_SUBSTANCE_UNITS, hasOnlySubstanceUnits);

        // optional
        if (s.isSetCharge()) {
            // reason: charge is deprecated in SBML L3, but still read from older SBML levels
            @SuppressWarnings("deprecation")
            int charge = s.getCharge();
            map.put(ATTR_CHARGE, Integer.toString(charge));
        }
        if (s.isSetConversionFactor()) {
            map.put(SBML.ATTR_CONVERSION_FACTOR, HtmlUtil.escape(s.getConversionFactor()));
        }
        if (s.isSetSubstanceUnits()) {
            map.put(SBML.ATTR_SUBSTANCE_UNITS, HtmlUtil.escape(s.getSubstanceUnits()));
        }

        // fbc
        FBCSpeciesPlugin fbcSpecies = (FBCSpeciesPlugin) s.getExtension(FBCConstants.shortLabel);
        if (fbcSpecies != null) {
            String charge = UNSET;
            if (fbcSpecies.isSetCharge()) {
                // a double in fbc v3, an integer before
                charge = numberString(fbcSpecies.getChargeAsDouble());
            }
            map.put(SBML.ATTR_FBC_CHARGE, charge);

            String chemicalFormula = UNSET;
            if (fbcSpecies.isSetChemicalFormula()) {
                chemicalFormula = HtmlUtil.escape(fbcSpecies.getChemicalFormula());
            }
            map.put(SBML.ATTR_FBC_CHEMICAL_FORMULA, chemicalFormula);
        }
        return map;
    }

    /**
     * Reaction map.
     */
    public static Map<String, String> createReactionMap(Reaction r) {
        Map<String, String> map = createNamedSBaseMap(r);

        String compartment =
                r.isSetCompartment() ? HtmlUtil.escape(r.getCompartment()) + idLink(r.getCompartment()) : UNSET;
        String reversible = r.isSetReversible() ? SBaseHTMLFactory.booleanHTML(r.getReversible()) : UNSET;
        // reason: fast is deprecated in SBML L3V2, but still read from older SBML versions
        @SuppressWarnings("deprecation")
        String fast = r.isSetFast() ? SBaseHTMLFactory.booleanHTML(r.getFast()) : UNSET;
        String kineticLaw = UNSET;
        if (r.isSetKineticLaw()) {
            KineticLaw law = r.getKineticLaw();
            if (law.isSetMath()) {
                kineticLaw = HtmlUtil.escape(ASTNodeUtil.toFormula(law.getMath())) + metaIdLink(law.getMetaId());
            }
        }
        String units = getDerivedUnitHtml(r);

        map.put(SBML.ATTR_EQUATION, equationHtml(r));
        map.put(ATTR_COMPARTMENT, compartment);
        map.put(SBML.ATTR_REVERSIBLE, reversible);
        map.put(SBML.ATTR_FAST, fast);
        map.put(SBML.ATTR_KINETIC_LAW, mathHtml(kineticLaw));
        map.put(SBML.ATTR_UNITS, unitHtml(units));

        // fbc
        FBCReactionPlugin fbcReaction = (FBCReactionPlugin) r.getExtension(FBCConstants.shortLabel);
        if (fbcReaction != null) {
            // the bound parameters with their values and links, like the bounds of constraints
            map.put(
                    SBML.ATTR_FBC_LOWER_FLUX_BOUND,
                    parameterHtml(
                            r.getModel(), fbcReaction.isSetLowerFluxBound() ? fbcReaction.getLowerFluxBound() : null));
            map.put(
                    SBML.ATTR_FBC_UPPER_FLUX_BOUND,
                    parameterHtml(
                            r.getModel(), fbcReaction.isSetUpperFluxBound() ? fbcReaction.getUpperFluxBound() : null));
        }
        map.putAll(fluxObjectiveMap(r));
        return map;
    }

    /**
     * Reaction equation, e.g. {@code 2 A + B ⇌ C; E} with the modifiers after the semicolon.
     * A missing side is written as the empty set.
     */
    static String equationHtml(Reaction r) {
        String arrow =
                String.format(" <span class=\"equation-arrow\">%s</span> ", r.getReversible() ? "&#8652;" : "&#8594;");
        // getListOfReactants and getListOfProducts would add an empty list to the reaction
        String equation = sideHtml(r.isSetListOfReactants() ? r.getListOfReactants() : null)
                + arrow
                + sideHtml(r.isSetListOfProducts() ? r.getListOfProducts() : null);
        if (r.isSetListOfModifiers() && r.getModifierCount() > 0) {
            List<String> modifiers = new ArrayList<>();
            for (ModifierSpeciesReference msr : r.getListOfModifiers()) {
                modifiers.add(HtmlUtil.escape(msr.getSpecies()));
            }
            equation += "; " + String.join(" ", modifiers);
        }
        return equation;
    }

    private static String sideHtml(ListOf<SpeciesReference> speciesReferences) {
        if (speciesReferences == null || speciesReferences.isEmpty()) {
            return "&#8709;";
        }
        List<String> terms = new ArrayList<>();
        for (SpeciesReference sr : speciesReferences) {
            String stoichiometry = stoichiometryHtml(sr);
            String species = HtmlUtil.escape(sr.getSpecies());
            terms.add(stoichiometry.isEmpty() ? species : stoichiometry + " " + species);
        }
        return String.join(" + ", terms);
    }

    /**
     * Stoichiometry in an equation: empty for 1, the formula of a (SBML L2) stoichiometryMath,
     * and the species reference id for an unset (SBML L3) stoichiometry which a rule or
     * assignment determines.
     */
    // reason: StoichiometryMath is deprecated in JSBML, but still read from SBML L2 models
    @SuppressWarnings("deprecation")
    private static String stoichiometryHtml(SpeciesReference sr) {
        if (sr.isSetStoichiometryMath() && sr.getStoichiometryMath().isSetMath()) {
            return "("
                    + HtmlUtil.escape(
                            ASTNodeUtil.toFormula(sr.getStoichiometryMath().getMath())) + ")";
        }
        if (sr.isSetStoichiometry()) {
            double value = sr.getStoichiometry();
            return value == 1.0 ? "" : numberString(value);
        }
        return sr.getLevel() >= 3 && sr.isSetId() ? HtmlUtil.escape(sr.getId()) : "";
    }

    /** A number without a fractional part of zero, e.g. {@code 2} for 2.0. */
    private static String numberString(double value) {
        if (value == Math.rint(value) && !Double.isInfinite(value) && Math.abs(value) < 1e15) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }

    /**
     * The coefficients of the reaction in the fbc objectives of its model, one entry per
     * objective the reaction is part of, keyed like the objective columns of the network.
     */
    private static Map<String, String> fluxObjectiveMap(Reaction r) {
        Map<String, String> map = new LinkedHashMap<>();
        Model model = r.getModel();
        if (model == null || !r.isSetId()) {
            return map;
        }
        FBCModelPlugin fbcModel = (FBCModelPlugin) model.getExtension(FBCConstants.shortLabel);
        // the getters of unset lists would add an empty list to the model
        if (fbcModel == null || !fbcModel.isSetListOfObjectives()) {
            return map;
        }
        for (Objective objective : fbcModel.getListOfObjectives()) {
            if (!objective.isSetListOfFluxObjectives()) {
                continue;
            }
            for (FluxObjective fluxObjective : objective.getListOfFluxObjectives()) {
                if (r.getId().equals(fluxObjective.getReaction())) {
                    map.put(
                            String.format(SBML.ATTR_FBC_OBJECTIVE_TEMPLATE, objective.getId()),
                            Double.toString(fluxObjective.getCoefficient()));
                    if (fluxObjective.isSetVariableType()) {
                        map.put(
                                String.format(SBML.ATTR_FBC_OBJECTIVE_VARIABLE_TYPE_TEMPLATE, objective.getId()),
                                fluxObjective.getVariableType().toString());
                    }
                }
            }
        }
        return map;
    }

    /**
     * InitialAssignment map.
     */
    public static Map<String, String> createInitialAssignmentMap(InitialAssignment ass) {

        Variable variable = ass.getVariableInstance();
        Map<String, String> map = createAbstractMathContainerNodeMap(ass, variable);

        return map;
    }

    /**
     * UnitDefinition map.
     */
    public static Map<String, String> createUnitDefinitionMap(UnitDefinition ud) {
        Map<String, String> map = createNamedSBaseMap(ud);
        // Add units
        String units = "";
        for (Unit u : ud.getListOfUnits()) {
            units += HtmlUtil.escape(u.printUnit()) + "<br />";
        }
        map.put("units", units);
        return map;
    }

    /**
     * Unit map.
     */
    public static Map<String, String> createUnitMap(Unit u) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();

        String kind = u.isSetKind() ? u.getKind().toString() : UNSET;
        String exponent = u.isSetExponent() ? ((Double) u.getExponent()).toString() : UNSET;
        String multiplier = u.isSetMultiplier() ? ((Double) u.getMultiplier()).toString() : UNSET;
        String scale = u.isSetScale() ? ((Integer) u.getScale()).toString() : UNSET;

        map.put(SBML.ATTR_UNIT_KIND, kind);
        map.put(SBML.ATTR_UNIT_EXPONENT, exponent);
        map.put(SBML.ATTR_UNIT_MULTIPLIER, multiplier);
        map.put(SBML.ATTR_UNIT_SCALE, scale);
        return map;
    }

    /**
     * Constraint map.
     */
    public static Map<String, String> createConstraintMap(Constraint constraint) {
        Map<String, String> map = createAbstractMathContainerNodeMap(constraint);
        String message = null;
        if (constraint.isSetMessage()) {
            try {
                // XHTML of the model, reduced to formatting markup like the notes
                message = sanitizedXhtml(constraint.getMessageString());
            } catch (XMLStreamException e) {
                logger.error("Constraint message could not be created.", e);
            }
        }
        map.put(SBML.ATTR_MESSAGE, message != null ? message : UNSET);
        return map;
    }

    /**
     * Event map.
     */
    public static Map<String, String> createEventMap(Event event) {
        Map<String, String> map = createNamedSBaseWithDerivedUnitMap(event);
        String triggerStr = UNSET;
        String initialValue = UNSET;
        String persistent = UNSET;
        if (event.isSetTrigger()) {
            Trigger trigger = event.getTrigger();
            if (trigger.isSetMath()) {
                triggerStr = mathHtml(HtmlUtil.escape(ASTNodeUtil.toFormula(trigger.getMath())));
            }
            if (trigger.isSetInitialValue()) {
                initialValue = SBaseHTMLFactory.booleanHTML(trigger.getInitialValue());
            }
            if (trigger.isSetPersistent()) {
                persistent = SBaseHTMLFactory.booleanHTML(trigger.getPersistent());
            }
        }
        map.put("trigger", triggerStr);
        map.put("trigger initialValue", initialValue);
        map.put("trigger persistent", persistent);

        String priorityStr = UNSET;
        if (event.isSetPriority()) {
            Priority priority = event.getPriority();
            if (priority.isSetMath()) {
                priorityStr = mathHtml(HtmlUtil.escape(ASTNodeUtil.toFormula(priority.getMath())));
            }
        }
        map.put("priority", priorityStr);
        String delayStr = UNSET;
        if (event.isSetDelay()) {
            Delay delay = event.getDelay();
            if (delay.isSetMath()) {
                delayStr = mathHtml(HtmlUtil.escape(ASTNodeUtil.toFormula(delay.getMath())));
            }
        }
        map.put("delay", delayStr);
        return map;
    }

    /**
     * EventAssignment map.
     */
    public static Map<String, String> createEventAssignmentMap(EventAssignment ea) {
        Variable variable = ea.getVariableInstance();
        Map<String, String> map = createAbstractMathContainerNodeMap(ea, variable);
        return map;
    }

    /**
     * Rule map.
     */
    public static Map<String, String> createRuleMap(Rule rule) {
        Variable variable = SBMLUtil.getVariableFromRule(rule);
        Map<String, String> map = createAbstractMathContainerNodeMap(rule, variable);
        return map;
    }

    /**
     * LocalParameter map.
     */
    public static Map<String, String> createLocalParameterMap(LocalParameter lp) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        KineticLaw law = (KineticLaw) lp.getParent().getParent();
        Reaction reaction = law.getParent();
        String reactionId = reaction.getId();
        map.put("reaction", HtmlUtil.escape(reactionId) + idLink(reactionId));
        map.putAll(createQuantityWithUnitNodeMap(lp));
        return map;
    }

    /**
     * KineticLaw map.
     */
    public static Map<String, String> createKineticLawMap(KineticLaw law) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        Reaction reaction = law.getParent();
        String reactionId = reaction.getId();
        map.put("reaction", HtmlUtil.escape(reactionId) + idLink(reactionId));
        map.putAll(createAbstractMathContainerNodeMap(law));
        return map;
    }

    // QUAL

    /**
     * QualitativeSpecies map.
     */
    public static Map<String, String> createQualitativeSpeciesMap(QualitativeSpecies qs) {
        Map<String, String> map = createNamedSBaseMap(qs);

        String compartment = qs.isSetCompartment() ? HtmlUtil.escape(qs.getCompartment()) : UNSET;
        String initialLevel = qs.isSetInitialLevel() ? ((Integer) qs.getInitialLevel()).toString() : UNSET;
        String maxLevel = qs.isSetMaxLevel() ? ((Integer) qs.getMaxLevel()).toString() : UNSET;
        String constant = qs.isSetConstant() ? SBaseHTMLFactory.booleanHTML(qs.getConstant()) : UNSET;
        map.put(ATTR_COMPARTMENT, compartment);
        map.put(SBML.ATTR_QUAL_INITIAL_LEVEL, initialLevel);
        map.put(SBML.ATTR_QUAL_MAX_LEVEL, maxLevel);
        map.put(SBML.ATTR_CONSTANT, constant);
        return map;
    }

    /**
     * Transition map.
     */
    public static Map<String, String> createTransitionMap(Transition transition) {
        Map<String, String> map = createNamedSBaseMap(transition);
        return map;
    }

    /**
     * Input map.
     */
    public static Map<String, String> createInputMap(Input input) {
        Map<String, String> map = createNamedSBaseMap(input);
        map.put(
                SBML.ATTR_QUAL_QUALITATIVE_SPECIES,
                input.isSetQualitativeSpecies() ? HtmlUtil.escape(input.getQualitativeSpecies()) : UNSET);
        map.put(
                SBML.ATTR_QUAL_TRANSITION_EFFECT,
                input.isSetTransitionEffect() ? input.getTransitionEffect().toString() : UNSET);
        map.put(SBML.ATTR_QUAL_SIGN, input.isSetSign() ? input.getSign().toString() : UNSET);
        map.put(
                SBML.ATTR_QUAL_THRESHOLD_LEVEL,
                input.isSetThresholdLevel() ? Integer.toString(input.getThresholdLevel()) : UNSET);
        return map;
    }

    /**
     * Output map.
     */
    public static Map<String, String> createOutputMap(Output output) {
        Map<String, String> map = createNamedSBaseMap(output);
        map.put(
                SBML.ATTR_QUAL_QUALITATIVE_SPECIES,
                output.isSetQualitativeSpecies() ? HtmlUtil.escape(output.getQualitativeSpecies()) : UNSET);
        map.put(
                SBML.ATTR_QUAL_TRANSITION_EFFECT,
                output.isSetTransitionEffect() ? output.getTransitionEffect().toString() : UNSET);
        map.put(
                SBML.ATTR_QUAL_OUTPUT_LEVEL,
                output.isSetOutputLevel() ? Integer.toString(output.getOutputLevel()) : UNSET);
        return map;
    }

    // FBC

    /**
     * GeneProduct map.
     */
    public static Map<String, String> createGeneProductMap(GeneProduct gp) {
        Map<String, String> map = createNamedSBaseMap(gp);
        return map;
    }

    /**
     * UserDefinedConstraint map (fbc v3): the bound parameters, the constraint
     * {@code lowerBound <= sum of the components <= upperBound} and a row per component.
     */
    public static Map<String, String> createUserDefinedConstraintMap(UserDefinedConstraint udc) {
        Map<String, String> map = createNamedSBaseMap(udc);
        Model model = udc.getModel();
        String lowerBound = udc.isSetLowerBound() ? udc.getLowerBound() : null;
        String upperBound = udc.isSetUpperBound() ? udc.getUpperBound() : null;
        map.put(SBML.ATTR_FBC_LOWER_BOUND, parameterHtml(model, lowerBound));
        map.put(SBML.ATTR_FBC_UPPER_BOUND, parameterHtml(model, upperBound));

        // getListOfUserDefinedConstraintComponents would add an empty list to the constraint
        List<UserDefinedConstraintComponent> components = udc.isSetListOfUserDefinedConstraintComponents()
                ? udc.getListOfUserDefinedConstraintComponents()
                : List.of();
        List<String> terms = new ArrayList<>();
        for (UserDefinedConstraintComponent component : components) {
            terms.add(componentTerm(component));
        }
        String sum = terms.isEmpty() ? "0" : String.join(" + ", terms);
        map.put(
                ATTR_CONSTRAINT,
                HtmlUtil.escape(lowerBound != null ? lowerBound : "?") + " &le; " + sum + " &le; "
                        + HtmlUtil.escape(upperBound != null ? upperBound : "?"));

        int k = 0;
        for (UserDefinedConstraintComponent component : components) {
            k++;
            String key = ATTR_COMPONENT + " "
                    + (component.isSetId() ? HtmlUtil.escape(component.getId()) : Integer.toString(k));
            String variableType = component.isSetVariableType() ? " (" + component.getVariableType() + ")" : "";
            map.put(key, componentTerm(component) + variableType + variableLinks(component));
        }
        return map;
    }

    /**
     * UserDefinedConstraintComponent map (fbc v3).
     */
    public static Map<String, String> createUserDefinedConstraintComponentMap(
            UserDefinedConstraintComponent component) {
        Map<String, String> map = createNamedSBaseMap(component);
        Model model = component.getModel();
        map.put(
                ATTR_COEFFICIENT,
                parameterHtml(model, component.isSetCoefficient() ? component.getCoefficient() : null));
        map.put(ATTR_VARIABLE, sidHtml(component.isSetVariable() ? component.getVariable() : null));
        map.put(ATTR_VARIABLE2, sidHtml(component.isSetVariable2() ? component.getVariable2() : null));
        map.put(
                SBML.ATTR_FBC_VARIABLE_TYPE,
                component.isSetVariableType() ? component.getVariableType().toString() : UNSET);
        return map;
    }

    /** The term of a component, {@code coefficient * variable [* variable2]}. */
    private static String componentTerm(UserDefinedConstraintComponent component) {
        List<String> factors = new ArrayList<>(3);
        factors.add(component.isSetCoefficient() ? component.getCoefficient() : "?");
        factors.add(component.isSetVariable() ? component.getVariable() : "?");
        if (component.isSetVariable2()) {
            factors.add(component.getVariable2());
        }
        return factors.stream().map(HtmlUtil::escape).collect(Collectors.joining(" &middot; "));
    }

    /** The links to the nodes of the variables of a component. */
    private static String variableLinks(UserDefinedConstraintComponent component) {
        String links = "";
        if (component.isSetVariable()) {
            links += idLink(component.getVariable());
        }
        if (component.isSetVariable2()) {
            links += idLink(component.getVariable2());
        }
        return links;
    }

    /** The SId with a link to its node, {@link #UNSET} for null. */
    private static String sidHtml(String sid) {
        return sid == null ? UNSET : HtmlUtil.escape(sid) + idLink(sid);
    }

    /**
     * The id of a parameter with its value, e.g. {@code five = 5}, and a link to its node,
     * {@link #UNSET} for null.
     */
    private static String parameterHtml(Model model, String sid) {
        if (sid == null) {
            return UNSET;
        }
        Parameter parameter = model != null ? model.getParameter(sid) : null;
        String value = parameter != null && parameter.isSetValue() ? " = " + numberString(parameter.getValue()) : "";
        return HtmlUtil.escape(sid) + value + idLink(sid);
    }

    // COMP

    // <root network SUID>/<metaid>
    private static final String LINK_TARGET_TEMPLATE = " <a href=\"" + BrowserHyperlinkListener.URL_SELECT_TARGET
            + "%s/%s\">" + GUIConstants.ICON_LINK.replace("{title}", "Link to the node in the network of its model.")
            + "</a>";

    /**
     * Submodel map: the referenced model, with a link to its network, and the conversion factors.
     *
     * @param targets the comp references of the open documents, may be null
     */
    public static Map<String, String> createSubmodelMap(Submodel submodel, CompTargets targets) {
        Map<String, String> map = createNamedSBaseMap(submodel);
        putIfSet(map, "modelRef", submodel.isSetModelRef() ? submodel.getModelRef() : null);
        putIfSet(
                map,
                "timeConversionFactor",
                submodel.isSetTimeConversionFactor() ? submodel.getTimeConversionFactor() : null);
        putIfSet(
                map,
                "extentConversionFactor",
                submodel.isSetExtentConversionFactor() ? submodel.getExtentConversionFactor() : null);
        map.put("deletions", Integer.toString(submodel.getDeletionCount()));
        SBaseRefResolver resolver = targets == null ? null : targets.resolver(submodel);
        if (resolver != null) {
            map.put("model", modelHtml(resolver.models().resolve(submodel), targets));
        }
        return map;
    }

    /**
     * Map of a port, deletion, replaced element or replaced by: the attributes that are
     * set, the chain of the reference, and its target with a link to the node in the
     * network of the target model.
     *
     * @param targets the comp references of the open documents, may be null
     */
    public static Map<String, String> createSBaseRefMap(SBaseRef ref, CompTargets targets) {
        Map<String, String> map = ref instanceof NamedSBase named ? createNamedSBaseMap(named) : createSBaseMap(ref);
        if (ref instanceof ReplacedElement replacedElement) {
            putIfSet(map, "submodelRef", replacedElement.isSetSubmodelRef() ? replacedElement.getSubmodelRef() : null);
            putIfSet(
                    map,
                    "conversionFactor",
                    replacedElement.isSetConversionFactor() ? replacedElement.getConversionFactor() : null);
            putIfSet(map, "deletion", replacedElement.isSetDeletion() ? replacedElement.getDeletion() : null);
        } else if (ref instanceof ReplacedBy replacedBy) {
            putIfSet(map, "submodelRef", replacedBy.isSetSubmodelRef() ? replacedBy.getSubmodelRef() : null);
        }
        putIfSet(map, "portRef", ref.isSetPortRef() ? ref.getPortRef() : null);
        putIfSet(map, "idRef", ref.isSetIdRef() ? ref.getIdRef() : null);
        putIfSet(map, "unitRef", ref.isSetUnitRef() ? ref.getUnitRef() : null);
        putIfSet(map, "metaIdRef", ref.isSetMetaIdRef() ? ref.getMetaIdRef() : null);
        if (ref.isSetSBaseRef() || map.containsKey("submodelRef")) {
            map.put("reference", HtmlUtil.escape(SBaseRefResolver.describe(ref)));
        }
        boolean replacesDeletion = ref instanceof ReplacedElement re && re.isSetDeletion();
        SBaseRefResolver resolver = targets == null ? null : targets.resolver(ref);
        if (resolver != null && !replacesDeletion) {
            map.put("target", targetHtml(resolver.resolve(ref), targets));
        }
        return map;
    }

    /** Puts the escaped value if it is set (not null and not empty). */
    private static void putIfSet(Map<String, String> map, String key, String value) {
        if (value != null && !value.isEmpty()) {
            map.put(key, HtmlUtil.escape(value));
        }
    }

    /**
     * The model definitions and external model definitions of the document, with the
     * model of each external model definition or why it could not be read.
     *
     * @param targets the comp references of the open documents, may be null
     */
    public static Map<String, String> createCompDocumentMap(SBMLDocument document, CompTargets targets) {
        Map<String, String> map = new LinkedHashMap<>();
        if (!(document.getExtension(CompConstants.shortLabel) instanceof CompSBMLDocumentPlugin plugin)) {
            return map;
        }
        SBaseRefResolver resolver = targets == null ? null : targets.resolver(document);
        // the getters of unset lists would add an empty list to the document
        List<ModelDefinition> modelDefinitions =
                plugin.isSetListOfModelDefinitions() ? plugin.getListOfModelDefinitions() : List.of();
        List<ExternalModelDefinition> externals =
                plugin.isSetListOfExternalModelDefinitions() ? plugin.getListOfExternalModelDefinitions() : List.of();
        for (ModelDefinition modelDefinition : modelDefinitions) {
            map.put(
                    "model definition " + HtmlUtil.escape(modelDefinition.getId()),
                    modelLink(modelDefinition, targets));
        }
        for (ExternalModelDefinition external : externals) {
            String source = HtmlUtil.escape(external.getSource());
            if (external.isSetModelRef()) {
                source += " (" + HtmlUtil.escape(external.getModelRef()) + ")";
            }
            if (resolver != null) {
                source += ": " + modelHtml(resolver.models().resolve(document, external.getId()), targets);
            }
            map.put("external model definition " + HtmlUtil.escape(external.getId()), source);
        }
        return map;
    }

    private static String modelHtml(ModelResolution resolution, CompTargets targets) {
        if (resolution instanceof ModelResolution.Resolved resolved) {
            return modelLink(resolved.model(), targets);
        }
        return String.format(
                "<span class=\"text-danger\">%s</span>",
                HtmlUtil.escape(((ModelResolution.Failed) resolution).reason()));
    }

    /** The name of the model, with a link to its network collection if there is one. */
    private static String modelLink(Model model, CompTargets targets) {
        String name = HtmlUtil.escape(modelName(model));
        return nodeLink(model, "", targets).map(link -> name + link).orElse(name);
    }

    private static String modelName(Model model) {
        return model.isSetId() ? model.getId() : "main model";
    }

    /**
     * The link to the node with the metaid in the network collection of the model, empty if
     * the model has no network collection.
     *
     * @param targets the network collections of the open documents, may be null
     */
    public static Optional<String> nodeLink(Model model, String metaId, CompTargets targets) {
        if (targets == null) {
            return Optional.empty();
        }
        return targets.rootNetwork(model)
                .map(root -> String.format(LINK_TARGET_TEMPLATE, root, HtmlUtil.escape(metaId)));
    }

    private static String targetHtml(SBaseRefResolution resolution, CompTargets targets) {
        if (resolution instanceof SBaseRefResolution.Resolved resolved) {
            SBase target = resolved.target();
            String name = target.isSetId() ? target.getId() : target.getMetaId();
            return String.format(
                    "%s <b>%s</b> in model %s%s",
                    HtmlUtil.escape(target.getElementName()),
                    HtmlUtil.escape(name),
                    HtmlUtil.escape(modelName(resolved.model())),
                    nodeLink(resolved.model(), target.getMetaId(), targets).orElse(""));
        }
        return String.format(
                "<span class=\"text-danger\">%s</span>",
                HtmlUtil.escape(((SBaseRefResolution.Unresolved) resolution).reason()));
    }

    // GROUP

    /**
     * Group map.
     */
    public static Map<String, String> createGroupMap(Group group) {
        Map<String, String> map = createNamedSBaseMap(group);
        // kind is required, but invalid models can miss it
        map.put("kind", group.isSetKind() ? group.getKind().name() : UNSET);

        StringBuilder membersStr = new StringBuilder("<ul>");
        // getListOfMembers would add an empty list to the group
        if (group.isSetListOfMembers()) {
            ListOfMembers members = group.getListOfMembers();
            if (members.isSetId()) {
                map.put("members id", HtmlUtil.escape(members.getId()));
            }
            if (members.isSetName()) {
                map.put("members name", HtmlUtil.escape(members.getName()));
            }
            for (Member member : members) {
                membersStr.append("<li>").append(memberHtml(member)).append("</li>");
            }
        }
        membersStr.append("</ul>");
        map.put("members", membersStr.toString());

        return map;
    }

    /**
     * Member of a group: the element name and the reference with a link to the node of the
     * element, or the reference in red if it does not resolve.
     */
    private static String memberHtml(Member member) {
        String ref = member.isSetIdRef() ? member.getIdRef() : member.getMetaIdRef();
        SBase sbase = member.getSBaseInstance();
        if (sbase == null) {
            return String.format("<span class=\"text-danger\">%s</span>", HtmlUtil.escape(ref));
        }
        String link = sbase.isSetMetaId() ? metaIdLink(sbase.getMetaId()) : "";
        return String.format("%s %s%s", sbase.getElementName(), HtmlUtil.escape(ref), link);
    }

    /**
     * Derived unit string.
     */
    private static String getDerivedUnitHtml(SBaseWithDerivedUnit usbase) {
        String units = usbase.getDerivedUnits();
        if (units == null || units.length() == 0) {
            units = UNSET;
        }
        return units;
    }
}

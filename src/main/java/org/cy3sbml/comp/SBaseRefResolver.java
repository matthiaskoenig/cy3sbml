package org.cy3sbml.comp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.swing.tree.TreeNode;
import org.cy3sbml.comp.ModelResolution.Failed;
import org.cy3sbml.comp.SBaseRefResolution.Resolved;
import org.cy3sbml.comp.SBaseRefResolution.Unresolved;
import org.cy3sbml.util.MappingUtil;
import org.sbml.jsbml.LocalParameter;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.UnitDefinition;
import org.sbml.jsbml.ext.SBasePlugin;
import org.sbml.jsbml.ext.comp.CompConstants;
import org.sbml.jsbml.ext.comp.CompModelPlugin;
import org.sbml.jsbml.ext.comp.Deletion;
import org.sbml.jsbml.ext.comp.Port;
import org.sbml.jsbml.ext.comp.ReplacedBy;
import org.sbml.jsbml.ext.comp.ReplacedElement;
import org.sbml.jsbml.ext.comp.SBaseRef;
import org.sbml.jsbml.ext.comp.Submodel;

/**
 * Resolves the element an {@link SBaseRef} of the comp package points to.
 * <p>
 * A {@link Port} points to an element of its own model. A {@link Deletion} points into
 * the model its submodel instantiates, a {@link ReplacedElement} and a
 * {@link ReplacedBy} into the model of the submodel with their {@code submodelRef}.
 * In that model, {@code portRef} follows the port, {@code idRef} finds the element in
 * the SId namespace, {@code unitRef} the unit definition and {@code metaIdRef} the
 * element with the metaid. A nested {@code sBaseRef} continues in the model of the
 * submodel found so far.
 * <p>
 * Every resolved target gets a metaid (see {@link MappingUtil#setSBaseMetaId}), so it
 * can be found by its metaid in the network of its model.
 */
public final class SBaseRefResolver {
    // bounds the chain of ports and nested references, which the specification forbids to loop
    private static final int MAX_DEPTH = 100;

    private final CompModels models;
    // by identity, JSBML's equals compares the content
    private final IdentityHashMap<Model, ModelIndex> indexes = new IdentityHashMap<>();

    public SBaseRefResolver(CompModels models) {
        this.models = models;
    }

    /** The resolver of the model references the SBaseRefs are resolved with. */
    public CompModels models() {
        return models;
    }

    /** The element the reference points to. */
    public SBaseRefResolution resolve(SBaseRef ref) {
        ModelResolution scope = scope(ref);
        if (scope instanceof Failed failed) {
            return new Unresolved(failed.reason());
        }
        ModelResolution.Resolved model = (ModelResolution.Resolved) scope;
        SBaseRefResolution resolution = resolve(model.model(), model.document(), ref, new ArrayList<>(), 0);
        if (resolution instanceof Resolved resolved && !(ref instanceof Deletion)) {
            resolution = deleted(ref, resolved)
                    .<SBaseRefResolution>map(Unresolved::new)
                    .orElse(resolution);
        }
        if (resolution instanceof Resolved resolved) {
            SBMLDocument document = resolved.target().getSBMLDocument();
            MappingUtil.setSBaseMetaId(document != null ? document : model.document(), resolved.target());
        }
        return resolution;
    }

    /**
     * The chain of the reference, e.g. {@code submodelRef=A > idRef=B > idRef=y}.
     */
    public static String describe(SBaseRef ref) {
        List<String> parts = new ArrayList<>();
        String submodelRef = submodelRef(ref);
        if (submodelRef != null) {
            parts.add("submodelRef=" + submodelRef);
        }
        for (SBaseRef current = ref;
                current != null;
                current = current.isSetSBaseRef() ? current.getSBaseRef() : null) {
            parts.add(reference(current));
        }
        return String.join(" > ", parts);
    }

    /**
     * Why the resolved target does not exist in the instantiated model: a deletion of a
     * submodel on the way removes the target or a submodel after it.
     *
     * @return the reason, empty if the target is not deleted
     */
    private Optional<String> deleted(SBaseRef ref, Resolved resolved) {
        List<Submodel> submodels = new ArrayList<>();
        Submodel first = scopeSubmodel(ref);
        if (first != null) {
            submodels.add(first);
        }
        submodels.addAll(resolved.path());
        for (int i = 0; i < submodels.size(); i++) {
            // by identity, JSBML's equals compares the content
            Set<SBase> removed = Collections.newSetFromMap(new IdentityHashMap<>());
            removed.addAll(submodels.subList(i + 1, submodels.size()));
            removed.add(resolved.target());
            Submodel submodel = submodels.get(i);
            for (Deletion deletion : submodel.getListOfDeletions()) {
                if (resolve(deletion) instanceof Resolved deletionTarget && removed.contains(deletionTarget.target())) {
                    String name = deletion.isSetId() ? deletion.getId() : describe(deletion);
                    return Optional.of(String.format(
                            "'%s' is deleted by the deletion '%s' of the submodel '%s'.",
                            deletionTarget.target().isSetId()
                                    ? deletionTarget.target().getId()
                                    : deletionTarget.target().getMetaId(),
                            name,
                            submodel.getId()));
                }
            }
        }
        return Optional.empty();
    }

    /** The model the reference points into, with its document. */
    private ModelResolution scope(SBaseRef ref) {
        if (ref instanceof Port) {
            Model model = ref.getModel();
            return model == null
                    ? new Failed("The port is not part of a model.")
                    : new ModelResolution.Resolved(model, ref.getSBMLDocument());
        }
        Submodel submodel = scopeSubmodel(ref);
        if (submodel == null) {
            return new Failed(
                    ref instanceof Deletion
                            ? "The deletion is not part of a submodel."
                            : String.format("There is no submodel '%s'.", submodelRef(ref)));
        }
        return models.resolve(submodel);
    }

    /**
     * The submodel the reference points into: the submodel of a deletion, the submodel
     * with the submodelRef of a replaced element or replaced by, null for a port.
     */
    private static Submodel scopeSubmodel(SBaseRef ref) {
        if (ref instanceof Port) {
            return null;
        }
        if (ref instanceof Deletion) {
            return parentSubmodel(ref);
        }
        String submodelRef = submodelRef(ref);
        Model model = ref.getModel();
        CompModelPlugin plugin = model == null ? null : compModelPlugin(model);
        return submodelRef == null || plugin == null ? null : plugin.getSubmodel(submodelRef);
    }

    private SBaseRefResolution resolve(
            Model model, SBMLDocument document, SBaseRef ref, List<Submodel> path, int depth) {
        if (depth > MAX_DEPTH) {
            return new Unresolved("The chain of references is too long, it may be a loop: " + describe(ref));
        }
        ModelIndex index = index(model);
        String modelName = modelName(model);
        SBase target;
        if (ref.isSetPortRef()) {
            Port port = index.ports.get(ref.getPortRef());
            if (port == null) {
                return new Unresolved(
                        String.format("There is no port '%s' in the model '%s'.", ref.getPortRef(), modelName));
            }
            SBaseRefResolution portTarget = resolve(model, document, port, path, depth + 1);
            if (!(portTarget instanceof Resolved resolved)) {
                return portTarget;
            }
            target = resolved.target();
            model = resolved.model();
            path = new ArrayList<>(resolved.path());
        } else if (ref.isSetIdRef()) {
            target = index.sIds.get(ref.getIdRef());
            if (target == null) {
                return new Unresolved(
                        String.format("There is no element '%s' in the model '%s'.", ref.getIdRef(), modelName));
            }
        } else if (ref.isSetUnitRef()) {
            target = model.getUnitDefinition(ref.getUnitRef());
            if (target == null || !model.getListOfUnitDefinitions().contains(target)) {
                return new Unresolved(String.format(
                        "There is no unit definition '%s' in the model '%s'.", ref.getUnitRef(), modelName));
            }
        } else if (ref.isSetMetaIdRef()) {
            target = index.metaIds.get(ref.getMetaIdRef());
            if (target == null) {
                return new Unresolved(String.format(
                        "There is no element with the metaid '%s' in the model '%s'.", ref.getMetaIdRef(), modelName));
            }
        } else {
            return new Unresolved("The reference sets none of portRef, idRef, unitRef and metaIdRef.");
        }

        if (!ref.isSetSBaseRef()) {
            return new Resolved(model, target, List.copyOf(path));
        }
        if (!(target instanceof Submodel submodel)) {
            return new Unresolved(String.format(
                    "'%s' is no submodel, so the nested reference '%s' cannot be followed.",
                    reference(ref), describe(ref.getSBaseRef())));
        }
        ModelResolution child = models.resolve(submodel);
        if (child instanceof Failed failed) {
            return new Unresolved(failed.reason());
        }
        ModelResolution.Resolved childModel = (ModelResolution.Resolved) child;
        List<Submodel> childPath = new ArrayList<>(path);
        childPath.add(submodel);
        return resolve(childModel.model(), childModel.document(), ref.getSBaseRef(), childPath, depth + 1);
    }

    private ModelIndex index(Model model) {
        return indexes.computeIfAbsent(model, ModelIndex::new);
    }

    private static Submodel parentSubmodel(SBase sbase) {
        for (TreeNode node = sbase.getParent(); node != null; node = node.getParent()) {
            if (node instanceof Submodel submodel) {
                return submodel;
            }
        }
        return null;
    }

    private static String submodelRef(SBaseRef ref) {
        if (ref instanceof ReplacedElement replacedElement && replacedElement.isSetSubmodelRef()) {
            return replacedElement.getSubmodelRef();
        }
        if (ref instanceof ReplacedBy replacedBy && replacedBy.isSetSubmodelRef()) {
            return replacedBy.getSubmodelRef();
        }
        return null;
    }

    private static String reference(SBaseRef ref) {
        if (ref.isSetPortRef()) {
            return "portRef=" + ref.getPortRef();
        } else if (ref.isSetIdRef()) {
            return "idRef=" + ref.getIdRef();
        } else if (ref.isSetUnitRef()) {
            return "unitRef=" + ref.getUnitRef();
        } else if (ref.isSetMetaIdRef()) {
            return "metaIdRef=" + ref.getMetaIdRef();
        }
        return "?";
    }

    private static CompModelPlugin compModelPlugin(Model model) {
        return model.getExtension(CompConstants.shortLabel) instanceof CompModelPlugin plugin ? plugin : null;
    }

    private static String modelName(Model model) {
        return model.isSetId() ? model.getId() : "main";
    }

    /**
     * The elements of a model by the identifier namespaces of the comp specification:
     * SIds (without unit definitions, ports and local parameters), PortSIds and metaids.
     */
    private static final class ModelIndex {
        private final Map<String, SBase> sIds = new HashMap<>();
        private final Map<String, Port> ports = new HashMap<>();
        private final Map<String, SBase> metaIds = new HashMap<>();

        ModelIndex(Model model) {
            index(model);
        }

        private void index(TreeNode node) {
            for (int i = 0; i < node.getChildCount(); i++) {
                TreeNode child = node.getChildAt(i);
                if (child instanceof SBase sbase) {
                    register(sbase);
                    index(child);
                } else if (child instanceof SBasePlugin) {
                    index(child);
                }
            }
        }

        private void register(SBase sbase) {
            if (sbase.isSetMetaId()) {
                metaIds.putIfAbsent(sbase.getMetaId(), sbase);
            }
            if (!sbase.isSetId()) {
                return;
            }
            if (sbase instanceof Port port) {
                ports.putIfAbsent(port.getId(), port);
            } else if (!(sbase instanceof UnitDefinition) && !(sbase instanceof LocalParameter)) {
                sIds.putIfAbsent(sbase.getId(), sbase);
            }
        }
    }
}

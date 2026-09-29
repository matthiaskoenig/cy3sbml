package org.cy3sbml.commands;

/** The descriptions of the arguments several commands share. */
final class CommandArguments {
    static final String NETWORK = "The network: its name, SUID:<SUID>, or current (the default) for the current"
            + " network. Any network of an SBML model imported by cy3sbml (base, kinetic, all or layout network).";
    static final String NETWORK_EXAMPLE = "current";
    static final String NODE_LIST = "The nodes: all, selected, unselected, a comma separated list of node names, or"
            + " <column>:<value>, e.g. \"sbml id:glc\" or SUID:<SUID>.";
    static final String NODE_LIST_EXAMPLE = "selected";

    private CommandArguments() {}
}

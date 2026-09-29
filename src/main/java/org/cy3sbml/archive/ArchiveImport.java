package org.cy3sbml.archive;

/**
 * The archive an SBML document was imported from.
 *
 * @param info     the content of the archive
 * @param location the location of the imported SBML file in the archive
 */
public record ArchiveImport(ArchiveInfo info, String location) {}

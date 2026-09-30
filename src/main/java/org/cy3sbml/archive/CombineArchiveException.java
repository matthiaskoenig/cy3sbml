package org.cy3sbml.archive;

/**
 * A COMBINE archive that cannot be imported: no zip file or COMBINE archive, an entry
 * outside the archive, too large, or without a readable manifest or an SBML file.
 */
public class CombineArchiveException extends Exception {
    private static final long serialVersionUID = 1L;

    /** Creates the exception with the message for the user. */
    public CombineArchiveException(String message) {
        super(message);
    }
}

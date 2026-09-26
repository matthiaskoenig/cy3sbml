package org.cy3sbml;

/**
 * Error thrown by the SBMLReaderTask if an SBML file cannot be read.
 */
public final class SBMLReaderError extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public SBMLReaderError(String s) {
        super(s);
    }

    public SBMLReaderError(String s, Throwable cause) {
        super(s, cause);
    }
}

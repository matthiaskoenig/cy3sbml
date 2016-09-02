package org.cy3sbml;

/**
 * Error thrown by the SBMLReaderTaskFactory.
 */
public final class SBMLReaderError extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public SBMLReaderError(String s) {
		super(s);
	}
}

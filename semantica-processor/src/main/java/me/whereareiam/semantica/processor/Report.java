package me.whereareiam.semantica.processor;

import lombok.RequiredArgsConstructor;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

/**
 * Reports problems of declarations to the compiler and counts the errors, so nothing is written for an interface
 * that has one.
 */
@RequiredArgsConstructor
final class Report {
	private final Messager messager;
	private int errors;

	/**
	 * Reports an error, which fails the build.
	 *
	 * @param element where it is
	 * @param text    what is wrong
	 */
	void error(Element element, String text) {
		errors++;
		messager.printMessage(Diagnostic.Kind.ERROR, text, element);
	}

	/**
	 * Reports a warning.
	 *
	 * @param element where it is
	 * @param text    what may be wrong
	 */
	void warning(Element element, String text) {
		messager.printMessage(Diagnostic.Kind.WARNING, text, element);
	}

	/**
	 * Returns how many errors were reported so far.
	 *
	 * @return the count
	 */
	int errors() {
		return errors;
	}
}

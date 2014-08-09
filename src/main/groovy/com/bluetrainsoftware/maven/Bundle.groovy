package com.bluetrainsoftware.maven

import groovy.transform.CompileStatic
import org.codehaus.plexus.util.SelectorUtils
import org.eclipse.jetty.util.resource.Resource
import org.eclipse.jetty.util.resource.ResourceCollection

import java.security.MessageDigest

/**
 *
 * @author: Richard Vowles - https://plus.google.com/+RichardVowles
 */
@CompileStatic
class Bundle {
	String id
	String type
	String name
	String suffix

	Properties properties

	List<String> patterns = null
	List<Resource> resources = []

	@Override
	String toString() {
		return "Bundle: jawr.${type?:'notype'}.${id?:'noid'}=${name?:'noname'}: ${resources?:'No resources'}"
	}

	void collectMatchingResources(String path, Resource resource) {
		if (patterns == null) {
			suffix = ".${type}"
			patterns = findMappingPatterns(properties.getProperty("jawr.${type}.bundle.${id}.mappings"))
		}

		if (path.endsWith(suffix)) {
			if (patterns != null) {
				for(String pattern : patterns) {
					if (SelectorUtils.matchPath(pattern, path, true)) {
						resources.add(resource)

						break
					}
				}
			}
		}
	}

	void writeBundle(File baseDir, boolean nameResources = true) {
		StringBuilder contents = new StringBuilder()

		// ensure consistent ordering for when we generate our SHA
		resources.sort({ Resource r1, Resource r2 ->
			return r1.name.compareTo(r2.name)
		}).each { Resource resource ->
			if (nameResources) {
				contents.append("// ${resource.name}")
			}

			contents.append(resource.inputStream.text)
		}

		String finalContents = contents.toString()

		MessageDigest md = MessageDigest.getInstance("SHA-1")
		byte[] digest = md.digest(finalContents.bytes)

	}

	List<String> findMappingPatterns(String line) {
		line = line?.trim()
		if (!line) { return null }

		return line.tokenize(",")
			.collect({it.trim()})
			.findAll({it.length() > 0})
		  .collect({SelectorUtils.ANT_HANDLER_PREFIX + it + SelectorUtils.PATTERN_HANDLER_SUFFIX})
	}
}

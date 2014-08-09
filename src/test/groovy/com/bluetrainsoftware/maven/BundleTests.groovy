package com.bluetrainsoftware.maven

import org.codehaus.plexus.util.SelectorUtils
import org.junit.Test

/**
 *
 * @author: Richard Vowles - https://plus.google.com/+RichardVowles
 */
class BundleTests {
	@Test
	public void matcher() {
		List<String> expects = ["mine/**/*.js", "yours", "**/*.js"].collect({SelectorUtils.ANT_HANDLER_PREFIX+it+SelectorUtils.PATTERN_HANDLER_SUFFIX})

		assert new Bundle().findMappingPatterns("mine/**/*.js,,yours,,**/*.js,,") == expects
	}
}

package com.bluetrainsoftware.maven

import groovy.transform.CompileStatic
import org.apache.maven.artifact.Artifact
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugins.annotations.ResolutionScope
import org.apache.maven.project.MavenProject
import org.eclipse.jetty.util.resource.Resource
import org.eclipse.jetty.util.resource.ResourceCollection

import java.util.regex.Matcher

/**
 *
 * @author: Richard Vowles - https://plus.google.com/+RichardVowles
 */
@CompileStatic
@Mojo(name="compile", requiresProject = true, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
class JawrCompilerMojo extends AbstractMojo {
	@Parameter(required = true, readonly = true, property = "project")
	protected MavenProject project

	@Override
	void execute() throws MojoExecutionException, MojoFailureException {
		if (project.packaging == "pom" || project.packaging == "tile") {
			return
		}

		File jawrFile = new File(project.basedir, "src/main/resources/META-INF/jawr.properties")

		if (jawrFile.exists()) {
			log.info("Processing resources in ${jawrFile.absolutePath}")
			processJawrProperties(jawrFile)
		} else {
			log.info("Project ${project} has no jawr resource in ${jawrFile.absolutePath} - skipping")
		}
	}

	ResourceCollection findDependencyResources() {
		List<Artifact> runtimeArtifacts = (List<Artifact>)project.getRuntimeArtifacts()
		List<Resource> foundResources = []

		File ourResources = new File(project.basedir, "src/main/resources/META-INF/resources")

		if (ourResources.exists()) {
			foundResources.add(Resource.newResource(ourResources))
		}

		runtimeArtifacts.each { Artifact artifact ->
			Resource resource = dependencyIsResource(artifact)

			if (resource) {
				foundResources.add(resource)
				log.info("JAWR-Staticizer: Found ${artifact} with resources.")
			}
		}

		if (foundResources.size() > 0) {
			Resource[] resources = new Resource[foundResources.size()]
			return new ResourceCollection(foundResources.toArray(resources))
		} else {
			return null
		}
	}

	protected List<Bundle> findLocalBundles(File jawrFile, Properties jawrProperties) {
		List<Bundle> bundles = []

		def pattern = ~/jawr.(css|js).bundle.(\w+).id=(.*)/

		jawrFile.eachLine { String line ->
			line = line.trim()

			Matcher match = line =~ pattern
			if (match.matches()) {
				List<String> matches = match[0] as List<String>
				bundles.add(new Bundle(type: matches[1], id:matches[2], name:matches[3], properties: jawrProperties))
			}
		}

		return bundles
	}

	protected void processJawrProperties(File jawrFile) {
		Properties jawr = new Properties()

		jawrFile.withReader { Reader reader ->
			jawr.load(reader)
		}

		if (jawr.size() == 0) {
			log.info("No properties to process - skipping")
			return
		}

		ResourceCollection resources = findDependencyResources()

		if (resources == null) {
			throw new MojoFailureException("JAWR Properties exist but no resources to process them with.")
		}

		List<Bundle> bundles = findLocalBundles(jawrFile, jawr)

		if (bundles.size() == 0) {
			log.info("No bundles in jawr.properties, skipping")
			return
		}


		discoverMatchingResources(bundles, resources.allResources)
	}

	void discoverMatchingResources(List<Bundle> bundles, Collection<Resource> resources) {
		for(Resource resource : resources) {
			String name = resource.name
			int pos = name.indexOf('META-INF/resources/')
			if (pos > 0) {
				name = name.substring(pos + 'META-INF/resources/'.length() - 1)
				log.debug("found ${name}")

				bundles.each { Bundle bundle ->
					bundle.collectMatchingResources(name, resource)
				}
			}
		}

		bundles.each {Bundle bundle ->
			println "Bundle ${bundle}"
		}
	}

	protected Resource dependencyIsResource(Artifact artifact) {
		if (artifact.type != "jar") { return null }

		Resource resource = Resource.newResource("jar:file:${artifact.file.absolutePath}!/META-INF/resources")

		if (resource && resource.exists()) {
			return resource
		} else {
			return null
		}
	}
}

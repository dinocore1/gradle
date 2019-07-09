/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.api.file

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.util.Requires
import org.gradle.util.TestPrecondition
import spock.lang.Issue
import spock.lang.Unroll

@Unroll
@Requires(TestPrecondition.SYMLINKS)
class OutputBrokenSymlinkCacheIntegrationTest extends AbstractIntegrationSpec {
    static taskName = 'producesLink'

    @Issue("https://github.com/gradle/gradle/issues/9906")
    def "don't cache if broken symlink in OutputDirectory"() {
        def root = file("root")
        def target = file("target")
        def link = root.file("link")

        buildFile << """
            import java.nio.file.*
            class ProducesLink extends DefaultTask {
                @OutputDirectory File outputDirectory

                @TaskAction execute() {
                    def link = Paths.get('${link}')
                    Files.deleteIfExists(link);
                    Files.createSymbolicLink(link, Paths.get('${target}'));
                }
            }

            task ${taskName}(type: ProducesLink) {
                outputDirectory = file '${root}'
                outputs.cacheIf { true }
            }
        """
        assert !link.exists()

        when:
        runTask(this)
        then:
        executedAndNotSkipped ":${taskName}"
        outputContains "Caching disabled for task ':${taskName}'"
        outputContains "Output contains uncacheable file: '${root}'"
    }

    @Issue("https://github.com/gradle/gradle/issues/9906")
    def "don't cache if broken symlink in OutputFile"() {
        def root = file("root").createDir()
        def target = file("target")
        def link = root.file("link")

        buildFile << """
            import java.nio.file.*
            class ProducesLink extends DefaultTask {
                @OutputFile Path outputFile
    
                @TaskAction execute() {
                    Files.deleteIfExists(outputFile);
                    Files.createSymbolicLink(outputFile, Paths.get('${target}'));
                }
            }
            
            task ${taskName}(type: ProducesLink) {
                outputFile = Paths.get('${link}')
                outputs.cacheIf { true }
            }
        """
        assert !link.exists()

        when:
        runTask(this)
        then:
        executedAndNotSkipped ":${taskName}"
        outputContains "Caching disabled for task ':${taskName}'"
        outputContains "Output contains uncacheable file: '${link}'"
    }

    static runTask = { AbstractIntegrationSpec context -> context.withBuildCache().run taskName, '--info' }
}

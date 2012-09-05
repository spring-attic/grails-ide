This 'runtime' bundle contains 'shared' classes that STS needs to add to the external Grails process's classpath.

The code in here is 'generic' in the sense that it is not Grails version specific and it is
used by both 'org.grails.ide.eclipse.runtime13' and 'org.grails.ide.eclipse.runtime22' as well as STS itself (i.e.
directly as a plugin that other plugins like the grails-IDE core plugin can depend on.

Care should be taken not to add any plugin dependencies to this plugin because it needs to be easily 'classloadable' into an external Grails process, thus it must be self-contained and not have external dependencies not commonly
available in a typical JVM runtime environment.

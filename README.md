# Grails IDE

  The Grails IDE brings you first class developer tooling for Grails into Eclipse.

It comes with Spring UAA (User Agent Analysis), an optional component that help us to collect some usage data. This is completely anonymous and helps us to understand better how the tooling is used and how to improve it in the future.

It also comes with the SpringSource Dashboard is an optional component, which brings you up-to-date information about SpringSource-related projects as well as an easy-to-use extension install to get additional tooling add-ons, like the tc Server Integration for Eclipse or the Cloud Foundry Integration for Eclipse.

## Installation (Release)

You can install the latest release of the Grails IDE from the Eclipse Marketplace by looking for "grails". You can also install it manually from one of the following update sites.

    http://dist.springsource.com/release/TOOLS/grails-ide

From version 3.1 and onward we will also be publishing 'one-stop' sites which contain all the required
dependencies for easy installation into a plain Eclipse. These sites are specific to a version of Eclipse
so make sure to pick the correct link for your version of Eclipse.

    http://dist.springsource.com/release/TOOLS/grails-ide/e3.7
    http://dist.springsource.com/release/TOOLS/grails-ide/e4.2

## Installation (Milestone)

You can install the latest milestone build of the Grails IDE manually from this udpate site:

    http://dist.springsource.com/milestone/TOOLS/grails-ide/
    
From version 3.1 and onward we will also be publishing 'one-stop' sites which contain all the required
dependencies for easy installation into a plain Eclipse. These sites are specific to a version of Eclipse
so make sure to pick the correct link for your version of Eclipse.
    
    http://dist.springsource.com/milestone/TOOLS/grails-ide/e3.7
    http://dist.springsource.com/milestone/TOOLS/grails-ide/e4.2

## Installation (CI builds)

If you want to live on the leading egde, you can also install always up-to-date continuous integration buids from this update site:

    http://dist.springsource.com/snapshot/TOOLS/grails-ide/nightly

The following sites contain the same content as the above site, but also contain all the required dependencies to install into plain Eclipse. 

    http://dist.springsource.com/snapshot/TOOLS/grails-ide/nightly/e3.7
    http://dist.springsource.com/snapshot/TOOLS/grails-ide/nightly/e4.2

But take care, those builds could be broken from time to time and might contain non-ship-ready
features that might never appear in the milestone or release builds.

## Questions and bug reports:

If you have a question that Google can't answer, the best way is to go to the forum:

    http://forum.springsource.org/forumdisplay.php?32-SpringSource-Tool-Suite

There you can also ask questions and search for other people with related or similar problems (and solutions). New versions of the Grails IDE (and other tooling that is brought to you by SpringSource) are announced there as well.

With regards to bug reports, please go to:

    https://issuetracker.springsource.com/browse/STS

and choose the GRAILS component when filing new issues.

## Developing Grails IDE

The remainder of this documents expects a familiarity with Eclipse architecture and how plugin development works.  If you are not already familiar with Eclipse development, then you may want to read some tutorials about the Eclipse plugin architecture and how to develop with it.  A good start is here: <http://www.ibm.com/developerworks/library/os-eclipse-plugindev1/>.


Also, using EGit is recommended but not required to provide integration between your workspace and git. EGit is installed into GGTS by default.  EGit docs are found here: <http://wiki.eclipse.org/EGit/User_Guide>.

These next steps will give you an environment where you can work on grails-ide, groovy-eclipse and eclipse-integration-commons (the dependencies of grails-ide).  We are working on a more streamlined set of steps for developers who simply want to work on grails-ide.

### Getting the Grails-ide source code into Eclipse

To get the source code into your workspace and (mostly) compiling, do the following:

1. Start with the latest version of the Groovy/Grails Tool Suite (GGTS).  Starting from an eclipse base is possible but extra software (like Groovy/Eclipse) would need to be installed.
2. Open with a clean workspace.
3. Clone the grails-ide git repo `git@github.com:SpringSource/grails-ide.git` (optional- use egit to do the cloning)
4. import all projects into the workspace (optional- use egit to do the importing)
5. There will be errors in the following test projects. The errors come from dependencies on test projects located in other git repositories.  If you want to compile and run the tests inside of Eclipse, you will need to clone the `eclipse-integration-commons` and the `groovy-eclipse` projects into your workspace. More information on this below.  Otherwise, you can close these projects.
  * `org.grails.ide.eclipse.test`
  * `org.grails.ide.eclipse.test.ui`
  * `org.grails.ide.eclipse.test.util`
6. Launch a runtime workbench (in debug mode if you want to be able to use the debugger) to verify that your workspace is correctly configured.
7. Rejoice!


### Getting the remaining Grails-IDE related source code into Eclipse

By cloning only the `grails-ide` repository, and not `eclipse-integration-commons` or `groovy-eclipse`, the Grails-IDE projects will resolve against the binaries of your Eclipse installation (aka the target platform).  Unless you explicitly installed the source code, it will not be available to browse.  Cloning these repositories will not only make the source code available for these projects, but it will also make (most of) the Grails-IDE test projects compile cleanly inside of Eclipse:

* Groovy-Eclipse: git@github.com:groovy/groovy-eclipse.git
* Eclipse-Integration-Commons: git@github.com:SpringSource/eclipse-integration-commons.git

More information on setting up the Groovy-Eclipse dev environment is available here: http://groovy.codehaus.org/Getting+Started+With+Groovy-Eclipse+Source+Code.  Note that there are projects that will not compile unless you have m2e (Maven-eclipse support) installed in your Eclipse.  These projects can be closed.

*Important* also, close the org.codehaus.groovy20 plugin unless you are working on Grails 2.2.x or later, which requires Groovy 2.0.

Since GGTS does not ship with the source code for Eclipse projects (Eclipse Platform, JDT, JFace, SWT, etc), you must install them separately.  Use this update site and install the Eclipse SDK (this site will be available until the Eclipse Juno release at the end of June 2012):

  * Composite Artifact Repository - <http://download.eclipse.org/eclipse/updates/4.2-I-builds>

Use this update site after Eclipse Juno is released: 

  * The Eclipse Project Updates - <http://download.eclipse.org/eclipse/updates/4.2>

### Getting the tests to compile inside of Eclipse

There are two test suites in Grails-IDE:

* `org.grails.ide.eclipse.test.AllGrailsTests`: tests the core Grails functionality including creating projects, editor support, launching, debugging, etc.  This suite is of general interest and should be run to ensure correctness.  **Usually takes about one hour to run completely** since it downloads multiple versions of grails, issues many commands, starts and stops servers, etc.
* `org.grails.ide.eclipse.test.ui.AllGrailsUITests`: tests wizards, window state, and other UI.  This test suite is generally a more fragile and may not pass on Windows.  **This test suite is not necessary to run** unless you know you are working in this area.  Also, if you want these tests to compile you will need to install swt-bot, which is our UI testing framework.  Keep this test suite closed unless you have SWT-Bot installed.  SWT-bot is available form this update site: http://download.eclipse.org/technology/swtbot/galileo/dev-build/update-site

### Running the tests inside of Eclipse

Once the test projects are compiling cleanly inside of Eclipse, you can now run the tests.  If you would like to contribute any significant piece of code to the Grails-IDE project, you will be required to include test cases.

To run the tests:

1. Select the `org.grails.ide.eclipse.test.AllGrailsTests` class
2. Right-click -> Run as -> Junit Plugin Tests.
3. This will likely fail due to memory constraints (you may even want to end the test if it is taking too long before even starting)
4. You must change your launch configuration and augment memory settings
5. Select the arrow next to the Debug icon in the toolbar -> Debug configurations...
6. Choose the launch configuration you just created in step 2
7. Go to the Arguments tab and add the following to the VM arguments: `-Xmx1024M -XX:PermSize=64M -XX:MaxPermSize=128M`
8. Re-run the tests.  They should be passing, except for the Groovy debug tests.
9. Rejoice!

The Groovy debug tests will fail unless you enable JDT weaving in your launch configuration.  Unless you are working on Groovy debugging, this is not something you should worry about.


## Building Grails IDE

The Grails IDE project uses [Maven](http://maven.apache.org/) Tycho [Tycho](http://eclipse.org/tycho) to do continuous integration builds and to produce p2 repos and update sites. To build the project yourself, you can execute:

    mvn -Pe37 -Dmaven.test.skip=true clean install

This will use maven to compile all Grails-IDE plugins and package them up to produce an update site for a snapshot build.  The update site will be located in `grails-ide/org.grails.ide.eclipse.site/target`.

If you want to run tests during your build, then remove `-Dmaven.test.skip=true`.


## Contributing

Here are some ways for you to get involved in the community:

  * Get involved with the Spring community on the Spring Community Forums.  Please help out on the [forum](http://forum.springsource.org/forumdisplay.php?32-SpringSource-Tool-Suite) by responding to questions and joining the debate.
  * Create [JIRA](https://issuetracker.springsource.com/browse/STS) tickets for bugs and new features and comment and vote on the ones that you are interested in.  
  * Github is for social coding: if you want to write code, we encourage contributions through pull requests from [forks of this repository](http://help.github.com/forking/). If you want to contribute code this way, please reference a JIRA ticket as well covering the specific issue you are addressing.
  * Watch for upcoming articles on Spring by [subscribing](http://www.springsource.org/node/feed) to springframework.org
  * Twitter: @mlippert @andy_clement @werdnagreb The Grails-IDE core team often tweets about new features and updates

Before we accept a pull request we will need you to sign the [contributor's agreement](https://support.springsource.com/spring_eclipsecla_committer_signup). When completing that page, select 'Grails IDE' from the pulldown. Signing the contributor's agreement does not grant anyone commit rights to the main repository, but it does mean that we can accept your contributions, and you will get an author credit if we do. Active contributors might be asked to join the core team, and given the ability to merge pull requests.

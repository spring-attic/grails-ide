/*
 * Copyright 2004-2005 the original author or authors.
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
package org.grails.ide.api.impl;

import gant.Gant;
import grails.build.logging.GrailsConsole;
import grails.util.BuildSettings;
import grails.util.BuildSettingsHolder;
import grails.util.CosineSimilarity;
import grails.util.Environment;
import grails.util.GrailsNameUtils;
import grails.util.PluginBuildSettings;
import groovy.lang.Closure;
import groovy.lang.ExpandoMetaClass;
import groovy.lang.GroovyObject;
import groovy.lang.GroovySystem;
import groovy.util.AntBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.tools.ant.Project;
import org.codehaus.gant.GantBinding;
import org.codehaus.gant.GantMetaClass;
import org.codehaus.groovy.grails.cli.CommandLineInputHandler;
import org.codehaus.groovy.grails.cli.ScriptNameResolver;
import org.codehaus.groovy.grails.cli.ScriptNotFoundException;
import org.codehaus.groovy.grails.cli.interactive.InteractiveMode;
import org.codehaus.groovy.grails.cli.parsing.CommandLine;
import org.codehaus.groovy.grails.cli.parsing.CommandLineParser;
import org.codehaus.groovy.grails.cli.parsing.DefaultCommandLine;
import org.codehaus.groovy.grails.cli.parsing.ParseException;
import org.codehaus.groovy.grails.cli.support.ClasspathConfigurer;
import org.codehaus.groovy.grails.cli.support.PluginPathDiscoverySupport;
import org.codehaus.groovy.grails.cli.support.ScriptBindingInitializer;
import org.codehaus.groovy.grails.io.support.PathMatchingResourcePatternResolver;
import org.codehaus.groovy.grails.io.support.Resource;
import org.codehaus.groovy.grails.plugins.GrailsPluginUtils;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.grails.ide.api.IGrailsConsole;

//This is a copy of GrailsScriptRunner, then modified to implement 'tooling API'.


/**
 * Handles Grails command line interface for running scripts.
 *
 * @author Graeme Rocher
 *
 * @since 0.4
 */
public class APIScriptRunner {

    private static final Pattern scriptFilePattern = Pattern.compile("^[^_]\\w+\\.groovy$");

    private static InputStream originalIn;

    private static PrintStream originalOut;
    
    @SuppressWarnings("rawtypes")
    public static final
    Closure DO_NOTHING_CLOSURE = new Closure(APIScriptRunner.class) {
        private static final long serialVersionUID = 1L;
        @Override public Object call(Object arguments) { return null; }
        @Override public Object call() { return null; }
        @Override public Object call(Object... args) { return null; }
    };
    private PluginPathDiscoverySupport pluginPathSupport;
    private BuildSettings settings;

    private PrintStream out = System.out;
    private boolean isInteractive = true;
    private URLClassLoader classLoader;
    private GrailsConsole console = GrailsConsole.getInstance(); //TODO: should be passed in when running commands.

    private File scriptCacheDir;
    private final List<File> scriptsAllowedOutsideOfProject = new ArrayList<File>();

    public APIScriptRunner(BuildSettings settings) {
        if (originalIn == null) {
            originalIn = System.in;
            originalOut = System.out;
        }
        this.settings = settings;
        pluginPathSupport = new PluginPathDiscoverySupport(settings);
    }

    public void setInteractive(boolean interactive) {
        isInteractive = interactive;
    }

    public static CommandLineParser getCommandLineParser() {
        CommandLineParser parser = new CommandLineParser();
        parser.addOption(CommandLine.REFRESH_DEPENDENCIES_ARGUMENT, "Whether to force a resolve of dependencies (skipping any caching)");
        parser.addOption(CommandLine.VERBOSE_ARGUMENT, "Enable verbose output");
        parser.addOption(CommandLine.OFFLINE_ARGUMENT, "Indicates that Grails should not connect to any remote servers during processing of the build");
        parser.addOption(CommandLine.STACKTRACE_ARGUMENT, "Enable stack traces in output");
        parser.addOption(CommandLine.AGENT_ARGUMENT, "Enable the reloading agent");
        parser.addOption(CommandLine.NON_INTERACTIVE_ARGUMENT, "Whether to allow the command line to request input");
        parser.addOption(CommandLine.VERSION_ARGUMENT, "Current Grails version");
        parser.addOption(CommandLine.NOANSI_ARGUMENT, "Disables ANSI output");
        return parser;
    }

    private static void exitWithError(String error, Throwable t) {
        GrailsConsole grailsConsole = GrailsConsole.getInstance();
        if (t != null) {
            grailsConsole.error(error, t);
        }
        else {
            grailsConsole.error(error);
        }
        grailsConsole.flush();
        System.exit(1);
    }

    private static ScriptAndArgs processArgumentsAndReturnScriptName(CommandLine commandLine) {

        if (commandLine.hasOption(CommandLine.VERBOSE_ARGUMENT)) {
            GrailsConsole.getInstance().setVerbose(true);
        }
        if (commandLine.hasOption(CommandLine.STACKTRACE_ARGUMENT)) {
            GrailsConsole.getInstance().setStacktrace(true);
        }

        processSystemArguments(commandLine);
        return processAndReturnArguments(commandLine);
    }

    private static ScriptAndArgs processAndReturnArguments(CommandLine commandLine) {
        ScriptAndArgs info = new ScriptAndArgs();
        if (Environment.isSystemSet()) {
            info.env = Environment.getCurrent().getName();
        }
        else if (commandLine.getEnvironment() != null) {
            info.env = commandLine.getEnvironment();
        }

        info.inputName = commandLine.getCommandName();
        info.name = GrailsNameUtils.getNameFromScript(commandLine.getCommandName());
        return info;
    }

    private static void processSystemArguments(CommandLine allArgs) {
        Properties systemProps = allArgs.getSystemProperties();
        if (systemProps != null) {
            for (Map.Entry<Object, Object> entry : systemProps.entrySet()) {
                System.setProperty(entry.getKey().toString(), entry.getValue().toString());
            }
        }
    }

    public PrintStream getOut() {
        return out;
    }

    public void setOut(PrintStream outputStream) {
        out = outputStream;
    }

    public int executeCommand(String scriptName, String args) {
        return executeCommand(scriptName, args, null);
    }

    public int executeCommand(String scriptName, String args, String env) {
        // Populate the root loader with all libraries that this app
        // depends on. If a root loader doesn't exist yet, create it now.

        if (args != null) {
            System.setProperty("grails.cli.args", args.replace(' ', '\n')); //TODO: Who reads this? Can we gte it to them in another way?
        }
        else {
            // If GrailsScriptRunner is executed more than once in a
            // single JVM, we have to make sure that the CLI args are reset.
            System.setProperty("grails.cli.args", "");
        }

        CommandLineParser parser = getCommandLineParser();
        DefaultCommandLine commandLine = (DefaultCommandLine) parser.parseString(scriptName,args);
        if (env != null) {
            commandLine.setEnvironment(env);
        }

        return executeCommand(commandLine, scriptName, env);
    }

    private int executeCommand(CommandLine commandLine, String scriptName, String env) {
        @SuppressWarnings("hiding")
        GrailsConsole console = getConsole(commandLine);

        // Load the BuildSettings file for this project if it exists. Note
        // that this does not load any environment-specific settings.
        try {
            System.setProperty("disable.grails.plugin.transform", "true");

            console.updateStatus("Loading Grails " + settings.getGrailsVersion());
            settings.loadConfig();

            System.setProperty("springloaded.directoriesContainingReloadableCode",
                   settings.getClassesDir().getAbsolutePath() + ',' +
                   settings.getPluginClassesDir().getAbsolutePath());
        }
        catch (Exception e) {
            console.error("There was an error loading the BuildConfig: " + e.getMessage(), e);
            System.exit(1);
        }
        finally {
            System.setProperty("disable.grails.plugin.transform", "false");
        }

        // Add some extra binding variables that are now available.
        // settings.setGrailsEnv(env);
        // settings.setDefaultEnv(useDefaultEnv);


        try {
            BuildSettingsHolder.setSettings(settings);
            return callPluginOrGrailsScript(commandLine, scriptName, env);
        } finally {
            GrailsConsole.getInstance().flush();
            BuildSettingsHolder.setSettings(null);
        }
    }

    private GrailsConsole getConsole(CommandLine commandLine) {
        @SuppressWarnings("hiding") GrailsConsole console = GrailsConsole.getInstance();

        // Set the console display properties
        console.setAnsiEnabled(!commandLine.hasOption(CommandLine.NOANSI_ARGUMENT));
        console.setStacktrace(commandLine.hasOption(CommandLine.STACKTRACE_ARGUMENT));
        console.setVerbose(commandLine.hasOption(CommandLine.VERBOSE_ARGUMENT));

        return console;
    }

    private void setRunningEnvironment(CommandLine commandLine, String env) {
        // Get the default environment if one hasn't been set.
        System.setProperty("base.dir", settings.getBaseDir().getPath());

        if (env != null) {
            // Add some extra binding variables that are now available.
            settings.setGrailsEnv(env);
            settings.setDefaultEnv(false);
        }
        else {
            // Add some extra binding variables that are now available.
            settings.setGrailsEnv(commandLine.getEnvironment());
            settings.setDefaultEnv(!commandLine.isEnvironmentSet());
        }
    }

    private int callPluginOrGrailsScript(CommandLine commandLine, String scriptName, String env) {
        initializeState(scriptName);
        return executeScriptWithCaching(commandLine,scriptName, env);
    }

    public int executeScriptWithCaching(CommandLine commandLine) {
        processSystemArguments(commandLine);

        System.setProperty("grails.cli.args", commandLine.getRemainingArgsLineSeparated());
        return executeScriptWithCaching(commandLine, GrailsNameUtils.getNameFromScript(commandLine.getCommandName()), commandLine.getEnvironment());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private int executeScriptWithCaching(CommandLine commandLine, String scriptName, String env) {
        List<File> potentialScripts;
        List<File> allScripts = getAvailableScripts();
        GantBinding binding = new GantBinding();
        binding.setVariable("scriptName", scriptName);

        setDefaultInputStream(binding);

        // Now find what scripts match the one requested by the user.
        potentialScripts = getPotentialScripts(scriptName, allScripts);

        if (potentialScripts.size() == 0) {
            try {
                File aliasFile = new File(settings.getUserHome(), ".grails/.aliases");
                if (aliasFile.exists()) {
                    Properties aliasProperties = new Properties();
                    aliasProperties.load(new FileReader(aliasFile));
                    if (aliasProperties.containsKey(commandLine.getCommandName())) {
                        String aliasValue = (String) aliasProperties.get(commandLine.getCommandName());
                        String[] aliasPieces = aliasValue.split(" ");
                        String commandName = aliasPieces[0];
                        String correspondingScriptName = GrailsNameUtils.getNameFromScript(commandName);
                        potentialScripts = getPotentialScripts(correspondingScriptName, allScripts);

                        if (potentialScripts.size() > 0) {
                            String[] additionalArgs = new String[aliasPieces.length - 1];
                            System.arraycopy(aliasPieces, 1, additionalArgs, 0, additionalArgs.length);
                            insertArgumentsInFrontOfExistingArguments(commandLine, additionalArgs);
                        }
                    }
                }
            } catch (Exception e) {
                console.error(e);
            }
        }

        // First try to load the script from its file. If there is no
        // file, then attempt to load it as a pre-compiled script. If
        // that fails, then let the user know and then exit.
        if (potentialScripts.size() > 0) {
            potentialScripts = (List) DefaultGroovyMethods.unique(potentialScripts);
            final File scriptFile = potentialScripts.get(0);
            if (!isGrailsProject() && !isExternalScript(scriptFile)) {
                return handleScriptExecutedOutsideProjectError();
            }
            return executeScriptFile(commandLine, scriptName, env, binding, scriptFile);
        }

        return attemptPrecompiledScriptExecute(commandLine, scriptName, env, binding, allScripts);
    }

    private List<File> getPotentialScripts(String scriptName, List<File> allScripts) {
        List<File> potentialScripts;
        boolean exactMatchFound = false;
        potentialScripts = new ArrayList<File>();
        for (File scriptPath : allScripts) {
            String fileName = scriptPath.getName();
            String scriptFileName = fileName.substring(0, fileName.length() - 7); // trim .groovy extension
            if (scriptFileName.endsWith("_")) {
                scriptsAllowedOutsideOfProject.add(scriptPath);
                scriptFileName = scriptFileName.substring(0, scriptFileName.length()-1);
            }

            if (scriptFileName.equals(scriptName)) {
                potentialScripts.add(scriptPath);
                exactMatchFound = true;
                continue;
            }

            if (!exactMatchFound && ScriptNameResolver.resolvesTo(scriptName, scriptFileName)) {
                potentialScripts.add(scriptPath);
            }
        }
        return potentialScripts;
    }

    private void insertArgumentsInFrontOfExistingArguments(CommandLine commandLine, String[] argumentsToInsert) {
        List<String> remainingArgs = commandLine.getRemainingArgs();
        for (int i = argumentsToInsert.length - 1; i >= 0; i-- ) {
            remainingArgs.add(0, argumentsToInsert[i]);
        }
    }

    private int attemptPrecompiledScriptExecute(CommandLine commandLine, String scriptName, String env, GantBinding binding, List<File> allScripts) {
        console.updateStatus("Running pre-compiled script");

        // Must be called before the binding is initialised.
        setRunningEnvironment(commandLine, env);

        // Get Gant to load the class by name using our class loader.
        ScriptBindingInitializer bindingInitializer = new ScriptBindingInitializer(commandLine,
                classLoader, settings, pluginPathSupport, isInteractive);
        Gant gant = new Gant(bindingInitializer.initBinding(binding, scriptName), classLoader);

        try {
            loadScriptClass(gant, scriptName);
        }
        catch (ScriptNotFoundException e) {
            if (!isInteractive || InteractiveMode.isActive()) {
                throw e;
            }
            scriptName = fixScriptName(scriptName, allScripts);
            if (scriptName == null) {
                throw e;
            }

            try {
                loadScriptClass(gant, scriptName);
            }
            catch (ScriptNotFoundException ce) {
                return executeScriptWithCaching(commandLine, scriptName, env);
            }

            // at this point if they were calling a script that has a non-default
            // env (e.g. war or test-app) it wouldn't have been correctly set, so
            // set it now, but only if they didn't specify the env (e.g. "grails test war" -> "grails test war")

            if (Boolean.TRUE.toString().equals(System.getProperty(Environment.DEFAULT))) {
                commandLine.setCommand(GrailsNameUtils.getScriptName(scriptName));
                env = commandLine.lookupEnvironmentForCommand();
                binding.setVariable("grailsEnv", env);
                settings.setGrailsEnv(env);
                System.setProperty(Environment.KEY, env);
                settings.setDefaultEnv(false);
                System.setProperty(Environment.DEFAULT, Boolean.FALSE.toString());
            }
        }

        return executeWithGantInstance(gant, DO_NOTHING_CLOSURE, binding).exitCode;
    }

    private int executeScriptFile(CommandLine commandLine, String scriptName, String env, GantBinding binding, File scriptFile) {
        // We can now safely set the default environment
        String scriptFileName = getScriptNameFromFile(scriptFile);
        setRunningEnvironment(commandLine, env);
        binding.setVariable("scriptName", scriptFileName);
        setInteractive(!commandLine.hasOption(CommandLine.NON_INTERACTIVE_ARGUMENT));

        // Setup the script to call.
        ScriptBindingInitializer bindingInitializer = new ScriptBindingInitializer(
                commandLine, classLoader,settings, pluginPathSupport, isInteractive);
        Gant gant = new Gant(bindingInitializer.initBinding(binding, scriptName), classLoader);
        gant.setUseCache(true);
        gant.setCacheDirectory(scriptCacheDir);
        GantResult result = null;
        try {
            gant.loadScript(scriptFile.toURI().toURL());
            result = executeWithGantInstance(gant, DO_NOTHING_CLOSURE, binding);
            return result.exitCode;
        } catch (IOException e) {
            console.error("I/O exception loading script [" + e.getMessage() + "]: " + e.getMessage());
            return 1;
        }
        finally {
            cleanup(result, binding);
        }
    }

    @SuppressWarnings("rawtypes")
    private void cleanup(GantResult result, GantBinding binding) {
        if (result != null) {
            Class cls = GantMetaClass.class;
            try {
                Field methodsInvoked = cls.getDeclaredField("methodsInvoked");
                methodsInvoked.setAccessible(true);
                Set methodsInvokedSet = (Set) methodsInvoked.get(cls);
                if (methodsInvokedSet != null) {
                    methodsInvokedSet.clear();
                }
            } catch (NoSuchFieldException e) {
                // ignore
            } catch (IllegalAccessException e) {
                // ignore
            }
        }
        System.setIn(originalIn);
        System.setOut(originalOut);
        GrailsPluginUtils.clearCaches();
        Map variables = binding.getVariables();
        Object pluginsSettingsObject = variables.get("pluginsSettings");
        if (pluginsSettingsObject instanceof PluginBuildSettings) {
            ((PluginBuildSettings)pluginsSettingsObject).clearCache();
        }
        GroovySystem.getMetaClassRegistry().removeMetaClass(GantBinding.class);
        GroovySystem.getMetaClassRegistry().removeMetaClass(Gant.class);
    }

    public void initializeState() {
        initializeState(null);
    }

    private void initializeState(String scriptName) {
        // The directory where scripts are cached.
        scriptCacheDir = new File(settings.getProjectWorkDir(), "scriptCache");
        console = GrailsConsole.getInstance();
        // Add the remaining JARs (from 'grailsHome', the app, and
        // the plugins) to the root loader.

        boolean skipPlugins = scriptName != null && ("UninstallPlugin".equals(scriptName) || "InstallPlugin".equals(scriptName));

        console.updateStatus("Configuring classpath");
        ClasspathConfigurer configurer = new ClasspathConfigurer(pluginPathSupport, settings, skipPlugins);
        if ("DependencyReport".equals(scriptName) || "Upgrade".equals(scriptName)) {
            configurer.setExitOnResolveError(false);
        }
        classLoader = configurer.configuredClassLoader();
        initializeLogging();
    }

    private int handleScriptExecutedOutsideProjectError() {
        console.error(settings.getBaseDir().getPath() + " does not appear to be part of a Grails application.");
        console.error("The following commands are supported outside of a project:");
        Collections.sort(scriptsAllowedOutsideOfProject, new Comparator<File>() {
            public int compare(File resource, File resource1) {
                return resource.getName().compareTo(resource1.getName());
            }
        });
        for (File file : scriptsAllowedOutsideOfProject) {
            console.log("\t" + GrailsNameUtils.getScriptName(file.getName()));
        }
        console.addStatus("Run 'grails help' for a complete list of available scripts.");
        return -1;
    }

    protected void initializeLogging() {
        if (settings.getGrailsHome() == null) {
            return;
        }

        try {
            Class<?> cls = Thread.currentThread().getContextClassLoader().loadClass("org.apache.log4j.PropertyConfigurator");
            Method configure = cls.getMethod("configure", URL.class);
            configure.setAccessible(true);
            File f = new File(settings.getGrailsHome() + "/scripts/log4j.properties");
            configure.invoke(cls, f.toURI().toURL());
        } catch (Throwable e) {
            console.verbose("Log4j was not found on the classpath and will not be used for command line logging. Cause "+e.getClass().getName()+": " + e.getMessage());
        }
    }

    private void setDefaultInputStream(GantBinding binding) {

        // Gant does not initialise the default input stream for
        // the Ant project, so we manually do it here.
        AntBuilder antBuilder = (AntBuilder) binding.getVariable("ant");
        Project p = antBuilder.getAntProject();

        try {
            System.setIn(originalIn);
            p.setInputHandler(new CommandLineInputHandler());
            p.setDefaultInputStream(originalIn);
        }
        catch (NoSuchMethodError nsme) {
            // will only happen due to a bug in JRockit
            // note - the only approach that works is to loop through the public methods
            for (Method m : p.getClass().getMethods()) {
                if ("setDefaultInputStream".equals(m.getName()) && m.getParameterTypes().length == 1 &&
                        InputStream.class.equals(m.getParameterTypes()[0])) {
                    try {
                        m.invoke(p, originalIn);
                        break;
                    }
                    catch (Exception e) {
                        // shouldn't happen, but let it bubble up to the catch(Throwable)
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    private void loadScriptClass(Gant gant, String scriptName) {
        try {
            // try externalized script first
            gant.loadScriptClass(scriptName + "_");
        }
        catch (Exception e) {
            try {
                gant.loadScriptClass(scriptName);
            }
            catch (Exception ex) {
                if (ex instanceof ClassNotFoundException &&
                        ex.getMessage() != null &&
                        ex.getMessage().contains(scriptName)) {
                    throw new ScriptNotFoundException(scriptName);
                }
            }
        }
    }

    private String fixScriptName(String scriptName, List<File> allScripts) {
        try {
            Set<String> names = new HashSet<String>();
            for (File script : allScripts) {
                String fileName = script.getName();
                names.add(fileName.substring(0, fileName.length() - 7));
            }
            List<String> mostSimilar = CosineSimilarity.mostSimilar(scriptName, names);
            if (mostSimilar.isEmpty()) {
                return null;
            }
            List<String> topMatches = mostSimilar.subList(0, Math.min(5, mostSimilar.size()));
            return askUserForBestMatch(scriptName, topMatches);
        }
        catch (Exception e) {
            return null;
        }
    }

    private String askUserForBestMatch(String scriptName, List<String> topMatches) {
        @SuppressWarnings("hiding") GrailsConsole console = GrailsConsole.getInstance();
        console.addStatus("Script '" + scriptName + "' not found, did you mean:");
        int i = 0;
        for (String s : topMatches) {
            console.log("   " + ++i + ") " + s);
        }

        int attempts = 0;
        while (true) {
            String selection = console.userInput("Please make a selection or enter Q to quit: ");

            if ("Q".equalsIgnoreCase(selection)) {
                System.exit(0);
            }

            try {
                int number = Integer.parseInt(selection);
                if (number > 0 && number <= topMatches.size()) {
                    return topMatches.get(number - 1);
                }
            }
            catch (NumberFormatException ignored) {
                // ignored
            }

            attempts++;
            if (attempts > 4) {
                exitWithError("Selection not found.", null);
            }
        }
    }

    private GantResult executeWithGantInstance(Gant gant, final Closure<?> doNothingClosure, GantBinding binding) {
        GantResult result = new GantResult();
        try {
            result.script = gant.prepareTargets();
            gant.setAllPerTargetPostHooks(doNothingClosure);
            gant.setAllPerTargetPreHooks(doNothingClosure);
            // Invoke the default target.
            result.exitCode = gant.executeTargets();
            return result;
        } finally {
            cleanup(result, binding);
        }
    }

    class GantResult {
        int exitCode;
        GroovyObject script;
    }

    private boolean isGrailsProject() {
        return new File(settings.getBaseDir(), "grails-app").exists();
    }

    private boolean isExternalScript(File scriptFile) {
        return scriptsAllowedOutsideOfProject.contains(scriptFile);
    }

    private String getScriptNameFromFile(File scriptPath) {
        String fileName = scriptPath.getName();
        String scriptFileName = fileName.substring(0, fileName.length() - 7); // trim .groovy extension
        if (scriptFileName.endsWith("_")) {
            scriptFileName = scriptFileName.substring(0, scriptFileName.length()-1);
        }
        return scriptFileName;
    }

    /**
     * Returns a list of all the executable Gant scripts available to this application.
     */
    public List<File> getAvailableScripts() {
        List<File> scripts = new ArrayList<File>();
        if (settings.getGrailsHome() != null) {
            addCommandScripts(new File(settings.getGrailsHome(), "scripts"), scripts);
        }
        addCommandScripts(new File(settings.getBaseDir(), "scripts"), scripts);
        addCommandScripts(new File(settings.getUserHome(), ".grails/scripts"), scripts);

        for (File dir : pluginPathSupport.listKnownPluginDirs()) {
            addPluginScripts(dir, scripts);
        }

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(settings.getRootLoader());
        try {
            final Resource[] resources = resolver.getResources("classpath*:META-INF/scripts/*.groovy");
            for (Resource resource : resources) {
                scripts.add(resource.getFile());
            }
        } catch (IOException e) {
            // ignore
        }
        return scripts;
    }

    /**
     * Collects all the command scripts provided by the plugin contained
     * in the given directory and adds them to the given list.
     */
    private static void addPluginScripts(File pluginDir, List<File> scripts) {
        if (!pluginDir.exists()) return;

        File scriptDir = new File(pluginDir, "scripts");
        if (scriptDir.exists()) addCommandScripts(scriptDir, scripts);
    }

    /**
     * Adds all the command scripts (i.e. those whose name does *not* start with an
     * underscore, '_') found in the given directory to the given list.
     */
    private static void addCommandScripts(File dir, List<File> scripts) {
        if (dir.exists()) {
            for (File file : dir.listFiles()) {
                if (scriptFilePattern.matcher(file.getName()).matches()) {
                    scripts.add(file);
                }
            }
        }
    }

    /**
     * Contains details about a Grails command invocation such as the
     * name of the corresponding script, the environment (if specified),
     * and the arguments to the command.
     */
    private static class ScriptAndArgs {
        public String inputName;
        public String name;
        public String env;
    }
}

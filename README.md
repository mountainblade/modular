![modular](http://puu.sh/iok33/e143f15d13.png)

Lightweight, annotation-driven modular dependency system for java applications with integrated field-based dependency
injection (inspired by [jSPF](https://code.google.com/p/jspf) and other java plugin systems).

A copy of modular can be retrieved through maven. It is assumed that it is known how one installs a custom maven
repository and dependency as this project uses that build system.

```xml
<repository>
    <id>modular</id>
    <url>http://repo.serkprojects.com/content/groups/public/</url>
</repository>

<dependency>
    <groupId>net.mountainblade</groupId>
    <artifactId>modular</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

# Usage guide
## First steps
To create the simplest of all implementations, just import the project into your workspace / setup.
Modular needs at least one manager and one module to work.

```java
// Create new manager and load modules from the whole classpath
Collection<Modules> modules = new DefaultModuleManager().loadModules("")
```

To create a module, just create a similar class like the following example:
```java
import net.mountainblade.modular.Module;
import net.mountainblade.modular.annotations.Implementation;

@Implementation
public class ExampleModule implements Module {
}
```

## Hierarchic Module Management
Modular also supports hierarchically loaded modules. This is often suited for applications that want to have different
sets of modules loaded at the same time, but have them limited each to their own class realm while keeping the same
parent. As the system already encapsulated the loaded modules into their own class loader, the hierarchic module
manager can have another manager as its parent and load modules as children.

Suitable applications would be custom editors (in conjunction with workspaces) or games with mod support
and different game saves.
```java
ModuleManager childManager = new HierarchicModuleManager(parentManager)
```

## Loading modules from various sources
Internally the system uses URI to load the classes/files. JAR files are automatically getting recognized and loaded.
Specifying a folder containing ".class" files is also possible.

Example: To load a list of plugins as JAR files inside a folder it would look like the following:
```java
Collection<Module> loadedPlugins = manager.loadModules(pluginDirectory);
```

If modules inside a specific package should be loaded the loadModules accepting a String can be used:
```java
Collection<Module> modules = manager.loadModules("com.example");
```

## Filtering modules
When only modules with a specific name, superclass, interface or annotation should be loaded filters can be used.
As the filter is just an interface, custom ones can be easily added.

Example: Loading modules with a specific annotation that are not implementing a specific interface:
```java
Collection<Module> modules = manager.loadModules("",
        new AnnotationPresent(Plugin.class),
        new Not(new InstanceOf(NativePlugin.class)));
```

## Field injections
By default modular already injects fields marked with the `@Inject` annotation (Limited to fields exclusively).
That includes a custom logger instance, the module information and other modules as dependency.
If specified the system can also inject objects belonging to other modules (a dependency's information for example).

If a custom object should be injected, it has to be configured beforehand. The Injector uses a builder-like pattern
to add custom injections (The following examples are using java 8, in version 6 and 7 anonymous classes are needed):

```java
Injector injector = manager.getInjector();
injector.inject(Logger.class).with((annotation, type, module) -> Logger.getLogger(module.getName()));
injector.inject(Module.class).with((annotation, type, module) -> registry.getModule(type));
injector.inject(File.class).marked(PluginDirectory.class).with(directory);
injector.inject(String.class).nullable().with(directory.getAbsolutePath());
injector.inject(ModuleInformation.class).with((annotation, type, module) -> registry.getInformation(module));
```

# Integration with existing systems
## Google Guice
If a more powerful dependency injection system is required google guice might be a good choice.
To integrate modular into the application a child-injector needs to be created, which could look like this:

```java
// Load modules beforehand
final ModuleManager manager = new DefaultModuleManager();
final Collection<Modules> modules = manager.loadModules("");

// Create child injector
final Injector childInjector = injector.createChildInjector(new AbstractModule() {
    @Override
    protected void configure() {
        for (Module module : modules) {
            final Class moduleClass = (Class) module.getClass();
            if (!hasBinding(moduleClass)) {
                bind(moduleClass).toInstance(module);
                requestInjection(module);
            }
        }
    }

    private boolean hasBinding(Class key) {
        return injector.getExistingBinding(Key.get(key)) != null;
    }
});
```

# FAQ
## My modules are not loading their dependencies right / duplicate modules or classes
If your application uses shading to include some basic modules, but also supports loading new ones from JAR files
the class loaders sometimes load classes twice (in your application's loader and in the one from the module manager).
Switching to the `ParentFirstStrategy` might help.
```java
manager.getLoader().setLoadingStrategy(ParentFirstStrategy.class);
```

# License
This project is licensed under the [Apache 2.0 license](https://www.tldrlegal.com/l/apache2).

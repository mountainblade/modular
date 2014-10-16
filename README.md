# modular
Annotation-driven modular dependency system for java applications (inspired by [jSPF](https://code.google.com/p/jspf)).

## Overview
To create a small demo, just import the project into your workspace / setup. The system needs at least one manager and
one module to work.

The manager can be created as follows:
```java
// Create new manager
ModuleManager moduleManager = new ModuleManagerImpl();

// Load modules from the whole classpath
moduleManager.loadModules(UriHelper.everything());
```

To create a module, just create the following class:
```java
import net.mountainblade.modular.Module;
import net.mountainblade.modular.annotations.Implementation;

@Implementation
public class ExampleModule implements Module {
}
```

For further usage examples just look at the test cases.

package net.mountainblade.modular.examples;

import net.mountainblade.modular.Module;
import net.mountainblade.modular.ModuleInformation;
import net.mountainblade.modular.annotations.Implementation;
import net.mountainblade.modular.annotations.Initialize;
import net.mountainblade.modular.annotations.Inject;

import java.util.logging.Logger;

@Implementation(version = "2.0-develop")
public class Example3Module implements Module {
    @Inject
    private Logger logger;

    @Inject
    private ModuleInformation info;


    @Initialize
    private void init() {
        logger.info("Running example 3 in version " + info.getVersion());
    }

}

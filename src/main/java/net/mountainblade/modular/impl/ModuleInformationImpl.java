package net.mountainblade.modular.impl;

import net.mountainblade.modular.ModuleInformation;
import net.mountainblade.modular.ModuleState;

import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;

class ModuleInformationImpl implements ModuleInformation {
    private final Properties properties = new Properties();

    private final Collection<String> authors;
    private final String version;

    private ModuleState state;


    ModuleInformationImpl(String version, String... authors) {
        this.version = version;
        this.authors = Arrays.asList(authors);

        this.state = ModuleState.UNKNOWN;
    }

    @Override
    public String[] getAuthors() {
        return authors.toArray(new String[authors.size()]);
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public ModuleState getState() {
        return state;
    }

    public void setState(ModuleState state) {
        this.state = state;
    }

    @Override
    public Properties getProperties() {
        return properties;
    }

}

package com.plugin.component

import org.gradle.api.artifacts.ModuleIdentifier
import org.gradle.api.artifacts.VersionConstraint
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.capabilities.Capability
import org.gradle.api.internal.artifacts.DefaultModuleIdentifier
import org.gradle.api.internal.artifacts.ImmutableVersionConstraint
import org.gradle.api.internal.artifacts.dependencies.DefaultImmutableVersionConstraint
import org.gradle.api.internal.attributes.ImmutableAttributes
import org.gradle.internal.component.external.model.DefaultModuleComponentSelector

public class FlatDirModuleComponentSelector implements ModuleComponentSelector {

    private final ModuleIdentifier moduleIdentifier
    private final ImmutableVersionConstraint versionConstraint
    private final ImmutableAttributes attributes
    private final List<Capability> requestedCapabilities
    private final int hashCode

    private FlatDirModuleComponentSelector(ModuleIdentifier module, ImmutableVersionConstraint version, ImmutableAttributes attributes, List<Capability> requestedCapabilities) {
        assert module != null: "module cannot be null"

        assert version != null: "sdkVersion cannot be null"

        assert attributes != null: "attributes cannot be null"

        assert requestedCapabilities != null: "capabilities cannot be null"

        this.moduleIdentifier = module
        this.versionConstraint = version
        this.attributes = attributes
        this.requestedCapabilities = requestedCapabilities
        this.hashCode = Objects.hash(version, module, attributes, requestedCapabilities)
    }

    public String getDisplayName() {
        String group = this.moduleIdentifier.getGroup()
        String module = this.moduleIdentifier.getName()
        String version = this.getVersion()
        StringBuilder builder = new StringBuilder(group.length() + module.length() + this.versionConstraint.getRequiredVersion().length() + 2)
        builder.append(group)
        builder.append(":")
        builder.append(module)
        if (version.length() > 0) {
            builder.append(":")
            builder.append(version)
        }

        if (this.versionConstraint.getBranch() != null) {
            builder.append(" (branch: ")
            builder.append(this.versionConstraint.getBranch())
            builder.append(")")
        }

        return builder.toString()
    }

    public String getGroup() {
        return this.moduleIdentifier.getGroup()
    }

    public String getModule() {
        return this.moduleIdentifier.getName()
    }

    public String getVersion() {
        return this.versionConstraint.getRequiredVersion().isEmpty() ? this.versionConstraint.getPreferredVersion() : this.versionConstraint.getRequiredVersion()
    }

    public VersionConstraint getVersionConstraint() {
        return this.versionConstraint
    }

    public ModuleIdentifier getModuleIdentifier() {
        return this.moduleIdentifier
    }

    public AttributeContainer getAttributes() {
        return this.attributes
    }

    List<Capability> getRequestedCapabilities() {
        return this.requestedCapabilities
    }

    public boolean matchesStrictly(ComponentIdentifier identifier) {
        assert identifier != null: "identifier cannot be null"

        if (identifier instanceof ModuleComponentIdentifier) {
            ModuleComponentIdentifier moduleComponentIdentifier = (ModuleComponentIdentifier) identifier
            if (this.moduleIdentifier.getGroup() == moduleComponentIdentifier.getGroup()
                    && this.moduleIdentifier.getName() == moduleComponentIdentifier.getModule()) {
                return true
            }
        }
        return false
    }

    public boolean equals(Object o) {
        if (o != null && o instanceof DefaultModuleComponentSelector) {
            DefaultModuleComponentSelector selector = (DefaultModuleComponentSelector) o
            if (this.moduleIdentifier.getName() == selector.moduleIdentifier.getName() &&
                    this.moduleIdentifier.getGroup() == selector.moduleIdentifier.getGroup()) {
                return true
            }
        }
        return false
    }

    public int hashCode() {
        return this.hashCode
    }

    public String toString() {
        return this.getDisplayName()
    }

    public static ModuleComponentSelector newSelector(String name) {
        return new FlatDirModuleComponentSelector(DefaultModuleIdentifier.newId("", name), DefaultImmutableVersionConstraint.of(), ImmutableAttributes.EMPTY, new ArrayList<Capability>())
    }
}
package com.chaoslab;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

/**
 * Hace cumplir las reglas de arquitectura del proyecto (CLAUDE.md, directrices §1.2) de forma
 * automática: la dependencia siempre apunta hacia adentro y el dominio es puro.
 */
class ArchitectureTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages("com.chaoslab");

    @Test
    void domainDoesNotDependOnFrameworksNorOuterLayers() {
        noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "org.springframework..",
                "org.yaml..",
                "jakarta..",
                "picocli..",
                "com.chaoslab.application..",
                "com.chaoslab.infrastructure..")
            .as("el dominio no debe depender de frameworks (Spring/SnakeYAML/web) ni de capas externas")
            .check(CLASSES);
    }

    @Test
    void dependenciesPointInward() {
        layeredArchitecture().consideringOnlyDependenciesInLayers()
            .layer("Domain").definedBy("com.chaoslab.domain..")
            .layer("Application").definedBy("com.chaoslab.application..")
            .layer("Infrastructure").definedBy("com.chaoslab.infrastructure..")
            .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Infrastructure")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure")
            .check(CLASSES);
    }
}

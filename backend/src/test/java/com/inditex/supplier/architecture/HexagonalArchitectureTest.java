package com.inditex.supplier.architecture;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.junit.jupiter.api.Test;

/**
 * Enforces, as a build-time check rather than just a package-info claim, the layering rules
 * documented throughout the codebase (see {@code domain}/{@code application}/
 * {@code infrastructure} package-info.java): dependencies only ever point inward, and
 * {@code domain} is framework-free plain Java.
 */
class HexagonalArchitectureTest {

    private static final com.tngtech.archunit.core.domain.JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.inditex.supplier");

    @Test
    void domainMustNotDependOnAnyFramework() {
        ArchRule rule = ArchRuleDefinition.noClasses()
                .that().resideInAPackage("com.inditex.supplier.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "jakarta..", "javax..", "org.hibernate..", "io.github.resilience4j..");

        rule.check(CLASSES);
    }

    @Test
    void domainMustNotDependOnApplicationOrInfrastructure() {
        ArchRule rule = ArchRuleDefinition.noClasses()
                .that().resideInAPackage("com.inditex.supplier.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.inditex.supplier.application..", "com.inditex.supplier.infrastructure..");

        rule.check(CLASSES);
    }

    @Test
    void applicationMustNotDependOnInfrastructure() {
        ArchRule rule = ArchRuleDefinition.noClasses()
                .that().resideInAPackage("com.inditex.supplier.application..")
                .should().dependOnClassesThat().resideInAPackage("com.inditex.supplier.infrastructure..");

        rule.check(CLASSES);
    }

    @Test
    void onlyApplicationServiceUsesTransactional() {
        ArchRule rule = ArchRuleDefinition.noClasses()
                .that().resideOutsideOfPackage("com.inditex.supplier.application.service..")
                .should().beAnnotatedWith(org.springframework.transaction.annotation.Transactional.class);

        rule.check(CLASSES);
    }
}

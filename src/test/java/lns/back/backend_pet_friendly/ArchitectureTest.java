package lns.back.backend_pet_friendly;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

@AnalyzeClasses(packages = "lns.back.backend_pet_friendly", importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitectureTest {

    @ArchTest
    static final ArchRule domain_must_not_depend_on_infrastructure =
        noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..infrastructure..", "..web..", "..config..");

    @ArchTest
    static final ArchRule controllers_must_not_call_each_other =
        noClasses().that().resideInAPackage("..web.controller..")
            .should().dependOnClassesThat().resideInAPackage("..web.controller..");

    @ArchTest
    static final ArchRule domain_services_only_use_domain =
        classes().that().resideInAPackage("..domain.service..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage("..domain..", "java..", "org.slf4j..", "org.springframework.stereotype..", "org.springframework.security..", "org.springframework.data.domain..", "lombok..");
}

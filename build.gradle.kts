plugins {
    id("com.diffplug.spotless") version "8.8.0" apply false
    id("standard-kotlin") apply false
    alias(libs.plugins.dokka)
    jacoco
    id("jacoco-report-aggregation")
    id("test-report-aggregation")
}

repositories { mavenCentral() }

dependencies {
    subprojects.forEach {
        if(it.name != "tools") {
            dokka(it)
            jacocoAggregation(it)
            testReportAggregation(it)
        }
    }
}

reporting {
    reports {
        create<AggregateTestReport>("aggregateTestReport") {
            testSuiteName = "test"
        }
        create<JacocoCoverageReport>("aggregateCoverageReport") {
            testSuiteName = "test"
        }
    }
}


dokka {
    dokkaPublications.html {
        includes.from(file("README.md"))
        suppressInheritedMembers.set(false)
        suppressObviousFunctions.set(true)
        offlineMode.set(false)
    }
}

jacoco {
    toolVersion = "0.8.15"
}

package org.keycloak.tests.suites;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.keycloak.tests.admin.AdminHeadersTest;

@Suite
// TODO: Select relevant test classes or packages once they have been migrated
@SelectClasses(AdminHeadersTest.class)
@SelectPackages({"org.keycloak.tests.welcomepage"})
public class JDKTestSuite {
}

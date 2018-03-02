/*
 * Copyright 2006-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.consol.citrus.testng;

import com.consol.citrus.TestClass;
import com.consol.citrus.main.AbstractTestEngine;
import com.consol.citrus.main.TestRunConfiguration;
import com.consol.citrus.main.scan.ClassPathTestScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.testng.*;
import org.testng.annotations.Test;
import org.testng.xml.*;

import java.util.*;

/**
 * @author Christoph Deppisch
 * @since 2.7.4
 */
public class TestNGEngine extends AbstractTestEngine {

    /** Logger */
    private static Logger log = LoggerFactory.getLogger(TestNGEngine.class);

    private List<ITestNGListener> listeners = new ArrayList<>();

    /**
     * Default constructor using run configuration.
     * @param configuration
     */
    public TestNGEngine(TestRunConfiguration configuration) {
        super(configuration);
    }

    public void run() {
        TestNG testng = new TestNG();

        for (ITestNGListener listener : listeners) {
            testng.addListener(listener);
        }

        XmlSuite suite = new XmlSuite();
        testng.setXmlSuites(Collections.singletonList(suite));

        if (!CollectionUtils.isEmpty(getConfiguration().getTestClasses())) {
            for (TestClass testClass : getConfiguration().getTestClasses()) {
                log.info(String.format("Running test %s", Optional.ofNullable(testClass.getMethod()).map(method -> testClass.getName() + "#" + method).orElse(testClass.getName())));

                XmlTest test = new XmlTest(suite);
                test.setClasses(new ArrayList<>());

                XmlClass clazz = new XmlClass(testClass.getName());
                if (StringUtils.hasText(testClass.getMethod())) {
                    clazz.setIncludedMethods(Collections.singletonList(new XmlInclude(testClass.getMethod())));
                }

                test.getClasses().add(clazz);
            }
        } else {
            List<String> packagesToRun = getConfiguration().getPackages();
            if (CollectionUtils.isEmpty(packagesToRun)) {
                packagesToRun = Collections.singletonList("");
                log.info("Running all tests in project");
            }

            for (String packageName : packagesToRun) {
                if (StringUtils.hasText(packageName)) {
                    log.info(String.format("Running tests in package %s", packageName));
                }

                XmlTest test = new XmlTest(suite);
                test.setClasses(new ArrayList<>());

                new ClassPathTestScanner(getConfiguration().getIncludes()).findTestsInPackage(packageName, Test.class)
                        .stream()
                        .peek(testClass -> log.info(String.format("Running test %s", Optional.ofNullable(testClass.getMethod()).map(method -> testClass.getName() + "#" + method).orElse(testClass.getName()))))
                        .map(clazz -> new XmlClass(clazz.getName()))
                        .forEach(test.getClasses()::add);

                log.info(String.format("Found %s test classes to execute", test.getClasses().size()));
            }
        }
        
        testng.run();
    }

    /**
     * Adds run listener in fluent API.
     * @param listener
     */
    public TestNGEngine addTestListener(ITestNGListener listener) {
        this.listeners.add(listener);
        return this;
    }
}

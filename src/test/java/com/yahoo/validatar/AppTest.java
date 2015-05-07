/*
 * Copyright 2014-2015 Yahoo! Inc.
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

package com.yahoo.validatar;

import com.yahoo.validatar.common.TestSuite;
import com.yahoo.validatar.report.FormatManager;
import com.yahoo.validatar.parse.ParseManager;
import com.yahoo.validatar.execution.EngineManager;
import com.yahoo.validatar.execution.hive.Apiary;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;

import java.sql.SQLException;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import static java.util.Arrays.*;

import org.testng.Assert;
import org.testng.annotations.Test;

public class AppTest {
    private class MemoryDB extends Apiary {
        private OptionParser parser = new OptionParser() {
            {
                acceptsAll(asList("hive-driver"), "Fully qualified package name to the hive driver.")
                    .withRequiredArg()
                    .describedAs("Hive driver");
                acceptsAll(asList("hive-jdbc"), "JDBC string to the HiveServer. Ex: 'jdbc:hive2://HIVE_SERVER:PORT/DATABASENAME' ")
                    .withRequiredArg()
                    .describedAs("Hive JDBC connector.");
                acceptsAll(asList("hive-username"), "Hive server username.")
                    .withRequiredArg()
                    .describedAs("Hive server username.")
                    .defaultsTo("anon");
                acceptsAll(asList("hive-password"), "Hive server password.")
                    .withRequiredArg()
                    .describedAs("Hive server password.")
                    .defaultsTo("anon");
                allowsUnrecognizedOptions();
            }
        };

        @Override
        public boolean setup(String[] arguments) {
            try {
                setupConnection(parser.parse(arguments));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return true;
        }
    }

    private class CustomEngineManager extends EngineManager {
        public CustomEngineManager(String[] arguments) {
            super();
            this.arguments = arguments;
            this.engines = new HashMap<String, WorkingEngine>();
            MemoryDB db = new MemoryDB();
            db.setup(arguments);
            engines.put(Apiary.ENGINE_NAME, new WorkingEngine(db));
        }
    }

    @Test
    public void testRunTests() throws Exception {
        String[] args = {"--report-file", "target/AppTest-testRunTests.xml",
                         "--hive-driver", "org.h2.Driver",
                         "--hive-jdbc",   "jdbc:h2:mem:"};
        Map<String, String> parameterMap = new HashMap<String, String>();
        File emptyTest = new File("src/test/resources/sample-tests/empty-test.yaml");
        ParseManager parseManager = new ParseManager();
        EngineManager engineManager = new CustomEngineManager(args);
        FormatManager formatManager = new FormatManager(args);

        App.run(emptyTest, parameterMap, parseManager, engineManager, formatManager);
    }

    @Test(expectedExceptions = {RuntimeException.class})
    public void testParameterParsingFailure() throws IOException {
        System.setOut(new PrintStream(new FileOutputStream("target/out")));
        System.setErr(new PrintStream(new FileOutputStream("target/err")));

        String args[] = {"--parameter", "DATE:20140807"};
        OptionSet options = App.parse(args);
        App.splitParameters(options, "parameter");

        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
        System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err)));
    }

    @Test
    public void testSimpleParameterParse() {
        // Fake CLI args
        String[] args = { "--test-suite", "tests.yaml",
                          "--parameter", "DATE=2014071800",
                          "--parameter", "NAME=ALPHA"};

        // Parse CLI args
        Map<String, String> paramMap = null;
        OptionSet options = null;
        try {
            options = App.parse(args);
            paramMap = App.splitParameters(options, "parameter");
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Should not reach here.");
        }

        // Check parse
        File testFile = (File) options.valueOf("test-suite");
        Assert.assertEquals((String) testFile.getName(), "tests.yaml");

        Assert.assertEquals(paramMap.get("DATE"), "2014071800");
        Assert.assertEquals(paramMap.get("NAME"), "ALPHA");
    }
}
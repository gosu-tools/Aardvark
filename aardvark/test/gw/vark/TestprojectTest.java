/*
 * Copyright (c) 2010 Guidewire Software, Inc.
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

package gw.vark;

import gw.lang.reflect.IType;
import gw.lang.reflect.TypeSystem;
import gw.lang.reflect.gs.IGosuClass;
import gw.lang.reflect.gs.IGosuProgram;
import gw.lang.shell.Gosu;
import gw.vark.testapi.AardvarkAssertions;
import gw.vark.testapi.InMemoryLogger;
import gw.vark.testapi.StringMatchAssertion;
import gw.vark.testapi.TestUtil;
import gw.vark.typeloader.AntlibTypeLoader;
import junit.framework.Assert;
import org.apache.tools.ant.Project;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

/**
 * A test class for running targets with a one-time Gosu initialization.
 *
 * Take note that this won't play nice if run in the same session as other
 * test classes, since it taints the environment by initializing Gosu.
 */
public class TestprojectTest extends AardvarkAssertions {

  private static File _varkFile;
  private static IGosuProgram _gosuProgram;

  @BeforeClass
  public static void initGosu() throws Exception {
    Aardvark.setProject(new Project()); // this allows Gosu initialization to have a Project to log to
    File home = TestUtil.getHome(TestprojectTest.class);
    _varkFile = new File(home, "testproject/build.vark");
    Aardvark.initGosu(_varkFile);
    _gosuProgram = Aardvark.parseAardvarkProgram(_varkFile);
  }

  @Test
  public void echoHello() {
    InMemoryLogger logger = vark("echo-hello");
    assertThat(logger).matches(
            StringMatchAssertion.exact(""),
            StringMatchAssertion.exact("echo-hello:"),
            StringMatchAssertion.exact("     [echo] Hello World"),
            StringMatchAssertion.exact(""),
            StringMatchAssertion.exact("BUILD SUCCESSFUL"),
            StringMatchAssertion.regex("Total time: \\d+ seconds?"));
  }

  @Test
  public void enhancementContributedTargetIsPresent() {
    IType type = TypeSystem.getByFullNameIfValid("vark.SampleVarkFileEnhancement");
    if (!type.isValid()) {
      Assert.fail("Enhancement should be valid: " + ((IGosuClass) type).getParseResultsException().getFeedback());
    }
    InMemoryLogger logger = vark("-p");
    assertThat(logger).containsLinesThatContain("target-from-enhancement");
    assertThat(logger).excludesLinesThatContain("not-a-target-from-enhancement");
  }

  @Test
  public void targetWithArg() {
    InMemoryLogger results = vark("target-with-arg", "-foo", "echo-hello");
    assertThat(results).matches(
            StringMatchAssertion.exact(""),
            StringMatchAssertion.exact("target-with-arg:"),
            StringMatchAssertion.exact("     [echo] foo: echo-hello (java.lang.String)"),
            StringMatchAssertion.exact(""),
            StringMatchAssertion.exact("BUILD SUCCESSFUL"),
            StringMatchAssertion.regex("Total time: \\d+ seconds?"));
  }

  @Test
  public void targetWithArgButNoValue() {
    try {
      vark("target-with-arg", "-foo");
      Assert.fail("expected " + IllegalArgumentException.class.getSimpleName());
    }
    catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("\"foo\" is expected to be followed by a value");
    }
  }

  @Test
  public void targetWithArgButNoArg() {
    try {
      vark("target-with-arg");
      Assert.fail("expected " + IllegalArgumentException.class.getSimpleName());
    }
    catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("requires parameter \"foo\"");
    }
  }

  @Test
  public void targetWithArgWithNonexistentArg() {
    try {
      vark("target-with-arg", "-foo", "somestring", "-bar", "somestring");
      Assert.fail("expected " + IllegalArgumentException.class.getSimpleName());
    }
    catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("no parameter named \"bar\"");
    }
  }

  @Test
  public void targetWithDefaultValueArg() {
    InMemoryLogger results = vark("target-with-default-value-arg");
    assertThat(results).matches(
            StringMatchAssertion.exact(""),
            StringMatchAssertion.exact("target-with-default-value-arg:"),
            StringMatchAssertion.exact("     [echo] foo: baz (java.lang.String)"),
            StringMatchAssertion.exact(""),
            StringMatchAssertion.exact("BUILD SUCCESSFUL"),
            StringMatchAssertion.regex("Total time: \\d+ seconds?"));
  }

  @Test
  public void targetWithDefaultValueOverriddenByUserValue() {
    InMemoryLogger results = vark("target-with-default-value-arg", "-foo", "somestring");
    assertThat(results).matches(
            StringMatchAssertion.exact(""),
            StringMatchAssertion.exact("target-with-default-value-arg:"),
            StringMatchAssertion.exact("     [echo] foo: somestring (java.lang.String)"),
            StringMatchAssertion.exact(""),
            StringMatchAssertion.exact("BUILD SUCCESSFUL"),
            StringMatchAssertion.regex("Total time: \\d+ seconds?"));
  }

  // BOOLEAN TARGET ARG TESTS

  @Test
  public void targetWithBooleanArgWithUnparseableUserValue() {
    try {
      vark("target-with-boolean-arg", "-foo", "abcd");
      Assert.fail("expected " + IllegalArgumentException.class.getSimpleName());
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("\"foo\" value is expected to be a boolean, was \"abcd\"");
    }
  }

  @Test
  public void targetWithBooleanArgNoDefaultValueUserValueTrue() {
    InMemoryLogger results = vark("target-with-boolean-arg", "-foo", "true");
    assertThat(results).matches(
            StringMatchAssertion.exact(""),
            StringMatchAssertion.exact("target-with-boolean-arg:"),
            StringMatchAssertion.exact("     [echo] foo: true (boolean)"),
            StringMatchAssertion.exact(""),
            StringMatchAssertion.exact("BUILD SUCCESSFUL"),
            StringMatchAssertion.regex("Total time: \\d+ seconds?"));
  }

  @Test
  public void targetWithBooleanArgNoDefaultValueUserValueFalse() {
    InMemoryLogger results = vark("target-with-boolean-arg", "-foo", "false");
    assertThat(results).matches(
            StringMatchAssertion.exact(""),
            StringMatchAssertion.exact("target-with-boolean-arg:"),
            StringMatchAssertion.exact("     [echo] foo: false (boolean)"),
            StringMatchAssertion.exact(""),
            StringMatchAssertion.exact("BUILD SUCCESSFUL"),
            StringMatchAssertion.regex("Total time: \\d+ seconds?"));
  }

  @Test
  public void targetWithBooleanArgNoDefaultValueNoUserValue() {
    InMemoryLogger results = vark("target-with-boolean-arg", "-foo");
    assertThat(results).matches(
            StringMatchAssertion.exact(""),
            StringMatchAssertion.exact("target-with-boolean-arg:"),
            StringMatchAssertion.exact("     [echo] foo: true (boolean)"),
            StringMatchAssertion.exact(""),
            StringMatchAssertion.exact("BUILD SUCCESSFUL"),
            StringMatchAssertion.regex("Total time: \\d+ seconds?"));
  }

  @Test
  public void targetWithBooleanArgNoDefaultValueNoUserParam() {
    InMemoryLogger results = vark("target-with-boolean-arg");
    assertThat(results).matches(
            StringMatchAssertion.exact(""),
            StringMatchAssertion.exact("target-with-boolean-arg:"),
            StringMatchAssertion.exact("     [echo] foo: false (boolean)"),
            StringMatchAssertion.exact(""),
            StringMatchAssertion.exact("BUILD SUCCESSFUL"),
            StringMatchAssertion.regex("Total time: \\d+ seconds?"));
  }

  @Test
  public void targetWithBooleanArgDefaultValueFalseUserValueTrue() {
    InMemoryLogger results = vark("target-with-boolean-arg-default-false", "-foo", "true");
    assertThat(results).matches(
            StringMatchAssertion.exact(""),
            StringMatchAssertion.exact("target-with-boolean-arg-default-false:"),
            StringMatchAssertion.exact("     [echo] foo: true (boolean)"),
            StringMatchAssertion.exact(""),
            StringMatchAssertion.exact("BUILD SUCCESSFUL"),
            StringMatchAssertion.regex("Total time: \\d+ seconds?"));
  }

  @Test
  public void targetWithBooleanArgDefaultValueFalseNoUserValue() {
    InMemoryLogger results = vark("target-with-boolean-arg-default-false", "-foo");
    assertThat(results).matches(
            StringMatchAssertion.exact(""),
            StringMatchAssertion.exact("target-with-boolean-arg-default-false:"),
            StringMatchAssertion.exact("     [echo] foo: true (boolean)"),
            StringMatchAssertion.exact(""),
            StringMatchAssertion.exact("BUILD SUCCESSFUL"),
            StringMatchAssertion.regex("Total time: \\d+ seconds?"));
  }

  @Test
  public void targetWithBooleanArgDefaultValueTrueUserValueFalse() {
    InMemoryLogger results = vark("target-with-boolean-arg-default-true", "-foo", "false");
    assertThat(results).matches(
            StringMatchAssertion.exact(""),
            StringMatchAssertion.exact("target-with-boolean-arg-default-true:"),
            StringMatchAssertion.exact("     [echo] foo: false (boolean)"),
            StringMatchAssertion.exact(""),
            StringMatchAssertion.exact("BUILD SUCCESSFUL"),
            StringMatchAssertion.regex("Total time: \\d+ seconds?"));
  }

  // INT TARGET ARG TESTS

  @Test
  public void targetWithIntArg() {
    InMemoryLogger results = vark("target-with-int-arg", "-foo", "2468");
    assertThat(results).matches(
            StringMatchAssertion.exact(""),
            StringMatchAssertion.exact("target-with-int-arg:"),
            StringMatchAssertion.exact("     [echo] foo: 2468 (int)"),
            StringMatchAssertion.exact(""),
            StringMatchAssertion.exact("BUILD SUCCESSFUL"),
            StringMatchAssertion.regex("Total time: \\d+ seconds?"));
  }

  @Test
  public void targetWithIntArgWithUnparseableUserValue() {
    try {
      vark("target-with-int-arg", "-foo", "abcd");
      Assert.fail("expected " + IllegalArgumentException.class.getSimpleName());
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("\"foo\" value is expected to be an int, was \"abcd\"");
    }
  }

  @Test
  public void targetWithDefaultValueIntArg() {
    InMemoryLogger results = vark("target-with-int-arg-default1357");
    assertThat(results).matches(
            StringMatchAssertion.exact(""),
            StringMatchAssertion.exact("target-with-int-arg-default1357:"),
            StringMatchAssertion.exact("     [echo] foo: 1357 (int)"),
            StringMatchAssertion.exact(""),
            StringMatchAssertion.exact("BUILD SUCCESSFUL"),
            StringMatchAssertion.regex("Total time: \\d+ seconds?"));
  }

  @Test
  public void targetWithDefaultValueIntArgOverriddenByUserValue() {
    InMemoryLogger results = vark("target-with-int-arg-default1357", "-foo", "2468");
    assertThat(results).matches(
            StringMatchAssertion.exact(""),
            StringMatchAssertion.exact("target-with-int-arg-default1357:"),
            StringMatchAssertion.exact("     [echo] foo: 2468 (int)"),
            StringMatchAssertion.exact(""),
            StringMatchAssertion.exact("BUILD SUCCESSFUL"),
            StringMatchAssertion.regex("Total time: \\d+ seconds?"));
  }

  // MISC TARGET ARG TESTS

  @Test
  public void targetArgAppearsInHelp() {
    InMemoryLogger logger = vark("-p");
    assertThat(logger).containsLinesThatContain("-foo: An argument called foo");
  }

  @Test
  public void targetArgDefaultAppearsInHelp() {
    InMemoryLogger logger = vark("-p");
    assertThat(logger).containsLinesThatContain("-foo (optional, default baz): An argument with a default value");
  }

  @Test
  public void targetWithTwoArgs() {
    InMemoryLogger results = vark("target-with-two-args", "-foo", "echo-hello", "-bar", "echo-hello-2");
    assertThat(results).matches(
            StringMatchAssertion.exact(""),
            StringMatchAssertion.exact("target-with-two-args:"),
            StringMatchAssertion.exact("     [echo] foo: echo-hello, bar: echo-hello-2"),
            StringMatchAssertion.exact(""),
            StringMatchAssertion.exact("BUILD SUCCESSFUL"),
            StringMatchAssertion.regex("Total time: \\d+ seconds?"));
  }

  @Test
  public void targetWithTwoDefaultValueArgs() {
    InMemoryLogger results = vark("target-with-two-default-value-args");
    assertThat(results).matches(
            StringMatchAssertion.exact(""),
            StringMatchAssertion.exact("target-with-two-default-value-args:"),
            StringMatchAssertion.exact("     [echo] foo: baz, bar: baz2"),
            StringMatchAssertion.exact(""),
            StringMatchAssertion.exact("BUILD SUCCESSFUL"),
            StringMatchAssertion.regex("Total time: \\d+ seconds?"));
  }

  @Test
  public void targetWithTwoDefaultValueArgsWithFirstUserValue() {
    InMemoryLogger results = vark("target-with-two-default-value-args", "-foo", "somestring");
    assertThat(results).matches(
            StringMatchAssertion.exact(""),
            StringMatchAssertion.exact("target-with-two-default-value-args:"),
            StringMatchAssertion.exact("     [echo] foo: somestring, bar: baz2"),
            StringMatchAssertion.exact(""),
            StringMatchAssertion.exact("BUILD SUCCESSFUL"),
            StringMatchAssertion.regex("Total time: \\d+ seconds?"));
  }

  @Test
  public void targetWithTwoDefaultValueArgsWithSecondUserValue() {
    InMemoryLogger results = vark("target-with-two-default-value-args", "-bar", "somestring");
    assertThat(results).matches(
            StringMatchAssertion.exact(""),
            StringMatchAssertion.exact("target-with-two-default-value-args:"),
            StringMatchAssertion.exact("     [echo] foo: baz, bar: somestring"),
            StringMatchAssertion.exact(""),
            StringMatchAssertion.exact("BUILD SUCCESSFUL"),
            StringMatchAssertion.regex("Total time: \\d+ seconds?"));
  }

  @Test
  public void targetWithTwoDefaultValueArgsWithBothUserValues() {
    InMemoryLogger results = vark("target-with-two-default-value-args", "-foo", "somestring", "-bar", "somestring2");
    assertThat(results).matches(
            StringMatchAssertion.exact(""),
            StringMatchAssertion.exact("target-with-two-default-value-args:"),
            StringMatchAssertion.exact("     [echo] foo: somestring, bar: somestring2"),
            StringMatchAssertion.exact(""),
            StringMatchAssertion.exact("BUILD SUCCESSFUL"),
            StringMatchAssertion.regex("Total time: \\d+ seconds?"));
  }

  @Test
  public void targetWithBooleanArgWithUserValueOmittedFollowedByStringArg() {
    InMemoryLogger results = vark("target-with-boolean-arg-and-string-arg", "-foo", "-bar", "somestring");
    assertThat(results).matches(
            StringMatchAssertion.exact(""),
            StringMatchAssertion.exact("target-with-boolean-arg-and-string-arg:"),
            StringMatchAssertion.exact("     [echo] foo: true (boolean), bar: somestring (java.lang.String)"),
            StringMatchAssertion.exact(""),
            StringMatchAssertion.exact("BUILD SUCCESSFUL"),
            StringMatchAssertion.regex("Total time: \\d+ seconds?"));
  }

  @Test
  public void targetWithIllegalArgType() {
    try {
      vark("target-with-double-arg", "-foo", Double.toString(Math.PI));
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("type double for \"foo\" not supported");
    }
  }

  private InMemoryLogger vark(String... args) {
    InMemoryLogger logger = new InMemoryLogger();
    Aardvark aardvark = new Aardvark(logger);
    aardvark.runBuild(_varkFile, _gosuProgram, new AardvarkOptions(args));
    return logger;
  }
}

/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.jarmode.layertools;

import java.io.File;
import java.io.PrintStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link LayerToolsJarMode}.
 *
 * @author Phillip Webb
 */
class LayerToolsJarModeTests {

	private static final String[] NO_ARGS = {};

	private TestPrintStream out;

	private PrintStream systemOut;

	@BeforeEach
	void setup() {
		Context context = mock(Context.class);
		given(context.getJarFile()).willReturn(new File("test.jar"));
		this.out = new TestPrintStream(this);
		this.systemOut = System.out;
		System.setOut(this.out);
		LayerToolsJarMode.Runner.contextOverride = context;
	}

	@AfterEach
	void restore() {
		System.setOut(this.systemOut);
		LayerToolsJarMode.Runner.contextOverride = null;
	}

	@Test
	void mainWithNoParamersShowsHelp() {
		new LayerToolsJarMode().run("layertools", NO_ARGS);
		assertThat(this.out).hasSameContentAsResource("help-output.txt");
	}

	@Test
	void mainWithArgRunsCommand() {
		new LayerToolsJarMode().run("layertools", new String[] { "list" });
		assertThat(this.out).hasSameContentAsResource("list-output.txt");
	}

}

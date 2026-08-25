/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.quarkus.runtime.cli.command;

import static org.keycloak.quarkus.runtime.cli.Picocli.NO_PARAM_LABEL;
import static org.keycloak.quarkus.runtime.cli.command.AbstractAutoBuildCommand.OPTIMIZED_BUILD_OPTION_LONG;

import java.util.Iterator;
import java.util.stream.Stream;

import picocli.CommandLine;

public final class BuildMixin {

    @CommandLine.Option(names = {OPTIMIZED_BUILD_OPTION_LONG},
            description = "DEPRECATED: see the --build option instead. Use this option to achieve an optimal startup time if you have previously built a server image using the 'build' command.",
            paramLabel = NO_PARAM_LABEL,
            order = 1)
    boolean optimized;

    AbstractCommand.BuildOption buildOption;

    static class BuildOptionCandidates implements Iterable<String> {

        @Override
        public Iterator<String> iterator() {
            return Stream.of(AbstractCommand.BuildOption.values()).map(bo -> bo.key).iterator();
        }

    }

    @CommandLine.Option(names = {"--build" },
            description = "Use this option to control how the server augments",
            paramLabel = NO_PARAM_LABEL,
            completionCandidates = BuildOptionCandidates.class, order = 1)
    public void setBuild(String value) {
        this.buildOption = Stream.of(AbstractCommand.BuildOption.values()).filter(bo -> bo.key.equals(value)).findFirst().orElseThrow();
    }

}

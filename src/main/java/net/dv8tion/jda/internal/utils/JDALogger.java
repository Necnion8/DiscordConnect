/*
 * Copyright 2015 Austin Keener, Michael Ritter, Florian Spieß, and the JDA contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications copyright (C) 2025 nova27

package net.dv8tion.jda.internal.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;

@SuppressWarnings("unused")
public class JDALogger {
    @SuppressWarnings("unused")
    public static Logger getLog(String name) {
        return LoggerFactory.getLogger(name);
    }

    @SuppressWarnings("unused")
    public static Logger getLog(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }

    /**
     * Utility function to enable logging of complex statements more efficiently (lazy).
     *
     * @param lazyLambda The Supplier used when evaluating the expression
     * @return An Object that can be passed to SLF4J's logging methods as lazy parameter
     */
    @SuppressWarnings("unused")
    public static Object getLazyString(LazyEvaluation lazyLambda) {
        return new Object() {
            @Override
            public String toString() {
                try {
                    return lazyLambda.getString();
                } catch (Exception ex) {
                    StringWriter sw = new StringWriter();
                    ex.printStackTrace(new PrintWriter(sw));
                    return "Error while evaluating lazy String... " + sw;
                }
            }
        };
    }

    /**
     * Functional interface used for {@link #getLazyString(LazyEvaluation)} to lazily construct a String.
     */
    @FunctionalInterface
    public interface LazyEvaluation {
        /**
         * This method is used by {@link #getLazyString(LazyEvaluation)}
         * when SLF4J requests String construction.
         * <br>The String returned by this is used to construct the log message.
         *
         * @return The String for log message
         * @throws Exception To allow lazy evaluation of methods that might throw exceptions
         */
        String getString() throws Exception;
    }
}
